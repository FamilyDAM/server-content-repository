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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
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

    @RequestMapping(value = "/info")
    public ResponseEntity<Object> info(HttpServletRequest request, HttpServletResponse response,
                                       @RequestBody Map props) throws LoginException, NoSuchWorkspaceException
    {
        String path = (String) props.get("path");

        File _f = new File(path);

        Map info = new HashMap();
        info.put("path", path);
        info.put("visible", _f.exists());
        info.put("isDirectory", _f.isDirectory());

        return new ResponseEntity<Object>(info, HttpStatus.OK);
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

        return copyLocalFile(request, response, dir, path);
    }



    public ResponseEntity<Object> copyLocalFile(
            HttpServletRequest request,
            HttpServletResponse response,
            String dir, String path) throws LoginException, NoSuchWorkspaceException
    {
        Session session = null;
        try {
            session = getRepositorySession(request, response);

            Node root = session.getNode("/");
            String dirPath = dir.replace("~", FamilyDAMConstants.DAM_ROOT);

            Node copyToDir = JcrUtils.getOrCreateByPath(dirPath, JcrConstants.NT_FOLDER, session);


            File file = new File(path);

            if (!file.exists()) {
                return new ResponseEntity<Object>(HttpStatus.NOT_FOUND);
            }
            else if (file.isFile()) {

                try {

                    String fileName = file.getName();

                    // first use the java lib, to get the mime type
                    String mimeType = Files.probeContentType(file.toPath());
                    if (mimeType == null) {
                        //default to our local check (based on file extension)
                        mimeType = MimeTypeManager.getMimeType(fileName);
                    }

                    Node fileNode = JcrUtils.putFile(copyToDir, fileName, mimeType, new BufferedInputStream(new FileInputStream(file)) );
                    //fileNode.setProperty(JcrConstants.JCR_CREATED, session.getUserID());

                    // apply mixins
                    applyMixins(mimeType, fileNode);

                    session.save();

                    MultiValueMap headers = new HttpHeaders();
                    headers.add("location", fileNode.getPath().replace("/" +FamilyDAMConstants.DAM_ROOT +"/", "/~/")); // return
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
        finally {
            if( session != null ) session.logout();
        }

        return new ResponseEntity<Object>(HttpStatus.NOT_FOUND);
    }


    private void applyMixins(String mimeType, Node fileNode) throws RepositoryException
    {
        //first assign the right mixins
        // supports String[] TAGS
        fileNode.addMixin("dam:taggable");
        // catch all to allow any property
        fileNode.addMixin("dam:extensible");


        // Check the mime type to decide if it's more then a generic file
        if(MimeTypeManager.isSupportedImageMimeType(mimeType))
        {
            fileNode.addMixin("dam:image");
            // add default sub-nodes
            if( !fileNode.hasNode(FamilyDAMConstants.METADATA) ) {
                fileNode.addNode(FamilyDAMConstants.METADATA, NodeType.NT_FOLDER);
            }
            if( !fileNode.hasNode(FamilyDAMConstants.RENDITIONS) ) {
                fileNode.addNode(FamilyDAMConstants.RENDITIONS, NodeType.NT_FOLDER);
            }
        }
    }
}
