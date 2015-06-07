/*
 * Copyright (c) 2015  Mike Nimer & 11:58 Labs
 */

package com.familydam.core.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.security.core.GrantedAuthority;

import java.io.Serializable;
import java.util.Collection;

/**
 * Created by mnimer on 1/26/15.
 */
public class User implements Serializable
{
    
    private String jcr_uuid  = null;
    private String principalName = null;
    private String path  = null;
    private String created = null;
    private String createdBy = null;
    @JsonIgnore
    private String password = null;


    // Authorized Roles
    private Collection<? extends GrantedAuthority> authorities = null;


    public String getJcr_uuid()
    {
        return jcr_uuid;
    }


    public void setJcr_uuid(String jcr_uuid)
    {
        this.jcr_uuid = jcr_uuid;
    }


    public String getPrincipalName()
    {
        return principalName;
    }


    public void setPrincipalName(String principalName)
    {
        this.principalName = principalName;
    }


    public String getPath()
    {
        return path;
    }


    public void setPath(String path)
    {
        this.path = path;
    }


    public String getCreated()
    {
        return created;
    }


    public void setCreated(String created)
    {
        this.created = created;
    }


    public String getCreatedBy()
    {
        return createdBy;
    }


    public void setCreatedBy(String createdBy)
    {
        this.createdBy = createdBy;
    }


    public String getPassword()
    {
        return password;
    }


    public void setPassword(String password)
    {
        this.password = password;
    }


    public Collection<? extends GrantedAuthority> getAuthorities()
    {
        return authorities;
    }


    public void setAuthorities(Collection<? extends GrantedAuthority> authorities)
    {
        this.authorities = authorities;
    }
}
