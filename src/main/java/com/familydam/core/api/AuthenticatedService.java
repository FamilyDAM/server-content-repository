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

package com.familydam.core.api;

import com.familydam.core.FamilyDAMConstants;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.oak.api.ContentRepository;
import org.apache.jackrabbit.oak.api.ContentSession;
import org.apache.jackrabbit.oak.api.Root;
import org.apache.jackrabbit.oak.api.Tree;
import org.apache.jackrabbit.oak.namepath.NamePathMapper;
import org.apache.jackrabbit.oak.security.SecurityProviderImpl;
import org.apache.jackrabbit.oak.security.user.UserManagerImpl;
import org.apache.jackrabbit.oak.spi.security.ConfigurationParameters;
import org.apache.jackrabbit.oak.spi.security.SecurityProvider;
import org.apache.jackrabbit.oak.spi.security.user.UserConstants;
import org.apache.jackrabbit.oak.util.TreeUtil;
import org.apache.jackrabbit.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.jcr.Credentials;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.security.auth.login.LoginException;
import javax.security.sasl.AuthenticationException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mnimer on 9/23/14.
 */
public class AuthenticatedService
{
    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ContentRepository contentRepository;
    //@Autowired private Root root;

    private SecurityProvider securityProvider;


    protected Tree getContentRoot(ContentSession session) throws LoginException, NoSuchWorkspaceException
    {
        Root root = session.getLatestRoot();
        Tree tree = root.getTree("/");
        return tree.getChild(FamilyDAMConstants.DAM_ROOT);
    }


    protected Tree getRelativeTree(Tree root, String relativePath)
    {
        String _path = relativePath.substring(relativePath.indexOf('~') + 1);
        return TreeUtil.getTree(root, _path);
    }


    private SecurityProvider getSecurityProvider()
    {
        if (securityProvider == null) {
            securityProvider = new SecurityProviderImpl(ConfigurationParameters.EMPTY);
        }
        return securityProvider;
    }



    ContentSession getSession(HttpServletRequest request, HttpServletResponse response) throws LoginException, NoSuchWorkspaceException, AuthenticationException
    {
        return getSession(request);
    }


    ContentSession getSession(HttpServletRequest request) throws LoginException, NoSuchWorkspaceException, AuthenticationException
    {
        Credentials credentials = null;

        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Basic ")) {
            String[] basic =
                    Base64.decode(authorization.substring("Basic ".length())).split(":");
            credentials = new SimpleCredentials(basic[0], basic[1].toCharArray());
        } else {
            String username = request.getParameter("j_username");
            String password = request.getParameter("j_password");
            if( username != null && password != null ){
                credentials = new SimpleCredentials(username, password.toCharArray());
            }
        }

        if( credentials == null ){
            throw new AuthenticationException();
        }

        ContentSession session = contentRepository.login(credentials, null);
        return session;
    }


    ContentSession getSession(Credentials credentials) throws LoginException, NoSuchWorkspaceException, AuthenticationException
    {
        ContentSession session = contentRepository.login(credentials, null);
        return session;
    }



    Session getRepositorySession(HttpServletRequest request, HttpServletResponse response) throws RepositoryException,AuthenticationException
    {
        Credentials credentials = null;

        String authorization = request.getHeader("Authorization");
        Cookie[] cookies = request.getCookies();

        Map<String, String> cookieMap = new HashMap<>();
        if( cookies != null ) {
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
        }
        else if (request.getParameter("token") != null ) {
            String[] basic = Base64.decode(request.getParameter("token").substring("Basic ".length())).split(":");
            credentials = new SimpleCredentials(basic[0], basic[1].toCharArray());
        }
        else if (cookieMap.containsKey("x-auth-token") ) {
            String[] basic = Base64.decode(cookieMap.get("authorization").substring("Basic ".length())).split(":");
            credentials = new SimpleCredentials(basic[0], basic[1].toCharArray());
        }
        else {
            String username = request.getParameter("j_username");
            String password = request.getParameter("j_password");
            if( username != null && password != null ){
                credentials = new SimpleCredentials(username, password.toCharArray());
            }
        }

        if( credentials == null ){
            throw new AuthenticationException();
        }

        Repository repository = JcrUtils.getRepository();
        Session session = repository.login(credentials, null);
        return session;
    }


    UserManager getUserManager(ContentSession session){
        ConfigurationParameters defaultConfig = ConfigurationParameters.EMPTY;
        String defaultUserPath = defaultConfig.getConfigValue(UserConstants.PARAM_USER_PATH, UserConstants.DEFAULT_USER_PATH);
        String defaultGroupPath = defaultConfig.getConfigValue(UserConstants.PARAM_GROUP_PATH, UserConstants.DEFAULT_GROUP_PATH);

        Map customOptions = new HashMap<String, Object>();
        //customOptions.put(UserConstants.PARAM_GROUP_PATH, "/home/groups");
        customOptions.put(UserConstants.PARAM_USER_PATH, "/home/users");


        UserManager userMgr = new UserManagerImpl(session.getLatestRoot(), NamePathMapper.DEFAULT, getSecurityProvider());
        return userMgr;
        /**
         beforeAuthorizables.clear();
         Iterator<Authorizable> iterator = userMgr.findAuthorizables("jcr:primaryType", null, org.apache.jackrabbit.api.security.user.UserManager.SEARCH_TYPE_AUTHORIZABLE);
         while (iterator.hasNext()) {
         beforeAuthorizables.add(iterator.next().getID());
         }
         **/
    }
}
