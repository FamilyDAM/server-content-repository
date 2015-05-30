/*
 * This file is part of FamilyDAM Project.
 *
 *     The FamilyDAM Project is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     The FamilyDAM Project is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with the FamilyDAM Project.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.familydam.core;

import com.familydam.core.observers.FileNodeObserver;
import com.familydam.core.plugins.CommitDAMHook;
import com.familydam.core.plugins.InitialDAMContent;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.commons.cnd.CompactNodeTypeDefReader;
import org.apache.jackrabbit.commons.cnd.DefinitionBuilderFactory;
import org.apache.jackrabbit.commons.cnd.TemplateBuilderFactory;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.jackrabbit.oak.plugins.segment.SegmentNodeStore;
import org.apache.jackrabbit.oak.plugins.segment.file.FileStore;
import org.apache.jackrabbit.oak.security.authentication.token.TokenLoginModule;
import org.apache.jackrabbit.oak.security.authentication.user.LoginModuleImpl;
import org.apache.jackrabbit.oak.spi.blob.FileBlobStore;
import org.apache.jackrabbit.oak.spi.commit.BackgroundObserver;
import org.apache.jackrabbit.oak.spi.security.authentication.token.TokenProvider;
import org.apache.jackrabbit.oak.spi.state.NodeStore;
import org.apache.jackrabbit.webdav.jcr.JCRWebdavServerServlet;
import org.apache.jackrabbit.webdav.server.AbstractWebdavServlet;
import org.apache.jackrabbit.webdav.simple.SimpleWebdavServlet;
import org.jetbrains.annotations.NotNull;
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
    //@Autowired() private ImageRenditionsService imageRenditionsService;

    @Bean
    public Repository jcrRepository()
    {
        try {
            ScheduledExecutorService executor = Executors.newScheduledThreadPool(3);
            ScheduledExecutorService observerExecutor = Executors.newScheduledThreadPool(10);

            // create Observers
            // create thumbnails
            //ImageRenditionsObserver imageRenditionObserver = new ImageRenditionsObserver("/dam");
            //imageRenditionObserver.setImageRenditionsService(imageRenditionsService);
            // parse out exif metadata
            //ImageExifObserver imageExifObserver = new ImageExifObserver("/dam");
            // generate a phash for each image (so we can find like photos & duplicates)
            //ImagePHashObserver imagePHashObserver = new ImagePHashObserver("/dam");

            // create JCR object
            Jcr jcr = new Jcr(getOak())
                    .with(executor)
                    .with(new BackgroundObserver(fileNodeObserver(), observerExecutor))
                    .withAsyncIndexing();

            // Create repository
            Repository repository = jcr.createRepository();

            // Using the CND file, make sure all of the required mix-ins have been created.
            registerCustomNodeTypes(repository);
            
            registerSystemDirectories(repository);
            registerCustomUsers(repository);

            // Add Session
            // imageRenditionObserver.setRepository(repository);
            //imageExifObserver.setRepository(repository);
            //imagePHashObserver.setRepository(repository);

            javax.security.auth.login.Configuration.setConfiguration(getConfiguration());



            return repository;
        }
        catch (Exception ex) {
            ex.printStackTrace(); //todo handle this.
            throw new RuntimeException(ex);
        }
    }



    private Oak getOak()
    {
        try {
            File repoDir = new File("./familydam-repo");
            ScheduledExecutorService observerExecutor = Executors.newScheduledThreadPool(10);

            FileBlobStore fileBlobStore = new FileBlobStore(repoDir.getAbsolutePath());
            FileStore source = new FileStore(fileBlobStore, repoDir, 100, true);
            //int maxFileSize = 1024 * 1024; //1gig
            //FileStore source = new FileStore(repoDir, maxFileSize, false);
            NodeStore segmentNodeStore = new SegmentNodeStore(source);

            Oak oak = new Oak(segmentNodeStore)
                    .with("familyDAM")
                    .with(new InitialDAMContent(segmentNodeStore))       // add initial content and folder structure
                            //.with(new SecurityProviderImpl())  // use the default security
                            //.with(new DefaultTypeEditor())     // automatically set default types
                            //.with(new NameValidatorProvider()) // allow only valid JCR names
                            //.with(new OpenSecurityProvider())
                            //.with(new PropertyIndexHook())     // simple indexing support
                            //.with(new PropertyIndexProvider()) // search support for the indexes
                    .with(new CommitDAMHook())
                    .withAsyncIndexing();


            return oak;
        }
        catch (IOException ex) {
            ex.printStackTrace(); //todo handle this.
            throw new RuntimeException(ex);
        }
    }





    @Bean
    public FileNodeObserver fileNodeObserver()
    {
        return new FileNodeObserver("/dam:files/", JcrConstants.NT_FILE);
    }



    /****
    @Bean
    public SecurityProvider securityProvider()
    {
        return new SecurityProviderImpl(ConfigurationParameters.EMPTY);
    }
    ***/


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
            session = repository.login(new SimpleCredentials(FamilyDAM.adminUserId, FamilyDAM.adminPassword.toCharArray()));

            Node _rootNode = session.getRootNode();
            Node _systemNode = createDAMSystemFolder(_rootNode, session);
            Node _filesNode = createDAMFilesFolder(_rootNode);
            Node _cloudNode = createDAMCloudFolder(_rootNode);

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

        Node _jobQueueFolderNode = JcrUtils.getOrAddNode(_filesNode, "job-queue", JcrConstants.NT_UNSTRUCTURED);
        _jobQueueFolderNode.addMixin("mix:created");
        _jobQueueFolderNode.addMixin("dam:systemfolder");
        _jobQueueFolderNode.addMixin("dam:extensible");
        _jobQueueFolderNode.setProperty(JcrConstants.JCR_NAME, "job-queue");
        return _filesNode;
    }


    @NotNull private Node createDAMFilesFolder(Node _rootNode) throws RepositoryException
    {
        Node _filesNode = JcrUtils.getOrAddNode(_rootNode, "dam:files", JcrConstants.NT_FOLDER);
        Node _documentsFolderNode = JcrUtils.getOrAddFolder(_filesNode, "documents");
        _documentsFolderNode.addMixin("mix:created");
        _documentsFolderNode.addMixin("dam:contentfolder");
        _documentsFolderNode.addMixin("dam:extensible");
        _documentsFolderNode.setProperty(JcrConstants.JCR_NAME, "Documents");
        _documentsFolderNode.setProperty("order", "1");
        return _filesNode;
    }


    @NotNull private Node createDAMCloudFolder(Node _rootNode) throws RepositoryException
    {
        Node _filesNode = JcrUtils.getOrAddNode(_rootNode, "dam:cloud", JcrConstants.NT_FOLDER);
        Node _documentsFolderNode = JcrUtils.getOrAddFolder(_filesNode, "cloud");
        _documentsFolderNode.addMixin("mix:created");
        _documentsFolderNode.addMixin("dam:contentfolder");
        _documentsFolderNode.addMixin("dam:extensible");
        _documentsFolderNode.setProperty(JcrConstants.JCR_NAME, "Cloud");
        _documentsFolderNode.setProperty("order", "2");
        return _filesNode;
    }









    private void registerCustomUsers(Repository repository)
    {
        String[] users = new String[]{"admin", "animer"};

        Session session = null;
        try {
            session = repository.login(new SimpleCredentials(FamilyDAM.adminUserId, FamilyDAM.adminPassword.toCharArray()));

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
                    for (String user : users) {
                        Node _node = JcrUtils.getOrAddFolder(node, user);
                        _node.addMixin("mix:created");
                        _node.addMixin("dam:systemfolder");
                        _node.addMixin("dam:extensible");
                        _node.setProperty(JcrConstants.JCR_NAME, user);
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

        ServletRegistrationBean bean = new ServletRegistrationBean(servlet, "/webdav/*");

        bean.addInitParameter(SimpleWebdavServlet.INIT_PARAM_RESOURCE_PATH_PREFIX, "/webdav");
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
