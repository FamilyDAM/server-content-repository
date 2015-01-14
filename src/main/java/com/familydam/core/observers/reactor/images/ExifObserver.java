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
public class ExifObserver
{
    @Autowired private Reactor reactor;
    @Autowired private Repository repository;
    @Autowired private ImageRenditionsService imageRenditionsService;

    //@ReplyTo("reply.topic")
    @Selector("image.metadata")
    public void handleImageMetadata(Event<String> evt)
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
                    System.out.println("{EXIF Image Observer} " +node.getPath());

                    // create renditions
                    if( node.isNodeType(FamilyDAMConstants.DAM_IMAGE)) {
                        InputStream is = JcrUtils.readFile(node);
                        Node metadataNode = JcrUtils.getOrAddNode(node, FamilyDAMConstants.METADATA, JcrConstants.NT_UNSTRUCTURED);

                        try {
                            Metadata metadata = ImageMetadataReader.readMetadata(is);

                            Iterable<Directory> directories = metadata.getDirectories();

                            for (Directory directory : directories) {
                                String _name = directory.getName();
                                Node dir = JcrUtils.getOrAddNode(metadataNode, _name, JcrConstants.NT_UNSTRUCTURED);

                                Collection<Tag> tags = directory.getTags();
                                for (Tag tag : tags) {
                                    int tagType = tag.getTagType();
                                    String tagTypeHex = tag.getTagTypeHex();
                                    String tagName = tag.getTagName();
                                    String nodeName = tagName.replace(" ", "_").replace("/", "_");
                                    String desc = tag.getDescription();

                                    Node prop = JcrUtils.getOrAddNode(dir, nodeName, JcrConstants.NT_UNSTRUCTURED);
                                    prop.setProperty("name", tagName);
                                    prop.setProperty("description", desc);
                                    prop.setProperty("type", tagType);
                                    prop.setProperty("typeHex", tagTypeHex);
                                }
                            }

                        }catch(ImageProcessingException |IOException ex){
                            ex.printStackTrace();
                            //swallow
                        }
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
