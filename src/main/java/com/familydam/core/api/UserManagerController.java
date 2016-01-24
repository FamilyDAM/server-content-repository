/*
 * Copyright (c) 2015  Mike Nimer & 11:58 Labs
 */

package com.familydam.core.api;

import com.familydam.core.FamilyDAMConstants;
import com.familydam.core.dao.UserDao;
import com.familydam.core.security.CustomUserDetails;
import com.familydam.core.security.TokenHandler;
import com.familydam.core.services.AuthenticatedHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.felix.scr.annotations.Reference;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.AuthorizableExistsException;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.QueryBuilder;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.oak.plugins.nodetype.NodeTypeConstants;
import org.apache.jackrabbit.oak.spi.security.authorization.AuthorizationConfiguration;
import org.apache.jackrabbit.value.BooleanValue;
import org.apache.jackrabbit.value.DateValue;
import org.apache.jackrabbit.value.DoubleValue;
import org.apache.jackrabbit.value.LongValue;
import org.apache.jackrabbit.value.StringValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.PostConstruct;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import javax.security.auth.login.LoginException;
import javax.security.sasl.AuthenticationException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by mnimer on 9/19/14.
 */
@Controller
@RequestMapping("/api/users")
public class UserManagerController
{
    @Reference
    private AuthorizationConfiguration authorizationConfiguration;

    @Autowired private Repository repository;
    @Autowired private UserDao userDao;
    @Autowired private TokenHandler tokenHandler;
    @Autowired private ApplicationContext applicationContext;

    private AuthenticatedHelper authenticatedHelper = null;

    @PostConstruct
    private void setup()
    {
        authenticatedHelper = applicationContext.getBean(AuthenticatedHelper.class);
    }




