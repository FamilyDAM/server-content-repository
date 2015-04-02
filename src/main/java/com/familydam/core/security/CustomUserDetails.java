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
