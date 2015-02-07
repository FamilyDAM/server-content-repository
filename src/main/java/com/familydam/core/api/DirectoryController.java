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

package com.familydam.core.api;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.sasl.AuthenticationException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by mnimer on 10/14/14.
 */
@Controller
@RequestMapping("/api/directory")
public class DirectoryController extends AuthenticatedService
{



    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ResponseEntity<List<Map>> listDirectoryTree(
            HttpServletRequest request, HttpServletResponse response,
            @RequestParam(value = "root", required = false, defaultValue = "/") String path)
            throws RepositoryException
    {
        Session session = null;
        try {
            session = getSession(request, response);
            Node root = session.getRootNode();
            Node contentRoot = getContentRoot(session, path);

            List<Map> nodes = walkDirectoryTree(contentRoot);
            return new ResponseEntity<>(nodes, HttpStatus.OK);

        }
        catch (AuthenticationException ae) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        catch (Exception ae) {
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
    @RequestMapping(value = "/", method = RequestMethod.POST)
    public ResponseEntity<List<Map>> createNewDirectory(
            HttpServletRequest request, HttpServletResponse response,
            @RequestParam(value = "path", required = false, defaultValue = "/") String path,
            @RequestParam(value = "name", required = false, defaultValue = "New Folder") String name)
            throws RepositoryException
    {
        Session session = null;
        try {
            session = getSession(request, response);
            Node root = session.getRootNode();
            Node contentRoot = getContentRoot(session, path);


            //todo: add validation to make sure we don't the few system properties
            Node newNode = contentRoot.addNode(name, JcrConstants.NT_FOLDER);
            newNode.addMixin( JcrConstants.MIX_REFERENCEABLE );
            newNode.addMixin( "dam:contentFolder" );
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
            if (session != null) {
                session.logout();
            }
        }
    }


    /**
     * List all files in a directory visible by a user.
     *
     * @param request
     * @param response
     * @param path
     * @return
     * @throws RepositoryException
     */

    @RequestMapping(value = "/", method = RequestMethod.DELETE)
    public ResponseEntity<List<Map>> deleteDirectory(
            HttpServletRequest request, HttpServletResponse response,
            @RequestBody MultiValueMap<String, String> formData)
            throws RepositoryException
    {
        // validate params
        if( !formData.containsKey("path") )
        {
            return new ResponseEntity<List<Map>>(HttpStatus.BAD_REQUEST);
        }
        
        
        Session session = null;
        String path = formData.get("path").get(0);
        try {
            session = getSession(request, response);
            Node root = session.getRootNode();
            Node contentRoot = getContentRoot(session, path);

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
    private List<Map> walkDirectoryTree(Node root) throws RepositoryException
    {
        Iterable<Node> _childNodes = JcrUtils.getChildNodes(root);
        List<Map> childNodes = new ArrayList<>();
        for (Node node : _childNodes) {
            if (node.getPrimaryNodeType().isNodeType(JcrConstants.NT_FOLDER)) {
                Map _node = new HashMap();
                _node.put("id", node.getIdentifier());
                _node.put("type", "folder");
                _node.put("name", node.getName());
                _node.put("path", node.getPath().replace("/dam:content/", "/~/"));
                _node.put("parent", node.getParent().getPath().replace("/dam:content/", "/~/"));
                _node.put("children", walkDirectoryTree(node));
                _node.put("isReadOnly", false);
                _node.put("mixins", StringUtils.join(node.getMixinNodeTypes(), ","));
                _node.put("fileType", "unknown");


                if( node.isNodeType("dam:image")  ) {
                    _node.put("fileType", "image");
                }
                
                childNodes.add(_node);
            }
        }

        Collections.sort(childNodes, new Comparator<Map>()
        {
            public int compare(Map o1, Map o2)
            {
                return ((Map) o1).get("name").toString().compareToIgnoreCase(((Map) o2).get("name").toString());
            }
        });
        
        return childNodes;
    }
}
