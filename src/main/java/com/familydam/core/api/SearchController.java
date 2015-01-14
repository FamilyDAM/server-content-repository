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

import com.familydam.core.FamilyDAMConstants;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mnimer on 12/13/14.
 */
@Controller
@RequestMapping("/api/search")
public class SearchController extends AuthenticatedService
{


    @RequestMapping(value = "/files", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Collection> searchFiles(HttpServletRequest request,
                                                   HttpServletResponse response,
                                                   @RequestParam(value = "type", required = false, defaultValue = "nt:file") String type,
                                                   @RequestParam(value = "orderBy", required = false, defaultValue = "jcr:lastModified") String orderBy,
                                                   @RequestParam(value = "limit", required = false, defaultValue = "100") Integer limit,
                                                   @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset)
    {
        return searchByType(request, response, "dam:file", orderBy, limit, offset);
    }

    @RequestMapping(value = "/images", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Collection> searchPhotos(HttpServletRequest request,
                                                   HttpServletResponse response,
                                                   @RequestParam(value = "type", required = false, defaultValue = "nt:file") String type,
                                                   @RequestParam(value = "orderBy", required = false, defaultValue = "jcr:lastModified") String orderBy,
                                                   @RequestParam(value = "limit", required = false, defaultValue = "100") Integer limit,
                                                   @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset)
    {
        return searchByType(request, response, "dam:image", orderBy, limit, offset);
    }


    @RequestMapping(value = "/movies", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Collection> searchMovies(HttpServletRequest request,
                                                   HttpServletResponse response,
                                                   @RequestParam(value = "type", required = false, defaultValue = "nt:file") String type,
                                                   @RequestParam(value = "orderBy", required = false, defaultValue = "jcr:lastModified") String orderBy,
                                                   @RequestParam(value = "limit", required = false, defaultValue = "100") Integer limit,
                                                   @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset)
    {
        return searchByType(request, response, "dam:movie", orderBy, limit, offset);
    }


    @RequestMapping(value = "/songs", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Collection> searchSongs(HttpServletRequest request,
                                                   HttpServletResponse response,
                                                   @RequestParam(value = "type", required = false, defaultValue = "nt:file") String type,
                                                   @RequestParam(value = "orderBy", required = false, defaultValue = "jcr:lastModified") String orderBy,
                                                   @RequestParam(value = "limit", required = false, defaultValue = "100") Integer limit,
                                                   @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset)
    {
        return searchByType(request, response, "dam:song", orderBy, limit, offset);
    }



    @RequestMapping(value = "/", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Collection> searchByType(HttpServletRequest request,
                                   HttpServletResponse response,
                                   @RequestParam(value = "type", required = false, defaultValue = "nt:file") String type,
                                   @RequestParam(value = "orderBy", required = false, defaultValue = "jcr:lastModified") String orderBy,
                                   @RequestParam(value = "limit", required = false, defaultValue = "100") Integer limit,
                                   @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset)
    {
        String sql = "SELECT * FROM [" +type +"]" +
                " ORDER BY [" +orderBy +"] DESC";
        if( limit > 0) { // 0 == return all
            //sql += " LIMIT " + limit + " OFFSET " + offset;
        }

        Session session = null;
        try {
            session = getSession(request, response);

            QueryManager queryManager = session.getWorkspace().getQueryManager();
            //Query query = queryManager.createQuery(sql, "JCR-SQL2");
            Query query = queryManager.createQuery(sql, "sql");

            // Execute the query and get the results ...
            QueryResult result = query.execute();


            // Iterate over the nodes in the results ...
            Collection<Map> nodes = new ArrayList<>();
            javax.jcr.NodeIterator nodeItr = result.getNodes();
            while ( nodeItr.hasNext() ) {
                javax.jcr.Node node = nodeItr.nextNode();

                Map nodeMap = new HashMap<>();
                nodeMap.put("id", node.getIdentifier());
                nodeMap.put("name", node.getName());
                nodeMap.put("path", node.getPath().replace("/" + FamilyDAMConstants.DAM_ROOT + "/", "/~/")  );
                nodeMap.put("parent", node.getParent().getPath().replace("/" + FamilyDAMConstants.DAM_ROOT + "/", "/~/")  );
                nodeMap.put("isReadOnly", false);
                nodeMap.put("mixins", org.apache.commons.lang3.StringUtils.join(node.getMixinNodeTypes(), ','));
                if( node.getPrimaryNodeType().isNodeType("nt:file") ) {
                    nodeMap.put("type", "file");
                }else if( node.getPrimaryNodeType().isNodeType("nt:folder") ) {
                    nodeMap.put("type", "folder");
                }else{
                    nodeMap.put("type", "unknown");
                }
                nodes.add(nodeMap);
            }

            return new ResponseEntity<Collection>(nodes, HttpStatus.OK);

        }
        catch (Exception ae) {
            ae.printStackTrace();
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        finally {
            if (session != null) {
                session.logout();
            }
        }
    }

}

