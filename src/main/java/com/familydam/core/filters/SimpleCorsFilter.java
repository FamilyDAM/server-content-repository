/*
 * Copyright (c) 2015  Mike Nimer & 11:58 Labs
 */

package com.familydam.core.filters;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by mnimer on 9/27/14.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SimpleCorsFilter extends OncePerRequestFilter
{


    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException
    {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");//HEAD, TRACE, PATCH
        response.setHeader("Access-Control-Allow-Headers", "origin,x-auth-token,x-auth-token-refresh,x-requested-with, x-csrf-token, content-type, accept, authentication, authorization");
        response.setHeader("Access-Control-Expose-Headers", "origin,x-auth-token,x-auth-token-refresh,x-requested-with, x-csrf-token, content-type, accept, authentication, authorization");
        response.setHeader("Access-Control-Max-Age", "3600");


        if ( !request.getMethod().equalsIgnoreCase("OPTIONS") ) {
            chain.doFilter(request, response);
        }
    }


}
