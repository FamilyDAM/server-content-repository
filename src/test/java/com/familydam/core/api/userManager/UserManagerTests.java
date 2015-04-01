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

package com.familydam.core.api.userManager;

import com.familydam.core.FamilyDAM;
import com.familydam.core.api.fileManager.RootDirTest;
import junit.framework.Assert;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.QueryBuilder;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.oak.jcr.session.SessionImpl;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import java.util.Iterator;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by mnimer on 9/19/14.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = FamilyDAM.class)
@WebAppConfiguration
public class UserManagerTests
{

    Logger logger = LoggerFactory.getLogger(RootDirTest.class);

    @Autowired
    public WebApplicationContext wac;

    @Autowired
    private Repository repository;

    private MockMvc mockMvc;

    //@Value("${server.port}")
    public String port;
    public String rootUrl;


    @Before
    public void setupMock() throws Exception
    {
        port = "9000";//wac.getEnvironment().getProperty("server.port");
        rootUrl = "http://localhost:" + port;

        this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }


    @Test
    public void loginUsers() throws Exception
    {
        MvcResult result = this.mockMvc
                .perform(post(rootUrl + "/api/users/login").param("username", "admin").param("password", "admin"))
                .andExpect(status().isOk())
                .andReturn();

        String resultJson = result.getResponse().getContentAsString();
        logger.debug(resultJson);
    }


    @Test
    public void listUsers() throws Exception
    {

        MvcResult userList = this.mockMvc
                .perform(get(rootUrl + "/api/search").param("type", "rep:User").param(" ", "jcr:name").param("limit", "0"))
                .andExpect(status().isOk())
                .andReturn();

        String resultJson = userList.getResponse().getContentAsString();
        logger.debug(resultJson);
    }


    //@Ignore
    @Test
    public Iterator<Authorizable> listUsersRaw() throws Exception
    {

        Session session = null;
        try {
            session = repository.login(new SimpleCredentials(FamilyDAM.adminUserId, FamilyDAM.adminPassword.toCharArray()));

            UserManager userManager = ((SessionImpl) session).getUserManager();

            final Value anonymousValue = session.getValueFactory().createValue("anonymous");
            //final Value adminValue = session.getValueFactory().createValue("admin");

            Iterator<Authorizable> users = userManager.findAuthorizables(new org.apache.jackrabbit.api.security.user.Query()
            {
                public <T> void build(QueryBuilder<T> builder)
                {
                    builder.setCondition(builder.
                            not(builder.eq("@rep:principalName", anonymousValue)));

                    builder.setSortOrder("@rep:principalName", QueryBuilder.Direction.ASCENDING);
                    builder.setSelector(User.class);
                }
            });
            //Iterator<Authorizable> users = userManager.findAuthorizables("/", "rep:principalName", UserManager.SEARCH_TYPE_USER);

            Assert.assertTrue(users.hasNext());


            while (users.hasNext()) {

                Authorizable user = users.next();
                System.out.println("***********************");
                System.out.println(user.getPrincipal());
                System.out.println(user.getID());
                System.out.println(user.getPath());
                System.out.println("***********************");

                Iterator<String> propertyNames = user.getPropertyNames();
                while(propertyNames.hasNext()) {
                    String key = propertyNames.next();
                    System.out.println(key +"=" +user.getProperty(key));
                }

            }

            return users;

        }
        finally {
            session.logout();
        }
    }


    @Test
    public void createLoginAndRemoveUserRaw() throws Exception
    {
        Session session = null;
        Session session2 = null;
        try {
            session = repository.login(new SimpleCredentials(FamilyDAM.adminUserId, FamilyDAM.adminPassword.toCharArray()));

            UserManager userManager = ((SessionImpl) session).getUserManager();

            // Create User
            String username = "test-" +System.currentTimeMillis();
            User _user = userManager.createUser(username, "test");
            _user.setProperty("foo", session.getValueFactory().createValue("bar"));
            session.save();


            Iterator<Authorizable> users = listUsersRaw();
            org.junit.Assert.assertEquals(2, users);


            // test login
            session2 = repository.login(new SimpleCredentials(username, "test".toCharArray()));
            org.junit.Assert.assertNotNull(session2);
            session2.logout();


            // remove the user
            ((SessionImpl) session).getUserManager().getAuthorizable(username).remove();
            session.save();

        }catch(Exception ex){
            ex.printStackTrace();
            Assert.fail(ex.getMessage());
        }
        finally {
            session.logout();
        }
    }


    @Ignore
    @Test
    public void createUsers() throws Exception
    {
        /*
        MvcResult loginResult = this.mockMvc
                .perform(post(rootUrl + "/api/users/login").param("userid", "admin").param("password", "admin"))
                .andExpect(status().isOk())
                .andReturn();

        String authHeader = loginResult.getResponse().getHeader("Authorization");
        Assert.assertNotNull(authHeader);
        */

        MvcResult createUserResult = this.mockMvc
                .perform(post(rootUrl + "/api/users")
                        .param("_userid", "admin").param("_password", "admin")
                        .param("userid", "mnimer").param("password", "foobar"))
                .andExpect(status().isNoContent())
                .andReturn();
        //String resultJson1 = createUserResult.getResponse().getContentAsString();

        MvcResult userList = this.mockMvc
                .perform(get(rootUrl + "/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", Matchers.hasSize(1)))
                .andReturn();


        String resultJson2 = userList.getResponse().getContentAsString();
        logger.debug(resultJson2);
    }
}
