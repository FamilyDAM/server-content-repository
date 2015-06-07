/*
 * Copyright (c) 2015  Mike Nimer & 11:58 Labs
 */

package com.familydam.core.dao;

import com.familydam.core.security.CustomUserDetails;
import com.familydam.core.services.AuthenticatedHelper;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.oak.jcr.session.SessionImpl;
import org.apache.jackrabbit.oak.spi.security.authentication.token.TokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.security.sasl.AuthenticationException;
import java.util.Collection;

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
            SimpleCredentials credentials = new SimpleCredentials(username_, password_.toCharArray());
            credentials.setAttribute(".token", "");
            credentials.setAttribute(TokenProvider.PARAM_TOKEN_EXPIRATION, 1209600000l);

            //todo: add "admin" filter, so it's not allowed.
            session = repository.login(credentials, null);

            UserManager userManager = (((SessionImpl) session).getUserManager());
            Authorizable authorizable = userManager.getAuthorizable(session.getUserID());

            _user = new CustomUserDetails();
            _user.setCredentials(credentials);
            _user.setPrincipalName(authorizable.getID());
            _user.setPassword(password_);
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
            session = authenticatedHelper.getAdminSession();


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
        catch(RepositoryException|AuthenticationException ex){
            throw new UsernameNotFoundException(principal_);
        }finally {
            if( session != null) session.logout();
        }
    }




    public CustomUserDetails getUserByPath(String path_) throws UsernameNotFoundException
    {
        Session session = null;
        try {
            session = authenticatedHelper.getAdminSession();

            UserManager userManager = (((SessionImpl) session).getUserManager());
            Authorizable authorizable = userManager.getAuthorizableByPath(path_);

            CustomUserDetails _user = new CustomUserDetails();
            _user.setPrincipalName(authorizable.getID());
            _user.setPath(authorizable.getPath());

            if (_user != null) {
                Collection<? extends GrantedAuthority> _roles = AuthorityUtils.createAuthorityList("ROLE_USER", "ROLE_ADMIN");
                _user.setAuthorities(_roles);
                return _user;
            }

            throw new UsernameNotFoundException(path_);
        }
        catch(RepositoryException|AuthenticationException ex){
            throw new UsernameNotFoundException(path_);
        }
        finally {
            if (session != null) {
                session.logout();
            }
        }
    }

}
