/*
 * Copyright (c) 2015  Mike Nimer & 11:58 Labs
 */

package com.familydam.core.observers.reactor.images;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.familydam.core.FamilyDAMConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import reactor.spring.context.annotation.Consumer;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

/**
 * Created by mnimer on 12/23/14.
 */
@Consumer
public class ExifObserver
{
    private Log log = LogFactory.getLog(this.getClass());

    private SimpleDateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd");


    public void execute(Session session, Node node) throws RepositoryException, ImageProcessingException, IOException
    {
        if (node != null) {
            if (node.isNodeType(FamilyDAMConstants.DAM_IMAGE)) {
                log.debug("{image.metadata Observer} " + node.getPath());

                // create renditions
                if (node.isNodeType(FamilyDAMConstants.DAM_IMAGE)) {
                    InputStream is = JcrUtils.readFile(node);
                    Node metadataNode = JcrUtils.getOrAddNode(node, FamilyDAMConstants.METADATA, JcrConstants.NT_UNSTRUCTURED);


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


                    // Extract Image Date Stamp, and save to root
                    Date date = new Date();
                    if (metadata.getDirectory(ExifIFD0Directory.class) != null) {
                        Date metadataDate = metadata.getDirectory(ExifIFD0Directory.class).getDate(306);
                        if (metadataDate != null) {
                            date = metadataDate;
                        }
                    }
                    String dateCreated = dateFormat.format(date);
                    node.setProperty(FamilyDAMConstants.DAM_DATECREATED, dateCreated);

                    session.save();


                }


            }
        }
    }


}
