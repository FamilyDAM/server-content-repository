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
import org.apache.jackrabbit.commons.JcrUtils;
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

/**
 * Created by mnimer on 12/23/14.
 */
@Consumer
public class ImageEvents
{
    @Autowired private Reactor reactor;
    @Autowired private Repository repository;

    @Selector("image.added")
    public void handleImageAddedEvents(Event<String> evt)
    {
        String path = evt.getData();

        SimpleCredentials credentials = new SimpleCredentials(FamilyDAM.adminUserId, FamilyDAM.adminPassword.toCharArray());
        Session session = null;
        try {
            session = repository.login(credentials);

            if (path.startsWith("/")) {
                path = path.substring(1);
            }

            Node node = JcrUtils.getNodeIfExists(session.getRootNode(), path);

            if (node != null) {
                if (node.isNodeType(FamilyDAMConstants.DAM_IMAGE)) {
                    // create a 200x200 thumbnail
                    reactor.notify("image." + FamilyDAMConstants.THUMBNAIL200, Event.wrap(node.getPath()));
                    // parse the EXIF metadata
                    reactor.notify("image.metadata", Event.wrap(node.getPath()));
                    // calculate the PHASH of the image
                    reactor.notify("image.phash", Event.wrap(node.getPath()));
                }
            }
        }catch(RepositoryException re){
            reactor.notify("error", Event.wrap(re.getMessage()));
        }

    }



    @Selector("image.changed")
    public void handleImageChangedEvents(Event<String> evt)
    {
        String path = evt.getData();

        SimpleCredentials credentials = new SimpleCredentials(FamilyDAM.adminUserId, FamilyDAM.adminPassword.toCharArray());
        Session session = null;
        try {
            session = repository.login(credentials);

            if (path.startsWith("/")) {
                path = path.substring(1);
            }

            Node node = JcrUtils.getNodeIfExists(session.getRootNode(), path);

            if (node != null) {
                if (node.isNodeType(FamilyDAMConstants.DAM_IMAGE)) {
                    // update the 200x200 thumbnail
                    reactor.notify("image." + FamilyDAMConstants.THUMBNAIL200, Event.wrap(node.getPath()));
                    // parse the EXIF metadata
                    reactor.notify("image.metadata", Event.wrap(node.getPath()));
                    // calculate the PHASH of the image
                    reactor.notify("image.phash", Event.wrap(node.getPath()));
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
}
