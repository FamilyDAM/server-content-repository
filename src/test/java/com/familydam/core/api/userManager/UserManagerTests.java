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

import com.familydam.core.Application;
import com.familydam.core.api.fileManager.RootDirTest;
import org.apache.jackrabbit.oak.api.ContentRepository;
import org.hamcrest.Matchers;
import org.junit.Before;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by mnimer on 9/19/14.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
public class UserManagerTests
{

    Logger logger = LoggerFactory.getLogger(RootDirTest.class);

    @Autowired
    private ContentRepository contentRepository;


    @Autowired
    public WebApplicationContext wac;

    private MockMvc mockMvc;

    //@Value("${server.port}")
    public String port;
    public String rootUrl;


    @Before
    public void setupMock() throws Exception
    {
        //port = context.getEnvironment().getProperty("server.port");
        rootUrl = "http://localhost:8080";// +port;

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
    public void loginUsers2() throws Exception
    {
        MvcResult result = this.mockMvc
                .perform(get(rootUrl + "/login").param("username", "admin").param("password", "password"))
                .andExpect(status().isOk())
                .andReturn();

        String resultJson = result.getResponse().getContentAsString();
        logger.debug(resultJson);
    }


    @Test
    public void listUsers() throws Exception
    {

        MvcResult userList = this.mockMvc
                .perform(get(rootUrl + "/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", Matchers.hasSize(0)))
                .andReturn();

        String resultJson = userList.getResponse().getContentAsString();
        logger.debug(resultJson);
    }

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
