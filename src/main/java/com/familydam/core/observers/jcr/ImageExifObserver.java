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

package com.familydam.core.observers.jcr;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.familydam.core.FamilyDAM;
import com.familydam.core.FamilyDAMConstants;
import org.apache.jackrabbit.JcrConstants;
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
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Created by mnimer on 9/16/14.
 * @Deprectated by Reactor observers
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
        //System.out.println("{exif observer} added | " +path);

        processImageNode(path);

    }


    @Override
    protected void changed(String path, Set<String> added, Set<String> deleted, Set<String> changed, Map<String, String> properties, CommitInfo commitInfo)
    {
        //System.out.println("{exif observer} changed | " +path);

        //processImageNode(path);
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

                    System.out.println("{EXIF Image Observer} " +node.getPath());

                    parseExif(session, node);
                    session.save();
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
        if( image.isNodeType(FamilyDAMConstants.DAM_IMAGE)) {
            InputStream is = JcrUtils.readFile(image);
            Node metadataNode = JcrUtils.getOrAddNode(image, FamilyDAMConstants.METADATA, JcrConstants.NT_UNSTRUCTURED);

            try {
                Metadata metadata = ImageMetadataReader.readMetadata(is);

                Iterable<Directory> directories = metadata.getDirectories();

                for (Directory directory : directories) {
                    String _name = directory.getName();
                    Node dir = JcrUtils.getOrAddNode(metadataNode, _name, JcrConstants.NT_UNSTRUCTURED);

                    Collection<Tag> tags = directory.getTags();
                    for (Tag tag : tags) {
                        int tagType = tag.getTagType();
                        String tagTypeHex = tag.getTagTypeHex();
                        String tagName = tag.getTagName();
                        String nodeName = tagName.replace(" ", "_").replace("/", "_");
                        String desc = tag.getDescription();

                        Node prop = JcrUtils.getOrAddNode(dir, nodeName, JcrConstants.NT_UNSTRUCTURED);
                        prop.setProperty("name", tagName);
                        prop.setProperty("description", desc);
                        prop.setProperty("type", tagType);
                        prop.setProperty("typeHex", tagTypeHex);
                    }
                }

            }catch(ImageProcessingException|IOException ex){
                ex.printStackTrace();
                //swallow
            }
        }
    }


}
