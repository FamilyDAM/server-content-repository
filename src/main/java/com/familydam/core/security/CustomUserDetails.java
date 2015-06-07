/*
 * Copyright (c) 2015  Mike Nimer & 11:58 Labs
 */

package com.familydam.core.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.jcr.Credentials;
import javax.jcr.Session;
import java.util.Collection;

/**
 * Created by mnimer on 1/26/15.
 */
public class CustomUserDetails extends User implements UserDetails
{
    private Credentials credentials;
    private Session session = null;


    public Credentials getCredentials()
    {
        return credentials;
    }


    public void setCredentials(Credentials credentials)
    {
        this.credentials = credentials;
    }


    @Override public Collection<? extends GrantedAuthority> getAuthorities(){
        return super.getAuthorities();
    }

    @Override public String getUsername(){
        return super.getPrincipalName();
    }

    @Override public String getPassword() { return super.getPassword(); }

    @Override public boolean isEnabled(){
        return true;
    }

    @Override public boolean isAccountNonExpired()
    {
        return true;
    }

    @Override public boolean isAccountNonLocked()
    {
        return true;
    }

    @Override public boolean isCredentialsNonExpired()
    {
        return false;
    }


    public Session getSession()
    {
        return session;
    }


    public void setSession(Session session)
    {
        this.session = session;
    }
}
