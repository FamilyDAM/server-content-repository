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
import com.familydam.core.helpers.MimeTypeManager;
import com.familydam.core.helpers.PropertyUtil;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.oak.api.ContentSession;
import org.apache.jackrabbit.oak.api.Root;
import org.apache.jackrabbit.oak.api.Tree;
import org.apache.jackrabbit.oak.plugins.nodetype.NodeTypeConstants;
import org.apache.jackrabbit.oak.util.NodeUtil;
import org.apache.jackrabbit.oak.util.TreeUtil;
import org.apache.jackrabbit.value.BinaryValue;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Value;
import javax.security.auth.login.LoginException;
import javax.security.sasl.AuthenticationException;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;

/**
 * A number of useful messages to use
 *
 * Created by mnimer on 10/14/14.
 */
@Controller
@RequestMapping("/api/import")
public class ImportController extends AuthenticatedService
{


    @RequestMapping(value = "/copy")
    public ResponseEntity<Object> copyLocalFile(HttpServletRequest request, @RequestBody Map props) throws LoginException, NoSuchWorkspaceException
    {
        String dir = (String)props.get("dir");
        String path = (String)props.get("path");
        Assert.notNull(dir);
        Assert.notNull(path);

        try (ContentSession session = getSession(request)) {
            File file = new File(path);


            if( !file.exists() )
            {
                return new ResponseEntity<Object>(HttpStatus.NOT_FOUND);
            }
            else if( file.isFile() )
            {
                Root root = session.getLatestRoot();
                Tree rootTree = getContentRoot(session);
                Tree dirTree = getRelativeTree(rootTree, dir);


                NodeUtil nodeDir;
                NodeUtil newNode;
                try {

                    String fileName = file.getName();

                    if( !dirTree.exists() ){
                        nodeDir = new NodeUtil(root.getTree("/")).getOrAddTree(dirTree.getPath(), "nt:folder");
                        root.commit();
                    }else{
                        nodeDir = new NodeUtil(dirTree);
                    }

                    newNode = nodeDir.getOrAddChild(fileName, "nt:file");
                    if( !newNode.getTree().exists() ) {
                        newNode.setString(JcrConstants.JCR_UUID, UUID.randomUUID().toString());
                    }
                    newNode.setString(JcrConstants.JCR_NAME, fileName);
                    newNode.setString(JcrConstants.JCR_CREATED, "todo");//session.getAuthInfo().getUserID());

                    // first use the java lib, to get the mime type
                    String mimeType = Files.probeContentType(file.toPath());
                    if( mimeType != null ) {
                        newNode.setString(JcrConstants.JCR_MIMETYPE, mimeType);
                    }else{
                        //default to our local check (based on file extension)
                        String type = MimeTypeManager.getMimeType(fileName);
                        newNode.setString(JcrConstants.JCR_MIMETYPE, type);
                    }

                    // set file contents
                    Value[] content = new Value[1];
                    InputStream is = new FileInputStream(file);
                    content[0] = new BinaryValue(is);
                    newNode.setValues(JcrConstants.JCR_CONTENT, content);

                    MultiValueMap headers = new HttpHeaders();
                    headers.add("location", newNode.getTree().getPath().replace("/dam/", "/~/"));
                    root.commit();
                    return new ResponseEntity<Object>(headers, HttpStatus.CREATED);
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                    return new ResponseEntity<Object>(ex, HttpStatus.INTERNAL_SERVER_ERROR);
                }

            }
            else if( file.isDirectory() )
            {
                //todo recursively copy everything in the dir.
            }

        } catch(AuthenticationException ae){
            return new ResponseEntity<Object>(HttpStatus.UNAUTHORIZED);
        } catch(IOException ex){
            return new ResponseEntity<Object>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<Object>(HttpStatus.NOT_FOUND);
    }

}
