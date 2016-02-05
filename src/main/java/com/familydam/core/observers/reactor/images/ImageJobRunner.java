/*
 * Copyright (c) 2015  Mike Nimer & 11:58 Labs
 */

package com.familydam.core.observers.reactor.images;

import com.familydam.core.FamilyDAMConstants;
import com.familydam.core.services.AuthenticatedHelper;
import com.familydam.core.services.ImageRenditionsService;
import com.familydam.core.services.JobQueueServices;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.Reactor;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
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
    @Autowired
    private Repository repository;
    @Autowired
    private ImageRenditionsService imageRenditionsService;
    @Autowired
    private JobQueueServices jobQueueServices;

    @Autowired
    ExifObserver exifObserver;
    @Autowired
    PHashObserver pHashObserver;
    @Autowired
    ThumbnailObserver thumbnailObserver;
    @Autowired
    AuthenticatedHelper authenticatedHelper;

    private int jobsPerIteration = 3;


    @Scheduled(fixedRate = 5000)
    public void checkForJobs()
    {
        long sTime = System.currentTimeMillis();
        List nodes = Collections.EMPTY_LIST;

        Session session = null;
        try {
            session = authenticatedHelper.getAdminSession();

            final Session _session = session;

            Stream<Node> events = jobQueueServices.getEventJobs(_session, null, FamilyDAMConstants.WAITING);
            nodes = events.sorted(new Comparator<Node>()
            {
                //  descending sort 100 -> 1
                @Override public int compare(Node o1, Node o2)
                {
                    try {
                        if (o1.hasProperty("weight") && o1.hasProperty("weight")) {
                            long weight1 = o1.getProperty("weight").getLong();
                            long weight2 = o2.getProperty("weight").getLong();
                            if (weight1 == weight2) {
                                return 0;
                            } else if (weight1 > weight2) {
                                return -1;
                            }
                            return 1;
                        } else {
                            return -1;
                        }
                    }
                    catch (Exception ex) {
                        return -1;
                    }
                }
            })
            .limit(jobsPerIteration)
            .map(new Function<Node, Object>()
            {
                @Override public Object apply(Node node)
                {
                    try {
                        Node _node = _session.getNodeByIdentifier(node.getProperty("nodeId").getString());

                        String _event = node.getProperty("event").getString();
                        if (_event.equals(FamilyDAMConstants.EVENT_IMAGE_THUMBNAIL)) {
                            jobQueueServices.startJob(_session, node);
                            thumbnailObserver.execute(_session, _node, 200);
                            jobQueueServices.deleteJob(_session, node);//, FamilyDAMConstants.EVENT_IMAGE_THUMBNAIL);
                        } else if (_event.equals(FamilyDAMConstants.EVENT_IMAGE_METADATA)) {
                            jobQueueServices.startJob(_session, node);
                            exifObserver.execute(_session, _node);
                            jobQueueServices.deleteJob(_session, node);//, FamilyDAMConstants.EVENT_IMAGE_THUMBNAIL);
                        } else if (_event.equals(FamilyDAMConstants.EVENT_IMAGE_PHASH)) {
                            jobQueueServices.startJob(_session, node);
                            pHashObserver.execute(_session, _node);
                            jobQueueServices.deleteJob(_session, node);
                        }
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                        log.error(ex);
                        jobQueueServices.failJob(_session, node, ex);
                    }

                    return node;
                }
            }).collect(Collectors.toList());
            //.parallel() we can't share JCR session between threads


            Thread.yield();
            System.gc();



            long eTime = System.currentTimeMillis();
            long _total = eTime - sTime;
            log.debug("Image Runner Scheduled Task: " + (_total) + "ms | jobs=" + jobsPerIteration);

            if( nodes.size() == 0 ){
              this.jobsPerIteration = 3;//back to default when we have an empty queue
            } else if (_total < 5000 && nodes.size() >= jobsPerIteration ) {
                this.jobsPerIteration = Math.min(50,this.jobsPerIteration * 2);
            } else if (_total > 9000) {
                this.jobsPerIteration = Math.max(1, this.jobsPerIteration / 2);
            }

        }
        catch (Exception re) {
            log.error(re);
        }
        finally {
            if (session != null) {
                session.logout();
            }
        }

    }


}
