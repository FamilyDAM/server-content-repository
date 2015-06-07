/*
 * Copyright (c) 2015  Mike Nimer & 11:58 Labs
 */

package com.familydam.core.observers.reactor.music;

import com.familydam.core.FamilyDAMConstants;
import com.familydam.core.services.AuthenticatedHelper;
import com.familydam.core.services.JobQueueServices;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.JcrConstants;
import org.springframework.beans.factory.annotation.Autowired;
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

/**
 * Created by mnimer on 12/23/14.
 */
@Consumer
public class MusicEvents
{

    private Log log = LogFactory.getLog(this.getClass());

    @Autowired private Reactor reactor;
    @Autowired private Repository repository;
    @Autowired private JobQueueServices jobQueueServices;
    @Autowired private AuthenticatedHelper authenticatedHelper;

    Session session = null;

    @Selector("file.added")
    public void handleImageAddedEvents(Event<String> evt)
    {
        String path = evt.getData();

        try {
            Session session = authenticatedHelper.getAdminSession();


            // Obvious child nodes we can skip
            if (!isRootPath(path)) {
                return;
            }


            Node node = session.getNode(path);

            if (node != null) {
                if (node.isNodeType(FamilyDAMConstants.DAM_MUSIC)) {

                    jobQueueServices.addJob(session, node, "mp3.metadata", Collections.EMPTY_MAP, 100l);

                    // save the jobs
                    session.save();

                    // create a 200x200 thumbnail
                    //reactor.notify("image." + FamilyDAMConstants.THUMBNAIL200, Event.wrap(node.getPath()));
                    // parse the EXIF metadata
                    //reactor.notify("image.metadata", Event.wrap(node.getPath()));
                    // calculate the PHASH of the image
                    //reactor.notify("image.phash", Event.wrap(node.getPath()));
                }
            }
        }catch(RepositoryException|AuthenticationException re){
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
                if (node.isNodeType(FamilyDAMConstants.DAM_MUSIC)) {

                    jobQueueServices.addJob(session, node, "mp3.metadata", Collections.EMPTY_MAP, 100l);
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


}
