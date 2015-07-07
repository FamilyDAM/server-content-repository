/*
 * Copyright (c) 2015  Mike Nimer & 11:58 Labs
 */

package com.familydam.core.api;

import com.familydam.core.exceptions.UnknownINodeException;
import com.familydam.core.helpers.NodeMapper;
import com.familydam.core.models.Directory;
import com.familydam.core.models.INode;
import com.familydam.core.services.AuthenticatedHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.sasl.AuthenticationException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * methods to work with nt:folder nodes in the jcr
 *
 * Created by mnimer on 10/14/14.
 */
@Controller
@RequestMapping("/api/directory")
public class DirectoryController 
{
    private Log log = LogFactory.getLog(this.getClass());
    
    @Autowired
    private AuthenticatedHelper authenticatedHelper;


    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ResponseEntity<List<INode>> getDirectoryTree(
            HttpServletRequest request, HttpServletResponse response,
            @org.springframework.security.core.annotation.AuthenticationPrincipal Authentication currentUser_,
            @RequestParam(value = "path", required = false, defaultValue = "/dam:files/") String path)
            throws RepositoryException
    {
        Session session = null;
        try {
            session = authenticatedHelper.getSession(currentUser_);
            Node root = session.getRootNode();
            Node contentRoot = session.getNode(path);

            List<INode> nodes = walkDirectoryTree(contentRoot);
            return new ResponseEntity<>(nodes, HttpStatus.OK);

        }
        catch (AuthenticationException ae) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        catch (Exception ae) {
            log.error(ae);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        finally {
            if (session != null) {
                session.logout();
            }
        }
    }



    /**
     * create a new folder
     *
     * @param request
     * @param response
     * @param path
     * @return
     * @throws RepositoryException
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value = "/", method = RequestMethod.POST)
    public ResponseEntity<Collection<Directory>> createNewDirectory(
            HttpServletRequest request, HttpServletResponse response,
            @org.springframework.security.core.annotation.AuthenticationPrincipal Authentication currentUser_,
            @RequestParam(value = "path", required = false, defaultValue = "/dam:files/") String path,
            @RequestParam(value = "name", required = false, defaultValue = "New Folder") String name)
            throws RepositoryException
    {
        Session session = null;
        try {
            session = authenticatedHelper.getSession(currentUser_);
            Node root = session.getRootNode();
            Node contentRoot = session.getNode(path);


            //todo: add validation to make sure we don't the few system properties
            String _nodeName = name.replace(' ', '_').toLowerCase().trim();
            /**
            Node node = contentRoot.addNode(_nodeName, JcrConstants.NT_FOLDER);
            node.addMixin("mix:created");
            node.addMixin("dam:userfolder");
            session.save();

            Node node2 = contentRoot.getNode(_nodeName);
            node2.setProperty(JcrConstants.JCR_NAME, name);
            **/

            Node newNode = JcrUtils.getOrAddNode(contentRoot, _nodeName, JcrConstants.NT_FOLDER);
            //newNode.setProperty(JcrConstants.JCR_NAME, name);
            //Node newNode = contentRoot.addNode(name, JcrConstants.NT_FOLDER);
            //newNode.setProperty(JcrConstants.JCR_CREATED, session.getUserID());
            newNode.addMixin( JcrConstants.MIX_REFERENCEABLE );
            newNode.addMixin("mix:created");
            newNode.addMixin("dam:userfolder");
            newNode.addMixin("dam:extensible");
            session.save();
            //todo assign permissions
            

            return new ResponseEntity<>(HttpStatus.CREATED);
        }
        catch (AuthenticationException ae) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        catch (ItemExistsException ae) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
        catch (Exception ae) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        finally {
            if (session != null) session.logout();
        }
    }


    /**
     * List all files in a directory visible by a user.
     *
     * @param request
     * @param response
     * @param formData
     * @return
     * @throws RepositoryException
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value = "/", method = RequestMethod.DELETE)
    public ResponseEntity deleteDirectory(
            HttpServletRequest request, HttpServletResponse response,
            @org.springframework.security.core.annotation.AuthenticationPrincipal Authentication currentUser_,
            @RequestBody MultiValueMap<String, String> formData)
            throws RepositoryException
    {
        // validate params
        if( !formData.containsKey("path") )
        {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        
        
        Session session = null;
        String path = formData.get("path").get(0);
        try {
            session = authenticatedHelper.getSession(currentUser_);
            Node root = session.getRootNode();
            Node contentRoot = session.getNode(path);

            // todo make sure it's not the system folder / content root
            if( !contentRoot.getName().equalsIgnoreCase("dam:content")) {
                if( contentRoot.isNodeType("dam:contentFolder") ) {
                    contentRoot.remove();
                    session.save();
                }else{
                    return new ResponseEntity<>(HttpStatus.FORBIDDEN);
                }
            }
            
            //todo change this hard delete to a soft delete and move to a trash bin

            return new ResponseEntity<>(HttpStatus.CREATED);
        }
        catch (AuthenticationException ae) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        finally {
            if (session != null) {
                session.logout();
            }
        }
    }



    
    

    /**
     * Recursively walk jcr tree and find directories that the user can see.
     *
     * @param root
     * @return
     * @throws RepositoryException
     */
    private List<INode> walkDirectoryTree(Node root) throws RepositoryException
    {
        Iterable<Node> _childNodes = JcrUtils.getChildNodes(root);
        List<INode> childNodes = new ArrayList<>();
        
        for (Node node : _childNodes) {
            try {
                if (node.getPrimaryNodeType().isNodeType(JcrConstants.NT_FOLDER)) {

                    List<INode> _childTree = walkDirectoryTree(node);

                    INode _node = NodeMapper.map(node);
                    _node.setChildren(_childTree);
                    
                    childNodes.add( _node );
                }
            }catch(PathNotFoundException|UnknownINodeException pnf){
                log.error(pnf);
            }
        }

        Collections.sort(childNodes, new Comparator<INode>()
        {
            public int compare(INode o1, INode o2)
            {
                if( o1.getOrder() < o2.getOrder()){
                    return -1;
                }else if( o1.getOrder() > o2.getOrder()){
                    return 1;
                }else{
                    return ( o1.getName().toString().compareToIgnoreCase( o2.getName().toString() ));
                }
            }
        });
        
        return childNodes;
    }
}
