/*
 * Copyright (c) 2015  Mike Nimer & 11:58 Labs
 */

package com.familydam.core.api;

import com.familydam.core.FamilyDAMConstants;
import com.familydam.core.helpers.MimeTypeManager;
import com.familydam.core.services.AuthenticatedHelper;
import com.familydam.core.services.ImageRenditionsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.util.Text;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.auth.login.LoginException;
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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * A number of useful messages to handle importing new files into the JCR
 * <p>
 * Created by mnimer on 10/14/14.
 */
@Controller
@RequestMapping("/api/import")
public class ImportController
{
    @Autowired
    private AuthenticatedHelper authenticatedHelper;


    @Autowired
    private ImageRenditionsService imageRenditionsService;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd");

    //@PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value = "/info")
    public ResponseEntity<Map> info(HttpServletRequest request, HttpServletResponse response) throws LoginException, NoSuchWorkspaceException
    {
        String path = request.getParameter("path");//(String) props.get("path");

        if (path == null) {
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
     *
     * @param request
     * @param response
     * @return
     * @throws LoginException
     * @throws NoSuchWorkspaceException
     * @throws IOException
     */
    //@PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value = "/file/upload", method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Node> fileUpload(
            HttpServletRequest request,
            HttpServletResponse response,
            @AuthenticationPrincipal Authentication currentUser_) throws LoginException, NoSuchWorkspaceException, IOException, ServletException
    {
        boolean fileExists = false;
        Session session = null;
        try {
            session = authenticatedHelper.getSession(currentUser_);

            // FIND the path
            String _path = request.getParameter("path");

            String dirPath = _path.replace("~", FamilyDAMConstants.CONTENT_ROOT);
            if (!dirPath.startsWith("/" + FamilyDAMConstants.CONTENT_ROOT)) {
                dirPath = "/" + FamilyDAMConstants.CONTENT_ROOT + dirPath;
            }

            Node _contentRoot = session.getNode("/" +FamilyDAMConstants.CONTENT_ROOT);
            String _relativePath = dirPath.replace("/" +FamilyDAMConstants.CONTENT_ROOT +"/", "");
            Node copyToDir = JcrUtils.getOrCreateByPath(_contentRoot, _relativePath, false, JcrConstants.NT_FOLDER, JcrConstants.NT_FOLDER, true);


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
                    fileNode.addMixin("dam:extensible");
                    //fileNode.setProperty(JcrConstants.JCR_CREATED, session.getUserID());


                    // Set a DAM specific date (as as tring so it's easy to parse later)
                    Property createdDate = fileNode.getProperty(JcrConstants.JCR_CREATED);
                    Calendar dateStamp = Calendar.getInstance();
                    if( createdDate == null ) {
                        dateStamp = createdDate.getDate();
                    }
                    fileNode.setProperty(FamilyDAMConstants.DAM_DATECREATED, dateFormat.format(dateStamp.getTime()));

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

        }
        catch (Exception ex) {
            ex.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        finally {
            if (session != null) {
                session.logout();
            }
        }
    }


    /**
     * Copy a local file into the JCR
     *
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
    //@PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value = "/file/copy", method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Object> fileCopy(HttpServletRequest request, HttpServletResponse response,
                                           @AuthenticationPrincipal Authentication currentUser_,
                                           @RequestParam(value = "type", required = false, defaultValue = "file") String type,
                                           @RequestParam(value = "recursive", required = false, defaultValue = "true") Boolean recursive,
                                           @RequestParam(value = "dir", required = false) String dir,
                                           @RequestParam(value = "path", required = false) String path
    ) throws LoginException, NoSuchWorkspaceException, IOException, RepositoryException
    {
        Session session = null;
        try {
            session = authenticatedHelper.getSession(currentUser_);
            if (dir == null && path == null) {
                // check body for json packet
                String json = IOUtils.toString(request.getInputStream());
                if (json != null) {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode props = mapper.readTree(json);
                    dir = props.path("dir").asText();
                    path = props.path("path").asText();
                }
            }

            File _file = new File(path);
            if (!_file.exists()) {
                return new ResponseEntity<Object>(HttpStatus.NOT_FOUND);
            }

            Node _parent = session.getNode(dir);

            if (_file.isDirectory()) {
                return copyLocalFolder(session, currentUser_, _file, _parent, recursive);
            } else {
                return copyLocalFile(session, currentUser_, _file, _parent);
            }
        }
        finally {
            if (session != null) {
                session.logout();
            }
        }
    }


    /**
     * Recursively copy all files under a folder
     *
     * @param session
     * @param currentUser_
     * @param folder
     * @param parent
     * @param recursive
     * @return
     * @throws LoginException
     * @throws NoSuchWorkspaceException
     * @throws javax.security.sasl.AuthenticationException
     * @throws IOException
     * @throws RepositoryException
     */
    private ResponseEntity<Object> copyLocalFolder(
            Session session,
            Authentication currentUser_,
            File folder,
            Node parent,
            boolean recursive) throws LoginException, NoSuchWorkspaceException, javax.security.sasl.AuthenticationException, IOException, RepositoryException
    {


        String _name = cleanFileName(folder.getName());
        Node _folderNode = JcrUtils.getOrAddNode(parent, _name, JcrConstants.NT_FOLDER);
        session.save();

        File[] files = folder.listFiles();

        for (int i = 0; i < files.length; i++) {
            File _folderOrFile = files[i];

            if (_folderOrFile.isDirectory() && recursive) {
                copyLocalFolder(session, currentUser_, _folderOrFile, _folderNode, true);
            } else if (_folderOrFile.isFile() && !_folderOrFile.isHidden()) {
                copyLocalFile(session, currentUser_, _folderOrFile, _folderNode);
            }
        }


        return new ResponseEntity<Object>(HttpStatus.CREATED);

    }


    /**
     * Copy a single file
     *
     * @param session
     * @param currentUser_
     * @param file
     * @param parent
     * @return
     * @throws LoginException
     * @throws NoSuchWorkspaceException
     * @throws javax.security.sasl.AuthenticationException
     * @throws IOException
     * @throws RepositoryException
     */
    private ResponseEntity<Object> copyLocalFile(
            Session session,
            Authentication currentUser_,
            File file,
            Node parent) throws LoginException, NoSuchWorkspaceException, javax.security.sasl.AuthenticationException, IOException, RepositoryException
    {

        if (!session.isLive()) {
            session = authenticatedHelper.getSession(currentUser_);
        }

        Node root = session.getNode("/");
        //Node copyToDir = JcrUtils.getOrCreateByPath(dirPath, JcrConstants.NT_FOLDER, session);


        String fileName = cleanFileName(file.getName());

        // first use the java lib, to get the mime type
        String mimeType = Files.probeContentType(file.toPath());
        if (mimeType == null) {
            //default to our local check (based on file extension)
            mimeType = MimeTypeManager.getMimeType(fileName);
        }


        // Upload the FILE
        //Node fileNode = JcrUtils.putFile(copyToDir, fileName, mimeType, new BufferedInputStream(new FileInputStream(file)));
        InputStream fileIS = new BufferedInputStream(new FileInputStream(file));
        Node fileNode = JcrUtils.putFile(parent, fileName, mimeType, fileIS);
        // save the primary file.
        session.save();


        // return a path to the new file, in the location header
        MultiValueMap headers = new HttpHeaders();
        headers.add("location", fileNode.getPath()); // return
        return new ResponseEntity<>(headers, HttpStatus.CREATED);


    }


    /**
     * JCR node names have a certain character set, which is actually very broad and includes
     * almost all of unicode minus some special characters such as /, [, ], |, :
     * and * (used to build paths, address same-name siblings etc. in JCR), and it
     * cannot be "." or ".." (obviously).
     *
     * @param fileName_
     * @return
     */
    private String cleanFileName(String fileName_)
    {
        return Text.escapeIllegalJcrChars(fileName_.replace("\u00A0", " "));
    }

}
