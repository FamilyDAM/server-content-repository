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
import org.apache.jackrabbit.oak.plugins.nodetype.write.InitialContent;
import org.apache.jackrabbit.oak.spi.state.NodeBuilder;
import org.apache.jackrabbit.oak.spi.state.NodeStore;

/**
 * Created by mnimer on 9/17/14.
 */
public class InitialDAMContent extends InitialContent
{
    NodeStore store;


    public InitialDAMContent(NodeStore store)
    {
        this.store = store;
    }


    @Override public void initialize(NodeBuilder builder)
    {

        NodeBuilder damNode;

        if (!builder.hasChildNode("dam")) {
            damNode = builder.child("dam");
            damNode.setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FOLDER);
            damNode.setProperty(JcrConstants.JCR_NAME, "dam");
            damNode.setProperty(JCR_CREATEDBY, "system");
        }else{
            damNode = builder.child("dam");
        }


        if( !damNode.hasChildNode("documents") ){
            NodeBuilder documents = damNode.child("documents");
            documents.setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FOLDER);
            documents.setProperty(JcrConstants.JCR_NAME, "documents");
            documents.setProperty(JcrConstants.JCR_CREATED, "system");
        }
        if( !damNode.hasChildNode("photos") ){
            NodeBuilder photos = damNode.child("photos");
            photos.setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FOLDER);
            photos.setProperty(JcrConstants.JCR_NAME, "photos");
            photos.setProperty(JCR_CREATEDBY, "system");
        }

        // add default admin user.
        //   /rep:security/rep:authorizables/rep:users/a/ad/admin

        super.initialize(builder);


        //InputStream is = this.getClass().getClassLoader().getResourceAsStream("familydam_nodetypes.cnd");

/**
        NodeState base = builder.getNodeState();
        NodeTypeRegistry.register(new SystemRoot(
                store, new EditorHook(new CompositeEditorProvider(
                new NamespaceEditorProvider(),
                new TypeEditorProvider())))
                , is, "familydam node types");
        NodeState target = store.getRoot();
        target.compareAgainstBaseState(base, new ApplyDiff(builder));
**/
    }
}
