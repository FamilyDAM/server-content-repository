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
import com.familydam.core.FamilyDAMConstants;
import com.familydam.core.helpers.MimeTypeManager;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.oak.plugins.observation.NodeObserver;
import org.apache.jackrabbit.oak.spi.commit.CommitInfo;
import org.imgscalr.Scalr;

import javax.imageio.ImageIO;
import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

/**
 * Created by mnimer on 9/16/14.
 */
public class ImageThumbnailObserver extends NodeObserver implements Closeable
{
    private Credentials credentials;
    private Repository repository;


    public void setRepository(Repository repository)
    {
        this.repository = repository;
    }


    public ImageThumbnailObserver(String path, String... propertyNames)
    {
        super(path, propertyNames);

        credentials = new SimpleCredentials(FamilyDAM.adminUserId, FamilyDAM.adminPassword.toCharArray());
    }


    @Override
    protected void added(String path, Set<String> added, Set<String> deleted, Set<String> changed, Map<String, String> properties, CommitInfo commitInfo)
    {
        System.out.println("{thumbnail observer} added");

        processImageNode(path);
    }


    @Override
    protected void changed(String path, Set<String> added, Set<String> deleted, Set<String> changed, Map<String, String> properties, CommitInfo commitInfo)
    {
        System.out.println("{thumbnail observer} changed");

        processImageNode(path);
    }


    @Override
    protected void deleted(String path, Set<String> added, Set<String> deleted, Set<String> changed, Map<String, String> properties, CommitInfo commitInfo)
    {
        System.out.println("{thumbnail observer} deleted");

    }


    @Override public void close() throws IOException
    {
        System.out.println("{thumbnail observer} close");
    }




    private void processImageNode(String path)
    {
        Session session = null;
        try{
            session = repository.login(credentials);

            if( path.startsWith("/") )
            {
                path = path.substring(1);
            }

            Node node = JcrUtils.getNodeIfExists(session.getRootNode(), path);
            if( node != null ){
                if( node.isNodeType(FamilyDAMConstants.DAM_IMAGE)) {
                    createThumbnails(session, node);
                }
            }

        }catch(RepositoryException re){
            re.printStackTrace();
        }
        finally {
            if( session != null) {
                session.logout();
            }
        }
    }



    /**
     * Create a small 200x200 thumbnail for each image
     * @param session
     * @param node
     */
    private void createThumbnails(Session session, Node node)
    {
        try {
            InputStream is = JcrUtils.readFile(node);

            BufferedImage image = ImageIO.read(is);

            //thumbnail for search results
            BufferedImage thumbnail = Scalr.resize(image, 200);


            Node renditions = JcrUtils.getOrAddNode(node, FamilyDAMConstants.RENDITIONS, JcrConstants.NT_FOLDER);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(thumbnail, "jpg", baos);
            InputStream thumbnail_is = new ByteArrayInputStream(baos.toByteArray());

            JcrUtils.putFile(renditions, FamilyDAMConstants.THUMBNAIL200, MimeTypeManager.JPG.name(), thumbnail_is);

            session.save();
        }catch(IOException|RepositoryException re){
            re.printStackTrace();
        }
    }

}
