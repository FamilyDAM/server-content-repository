/*
 * Copyright (c) 2015  Mike Nimer & 11:58 Labs
 */

package com.familydam.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.MultipartConfigFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import javax.servlet.MultipartConfigElement;
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



    @Bean
    MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        //factory.setMaxFileSize("5000MB");
        //factory.setMaxRequestSize("5000MB");
        return factory.createMultipartConfig();
    }
}
