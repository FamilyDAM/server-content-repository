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

import com.familydam.core.FamilyDAMConstants;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.oak.api.CommitFailedException;
import org.apache.jackrabbit.oak.api.PropertyState;
import org.apache.jackrabbit.oak.api.Tree;
import org.apache.jackrabbit.oak.api.Type;
import org.apache.jackrabbit.oak.util.NodeUtil;
import org.apache.jackrabbit.value.BinaryValue;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.jcr.AccessDeniedException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by mnimer on 9/23/14.
 */
public class PropertyUtil
{


    /**
     * Populate the return object with simple properties, nested objects, and child nodes (for nt_folders)
     * @param node
     * @return
     */
    public static Map readProperties(Tree node)
    {
        Map<String, Object> nodeProps = new HashMap();

        // change the real path to match the REST path
        nodeProps.put(JcrConstants.JCR_PATH, node.getPath().replace("/dam/", "/~/"));
        nodeProps.put(JcrConstants.JCR_NAME, node.getName());

        // get simple properties
        for (PropertyState propertyState : node.getProperties()) {
            // add all properties, but the binary content (to reduce size.)
            // binary content can be returned by a direct request for that node
            if( !propertyState.getName().equals(JcrConstants.JCR_CONTENT) ) {
                String _name = propertyState.getName();
                nodeProps.put(_name, propertyState.getValue(propertyState.getType()));
            }
        }

        PropertyUtil.readPropertyTree(node, nodeProps);

        if( node.getProperty(JcrConstants.JCR_PRIMARYTYPE).getValue(Type.STRING).equals(JcrConstants.NT_FOLDER) ) {
            PropertyUtil.readChildFolders(node, nodeProps);
        }


        return nodeProps;
    }


    /**
     * Walk the NT_UNSTRUCTURED tree adding any nest map or array objects
     * @param node
     * @param nodeProps
     */
    public static void readPropertyTree(Tree node, Map<String, Object> nodeProps){

        for (Tree propTree : node.getChildren()) {
            if( propTree.getProperty(JcrConstants.JCR_PRIMARYTYPE).getValue(Type.STRING).equals(NodeType.NT_UNSTRUCTURED) ) {

                //first, get the name
                Map<String, Object> propMap =  new HashMap();
                nodeProps.put(propTree.getName(), propMap);

                for (PropertyState propertyState : propTree.getProperties()) {
                    //skip name,and node type
                    if(!propertyState.getName().equals(JcrConstants.JCR_NAME) && !propertyState.getName().equals(JcrConstants.JCR_PRIMARYTYPE)) {
                        String _name = propertyState.getName();
                        propMap.put(_name, propertyState.getValue(propertyState.getType()));
                    }
                }

                readPropertyTree(propTree, propMap);
            }
        }
    }


    /**
     * Go down a level and add all NT_FOLDER nodes to "children" property
     * @param node
     * @param nodeProps
     */
    public static void readChildFolders(Tree node, Map<String, Object> nodeProps){

        if( nodeProps.get(FamilyDAMConstants.CHILDREN) == null ){
            nodeProps.put(FamilyDAMConstants.CHILDREN, new ArrayList());
        }

        // Find the child folders
        for (Tree childFolder : node.getChildren()) {
            if (childFolder.getProperty(JcrConstants.JCR_PRIMARYTYPE).getValue(Type.STRING).equals(JcrConstants.NT_FILE)
                    || childFolder.getProperty(JcrConstants.JCR_PRIMARYTYPE).getValue(Type.STRING).equals(JcrConstants.NT_FOLDER)
                    || childFolder.getProperty(JcrConstants.JCR_PRIMARYTYPE).getValue(Type.STRING).equals(JcrConstants.NT_HIERARCHYNODE)) {

                Map<String, Object> propMap = PropertyUtil.readProperties(childFolder);

                ((List)nodeProps.get(FamilyDAMConstants.CHILDREN)).add(propMap);
            }
        }

    }








    public static void writeParametersToNode(NodeUtil newNode, Map<String, String[]> parameters) throws AccessDeniedException, CommitFailedException
    {
        for (String key : parameters.keySet()) {
            String[] values = parameters.get(key);

            // override to create nested MAP properties
            NodeUtil currentNode = newNode;
            if( key.matches("[0-9a-zA-Z]+\\.[0-9a-zA-Z\\w\\.]+")) {
                String propertyPath = key.replace(".", "/");
                int lastSpotPos = propertyPath.lastIndexOf("/");

                currentNode = newNode.getOrAddTree(propertyPath.substring(0, lastSpotPos), NodeType.NT_UNSTRUCTURED);
                key = propertyPath.substring(lastSpotPos+1);
            }

            //todo add support for Date, based on predefined format
            if (values.length == 1) {

                if (values[0].equalsIgnoreCase("true") || values[0].equalsIgnoreCase("yes") || values[0].equals("1")) {
                    currentNode.setBoolean(key, true);
                } else if (values[0].equalsIgnoreCase("false") || values[0].equalsIgnoreCase("no") || values[0].equals("0")) {
                    currentNode.setBoolean(key, false);
                } else {
                    currentNode.setString(key, values[0]);
                }

            } else {
                currentNode.setStrings(key, values);
            }
        }
    }



    public static void writeJsonToNode(NodeUtil newNode, String jsonBody)
    {
        throw new RuntimeException("Not Implemented Yet");
    }


    public static NodeUtil writeFileToNode(NodeUtil newNode, MultipartHttpServletRequest request) throws IOException, AccessDeniedException
    {
        if( request.getFileMap() != null )
        {
            for (String key : request.getFileMap().keySet() ) {
                MultipartFile file = request.getFile(key);

                Value[] content = new Value[1];
                content[0] = new BinaryValue(file.getInputStream());

                if( newNode.getTree().getProperty(JcrConstants.JCR_PRIMARYTYPE).getValue(Type.STRING) == JcrConstants.NT_FOLDER ) {
                    NodeUtil fileNode = newNode.addChild(key, "nt:file");
                    fileNode.setString(JcrConstants.JCR_UUID, UUID.randomUUID().toString());
                    fileNode.setString(JcrConstants.JCR_NAME, key);
                    fileNode.setString(JcrConstants.JCR_CREATED, "todo");//session.getAuthInfo().getUserID());
                    fileNode.setString(JcrConstants.JCR_MIMETYPE, file.getContentType());
                    fileNode.setValues(JcrConstants.JCR_CONTENT, content);

                    return fileNode;
                }else{
                    // Update
                    newNode.setValues(JcrConstants.JCR_CONTENT, content);
                    return newNode;
                }
            }
        }

        return newNode;
    }


}
