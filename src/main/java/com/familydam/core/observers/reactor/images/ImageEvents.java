/*
 * Copyright (c) 2015  Mike Nimer & 11:58 Labs
 */

package com.familydam.core.observers.reactor.images;

import com.familydam.core.FamilyDAMConstants;
import com.familydam.core.services.AuthenticatedHelper;
import com.familydam.core.services.JobQueueServices;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.JcrConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import reactor.core.Reactor;
import reactor.event.Event;
import reactor.spring.context.annotation.Consumer;
import reactor.spring.context.annotation.Selector;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.sasl.AuthenticationException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mnimer on 12/23/14.
 */
@Consumer
public class ImageEvents
{

    private Log log = LogFactory.getLog(this.getClass());

    @Qualifier("reactorEngine")
    @Autowired private Reactor reactor;
    @Autowired private Repository repository;
    @Autowired private JobQueueServices jobQueueServices;
    @Autowired private AuthenticatedHelper authenticatedHelper;

    @Selector("file.added")
    public void handleImageAddedEvents(Event<String> evt)
    {
        String path = evt.getData();

        Session session = null;
        try {
            session = authenticatedHelper.getAdminSession();


            if (!isRootPath(path)) {
                return;
            }


            Node node = session.getNode(path);

            if (node != null) {
                if (node.isNodeType(FamilyDAMConstants.DAM_IMAGE)) {

                    createJobs(node, session);
                }
            }
        }catch(RepositoryException|AuthenticationException re){
            re.printStackTrace();
            reactor.notify("error", Event.wrap(re.getMessage()));
        }finally {
            if( session != null) session.logout();
        }

    }


    @Selector("file.changed")
    public void handleImageChangedEvents(Event<String> evt)
    {
        String path = evt.getData();

        Session session = null;
        try {
            session = authenticatedHelper.getAdminSession();


            if (!isRootPath(path)) {
                return;
            }


            Node node = session.getNode(path);

            if (node != null) {
                if (node.isNodeType(FamilyDAMConstants.DAM_IMAGE)) {
                    createJobs(node, session);
                    // save the jobs
                    session.save();
                }
            }
        }catch(RepositoryException|AuthenticationException re){
            reactor.notify("error", Event.wrap(re.getMessage()));
        }finally {
            if( session != null) session.logout();
        }
    }






    @Selector("image.moved")
    public void handleImageMovedEvents(Event<String> evt)
    {
        //do nothing for now
    }



    @Selector("image.deleted")
    public void handleImageDeletedEvents(Event<String> evt)
    {
        //do nothing for now
    }




    private boolean isRootPath(String path)
    {
        // Obvious child nodes we can skip
        if( path.contains(FamilyDAMConstants.RENDITIONS)
                || path.contains(FamilyDAMConstants.THUMBNAIL200)
                || path.contains(FamilyDAMConstants.METADATA)
                || path.contains(JcrConstants.JCR_CONTENT) )
        {
            return false;
        }
        return true;
    }




    private void createJobs(Node node, Session session_) throws RepositoryException
    {
        // create a thumbnail
        Map props = new HashMap();
        props.put("width", 200);
        props.put("height", 200);
        jobQueueServices.addJob(session_, node, "image.thumbnail", props, 80l);
        session_.save();

        // parse the EXIF metadata
        jobQueueServices.addJob(session_, node, "image.metadata", Collections.EMPTY_MAP, 60l);
        session_.save();

        // calculate the phash of the image
        jobQueueServices.addJob(session_, node, "image.phash", Collections.EMPTY_MAP, 40l);
        session_.save();
    }
}
