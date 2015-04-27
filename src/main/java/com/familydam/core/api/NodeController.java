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

package com.familydam.core.api;

import com.familydam.core.FamilyDAMConstants;
import com.familydam.core.helpers.PropertyUtil;
import com.familydam.core.services.AuthenticatedHelper;
import com.familydam.core.services.ImageRenditionsService;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.oak.api.CommitFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import reactor.event.Event;

import javax.imageio.ImageIO;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Used to return the data for a specific node, with all child nodes. Except JCR:CONTENT (file) node.
 *
 * Created by mnimer on 9/16/14.
 */
@Controller
public class NodeController
{
    @Autowired
    private AuthenticatedHelper authenticatedHelper;


    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired private Reactor reactor;
    @Autowired private ImageRenditionsService imageRenditionsService;



    /**
     * Get the node details by path 
     * @param request
     * @param response
     * @return
     * @throws IOException
     * @throws LoginException
     * @throws NoSuchWorkspaceException
     * @throws CommitFailedException
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value = "/~/**", method = RequestMethod.GET)
    public ResponseEntity<Object> getNodeByPath(
            HttpServletRequest request,
            HttpServletResponse response,
            @AuthenticationPrincipal Authentication currentUser_) throws IOException, LoginException, NoSuchWorkspaceException, CommitFailedException
    {
        Session session = null;
        try {
            session = authenticatedHelper.getSession(currentUser_);
            String _relativePath = request.getRequestURI().replace("%20", " ");
            Node contentRoot = authenticatedHelper.getContentRoot(session, _relativePath);


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
                session.getWorkspace().getVersionManager().checkin(node.getPath());
                session.save();
            }

            session.getWorkspace().getVersionManager().checkout(node.getPath());

            PropertyUtil.writeParametersToNode(node, data_);
            session.save();

            session.getWorkspace().getVersionManager().checkin(node.getPath());


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
                                                 @AuthenticationPrincipal Authentication currentUser_,
                                               @PathVariable(value = "id") String id_) throws IOException, LoginException, NoSuchWorkspaceException, CommitFailedException
    {
        Session session = null;
        try {
            session = authenticatedHelper.getSession(currentUser_);
            Node node = session.getNodeByIdentifier(id_);

            node.remove();
            session.save();

            return new ResponseEntity<>(id_, HttpStatus.OK);
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


    
    private InputStreamResource readFileNode(
            HttpServletRequest request,
            Session session, Node node) throws Exception
    {
        Node imageNode = node;

        // check for a rendition different then the original
        String rendition = request.getParameter("rendition");
        if( rendition != null && node.isNodeType("dam:image"))
        {
            Node thumbnailNode = JcrUtils.getNodeIfExists(node, FamilyDAMConstants.RENDITIONS + "/" + rendition);
            if (thumbnailNode != null) {
                imageNode = thumbnailNode;
            }else{
                if( rendition.equalsIgnoreCase(FamilyDAMConstants.THUMBNAIL200 )) {
                    reactor.notify("image." + FamilyDAMConstants.THUMBNAIL200, Event.wrap(imageNode.getPath()));
                }else if( rendition.equalsIgnoreCase(FamilyDAMConstants.WEB1024 )) {
                    reactor.notify("image." + FamilyDAMConstants.WEB1024, Event.wrap(imageNode.getPath()));
                }

                // TODO, this is slow, we should move this to the IMPORT process before we store the original
                // Since we are going to load the Original image, as a fallback, We'll rotate it as needed
                BufferedImage rotatedImage = imageRenditionsService.rotateImage(session, node);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(rotatedImage, "jpg", baos);
                InputStream is = new ByteArrayInputStream(baos.toByteArray());
                return new InputStreamResource(is);
            }
        }

        InputStream is = JcrUtils.readFile(imageNode);
        return new InputStreamResource(is);
    }


}
