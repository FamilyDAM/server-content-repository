/*
 * Copyright (c) 2015  Mike Nimer & 11:58 Labs
 */

package com.familydam.core.observers.reactor.images;

import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.MetadataException;
import com.familydam.core.FamilyDAMConstants;
import com.familydam.core.services.AuthenticatedHelper;
import com.familydam.core.services.ImageRenditionsService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.commons.JcrUtils;
import org.imgscalr.Scalr;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.Reactor;
import reactor.event.Event;
import reactor.spring.context.annotation.Consumer;
import reactor.spring.context.annotation.Selector;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Created by mnimer on 12/23/14.
 */
@Consumer
public class ThumbnailObserver
{
    private Log log = LogFactory.getLog(this.getClass());


    @Autowired private Reactor reactor;
    @Autowired private ImageRenditionsService imageRenditionsService;
    @Autowired private AuthenticatedHelper authenticatedHelper;

    private int jobsPerIteration = 4;


    public void execute(Session session, Node node) throws RepositoryException, ImageProcessingException, MetadataException
    {
        if( node != null ){
            if( node.isNodeType(FamilyDAMConstants.DAM_IMAGE))
            {
                log.debug("{Thumbnail Image Observer} " +node.getPath());

                // create renditions
                if( node.isNodeType("dam:image") ) {
                    try {
                        BufferedImage rotatedImage = imageRenditionsService.rotateImage(session, node);

                        if( rotatedImage != null ) {
                            node.setProperty(FamilyDAMConstants.WIDTH, rotatedImage.getWidth());
                            node.setProperty(FamilyDAMConstants.HEIGHT, rotatedImage.getHeight());
                            BufferedImage scaledImage = imageRenditionsService.scaleImage(session, node, rotatedImage, 200, Scalr.Method.AUTOMATIC);
                            String renditionPath = imageRenditionsService.saveRendition(session, node, FamilyDAMConstants.THUMBNAIL200, scaledImage, "PNG");
                            session.save();
                        }else{
                            BufferedImage scaledImage = imageRenditionsService.scaleImage(session, node, 200, Scalr.Method.AUTOMATIC);
                            String renditionPath = imageRenditionsService.saveRendition(session, node, FamilyDAMConstants.THUMBNAIL200, scaledImage, "PNG");
                            session.save();
                        }

                        session.save();
                    }
                    catch (RepositoryException | IOException ex) {
                        ex.printStackTrace();
                    }finally {

                    }
                }

            }
        }
    }


    //@ReplyTo("reply.topic")
    @Selector("image.web.1024")
    public void handleWeb1024(Event<String> evt)
    {
        String path = evt.getData();

        Session session = null;
        try{
            session = authenticatedHelper.getAdminSession();

            if( path.startsWith("/") )
            {
                path = path.substring(1);
            }

            Node node = JcrUtils.getNodeIfExists(session.getRootNode(), path);
            if( node != null ){
                if( node.isNodeType(FamilyDAMConstants.DAM_IMAGE))
                {
                    log.debug("{Thumbnail/Rendition Image Observer} " +node.getPath());

                    // create renditions
                    try {
                        BufferedImage rotatedImage = imageRenditionsService.rotateImage(session, node);
                        BufferedImage scaledImage = imageRenditionsService.scaleImage(session, node, rotatedImage, 1024, Scalr.Method.AUTOMATIC);
                        String renditionPath = imageRenditionsService.saveRendition(session, node, FamilyDAMConstants.WEB1024, scaledImage, "PNG");
                        session.save();
                    }
                    catch (RepositoryException | IOException ex) {
                        ex.printStackTrace();
                    }


                    session.save();
                }
            }

        }catch(Exception re){
            re.printStackTrace();
            log.error(re);
        }finally {
            if( session != null) session.logout();
        }
    }

}
