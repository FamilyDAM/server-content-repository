/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.familydam.core;

import com.familydam.core.observers.FileNodeObserver;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.felix.connect.launch.PojoServiceRegistry;
import org.apache.felix.scr.annotations.Reference;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.QueryBuilder;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.commons.cnd.CompactNodeTypeDefReader;
import org.apache.jackrabbit.commons.cnd.DefinitionBuilderFactory;
import org.apache.jackrabbit.commons.cnd.TemplateBuilderFactory;
import org.apache.jackrabbit.commons.jackrabbit.authorization.AccessControlUtils;
import org.apache.jackrabbit.oak.api.ContentRepository;
import org.apache.jackrabbit.oak.run.osgi.OakOSGiRepositoryFactory;
import org.apache.jackrabbit.oak.run.osgi.ServiceRegistryProvider;
import org.apache.jackrabbit.oak.spi.security.authorization.permission.PermissionConstants;
import org.apache.jackrabbit.oak.spi.security.authorization.permission.Permissions;
import org.apache.jackrabbit.oak.spi.state.NodeStore;
import org.apache.jackrabbit.value.StringValue;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.Privilege;
import javax.servlet.ServletContext;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Configuration
public class RepositoryConfig
{
    private final Logger log = LoggerFactory.getLogger(getClass());
    public static String trashPath = "/" +FamilyDAMConstants.SYSTEM_ROOT;
    public static String assetsPath = "/" +FamilyDAMConstants.SYSTEM_ROOT;
    public static String jobQueuePath = "/" +FamilyDAMConstants.SYSTEM_ROOT;

    @Autowired
    private ServletContext servletContext;

    @Value("classpath:/config/jcr/repository-config.json")
    private Resource defaultRepoConfig;

    @Value("${jcr.repo.home}")
    private String repoHome;
    private File repoHomeDir;

    @Autowired
    private ApplicationArguments args;

    @Reference
    private NodeStore nodeStore;

    @Reference
    private AccessControlManager accessControlManager;

    private Repository repository;
    private ContentRepository contentRepository;


    @PostConstruct
    public void initialize() throws Exception
    {
        initRepository();
    }

    @PreDestroy
    public void close() {
        if (repository instanceof JackrabbitRepository) {
            ((JackrabbitRepository) repository).shutdown();
            log.info("Repository shutdown complete");
            repository = null;
        }
    }

    @Bean(name="repository")
    public Repository getRepository(){
        return repository;
    }

    @Bean(name="contentRepository")
    public ContentRepository getContentRepository()
    {
        return contentRepository;
    }

    @Bean
    public FileNodeObserver fileNodeObserver()
    {
        return new FileNodeObserver("/dam:files/", JcrConstants.NT_FILE);
    }

    @Bean
    public PojoServiceRegistry getServiceRegistry(){
        return ((ServiceRegistryProvider) repository).getServiceRegistry();
    }


    private void initRepository() throws IOException, RepositoryException {
        repoHomeDir = new File(repoHome);
        FileUtils.forceMkdir(repoHomeDir);

        log.info("Repository Location: " +repoHomeDir.getAbsolutePath());

        List<String> configFileNames = determineConfigFileNamesToCopy();
        List<String> configFilePaths = copyConfigs(repoHomeDir, configFileNames);

        repository = createRepository(configFilePaths, repoHomeDir);
    }


    private Repository createRepository(List<String> repoConfigs, File repoHomeDir) throws RepositoryException {
        Map<String,Object> config = Maps.newHashMap();
        config.put(OakOSGiRepositoryFactory.REPOSITORY_HOME, repoHomeDir.getAbsolutePath());
        config.put(OakOSGiRepositoryFactory.REPOSITORY_CONFIG_FILE, commaSepFilePaths(repoConfigs));
        config.put(OakOSGiRepositoryFactory.REPOSITORY_SHUTDOWN_ON_TIMEOUT, false);
        config.put(OakOSGiRepositoryFactory.REPOSITORY_ENV_SPRING_BOOT, true);
        config.put(OakOSGiRepositoryFactory.REPOSITORY_TIMEOUT_IN_SECS, 10);

        //Set of properties used to perform property substitution in
        //OSGi configs
        config.put("repo.home", repoHomeDir.getAbsolutePath());

        config.put("PARAM_OMIT_ADMIN_PW", true);

        //Configures BundleActivator to get notified of
        //OSGi startup and shutdown
        configureActivator(config);

        Repository repository = new OakOSGiRepositoryFactory().getRepository(config);

        // Create a random password and save it as the new admin password. Overiding the shipping default.
        resetAdminPassword(repository);

        // Create User Groups
        createGroups(repository);

        // Using the CND file, make sure all of the required mix-ins have been created.
        registerCustomNodeTypes(repository);

        // register the default system directories
        registerSystemDirectories(repository);

        // add users if the start up argument was passed in
        createCustomUserFolders(repository);


        return repository;
    }


