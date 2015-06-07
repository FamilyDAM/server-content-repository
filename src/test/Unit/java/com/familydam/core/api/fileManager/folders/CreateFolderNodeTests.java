/*
 * Copyright (c) 2015  Mike Nimer & 11:58 Labs
 */

package com.familydam.core.api.fileManager.folders;

import com.familydam.core.FamilyDAM;
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
 * Created by mnimer on 9/22/14.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = FamilyDAM.class)
@WebAppConfiguration
public class CreateFolderNodeTests
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
                .perform(delete(rootUrl + "/~/documents/test"))
                .andReturn();

    }



    @Test
    public void createSimpleFolders() throws Exception
    {
        try {
            //Create folder
            MvcResult photoReq = this.mockMvc
                    .perform(post(rootUrl + "/~/documents/test"))
                    .andReturn();

            Assert.assertTrue(photoReq.getResponse().getStatus() == 201 || photoReq.getResponse().getStatus() == 200);


            // pull a list to see if the post above worked
            MvcResult result = this.mockMvc
                    .perform(get(rootUrl + "/~/documents").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.children").isArray())
                    .andExpect(jsonPath("$.children", Matchers.hasSize(1)))
                    .andExpect(jsonPath("$.children[0].jcr:name").value("test"))
                    .andReturn();

            String resultJson = result.getResponse().getContentAsString();
            logger.debug(resultJson);
        }
        finally {

        }
    }

}
