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
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.oak.api.CommitFailedException;
import org.apache.jackrabbit.oak.api.ContentSession;
import org.apache.jackrabbit.oak.api.PropertyState;
import org.apache.jackrabbit.oak.api.Root;
import org.apache.jackrabbit.oak.api.Tree;
import org.apache.jackrabbit.oak.api.Type;
import org.apache.jackrabbit.oak.plugins.nodetype.NodeTypeConstants;
import org.apache.jackrabbit.oak.plugins.value.BinaryBasedBlob;
import org.apache.jackrabbit.oak.util.NodeUtil;
import org.apache.jackrabbit.oak.util.TreeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.jcr.NoSuchWorkspaceException;
import javax.security.auth.login.LoginException;
import javax.security.sasl.AuthenticationException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Created by mnimer on 9/16/14.
 */
@Controller
@RequestMapping("/~/**")
public class FileManagerController extends AuthenticatedService
{
    Logger logger = LoggerFactory.getLogger(this.getClass());



    private Tree getContentRoot(ContentSession session) throws LoginException, NoSuchWorkspaceException
    {
        Root root = session.getLatestRoot();
        Tree tree = root.getTree("/");
        return tree.getChild(FamilyDAMConstants.DAM_ROOT);
    }


    private Tree getRelativeTree(Tree root, String relativePath)
    {
        String _path = relativePath.substring(relativePath.indexOf('~') + 1);
        return TreeUtil.getTree(root, _path);
    }


    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<Object> getNode(HttpServletRequest request) throws IOException, LoginException, NoSuchWorkspaceException, CommitFailedException
    {
        try (ContentSession session = getSession(request)) {

            Tree rootTree = getContentRoot(session);
            // walk the tree and get a reference to the requested path, or return a not found status
            Tree tree = getRelativeTree(rootTree, request.getRequestURI());

            if (!tree.exists()) {
                return new ResponseEntity<Object>(HttpStatus.NOT_FOUND);
            }


            if( tree.getProperty(JcrConstants.JCR_PRIMARYTYPE).getValue(Type.STRING) == JcrConstants.NT_FILE ) {

                return readFileNode(tree);

            } else {
                // return unstructured node of name/value properties
                Map nodeInfo = PropertyUtil.readProperties(tree);
                return new ResponseEntity<Object>(nodeInfo, HttpStatus.OK);
            }

        }
        catch(Exception ae){
            return new ResponseEntity<Object>(HttpStatus.UNAUTHORIZED);
        }
    }



    /**
     * Pull out and return the actual file saved in a given node.
     * @param tree
     * @return
     * @throws IOException
     */
    private ResponseEntity<Object> readFileNode(Tree tree) throws IOException
    {
        // Set headers
        final HttpHeaders headers = new HttpHeaders();

        if( tree.getProperty(JcrConstants.JCR_MIMETYPE) != null  ) {
            headers.setContentType( MediaType.parseMediaType(tree.getProperty(JcrConstants.JCR_MIMETYPE).getValue(Type.STRING))  );
        }else{
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        }

        PropertyState state = tree.getProperty(JcrConstants.JCR_CONTENT);
        Type<?> type = state.getType();

        InputStream is = ((BinaryBasedBlob)((List)state.getValue(type)).get(0)).getNewStream();
        byte[] bytes = org.apache.commons.io.IOUtils.toByteArray(is);
        //byte[] bytes = IOUtils.readBytes(is);
        return new ResponseEntity<Object>(bytes, headers, HttpStatus.OK);
    }



    /**
     * Create or replace a node
     * @param request
     * @return
     * @throws IOException
     * @throws LoginException
     * @throws NoSuchWorkspaceException
     * @throws CommitFailedException
     */
    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<Tree> createFolder(HttpServletRequest request) throws IOException, LoginException, NoSuchWorkspaceException, CommitFailedException
    {
        try (ContentSession session = getSession(request)) {
            Root root = session.getLatestRoot();
            Tree rootTree = getContentRoot(session);
            Tree tree = getRelativeTree(rootTree, request.getRequestURI());

            if (!tree.exists()) {

                NodeUtil newNode;
                try {
                    NodeUtil nodeUtil = new NodeUtil(root.getTree("/"));
                    newNode = nodeUtil.getOrAddTree(tree.getPath(), JcrConstants.NT_FOLDER);

                    String name = tree.getPath().substring(tree.getPath().lastIndexOf("/")+1);
                    newNode.setString(JcrConstants.JCR_NAME, name);
                    newNode.setStrings(NodeTypeConstants.JCR_CREATEDBY, "todo");//todo, get userId from auth user - request.getUserPrincipal().getName()


                    /**
                     * Uploaded File(s)
                     * Define and Save NT:FILE nodes
                     */
                    if( request instanceof MultipartHttpServletRequest )
                    {
                        NodeUtil fileNode = PropertyUtil.writeFileToNode(newNode, (MultipartHttpServletRequest) request);


                        /**
                         * process any request Parameters
                         */
                        Map<String, String[]> parameters = request.getParameterMap();
                        if( parameters.size() > 0) {
                            PropertyUtil.writeParametersToNode(fileNode, parameters);
                        }

                        /**
                         * check for and process a JSON body
                         * todo: is this legal, need to test
                         */
                        String jsonBody = IOUtils.toString(request.getInputStream());
                        if( jsonBody.length() > 0 && jsonBody.startsWith("{") && jsonBody.endsWith("}")){
                            PropertyUtil.writeJsonToNode(fileNode, jsonBody);
                        }



                    }else{
                        /**
                         * Create Folders
                         * Define and Save NT:FOLDER nodes
                         */


                        /**
                         * process any request Parameters
                         */
                        Map<String, String[]> parameters = request.getParameterMap();
                        if( parameters.size() > 0) {
                            PropertyUtil.writeParametersToNode(newNode, parameters);
                        }

                        /**
                         * check for and process a JSON body
                         */
                        String jsonBody = IOUtils.toString(request.getInputStream());
                        if( jsonBody.length() > 0 && jsonBody.startsWith("{") && jsonBody.endsWith("}")){
                            PropertyUtil.writeJsonToNode(newNode, jsonBody);
                        }


                    }


                    root.commit();
                }
                catch (Exception ex) {
                    if( logger.isDebugEnabled() ) ex.printStackTrace();
                    logger.error(ex.getMessage(), ex);
                    return new ResponseEntity<Tree>(HttpStatus.INTERNAL_SERVER_ERROR);
                }

            }
            return new ResponseEntity<Tree>(HttpStatus.OK);
        }
        catch(AuthenticationException ae){
            return new ResponseEntity<Tree>(HttpStatus.FORBIDDEN);
        }
        finally {
        }
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
     * @param request
     * @return
     * @throws IOException
     * @throws LoginException
     * @throws NoSuchWorkspaceException
     * @throws CommitFailedException
     */
    @RequestMapping(method = RequestMethod.DELETE)
    public ResponseEntity<Tree> removeFolder(HttpServletRequest request) throws IOException, LoginException, NoSuchWorkspaceException, CommitFailedException
    {
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

    }

}
