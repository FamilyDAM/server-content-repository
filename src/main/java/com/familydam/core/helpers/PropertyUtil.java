/*
 * Copyright (c) 2015  Mike Nimer & 11:58 Labs
 */

package com.familydam.core.helpers;

import com.familydam.core.FamilyDAMConstants;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.oak.api.CommitFailedException;
import org.apache.jackrabbit.oak.util.NodeUtil;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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
    public static Map readProperties(Node node) throws RepositoryException
    {
        Map<String, Object> nodeProps = new HashMap();

        // change the real path to match the REST path
        nodeProps.put(JcrConstants.JCR_PATH, node.getPath());
        nodeProps.put(JcrConstants.JCR_NAME, node.getName());

        // get simple properties
        PropertyIterator propertyIterator = node.getProperties();
        while (propertyIterator.hasNext()) {
            Property property = (Property) propertyIterator.next();
            String _name = property.getName();
            // add all properties, but the binary content (to reduce size.)
            // binary content can be returned by a direct request for that node

            if (!property.isMultiple()) {


                if (!property.getName().equals(JcrConstants.JCR_DATA)) //skip data nodes
                {
                    if (!property.isNode()) {
                        String _value = property.getString();
                        nodeProps.put(_name, _value); //todo, make this dynamic based on type.
                    } else {
                        Map childProps = PropertyUtil.readProperties((Node) property);
                        nodeProps.put(_name, childProps);
                    }
                }

            } else {
                // HANDLE ARRAY Types
                int _type = property.getType();

                Value[] values = property.getValues();
                Object[] _values = new Object[values.length];
                for (int i = 0; i < values.length; i++) {
                    Value value = values[i];
                    _values[i] = value.getString();
                }

                nodeProps.put(_name, _values);
            }
        }


        Iterable<Node> _childNodes = JcrUtils.getChildNodes(node);
        for (Node childNode : _childNodes) {
            String _childNodeName = childNode.getName();
            Map _childNodeProps = PropertyUtil.readProperties(childNode);

            nodeProps.put(_childNodeName, _childNodeProps);
        }

        return nodeProps;
    }





    public static void writeParametersToNode(Node node_, Map params_) throws AccessDeniedException, CommitFailedException, RepositoryException
    {
        for (Object key : params_.keySet()) {

            Object _val = params_.get(key);


            if( _val instanceof Map ){
                Node _currentNode = JcrUtils.getOrAddNode(node_, key.toString(), JcrConstants.NT_UNSTRUCTURED);
                writeParametersToNode(_currentNode, (Map) _val);
            }else{

                boolean hasProperty = node_.hasProperty(key.toString());

                // skip some system paths, like jcr:path
                if( key.toString().startsWith("jcr:") && !hasProperty )
                {
                    continue;
                }

                // check & skip known protected properties
                if( hasProperty && node_.getProperty(key.toString()).getDefinition().isProtected() ){
                    continue;
                }


                //get current value, so we only update changed values
                Value _currentProp = null;
                Value[] _currentMultiProp = null;
                if( hasProperty && !node_.getProperty(key.toString()).isMultiple() ) {
                    _currentProp = node_.getProperty(key.toString()).getValue();
                }else if( hasProperty && node_.getProperty(key.toString()).isMultiple() ) {
                    _currentMultiProp = node_.getProperty(key.toString()).getValues();
                }

                if( _val instanceof Collection){
                    String[] _arrVal = ((ArrayList<String>)_val).toArray( new String[((ArrayList<String>)_val).size()] );
                    node_.setProperty(key.toString(), _arrVal);
                }
                else if( _val instanceof Boolean && (!hasProperty || _currentProp.getBoolean() != (Boolean)_val) ){
                    node_.setProperty(key.toString(), (Boolean)_val );
                }
                else if( _val instanceof BigDecimal && (!hasProperty || _currentProp.getDecimal() != (BigDecimal)_val) ){
                    node_.setProperty(key.toString(), (BigDecimal)_val );
                }
                else if( _val instanceof Long  && (!hasProperty || _currentProp.getLong() != (Long)_val)  ){
                    node_.setProperty(key.toString(), (Long)_val );
                }
                else if( _val instanceof Double  && (!hasProperty || _currentProp.getDouble() != (Double)_val)  ){
                    node_.setProperty(key.toString(), (Double)_val );
                }
                else if( _val instanceof String[]  ){
                    node_.setProperty(key.toString(), (String[])_val );
                }
                else {
                    if( _val != null ){
                        _val = _val.toString();

                        if( !hasProperty ||  !_currentProp.getString().equals(_val) ) {
                            node_.setProperty(key.toString(), (String) _val);
                        }
                    }
                }
            }
        }
    }



    public static void writeJsonToNode(NodeUtil newNode, String jsonBody)
    {
        throw new RuntimeException("Not Implemented Yet");
    }


    /**
     * Walk the NT_UNSTRUCTURED tree adding any nest map or array objects
     * @param node
     * @param nodeProps

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
    }*/


    /**
     * Go down a level and add all NT_FOLDER nodes to "children" property
     * @param node
     * @param nodeProps

    public static void readChildFolders(Tree node, Map<String, Object> nodeProps){

    if( nodeProps.get(FamilyDAMConstants.CHILDREN) == null ){
    nodeProps.put(FamilyDAMConstants.CHILDREN, new ArrayList());
    }

    // Find the child folders
    for (Tree childFolder : node.getChildren()) {
    if (childFolder.getProperty(JcrConstants.JCR_PRIMARYTYPE).getValue(Type.STRING).equals(JcrConstants.NT_FILE)
    || childFolder.getProperty(JcrConstants.JCR_PRIMARYTYPE).getValue(Type.STRING).equals(JcrConstants.NT_FOLDER)
    || childFolder.getProperty(JcrConstants.JCR_PRIMARYTYPE).getValue(Type.STRING).equals(JcrConstants.NT_HIERARCHYNODE)) {

    //Map<String, Object> propMap = PropertyUtil.readProperties(childFolder);

    //((List)nodeProps.get(FamilyDAMConstants.CHILDREN)).add(propMap);
    }
    }

    }*/

    /**
    public static NodeUtil writeFileToNode(NodeUtil node_, MultipartHttpServletRequest request) throws IOException, AccessDeniedException
    {
        if( request.getFileMap() != null )
        {
            for (String key : request.getFileMap().keySet() ) {
                MultipartFile file = request.getFile(key);

                Value[] content = new Value[1];
                content[0] = new BinaryValue(file.getInputStream());

                if( node_.getTree().getProperty(JcrConstants.JCR_PRIMARYTYPE).getValue(Type.STRING).equalsIgnoreCase(JcrConstants.NT_FOLDER) )
                {
                    String fileName = file.getOriginalFilename();
                    NodeUtil fileNode = node_.addChild(fileName, "nt:file");
                    fileNode.setString(JcrConstants.JCR_UUID, UUID.randomUUID().toString());
                    fileNode.setString(JcrConstants.JCR_NAME, fileName);
                    fileNode.setString(JcrConstants.JCR_CREATED, "todo");//session.getAuthInfo().getUserID());
                    if( file.getContentType() != null ) {
                        fileNode.setString(JcrConstants.JCR_MIMETYPE, file.getContentType());
                    }else{
                        String type = MimeTypeManager.getMimeType(fileName);
                        fileNode.setString(JcrConstants.JCR_MIMETYPE, type);
                    }
                    fileNode.setValues(JcrConstants.JCR_CONTENT, content);

                    return fileNode;
                }else{
                    // Update
                    node_.setValues(JcrConstants.JCR_CONTENT, content);
                    return node_;
                }
            }
        }

        return node_;
    }
     **/


}
