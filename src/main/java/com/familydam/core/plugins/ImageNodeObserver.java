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

package com.familydam.core.plugins;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.oak.plugins.observation.NodeObserver;
import org.apache.jackrabbit.oak.spi.commit.CommitInfo;

import java.util.Map;
import java.util.Set;

/**
 * Created by mnimer on 9/16/14.
 */
public class ImageNodeObserver extends NodeObserver
{

    public ImageNodeObserver()
    {
        super("/dam", "jcr:content");
    }

    public ImageNodeObserver(String path, String... propertyNames)
    {
        super(path, propertyNames);
    }


    @Override
    protected void added(String path, Set<String> added, Set<String> deleted, Set<String> changed, Map<String, String> properties, CommitInfo commitInfo)
    {
        System.out.println("{ImageNodeObserver} added");

        if( added.contains(JcrConstants.JCR_CONTENT) )
        {
            System.out.println("File Added: " +path +" | mime type=" );
        }
    }


    @Override
    protected void deleted(String path, Set<String> added, Set<String> deleted, Set<String> changed, Map<String, String> properties, CommitInfo commitInfo)
    {
        System.out.println("{ImageNodeObserver} deleted");
    }


    @Override
    protected void changed(String path, Set<String> added, Set<String> deleted, Set<String> changed, Map<String, String> properties, CommitInfo commitInfo)
    {
        System.out.println("{ImageNodeObserver} changed");
    }
}
