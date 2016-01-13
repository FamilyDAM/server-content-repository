/*
 * Copyright (c) 2015  Mike Nimer & 11:58 Labs
 */

package com.familydam.core.api;

import com.familydam.core.FamilyDAMConstants;
import com.familydam.core.helpers.NodeMapper;
import com.familydam.core.helpers.PropertyUtil;
import com.familydam.core.models.INode;
import com.familydam.core.services.AuthenticatedHelper;
import com.familydam.core.services.ImageRenditionsService;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.oak.api.CommitFailedException;
import org.apache.jackrabbit.oak.api.PropertyState;
import org.apache.jackrabbit.oak.api.Tree;
import org.apache.jackrabbit.oak.api.Type;
import org.apache.jackrabbit.oak.plugins.value.BinaryBasedBlob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.Reactor;
import reactor.event.Event;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Methods to work with the actual binary data of an nt:file / jcr:content node.
 *
 * Created by mnimer on 9/16/14.
 */
@Controller
@RequestMapping(value = "/api/files/")
public class FileController
{
    private Log log = LogFactory.getLog(this.getClass());

    @Autowired private Reactor reactor;
    @Autowired private ImageRenditionsService imageRenditionsService;
    @Autowired private ApplicationContext applicationContext;

    private AuthenticatedHelper authenticatedHelper = null;

    @PostConstruct
    private void setup()
    {
        authenticatedHelper = applicationContext.getBean(AuthenticatedHelper.class);
    }


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
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ResponseEntity<List<INode>> getFileList(
            HttpServletRequest request, HttpServletResponse response,
            @org.springframework.security.core.annotation.AuthenticationPrincipal Authentication currentUser_,
            @RequestParam(value = "path", required = false, defaultValue = "/") String path)
            throws RepositoryException
    {
        Session session = null;
        try {
            session = authenticatedHelper.getSession(currentUser_);
            Node contentRoot = session.getNode(path);


            Iterable<Node> _childNodes = JcrUtils.getChildNodes(contentRoot);
            List<INode> childNodes = new ArrayList<>();
            
            for (Node node : _childNodes) {
                if( node.getPrimaryNodeType().getName().equals(JcrConstants.NT_FOLDER)
                    || node.getPrimaryNodeType().getName().equals(JcrConstants.NT_FILE) ) {
                    childNodes.add(NodeMapper.map(node));
                }
            }

            Collections.sort(childNodes, new Comparator<INode>()
            {
                public int compare(INode o1, INode o2)
                {
                    if( o1.getOrder() < o2.getOrder()){
                        return -1;
                    }else if( o1.getOrder() > o2.getOrder()){
                        return 1;
                    }else{
                        return ( o1.getName().toString().compareToIgnoreCase( o2.getName().toString() ));
                    }
                }
            });


            return new ResponseEntity<>(childNodes, HttpStatus.OK);
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


    
    /**
     * Return a FILE InputStream for a specific ID
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
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ResponseEntity<Object> getFileById(HttpServletRequest request,
                                              HttpServletResponse response,
                                              @org.springframework.security.core.annotation.AuthenticationPrincipal Authentication currentUser_,
                                              @PathVariable(value = "id") String id) throws IOException, LoginException, NoSuchWorkspaceException, CommitFailedException
    {
        Session session = null;
        try {
            session = authenticatedHelper.getSession(currentUser_);
            Node node = session.getNodeByIdentifier(id);

            if (node.isNodeType(JcrConstants.NT_FILE)) {

                InputStreamResource inputStreamResource = readFileNode(request, session, node);
                //response.setHeader("content-length", 1000); //todo set.

                HttpHeaders _headers = new HttpHeaders();
                if (node.getNode(JcrConstants.JCR_CONTENT).getProperty(JcrConstants.JCR_MIMETYPE) != null) {
                    _headers.setContentType( MediaType.parseMediaType(node.getNode(JcrConstants.JCR_CONTENT).getProperty(JcrConstants.JCR_MIMETYPE).getString()) );
                } else {
                    _headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                }
                _headers.set("Content-Disposition ", "attachment;filename=" +node.getName());

                return new ResponseEntity(inputStreamResource, _headers, HttpStatus.OK);

            } else {

                // return unstructured node of name/value properties
                Map nodeInfo = PropertyUtil.readProperties(node);
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



    private InputStreamResource readFileNode(HttpServletRequest request, Session session, Node node) throws Exception
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

                if( rendition.startsWith("web") ) {
                    //reactor.notify("image." + FamilyDAMConstants.WEB1024, Event.wrap(imageNode.getPath()));
                    reactor.notify("image.rendition", Event.wrap(imageNode.getPath() +"|" +rendition.substring(rendition.indexOf('.'))));
                }

                // TODO, this is slow, we should move this to the IMPORT process before we store the original
                // Since we are going to load the Original image, as a fallback, We'll rotate it as needed
                BufferedImage rotatedImage = imageRenditionsService.rotateImage(session, node);
                if( rotatedImage != null) {
                    node.setProperty(FamilyDAMConstants.WIDTH, rotatedImage.getWidth());
                    node.setProperty(FamilyDAMConstants.HEIGHT, rotatedImage.getHeight());

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(rotatedImage, "png", baos);
                    InputStream is = new ByteArrayInputStream(baos.toByteArray());
                    return new InputStreamResource(is);
                }

            }
        }

        InputStream is = JcrUtils.readFile(imageNode);
        return new InputStreamResource(is);
    }


    /**
     * Pull out and return the actual file saved in a given node.
     *
     * @param tree
     * @return
     * @throws IOException
     */
    private ResponseEntity<Object> readFileNode(Tree tree) throws IOException
    {
        // Set headers
        final HttpHeaders headers = new HttpHeaders();

        if (tree.getProperty(JcrConstants.JCR_MIMETYPE) != null) {
            headers.setContentType(MediaType.parseMediaType(tree.getProperty(JcrConstants.JCR_MIMETYPE).getValue(Type.STRING)));
        } else {
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        }

        PropertyState state = tree.getProperty(JcrConstants.JCR_CONTENT);
        Type<?> type = state.getType();

        InputStream is = ((BinaryBasedBlob) ((List) state.getValue(type)).get(0)).getNewStream();
        byte[] bytes = IOUtils.toByteArray(is);
        //byte[] bytes = IOUtils.readBytes(is);
        return new ResponseEntity<Object>(bytes, headers, HttpStatus.OK);
    }



    /**
     * Pull out the filename from the header
     *
     * @param part
     * @return
     */
    private String extractFileName(Part part)
    {
        String contentDisp = part.getHeader("content-disposition");
        String[] items = contentDisp.split(";");
        for (String s : items) {
            if (s.trim().startsWith("filename")) {
                return s.substring(s.indexOf("=") + 2, s.length() - 1);
            }
        }
        return "";
    }




}
