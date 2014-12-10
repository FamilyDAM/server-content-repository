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
import com.familydam.core.helpers.MimeTypeManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.oak.api.Blob;
import org.apache.jackrabbit.oak.api.ContentSession;
import org.apache.jackrabbit.oak.api.Root;
import org.apache.jackrabbit.oak.api.Tree;
import org.apache.jackrabbit.oak.api.Type;
import org.apache.jackrabbit.oak.commons.PathUtils;
import org.apache.jackrabbit.oak.plugins.segment.SegmentNodeBuilder;
import org.apache.jackrabbit.oak.spi.state.NodeBuilder;
import org.apache.jackrabbit.oak.spi.state.NodeStore;
import org.apache.jackrabbit.oak.util.NodeUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.security.auth.login.LoginException;
import javax.security.sasl.AuthenticationException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * A number of useful messages to use
 * <p/>
 * Created by mnimer on 10/14/14.
 */
@Controller
@RequestMapping("/api/import")
public class ImportController extends AuthenticatedService
{

    //@Autowired
    private NodeStore nodeStore;


    @RequestMapping(value = "/info")
    public ResponseEntity<Object> info(HttpServletRequest request, HttpServletResponse response,
                                       @RequestBody Map props) throws LoginException, NoSuchWorkspaceException
    {
        String path = (String) props.get("path");

        File _f = new File(path);

        Map info = new HashMap();
        info.put("visible", _f.exists());
        info.put("isDirectory", _f.isDirectory());
        info.put("path", path);

        return new ResponseEntity<Object>(info, HttpStatus.OK);
    }


    //@RequestMapping(value = "/copy")
    public ResponseEntity<Object> copy(HttpServletRequest request, HttpServletResponse response,
                                       @RequestBody Map props) throws LoginException, NoSuchWorkspaceException
    {
        String dir = (String) props.get("dir");
        String path = (String) props.get("path");
        Boolean recursive = (Boolean) props.get("recursive");
        Assert.notNull(dir);
        Assert.notNull(path);


        return copyTest(request, response, dir, path);
        //return copyLocalFile(request, response, dir, path);
        //return copyLocalFileWithNodeBuilder(request, response, dir, path);
    }


    @RequestMapping(value = "/copy")
    public ResponseEntity<Object> copyWithParams(HttpServletRequest request, HttpServletResponse response,
                                       @RequestParam(value = "dir", required = false) String dir,
                                       @RequestParam(value = "path", required = false) String path) throws LoginException, NoSuchWorkspaceException, IOException
    {
        if( dir == null && path == null ){
            // check body for json packet
            String json = IOUtils.toString(request.getInputStream());
            if( json != null ){
                ObjectMapper mapper = new ObjectMapper();
                JsonNode props = mapper.readTree(json);
                dir = props.path("dir").asText();
                path = props.path("path").asText();
            }
        }

        return copyTest(request, response, dir, path);
        //return copyLocalFile(request, response, props);
        //return copyLocalFileWithNodeBuilder(request, response, props);
    }


