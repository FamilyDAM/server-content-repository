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
import com.familydam.core.services.JobQueueServices;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import reactor.core.Reactor;
import reactor.spring.context.annotation.Consumer;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.stream.Stream;

/**
 * Created by mnimer on 12/23/14.
 */
@Consumer
public class ExifObserver
{
    private Log log = LogFactory.getLog(this.getClass());


    @Autowired private Reactor reactor;
    @Autowired private Repository repository;
    @Autowired private ImageRenditionsService imageRenditionsService;
    @Autowired private JobQueueServices jobQueueServices;

    private int jobsPerIteration = 4;


    @Scheduled(fixedRate = 10000)
    public void checkForJobs()
    {
        SimpleCredentials credentials = new SimpleCredentials(FamilyDAM.adminUserId, FamilyDAM.adminPassword.toCharArray());
        Session session = null;
        try{
            session = repository.login(credentials);

            final Session _session = session;

            Stream<Node> events = jobQueueServices.getEventJobs(_session, FamilyDAMConstants.EVENT_IMAGE_METADATA, FamilyDAMConstants.WAITING);
            events
                    .limit(jobsPerIteration)
                    .forEach(new java.util.function.Consumer<Node>()
                    {
                        @Override public void accept(Node node)
                        {

                            try {
                                Node _node = node.getProperty("node").getNode();
                                jobQueueServices.startJob(_session, node);
                                execute(_session, _node);
                                jobQueueServices.deleteJob(_session, _node, FamilyDAMConstants.EVENT_IMAGE_THUMBNAIL);
                            }
                            catch (InvalidItemStateException iex) {
                                iex.printStackTrace();
                                log.error(iex);
                            }
                            catch (javax.jcr.RepositoryException ex) {
                                ex.printStackTrace();
                                log.error(ex);
                                jobQueueServices.failJob(_session, node, ex);
                            }

                        }
                    });

        }catch( RepositoryException re){
            log.error(re);
        }
    }



    /***
    //@ReplyTo("reply.topic")
    @Selector("image.metadata")
    public void handleImageMetadata(Event<String> evt)
    {
        String jobId = evt.getData();

        SimpleCredentials credentials = new SimpleCredentials(FamilyDAM.adminUserId, FamilyDAM.adminPassword.toCharArray());

        try{
            Session _session = repository.login(credentials);

            Node node = jobQueueServices.getEventJob(_session, jobId, FamilyDAMConstants.EVENT_IMAGE_METADATA);

            try {
                Node _node = node.getProperty("node").getNode();
                execute(_session, _node);
                jobQueueServices.deleteJobById(_session, jobId, FamilyDAMConstants.EVENT_IMAGE_METADATA);
            }
            catch (RepositoryException re) {
                re.printStackTrace();
                log.error(re);
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
    ***/


    private void execute(Session session, Node node) throws RepositoryException
    {
        if( node != null ){
            if( node.isNodeType(FamilyDAMConstants.DAM_IMAGE))
            {
                log.debug("{image.metadata Observer} " +node.getPath());

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

                        session.save();

                    }catch(ImageProcessingException |IOException ex){
                        ex.printStackTrace();
                        log.error(ex);
                        //swallow
                    }
                }


            }
        }
    }


}
