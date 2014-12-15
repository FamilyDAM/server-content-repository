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
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.oak.plugins.observation.NodeObserver;
import org.apache.jackrabbit.oak.spi.commit.CommitInfo;

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
 */
public class ImageExifObserver extends NodeObserver implements Closeable
{
    private Credentials credentials;
    private Repository repository;


    public void setRepository(Repository repository)
    {
        this.repository = repository;
    }


    public ImageExifObserver(String path, String... propertyNames)
    {
        super(path, propertyNames);

        credentials = new SimpleCredentials(FamilyDAM.adminUserId, FamilyDAM.adminPassword.toCharArray());
    }


    @Override
    protected void added(String path, Set<String> added, Set<String> deleted, Set<String> changed, Map<String, String> properties, CommitInfo commitInfo)
    {
        System.out.println("{exif observer} added");

        processImageNode(path);

    }


    @Override
    protected void changed(String path, Set<String> added, Set<String> deleted, Set<String> changed, Map<String, String> properties, CommitInfo commitInfo)
    {
        System.out.println("{exif observer} changed");

        processImageNode(path);
    }


    @Override
    protected void deleted(String path, Set<String> added, Set<String> deleted, Set<String> changed, Map<String, String> properties, CommitInfo commitInfo)
    {
        System.out.println("{exif observer} deleted");

    }


    @Override public void close() throws IOException
    {
        System.out.println("{exif observer} close");
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
                    parseExif(session, node);
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



    private void parseExif(Session session, Node image) throws RepositoryException
    {

    }


}
