package com.familydam.core;

import com.familydam.core.observers.ImageNodeObserver;
import com.familydam.core.plugins.CommitDAMHook;
import com.familydam.core.plugins.InitialDAMContent;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.jackrabbit.oak.plugins.segment.SegmentNodeStore;
import org.apache.jackrabbit.oak.plugins.segment.file.FileStore;
import org.apache.jackrabbit.oak.spi.blob.FileBlobStore;
import org.apache.jackrabbit.oak.spi.commit.BackgroundObserver;
import org.apache.jackrabbit.oak.spi.commit.Observer;
import org.apache.jackrabbit.oak.spi.state.NodeStore;
import org.apache.jackrabbit.webdav.jcr.JCRWebdavServerServlet;
import org.apache.jackrabbit.webdav.server.AbstractWebdavServlet;
import org.apache.jackrabbit.webdav.simple.SimpleWebdavServlet;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.embedded.MultipartConfigFactory;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import javax.jcr.Repository;
import javax.servlet.MultipartConfigElement;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
@ComponentScan
@EnableAutoConfiguration
public class FamilyDAM
{
    public static String adminUserId = "admin";
    public static String adminPassword = "admin";


    public static void main(String[] args)
    {
        SpringApplication.run(FamilyDAM.class, args);
    }



    @Bean
    public Oak oak(){
        try {
            File repoDir = new File("./repository");

            FileBlobStore fileBlobStore = new FileBlobStore(repoDir.getAbsolutePath());
            FileStore source = new FileStore(fileBlobStore, repoDir, 100, true);
            NodeStore segmentNodeStore = new SegmentNodeStore(source);

            Oak oak = new Oak(segmentNodeStore)
                    .with("familyDAM")
                    .with(new InitialDAMContent())       // add initial content and folder structure
                    //.with(new SecurityProviderImpl())  // use the default security
                            //.with(new DefaultTypeEditor())     // automatically set default types
                            //.with(new NameValidatorProvider()) // allow only valid JCR names
                            //.with(new OpenSecurityProvider())
                            //.with(new PropertyIndexHook())     // simple indexing support
                            //.with(new PropertyIndexProvider()) // search support for the indexes
                    //.with(new ImageNodeObserver())
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
    public Jcr jcr()
        {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(3);
        ScheduledExecutorService observerExecutor = Executors.newScheduledThreadPool(3);

        Observer imageObserver = new BackgroundObserver(new ImageNodeObserver("/dam"), observerExecutor);

        Jcr jcr = new Jcr(oak())
                .with(executor);
                //.with(new BackgroundObserver(new ImageNodeObserver(), observerExecutor));
        return jcr;
    }


    @Bean
    public Repository jcrRepository()
    {
        try {
            Repository repository = jcr().createRepository();
            return repository;
        }
        catch (Exception ex) {
            ex.printStackTrace(); //todo handle this.
            throw new RuntimeException(ex);
        }
    }


    /****
    @Bean
    public ContentRepository contentRepository()
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
    @Bean
    public ServletRegistrationBean oakServlet()
    {
        OakServlet servlet = new OakServlet(contentRepository());
        ServletRegistrationBean bean = new ServletRegistrationBean(servlet, "/oak/*");
        return bean;
    }
     **/


    @Bean
    public ServletRegistrationBean webDavServlet()
    {
        SimpleWebdavServlet servlet = new SimpleWebdavServlet() {
            @Override
            public Repository getRepository() {
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
        JCRWebdavServerServlet servlet = new JCRWebdavServerServlet() {
                    @Override
                    protected Repository getRepository() {
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



    @Bean
    MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize("1000MB");
        factory.setMaxRequestSize("1000MB");
        return factory.createMultipartConfig();
    }
}
