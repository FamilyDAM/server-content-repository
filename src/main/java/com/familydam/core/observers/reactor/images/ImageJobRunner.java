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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.Reactor;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Created by mnimer on 6/1/15.
 */
@Component
public class ImageJobRunner
{

    private Log log = LogFactory.getLog(this.getClass());


    @Autowired
    private Reactor reactor;
    @Autowired private Repository repository;
    @Autowired private ImageRenditionsService imageRenditionsService;
    @Autowired private JobQueueServices jobQueueServices;

    @Autowired ExifObserver exifObserver;
    @Autowired PHashObserver pHashObserver;
    @Autowired ThumbnailObserver thumbnailObserver;

    private int jobsPerIteration = 10;


    @Scheduled(fixedRate = 10000)
    public void checkForJobs()
    {
        SimpleCredentials credentials = new SimpleCredentials(FamilyDAM.adminUserId, FamilyDAM.adminPassword.toCharArray());
        Session session = null;
        try{
            session = repository.login(credentials);

            final Session _session = session;

            Stream<Node> events = jobQueueServices.getEventJobs(_session, null, FamilyDAMConstants.WAITING);
            events
                    .sorted(new Comparator<Node>()
                    {
                        //  descending sort 100 -> 1
                        @Override public int compare(Node o1, Node o2)
                        {
                            try {
                                Long weight1 = o1.getProperty("weight").getLong();
                                Long weight2 = o2.getProperty("weight").getLong();
                                if (weight1 == weight2) {
                                    return 0;
                                } else if (weight1 > weight2) {
                                    return -1;
                                }
                                return 1;
                            }
                            catch (Exception ex) {
                                return -1;
                            }
                        }
                    })
                    .limit(jobsPerIteration)
                    .forEach(new java.util.function.Consumer<Node>()
                    {
                        @Override public void accept(Node node)
                        {

                            try {
                                Node _node = _session.getNodeByIdentifier(node.getProperty("nodeId").getString());
                                jobQueueServices.startJob(_session, node);

                                String _event = node.getProperty("event").getString();
                                if (_event.equals(FamilyDAMConstants.EVENT_IMAGE_THUMBNAIL)) {
                                    thumbnailObserver.execute(_session, _node);
                                } else if (_event.equals(FamilyDAMConstants.EVENT_IMAGE_METADATA)) {
                                    exifObserver.execute(_session, _node);
                                } else if (_event.equals(FamilyDAMConstants.EVENT_IMAGE_PHASH)) {
                                    pHashObserver.execute(_session, _node);
                                }
                                jobQueueServices.deleteJob(_session, _node, FamilyDAMConstants.EVENT_IMAGE_THUMBNAIL);
                            }
                            catch (InvalidItemStateException iex) {
                                iex.printStackTrace();
                                log.error(iex);
                            }
                            catch (javax.jcr.RepositoryException | ImageProcessingException | MetadataException ex) {
                                ex.printStackTrace();
                                log.error(ex);
                                jobQueueServices.failJob(_session, node, ex);
                            }

                        }
                    });

            System.gc();

        }catch( RepositoryException re){
            log.error(re);
        }
    }


}
