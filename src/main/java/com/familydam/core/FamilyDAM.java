package com.familydam.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;

import javax.servlet.MultipartConfigElement;

@SpringBootApplication
public class FamilyDAM
{
    public static String adminUserId = "admin";
    public static String adminPassword = "admin";


    public static void main(String[] args)
    {
        SpringApplication.run(FamilyDAM.class, args);
    }



    @Bean
    MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize("2000MB");
        factory.setMaxRequestSize("2000MB");
        return factory.createMultipartConfig();
    }
}
