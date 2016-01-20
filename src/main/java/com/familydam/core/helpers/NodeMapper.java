/*
 * Copyright (c) 2015  Mike Nimer & 11:58 Labs
 */

package com.familydam.core.helpers;

import com.familydam.core.FamilyDAMConstants;
import com.familydam.core.exceptions.UnknownINodeException;
import com.familydam.core.models.DamImage;
import com.familydam.core.models.Directory;
import com.familydam.core.models.File;
import com.familydam.core.models.INode;
import org.apache.jackrabbit.JcrConstants;
import org.jetbrains.annotations.NotNull;

import javax.el.PropertyNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

/**
 * Created by mnimer on 2/18/15.
 */
public class NodeMapper
{

    public static INode map(Node node) throws RepositoryException,UnknownINodeException
    {

        if (node.getPrimaryNodeType().isNodeType(JcrConstants.NT_FOLDER)) {
            return mapDirectoryNode(node);
            
        } else if (node.getPrimaryNodeType().isNodeType(JcrConstants.NT_FILE)) {
            return mapFileNode(node);
        }
        
        throw new UnknownINodeException("no mapping for type " +node.getPrimaryNodeType().getName());
    }



    @NotNull private static INode mapDirectoryNode(Node node) throws RepositoryException
    {
        // return map for every file
        Directory directory = new Directory();
        directory.setId(node.getIdentifier());
        if( node.hasProperty(JcrConstants.JCR_NAME) ) {
            directory.setName(node.getProperty(JcrConstants.JCR_NAME).getString());
        }else{
            directory.setName(node.getName());
        }

        directory.setPath(node.getPath());
        if (node.hasProperty("order")) {
            directory.setOrder(new Long(node.getProperty("order").getLong()).intValue());
        }
        directory.setParent(node.getParent().getPath());
        directory.setChildren(Collections.EMPTY_LIST);
        directory.setIsReadOnly(false);

        Collection<String> _mixins = new ArrayList();
        for (NodeType nodeType : node.getMixinNodeTypes()) {
            _mixins.add(nodeType.getName());
        }
        directory.setMixins(_mixins);

        try {
            if (node.getProperty("loading") != null) {
                directory.setLoading(true);
            }
        }catch(PathNotFoundException|PropertyNotFoundException pe){
            directory.setLoading(false);
        }

        return directory;
    }



    @NotNull private static INode mapFileNode(Node node) throws RepositoryException
    {
        // return map for every file
        File file = new File();

        // Add image specific properties
        if( node.isNodeType(FamilyDAMConstants.DAM_IMAGE) )
        {
            file = new DamImage();
            try {
                ((DamImage) file).setWidth(node.getProperty(FamilyDAMConstants.WIDTH).getDouble());
                ((DamImage) file).setHeight(node.getProperty(FamilyDAMConstants.HEIGHT).getDouble());
            }catch(Exception ex){
                //set a default
                ((DamImage) file).setWidth(250d);
                ((DamImage) file).setHeight(250d);
            }
        }

        file.setId(node.getIdentifier());
        if( node.hasProperty(JcrConstants.JCR_NAME) ) {
            file.setName(node.getProperty(JcrConstants.JCR_NAME).getString());
        }else{
            file.setName(node.getName());
        }
        file.setPath(node.getPath());
        if (node.hasProperty("order")) {
            file.setOrder(new Long(node.getProperty("order").getLong()).intValue());
        }

        file.setParent(node.getParent().getPath());
        file.setIsReadOnly(false);

        Collection<String> _mixins = new ArrayList();
        for (NodeType nodeType : node.getMixinNodeTypes()) {
            _mixins.add(nodeType.getName());
        }
        file.setMixins(_mixins);

        Collection<String> _tags = new ArrayList();
        if( node.hasProperty("dam:tags") ){
            Value[] values = node.getProperty("dam:tags").getValues();
            for (int i = 0; i < values.length; i++) {
                _tags.add(values[i].getString());
            }
        }
        file.setTags(_tags);

        Collection<String> _people = new ArrayList();
        if( node.hasProperty("dam:people") ){
            Value[] values = node.getProperty("dam:people").getValues();
            for (int i = 0; i < values.length; i++) {
                _people.add(values[i].getString());
            }
        }
        file.setPeople(_people);



        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date date = sdf.parse(node.getProperty(FamilyDAMConstants.DAM_DATECREATED).getString());
            Calendar cal = sdf.getCalendar();
            file.setDateCreated(cal);
        }catch(ParseException pe ){
            //todo: decide what to do
        }

        return file;
    }


}
