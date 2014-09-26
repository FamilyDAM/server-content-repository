package com.familydam.core;

import com.familydam.core.plugins.ImageNodeObserver;
import com.familydam.core.plugins.InitialDAMContent;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.api.ContentRepository;
import org.apache.jackrabbit.oak.plugins.segment.SegmentNodeStore;
import org.apache.jackrabbit.oak.plugins.segment.file.FileStore;
import org.apache.jackrabbit.oak.security.SecurityProviderImpl;
import org.apache.jackrabbit.oak.spi.blob.FileBlobStore;
import org.apache.jackrabbit.oak.spi.state.NodeStore;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

import java.io.File;
import java.io.IOException;

@Configuration
@ComponentScan
@EnableAutoConfiguration
public class Application
{
    public static String adminUserId = "admin";
    public static String adminPassword = "admin";


    public static void main(String[] args)
    {
        SpringApplication.run(Application.class, args);
    }


    @Bean
    public ContentRepository contentRepository()
    {
        try {
            File repoDir = new File("./repository");

            FileBlobStore fileBlobStore = new FileBlobStore(repoDir.getAbsolutePath());
            FileStore source = new FileStore(fileBlobStore, repoDir, 100, true);
            NodeStore segmentNodeStore = new SegmentNodeStore(source);

            ContentRepository repository = new Oak(segmentNodeStore)
                    .with("default")
                    .with(new InitialDAMContent())        // add initial content and folder structure
                            //.with(new DefaultTypeEditor())     // automatically set default types
                    //.with(new NameValidatorProvider()) // allow only valid JCR names
                    //.with(new OpenSecurityProvider())
                    .with(new SecurityProviderImpl())  // use the default security
                            //.with(new PropertyIndexHook())     // simple indexing support
                    //.with(new PropertyIndexProvider()) // search support for the indexes
                    .with(new ImageNodeObserver())
                    //.with(new CommitDAMHook())
                    .createContentRepository();

            return repository;
        }
        catch (IOException ex) {
            ex.printStackTrace(); //todo handle this.
            throw new RuntimeException(ex);
        }


    }


    @Bean
    public CommonsMultipartResolver multipartResolver(){
        CommonsMultipartResolver resolver = new CommonsMultipartResolver();
        resolver.setMaxUploadSize(100000000);
        return resolver;
    }
}
