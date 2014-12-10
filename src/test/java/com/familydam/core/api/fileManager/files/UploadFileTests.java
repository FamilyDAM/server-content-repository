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

import com.familydam.core.FamilyDAM;
import com.familydam.core.FamilyDAMConstants;
import org.apache.commons.codec.binary.Base64;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.core.data.FileDataStore;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.plugins.blob.datastore.DataStoreBlobStore;
import org.apache.jackrabbit.oak.plugins.segment.SegmentNodeStore;
import org.apache.jackrabbit.oak.plugins.segment.file.FileStore;
import org.apache.jackrabbit.oak.spi.blob.BlobStore;
import org.apache.jackrabbit.oak.spi.blob.FileBlobStore;
import org.apache.jackrabbit.oak.spi.state.NodeStore;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.fileUpload;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by mnimer on 9/17/14.
 */
@IntegrationTest({"server.port=9000"})
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = FamilyDAM.class)
public class UploadFileTests
{
    Logger logger = LoggerFactory.getLogger(UploadFileTests.class);

    //@Autowired
    public WebApplicationContext wac;

    private MockMvc mockMvc;

    @Value("${server.port}")
    public String port;
    public String rootUrl;

    @Autowired private Oak oak;



    //@Before
    public void setupMock() throws Exception
    {
        rootUrl = "http://localhost:" +port;

        this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }


    //@After
    public void tearDown() throws Exception{
        this.mockMvc
                .perform(delete(rootUrl + "/~/documents/test/AppleiPhone4.jpeg"))
                .andReturn();
        this.mockMvc
                .perform(delete(rootUrl + "/~/documents/test"))
                .andReturn();
        this.mockMvc
                .perform(delete(rootUrl + "/~/documents/test/AppleiPhone4.jpeg"))
                .andReturn();
    }

    @Test
    public void jcrCopy() throws Exception
    {
        String dir = "/~/documents";
        String dirPath = dir.replace("~", FamilyDAMConstants.DAM_ROOT);

        SimpleCredentials credentials = new SimpleCredentials("admin", "admin".toCharArray());
        Repository repository = JcrUtils.getRepository();
        Session session = repository.login(credentials, null);


        /////////////////////////////////////////
        Node docs1 = session.getNode("/");
        docs1.refresh(true);
        Iterable<Node> children1 = JcrUtils.getChildNodes(docs1);

        for (Node child : children1) {
            child.toString();
        }
        ////////////////////////////////////////


        URL fileUrl = this.getClass().getClassLoader().getResource("images/AppleiPhone4.jpeg");
        //URL fileUrl = this.getClass().getClassLoader().getResource("images/FujiFilmFinePixS1Pro.jpg");
        String path = fileUrl.getPath();
        File file = new File(path);


        Node root = session.getNode("/");

        Node copyToDir = JcrUtils.getOrCreateByPath(dirPath, JcrConstants.NT_FOLDER, session);

        String fileName = file.getName();

        // first use the java lib, to get the mime type
        String mimeType = "image/jpg";

        Node testFolder = JcrUtils.getOrAddFolder(root, "test");
        Node fileNode = JcrUtils.putFile(copyToDir, fileName, "application/octet-stream", new BufferedInputStream(new FileInputStream(file)) );
        //Node fileNode = JcrUtils.putFile(copyToDir, fileName, "application/octet-stream", new BufferedInputStream(new FileInputStream(file)) );
        //fileNode.setProperty(JcrConstants.JCR_UUID, UUID.randomUUID().toString());
        //fileNode.setProperty(JcrConstants.JCR_CREATED, session.getUserID());

        //fileNode.getParent().save();

        session.save();


        /////////////////////////////////////////
        Node docs = session.getNode(dirPath);
        Iterable<Node> children = JcrUtils.getChildNodes(docs);
        int count = 0;

        for (Node child : children) {
            count++;
        }

        Assert.assertEquals(1, count);
        session.logout();

        /////////////////////////////////////////
        Session session2 = repository.login(credentials, null);
        Node root2 = session2.getNode("/");
        Node docs2 = session2.getNode(dirPath);
        Iterable<Node> children2 = JcrUtils.getChildNodes(root2);
        Iterable<Node> children3 = JcrUtils.getChildNodes(docs2);
        int count2 = 0;
        int count3 = 0;

        for (Node child : children2) {
            count2++;
        }
        for (Node child : children3) {
            count3++;
        }

        Assert.assertEquals(1, count2);
    }


/*****
    @Ignore
    @Test
    public void copyFile() throws Exception
    {
        try {
            URL fileUrl = this.getClass().getClassLoader().getResource("images/AppleiPhone4.jpeg");
            //MockMultipartFile testImage = new MockMultipartFile("AppleiPhone4.jpeg", "AppleiPhone4.jpeg", "image/jpg", is);




            String basicDigestHeaderValue = "Basic " + new String(Base64.encodeBase64(("admin:admin").getBytes()));

            MvcResult photoReq = this.mockMvc
                    .perform(post(rootUrl + "/api/import/copy/").accept("application/json")
                            .header("Authorization", basicDigestHeaderValue)
                            .param("path", fileUrl.toURI().getPath())
                            .param("dir", "/~/documents/test"))
                    .andReturn();

            Assert.assertTrue(photoReq.getResponse().getStatus() == 201 || photoReq.getResponse().getStatus() == 200);


            MvcResult folderList = this.mockMvc
                    .perform(get(rootUrl + "/~/documents/test").accept(MediaType.APPLICATION_JSON)
                            .header("Authorization", basicDigestHeaderValue))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.children").isArray())
                    .andExpect(jsonPath("$.children", Matchers.hasSize(1)))
                    .andReturn();

            MvcResult result = this.mockMvc
                    .perform(get(rootUrl + "/~/documents/test/AppleiPhone4.jpeg")
                            .accept(MediaType.IMAGE_JPEG)
                            .header("Authorization", basicDigestHeaderValue))
                    .andExpect(status().isOk())
                    .andReturn();

            byte[] _result = result.getResponse().getContentAsByteArray();
            Assert.assertEquals(338025, _result.length);

            //Assert.assertTrue(resultJson.equals("[\"/photos\"]"));

        }
        finally {

        }
    }

****/
}
