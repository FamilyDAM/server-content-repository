/*
 * Copyright (c) 2015  Mike Nimer & 11:58 Labs
 */

package com.familydam.core.api.photos;

import com.familydam.core.FamilyDAM;
import com.familydam.core.dao.photosModule.TreeDao;
import com.familydam.core.services.AuthenticatedHelper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.jcr.Session;
import java.util.List;
import java.util.Map;

/**
 * Created by mnimer on 9/17/14.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = FamilyDAM.class)
public class PhotoPeopleListTest
{
    Logger logger = LoggerFactory.getLogger(PhotoPeopleListTest.class);

    @Autowired
    private AuthenticatedHelper authenticatedHelper;
    @Autowired
    private UserDetailsService userDetailsService;
    @Autowired
    private TreeDao treeDao;

    @Value("${server.port}")
    public String port;
    public String rootUrl;



    @Test
    public void checkPeopleList() throws Exception
    {
        Session session = authenticatedHelper.getAdminSession();
        List<Map> tree = treeDao.peopleList(session);

        Assert.assertTrue(tree.size() > 0);
        //logger.debug(tree.toString());
    }

}
