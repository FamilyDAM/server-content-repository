package com.familydam.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.embedded.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import javax.servlet.MultipartConfigElement;

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
    MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize("1000MB");
        factory.setMaxRequestSize("1000MB");
        return factory.createMultipartConfig();
    }
}
