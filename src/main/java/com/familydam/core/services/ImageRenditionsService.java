/*
 * Copyright (c) 2015  Mike Nimer & 11:58 Labs
 */

package com.familydam.core.services;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.jpeg.JpegDirectory;
import com.familydam.core.FamilyDAMConstants;
import com.familydam.core.helpers.MimeTypeManager;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.oak.commons.IOUtils;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IMOperation;
import org.im4java.core.IdentifyCmd;
import org.imgscalr.Scalr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;


/**
 * Created by mnimer on 12/16/14.
 */
@Service
public class ImageRenditionsService
{
    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Value("${image.tools.enabled}")
    Boolean imageToolsEnabled = false;
    @Value("${image.tools.path}")
    String imageToolsPath;

    private ConvertCmd convertCommand;
    private IdentifyCmd identifyCmd;



    /**
     * A lot of images, especially mobile, are technically saved in any orientation with metadata to tell us how to flip it to
     * what humans would expect to see.
     * @param session
     * @param node
     * @return
     * @throws RepositoryException
     * @throws IOException
     * @throws ImageProcessingException
     * @throws MetadataException
     */
    public BufferedImage rotateImage(Session session, Node node) throws RepositoryException, IOException, ImageProcessingException, MetadataException
    {

        InputStream metadataIS = JcrUtils.readFile(node);
        InputStream originalImageIS = JcrUtils.readFile(node);
        BufferedImage originalImage = ImageIO.read(originalImageIS);

        if( metadataIS == null ) return originalImage;

        Metadata metadata = ImageMetadataReader.readMetadata(metadataIS);

        ExifIFD0Directory exifIFD0Directory = metadata.getDirectory(ExifIFD0Directory.class);
        JpegDirectory jpegDirectory = (JpegDirectory) metadata.getDirectory(JpegDirectory.class);

        int orientation = 1;
        if( exifIFD0Directory != null && exifIFD0Directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION) ) {
            try {
                orientation = exifIFD0Directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }else{
            return originalImage;
        }

        if( jpegDirectory != null ) {
            int width = jpegDirectory.getImageWidth();
            int height = jpegDirectory.getImageHeight();

            AffineTransform affineTransform = new AffineTransform();

            switch (orientation) {
                case 1:
                    return originalImage;
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
                    return originalImage;
            }

            AffineTransformOp affineTransformOp = new AffineTransformOp(affineTransform, AffineTransformOp.TYPE_BILINEAR);
            BufferedImage destinationImage = new BufferedImage(originalImage.getHeight(), originalImage.getWidth(), originalImage.getType());
            destinationImage = affineTransformOp.filter(originalImage, destinationImage);

            return destinationImage;
        }
        return originalImage;
    }



    public String scaleImage(Session session, Node node, int longSize) throws RepositoryException, IOException
    {
        if( !imageToolsEnabled ){

            String newImagePath = ScaleWithScalr(session, node, longSize, Scalr.Method.AUTOMATIC);
            return newImagePath;

        }else {

            String newImagePath = ScaleWithImageMagik(session, node, longSize, "PNG");
            return newImagePath;
        }
    }




    private String ScaleWithScalr(Session session, Node node, int longSize, Scalr.Method quality) throws RepositoryException, IOException
    {
        InputStream is = JcrUtils.readFile(node);
        BufferedImage bufferedImage = ImageIO.read(is);

        BufferedImage scaledImage = Scalr.resize(bufferedImage, quality, longSize);


        // Save rendition
        String mimeType = MimeTypeManager.getMimeType("PNG");
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(scaledImage, mimeType, byteArrayOutputStream);
        InputStream thumbnail_is = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

        String path = this.saveRendition(session, node, "web." +longSize, scaledImage.getWidth(), scaledImage.getHeight(), thumbnail_is, mimeType);
        return path;
    }


    private String ScaleWithImageMagik(Session session, Node node, int longSize, String format) throws RepositoryException, IOException
    {
        InputStream is = JcrUtils.readFile(node);
        String[] nameParts = node.getName().split("\\.");
        File _tmpFile = File.createTempFile(nameParts[0], "." +nameParts[1]);
        File _tmpSmallFile = File.createTempFile(nameParts[0] +"x" +longSize, "." +nameParts[1]);
        _tmpFile.deleteOnExit();
        _tmpSmallFile.deleteOnExit();
        // write the inputStream to a FileOutputStream
        IOUtils.copy(is, new FileOutputStream(_tmpFile));

        //File _tmpThumbnailFile = File.createTempFile(node.getName() + "_thumbnail", "");


        try {
            // create command
            if( convertCommand == null ) {
                //ProcessStarter.setGlobalSearchPath(imageToolsPath);
                convertCommand = new ConvertCmd();
                identifyCmd = new IdentifyCmd();
                convertCommand.setSearchPath(imageToolsPath);
            }



            // create the operation, add images and operators/options
            IMOperation op = new IMOperation();
            op.addImage(_tmpFile.getAbsolutePath());
            op.autoOrient();
            op.resize(longSize);
            op.addImage(_tmpSmallFile.getAbsolutePath());
            //op.addImage(_tmpThumbnailFile.getAbsolutePath());

            // execute the operation
            convertCommand.run(op);

            log.trace("    Scale Image with ImageMagick x" +longSize +" | " +node.getPath());
            String newImagePath = _tmpSmallFile.getAbsolutePath();


            // GET The image info for the thumbnail
            InputStream tmpImageIS = new FileInputStream(_tmpSmallFile);
            BufferedImage tmpBufferedImage = ImageIO.read(tmpImageIS);

            // Save Rendition
            log.trace("    Resize thumbnail w=" +tmpBufferedImage.getWidth() +" | h=" +tmpBufferedImage.getHeight() +" | " +node.getPath());
            FileInputStream fileInputStream = new FileInputStream(newImagePath);
            String newNodePath = this.saveRendition(session, node, "web." +longSize, tmpBufferedImage.getWidth(), tmpBufferedImage.getHeight(), fileInputStream, nameParts[1]);
            return newNodePath;

        }
        catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
        finally{
            if( _tmpFile.exists() ) _tmpFile.delete();
            if( _tmpSmallFile.exists() ) _tmpSmallFile.delete();
        }
    }


    public String saveRendition(Session session, Node node, String name, int width_, int height_, InputStream is, String mimeType) throws RepositoryException, IOException
    {
        try {
            // Save Image
            session.refresh(true);
            Node renditions = JcrUtils.getOrAddNode(node, FamilyDAMConstants.RENDITIONS, JcrConstants.NT_FOLDER);
            renditions.addMixin("dam:extensible");
            renditions.setProperty(FamilyDAMConstants.WIDTH, width_);
            renditions.setProperty(FamilyDAMConstants.HEIGHT, height_);

            Node newNode = JcrUtils.putFile(renditions, name, mimeType, is);

            session.save();
            log.trace("    Save Rendition w=" +width_ +" | h=" +height_ +" | " + newNode.getPath());
            return newNode.getPath();
        }catch(Exception ex){
            ex.printStackTrace();
            return null;
        }
    }
}
