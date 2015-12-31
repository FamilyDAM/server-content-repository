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
import org.imgscalr.Scalr;
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
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by mnimer on 12/16/14.
 */
@Service
public class ImageRenditionsService
{

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

        if( metadataIS == null ) return null;

        Metadata metadata = ImageMetadataReader.readMetadata(metadataIS);
        BufferedImage originalImage = ImageIO.read(originalImageIS);

        ExifIFD0Directory exifIFD0Directory = metadata.getDirectory(ExifIFD0Directory.class);
        JpegDirectory jpegDirectory = (JpegDirectory) metadata.getDirectory(JpegDirectory.class);

        int orientation = 1;
        try {
            orientation = exifIFD0Directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
        }
        catch (Exception ex) {
            //ex.printStackTrace();
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
        return null;
    }



    public BufferedImage scaleImage(Session session, Node node, int longSize) throws RepositoryException, IOException
    {
        InputStream is = JcrUtils.readFile(node);
        BufferedImage bufferedImage = ImageIO.read(is);

        return scaleImage(session, node, bufferedImage, longSize, Scalr.Method.AUTOMATIC);
    }


    public BufferedImage scaleImage(Session session, Node node, int longSize, Scalr.Method quality) throws RepositoryException, IOException
    {
        InputStream is = JcrUtils.readFile(node);
        BufferedImage bufferedImage = ImageIO.read(is);

        return scaleImage(session, node, bufferedImage, longSize, quality);
    }


    public BufferedImage scaleImage(Session session, Node node, BufferedImage bufferedImage, int longSize) throws RepositoryException, IOException
    {
        return scaleImage(session, node, bufferedImage, longSize, Scalr.Method.AUTOMATIC);
    }


    public BufferedImage scaleImage(Session session, Node node, BufferedImage bufferedImage, int longSize, Scalr.Method quality) throws RepositoryException, IOException
    {
        return Scalr.resize(bufferedImage, quality, longSize);


    }


    public String saveRendition(Session session, Node node, String name, BufferedImage image, String mimeType) throws RepositoryException, IOException
    {
        String fullMimeType = MimeTypeManager.getMimeType(mimeType);

        // Save Image
        Node renditions = JcrUtils.getOrAddNode(node, FamilyDAMConstants.RENDITIONS, JcrConstants.NT_FOLDER);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(image, mimeType, byteArrayOutputStream);
        InputStream thumbnail_is = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

        Node newNode = JcrUtils.putFile(renditions, name, fullMimeType, thumbnail_is);

        return newNode.getPath();
    }
}
