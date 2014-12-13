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

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.sasl.AuthenticationException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
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

    @RequestMapping(value = "/", method = RequestMethod.POST)
    public ResponseEntity<List<Map>> listDirectoryItems(
            HttpServletRequest request, HttpServletResponse response,
            @RequestParam(value = "path", required = false, defaultValue = "/") String path)
            throws RepositoryException
    {
        Session session = null;
        try{
            session = getSession(request, response);
            Node root = session.getRootNode();
            Node contentRoot = getContentRoot(session);
            path = path.replace("/~/", "/");
            if (path != null && path.length()>1) {
                if( path.startsWith("/"))
                {
                    path = path.substring(1);
                }
                contentRoot = contentRoot.getNode(path);
            }



            Iterable<Node> _childNodes = JcrUtils.getChildNodes(contentRoot);
            List<Map> childNodes = new ArrayList<>();
            for (Node node : _childNodes )
            {
                if ( node.getPrimaryNodeType().isNodeType(JcrConstants.NT_FOLDER)
                        || node.getPrimaryNodeType().isNodeType(JcrConstants.NT_FILE)) {
                    Map _node = new HashMap();
                    _node.put("name", node.getName());
                    _node.put("path", node.getPath().replace("/dam/", "/~/"));
                    _node.put("parent", node.getParent().getPath().replace("/dam/", "/~/"));
                    _node.put("children", new ArrayList());


                    if ( node.getPrimaryNodeType().isNodeType(JcrConstants.NT_FOLDER) ) {
                        _node.put("type", "folder");
                    } else if ( node.isNodeType(JcrConstants.NT_FILE) ) {
                        _node.put("type", "file");
                    }

                    _node.put("isReadOnly", false);
                    childNodes.add(_node);
                }
            }


            return new ResponseEntity<>(childNodes, HttpStatus.OK);

        }
        catch (AuthenticationException ae) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }finally {
            if( session != null ) session.logout();
        }
    }


    @RequestMapping(value = "/tree", method = RequestMethod.GET)
    public ResponseEntity<List<Map>> listDirectoryTree(
            HttpServletRequest request, HttpServletResponse response,
            @RequestParam(value = "root", required = false, defaultValue = "/") String path)
            throws RepositoryException
    {
        Session session = null;
        try{
            session = getSession(request, response);
            Node root = session.getRootNode();
            Node contentRoot = getContentRoot(session);
            if (path != null && path.length()>1) {
                if( path.startsWith("/"))
                {
                    path = path.substring(1);
                }
                contentRoot = contentRoot.getNode(path);
            }

            List<Map> nodes = walkTree(contentRoot);
            return new ResponseEntity<>(nodes, HttpStatus.OK);

        }
        catch (AuthenticationException ae) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        catch (Exception ae) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }finally {
            if( session != null ) session.logout();
        }
    }


    private List<Map> walkTree(Node root) throws RepositoryException
    {
        Iterable<Node> _childNodes = JcrUtils.getChildNodes(root);
        List<Map> childNodes = new ArrayList<>();
        for ( Node node : _childNodes ) {
             if ( node.getPrimaryNodeType().isNodeType(JcrConstants.NT_FOLDER) ) {
                Map _node = new HashMap();
                _node.put("type", "folder");
                _node.put("name", node.getName());
                _node.put("path", node.getPath().replace("/dam/", "/~/"));
                _node.put("parent", node.getParent().getPath().replace("/dam/", "/~/"));
                _node.put("children", walkTree(node));
                _node.put("isReadOnly", false);
                childNodes.add(_node);
            }
        }
        return childNodes;
    }
}
