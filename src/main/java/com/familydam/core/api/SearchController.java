/*
 * Copyright (c) 2015  Mike Nimer & 11:58 Labs
 */

package com.familydam.core.api;

import com.familydam.core.helpers.NodeMapper;
import com.familydam.core.models.INode;
import com.familydam.core.services.AuthenticatedHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.PostConstruct;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Search for content by jcr node type or mixin
 * <p>
 * Created by mnimer on 12/13/14.
 */
@Controller
@RequestMapping("/api/search")
public class SearchController
{
    @Autowired
    private ApplicationContext applicationContext;

    private AuthenticatedHelper authenticatedHelper = null;


    @PostConstruct
    private void setup()
    {
        authenticatedHelper = applicationContext.getBean(AuthenticatedHelper.class);
    }


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
    @RequestMapping(value = "/{type}", method = RequestMethod.POST)
    public ResponseEntity<Collection<INode>> searchByType(HttpServletRequest request,
                                                          HttpServletResponse response,
                                                          @AuthenticationPrincipal Authentication currentUser_,
                                                          @RequestBody() String jsonBody,
                                                          @PathVariable(value = "type") String type,
                                                          @RequestParam(value = "limit", required = false, defaultValue = "100") Integer limit,
                                                          @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset)
    {

        Session session = null;
        try {
            session = authenticatedHelper.getSession(currentUser_);

            String _orderBy = "jcr:lastModified";
            String _orderByDirection = "asc";
            ObjectMapper objectMapper = new ObjectMapper();
            Map _filters = objectMapper.readValue(jsonBody, Map.class);

            if (_filters.containsKey("order")) {
                _orderBy = (String) ((Map) _filters.get("order")).get("field");
                _orderByDirection = (String) ((Map) _filters.get("order")).get("direction");
            }


            StringBuffer sql = new StringBuffer("SELECT * FROM [").append(type).append("] ");

            sql.append(" WHERE [jcr:primaryType] = 'nt:file'");

            boolean hasTags = false;
            boolean hasPeople = false;
            StringBuilder tagClause = new StringBuilder();
            StringBuilder peopleClause = new StringBuilder();


            if (_filters.containsKey("tags")) {
                List<Map> _tags = (List) _filters.get("tags");
                if (_tags.size() > 0) {
                    hasTags = true;
                    for (int i = 0; i < _tags.size(); i++) {
                        Map tag = _tags.get(i);
                        tagClause.append(" lower([dam:tags]) = '" + tag.get("name").toString().toLowerCase() +"'");
                        if( i >= 0 && i < (_tags.size()-1) ){
                            tagClause.append(" OR ");
                        }
                    }
                }
            }
            if (_filters.containsKey("people")) {
                List<Map> _people = (List) _filters.get("people");
                if (_people.size() > 0) {
                    hasPeople = true;
                    for (int i = 0; i < _people.size(); i++) {
                        Map people = _people.get(i);
                        peopleClause.append(" lower([dam:people]) = '" + people.get("name").toString().toLowerCase() +"'");
                        if( i >= 0 && i < (_people.size()-1) ){
                            peopleClause.append(" OR ");
                        }
                    }
                }
            }


            if( hasTags || hasPeople ){
                sql.append(" AND (");
                if( hasTags ){
                    sql.append(tagClause.toString());
                }
                if( hasTags && hasPeople ){
                    sql.append(" OR ");
                }
                if( hasPeople ){
                    sql.append(peopleClause.toString());
                }
                sql.append(" ) ");
            }


            sql.append(" ORDER BY [").append(_orderBy).append("] DESC");
            if (limit > 0) { // 0 == return all
                //sql.append(" LIMIT ").append(limit);
                //sql.append(" OFFSET ").append(offset);
            }


            QueryManager queryManager = session.getWorkspace().getQueryManager();
            Query query = queryManager.createQuery(sql.toString(), "JCR-SQL2");
            //Query query = queryManager.createQuery(sql.toString(), "sql");

            // Execute the query and get the results ...
            QueryResult result = query.execute();


            // Iterate over the nodes in the results ...
            Collection<INode> _nodes = new ArrayList<>();
            javax.jcr.NodeIterator nodeItr = result.getNodes();
            while (nodeItr.hasNext()) {
                javax.jcr.Node node = nodeItr.nextNode();
                _nodes.add(NodeMapper.map(node));
            }

            return new ResponseEntity<>(_nodes, HttpStatus.OK);

        }
        catch (Exception ae) {
            ae.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        finally {
            if (session != null) {
                session.logout();
            }
        }
    }

}

