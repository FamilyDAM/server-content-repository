/*
 * Copyright (c) 2015  Mike Nimer & 11:58 Labs
 */

package com.familydam.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.io.IOException;

@EnableScheduling
@SpringBootApplication
public class FamilyDAM extends WebMvcConfigurerAdapter
{
    public static String mode = "desktop";
    public static String adminUserId = "admin";
    public static String adminPassword = "admin";



    public static void main(String[] args) throws IOException
    {
        ConfigurableApplicationContext context = SpringApplication.run(FamilyDAM.class, args);
    }


/**
    @Bean
    public CommonsMultipartResolver multipartResolver() {
        CommonsMultipartResolver resolver=new CommonsMultipartResolver();
        resolver.setDefaultEncoding("utf-8");
        return resolver;
    }


    @Bean
    MultipartConfigElement multipartConfigElement() {

        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize("500MB");
        factory.setMaxRequestSize("500MB");
        return factory.createMultipartConfig();
    }
    **/

}