    private void resetAdminPassword(Repository repository)
    {
        try {
            File file = new File(repoHomeDir.getAbsolutePath()+"/_password");
            if( !file.exists() ) {
                //todo: save random password in properties file
                //@see http://www.jasypt.org/encrypting-configuration.html
                //@see http://stackoverflow.com/questions/41107/how-to-generate-a-random-alpha-numeric-string
                String _password = "admin123";
                //save password to new file
                FileUtils.writeStringToFile(file, _password);

                // Login with default admin/admin loging
                SimpleCredentials _credentials = new SimpleCredentials(FamilyDAM.adminUserId, FamilyDAM.adminPassword.toCharArray());
                Session session = repository.login(_credentials);

                // get the user manager
                UserManager userManager = ((JackrabbitSession) session).getUserManager();
                Authorizable authorizable = userManager.getAuthorizable(FamilyDAM.adminUserId);

                //reset the password
                ((User) authorizable).changePassword(_password);
                FamilyDAM.adminPassword = _password;
                session.save();
            }else{
                FamilyDAM.adminPassword = FileUtils.readFileToString(file);
            }
        }
        catch (RepositoryException|IOException ex) {
            ex.printStackTrace();
        }
    }



    private Object commaSepFilePaths(List<String> repoConfigs) {
        return Joiner.on(",").join(repoConfigs);
    }

    private List<String> copyConfigs(File repoHomeDir, List<String> configFileNames)
            throws IOException, RepositoryException {
        List<String> filePaths = Lists.newArrayList();
        for (String configName : configFileNames) {
            File dest = new File(repoHomeDir, configName);
            Resource source = new ClassPathResource("config/jcr/" + configName);
            copyDefaultConfig(dest, source);
            filePaths.add(dest.getAbsolutePath());
        }
        return filePaths;
    }


    private List<String> determineConfigFileNamesToCopy() {
        List<String> configNames = Lists.newArrayList();
        configNames.add("repository-config.json");
        configNames.add("segmentmk-config.json");
        return configNames;
    }


    private void copyDefaultConfig(File repoConfig, Resource defaultRepoConfig)
            throws IOException, RepositoryException {
        if (!repoConfig.exists()){
            log.info("Copying default repository config to {}", repoConfig.getAbsolutePath());
            InputStream in = defaultRepoConfig.getInputStream();
            if (in == null){
                throw new RepositoryException("No config file found in classpath " + defaultRepoConfig);
            }
            OutputStream os = null;
            try {
                os = FileUtils.openOutputStream(repoConfig);
                IOUtils.copy(in, os);
            } finally {
                IOUtils.closeQuietly(os);
                IOUtils.closeQuietly(in);
            }
        }
    }

    private void configureActivator(Map<String, Object> config) {
        config.put(BundleActivator.class.getName(), new BundleActivator() {
            @Override
            public void start(BundleContext bundleContext) throws Exception
            {
                servletContext.setAttribute(BundleContext.class.getName(), bundleContext);
            }

            @Override
            public void stop(BundleContext bundleContext) throws Exception
            {
                servletContext.removeAttribute(BundleContext.class.getName());
            }
        });
    }


