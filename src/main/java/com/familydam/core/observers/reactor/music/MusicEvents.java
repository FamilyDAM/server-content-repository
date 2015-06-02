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

package com.familydam.core.observers.reactor.music;

import com.familydam.core.FamilyDAM;
import com.familydam.core.FamilyDAMConstants;
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
import javax.jcr.SimpleCredentials;
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

    Session session = null;

    @Selector("file.added")
    public void handleImageAddedEvents(Event<String> evt)
    {
        String path = evt.getData();

        SimpleCredentials credentials = new SimpleCredentials(FamilyDAM.adminUserId, FamilyDAM.adminPassword.toCharArray());
        try {
            Session session = repository.login(credentials);


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
        }catch(RepositoryException re){
            reactor.notify("error", Event.wrap(re.getMessage()));
        }

    }


    @Selector("file.changed")
    public void handleImageChangedEvents(Event<String> evt)
    {
        String path = evt.getData();

        SimpleCredentials credentials = new SimpleCredentials(FamilyDAM.adminUserId, FamilyDAM.adminPassword.toCharArray());
        Session session = null;
        try {
            session = repository.login(credentials);


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
        }catch(RepositoryException re){
            reactor.notify("error", Event.wrap(re.getMessage()));
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
