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

import com.familydam.core.FamilyDAM;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.api.security.user.AuthorizableExistsException;
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
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.security.auth.login.LoginException;
import javax.security.sasl.AuthenticationException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mnimer on 9/19/14.
 */
@Controller
@RequestMapping("/api/users")
public class UserManagerController extends AuthenticatedService
{
    @Autowired
    private Repository repository;


    @RequestMapping(value = "/login", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Map> authenticateUser(HttpServletRequest request, HttpServletResponse response, @RequestParam("username") String username, @RequestParam("password") String password) throws IOException, LoginException, RepositoryException
    {
        Session session = null;
        try {
            Credentials credentials = new SimpleCredentials(username, password.toCharArray());
            session = repository.login(credentials, null);

            Map userProps = new HashMap();
            userProps.put("userid", session.getUserID());
            userProps.put("username", session.getUserID());
            session.getAttributeNames();

            return new ResponseEntity<Map>(userProps, HttpStatus.OK);
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
        Session session = null;
        try {
            session = getSession(new SimpleCredentials(FamilyDAM.adminUserId, FamilyDAM.adminPassword.toCharArray()));
            //UserManager userManager = getUserManager(session);

            /**
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
                if ( !user.getID().equalsIgnoreCase(UserConstants.DEFAULT_ANONYMOUS_ID) )//&& !user.getID().equalsIgnoreCase(UserConstants.DEFAULT_ADMIN_ID) )
                {
                    //todo: replace with User pojo
                    Map<String, String> userMap = new HashMap<>();
                    userMap.put("username", user.getID());
                    userMap.put(JcrConstants.JCR_PATH, user.getPath());
                    userLists.add(userMap);
                }
            }
             **/

            //todo: query for a real list of users, replace with User pojo
            ArrayList<Map<String, String>> userLists = new ArrayList<>();
            Map<String, String> userMap = new HashMap<>();
            userMap.put("username", session.getUserID());
            userMap.put(JcrConstants.JCR_PATH, "/");
            userLists.add(userMap);

            return new ResponseEntity<>(userLists, HttpStatus.OK);
        }
        /**
        catch (AuthenticationException ae) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }**/
        finally {
            if( session != null) session.logout();
        }
    }



    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<String> createUser(HttpServletRequest request, @RequestParam("username") String username, @RequestParam("password") String newPassword) throws IOException, LoginException, NoSuchWorkspaceException, AuthorizableExistsException, RepositoryException
    {
        Session session = null;
        try{
            session = getSession(new SimpleCredentials(username, newPassword.toCharArray()));
            //UserManager userManager = getUserManager(session);
            //User user = userManager.createUser(username, newPassword);
            //session.save();

            //return new ResponseEntity<String>(user.getPath(), HttpStatus.CREATED);
            return new ResponseEntity<String>(HttpStatus.CREATED);
        }
        /**
        catch (AuthenticationException ae) {
            return new ResponseEntity<String>(HttpStatus.FORBIDDEN);
        }
        catch (CommitFailedException ae) {
            return new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
        }**/
        finally {
            if( session != null ) session.logout();
        }
    }


    /**
     * Get single user
     *
     * @param request
     * @param username
     * @return
     * @throws IOException
     * @throws LoginException
     * @throws NoSuchWorkspaceException
     * @throws AuthorizableExistsException
     * @throws RepositoryException
     */
    @RequestMapping(value = "/{username}", method = RequestMethod.GET)
    public ResponseEntity<Map> getUser(HttpServletRequest request, HttpServletResponse response,
                                       @PathVariable("username") String username) throws IOException, LoginException, RepositoryException
    {
        Session session = null;
        try{
            session = getSession(request, response);
            //UserManager userManager = getUserManager(session);
            //Authorizable user = userManager.getAuthorizable(username);
            //NodeUtil userNode = new NodeUtil(session.getLatestRoot().getTree(user.getPath()));

            //todo: replace with User pojo
            Map userProps = new HashMap();//PropertyUtil.readProperties(userNode.getTree());
            userProps.put("userid", session.getUserID());
            userProps.put("username", session.getUserID());
            userProps.put(JcrConstants.JCR_PATH, "/");//user.getPath());
            //userProps.remove("rep:password");

            return new ResponseEntity<>(userProps, HttpStatus.OK);
        }
        catch (AuthenticationException ae) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        finally {
            if( session != null) session.logout();
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
    public ResponseEntity<Map> updateUser(HttpServletRequest request, HttpServletResponse response,
                                          @PathVariable("username") String username) throws IOException, LoginException, RepositoryException
    {
        Session session = null;
        try {
            session = getSession(request, response);
            //UserManager userManager = getUserManager(session);
            //Authorizable user = userManager.getAuthorizable(username);

            //NodeUtil userNode = new NodeUtil(session.getLatestRoot().getTree(user.getPath()));
            //PropertyUtil.writeParametersToNode(userNode, request.getParameterMap());


            return new ResponseEntity<Map>(HttpStatus.OK);
        } catch (AuthenticationException ae) {
            return new ResponseEntity<Map>(HttpStatus.FORBIDDEN);
        }
        /**
        catch (CommitFailedException ae) {
            return new ResponseEntity<Map>(HttpStatus.INTERNAL_SERVER_ERROR);
        }**/
        finally {
            if( session!=null ) session.logout();
        }
    }


}
