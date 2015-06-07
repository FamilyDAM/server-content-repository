/*
 * Copyright (c) 2015  Mike Nimer & 11:58 Labs
 */

package com.familydam.core.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import javax.jcr.Credentials;
import java.util.Collection;

/**
 * Created by mnimer on 3/19/15.
 */
public class UserAuthentication implements Authentication
{
    private final User user;
    private boolean authenticated = true;

    private Credentials credentials;


    public UserAuthentication(CustomUserDetails user)
    {
        this.user = user;
    }


    public User getUser()
    {
        return user;
    }


    @Override
    public String getName()
    {
        return user.getPrincipalName();
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities()
    {
        return user.getAuthorities();
    }


    @Override public Credentials getCredentials()
    {
        return credentials;
    }


    public void setCredentials(Credentials credentials)
    {
        this.credentials = credentials;
    }


    @Override
    public User getDetails()
    {
        return user;
    }


    @Override
    public Object getPrincipal()
    {
        return user.getPrincipalName();
    }


    @Override
    public boolean isAuthenticated()
    {
        return authenticated;
    }


    @Override
    public void setAuthenticated(boolean authenticated)
    {
        this.authenticated = authenticated;
    }

}
