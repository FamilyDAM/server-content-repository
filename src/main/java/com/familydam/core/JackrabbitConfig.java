/*
 * Copyright (c) 2015  Mike Nimer & 11:58 Labs
 */

package com.familydam.core;

import com.familydam.core.observers.FileNodeObserver;
import com.familydam.core.plugins.CommitDAMHook;
import com.familydam.core.plugins.InitialDAMContent;
import com.familydam.core.security.SecurityProvider;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.QueryBuilder;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.commons.cnd.CompactNodeTypeDefReader;
import org.apache.jackrabbit.commons.cnd.DefinitionBuilderFactory;
import org.apache.jackrabbit.commons.cnd.TemplateBuilderFactory;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.api.ContentRepository;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.jackrabbit.oak.plugins.segment.SegmentNodeStore;
import org.apache.jackrabbit.oak.plugins.segment.file.FileStore;
import org.apache.jackrabbit.oak.security.authentication.token.TokenLoginModule;
import org.apache.jackrabbit.oak.security.authentication.user.LoginModuleImpl;
import org.apache.jackrabbit.oak.spi.blob.FileBlobStore;
import org.apache.jackrabbit.oak.spi.commit.BackgroundObserver;
import org.apache.jackrabbit.oak.spi.security.ConfigurationParameters;
import org.apache.jackrabbit.oak.spi.security.authentication.token.TokenProvider;
import org.apache.jackrabbit.oak.spi.state.NodeStore;
import org.apache.jackrabbit.value.StringValue;
import org.apache.jackrabbit.webdav.jcr.JCRWebdavServerServlet;
import org.apache.jackrabbit.webdav.server.AbstractWebdavServlet;
import org.apache.jackrabbit.webdav.simple.SimpleWebdavServlet;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
import javax.security.auth.login.AppConfigurationEntry;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by mnimer on 12/11/14.
 */
@Configuration
public class JackrabbitConfig
{

    @Value("${jcr.threads}")
    public Integer jcrThreads = 3;

    @Value("${jcr.observer.threads}")
    public Integer jcrObserverThreads = 2;

    public static String trashPath = "/" +FamilyDAMConstants.SYSTEM_ROOT;
    public static String assetsPath = "/" +FamilyDAMConstants.SYSTEM_ROOT;
    public static String jobQueuePath = "/" +FamilyDAMConstants.SYSTEM_ROOT;

    Repository repository = null;
    ContentRepository contentRepository = null;

    @Bean
    public Repository jcrRepository()
    {
        if( repository != null ){
            return repository;
        }


        try {
            ScheduledExecutorService executor = Executors.newScheduledThreadPool(jcrThreads);
            ScheduledExecutorService observerExecutor = Executors.newScheduledThreadPool(jcrObserverThreads);



            // create JCR object
            Jcr jcr = new Jcr(getOak())
                    .with(executor)
                    .with(new BackgroundObserver(fileNodeObserver(), observerExecutor))
                    .withAsyncIndexing();

            // Create repository
            repository = jcr.createRepository();
            // get content repository that was previously created with the call above.
            contentRepository = jcr.createContentRepository();


            // Using the CND file, make sure all of the required mix-ins have been created.
            registerCustomNodeTypes(repository);

            // register the default system directories
            registerSystemDirectories(repository);

            // add users if the start up argument was passed in
            createCustomUserFolders(repository);

            javax.security.auth.login.Configuration.setConfiguration(getConfiguration());

            return repository;
        }
        catch (Exception ex) {
            ex.printStackTrace(); //todo handle this.
            throw new RuntimeException(ex);
        }
    }


    public ContentRepository getContentRepository()
    {
        return contentRepository;
    }



    private Oak getOak()
    {
        try {
            NodeStore segmentNodeStore = SegmentNodeStore.newSegmentNodeStore(fileStore()).create();

            Map props = new HashMap<>();
            props.put("PARAM_ADMIN_ID", "aaddmmiinn");
            props.put("PARAM_OMIT_ADMIN_PW", false);
            ConfigurationParameters _securityConfig = ConfigurationParameters.of(props);

            Oak oak = new Oak(segmentNodeStore)
                    .with("FamilyDAM")
                    .with(new InitialDAMContent(segmentNodeStore))       // add initial content and folder structure
                            //.with(securityProvider())  // use the default security
                            //.with(new DefaultTypeEditor())     // automatically set default types
                            //.with(new NameValidatorProvider()) // allow only valid JCR names
                            //.with(new OpenSecurityProvider())
                            //.with(new PropertyIndexHook())     // simple indexing support
                            //.with(new PropertyIndexProvider()) // search support for the indexes
                            .with(new SecurityProvider(_securityConfig))
                    .with(new CommitDAMHook());


            return oak;
        }
        catch (IOException ex) {
            ex.printStackTrace(); //todo handle this.
            throw new RuntimeException(ex);
        }
    }



