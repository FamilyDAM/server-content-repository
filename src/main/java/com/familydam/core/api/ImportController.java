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
import com.familydam.core.services.AuthenticatedHelper;
import com.familydam.core.services.ImageRenditionsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.util.Text;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.security.auth.login.LoginException;
import javax.security.sasl.AuthenticationException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A number of useful messages to handle importing new files into the JCR
 * <p/>
 * Created by mnimer on 10/14/14.
 */
@Controller
@RequestMapping("/api/import")
public class ImportController
{
    @Autowired
    private AuthenticatedHelper authenticatedHelper;

    
    @Autowired private ImageRenditionsService imageRenditionsService;


    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value = "/info")
    public ResponseEntity<Map> info(HttpServletRequest request, HttpServletResponse response) throws LoginException, NoSuchWorkspaceException
    {
        String path = request.getParameter("path");//(String) props.get("path");

        if( path == null ){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        File _f = new File(path);

        Map info = new HashMap();
        info.put("path", path);
        info.put("visible", _f.exists());
        info.put("isDirectory", _f.isDirectory());
        // pass back any extra properties that were sent
        for (String s : request.getParameterMap().keySet()) {
            info.put(s, request.getParameter(s));    
        }

        return new ResponseEntity<>(info, HttpStatus.OK);
    }


    /**
     * Copy a local file into the JCR 
     * @param request
     * @param response
     * @return
     * @throws LoginException
     * @throws NoSuchWorkspaceException
     * @throws IOException
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value = "/file/upload", method= RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Node> fileUpload(
            HttpServletRequest request,
            HttpServletResponse response,
            @AuthenticationPrincipal Authentication currentUser_) throws LoginException, NoSuchWorkspaceException, IOException, ServletException
    {
        boolean fileExists = false;
        try {
            Session session = authenticatedHelper.getSession(currentUser_);

            // FIND the path
            String _path = request.getParameter("path");

            Node root = session.getNode("/");
            String dirPath = _path.replace("~", FamilyDAMConstants.CONTENT_ROOT);
            if (!dirPath.startsWith("/" + FamilyDAMConstants.CONTENT_ROOT)) {
                dirPath = "/" + FamilyDAMConstants.CONTENT_ROOT + dirPath;
            }

            Node copyToDir = JcrUtils.getOrCreateByPath(dirPath, JcrConstants.NT_FOLDER, session);


            for (Part part : request.getParts()) {
                String _name = part.getName();
                if (_name.equalsIgnoreCase("file")) {
                    String _contentType = part.getContentType();
                    String _fileName = ((Part) part).getSubmittedFileName();

                    InputStream _file = part.getInputStream();

                    // figure out the mime type
                    String mimeType = MimeTypeManager.getMimeTypeForContentType(_contentType);
                    if (mimeType == null) {
                        //default to our local check (based on file extension)
                        mimeType = MimeTypeManager.getMimeType(_fileName);
                    }


                    // Check to see if this is a new node or if we are updating an existing node
                    Node nodeExistsCheck = JcrUtils.getNodeIfExists(copyToDir, _fileName);
                    if (nodeExistsCheck != null) {
                        fileExists = true;
                    }

                    _fileName = cleanFileName(_fileName);

                    // Upload the FILE
                    Node fileNode = JcrUtils.putFile(copyToDir, _fileName, mimeType, new BufferedInputStream(_file));
                    //fileNode.setProperty(JcrConstants.JCR_CREATED, session.getUserID());

                    // save the primary file.
                    session.save();


                    // return a path to the new file, in the location header
                    MultiValueMap headers = new HttpHeaders();
                    headers.add("location", fileNode.getPath().replace("/" + FamilyDAMConstants.CONTENT_ROOT + "/", "/~/")); // return
                    
                    //Node _newNode = session.getNodeByIdentifier(fileNode.getIdentifier());
                    return new ResponseEntity<>(headers, HttpStatus.CREATED); //HttpStatus.CREATED
                }
            }

            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

        }catch(Exception ex){
            ex.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    /**
     * JCR node names have a certain character set, which is actually very broad and includes
     * almost all of unicode minus some special characters such as /, [, ], |, :
     * and * (used to build paths, address same-name siblings etc. in JCR), and it
     * cannot be "." or ".." (obviously).
     * @param fileName_
     * @return
     */
    private String cleanFileName(String fileName_)
    {
        return Text.escapeIllegalJcrChars(fileName_);
    }


    /**
     * Copy a local file into the JCR 
     * @param request
     * @param response
     * @param type
     * @param recursive
     * @param dir
     * @param path
     * @return
     * @throws LoginException
     * @throws NoSuchWorkspaceException
     * @throws IOException
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value = "/file/copy")
    public ResponseEntity<Object> fileCopy(HttpServletRequest request, HttpServletResponse response,
                                           @AuthenticationPrincipal Authentication currentUser_,
                                           @RequestParam(value = "type", required = false, defaultValue = "file") String type,
                                           @RequestParam(value = "recursive", required = false, defaultValue = "true") Boolean recursive,
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

        if( type.equalsIgnoreCase("folder") ) {
            return copyLocalFolder(request, response, dir, recursive);
        } else {
            return copyLocalFile(request, response, dir, path);
        }
    }

    

    /**
     * Recursively copy all files under a folder
     * @param request
     * @param response
     * @param dir
     * @return
     * @throws LoginException
     * @throws NoSuchWorkspaceException
     */
    private ResponseEntity<Object> copyLocalFolder(
            HttpServletRequest request,
            HttpServletResponse response,
            String dir,
            boolean recursive) throws LoginException, NoSuchWorkspaceException
    {
        File folder = new File(dir);
        Assert.assertTrue(folder.exists());

        Collection<File> files = FileUtils.listFiles(folder, null, null);

        for (File file : files) {
            if( file.isDirectory() && recursive ){
                copyLocalFolder(request, response, file.getAbsolutePath(), true);
            }else if( file.isFile() && !file.isHidden() ){
                copyLocalFile(request, response, folder.getAbsolutePath(), file.getName());
            }
        }

        return new ResponseEntity<Object>(HttpStatus.CREATED);
    }


    /**
     * Copy a single file
     * @param request
     * @param response
     * @param dir
     * @param path
     * @return
     * @throws LoginException
     * @throws NoSuchWorkspaceException
     */
    private ResponseEntity<Object> copyLocalFile(
            HttpServletRequest request,
            HttpServletResponse response,
            String dir, String path) throws LoginException, NoSuchWorkspaceException
    {
        boolean fileExists = false;
        Session session = null;
        try {
            session = authenticatedHelper.getRepositorySession(request, response);

            Node root = session.getNode("/");
            String dirPath = dir.replace("~", FamilyDAMConstants.CONTENT_ROOT);
            if( !dirPath.startsWith("/"+FamilyDAMConstants.CONTENT_ROOT) ){
                dirPath = "/" +FamilyDAMConstants.CONTENT_ROOT +dirPath;
            }

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


                    // Check to see if this is a new node or if we are updating an existing node
                    Node nodeExistsCheck = JcrUtils.getNodeIfExists(copyToDir, fileName);
                    if( nodeExistsCheck != null ){
                        fileExists = true;
                    }

                    // Upload the FILE
                    Node fileNode = JcrUtils.putFile(copyToDir, fileName, mimeType, new BufferedInputStream(new FileInputStream(file)) );
                    //fileNode.setProperty(JcrConstants.JCR_CREATED, session.getUserID());


                    // save the primary file.
                    session.save();

                    // trigger any system file handling events
                    //triggerEvents(fileExists, fileNode);

                    // return a path to the new file, in the location header
                    MultiValueMap headers = new HttpHeaders();
                    headers.add("location", fileNode.getPath().replace("/" +FamilyDAMConstants.CONTENT_ROOT +"/", "/~/")); // return
                    return new ResponseEntity<>(headers, HttpStatus.CREATED);
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                    return new ResponseEntity<>(ex, HttpStatus.INTERNAL_SERVER_ERROR);
                }
            } else if (file.isDirectory()) {
                //todo recursively copy everything in the dir.
            }

        }
        catch (AuthenticationException ae) {
            ae.printStackTrace();
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        finally {
            if( session != null ) session.logout();
        }

        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }



}
