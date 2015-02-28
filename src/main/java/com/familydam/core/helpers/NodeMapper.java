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

package com.familydam.core.helpers;

import com.familydam.core.exceptions.UnknownINodeException;
import com.familydam.core.models.Directory;
import com.familydam.core.models.File;
import com.familydam.core.models.INode;
import org.apache.jackrabbit.JcrConstants;
import org.jetbrains.annotations.NotNull;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

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


    @NotNull private static INode mapFileNode(Node node) throws RepositoryException
    {
        // return map for every file
        File file = new File();
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

        return file;
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

        return directory;
    }
}
