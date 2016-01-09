/*
 * Copyright (c) 2015  Mike Nimer & 11:58 Labs
 */

package com.familydam.core.api;

import com.familydam.core.FamilyDAMConstants;
import com.familydam.core.helpers.PropertyUtil;
import com.familydam.core.services.AuthenticatedHelper;
import com.familydam.core.services.ImageRenditionsService;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.oak.api.CommitFailedException;
import org.imgscalr.Scalr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import reactor.core.Reactor;

import javax.annotation.PostConstruct;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Map;

/**
 * Used to return the data for a specific node, with all child nodes. Except JCR:CONTENT (file) node.
 *
 * Created by mnimer on 9/16/14.
 */
@Controller
public class NodeDaoController
{
    @Autowired private ApplicationContext applicationContext;

    private AuthenticatedHelper authenticatedHelper = null;

    @PostConstruct
    private void setup()
    {
        authenticatedHelper = applicationContext.getBean(AuthenticatedHelper.class);
    }


    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired private Reactor reactor;
    @Autowired private ImageRenditionsService imageRenditionsService;



    /**
     * Get a single File or Property map if it is not a nt_file node
     * @param request
     * @param response
     * @return
     * @throws IOException
     * @throws LoginException
     * @throws NoSuchWorkspaceException
     * @throws CommitFailedException
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value = "/dam:files/**", method = RequestMethod.GET)
    public ResponseEntity<Object> getNodeByPath(
            HttpServletRequest request,
            HttpServletResponse response,
            @org.springframework.security.core.annotation.AuthenticationPrincipal Authentication currentUser_) throws IOException, LoginException, NoSuchWorkspaceException, CommitFailedException
    {
        Session session = null;
        try {
            session = authenticatedHelper.getSession(currentUser_);
            String _relativePath = request.getRequestURI().replace("%20", " ").replace("/~/", "/");
            Node contentRoot = session.getNode(_relativePath);


            if (contentRoot.isNodeType(JcrConstants.NT_FILE)) {

                InputStreamResource inputStreamResource = readFileNode(request, session, contentRoot);
                //response.setHeader("content-length", 1000); //todo set.

                HttpHeaders headers = new HttpHeaders();
                MediaType mediaType = MediaType.parseMediaType(contentRoot.getNode(JcrConstants.JCR_CONTENT).getProperty(JcrConstants.JCR_MIMETYPE).getString());
                headers.setContentType(mediaType);
                return new ResponseEntity(inputStreamResource, headers, HttpStatus.OK);

            } else {

                // return unstructured node of name/value properties
                Map nodeInfo = PropertyUtil.readProperties(contentRoot);
                return new ResponseEntity<Object>(nodeInfo, HttpStatus.OK);

            }

        }
        catch (Exception ae) {
            ae.printStackTrace();
            return new ResponseEntity<Object>(HttpStatus.UNAUTHORIZED);
        }
        finally {
            if (session != null) {
                session.logout();
            }
        }
    }


    
    
    /**
     * Return the data for any given node in the JCR by ID
     * @param request
     * @param response
     * @param id
     * @return
     * @throws IOException
     * @throws LoginException
     * @throws NoSuchWorkspaceException
     * @throws CommitFailedException
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value = "/api/data/{id}", method = RequestMethod.GET)
    public ResponseEntity<Object> getNodeById(HttpServletRequest request,
                                              HttpServletResponse response,
                                              @AuthenticationPrincipal Authentication currentUser_,
                                              @PathVariable(value = "id") String id) throws IOException, LoginException, NoSuchWorkspaceException, CommitFailedException
    {
        Session session = null;
        try {
            session = authenticatedHelper.getSession(currentUser_);
            Node node = session.getNodeByIdentifier(id);

            if (node.isNodeType(JcrConstants.NT_FILE)) {

                Map props = PropertyUtil.readProperties(node);

                return new ResponseEntity(props, HttpStatus.OK);

            } else {

                // return unstructured node of name/value properties
                Map nodeInfo = PropertyUtil.readProperties(node);
                return new ResponseEntity<>(nodeInfo, HttpStatus.OK);

            }

        }
        catch (Exception ae) {
            ae.printStackTrace();
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        finally {
            if (session != null) {
                session.logout();
            }
        }
    }






    /**
     * Hard delete of a node and all children under it
     *
     * @param request
     * @return
     * @throws IOException
     * @throws LoginException
     * @throws NoSuchWorkspaceException
     * @throws CommitFailedException
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value = "/api/data/{id}", method = RequestMethod.POST)
    public ResponseEntity<String> updateNodeById(HttpServletRequest request, HttpServletResponse response,
                                                 @AuthenticationPrincipal Authentication currentUser_,
                                               @PathVariable(value = "id") String id_,
                                                 @RequestBody Map data_
    ) throws IOException, LoginException, NoSuchWorkspaceException, CommitFailedException
    {
        Session session = null;
        try {
            session = authenticatedHelper.getSession(currentUser_);
            Node node = session.getNodeByIdentifier(id_);

            if( node.isCheckedOut() ){
                //session.getWorkspace().getVersionManager().checkin(node.getPath());
                //session.save();
            }

            //session.getWorkspace().getVersionManager().checkout(node.getPath());

            PropertyUtil.writeParametersToNode(node, data_);
            session.save();

            //session.getWorkspace().getVersionManager().checkin(node.getPath());


            return new ResponseEntity<>(id_, HttpStatus.OK);
        }
        catch (RepositoryException re) {
            re.printStackTrace();
            return new ResponseEntity<>(re.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        catch (Exception ae) {
            return new ResponseEntity<>(id_, HttpStatus.UNAUTHORIZED);
        }
        finally {
            if (session != null) {
                session.logout();
            }
        }
    }






    /**
     * Hard delete of a node and all children under it
     *
     * @param request
     * @return
     * @throws IOException
     * @throws LoginException
     * @throws NoSuchWorkspaceException
     * @throws CommitFailedException
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value = "/api/data/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<String> deleteNodeById(HttpServletRequest request, HttpServletResponse response,
                                                 @org.springframework.security.core.annotation.AuthenticationPrincipal Authentication currentUser_,
                                               @PathVariable(value = "id") String id_) throws IOException, LoginException, NoSuchWorkspaceException, CommitFailedException
    {
        Session session = null;
        try {
            session = authenticatedHelper.getSession(currentUser_);
            Node node = session.getNodeByIdentifier(id_);

            // only allow the deletion of user nodes
            if( node.isNodeType("dam:userfolder") || node.isNodeType("dam:file") ) {

                // special check, so we can add _source prop
                if( !node.isNodeType("dam:extensible") ){
                    node.addMixin("dam:extensible");
                    session.save();
                }

                node.setProperty("_trashSource", node.getPath());
                node.setProperty("_trashDate", Calendar.getInstance());

                //todo check delete permission first
                Node trashDest = session.getNode("/" +FamilyDAMConstants.SYSTEM_ROOT +"/").getNode("trash");

                Node checkExisting = JcrUtils.getNodeIfExists(trashDest.getPath() + "/" + node.getName(), session);
                if (checkExisting != null) {
                    // item in the trash with the same name/path. So we'll delete the old item so we can replace it.
                    checkExisting.remove();
                    session.save();
                }

                session.move(node.getPath(), trashDest.getPath() + "/" + node.getName());
                session.save();

                return new ResponseEntity<>(id_, HttpStatus.OK);
            }else{
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }
        }
        catch (Exception ae) {
            ae.printStackTrace();
            return new ResponseEntity<>(id_, HttpStatus.UNAUTHORIZED);
        }
        finally {
            if (session != null) {
                session.logout();
            }
        }
    }


    
    private InputStreamResource readFileNode(
            HttpServletRequest request,
            Session session, Node node) throws Exception
    {
        Node imageNode = node;

        // check for a rendition different then the original
        String rendition = request.getParameter("rendition");
        if( rendition != null && node.isNodeType("dam:image"))
        {
            Number size = new Integer(rendition.split("\\.")[1]);

            Node thumbnailNode = JcrUtils.getNodeIfExists(node, FamilyDAMConstants.RENDITIONS + "/" + rendition);
            if (thumbnailNode != null) {
                imageNode = thumbnailNode;
            }else{

                // TODO, this is slow, we should move this to the IMPORT process before we store the original
                // Since we are going to load the Original image, as a fallback, We'll rotate it as needed
                BufferedImage rotatedImage = imageRenditionsService.rotateImage(session, node);

                BufferedImage scaledImage = imageRenditionsService.scaleImage(session, node, rotatedImage, size.intValue(), Scalr.Method.AUTOMATIC);

                String renditionPath = imageRenditionsService.saveRendition(session, node, rendition, scaledImage, "PNG");
                session.save();

                imageNode = session.getNode(renditionPath);
            }
        }

        InputStream is = JcrUtils.readFile(imageNode);
        return new InputStreamResource(is);
    }


}
