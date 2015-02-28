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
import org.apache.jackrabbit.oak.api.Type;
import org.apache.jackrabbit.oak.plugins.nodetype.write.InitialContent;
import org.apache.jackrabbit.oak.spi.state.NodeBuilder;
import org.apache.jackrabbit.oak.spi.state.NodeStore;

import java.util.HashSet;
import java.util.Set;

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
        Set<String> _systemMixins = new HashSet<>();
        _systemMixins.add("mix:created");
        _systemMixins.add("dam:systemfolder");

        Set<String> _contentMixins = new HashSet<>();
        _systemMixins.add("mix:created");
        _contentMixins.add("dam:contentfolder");

        NodeBuilder contentNode;

        if (!builder.hasChildNode("dam:content")) {
            contentNode = builder.child("dam:content");
            contentNode.setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FOLDER);
            contentNode.setProperty(JcrConstants.JCR_NAME, "dam");
            contentNode.setProperty(JcrConstants.JCR_MIXINTYPES, _systemMixins, Type.STRINGS);
            contentNode.setProperty(JCR_CREATEDBY, "system");
        }else{
            contentNode = builder.child("dam:content");
        }
        

        if( !contentNode.hasChildNode("documents") ){
            NodeBuilder documents = contentNode.child("documents");
            documents.setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FOLDER);
            documents.setProperty(JcrConstants.JCR_MIXINTYPES, _contentMixins, Type.STRINGS);
            documents.setProperty(JcrConstants.JCR_NAME, "Documents");
            documents.setProperty(JcrConstants.JCR_CREATED, "system");
            documents.setProperty("order", "1");
        }


        /*************
         * Future USE 
         */
        if( !contentNode.hasChildNode("cloud") ){
            NodeBuilder cloud = contentNode.child("cloud");
            cloud.setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FOLDER);
            cloud.setProperty(JcrConstants.JCR_MIXINTYPES, _systemMixins, Type.STRINGS);
            cloud.setProperty(JcrConstants.JCR_NAME, "Cloud");
            cloud.setProperty(JcrConstants.JCR_CREATED, "system");
            cloud.setProperty("order", "2");

                NodeBuilder dropbox = cloud.child("dropbox");
                dropbox.setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FOLDER);
                dropbox.setProperty(JcrConstants.JCR_MIXINTYPES, _contentMixins, Type.STRINGS);
                dropbox.setProperty(JcrConstants.JCR_NAME, "Dropbox");
                dropbox.setProperty(JcrConstants.JCR_CREATED, "system");

                    // todo: remove, this should not be hard coded
                    NodeBuilder mnimer = dropbox.child("mike");
                    mnimer.setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FOLDER);
                    mnimer.setProperty(JcrConstants.JCR_MIXINTYPES, _systemMixins, Type.STRINGS);
                    mnimer.setProperty(JcrConstants.JCR_NAME, "mike");
                    mnimer.setProperty(JcrConstants.JCR_CREATED, "system");
                    NodeBuilder animer = dropbox.child("angela");
                    animer.setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FOLDER);
                    animer.setProperty(JcrConstants.JCR_MIXINTYPES, _systemMixins, Type.STRINGS);
                    animer.setProperty(JcrConstants.JCR_NAME, "angela");
                    animer.setProperty(JcrConstants.JCR_CREATED, "system");

                NodeBuilder gdrive = cloud.child("google_drive");
                gdrive.setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FOLDER);
                gdrive.setProperty(JcrConstants.JCR_MIXINTYPES, _contentMixins, Type.STRINGS);
                gdrive.setProperty(JcrConstants.JCR_NAME, "Google Drive");
                gdrive.setProperty(JcrConstants.JCR_CREATED, "system");

                    // todo: remove, this should not be hard coded
                    NodeBuilder mnimer2 = gdrive.child("mike");
                    mnimer2.setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FOLDER);
                    mnimer2.setProperty(JcrConstants.JCR_MIXINTYPES, _systemMixins, Type.STRINGS);
                    mnimer2.setProperty(JcrConstants.JCR_NAME, "mike");
                    mnimer2.setProperty(JcrConstants.JCR_CREATED, "system");
                    NodeBuilder animer3 = gdrive.child("angela");
                    animer3.setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FOLDER);
                    animer3.setProperty(JcrConstants.JCR_MIXINTYPES, _systemMixins, Type.STRINGS);
                    animer3.setProperty(JcrConstants.JCR_NAME, "angela");
                    animer3.setProperty(JcrConstants.JCR_CREATED, "system");
            
        }
        if( !contentNode.hasChildNode("email") ){
            NodeBuilder email = contentNode.child("email_archive");
            email.setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FOLDER);
            email.setProperty(JcrConstants.JCR_MIXINTYPES, _contentMixins, Type.STRINGS);
            email.setProperty(JcrConstants.JCR_NAME, "Email Archive");
            email.setProperty(JcrConstants.JCR_CREATED, "system");
            email.setProperty("order", "4");


                // todo: remove, this should not be hard coded
                NodeBuilder mnimer = email.child("mike");
                mnimer.setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FOLDER);
                mnimer.setProperty(JcrConstants.JCR_MIXINTYPES, _systemMixins, Type.STRINGS );
                mnimer.setProperty(JcrConstants.JCR_NAME, "mike");
                mnimer.setProperty(JcrConstants.JCR_CREATED, "system");
                NodeBuilder animer = email.child("angela");
                animer.setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FOLDER);
                animer.setProperty(JcrConstants.JCR_MIXINTYPES, _systemMixins, Type.STRINGS );
                animer.setProperty(JcrConstants.JCR_NAME, "angela");
                animer.setProperty(JcrConstants.JCR_CREATED, "system");
        }
        if( !contentNode.hasChildNode("web") ){
            NodeBuilder web = contentNode.child("web_archive");
            web.setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FOLDER);
            web.setProperty(JcrConstants.JCR_MIXINTYPES, _systemMixins, Type.STRINGS );
            web.setProperty(JcrConstants.JCR_NAME, "Web Archive");
            web.setProperty(JcrConstants.JCR_CREATED, "system");
            web.setProperty("order", "3");


                NodeBuilder fb = web.child("facebook");
                fb.setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FOLDER);
                fb.setProperty(JcrConstants.JCR_MIXINTYPES, _contentMixins, Type.STRINGS );
                fb.setProperty(JcrConstants.JCR_NAME, "Facebook");
                fb.setProperty(JcrConstants.JCR_CREATED, "system");


                    // todo: remove, this should not be hard coded
                    NodeBuilder mnimer1 = fb.child("mike");
                    mnimer1.setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FOLDER);
                    mnimer1.setProperty(JcrConstants.JCR_MIXINTYPES, _systemMixins, Type.STRINGS);
                    mnimer1.setProperty(JcrConstants.JCR_NAME, "mike");
                    mnimer1.setProperty(JcrConstants.JCR_CREATED, "system");
                    NodeBuilder animer1 = fb.child("angela");
                    animer1.setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FOLDER);
                    animer1.setProperty(JcrConstants.JCR_MIXINTYPES, _systemMixins, Type.STRINGS);
                    animer1.setProperty(JcrConstants.JCR_NAME, "angela");
                    animer1.setProperty(JcrConstants.JCR_CREATED, "system");

            
                NodeBuilder instagram = web.child("instagram");
                instagram.setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FOLDER);
                instagram.setProperty(JcrConstants.JCR_MIXINTYPES, _contentMixins, Type.STRINGS );
                instagram.setProperty(JcrConstants.JCR_NAME, "Instagram");
                instagram.setProperty(JcrConstants.JCR_CREATED, "system");


                    // todo: remove, this should not be hard coded
                    NodeBuilder mnimer2 = instagram.child("mike");
                    mnimer2.setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FOLDER);
                    mnimer2.setProperty(JcrConstants.JCR_MIXINTYPES,  _systemMixins, Type.STRINGS );
                    mnimer2.setProperty(JcrConstants.JCR_NAME, "mike");
                    mnimer2.setProperty(JcrConstants.JCR_CREATED, "system");
                    NodeBuilder animer2 = instagram.child("angela");
                    animer2.setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FOLDER);
                    animer2.setProperty(JcrConstants.JCR_MIXINTYPES,  _systemMixins, Type.STRINGS );
                    animer2.setProperty(JcrConstants.JCR_NAME, "angela");
                    animer2.setProperty(JcrConstants.JCR_CREATED, "system");
            
            
                NodeBuilder twitter = web.child("twitter");
                twitter.setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FOLDER);
                twitter.setProperty(JcrConstants.JCR_MIXINTYPES,  _contentMixins, Type.STRINGS );
                twitter.setProperty(JcrConstants.JCR_NAME, "Twitter");
                twitter.setProperty(JcrConstants.JCR_CREATED, "system");


                    // todo: remove, this should not be hard coded
                    NodeBuilder mnimer3 = twitter.child("mike");
                    mnimer3.setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FOLDER);
                    mnimer3.setProperty(JcrConstants.JCR_MIXINTYPES, _systemMixins, Type.STRINGS );
                    mnimer3.setProperty(JcrConstants.JCR_NAME, "mike");
                    mnimer3.setProperty(JcrConstants.JCR_CREATED, "system");
                    NodeBuilder animer4 = twitter.child("angela");
                    animer4.setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FOLDER);
                    animer4.setProperty(JcrConstants.JCR_MIXINTYPES, _systemMixins, Type.STRINGS );
                    animer4.setProperty(JcrConstants.JCR_NAME, "angela");
                    animer4.setProperty(JcrConstants.JCR_CREATED, "system");
        }
        


        // todo add default admin user.
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