    private FileStore fileStore() throws IOException
    {
        File repoDir = new File("./familydam-repo");
        File blobStoreDir = new File(repoDir.getPath() +"/blobstore");
        /**
         ScheduledExecutorService observerExecutor = Executors.newScheduledThreadPool(10);
         FileDataStore fileDataStore = new FileDataStore();
         fileDataStore.setMinRecordLength(100);
         fileDataStore.setPath(repoDir.getAbsolutePath());
         DataStoreBlobStore dataStoreBlobStore = new DataStoreBlobStore(fileDataStore);
         **/


        FileBlobStore fileBlobStore = new FileBlobStore(blobStoreDir.getAbsolutePath());
        //int maxFileSize = (1024 * 1024) * 10; //1gig
        //FileStore source = new FileStore(fileBlobStore, repoDir, 100, true);
        //FileStore source = new FileStore(repoDir, maxFileSize, false);

        FileStore source = FileStore
                .newFileStore(repoDir)
                .withBlobStore(fileBlobStore)
                .create();

        return source;
    }



    @Bean
    public FileNodeObserver fileNodeObserver()
    {
        return new FileNodeObserver("/dam:files/", JcrConstants.NT_FILE);
    }




    /**
    @Bean
    public SecurityProvider securityProvider()
    {
        return new com.familydam.core.security.SecurityProvider();
    } **/