    public ResponseEntity<Object> copyLocalFile(HttpServletRequest request, HttpServletResponse response,
                                                String dir, String path) throws LoginException, NoSuchWorkspaceException
    {

        try (ContentSession session = getSession(request)) {
            File file = new File(path);


            if (!file.exists()) {
                return new ResponseEntity<Object>(HttpStatus.NOT_FOUND);
            } else if (file.isFile()) {
                Root root = session.getLatestRoot();
                Tree contentRoot = getContentRoot(session);
                Tree dirTree = getRelativeTree(contentRoot, dir);


                NodeUtil nodeDir;
                NodeUtil newNode;
                try {

                    String fileName = file.getName();

                    /**
                    if (!dirTree.exists()) {
                        nodeDir = new NodeUtil(root.getTree("/")).getOrAddTree(dirTree.getPath(), JcrConstants.NT_FOLDER);
                        root.commit();
                    } else {
                        nodeDir = new NodeUtil(dirTree);
                    }
                     **/

                    String relativePath = dir.replace("/~", "");
                    Iterable<String> pathElements = PathUtils.elements(relativePath);
                    for (String name : pathElements) {
                        if( contentRoot.hasChild(name) ) {
                            dirTree = dirTree.getChild(name);
                        }else{
                            dirTree = dirTree.addChild(name);
                        }
                    }


                    /**
                    newNode = nodeDir.getChild(fileName);
                    if (newNode == null) {
                        newNode = nodeDir.addChild(fileName, JcrConstants.NT_FILE);
                        newNode.setString(JcrConstants.JCR_UUID, UUID.randomUUID().toString());
                    }
                    newNode.setString(JcrConstants.JCR_NAME, fileName);
                    newNode.setString(JcrConstants.JCR_CREATED, session.getAuthInfo().getUserID());


                    // first use the java lib, to get the mime type
                    String mimeType = Files.probeContentType(file.toPath());
                    if (mimeType != null) {
                        newNode.setString(JcrConstants.JCR_MIMETYPE, mimeType);
                    } else {
                        //default to our local check (based on file extension)
                        String type = MimeTypeManager.getMimeType(fileName);
                        newNode.setString(JcrConstants.JCR_MIMETYPE, type);
                    }

                    // set file contents
                    Value[] content = new Value[1];
                    InputStream is = new FileInputStream(file);
                    content[0] = new BinaryValue(is);
                    newNode.setValues(JcrConstants.JCR_CONTENT, content);
                    root.commit();
                    session.getLatestRoot().commit();

                    System.out.println("File Copied:" + newNode.getTree().getPath());
                     **/
                    MultiValueMap headers = new HttpHeaders();
                    //headers.add("location", newNode.getTree().getPath().replace("/dam/", "/~/"));
                    return new ResponseEntity<Object>(headers, HttpStatus.CREATED);
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                    return new ResponseEntity<Object>(ex, HttpStatus.INTERNAL_SERVER_ERROR);
                }
            } else if (file.isDirectory()) {
                //todo recursively copy everything in the dir.
            }

        }
        catch (AuthenticationException ae) {
            return new ResponseEntity<Object>(HttpStatus.UNAUTHORIZED);
        }
        catch (IOException ex) {
            return new ResponseEntity<Object>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return new ResponseEntity<Object>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<Object>(HttpStatus.NOT_FOUND);
    }


    public ResponseEntity<Object> copyLocalFileWithNodeBuilder(HttpServletRequest request, HttpServletResponse response,
                                                               String dir, String path) throws LoginException, NoSuchWorkspaceException
    {

        try (ContentSession session = getSession(request)) {
            File file = new File(path);


            if (!file.exists()) {
                return new ResponseEntity<Object>(HttpStatus.NOT_FOUND);
            } else if (file.isFile()) {
                Root root = session.getLatestRoot();
                Tree rootTree = getContentRoot(session);
                Tree dirTree = getRelativeTree(rootTree, dir);

                NodeBuilder builder = nodeStore.getRoot().builder();
                NodeBuilder newNodeBuilder = builder.getChildNode(rootTree.getName());

                // create nodes for path.
                String[] paths = dir.replace("/~", "").split("\\/");
                for (String s : paths) {
                    if (s.length() > 0) {
                        //newNodeBuilder = newNodeBuilder.getChildNode(s);
                    }
                }

                Iterable<String> pathElements = PathUtils.elements(dir.replace("/~", ""));
                for (String name : pathElements) {
                    if( newNodeBuilder.hasChildNode(name) ) {
                        newNodeBuilder = newNodeBuilder.getChildNode(name);
                    }else{
                        newNodeBuilder = newNodeBuilder.child(name);
                    }
                }


                String fileName = file.getName();
                NodeBuilder fileNode = null;
                if( newNodeBuilder.hasChildNode(fileName) ) {
                    fileNode = newNodeBuilder.getChildNode(fileName);
                }else{
                    fileNode = newNodeBuilder.child(fileName);
                }


                fileNode.setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FILE, Type.NAME);
                NodeBuilder content = fileNode.child(JcrConstants.JCR_CONTENT);
                content.setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_RESOURCE);

                // first use the java lib, to get the mime type
                String mimeType = Files.probeContentType(file.toPath());
                if (mimeType != null) {
                    content.setProperty(JcrConstants.JCR_MIMETYPE, mimeType);
                } else {
                    //default to our local check (based on file extension)
                    String type = MimeTypeManager.getMimeType(fileName);
                    content.setProperty(JcrConstants.JCR_MIMETYPE, type);
                }

                Blob blob = nodeStore.createBlob(new FileInputStream(file));
                content.setProperty(JcrConstants.JCR_DATA, blob);

                root.commit();
                //session.getLatestRoot().commit();


                MultiValueMap headers = new HttpHeaders();
                //headers.add("location", newNode.getTree().getPath().replace("/dam/", "/~/"));
                headers.add("location", ((SegmentNodeBuilder) fileNode).getPath());
                return new ResponseEntity<Object>(headers, HttpStatus.CREATED);

            } else if (file.isDirectory()) {
                //todo recursively copy everything in the dir.
            }

        }
        catch (AuthenticationException ae) {
            return new ResponseEntity<Object>(HttpStatus.UNAUTHORIZED);
        }
        catch (IOException ex) {
            return new ResponseEntity<Object>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return new ResponseEntity<Object>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<Object>(HttpStatus.NOT_FOUND);
    }


