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

package com.familydam.core.api.fileManager.folders;

import com.familydam.core.Application;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by mnimer on 9/23/14.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
public class CreateSimpleFolderWithMapParametersTests
{

    Logger logger = LoggerFactory.getLogger(this.getClass());

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

    @After
    public void tearDown() throws Exception
    {
        this.mockMvc
                .perform(delete(rootUrl + "/~/documents/test3"))
                .andReturn();
    }


    /**
     * Use dot notation to signify nested maps
     * @throws Exception
     */
    @Test
    public void createSimpleFolderWithMapParameters() throws Exception
    {
        try {
            MvcResult photoReq = this.mockMvc
                    .perform(post(rootUrl + "/~/documents/test3")
                            .param("key1Array", "key1 value #1")
                            .param("key1Array", "key1 value #2")
                            .param("key2String", "key2 value")
                            .param("key3.prop1", "#1")
                            .param("key3.prop2", "#2")
                            .param("key3.prop3", "#3")
                            .param("key4.A1.prop1", "#1")
                            .param("key4.A1.prop2", "#2")
                            .param("key4.A1.prop3", "#3A")
                            .param("key4.A1.prop3", "#3B"))
                    .andReturn();

            Assert.assertTrue(photoReq.getResponse().getStatus() == 201 || photoReq.getResponse().getStatus() == 200);


            MvcResult directNodeResult = this.mockMvc
                    .perform(get(rootUrl + "/~/documents/test3")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("jcr:name").value("test3"))
                    .andExpect(jsonPath("key1Array").isArray())
                    .andExpect(jsonPath("key1Array", Matchers.hasSize(2)))
                    .andExpect(jsonPath("key1Array[0]").value("key1 value #1"))
                    .andExpect(jsonPath("key1Array[1]").value("key1 value #2"))
                    .andExpect(jsonPath("key3.prop1").value("#1"))
                    .andExpect(jsonPath("key3.prop2").value("#2"))
                    .andExpect(jsonPath("key3.prop3").value("#3"))
                    .andExpect(jsonPath("key4.A1.prop1").value("#1"))
                    .andExpect(jsonPath("key4.A1.prop2").value("#2"))
                    .andExpect(jsonPath("key4.A1.prop3", Matchers.hasSize(2)))
                    .andExpect(jsonPath("key4.A1.prop3[0]").value("#3A"))
                    .andExpect(jsonPath("key4.A1.prop3[1]").value("#3B"))
                    .andReturn();

            String resultJson = directNodeResult.getResponse().getContentAsString();
            logger.debug(resultJson);
        }
        finally {

        }
    }

}
