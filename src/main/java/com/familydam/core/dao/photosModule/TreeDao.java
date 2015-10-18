/*
 * Copyright (c) 2015  Mike Nimer & 11:58 Labs
 */

package com.familydam.core.dao.photosModule;

import com.familydam.core.FamilyDAMConstants;
import com.familydam.core.exceptions.UnknownINodeException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import java.text.SimpleDateFormat;
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
        StringBuffer sql = new StringBuffer("SELECT [" + FamilyDAMConstants.DAM_DATECREATED +"]  FROM [dam:image] where [" +FamilyDAMConstants.DAM_DATECREATED +"] is not null");


        QueryManager queryManager = session.getWorkspace().getQueryManager();
        //Query query = queryManager.createQuery(sql, "JCR-SQL2");
        Query query = queryManager.createQuery(sql.toString(), Query.JCR_SQL2);
//
        // Execute the query and get the results ...
        QueryResult result = query.execute();


        // Iterate over the nodes in the results ...
        Map<String, Map> _nodeMap = new HashMap<>();
        RowIterator nodeItr = result.getRows();
        while ( nodeItr.hasNext() ) {
            Row row = nodeItr.nextRow();

            String date = row.getValue(FamilyDAMConstants.DAM_DATECREATED).getString();
            String[] dateParts = date.split("-");
            /**
            try {
                Value dateNode = row.getValue("Date_Created");
                if (dateNode != null) {
                    //date = DateFormat.getDateInstance().parse(dateNode.getProperty("description").getString());
                }
            }
            catch (Exception pe) {
                //swallow and use jcrDate instead
            }
             **/



            String year = dateParts[0];
            String monthName = dateParts[1];
            String monthNumber = dateParts[1];
            String day = dateParts[2];

            Map yearMap = _nodeMap.get(year);
            if( yearMap == null){
                yearMap = new HashMap();
                yearMap.put("label", year);
                yearMap.put("year", year);
                yearMap.put("children", new HashMap());
                _nodeMap.put(year, yearMap);
            }


            Map monthMap = (Map)((Map)_nodeMap.get(year).get("children")).get(monthName);
            if( monthMap == null){

                monthMap = new HashMap();
                monthMap.put("label", monthName);
                monthMap.put("year", year);
                monthMap.put("month", monthNumber);
                monthMap.put("children", new HashMap());

                ((Map)_nodeMap.get(year).get("children")).put(monthNumber, monthMap);

            }

            Map dayMap = (Map)((Map)((Map)((Map)_nodeMap.get(year).get("children")).get(monthNumber)).get("children")).get(day);
            if( dayMap == null){
                dayMap = new HashMap();
                dayMap.put("label", day);
                dayMap.put("year", year);
                dayMap.put("month", date);
                dayMap.put("day", day);
                dayMap.put("children", new HashMap());

                ((Map)((Map)((Map)_nodeMap.get(year).get("children")).get(monthNumber)).get("children")).put(day, dayMap);
            }

            log.debug(row);
        }

        return _nodeMap;
    }
}
