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

package com.familydam.core.services;

import com.familydam.core.FamilyDAMConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.springframework.stereotype.Service;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Created by mnimer on 5/28/15.
 */
@Service
public class JobQueueServices
{
    private Log log = LogFactory.getLog(this.getClass());

    SimpleDateFormat df = new SimpleDateFormat("YYYYMMDD");


    public void addJob(Session session, Node node, String event, Map props, Long weight)
    {
        String dateStamp = df.format(new Date());

        try {
            Node jobQueueNode = session.getNode("/" + FamilyDAMConstants.SYSTEM_ROOT + "/" + FamilyDAMConstants.SYSTEM_JOBQUEUE_FOLDER);

            String job1Id = dateStamp + "_" + event + "_" + node.getIdentifier();
            Node job1 = JcrUtils.getOrAddNode(jobQueueNode, job1Id, JcrConstants.NT_UNSTRUCTURED);
            job1.addMixin("mix:created");
            job1.addMixin("dam:extensible");
            //job1.setProperty("node", node);
            job1.setProperty("nodeId", node.getIdentifier());
            job1.setProperty("event", event);
            job1.setProperty("weight", weight);
            job1.setProperty("status", FamilyDAMConstants.WAITING);


            for (Object key : props.keySet()) {
                Object val = props.get(key);

                if( val instanceof Long ) {
                    job1.setProperty(key.toString(), ((Long) props.get(key)).longValue());
                }else if( val instanceof Integer) {
                    job1.setProperty(key.toString(), ((Integer)props.get(key)).longValue());
                }else if( val instanceof Boolean) {
                    job1.setProperty(key.toString(), ((Boolean)props.get(key)).booleanValue());
                }else if( val instanceof Double ) {
                    job1.setProperty(key.toString(), ((Double)props.get(key)).doubleValue());
                }else if( val instanceof BigDecimal) {
                    job1.setProperty(key.toString(), ((BigDecimal)props.get(key)));
                }else if( val instanceof Calendar) {
                    job1.setProperty(key.toString(), ((Calendar)props.get(key)));
                }else{
                    job1.setProperty(key.toString(), props.get(key).toString());
                }
            }

            session.save();

        }catch( RepositoryException re){
            log.error(re);
        }
    }


    public Stream<Node> getEventJobs(Session session, String event_, String status_)
    {
        try {
            Node jobQueueNode = session.getNode("/" + FamilyDAMConstants.SYSTEM_ROOT + "/" + FamilyDAMConstants.SYSTEM_JOBQUEUE_FOLDER);

            return StreamSupport
                    .stream(JcrUtils.getChildNodes(jobQueueNode).spliterator(), false)
                    .filter(new Predicate<Node>()
                    {
                        @Override public boolean test(Node node)
                        {
                            try {
                                if (event_ != null) {
                                    return node.getProperty("event").getString().equals(event_) && node.getProperty("status").getString().equals(status_);
                                } else {
                                    return node.getProperty("status").getString().equals(status_);
                                }
                            }
                            catch (RepositoryException re) {
                                log.error(re);
                                return false;
                            }

                        }
                    });
        }catch( RepositoryException re){
            log.error(re);
        }

        return Arrays.stream(new Node[0]);
    }



    public void deleteJob(Session session, Node node_, String event_)
    {
        try {
            Node jobQueueNode = session.getNode("/" + FamilyDAMConstants.SYSTEM_ROOT + "/" + FamilyDAMConstants.SYSTEM_JOBQUEUE_FOLDER);

            StreamSupport
                    .stream(JcrUtils.getChildNodes(jobQueueNode).spliterator(), false)
                    .filter(new Predicate<Node>()
                    {
                        @Override public boolean test(Node node)
                        {
                            try {
                                return node.getProperty("nodeId").getString().equals(node_.getIdentifier())
                                        && node.getProperty("event").getString().equals(event_);
                            }
                            catch(RepositoryException re){
                                return false;
                            }

                        }
                    })
                    .forEach(new Consumer<Node>()
                    {
                        @Override public void accept(Node node)
                        {
                            try {
                                node.remove();
                                session.save();
                            }catch(RepositoryException re){
                                log.error(re);
                            }
                        }
                    });
        }catch( RepositoryException re){
            log.error(re);
        }
    }



    public void deleteAllJobs(Session session, Node node_)
    {
        try {
            Node jobQueueNode = session.getNode("/" + FamilyDAMConstants.SYSTEM_ROOT + "/" + FamilyDAMConstants.SYSTEM_JOBQUEUE_FOLDER);

            StreamSupport
                    .stream(JcrUtils.getChildNodes(jobQueueNode).spliterator(), false)
                    .filter(new Predicate<Node>()
                    {
                        @Override public boolean test(Node node)
                        {
                            try {
                                return node.getProperty("nodeId").equals(node_.getIdentifier());
                            }
                            catch(RepositoryException re){
                                return false;
                            }

                        }
                    })
                    .forEach(new Consumer<Node>()
                    {
                        @Override public void accept(Node node)
                        {
                            try {
                                node.remove();
                            }catch(RepositoryException re){}
                        }
                    });
        }catch( RepositoryException re){
            log.error(re);
        }
    }




    public void startJob(Session session, Node node)
    {
        try {
            node.setProperty("status", FamilyDAMConstants.PROCESSING);
            session.save();
        }catch(javax.jcr.RepositoryException re){
            re.printStackTrace();
            log.error(re);
        }
    }


    public void failJob(Session session, Node node, Exception ex)
    {
        try {
            node.setProperty("status", FamilyDAMConstants.FAILED);
            node.setProperty("message", ex.getMessage());
            session.save();
        }catch(javax.jcr.RepositoryException re){
            re.printStackTrace();
            log.error(re);
        }
    }

}
