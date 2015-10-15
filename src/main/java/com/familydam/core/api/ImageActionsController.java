/*
 * Copyright (c) 2015  Mike Nimer & 11:58 Labs
 */

package com.familydam.core.api;

import com.familydam.core.services.AuthenticatedHelper;
import com.familydam.core.services.ImageRenditionsService;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.Reactor;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;

/**
 * Methods to work with the actual binary data of an nt:file / jcr:content node.
 *
 * Created by mnimer on 9/16/14.
 */
@Controller
@RequestMapping(value = "/api/")
public class ImageActionsController
{
    private Log log = LogFactory.getLog(this.getClass());

    @Autowired
    private AuthenticatedHelper authenticatedHelper;

    @Autowired private Reactor reactor;
    @Autowired private ImageRenditionsService imageRenditionsService;


    /**
     * List all files & directory in a directory visible by a user.
     *
     * @param request
     * @param response
     * @param path
     * @return
     * @throws RepositoryException
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value = "/{id}/base64", method = RequestMethod.GET)
    public ResponseEntity<String> getBase64Url(
            HttpServletRequest request, HttpServletResponse response,
            @org.springframework.security.core.annotation.AuthenticationPrincipal Authentication currentUser_,
            @PathVariable(value = "id") String id)
            throws RepositoryException
    {
        Session session = null;
        try {
            session = authenticatedHelper.getSession(currentUser_);
            //Node root = session.getRootNode();
            Node node = session.getNodeByIdentifier(id);

            InputStream image = JcrUtils.readFile(node);
            String _base64 = "data:image/jpg;base64," +Base64.encodeBase64String(IOUtils.toByteArray(image));

            return new ResponseEntity<>(_base64, HttpStatus.OK);
        }
        catch (Exception ex) {
            log.error(ex);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        finally {
            if (session != null) {
                session.logout();
            }
        }
    }



}
