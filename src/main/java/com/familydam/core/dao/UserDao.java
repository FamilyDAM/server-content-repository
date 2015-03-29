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

package com.familydam.core.dao;

import com.familydam.core.FamilyDAM;
import com.familydam.core.security.CustomUserDetails;
import com.familydam.core.services.AuthenticatedHelper;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.QueryBuilder;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.oak.jcr.session.SessionImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by mnimer on 3/28/15.
 */
@Service
public class UserDao
{

    @Autowired private Repository repository;
    @Autowired private AuthenticatedHelper authenticatedHelper;


    public CustomUserDetails getUser(String username_, String password_) throws UsernameNotFoundException
    {
        CustomUserDetails _user = null;

        Session session = null;
        try {
            Credentials credentials = new SimpleCredentials(username_, password_.toCharArray());

            //todo: add "admin" filter, so it's not allowed.
            session = repository.login(credentials, null);

            UserManager userManager = (((SessionImpl) session).getUserManager());
            Authorizable authorizable = userManager.getAuthorizable(session.getUserID());

            _user = new CustomUserDetails();
            _user.setPrincipalName(authorizable.getID());
            _user.setPath(authorizable.getPath());



            if (_user != null) {
                Collection<? extends GrantedAuthority> _roles = AuthorityUtils.createAuthorityList("ROLE_USER", "ROLE_ADMIN");
                _user.setAuthorities(_roles);
                return _user;
            }
        }
        catch(RepositoryException ex){
            throw new UsernameNotFoundException(username_);
        }
        finally {
            if (session != null) {
                session.logout();
            }
        }

        return _user;
    }


    public CustomUserDetails getUserByPrincipal(String principal_) throws UsernameNotFoundException
    {
        Session session = null;
        try {
            Credentials credentials = new SimpleCredentials(FamilyDAM.adminUserId, FamilyDAM.adminPassword.toCharArray());

            //todo: add "admin" filter, so it's not allowed.
            session = repository.login(credentials, null);

            UserManager userManager = (((SessionImpl) session).getUserManager());
            Authorizable authorizable = userManager.getAuthorizable(session.getUserID());

            CustomUserDetails _user = new CustomUserDetails();
            _user.setPrincipalName(authorizable.getID());
            _user.setPath(authorizable.getPath());

            if (_user != null) {
                Collection<? extends GrantedAuthority> _roles = AuthorityUtils.createAuthorityList("ROLE_USER", "ROLE_ADMIN");
                _user.setAuthorities(_roles);
                return _user;
            }


            throw new UsernameNotFoundException(principal_);
        }
        catch(RepositoryException ex){
            throw new UsernameNotFoundException(principal_);
        }
        finally {
            if (session != null) {
                session.logout();
            }
        }
    }

}
