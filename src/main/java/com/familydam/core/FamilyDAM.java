/*
 * Copyright (c) 2015  Mike Nimer & 11:58 Labs
 */

package com.familydam.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import javax.servlet.MultipartConfigElement;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.UUID;

@SpringBootApplication
@EnableScheduling
public class FamilyDAM extends WebMvcConfigurerAdapter
{
    public static String mode = "desktop";
    public static String adminUserId = "admin";
    public static String adminPassword = "admin";


    public static void main(String[] args)
    {
        setupAdminUser();

        SpringApplication.run(FamilyDAM.class, args);
    }


    private static void setupAdminUser()
    {
        String newAdminPwd = UUID.randomUUID().toString();


        try {
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());

            // get user password and file input stream
            char[] password = newAdminPwd.toCharArray();
            ks.load(null, password);

            // save my secret key
            //javax.crypto.SecretKey mySecretKey = new Sec;
            //KeyStore.SecretKeyEntry skEntry = new KeyStore.SecretKeyEntry(mySecretKey);
            //ks.setEntry("secretKeyAlias", skEntry, password);


        }
        catch (KeyStoreException e) {
            e.printStackTrace();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        catch (CertificateException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Bean
    MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        //factory.setMaxFileSize("5000MB");
        //factory.setMaxRequestSize("5000MB");
        return factory.createMultipartConfig();
    }
}
