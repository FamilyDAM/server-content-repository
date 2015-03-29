/*
 * This file is part of FamilyDAM Project.
 *
 *     The FamilyDAM Project is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     The FamilyDAM Project is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with the FamilyDAM Project.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.familydam.core.security;

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


    public Collection<? extends GrantedAuthority> getAuthorities()
    {
        return authorities;
    }


    public void setAuthorities(Collection<? extends GrantedAuthority> authorities)
    {
        this.authorities = authorities;
    }
}