    public ResponseEntity<Object> copyTest(HttpServletRequest request, HttpServletResponse response,
                                           String dir, String path) throws LoginException, NoSuchWorkspaceException
    {
        try {
            Session session = getRepositorySession(request, response);

            Node root = session.getNode("/");
            //Node contentRoot = root.getNode(FamilyDAMConstants.DAM_ROOT);
            String dirPath = dir.replace("~", FamilyDAMConstants.DAM_ROOT);

            Node copyToDir = JcrUtils.getOrCreateByPath(dirPath, JcrConstants.NT_FOLDER, session);




            File file = new File(path);

            if (!file.exists()) {
                return new ResponseEntity<Object>(HttpStatus.NOT_FOUND);
            } else if (file.isFile()) {

                try {

                    String fileName = file.getName();

                    // first use the java lib, to get the mime type
                    String mimeType = Files.probeContentType(file.toPath());
                    if (mimeType == null) {
                        //default to our local check (based on file extension)
                        mimeType = MimeTypeManager.getMimeType(fileName);
                    }

                    Node fileNode = JcrUtils.putFile(copyToDir, fileName, "application/octet-stream", new BufferedInputStream(new FileInputStream(file)) );
                    //fileNode.setProperty(JcrConstants.JCR_UUID, UUID.randomUUID().toString());
                    //fileNode.setProperty(JcrConstants.JCR_CREATED, session.getUserID());

                    //fileNode.getParent().save();
                    session.save();

                    MultiValueMap headers = new HttpHeaders();
                    //headers.add("location", newNode.getTree().getPath().replace("/dam/", "/~/"));
                    return new ResponseEntity<Object>(headers, HttpStatus.CREATED);
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                    return new ResponseEntity<Object>(ex, HttpStatus.INTERNAL_SERVER_ERROR);
                }
            } else if (file.isDirectory()) {
                //todo recursively copy everything in the dir.
            }

        }
        catch (AuthenticationException ae) {
            ae.printStackTrace();
            return new ResponseEntity<Object>(HttpStatus.UNAUTHORIZED);
        }
        catch (IOException ex) {
            ex.printStackTrace();
            return new ResponseEntity<Object>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return new ResponseEntity<Object>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<Object>(HttpStatus.NOT_FOUND);
    }
}
