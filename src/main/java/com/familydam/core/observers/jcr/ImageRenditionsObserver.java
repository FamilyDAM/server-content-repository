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

package com.familydam.core.observers.jcr;

import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.MetadataException;
import com.familydam.core.FamilyDAM;
import com.familydam.core.FamilyDAMConstants;
import com.familydam.core.services.ImageRenditionsService;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.oak.plugins.observation.NodeObserver;
import org.apache.jackrabbit.oak.spi.commit.CommitInfo;
import org.imgscalr.Scalr;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Created by mnimer on 9/16/14.
 * @Deprectated by Reactor observers
 */
public class ImageRenditionsObserver extends NodeObserver implements Closeable
{

    private Credentials credentials;
    private Repository repository;
    private ImageRenditionsService imageRenditionsService;

    public void setRepository(Repository repository)
    {
        this.repository = repository;
    }


    public void setImageRenditionsService(ImageRenditionsService imageRenditionsService)
    {
        this.imageRenditionsService = imageRenditionsService;
    }


    public ImageRenditionsObserver(String path, String... propertyNames)
    {
        super(path, propertyNames);

        credentials = new SimpleCredentials(FamilyDAM.adminUserId, FamilyDAM.adminPassword.toCharArray());
    }


    @Override
    protected void added(String path, Set<String> added, Set<String> deleted, Set<String> changed, Map<String, String> properties, CommitInfo commitInfo)
    {
        //System.out.println("{thumbnail observer} added | " +path);

        processImageNode(path);
    }


    @Override
    protected void changed(String path, Set<String> added, Set<String> deleted, Set<String> changed, Map<String, String> properties, CommitInfo commitInfo)
    {
        //System.out.println("{thumbnail observer} changed | " +path);

        //processImageNode(path);
    }


    @Override
    protected void deleted(String path, Set<String> added, Set<String> deleted, Set<String> changed, Map<String, String> properties, CommitInfo commitInfo)
    {
        System.out.println("{thumbnail observer} deleted");

    }


    @Override public void close() throws IOException
    {
        System.out.println("{thumbnail observer} close");
    }




    private void processImageNode(String path)
    {
        Session session = null;
        try{
            session = repository.login(credentials);

            if( path.startsWith("/") )
            {
                path = path.substring(1);
            }

            Node node = JcrUtils.getNodeIfExists(session.getRootNode(), path);
            if( node != null ){
                if( node.isNodeType(FamilyDAMConstants.DAM_IMAGE))
                {
                    System.out.println("{Rendition Image Observer} " +node.getPath());

                    // create renditions
                    createThumbnailRendition(session, node);
                    //createWeb1024Rendition(session, node);

                    session.save();
                }
            }

        }catch(RepositoryException re){
            re.printStackTrace();
        }
        finally {
            if( session != null) {
                session.logout();
            }
        }
    }



    /**
     * Create a small 200 thumbnail for each image
     * @param session
     * @param node
     */
    private void createThumbnailRendition(Session session, Node node) throws RepositoryException
    {
        String path = FamilyDAMConstants.RENDITIONS +"/" +FamilyDAMConstants.THUMBNAIL200;
        if( JcrUtils.getNodeIfExists(node, path) == null ) {
            // Create a thumbnail (rotated & scaled)
            try {
                BufferedImage rotatedImage = imageRenditionsService.rotateImage(session, node);
                BufferedImage scaledImage = imageRenditionsService.scaleImage(session, node, rotatedImage, 200, Scalr.Method.AUTOMATIC);
                String renditionPath = imageRenditionsService.saveRendition(session, node, FamilyDAMConstants.THUMBNAIL200, scaledImage, "PNG");
                //session.save();
            }
            catch (RepositoryException | IOException | MetadataException | ImageProcessingException ex) {
                ex.printStackTrace();
            }
        }
    }


    /**
     * Create a larger 1024 thumbnail for each image
     * @param session
     * @param node
     */
    private void createWeb1024Rendition(Session session, Node node) throws RepositoryException
    {
        String path = FamilyDAMConstants.RENDITIONS +"/" +FamilyDAMConstants.WEB1024;
        if( JcrUtils.getNodeIfExists(node, path) == null ) {
            // Create a thumbnail (rotated & scaled)
            try {
                BufferedImage rotatedImage = imageRenditionsService.rotateImage(session, node);
                BufferedImage scaledImage = imageRenditionsService.scaleImage(session, node, rotatedImage, 1024, Scalr.Method.AUTOMATIC);
                String renditionPath = imageRenditionsService.saveRendition(session, node, FamilyDAMConstants.WEB1024, scaledImage, "PNG");
                session.save();
            }
            catch (RepositoryException | IOException | MetadataException | ImageProcessingException ex) {
                ex.printStackTrace();
            }
        }
    }

}
