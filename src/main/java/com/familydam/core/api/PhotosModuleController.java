/*
 * Copyright (c) 2015  Mike Nimer & 11:58 Labs
 */

package com.familydam.core.api;

import com.familydam.core.dao.photosModule.TreeDao;
import com.familydam.core.services.AuthenticatedHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.PostConstruct;
import javax.jcr.Session;
import javax.security.sasl.AuthenticationException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * Search for content by jcr node type or mixin
 *
 * Created by mnimer on 12/13/14.
 */
@Controller
@RequestMapping("/api/photos")
public class PhotosModuleController
{
    @Autowired private ApplicationContext applicationContext;

    private AuthenticatedHelper authenticatedHelper = null;

    @PostConstruct
    private void setup()
    {
        authenticatedHelper = applicationContext.getBean(AuthenticatedHelper.class);
    }


    @Autowired
    private TreeDao treeDao;


    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value = "/tree/date", method = RequestMethod.GET)
    public ResponseEntity<Map> getDateTree(HttpServletRequest request,
                                   HttpServletResponse response,
                                    Authentication currentUser_)
    {
        
        Session session = null;
        try {
            session = authenticatedHelper.getSession(currentUser_);

            Map tree = treeDao.dateTree(session);

            return new ResponseEntity<>(tree, HttpStatus.OK);
        }
        catch (AuthenticationException ae) {
            ae.printStackTrace();
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        catch (Exception ae) {
            ae.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        finally {
            if (session != null) {
                session.logout();
            }
        }
    }


    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value = "/list/tags", method = RequestMethod.GET)
    public ResponseEntity<List<Map>> getTagList(HttpServletRequest request,
                                   HttpServletResponse response,
                                    Authentication currentUser_)
    {

        Session session = null;
        try {
            session = authenticatedHelper.getSession(currentUser_);

            List<Map> tree = treeDao.tagList(session);

            return new ResponseEntity<>(tree, HttpStatus.OK);
        }
        catch (AuthenticationException ae) {
            ae.printStackTrace();
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        catch (Exception ae) {
            ae.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        finally {
            if (session != null) {
                session.logout();
            }
        }
    }




    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value = "/list/people", method = RequestMethod.GET)
    public ResponseEntity<List<Map>> getPeopleList(HttpServletRequest request,
                                   HttpServletResponse response,
                                    Authentication currentUser_)
    {

        Session session = null;
        try {
            session = authenticatedHelper.getSession(currentUser_);

            List<Map> tree = treeDao.peopleList(session);

            return new ResponseEntity<>(tree, HttpStatus.OK);
        }
        catch (AuthenticationException ae) {
            ae.printStackTrace();
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        catch (Exception ae) {
            ae.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        finally {
            if (session != null) {
                session.logout();
            }
        }
    }

}

