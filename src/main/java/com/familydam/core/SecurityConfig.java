/*
 * Copyright (c) 2015  Mike Nimer & 11:58 Labs
 */

package com.familydam.core;

import com.familydam.core.security.CustomAuthenticationProvider;
import com.familydam.core.security.TokenAuthFilter;
import com.familydam.core.security.TokenHandler;
import com.familydam.core.security.UserDetailServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.jcr.Repository;

/**
 * Created by mnimer on 1/26/15.
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter
{
    @Autowired private CustomAuthenticationProvider authenticationProvider;

    @Autowired private TokenHandler tokenHandler;

    @Autowired UserDetailServiceImpl userDetailsService;
    @Autowired Repository repository;

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                //.antMatchers(actuatorEndpoints()).hasRole(backendAdminRole)
                // Allow anonymous resource requests
                .antMatchers("/").permitAll()
                .antMatchers("/**").permitAll()
                .antMatchers("/favicon.ico").permitAll()
                .antMatchers("/public/**").permitAll()
                .antMatchers("/static/**").permitAll()
                .antMatchers("/api/users/**").permitAll()
                .antMatchers("/api/~/**").permitAll()
                .antMatchers("/api/**").authenticated();

        http.addFilterBefore(new TokenAuthFilter(tokenHandler, userDetailsService, repository, null), BasicAuthenticationFilter.class);

    }



    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth, UserDetailsService userDetailService) throws Exception
    {
        auth
                .authenticationProvider(authenticationProvider)
                .userDetailsService(userDetailService)
                .passwordEncoder(new BCryptPasswordEncoder());

    }

    @Bean
    public AuthenticationEntryPoint unauthorizedEntryPoint() {
        return null;
        //return (request, response, authException) => response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }
}
