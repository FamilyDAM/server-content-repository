/*
 * Copyright (c) 2015  Mike Nimer & 11:58 Labs
 */

package com.familydam.core.api.fileManager;

import com.familydam.core.FamilyDAM;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by mnimer on 9/17/14.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = FamilyDAM.class)
@WebAppConfiguration
public class BadDirTest
{
    Logger logger = LoggerFactory.getLogger(BadDirTest.class);

    @Autowired
    public WebApplicationContext wac;

    private MockMvc mockMvc;

    //@Value("${server.port}")
    public String port;
    public String rootUrl;


    @Before
    public void setupMock() throws Exception
    {
        port = wac.getEnvironment().getProperty("server.port");
        rootUrl = "http://localhost:" +port;

        this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }



    @Test
    public void getBadDirectoryList() throws Exception
    {
        MvcResult result = this.mockMvc
                .perform(get(rootUrl + "/~/foobar"))
                .andExpect(status().isNotFound())
                .andReturn();

        String resultJson = result.getResponse().getContentAsString();
        logger.debug(resultJson);
    }


}