    @RequestMapping(value = "/login", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<CustomUserDetails> authenticateUser(HttpServletRequest request, HttpServletResponse response,
                                                              @RequestParam("username") String username_,
                                                              @RequestParam("password") String password_) throws IOException, LoginException, RepositoryException
    {
        CustomUserDetails customUserDetails = userDao.getUser(username_, password_);

        customUserDetails.setPrincipalName(username_);
        customUserDetails.setPassword(password_);


        MultiValueMap<String, String> _headers = new LinkedMultiValueMap<>();
        if( customUserDetails.getCredentials() != null ) {
            String token = ((SimpleCredentials) customUserDetails.getCredentials()).getAttribute(".token").toString();
            //_headers.add("token", token);
            _headers.add(FamilyDAMConstants.XAUTHTOKEN, token);
        }else {
            // we should not support the else scenarion
            //String _token = tokenHandler.createTokenForUser(customUserDetails);
            //_headers.add(FamilyDAMConstants.XAUTHTOKENREFRESH, _token);
        }

        return new ResponseEntity<>(customUserDetails, _headers, HttpStatus.OK);

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
    @RequestMapping(method = {RequestMethod.GET})
    public ResponseEntity<Collection<Map>> getUserList(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException, LoginException, RepositoryException
    {
        Session session = null;
        try {
            session = authenticatedHelper.getAdminSession();

            UserManager userManager = ((JackrabbitSession) session).getUserManager();

            final Value anonymousValue = session.getValueFactory().createValue("anonymous");
            //final Value adminValue = session.getValueFactory().createValue("admin");

            Iterator<Authorizable> users = userManager.findAuthorizables(new org.apache.jackrabbit.api.security.user.Query()
            {
                public <T> void build(QueryBuilder<T> builder)
                {
                    builder.setCondition(builder.neq("rep:principalName", new StringValue("anonymous")));
                        builder.setCondition(builder.and(builder.neq("rep:principalName", new StringValue("admin")), builder.neq("rep:principalName", new StringValue("anonymous"))));
                    builder.setSortOrder("@rep:principalName", QueryBuilder.Direction.ASCENDING);
                    builder.setSelector(User.class);
                }
            });


            List<Map> userList = new ArrayList<>();
            while (users.hasNext()) {

                Authorizable user = users.next();

                Map userMap = new HashMap<>();
                userMap.put("username", user.getID());
                userMap.put("path", user.getPath());
                userMap.put("id", user.getID());

                Iterator<String> names = user.getPropertyNames();
                while(names.hasNext())
                {
                    String _name = names.next();
                    if( user.getProperty(_name).length == 1) {
                        userMap.put(_name, user.getProperty(_name)[0].getString());
                    }
                }

                Map props = new HashMap<>();
                //userMap.put("properties", props);

                Iterator<String> propertyNames = user.getPropertyNames();
                while (propertyNames.hasNext()) {
                    String key = propertyNames.next();
                    props.put(key, user.getProperty(key));
                }

                userList.add(userMap);
            }


            Collections.sort(userList, new Comparator<Object>()
            {
                @Override public int compare(Object o1, Object o2)
                {
                    return ((String)((Map)o1).get("username")).compareToIgnoreCase((String)((Map)o2).get("username"));
                }
            });


            return new ResponseEntity<>(userList, HttpStatus.OK);
        }
        finally {
            if( session != null) session.logout();
        }
    }



    //@PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<User> createUser(HttpServletRequest request,
                                             @AuthenticationPrincipal Authentication currentUser_,
                                             @RequestParam("username") String username, 
                                             @RequestParam("password") String newPassword,
                                             @RequestParam("userProps") String userProps
    ) throws IOException, LoginException, NoSuchWorkspaceException, AuthorizableExistsException, RepositoryException
    {
        Session session = null;
        try{
            session = authenticatedHelper.getAdminSession();

            UserManager userManager = ((JackrabbitSession) session).getUserManager();
            User user = userManager.createUser(username, newPassword);


            Group familyGroup = (Group)userManager.getAuthorizable(FamilyDAMConstants.FAMILY_GROUP);
            // if this family group is empty and this is the first user, make them an admin
            if( !familyGroup.getMembers().hasNext() ) {
                Group familyAdminGroup = (Group)userManager.getAuthorizable(FamilyDAMConstants.FAMILY_ADMIN_GROUP);
                familyAdminGroup.addMember(user);
            }
            //now add the user to the family group
            familyGroup.addMember(user);


            Map _userProps = new ObjectMapper().readValue(userProps, Map.class);
            for (Object key : _userProps.keySet()) {

                if( _userProps.get(key) instanceof String ) {
                    user.setProperty(key.toString(), new StringValue((String) _userProps.get(key)));
                }else if( _userProps.get(key) instanceof Double ) {
                    user.setProperty(key.toString(), new DoubleValue((Double) _userProps.get(key)));
                }else if( _userProps.get(key) instanceof Boolean ) {
                    user.setProperty(key.toString(), new BooleanValue((Boolean) _userProps.get(key)));
                }else if( _userProps.get(key) instanceof Long ) {
                    user.setProperty(key.toString(), new LongValue((Long) _userProps.get(key)));
                }else if( _userProps.get(key) instanceof Date) {
                    Calendar _cal = Calendar.getInstance();
                    _cal.setTime(  (Date)_userProps.get(key)  );
                    user.setProperty(key.toString(), new DateValue( _cal ));
                }
            }

            session.save();

            // make sure the user has an UUID
            Node userNode = session.getNode(user.getPath());
            userNode.addMixin(NodeTypeConstants.MIX_REFERENCEABLE);
            session.save();

            // now create a new session for the new users.
            //Session userSession = authenticatedHelper.getSession(new SimpleCredentials(user.getID(), newPassword.toCharArray()));
            // create all of the users folders, using their session.
            userDao.createUserDirectories(session, user);


            return new ResponseEntity<>(HttpStatus.CREATED);
        }
        catch (AuthenticationException ae) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        catch (Exception ae) {
            ae.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
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
    @PreAuthorize("hasRole('ROLE_USER')")
    @RequestMapping(value = "/{username}", method = RequestMethod.GET)
    public ResponseEntity<Map> getUser(HttpServletRequest request, HttpServletResponse response,
                                       @AuthenticationPrincipal Authentication currentUser_,
                                       @PathVariable("username") String username
    ) throws IOException, LoginException, RepositoryException
    {
        Session session = null;
        try{
            session = authenticatedHelper.getSession(currentUser_);
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
    //@PreAuthorize("hasRole('ROLE_USER')")
    @RequestMapping(value = "/{username}", method = RequestMethod.POST)
    public ResponseEntity<Map> updateUser(HttpServletRequest request, HttpServletResponse response,
                                          @AuthenticationPrincipal Authentication currentUser_,
                                          @PathVariable("username") String username,
                                          @RequestParam(value = "password", required = false) String newPassword,
                                          @RequestParam("userProps") String userProps
    ) throws IOException, LoginException, RepositoryException
    {
        Session session = null;
        try {
            session = authenticatedHelper.getAdminSession();

            UserManager userManager = ((JackrabbitSession) session).getUserManager();
            Authorizable user = userManager.getAuthorizable(username);

            //NodeUtil userNode = new NodeUtil(session.getLatestRoot().getTree(user.getPath()));
            //PropertyUtil.writeParametersToNode(userNode, request.getParameterMap());

            Map props = new ObjectMapper().readValue(userProps, Map.class);
            for (Object key : props.keySet()) {

                Object _val = props.get(key);
                if( _val instanceof String ) {
                    user.setProperty((String)key, new StringValue((String)_val));
                }else if( _val instanceof Boolean) {
                    user.setProperty((String)key, new BooleanValue((Boolean)_val));
                }else if( _val instanceof Long ) {
                    user.setProperty((String)key, new LongValue((Long)_val));
                }else if( _val instanceof Date ) {
                    Calendar _cal = Calendar.getInstance();
                    _cal.setTime((Date)_val);
                    user.setProperty((String)key, new DateValue(_cal));
                }else if( _val instanceof Double ) {
                    user.setProperty((String)key, new DoubleValue((Double)_val));
                }
            }

            session.save();

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


    private static String buildSearchPattern(String nameHint) {
        if (nameHint == null) {
            return "%";
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append('%');
            sb.append(nameHint.replace("%", "\\%").replace("_", "\\_"));
            sb.append('%');
            return sb.toString();
        }
    }


    private static Class<? extends Authorizable> getAuthorizableClass(int searchType) {
        switch (searchType) {
            case PrincipalManager.SEARCH_TYPE_GROUP:
                return org.apache.jackrabbit.api.security.user.Group.class;
            case PrincipalManager.SEARCH_TYPE_NOT_GROUP:
                return User.class;
            case PrincipalManager.SEARCH_TYPE_ALL:
                return Authorizable.class;
            default:
                throw new IllegalArgumentException("Invalid search type " + searchType);

        }
    }
}