    /**
     * Create the default groups we expect to have in the FamilyDAM.
     *
     * The group(s) are:
     * 1. "family_group" - all family members with an account are in this group.
     * 2. TBD
     *
     * @param repository
     */
    private void createGroups(Repository repository)
    {
        Session session = null;
        try {
            session = repository.login(new SimpleCredentials(FamilyDAM.adminUserId, FamilyDAM.adminPassword.toCharArray()));

            Privilege[] privledges = session.getAccessControlManager().getPrivileges("/");

            UserManager userManager = ((JackrabbitSession) session).getUserManager();
            if( userManager.getAuthorizable(FamilyDAMConstants.FAMILY_ADMIN_GROUP) == null ) {
                Group familyAdminGroup = userManager.createGroup(FamilyDAMConstants.FAMILY_ADMIN_GROUP);
                log.debug("Group Created: " + familyAdminGroup.getPath());
            }
            if( userManager.getAuthorizable(FamilyDAMConstants.FAMILY_GROUP) == null ) {
                Group familyGroup = userManager.createGroup(FamilyDAMConstants.FAMILY_GROUP);
                log.debug("Group Created: " + familyGroup.getPath());
            }

            session.save();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        finally {
            if (session != null) session.logout();
        }
    }



    /**
     * Using the CND file, make sure all of the required mix-ins have been created.
     * @param repository
     */
    private void registerCustomNodeTypes(Repository repository)
    {
        Session session = null;
        try {
            session = repository.login(new SimpleCredentials(FamilyDAM.adminUserId, FamilyDAM.adminPassword.toCharArray()));
            InputStream is = this.getClass().getClassLoader().getResourceAsStream("familydam_nodetypes.cnd");

            // Get the JackrabbitNodeTypeManager from the Workspace.
            // Note that it must be cast from the generic JCR NodeTypeManager to the
            // Jackrabbit-specific implementation.
            NodeTypeManager manager = session.getWorkspace().getNodeTypeManager();


            // TODO: for simplicity it's currently either registration or unregistration as nt-modifications are immediately persisted.
            String registerCnd = org.apache.commons.io.IOUtils.toString(is);
            List<String> unregisterNames = new ArrayList<String>();

            NodeTypeManager ntMgr = session.getWorkspace().getNodeTypeManager();
            if (registerCnd != null) {
                StringReader reader = new StringReader(registerCnd);
                DefinitionBuilderFactory<NodeTypeTemplate, NamespaceRegistry> factory =
                        new TemplateBuilderFactory(ntMgr,
                                session.getValueFactory(),
                                session.getWorkspace().getNamespaceRegistry());

                CompactNodeTypeDefReader<NodeTypeTemplate, NamespaceRegistry> cndReader =
                        new CompactNodeTypeDefReader<NodeTypeTemplate, NamespaceRegistry>(reader, "davex", factory);

                List<NodeTypeTemplate> ntts = cndReader.getNodeTypeDefinitions();
                ntMgr.registerNodeTypes(ntts.toArray(new NodeTypeTemplate[ntts.size()]), true);
            } else if (!unregisterNames.isEmpty()) {
                //ntMgr.unregisterNodeTypes(unregisterNames.toArray(new String[unregisterNames.size()]));
            }

            //CndImporter.registerNodeTypes(new InputStreamReader(is), session, true);

            session.save();
        }catch(Exception ex){
            ex.printStackTrace();
        }finally {
            if( session != null) session.logout();
        }
    }


    private void registerSystemDirectories(Repository repository)
    {
        Session session = null;
        try {
            SimpleCredentials credentials = new SimpleCredentials(FamilyDAM.adminUserId, FamilyDAM.adminPassword.toCharArray());
            session = repository.login(new SimpleCredentials(FamilyDAM.adminUserId, FamilyDAM.adminPassword.toCharArray()));

            Node _rootNode = session.getRootNode();
            Node _systemNode = createDAMSystemFolder(_rootNode, session);
            Node _filesNode = createDAMFilesFolder(_rootNode, session);
            //Node _cloudNode = createDAMCloudFolder(_rootNode);

            session.save();
        }catch(Exception ex){
            ex.printStackTrace();
        }finally {
            if( session != null) session.logout();
        }
    }



    private Node createDAMSystemFolder(Node _rootNode, Session session) throws RepositoryException
    {
        //Node _tmpNode = JcrUtils.getNodeIfExists(_rootNode, FamilyDAMConstants.SYSTEM_ROOT);
        //_tmpNode.remove();
        //session.save();
        UserManager userManager = ((JackrabbitSession) session).getUserManager();
        Group familyAdminGroup = (Group)userManager.getAuthorizable(FamilyDAMConstants.FAMILY_ADMIN_GROUP);
        Group familyGroup = (Group)userManager.getAuthorizable(FamilyDAMConstants.FAMILY_GROUP);


        Node _filesNode = JcrUtils.getOrAddNode(_rootNode, FamilyDAMConstants.SYSTEM_ROOT, JcrConstants.NT_UNSTRUCTURED);

        // Add read only access for everyone in the family group
        AccessControlUtils.addAccessControlEntry(session, _filesNode.getPath(), familyAdminGroup.getPrincipal(), new String[]{Privilege.JCR_ALL}, true);
        AccessControlUtils.addAccessControlEntry(session, _filesNode.getPath(), familyGroup.getPrincipal(), new String[]{Privilege.JCR_READ}, true);


        // Assets folder is used as a placeholder for system generated images and files (album covers, thumbnails, etc)
        Node _assetsFolderNode = JcrUtils.getOrAddNode(_filesNode, "assets", JcrConstants.NT_UNSTRUCTURED);
        _assetsFolderNode.addMixin("mix:created");
        _assetsFolderNode.addMixin("dam:systemfolder");
        _assetsFolderNode.addMixin("dam:extensible");
        _assetsFolderNode.setProperty(JcrConstants.JCR_NAME, "assets");
        this.assetsPath = _assetsFolderNode.getPath();


        // A folder to store deleted items in.
        Node _trashFolderNode = JcrUtils.getOrAddNode(_filesNode, "trash", JcrConstants.NT_UNSTRUCTURED);
        // Add Read/Write access to the trash for everyone in the family group
        AccessControlUtils.addAccessControlEntry(session, _filesNode.getPath(), familyGroup.getPrincipal(), new String[]{Privilege.JCR_READ, Privilege.JCR_WRITE, Privilege.JCR_ADD_CHILD_NODES}, true);

        _trashFolderNode.addMixin("mix:created");
        _trashFolderNode.addMixin("dam:systemfolder");
        _trashFolderNode.addMixin("dam:extensible");
        _trashFolderNode.setProperty(JcrConstants.JCR_NAME, "trash");
        this.trashPath = _trashFolderNode.getPath();


        // Persistent store for Observable file events
        Node _jobQueueFolderNode = JcrUtils.getOrAddNode(_filesNode, "job-queue", JcrConstants.NT_UNSTRUCTURED);
        _jobQueueFolderNode.addMixin("mix:created");
        _jobQueueFolderNode.addMixin("dam:systemfolder");
        _jobQueueFolderNode.addMixin("dam:extensible");
        _jobQueueFolderNode.setProperty(JcrConstants.JCR_NAME, "job-queue");
        this.jobQueuePath = _jobQueueFolderNode.getPath();
        return _filesNode;
    }


    /**
     * Create the root folder for all user uploaded DAM:FILES
     * @param _rootNode
     * @param session
     * @return
     * @throws RepositoryException
     */
    private Node createDAMFilesFolder(Node _rootNode, Session session) throws RepositoryException
    {
        UserManager userManager = ((JackrabbitSession) session).getUserManager();
        Group familyAdminGroup = (Group)userManager.getAuthorizable(FamilyDAMConstants.FAMILY_ADMIN_GROUP);
        Group familyGroup = (Group)userManager.getAuthorizable(FamilyDAMConstants.FAMILY_GROUP);


        Node _filesNode = JcrUtils.getOrAddNode(_rootNode, "dam:files", JcrConstants.NT_FOLDER);
        AccessControlUtils.addAccessControlEntry(session, _filesNode.getPath(), familyAdminGroup.getPrincipal(), new String[]{Privilege.JCR_ALL}, true);
        AccessControlUtils.addAccessControlEntry(session, _filesNode.getPath(), familyGroup.getPrincipal(), new String[]{Privilege.JCR_READ}, true);


        _filesNode.addMixin("mix:created");
        _filesNode.addMixin("dam:contentfolder");
        _filesNode.addMixin("dam:extensible");
        _filesNode.setProperty(JcrConstants.JCR_NAME, "Files");
        _filesNode.setProperty("order", "1");
        return _filesNode;
    }


    /**
     * Create the root folder for all of the user sync'd cloud services
     * @param _rootNode
     * @param session
     * @return
     * @throws RepositoryException
     */
    private Node createDAMCloudFolder(Node _rootNode, Session session) throws RepositoryException
    {
        UserManager userManager = ((JackrabbitSession) session).getUserManager();
        Group familyAdminGroup = (Group)userManager.getAuthorizable(FamilyDAMConstants.FAMILY_ADMIN_GROUP);
        Group familyGroup = (Group)userManager.getAuthorizable(FamilyDAMConstants.FAMILY_GROUP);

        Node _filesNode = JcrUtils.getOrAddNode(_rootNode, "dam:cloud", JcrConstants.NT_FOLDER);
        AccessControlUtils.addAccessControlEntry(session, _filesNode.getPath(), familyAdminGroup.getPrincipal(), new String[]{Privilege.JCR_ALL}, true);
        AccessControlUtils.addAccessControlEntry(session, _filesNode.getPath(), familyGroup.getPrincipal(), new String[]{Privilege.JCR_READ}, true);


        _filesNode.addMixin("mix:created");
        _filesNode.addMixin("dam:contentfolder");
        _filesNode.addMixin("dam:extensible");
        _filesNode.setProperty(JcrConstants.JCR_NAME, "Cloud");
        _filesNode.setProperty("order", "2");
        return _filesNode;
    }
    // TODO: create a dam:email with the same logic/permissions as the dam:cloud folder
    // TODO: create a dam:web with the same logic/permissions as the dam:cloud folder




    /**
     * On startup make sure that every dam:contentfolder node as the right child folders for every user in the Family Group
     * @param repository
     */
    private void createCustomUserFolders(Repository repository)
    {
        Session session = null;
        try {
            SimpleCredentials credentials = new SimpleCredentials(FamilyDAM.adminUserId, FamilyDAM.adminPassword.toCharArray());
            session = repository.login(credentials);

            UserManager userManager = ((JackrabbitSession) session).getUserManager();
            Group familyGroup = (Group)userManager.getAuthorizable(FamilyDAMConstants.FAMILY_GROUP);
            Iterator<Authorizable> familyMembers = familyGroup.getMembers();



            QueryManager queryManager = session.getWorkspace().getQueryManager();
            //Query query = queryManager.createQuery(sql, "JCR-SQL2");

            // find all DAM Content Folders, we'll add a user folder to each one
            Query query = queryManager.createQuery("SELECT * FROM [dam:contentfolder] AS s", "sql");

            // Execute the query and get the results ...
            QueryResult result = query.execute();

            javax.jcr.NodeIterator nodeItr = result.getNodes();
            while ( nodeItr.hasNext() ) {
                javax.jcr.Node node = nodeItr.nextNode();

                if( !node.getPath().equals("/") ) {
                    while( familyMembers.hasNext() ){
                        Authorizable user = familyMembers.next();

                        if( !user.getID().equals(FamilyDAM.adminUserId) && !user.getID().equals("anonymous") ) {
                            Node _node = JcrUtils.getOrAddFolder(node, user.getID());
                            _node.addMixin("mix:created");
                            _node.addMixin("dam:userfolder");
                            _node.addMixin("dam:extensible");
                            _node.setProperty(JcrConstants.JCR_NAME, user.getID());

                            session.save();

                            // Everyone in the group gets read only access
                            AccessControlUtils.addAccessControlEntry(session, _node.getPath(), familyGroup.getPrincipal(), new String[]{Privilege.JCR_READ}, true);
                            // this user gets "ALL" access
                            AccessControlUtils.addAccessControlEntry(session, _node.getPath(), user.getPrincipal(), new String[]{Privilege.JCR_ALL}, true);
                        }


                    }
                }
            }

            session.save();
        }catch(Exception ex){
            ex.printStackTrace();
        }finally {
            if( session != null) session.logout();
        }
    }


}
