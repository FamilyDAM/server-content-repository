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

import org.apache.jackrabbit.oak.plugins.nodetype.write.InitialContent;
import org.apache.jackrabbit.oak.spi.state.NodeBuilder;

/**
 * Created by mnimer on 9/17/14.
 */
public class InitialDAMContent extends InitialContent
{

    @Override public void initialize(NodeBuilder builder)
    {
        NodeBuilder damNode;


        if (!builder.hasChildNode("dam")) {
            damNode = builder.child("dam");
            damNode.setProperty(JCR_PRIMARYTYPE, NT_HIERARCHYNODE);
            damNode.setProperty(JCR_NAME, "dam");
            damNode.setProperty(JCR_CREATEDBY, "system");
        }else{
            damNode = builder.child("dam");
        }


        if( !damNode.hasChildNode("documents") ){
            NodeBuilder documents = damNode.child("documents");
            documents.setProperty(JCR_PRIMARYTYPE, NT_FOLDER);
            documents.setProperty(JCR_NAME, "documents");
            documents.setProperty(JCR_CREATEDBY, "system");
        }
        if( !damNode.hasChildNode("photos") ){
            NodeBuilder photos = damNode.child("photos");
            photos.setProperty(JCR_PRIMARYTYPE, NT_FOLDER);
            photos.setProperty(JCR_NAME, "photos");
            photos.setProperty(JCR_CREATEDBY, "system");
        }

        // add default admin user.
        //   /rep:security/rep:authorizables/rep:users/a/ad/admin


        super.initialize(builder);
    }
}
