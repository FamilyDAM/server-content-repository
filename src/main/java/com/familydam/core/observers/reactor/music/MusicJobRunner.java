/*
 * Copyright (c) 2015  Mike Nimer & 11:58 Labs
 */

package com.familydam.core.observers.reactor.music;

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
import javax.security.sasl.AuthenticationException;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Created by mnimer on 6/1/15.
 */
@Component
public class MusicJobRunner
{
    private Log log = LogFactory.getLog(this.getClass());


    @Autowired private Reactor reactor;
    @Autowired private Repository repository;
    @Autowired private ImageRenditionsService imageRenditionsService;
    @Autowired private JobQueueServices jobQueueServices;
    @Autowired Mp3Observer mp3Observer;
    @Autowired private AuthenticatedHelper authenticatedHelper;

    private int jobsPerIteration = 10;


    @Scheduled(fixedRate = 10000)
    public void checkForJobs()
    {
        try{
            final Session _session = authenticatedHelper.getAdminSession();

            Stream<Node> events = jobQueueServices.getEventJobs(_session, null, FamilyDAMConstants.WAITING);
            events
                    .sorted(new Comparator<Node>()
                    {
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
                                String _event = node.getProperty("event").getString();
                                if (_event.equals(FamilyDAMConstants.EVENT_MP3_METADATA)){
                                    jobQueueServices.startJob(_session, node);
                                    mp3Observer.execute(_session, _node);
                                    jobQueueServices.deleteJob(_session, node);//, FamilyDAMConstants.EVENT_MP3_METADATA);
                                }
                            }
                            catch (Exception ex) {
                                ex.printStackTrace();
                                log.error(ex);
                                jobQueueServices.failJob(_session, node, ex);
                            }

                        }
                    });

            if( _session != null) _session.logout();

        }catch( AuthenticationException re){
            log.error(re);
        }
    }



}
