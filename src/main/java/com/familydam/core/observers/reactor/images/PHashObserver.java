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

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.familydam.core.FamilyDAM;
import com.familydam.core.FamilyDAMConstants;
import com.familydam.core.helpers.ImagePHash;
import com.familydam.core.services.ImageRenditionsService;
import org.apache.jackrabbit.JcrConstants;
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
import java.io.InputStream;
import java.util.Collection;

/**
 * Created by mnimer on 12/23/14.
 */
@Consumer
public class PHashObserver
{
    @Autowired private Reactor reactor;
    @Autowired private Repository repository;
    @Autowired private ImageRenditionsService imageRenditionsService;

    //@ReplyTo("reply.topic")
    @Selector("image.phash")
    public void calculatePHash(Event<String> evt)
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

                    System.out.println("{PHASH Image Observer} " +node.getPath());

                    try {
                        InputStream is = JcrUtils.readFile(node);

                        ImagePHash pHash = new ImagePHash();
                        String hash = pHash.getHash(is);
                        Node _node = session.getNode(node.getPath());
                        _node.setProperty("phash", hash);
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    session.save();
                }
            }

        }catch(Exception re){
            re.printStackTrace();
        }
        finally {
            if( session != null) {
                session.logout();
            }
        }
    }

}
