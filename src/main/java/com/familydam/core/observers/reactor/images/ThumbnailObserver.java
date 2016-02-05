/*
 * Copyright (c) 2015  Mike Nimer & 11:58 Labs
 */

package com.familydam.core.observers.reactor.images;

import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.MetadataException;
import com.familydam.core.FamilyDAMConstants;
import com.familydam.core.services.AuthenticatedHelper;
import com.familydam.core.services.ImageRenditionsService;
import org.apache.jackrabbit.commons.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.Reactor;
import reactor.event.Event;
import reactor.spring.context.annotation.Consumer;
import reactor.spring.context.annotation.Selector;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;

/**
 * Created by mnimer on 12/23/14.
 */
@Consumer
public class ThumbnailObserver
{
    private Logger log = LoggerFactory.getLogger(this.getClass());


    @Autowired private Reactor reactor;
    @Autowired private ImageRenditionsService imageRenditionsService;
    @Autowired private AuthenticatedHelper authenticatedHelper;

    private int jobsPerIteration = 4;


    public void execute(Session session, Node node, int size_) throws RepositoryException, ImageProcessingException, MetadataException, IOException
    {
        if( node != null ){
            if( node.isNodeType(FamilyDAMConstants.DAM_IMAGE))
            {
                log.debug("{Thumbnail Image Observer} " +node.getPath());

                // create renditions
                if( node.isNodeType("dam:image") ) {

                    String renditionPath = imageRenditionsService.scaleImage(session, node, size_);
                }

            }
        }
    }


    //@ReplyTo("reply.topic")
    @Selector("image.rendition")
    public void handleRendition(Event<String> evt)
    {
        String _path = evt.getData().substring(0);
        Integer _size = 1024;
        int pos = evt.getData().indexOf("|");
        if( pos > -1 ) {
            _path = evt.getData().substring(0, pos);
            _size = new Integer(evt.getData().substring(pos));
        }

        Session session = null;
        try{
            session = authenticatedHelper.getAdminSession();

            if( _path.startsWith("/") )
            {
                _path = _path.substring(1);
            }

            Node node = JcrUtils.getNodeIfExists(session.getRootNode(), _path);
            if( node != null ){
                if( node.isNodeType(FamilyDAMConstants.DAM_IMAGE))
                {
                    log.debug("{Thumbnail/Rendition Image Observer} " +node.getPath());

                    // create renditions
                    try {
                        //BufferedImage rotatedImage = imageRenditionsService.rotateImage(session, node);
                        String scaledImage = imageRenditionsService.scaleImage(session, node, _size);
                        //String renditionPath = imageRenditionsService.saveRendition(session, node, "web." +_size, scaledImage.getWidth(), scaledImage.getHeight(), scaledImage, "PNG");
                        //session.save();
                    }
                    catch (RepositoryException | IOException ex) {
                        ex.printStackTrace();
                    }


                    session.save();
                }
            }

        }catch(Exception re){
            re.printStackTrace();
            log.error(re.getMessage(), re);
        }finally {
            if( session != null) session.logout();
        }
    }

}
