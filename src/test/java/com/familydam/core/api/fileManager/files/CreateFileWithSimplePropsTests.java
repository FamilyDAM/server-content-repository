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

package com.familydam.core.api.fileManager.files;

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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.InputStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.fileUpload;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by mnimer on 9/17/14.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
public class CreateFileWithSimplePropsTests
{
    Logger logger = LoggerFactory.getLogger(CreateFileWithSimplePropsTests.class);

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
    public void tearDown() throws Exception{
        this.mockMvc
                .perform(delete(rootUrl + "/~/documents/test2/AppleiPhone4.jpeg"))
                .andReturn();
        this.mockMvc
                .perform(delete(rootUrl + "/~/documents/test2"))
                .andReturn();
    }



    @Test
    public void createFileWithSimpleProperties() throws Exception
    {
        try {
            InputStream is = this.getClass().getClassLoader().getResourceAsStream("images/AppleiPhone4.jpeg");
            MockMultipartFile testImage = new MockMultipartFile("AppleiPhone4.jpeg", "AppleiPhone4.jpeg", "image/jpg", is);

            MvcResult photoReq = this.mockMvc
                    .perform(fileUpload(rootUrl + "/~/documents/test2").file(testImage)
                            .param("key1", "key1 value")
                            .param("key2", "key2 value")
                            .param("key3", "key3 value"))
                    .andReturn();

            Assert.assertTrue(photoReq.getResponse().getStatus() == 201 || photoReq.getResponse().getStatus() == 200);


            MvcResult folderList = this.mockMvc
                    .perform(get(rootUrl + "/~/documents/test2").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.children").isArray())
                    .andExpect(jsonPath("$.children", Matchers.hasSize(1)))
                    .andExpect(jsonPath("$.children[0].key1").value("key1 value"))
                    .andExpect(jsonPath("$.children[0].key2").value("key2 value"))
                    .andExpect(jsonPath("$.children[0].key3").value("key3 value"))
                    .andReturn();

            MvcResult result = this.mockMvc
                    .perform(get(rootUrl + "/~/documents/test2/AppleiPhone4.jpeg")
                            .accept(MediaType.IMAGE_JPEG))
                    .andExpect(status().isOk())
                    .andReturn();

            byte[] _result = result.getResponse().getContentAsByteArray();
            Assert.assertEquals(338025, _result.length);

            //Assert.assertTrue(resultJson.equals("[\"/photos\"]"));

        }
        finally {

        }
    }
}
