/*
 * Copyright (c) 2015  Mike Nimer & 11:58 Labs
 */

package com.familydam.core.dao;

import com.familydam.core.FamilyDAMConstants;
import com.familydam.core.security.CustomUserDetails;
import com.familydam.core.services.AuthenticatedHelper;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.commons.jackrabbit.authorization.AccessControlUtils;
import org.apache.jackrabbit.oak.jcr.session.SessionImpl;
import org.apache.jackrabbit.oak.spi.security.authentication.token.TokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.security.Privilege;
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

    private UserManager adminUserManager;



    public CustomUserDetails getUser(String username_, String password_) throws UsernameNotFoundException
    {


        CustomUserDetails _user = null;

        Session session = null;
        Session adminSession = null;
        try {
            adminSession = authenticatedHelper.getAdminSession();
            UserManager adminUserManager = (((SessionImpl) adminSession).getUserManager());

            SimpleCredentials credentials = new SimpleCredentials(username_, password_.toCharArray());
            credentials.setAttribute(".token", "");
            credentials.setAttribute(TokenProvider.PARAM_TOKEN_EXPIRATION, 1209600000l);

            //todo: add "admin" filter, so it's not allowed.
            session = repository.login(credentials, null);

            Authorizable authorizable = adminUserManager.getAuthorizable(session.getUserID());

            _user = new CustomUserDetails();
            _user.setCredentials(credentials);
            _user.setPrincipalName(authorizable.getID());
            _user.setPassword(password_);
            _user.setPath(authorizable.getPath());

            if( authorizable.getProperty(FamilyDAMConstants.FIRST_NAME) != null ) {
                _user.setFirstName(((Value[]) authorizable.getProperty(FamilyDAMConstants.FIRST_NAME))[0].getString());
            }
            if( authorizable.getProperty(FamilyDAMConstants.LAST_NAME) != null ) {
                _user.setLastName(((Value[]) authorizable.getProperty(FamilyDAMConstants.LAST_NAME))[0].getString());
            }
            if( authorizable.getProperty(FamilyDAMConstants.EMAIL) != null ) {
                _user.setEmail(((Value[]) authorizable.getProperty(FamilyDAMConstants.EMAIL))[0].getString());
            }


            if (_user != null) {
                Collection<? extends GrantedAuthority> _roles = AuthorityUtils.createAuthorityList("ROLE_USER", "ROLE_ADMIN");
                _user.setAuthorities(_roles);
                return _user;
            }
        }
        catch(RepositoryException ex){
            throw new UsernameNotFoundException(username_);
        }
        catch(AuthenticationException ex){
            throw new UsernameNotFoundException(username_);
        }
        finally {
            if (session != null) {
                session.logout();
            }
            if( adminSession != null){
                adminSession.logout();
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




    public void createUserDirectories(Session session_, User user_) throws RepositoryException, AuthenticationException
    {
        UserManager userManager = ((JackrabbitSession) session_).getUserManager();
        Group familyGroup = (Group)userManager.getAuthorizable(FamilyDAMConstants.FAMILY_GROUP);


        //find system folder (parent folders)
        QueryManager queryManager = session_.getWorkspace().getQueryManager();

        // find all DAM Content Folders, we'll add a user folder to each one
        Query query = queryManager.createQuery("SELECT * FROM [dam:contentfolder] AS s", "sql");

        // Execute the query and get the results ...
        QueryResult result = query.execute();


        javax.jcr.NodeIterator nodeItr = result.getNodes();
        while ( nodeItr.hasNext() ) {
            javax.jcr.Node node = nodeItr.nextNode();

            if( !node.getPath().equals("/") ) {

                Node _node = JcrUtils.getOrAddFolder(node, user_.getID());
                _node.addMixin("mix:created");
                _node.addMixin("dam:userfolder");
                _node.addMixin("dam:extensible");
                _node.setProperty(JcrConstants.JCR_NAME, user_.getID());
                session_.save();

                AccessControlUtils.addAccessControlEntry(session_, _node.getPath(), user_.getPrincipal(), new String[]{Privilege.JCR_ALL}, true);
            }
        }


        // commit new folders.
        session_.save();
    }

}
