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
