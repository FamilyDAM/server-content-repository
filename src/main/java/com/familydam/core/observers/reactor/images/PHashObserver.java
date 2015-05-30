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

import com.familydam.core.FamilyDAM;
import com.familydam.core.FamilyDAMConstants;
import com.familydam.core.helpers.ImagePHash;
import com.familydam.core.services.ImageRenditionsService;
import com.familydam.core.services.JobQueueServices;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import java.io.InputStream;
import java.util.stream.Stream;

/**
 * Created by mnimer on 12/23/14.
 */
@Consumer
public class PHashObserver
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

            Stream<Node> events = jobQueueServices.getEventJobs(_session, FamilyDAMConstants.EVENT_IMAGE_PHASH, FamilyDAMConstants.WAITING);
            events
                    .limit(jobsPerIteration)
                    .parallel()
                    .forEach(new java.util.function.Consumer<Node>()
                    {
                        @Override public void accept(Node node)
                        {
                            try {
                                Node _node = node.getProperty("node").getNode();
                                jobQueueServices.startJob(_session, node);
                                execute(_session, _node);
                                jobQueueServices.deleteJob(_session, _node, FamilyDAMConstants.EVENT_IMAGE_THUMBNAIL);
                            }catch(InvalidItemStateException iex){
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



    /**
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
            execute(session, node);

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

                log.debug("{PHASH Image Observer} " +node.getPath());

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
    }

}