    /**
     * Configure the sercurity provider and set token settings
     * @return
     */
    protected javax.security.auth.login.Configuration getConfiguration() {
        return new javax.security.auth.login.Configuration() {
            @Override
            public AppConfigurationEntry[] getAppConfigurationEntry(String s) {

                Map<String, Object> params = new HashMap<>();
                params.put(TokenProvider.PARAM_TOKEN_EXPIRATION, 86400000);//1day
                params.put(TokenProvider.PARAM_TOKEN_LENGTH, 1024);

                AppConfigurationEntry tokenEntry = new AppConfigurationEntry(
                        TokenLoginModule.class.getName(),
                        AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT,
                        params);

                AppConfigurationEntry defaultEntry = new AppConfigurationEntry(
                        LoginModuleImpl.class.getName(),
                        AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                        Collections.<String, Object>emptyMap());
                return new AppConfigurationEntry[] {tokenEntry, defaultEntry};
            }
        };
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
            Node _filesNode = createDAMFilesFolder(_rootNode);
            //Node _cloudNode = createDAMCloudFolder(_rootNode);

            session.save();
        }catch(Exception ex){
            ex.printStackTrace();
        }finally {
            if( session != null) session.logout();
        }
    }




    @NotNull private Node createDAMSystemFolder(Node _rootNode, Session session) throws RepositoryException
    {
        //Node _tmpNode = JcrUtils.getNodeIfExists(_rootNode, FamilyDAMConstants.SYSTEM_ROOT);
        //_tmpNode.remove();
        //session.save();

        Node _filesNode = JcrUtils.getOrAddNode(_rootNode, FamilyDAMConstants.SYSTEM_ROOT, JcrConstants.NT_UNSTRUCTURED);

        Node _assetsFolderNode = JcrUtils.getOrAddNode(_filesNode, "assets", JcrConstants.NT_UNSTRUCTURED);
        _assetsFolderNode.addMixin("mix:created");
        _assetsFolderNode.addMixin("dam:systemfolder");
        _assetsFolderNode.addMixin("dam:extensible");
        _assetsFolderNode.setProperty(JcrConstants.JCR_NAME, "assets");
        this.assetsPath = _assetsFolderNode.getPath();

        Node _trashFolderNode = JcrUtils.getOrAddNode(_filesNode, "trash", JcrConstants.NT_UNSTRUCTURED);
        _trashFolderNode.addMixin("mix:created");
        _trashFolderNode.addMixin("dam:systemfolder");
        _trashFolderNode.addMixin("dam:extensible");
        _trashFolderNode.setProperty(JcrConstants.JCR_NAME, "trash");
        this.trashPath = _trashFolderNode.getPath();

        Node _jobQueueFolderNode = JcrUtils.getOrAddNode(_filesNode, "job-queue", JcrConstants.NT_UNSTRUCTURED);
        _jobQueueFolderNode.addMixin("mix:created");
        _jobQueueFolderNode.addMixin("dam:systemfolder");
        _jobQueueFolderNode.addMixin("dam:extensible");
        _jobQueueFolderNode.setProperty(JcrConstants.JCR_NAME, "job-queue");
        this.jobQueuePath = _jobQueueFolderNode.getPath();
        return _filesNode;
    }


    @NotNull private Node createDAMFilesFolder(Node _rootNode) throws RepositoryException
    {
        Node _filesNode = JcrUtils.getOrAddNode(_rootNode, "dam:files", JcrConstants.NT_FOLDER);
        _filesNode.addMixin("mix:created");
        _filesNode.addMixin("dam:contentfolder");
        _filesNode.addMixin("dam:extensible");
        _filesNode.setProperty(JcrConstants.JCR_NAME, "Files");
        _filesNode.setProperty("order", "1");
        return _filesNode;
    }


    @NotNull private Node createDAMCloudFolder(Node _rootNode) throws RepositoryException
    {
        Node _filesNode = JcrUtils.getOrAddNode(_rootNode, "dam:cloud", JcrConstants.NT_FOLDER);
        _filesNode.addMixin("mix:created");
        _filesNode.addMixin("dam:contentfolder");
        _filesNode.addMixin("dam:extensible");
        _filesNode.setProperty(JcrConstants.JCR_NAME, "Cloud");
        _filesNode.setProperty("order", "2");
        return _filesNode;
    }





    /**
     * Create the initial users accounts and folder - defined in the startup wizard.
     * And create the new ADMIN user.
     * @param repository
     */
    private void createCustomUserFolders(Repository repository)
    {
        Session session = null;
        try {
            SimpleCredentials credentials = new SimpleCredentials(FamilyDAM.adminUserId, FamilyDAM.adminPassword.toCharArray());
            session = repository.login(credentials);

            UserManager userManager = ((JackrabbitSession) session).getUserManager();

            final javax.jcr.Value anonymousValue = session.getValueFactory().createValue("anonymous");
            final javax.jcr.Value adminValue = session.getValueFactory().createValue("admin");

            Iterator<Authorizable> users = userManager.findAuthorizables(new org.apache.jackrabbit.api.security.user.Query()
            {
                @Override public <T> void build(QueryBuilder<T> builder)
                {
                    builder.setCondition(builder.and( builder.neq("rep:principalName", new StringValue("admin")) , builder.neq("rep:principalName", new StringValue("anonymous")) ) );
                    builder.setSortOrder("@rep:principalName", QueryBuilder.Direction.ASCENDING);
                    builder.setSelector(User.class);
                }
            });


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
                    while( users.hasNext() ){
                        Authorizable user = users.next();

                        if( !user.getID().equals("admin") && !user.getID().equals("anonymous") ) {
                            Node _node = JcrUtils.getOrAddFolder(node, user.getID());
                            _node.addMixin("mix:created");
                            _node.addMixin("dam:userfolder");
                            _node.addMixin("dam:extensible");
                            _node.setProperty(JcrConstants.JCR_NAME, user.getID());
                        }

                        session.save();
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






    @Bean
    public ServletRegistrationBean webDavServlet()
    {
        SimpleWebdavServlet servlet = new SimpleWebdavServlet()
        {
            @Override
            public Repository getRepository()
            {
                return jcrRepository();
            }
        };

        ServletRegistrationBean bean = new ServletRegistrationBean(servlet, "/drive/*");

        bean.addInitParameter(SimpleWebdavServlet.INIT_PARAM_RESOURCE_PATH_PREFIX, "/drive");
        //bean.addInitParameter(AbstractWebdavServlet.INIT_PARAM_AUTHENTICATE_HEADER, "Basic realm=\"Oak\"");

        return bean;
    }


    @Bean
    public ServletRegistrationBean davexServlet()
    {
        JCRWebdavServerServlet servlet = new JCRWebdavServerServlet()
        {
            @Override
            protected Repository getRepository()
            {
                return jcrRepository();
            }
        };

        ServletRegistrationBean bean = new ServletRegistrationBean(servlet, "/davex/*");


        bean.addInitParameter(
                JCRWebdavServerServlet.INIT_PARAM_RESOURCE_PATH_PREFIX,
                "/davex");
        bean.addInitParameter(
                AbstractWebdavServlet.INIT_PARAM_AUTHENTICATE_HEADER,
                "Basic realm=\"Oak\"");

        return bean;
    }


}
