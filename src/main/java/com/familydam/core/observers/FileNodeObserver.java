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
import com.familydam.core.helpers.MimeTypeManager;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.oak.plugins.observation.NodeObserver;
import org.apache.jackrabbit.oak.spi.commit.CommitInfo;
import org.apache.jackrabbit.oak.spi.state.NodeState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import reactor.core.Reactor;
import reactor.event.Event;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
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
public class FileNodeObserver extends NodeObserver implements Closeable
{

    @Autowired private Reactor reactor;
    @Autowired private ApplicationContext context;
    private Repository repository;
    private Credentials credentials;
    private Session session;

    String propName = "";


    public FileNodeObserver(String path, String... propertyNames)
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
        if( repository == null ){
            repository = context.getBean(Repository.class);
        }

        //System.out.println("{dir observer} added");

        // apply mixins
        this.applyMixins(path);


    }


    @Override
    protected void changed(String path, Set<String> added, Set<String> deleted, Set<String> changed, Map<String, String> properties, CommitInfo commitInfo)
    {
        if( repository == null ){
            repository = context.getBean(Repository.class);
        }
        //System.out.println("{dir observer} changed");
        reactor.notify("file.changed", Event.wrap(path));

    }


    @Override
    protected void deleted(String path, Set<String> added, Set<String> deleted, Set<String> changed, Map<String, String> properties, CommitInfo commitInfo)
    {
        if( repository == null ){
            repository = context.getBean(Repository.class);
        }
        //System.out.println("{dir observer} deleted");
        reactor.notify("file.deleted", Event.wrap(path));
    }



    @Override public void close() throws IOException
    {
        System.out.println("{dir observer} close");
    }




    /**
     * apply mixins
     * @param path
     * @throws RepositoryException
     */
    protected void applyMixins(String path)
    {

        try {
            Session session = repository.login(credentials);
            Node fileNode = session.getNode(path);

            if( fileNode.isNodeType(JcrConstants.NT_FILE) ) {

                String mimeType = fileNode.getNode(JcrConstants.JCR_CONTENT).getProperty(JcrConstants.JCR_MIMETYPE).getString();

                //first assign the right mixins
                // generic mixin for all user uploaded files (so we can separate users files from system generated revisions
                fileNode.addMixin("dam:file");
                // supports String[] TAGS
                fileNode.addMixin("dam:taggable");
                // catch all to allow any property
                fileNode.addMixin("dam:extensible");
                // make all files versionable
                fileNode.addMixin(JcrConstants.MIX_VERSIONABLE);
                // make all files referencable
                fileNode.addMixin(JcrConstants.MIX_REFERENCEABLE);


                // Check the mime type to decide if it's more then a generic file
                if (MimeTypeManager.isSupportedImageMimeType(mimeType)) {
                    fileNode.addMixin("dam:image");

                    //System.out.println("{dir observer | " +propName +"} added | " + path);
                    reactor.notify("image.added", Event.wrap(path));
                }

                if (MimeTypeManager.isSupportedMusicMimeType(mimeType)) {
                    fileNode.addMixin("dam:music");

                    //System.out.println("{dir observer | " +propName +"} added | " + path);
                    reactor.notify("music.added", Event.wrap(path));
                }

                if (MimeTypeManager.isSupportedVideoMimeType(mimeType)) {
                    fileNode.addMixin("dam:video");

                    //System.out.println("{dir observer | " +propName +"} added | " + path);
                    reactor.notify("video.added", Event.wrap(path));
                }

                session.save();
            }
        }catch(RepositoryException re){
            re.printStackTrace();
        }
    }
}
