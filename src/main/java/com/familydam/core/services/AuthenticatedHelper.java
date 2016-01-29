/*
 * Copyright (c) 2015  Mike Nimer & 11:58 Labs
 */

package com.familydam.core.services;

import com.familydam.core.FamilyDAM;
import com.familydam.core.security.CustomUserDetails;
import com.familydam.core.security.UserAuthentication;
import org.apache.jackrabbit.api.security.authentication.token.TokenCredentials;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.oak.spi.security.ConfigurationParameters;
import org.apache.jackrabbit.oak.spi.security.authentication.token.TokenProvider;
import org.apache.jackrabbit.oak.spi.security.user.UserConstants;
import org.apache.jackrabbit.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.security.sasl.AuthenticationException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mnimer on 9/23/14.
 */
@Service
public class AuthenticatedHelper
{
    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired private Repository repository;

    private Session adminSession = null;
    private Map<String, Session> savedSessions = new HashMap<>();

    /**
     * internal use only. This is used by background process to do async work on nodes (generate thumbnails, etc.)
     * @return
     * @throws AuthenticationException
     */
    public Session getAdminSession() throws AuthenticationException
    {
        /**
        if (adminSession != null && adminSession.isLive()) {
            return adminSession;
        }
         **/

        SimpleCredentials credentials = new SimpleCredentials(FamilyDAM.adminUserId, FamilyDAM.adminPassword.toCharArray());
        adminSession = getSession(credentials);
        return adminSession;
    }

    /**
     * pull the credentials out of spring and login to the jcr repo
     * @param authentication_
     * @return
     * @throws AuthenticationException
     */
    public Session getSession(Authentication authentication_) throws AuthenticationException
    {
        try{
            Session _session = savedSessions.get( ((UserAuthentication) authentication_).getUser().getPath() );
            if( _session != null){
                if(!_session.isLive()){
                    _session.refresh(true);
                }
                return _session;
            }
        } catch (RepositoryException ex){
            ex.printStackTrace();
            //swallow
        }


        if( ((CustomUserDetails) ((UserAuthentication) authentication_).getUser()).getSession() != null ){
            return ((CustomUserDetails) ((UserAuthentication) authentication_).getUser()).getSession();
        }else {
            Credentials credentials = (Credentials) authentication_.getCredentials();
            Session session = getSession(credentials);
            ((CustomUserDetails) ((UserAuthentication) authentication_).getUser()).setSession(session);
            return session;
        }
    }


    /**
     * Get the session tied to a credentials object
     * @param credentials
     * @return
     * @throws AuthenticationException
     */
    public Session getSession(Credentials credentials) throws AuthenticationException
    {
        try {
            if( credentials instanceof TokenCredentials) {
                ((TokenCredentials) credentials).setAttribute("oak.refresh-interval", "20000");
                ((TokenCredentials) credentials).setAttribute(TokenProvider.PARAM_TOKEN_EXPIRATION, "1209600000");
            }else if( credentials instanceof SimpleCredentials ){
                ((SimpleCredentials) credentials).setAttribute(TokenProvider.PARAM_TOKEN_EXPIRATION, "86400000");
            }
            Session session = repository.login(credentials, null);
            return session;
        }catch (Exception ex){
            throw new AuthenticationException(ex.getMessage(), ex);
        }
    }


    public Session getRepositorySession(HttpServletRequest request, HttpServletResponse response) throws RepositoryException, AuthenticationException
    {
        Credentials credentials = null;

        String authorization = request.getHeader("Authorization");
        Cookie[] cookies = request.getCookies();

        Map<String, String> cookieMap = new HashMap<>();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                cookieMap.put(cookie.getName(), cookie.getValue());
            }
        }

        // Try to pull the auth data a few different ways
        if (authorization != null && authorization.startsWith("Basic ")) {
            String[] basic =
                    Base64.decode(authorization.substring("Basic ".length())).split(":");
            credentials = new SimpleCredentials(basic[0], basic[1].toCharArray());

            Cookie _cookie = new Cookie("x-auth-token", authorization);
            _cookie.setDomain(".localhost");
            _cookie.setHttpOnly(true);
            response.addCookie(_cookie);

            request.getSession().setAttribute("x-auth-token", authorization);
        } else if (request.getParameter("token") != null) {
            String[] basic = Base64.decode(request.getParameter("token").substring("Basic ".length())).split(":");
            credentials = new SimpleCredentials(basic[0], basic[1].toCharArray());
        } else if (cookieMap.containsKey("x-auth-token")) {
            String[] basic = Base64.decode(cookieMap.get("authorization").substring("Basic ".length())).split(":");
            credentials = new SimpleCredentials(basic[0], basic[1].toCharArray());
        } else {
            String username = request.getParameter("j_username");
            String password = request.getParameter("j_password");
            if (username != null && password != null) {
                credentials = new SimpleCredentials(username, password.toCharArray());
            }
        }

        if (credentials == null) {
            throw new AuthenticationException();
        }

        Session session = repository.login(credentials, null);
        return session;
    }


    public UserManager getUserManager(Session session) throws RepositoryException
    {
        ConfigurationParameters defaultConfig = ConfigurationParameters.EMPTY;
        String defaultUserPath = defaultConfig.getConfigValue(UserConstants.PARAM_USER_PATH, UserConstants.DEFAULT_USER_PATH);
        String defaultGroupPath = defaultConfig.getConfigValue(UserConstants.PARAM_GROUP_PATH, UserConstants.DEFAULT_GROUP_PATH);

        Map customOptions = new HashMap<String, Object>();
        //customOptions.put(UserConstants.PARAM_GROUP_PATH, "/home/groups");
        customOptions.put(UserConstants.PARAM_USER_PATH, "/home/users");


        UserManager userMgr = null;//new UserManagerImpl(session.getRootNode(), NamePathMapper.DEFAULT, ConfigurationParameters.EMPTY);
        return userMgr;
    }

}
