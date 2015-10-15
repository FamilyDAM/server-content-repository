/*
 * Copyright (c) 2015  Mike Nimer & 11:58 Labs
 */

package com.familydam.core.dao.photosModule;

import com.familydam.core.exceptions.UnknownINodeException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mnimer on 10/14/15.
 */
@Component
public class TreeDao
{
    private Log log = LogFactory.getLog(this.getClass());
    SimpleDateFormat dfYear = new SimpleDateFormat("yyyy");
    SimpleDateFormat dfMonth = new SimpleDateFormat("MMMM");
    SimpleDateFormat dfMonth2 = new SimpleDateFormat("MM");
    SimpleDateFormat dfDay = new SimpleDateFormat("dd");


    /**
     * Create a tree of YEAR -> MONTH -> DATE
     * @param session
     * @return
     * @throws RepositoryException
     */
    public Map dateTree(Session session) throws RepositoryException, UnknownINodeException
    {
        StringBuffer sql = new StringBuffer("SELECT * FROM [dam:image]");


        QueryManager queryManager = session.getWorkspace().getQueryManager();
        //Query query = queryManager.createQuery(sql, "JCR-SQL2");
        Query query = queryManager.createQuery(sql.toString(), Query.JCR_SQL2);

        // Execute the query and get the results ...
        QueryResult result = query.execute();


        // Iterate over the nodes in the results ...
        Map<String, Map> _nodeMap = new HashMap<>();
        javax.jcr.NodeIterator nodeItr = result.getNodes();
        while ( nodeItr.hasNext() ) {
            javax.jcr.Node node = nodeItr.nextNode();

            Calendar jcrDate = node.getProperty("jcr:created").getDate();

            Date date = jcrDate.getTime();
            try {
                Node dateNode = node.getNode("dam:metadata/IPTC/Date_Created");
                if (dateNode != null) {
                    date = DateFormat.getDateInstance().parse(dateNode.getProperty("description").getString());
                }
            }
            catch (ParseException|PathNotFoundException pe) {
                //swallow and use jcrDate instead
            }



            String year = dfYear.format(date);
            String month = dfMonth.format(date);
            String day = dfDay.format(date);

            Map yearMap = _nodeMap.get(year);
            if( yearMap == null){
                yearMap = new HashMap();
                yearMap.put("label", year);
                yearMap.put("year", year);
                yearMap.put("children", new HashMap());
                _nodeMap.put(year, yearMap);
            }


            Map monthMap = (Map)((Map)_nodeMap.get(year).get("children")).get(month);
            if( monthMap == null){
                monthMap = new HashMap();
                monthMap.put("label", month);
                monthMap.put("year", year);
                monthMap.put("month", dfMonth2.format(date));
                monthMap.put("children", new HashMap());

                ((Map)_nodeMap.get(year).get("children")).put(month, monthMap);

            }

            Map dayMap = (Map)((Map)((Map)((Map)_nodeMap.get(year).get("children")).get(month)).get("children")).get(day);
            if( dayMap == null){
                dayMap = new HashMap();
                dayMap.put("label", day);
                dayMap.put("year", year);
                dayMap.put("month", dfMonth2.format(date));
                dayMap.put("day", day);
                dayMap.put("children", new HashMap());

                ((Map)((Map)((Map)_nodeMap.get(year).get("children")).get(month)).get("children")).put(day, dayMap);
            }

            log.debug(node);
        }

        return _nodeMap;
    }
}
