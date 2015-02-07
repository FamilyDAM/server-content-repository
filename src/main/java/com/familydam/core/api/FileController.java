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
import com.familydam.core.services.ImageRenditionsService;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.oak.api.CommitFailedException;
import org.apache.jackrabbit.oak.api.PropertyState;
import org.apache.jackrabbit.oak.api.Tree;
import org.apache.jackrabbit.oak.api.Type;
import org.apache.jackrabbit.oak.plugins.value.BinaryBasedBlob;
import org.apache.jackrabbit.oak.util.NodeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import reactor.core.Reactor;
import reactor.event.Event;

import javax.imageio.ImageIO;
import javax.jcr.AccessDeniedException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.auth.login.LoginException;
import javax.security.sasl.AuthenticationException;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by mnimer on 9/16/14.
 */
@Controller
@RequestMapping(value = "/api/files/")
public class FileController extends AuthenticatedService
{
    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired private Reactor reactor;
    @Autowired private ImageRenditionsService imageRenditionsService;


    /**
     * List all files in a directory visible by a user.
     *
     * @param request
     * @param response
     * @param path
     * @return
     * @throws RepositoryException
     */
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ResponseEntity<List<Map>> listDirectoryItems(
            HttpServletRequest request, HttpServletResponse response,
            @RequestParam(value = "path", required = false, defaultValue = "/") String path)
            throws RepositoryException
    {
        Session session = null;
        try {
            session = getSession(request, response);
            Node root = session.getRootNode();
            Node contentRoot = getContentRoot(session, path);


            Iterable<Node> _childNodes = JcrUtils.getChildNodes(contentRoot);
            List<Map> childNodes = new ArrayList<>();
            for (Node node : _childNodes) {
                if (node.getPrimaryNodeType().isNodeType(JcrConstants.NT_FOLDER)
                        || node.getPrimaryNodeType().isNodeType(JcrConstants.NT_FILE)) {

                    // return map for every file
                    Map _node = new HashMap();
                    _node.put("id", node.getIdentifier());
                    _node.put("name", node.getName());
                    _node.put("path", node.getPath().replace("/dam:content/", "/~/"));
                    _node.put("parent", node.getParent().getPath().replace("/dam:content/", "/~/"));
                    _node.put("children", new ArrayList());
                    _node.put("fileType", "unknown");


                    if (node.getPrimaryNodeType().isNodeType(JcrConstants.NT_FOLDER)) {
                        _node.put("type", "folder");
                    } else if (node.isNodeType(JcrConstants.NT_FILE)) {
                        _node.put("type", "file");

                        if( node.isNodeType("dam:image")  ) {
                            _node.put("fileType", "image");
                        }
                    }

                    _node.put("isReadOnly", false);
                    childNodes.add(_node);
                }
            }

            Collections.sort(childNodes, new Comparator<Map>()
            {
                public int compare(Map o1, Map o2)
                {
                    return ((Map)o1).get("name").toString().compareToIgnoreCase( ((Map)o2).get("name").toString() );
                }
            });


            return new ResponseEntity<>(childNodes, HttpStatus.OK);
        }
        catch (AuthenticationException ae) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
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
    @RequestMapping(value = "/binary/{id}", method = RequestMethod.GET)
    public ResponseEntity<Object> getFileById(HttpServletRequest request,
                                              HttpServletResponse response,
                                              @PathVariable(value = "id") String id) throws IOException, LoginException, NoSuchWorkspaceException, CommitFailedException
    {
        Session session = null;
        try {
            session = getSession(request, response);
            Node node = session.getNodeByIdentifier(id);

            if (node.isNodeType(JcrConstants.NT_FILE)) {

                InputStreamResource inputStreamResource = readFileNode(request, session, node);
                //response.setHeader("content-length", 1000); //todo set.

                return new ResponseEntity(inputStreamResource, HttpStatus.OK);

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
                if( rendition.equalsIgnoreCase(FamilyDAMConstants.THUMBNAIL200 )) {
                    reactor.notify("image." + FamilyDAMConstants.THUMBNAIL200, Event.wrap(imageNode.getPath()));
                }else if( rendition.equalsIgnoreCase(FamilyDAMConstants.WEB1024 )) {
                    reactor.notify("image." + FamilyDAMConstants.WEB1024, Event.wrap(imageNode.getPath()));
                }

                // TODO, this is slow, we should move this to the IMPORT process before we store the original
                // Since we are going to load the Original image, as a fallback, We'll rotate it as needed
                BufferedImage rotatedImage = imageRenditionsService.rotateImage(session, node);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(rotatedImage, "png", baos);
                InputStream is = new ByteArrayInputStream(baos.toByteArray());
                return new InputStreamResource(is);
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
     * Create or replace a node
     *
     * @param request
     * @return
     * @throws IOException
     * @throws LoginException
     * @throws NoSuchWorkspaceException
     * @throws CommitFailedException
     */

    private void saveBodyProperties(HttpServletRequest request, NodeUtil newNode) throws IOException
    {
        /**
         * check for and process a JSON body
         */
        String jsonBody = IOUtils.toString(request.getInputStream());
        if (jsonBody.length() > 0 && jsonBody.startsWith("{") && jsonBody.endsWith("}")) {
            PropertyUtil.writeJsonToNode(newNode, jsonBody);
        }
    }


    private void saveProperties(HttpServletRequest request, NodeUtil newNode) throws AccessDeniedException, CommitFailedException
    {
        /**
         * process any request Parameters
         */
        Map<String, String[]> parameters = request.getParameterMap();
        if (parameters.size() > 0) {
            PropertyUtil.writeParametersToNode(newNode, parameters);
        }
    }


    private void saveFile(MultipartHttpServletRequest request, NodeUtil node) throws AccessDeniedException, IOException, CommitFailedException
    {
        throw new RuntimeException("Not implemented exception");
        //NodeUtil fileNode = PropertyUtil.writeFileToNode(node, request);

        /**
         * process any request Parameters
         */
        //saveProperties(request, fileNode);

        /**
         * check for and process a JSON body
         * todo: is this legal, need to test
         */
        //saveBodyProperties(request, fileNode);

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


    /**
     * Update the properties of a node
     * @param request
     * @param property
     * @param value
     * @return
     * @throws IOException
     * @throws LoginException
     * @throws NoSuchWorkspaceException
     * @throws CommitFailedException

     @RequestMapping(method = RequestMethod.PUT)
     public ResponseEntity<Tree> updateFolder(HttpServletRequest request, String property, Object value) throws IOException, LoginException, NoSuchWorkspaceException, CommitFailedException
     {
     try (ContentSession session = getSession(request)) {
     Root root = session.getLatestRoot();
     Tree tree = getContentRoot(session);


     String jcrPath = request.getRequestURI().substring(2);
     Tree childPath = tree.getChild(jcrPath);
     if (!childPath.exists()) {
     return new ResponseEntity<Tree>(HttpStatus.NOT_FOUND);
     }

     childPath.setProperty(property, value);
     root.commit();

     return new ResponseEntity<Tree>(tree.getChild(jcrPath), HttpStatus.OK);
     }
     catch(AuthenticationException ae){
     return new ResponseEntity<Tree>(HttpStatus.FORBIDDEN);
     }
     finally {
     }
     } */


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
    @RequestMapping(value = "/~/**", method = RequestMethod.DELETE)
    public ResponseEntity<Tree> removeFolder(HttpServletRequest request) throws IOException, LoginException, NoSuchWorkspaceException, CommitFailedException
    {
        throw new RuntimeException("Not implemented exception");
        /***
         try (ContentSession session = getSession(request)) {
         Root root = session.getLatestRoot();
         Tree rootTree = getContentRoot(session);
         Tree tree = getRelativeTree(rootTree, request.getRequestURI());

         if (!tree.exists()) {
         return new ResponseEntity<Tree>(HttpStatus.NOT_FOUND);
         }else {
         tree.remove();
         root.commit();
         }

         return new ResponseEntity<Tree>(HttpStatus.NO_CONTENT);
         }
         catch(AuthenticationException ae){
         return new ResponseEntity<Tree>(HttpStatus.FORBIDDEN);
         }
         finally {
         }
         ***/
    }

}
