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

package com.familydam.core.observers;

import com.familydam.core.FamilyDAM;
import org.apache.jackrabbit.oak.plugins.observation.NodeObserver;
import org.apache.jackrabbit.oak.spi.commit.CommitInfo;
import org.apache.jackrabbit.oak.spi.state.NodeState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import reactor.core.Reactor;
import reactor.event.Event;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Created by mnimer on 9/16/14.
 * @Deprectated by Reactor observers
 */
@Lazy
public class ImageNodeObserver extends NodeObserver implements Closeable
{

    @Autowired private Reactor reactor;
    @Autowired private ApplicationContext context;
    private Repository repository;
    private Credentials credentials;
    private Session session;

    String propName = "";


    public ImageNodeObserver(String path, String... propertyNames)
    {
        super(path, propertyNames);
        propName = propertyNames[0];
        credentials = new SimpleCredentials(FamilyDAM.adminUserId, FamilyDAM.adminPassword.toCharArray());
    }


    @Override public void contentChanged(NodeState root, CommitInfo info)
    {
        super.contentChanged(root, info);
    }


    @Override
    protected void added(String path, Set<String> added, Set<String> deleted, Set<String> changed, Map<String, String> properties, CommitInfo commitInfo)
    {
        //System.out.println("{dir observer | " +propName +"} added | " + path);
        reactor.notify("image.added", Event.wrap(path));

    }


    @Override
    protected void changed(String path, Set<String> added, Set<String> deleted, Set<String> changed, Map<String, String> properties, CommitInfo commitInfo)
    {
        //System.out.println("{dir observer} changed | " + path);
        reactor.notify("image.changed", Event.wrap(path));

    }


    @Override
    protected void deleted(String path, Set<String> added, Set<String> deleted, Set<String> changed, Map<String, String> properties, CommitInfo commitInfo)
    {
        //System.out.println("{dir observer} deleted");
        reactor.notify("file.deleted", Event.wrap(path));

        //todo check node type
        reactor.notify("image.deleted", Event.wrap(path));
    }


    @Override public void close() throws IOException
    {
        System.out.println("{dir observer} close");
    }

}
