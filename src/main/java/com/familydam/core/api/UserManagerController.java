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

import com.familydam.core.Application;
import com.familydam.core.FamilyDAMConstants;
import com.familydam.core.helpers.PropertyUtil;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.AuthorizableExistsException;
import org.apache.jackrabbit.api.security.user.QueryBuilder;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.oak.api.CommitFailedException;
import org.apache.jackrabbit.oak.api.ContentRepository;
import org.apache.jackrabbit.oak.api.ContentSession;
import org.apache.jackrabbit.oak.api.Root;
import org.apache.jackrabbit.oak.api.Tree;
import org.apache.jackrabbit.oak.spi.security.user.UserConstants;
import org.apache.jackrabbit.oak.util.NodeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.jcr.Credentials;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import javax.security.auth.login.LoginException;
import javax.security.sasl.AuthenticationException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by mnimer on 9/19/14.
 */
@Controller
@RequestMapping("/api/users")
public class UserManagerController extends AuthenticatedService
{
    @Autowired
    private ContentRepository contentRepository;


    private Tree getContentRoot(ContentSession session) throws LoginException, NoSuchWorkspaceException
    {
        Root root = session.getLatestRoot();
        Tree tree = root.getTree("/");
        return tree.getChild(FamilyDAMConstants.DAM_ROOT);
    }


    @RequestMapping(value = "/login", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Map> authenticateUser(HttpServletRequest request, HttpServletResponse response, @RequestParam("username") String username, @RequestParam("password") String password) throws IOException, LoginException, RepositoryException
    {
        try {
            Credentials credentials = new SimpleCredentials(username, password.toCharArray());
            ContentSession session = contentRepository.login(credentials, null);
            //Set<Principal> principals = session.getAuthInfo().getPrincipals();
            //Authorizable authorizable = getUserManager(session).getAuthorizable(session.getAuthInfo().getUserID());


            UserManager userManager = getUserManager(session);
            Authorizable user = userManager.getAuthorizable(username);

            NodeUtil userNode = new NodeUtil(session.getLatestRoot().getTree(user.getPath()));
            Map userProps = PropertyUtil.readProperties(userNode.getTree());
            userProps.remove("rep:password");

            return new ResponseEntity<Map>(userProps, HttpStatus.OK);
        }
        catch (LoginException ae) {
            return new ResponseEntity<Map>(HttpStatus.FORBIDDEN);
        }
        finally {

        }
    }


    /**
     * Get list of all users in system.
     * <p/>
     * todo: this should not require an authenticated list
     *
     * @param request
     * @return
     * @throws IOException
     * @throws LoginException
     * @throws NoSuchWorkspaceException
     * @throws AuthorizableExistsException
     * @throws RepositoryException
     */
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<ArrayList<Map<String, String>>> getUserList(HttpServletRequest request) throws IOException, LoginException, RepositoryException
    {
        try (ContentSession session = getSession(new SimpleCredentials(Application.adminUserId, Application.adminPassword.toCharArray()))) {
            UserManager userManager = getUserManager(session);

            Iterator<Authorizable> userQueryResult = userManager.findAuthorizables(new org.apache.jackrabbit.api.security.user.Query()
            {
                public <T> void build(QueryBuilder<T> builder)
                {
                    //builder.setSelector(Authorizable.class);
                    builder.setLimit(0, 100);
                }
            });

            ArrayList<Map<String, String>> userLists = new ArrayList<>();
            while (userQueryResult.hasNext()) {
                Authorizable user = userQueryResult.next();

                // return all users but the system admin or anonymous accounts
                if (!user.getID().equalsIgnoreCase(UserConstants.DEFAULT_ADMIN_ID) && !user.getID().equalsIgnoreCase(UserConstants.DEFAULT_ANONYMOUS_ID)) {
                    Map<String, String> userMap = new HashMap<>();
                    userMap.put("username", user.getID());
                    userMap.put(JcrConstants.JCR_PATH, user.getPath());
                    userLists.add(userMap);
                }
            }

            return new ResponseEntity<>(userLists, HttpStatus.OK);
        }
        catch (AuthenticationException ae) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        finally {

        }
    }



    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<String> createUser(HttpServletRequest request, @RequestParam("username") String username, @RequestParam("password") String newPassword) throws IOException, LoginException, NoSuchWorkspaceException, AuthorizableExistsException, RepositoryException
    {
        try (ContentSession session = getSession(new SimpleCredentials(username, newPassword.toCharArray()))) {
            UserManager userManager = getUserManager(session);
            User user = userManager.createUser(username, newPassword);

            session.getLatestRoot().commit();

            return new ResponseEntity<String>(user.getPath(), HttpStatus.CREATED);
        }
        catch (AuthenticationException ae) {
            return new ResponseEntity<String>(HttpStatus.FORBIDDEN);
        }
        catch (CommitFailedException ae) {
            return new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        finally {

        }
    }


    /**
     * Get single user
     *
     * @param request
     * @param userId
     * @return
     * @throws IOException
     * @throws LoginException
     * @throws NoSuchWorkspaceException
     * @throws AuthorizableExistsException
     * @throws RepositoryException
     */
    @RequestMapping(value = "/{username}", method = RequestMethod.GET)
    public ResponseEntity<Map> getUser(HttpServletRequest request, @PathVariable("username") String username) throws IOException, LoginException, RepositoryException
    {
        try (ContentSession session = getSession(request)) {
            UserManager userManager = getUserManager(session);
            Authorizable user = userManager.getAuthorizable(username);

            NodeUtil userNode = new NodeUtil(session.getLatestRoot().getTree(user.getPath()));
            Map userProps = PropertyUtil.readProperties(userNode.getTree());
            userProps.remove("rep:password");

            return new ResponseEntity<Map>(userProps, HttpStatus.OK);
        }
        catch (AuthenticationException ae) {
            return new ResponseEntity<Map>(HttpStatus.FORBIDDEN);
        }
        finally {

        }
    }


    /**
     * Update a single user, including resetting password
     *
     * @param request
     * @return
     * @throws IOException
     * @throws LoginException
     * @throws RepositoryException
     */
    @RequestMapping(value = "/{username}", method = RequestMethod.POST)
    public ResponseEntity<Map> updateUser(HttpServletRequest request, @PathVariable("username") String username) throws IOException, LoginException, RepositoryException
    {
        try (ContentSession session = getSession(request)) {
            UserManager userManager = getUserManager(session);
            Authorizable user = userManager.getAuthorizable(username);

            NodeUtil userNode = new NodeUtil(session.getLatestRoot().getTree(user.getPath()));
            PropertyUtil.writeParametersToNode(userNode, request.getParameterMap());


            return new ResponseEntity<Map>(HttpStatus.OK);
        } catch (AuthenticationException ae) {
            return new ResponseEntity<Map>(HttpStatus.FORBIDDEN);
        } catch (CommitFailedException ae) {
            return new ResponseEntity<Map>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        finally {

        }
    }


}
