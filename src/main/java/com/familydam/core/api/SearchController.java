/*
 * Copyright (c) 2015  Mike Nimer & 11:58 Labs
 */

package com.familydam.core.api;

import com.familydam.core.helpers.NodeMapper;
import com.familydam.core.models.INode;
import com.familydam.core.services.AuthenticatedHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
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

/**
 * Search for content by jcr node type or mixin
 *
 * Created by mnimer on 12/13/14.
 */
@Controller
@RequestMapping("/api/search")
public class SearchController
{
    @Autowired
    private AuthenticatedHelper authenticatedHelper;


    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ResponseEntity<Collection<INode>> searchByKeyword(HttpServletRequest request,
                                   HttpServletResponse response,
                                   @AuthenticationPrincipal Authentication currentUser_,
                                   @PathVariable(value = "type") String type,
                                   @RequestParam(value = "keywords", required = true) String keywords,
                                   @RequestParam(value = "orderBy", required = false, defaultValue = "jcr:lastModified") String orderBy,
                                   @RequestParam(value = "limit", required = false, defaultValue = "100") Integer limit,
                                   @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset)
    {
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }



    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value = "/{type}", method = RequestMethod.GET)
    public ResponseEntity<Collection<INode>> searchByType(HttpServletRequest request,
                                   HttpServletResponse response,
                                   @AuthenticationPrincipal Authentication currentUser_,
                                   @PathVariable(value = "type") String type,
                                   @RequestParam(value = "orderBy", required = false, defaultValue = "jcr:lastModified") String orderBy,
                                   @RequestParam(value = "limit", required = false, defaultValue = "100") Integer limit,
                                   @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset)
    {
        
        Session session = null;
        try {
            session = authenticatedHelper.getSession(currentUser_);


            StringBuffer sql = new StringBuffer("SELECT * FROM [").append(type).append("] ");
            sql.append(" ORDER BY [").append(orderBy).append("] DESC");
            if( limit > 0) { // 0 == return all
                //sql.append(" LIMIT ").append(limit);
                //sql.append(" OFFSET ").append(offset);
            }


            QueryManager queryManager = session.getWorkspace().getQueryManager();
            //Query query = queryManager.createQuery(sql, "JCR-SQL2");
            Query query = queryManager.createQuery(sql.toString(), "sql");

            // Execute the query and get the results ...
            QueryResult result = query.execute();


            // Iterate over the nodes in the results ...
            Collection<INode> _nodes = new ArrayList<>();
            javax.jcr.NodeIterator nodeItr = result.getNodes();
            while ( nodeItr.hasNext() ) {
                javax.jcr.Node node = nodeItr.nextNode();
                _nodes.add(NodeMapper.map(node));
            }

            return new ResponseEntity<>(_nodes, HttpStatus.OK);

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

