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

import com.familydam.core.plugins.CommitDAMHook;
import com.familydam.core.plugins.InitialDAMContent;
import org.apache.jackrabbit.commons.cnd.CompactNodeTypeDefReader;
import org.apache.jackrabbit.commons.cnd.DefinitionBuilderFactory;
import org.apache.jackrabbit.commons.cnd.TemplateBuilderFactory;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.jackrabbit.oak.plugins.segment.SegmentNodeStore;
import org.apache.jackrabbit.oak.plugins.segment.file.FileStore;
import org.apache.jackrabbit.oak.spi.blob.FileBlobStore;
import org.apache.jackrabbit.oak.spi.state.NodeStore;
import org.apache.jackrabbit.webdav.jcr.JCRWebdavServerServlet;
import org.apache.jackrabbit.webdav.server.AbstractWebdavServlet;
import org.apache.jackrabbit.webdav.simple.SimpleWebdavServlet;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
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
                    //.with(new BackgroundObserver(imageRenditionObserver, observerExecutor))
                    //.with(new BackgroundObserver(imageExifObserver, observerExecutor))
                    //.with(new BackgroundObserver(imagePHashObserver, observerExecutor))
                    .withAsyncIndexing();


            // Create repository
            Repository repository = jcr.createRepository();

            // Using the CND file, make sure all of the required mix-ins have been created.
            registerCustomNodeTypes(repository);

            // Add Session
            // imageRenditionObserver.setRepository(repository);
            //imageExifObserver.setRepository(repository);
            //imagePHashObserver.setRepository(repository);

            return repository;
        }
        catch (Exception ex) {
            ex.printStackTrace(); //todo handle this.
            throw new RuntimeException(ex);
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


    /****
     @Bean public ContentRepository contentRepository()
     {
     try {
     ContentRepository repository = oak().createContentRepository();
     return repository;
     }
     catch (Exception ex) {
     ex.printStackTrace(); //todo handle this.
     throw new RuntimeException(ex);
     }
     }*****/


    /**
     @Bean public ServletRegistrationBean oakServlet()
     {
     OakServlet servlet = new OakServlet(contentRepository());
     ServletRegistrationBean bean = new ServletRegistrationBean(servlet, "/oak/*");
     return bean;
     }
     **/
}
