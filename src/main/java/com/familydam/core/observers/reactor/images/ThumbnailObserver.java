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

package com.familydam.core.observers.reactor.images;

import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.MetadataException;
import com.familydam.core.FamilyDAM;
import com.familydam.core.FamilyDAMConstants;
import com.familydam.core.services.ImageRenditionsService;
import com.familydam.core.services.JobQueueServices;
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
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
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
    @Autowired private Repository repository;
    @Autowired private ImageRenditionsService imageRenditionsService;
    @Autowired private JobQueueServices jobQueueServices;

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

        SimpleCredentials credentials = new SimpleCredentials(FamilyDAM.adminUserId, FamilyDAM.adminPassword.toCharArray());
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
        }
        finally {
            if( session != null) {
                session.logout();
            }
        }
    }

}
