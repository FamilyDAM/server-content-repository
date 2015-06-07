/*
 * Copyright (c) 2015  Mike Nimer & 11:58 Labs
 */

package com.familydam.core.observers.reactor.images;

import com.familydam.core.FamilyDAMConstants;
import com.familydam.core.helpers.ImagePHash;
import com.familydam.core.services.ImageRenditionsService;
import com.familydam.core.services.JobQueueServices;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.commons.JcrUtils;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.Reactor;
import reactor.spring.context.annotation.Consumer;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.InputStream;

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



    public void execute(Session session, Node node) throws RepositoryException
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
