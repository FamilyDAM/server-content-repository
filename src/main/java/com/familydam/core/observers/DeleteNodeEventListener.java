/*
 * Copyright (c) 2015  Mike Nimer & 11:58 Labs
 */

package com.familydam.core.observers;

import com.familydam.core.FamilyDAMConstants;
import com.familydam.core.helpers.MimeTypeManager;
import com.familydam.core.services.AuthenticatedHelper;
import com.familydam.core.services.JobQueueServices;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.api.observation.JackrabbitEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import reactor.core.Reactor;
import reactor.event.Event;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.security.sasl.AuthenticationException;

/**
 * Created by mnimer on 12/27/15.
 */
public class DeleteNodeEventListener implements EventListener
{
    private Log log = LogFactory.getLog(this.getClass());


    @Autowired private Reactor reactor;
    @Autowired private ApplicationContext context;
    @Autowired private JobQueueServices jobQueueServices;


    private AuthenticatedHelper authenticatedHelper;
    private Repository repository;


    public DeleteNodeEventListener(Repository repository)
    {
        this.repository = repository;
    }


    @Override public void onEvent(EventIterator events)
    {
        while(events.hasNext())
        {
            try {
                JackrabbitEvent _event = (JackrabbitEvent) events.next();
                if (_event.getInfo().get(JcrConstants.JCR_PRIMARYTYPE).equals("nt:file")) {
                    System.out.println(_event.toString());

                    if( authenticatedHelper == null){
                        authenticatedHelper = context.getBean(AuthenticatedHelper.class);
                    }

                    Session session = null;
                    try {
                        session = authenticatedHelper.getAdminSession();

                        String _path = _event.getPath();
                        Node node = session.getNode(_path);

                        // System.out.println("{dir observer} added");
                        // Apply mixins
                        if( node.getPrimaryNodeType().isNodeType(JcrConstants.NT_FILE) ) {
                            // trigger the real event system
                            reactor.notify("file.deleted", Event.wrap(_path));
                        }
                    }catch(Exception ex){
                        log.error(ex);
                    }finally {
                        if( session != null) session.logout();
                    }


                }
            }catch(RepositoryException re){
                re.printStackTrace();
                //swallow
            }
        }
    }



    /**
     * apply mixins
     * @param path
     * @throws RepositoryException
     */
    protected void applyMixins(String path)
    {

        try {
            Session session = authenticatedHelper.getAdminSession();
            Node fileNode = session.getNode(path);

            if( fileNode.isNodeType(JcrConstants.NT_FILE) ) {

                // Obvious child nodes we can skip
                if (path.contains(FamilyDAMConstants.RENDITIONS)
                        || path.contains(FamilyDAMConstants.THUMBNAIL200)
                        || path.contains(FamilyDAMConstants.METADATA)
                        || path.contains(JcrConstants.JCR_CONTENT)) {
                    return;
                }

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
                }

                if (MimeTypeManager.isSupportedMusicMimeType(mimeType)) {
                    fileNode.addMixin("dam:music");
                }

                if (MimeTypeManager.isSupportedVideoMimeType(mimeType)) {
                    fileNode.addMixin("dam:video");
                }

                session.save();


            }
        }catch(RepositoryException|AuthenticationException re){
            re.printStackTrace();
        }
    }
}
