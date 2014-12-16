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

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.jpeg.JpegDirectory;
import com.familydam.core.FamilyDAMConstants;
import com.familydam.core.helpers.MimeTypeManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.imgscalr.Scalr;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.imageio.ImageIO;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.security.auth.login.LoginException;
import javax.security.sasl.AuthenticationException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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

                    //If the file exists, check it out before we overwrite
                    String relativePath = dirPath;
                    if( relativePath.startsWith("/") )
                    {
                        relativePath = dirPath.substring(1);
                    }

                    Node _checkFilePath = JcrUtils.getNodeIfExists(root, relativePath +"/" +fileName);
                    if( _checkFilePath != null )
                    {
                        session.getWorkspace().getVersionManager().checkout(_checkFilePath.getPath());
                    }



                    // Upload the FILE
                    Node fileNode = JcrUtils.putFile(copyToDir, fileName, mimeType, new BufferedInputStream(new FileInputStream(file)) );
                    //fileNode.setProperty(JcrConstants.JCR_CREATED, session.getUserID());

                    // apply mixins
                    applyMixins(mimeType, fileNode);

                    session.save();

                    // Create a thumbnail (rotated to the right angle)
                    rotateAndScaleThumbnail(session, fileNode);

                    // save the new file as a "versioned" file
                    session.getWorkspace().getVersionManager().checkin(fileNode.getPath());


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
        // make all files versionable
        fileNode.addMixin("mix:versionable");


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



    private void rotateAndScaleThumbnail(Session session, Node node) throws RepositoryException, IOException, ImageProcessingException, MetadataException
    {
        try {
            InputStream is = JcrUtils.readFile(node);
            Metadata metadata = ImageMetadataReader.readMetadata(is);


            ExifIFD0Directory exifIFD0Directory = metadata.getDirectory(ExifIFD0Directory.class);
            JpegDirectory jpegDirectory = (JpegDirectory) metadata.getDirectory(JpegDirectory.class);

            int orientation = 1;
            try {
                orientation = exifIFD0Directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }

            int width = jpegDirectory.getImageWidth();
            int height = jpegDirectory.getImageHeight();

            AffineTransform affineTransform = new AffineTransform();

            switch (orientation) {
                case 1:
                    break;
                case 2: // Flip X
                    affineTransform.scale(-1.0, 1.0);
                    affineTransform.translate(-width, 0);
                    break;
                case 3: // PI rotation
                    affineTransform.translate(width, height);
                    affineTransform.rotate(Math.PI);
                    break;
                case 4: // Flip Y
                    affineTransform.scale(1.0, -1.0);
                    affineTransform.translate(0, -height);
                    break;
                case 5: // - PI/2 and Flip X
                    affineTransform.rotate(-Math.PI / 2);
                    affineTransform.scale(-1.0, 1.0);
                    break;
                case 6: // -PI/2 and -width
                    affineTransform.translate(height, 0);
                    affineTransform.rotate(Math.PI / 2);
                    break;
                case 7: // PI/2 and Flip
                    affineTransform.scale(-1.0, 1.0);
                    affineTransform.translate(-height, 0);
                    affineTransform.translate(0, width);
                    affineTransform.rotate(3 * Math.PI / 2);
                    break;
                case 8: // PI / 2
                    affineTransform.translate(0, width);
                    affineTransform.rotate(3 * Math.PI / 2);
                    break;
                default:
                    break;
            }

            AffineTransformOp affineTransformOp = new AffineTransformOp(affineTransform, AffineTransformOp.TYPE_BILINEAR);


            InputStream is2 = JcrUtils.readFile(node);
            BufferedImage image = ImageIO.read(is2);
            //BufferedImage thumbnail = Scalr.resize(image, Scalr.Method.AUTOMATIC, 200);//, affineTransformOp);
            BufferedImage thumbnail = Scalr.apply(image, affineTransformOp);


            // Save Image
            Node renditions = JcrUtils.getOrAddNode(node, FamilyDAMConstants.RENDITIONS, JcrConstants.NT_FOLDER);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ImageIO.write(thumbnail, "jpg", byteArrayOutputStream);
            InputStream thumbnail_is = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

            JcrUtils.putFile(renditions, FamilyDAMConstants.THUMBNAIL200, MimeTypeManager.JPG.name(), thumbnail_is);

            session.save();
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }
}
