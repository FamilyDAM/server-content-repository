package com.familydam.core;

import com.familydam.core.plugins.ImageNodeObserver;
import com.familydam.core.plugins.InitialDAMContent;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.api.ContentRepository;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.jackrabbit.oak.plugins.segment.SegmentNodeStore;
import org.apache.jackrabbit.oak.plugins.segment.file.FileStore;
import org.apache.jackrabbit.oak.security.SecurityProviderImpl;
import org.apache.jackrabbit.oak.spi.blob.FileBlobStore;
import org.apache.jackrabbit.oak.spi.state.NodeStore;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.embedded.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

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
                    .with("default")
                    .with(new InitialDAMContent())        // add initial content and folder structure
                            //.with(new DefaultTypeEditor())     // automatically set default types
                            //.with(new NameValidatorProvider()) // allow only valid JCR names
                            //.with(new OpenSecurityProvider())
                    .with(new SecurityProviderImpl())  // use the default security
                            //.with(new PropertyIndexHook())     // simple indexing support
                            //.with(new PropertyIndexProvider()) // search support for the indexes
                    .with(new ImageNodeObserver());
            //.with(new CommitDAMHook())
            return oak;
        }
        catch (IOException ex) {
            ex.printStackTrace(); //todo handle this.
            throw new RuntimeException(ex);
        }
    }


    @Bean
    public Jcr jcr(){
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(3);
        Jcr jcr = new Jcr(oak()).with(executor);
        return jcr;
    }


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
    }


    /**
    @Bean
    public ServletRegistrationBean oakServlet()
    {
        OakServlet servlet = new OakServlet(contentRepository());
        ServletRegistrationBean bean = new ServletRegistrationBean(servlet, "/webdav/*");
        return bean;
    }


    @Bean
    public ServletRegistrationBean webDavServlet()
    {
        SimpleWebdavServlet webdavServlet = new AbstractWebdavServlet();

        ServletRegistrationBean bean = new ServletRegistrationBean(servlet, "/webdav/*");
        bean.setInitParameters();
    }
    **/



    //@see https://apache.googlesource.com/jackrabbit-oak/+/173e8c17a26fb7479b7139cf8c477c41278ef636/oak-run/src/main/java/org/apache/jackrabbit/oak/run/Main.java
    // @see http://stackoverflow.com/questions/20915528/how-can-i-register-a-secondary-servlet-with-spring-boot
    /*** reference from oak-run project
    protected void initializeOakServlets(Oak oak, ContentRepository contentRepository)
    {
        // start up webdav

        context.addServlet(holder, path + "/*");
        final Repository jcrRepository = jcr.createRepository();
        ServletHolder webdav =
                new ServletHolder(new SimpleWebdavServlet() {
                    @Override
                    public Repository getRepository() {
                        return jcrRepository;
                    }
                });
        webdav.setInitParameter( SimpleWebdavServlet.INIT_PARAM_RESOURCE_PATH_PREFIX, path + "/webdav");
        webdav.setInitParameter( AbstractWebdavServlet.INIT_PARAM_AUTHENTICATE_HEADER, "Basic realm=\"Oak\"");
        context.addServlet(webdav, path + "/webdav/*");
        ServletHolder davex =
                new ServletHolder(new JCRWebdavServerServlet() {
                    @Override
                    protected Repository getRepository() {
                        return jcrRepository;
                    }
                });
        davex.setInitParameter(
                JCRWebdavServerServlet.INIT_PARAM_RESOURCE_PATH_PREFIX,
                path + "/davex");
        webdav.setInitParameter(
                AbstractWebdavServlet.INIT_PARAM_AUTHENTICATE_HEADER,
                "Basic realm=\"Oak\"");
        context.addServlet(davex, path + "/davex/*");
    }
    **/


    @Bean MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize("1000MB");
        factory.setMaxRequestSize("1000MB");
        return factory.createMultipartConfig();
    }
}
