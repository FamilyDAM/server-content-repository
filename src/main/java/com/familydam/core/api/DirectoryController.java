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
import org.apache.jackrabbit.oak.api.ContentSession;
import org.apache.jackrabbit.oak.api.Root;
import org.apache.jackrabbit.oak.api.Tree;
import org.apache.jackrabbit.oak.api.Type;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.jcr.NoSuchWorkspaceException;
import javax.security.auth.login.LoginException;
import javax.security.sasl.AuthenticationException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
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
            @RequestParam(value = "path", required = false, defaultValue = "/") String path )
            throws NoSuchWorkspaceException, IOException
    {
        try (ContentSession session = getSession(request)) {
            Root root = session.getLatestRoot();
            Tree tree = getContentRoot(session);
            if( path != null ) {
                tree = getRelativeTree(tree, path);
            }


            List<Map> childNodes = new ArrayList<>();
            for (Tree node : tree.getChildren()) {
                if( node.getProperty(JcrConstants.JCR_PRIMARYTYPE).getValue(Type.STRING).equals(JcrConstants.NT_FOLDER)
                        || node.getProperty(JcrConstants.JCR_PRIMARYTYPE).getValue(Type.STRING).equals(JcrConstants.NT_HIERARCHYNODE)
                        || node.getProperty(JcrConstants.JCR_PRIMARYTYPE).getValue(Type.STRING).equals(JcrConstants.NT_FILE)) {
                    Map _node = new HashMap();
                    _node.put("name", node.getName());
                    _node.put("path", node.getPath().replace("/dam/", "/~/")  );
                    _node.put("parent", node.getParent().getPath().replace("/dam/", "/~/")  );
                    _node.put("children", new ArrayList() );

                    if( node.getProperty(JcrConstants.JCR_PRIMARYTYPE).getValue(Type.STRING).equals(JcrConstants.NT_FOLDER)
                            || node.getProperty(JcrConstants.JCR_PRIMARYTYPE).getValue(Type.STRING).equals(JcrConstants.NT_HIERARCHYNODE)) {
                        _node.put("type", "folder");
                    }else if( node.getProperty(JcrConstants.JCR_PRIMARYTYPE).getValue(Type.STRING).equals(JcrConstants.NT_FILE)) {
                        _node.put("type", "file");
                    }
                    childNodes.add(_node);
                }
            }


            return new ResponseEntity<>(childNodes, HttpStatus.OK);

        }catch(AuthenticationException|LoginException ae){
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/tree", method = RequestMethod.GET)
    public ResponseEntity<List<Map>> listDirectoryTree(
            HttpServletRequest request, HttpServletResponse response,
            @RequestParam(value = "root", required = false, defaultValue = "/") String rootDir )
            throws NoSuchWorkspaceException, IOException
    {
        try (ContentSession session = getSession(request)) {
            Root root = session.getLatestRoot();
            Tree tree = getContentRoot(session);
            if( rootDir != null ) {
                tree = getRelativeTree(tree, rootDir);
            }

            List<Map> nodes = walkTree(tree);
            return new ResponseEntity<>(nodes, HttpStatus.OK);

        }catch(AuthenticationException|LoginException ae){
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }


    private List<Map> walkTree(Tree tree)
    {
        List<Map> childNodes = new ArrayList<>();
        for (Tree node : tree.getChildren()) {
            if( node.getProperty(JcrConstants.JCR_PRIMARYTYPE).getValue(Type.STRING).equals(JcrConstants.NT_FOLDER)
                    || node.getProperty(JcrConstants.JCR_PRIMARYTYPE).getValue(Type.STRING).equals(JcrConstants.NT_HIERARCHYNODE) ) {
                Map _node = new HashMap();
                _node.put("name", node.getName());
                _node.put("path", node.getPath().replace("/dam/","/~/")  );
                _node.put("parent", node.getParent().getPath().replace("/dam/","/~/")  );
                _node.put("children", walkTree(node) );
                childNodes.add(_node);
            }
        }
        return childNodes;
    }
}
