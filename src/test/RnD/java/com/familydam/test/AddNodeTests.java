package com.familydam.test;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.api.Blob;
import org.apache.jackrabbit.oak.api.ContentRepository;
import org.apache.jackrabbit.oak.api.ContentSession;
import org.apache.jackrabbit.oak.api.Root;
import org.apache.jackrabbit.oak.api.Tree;
import org.apache.jackrabbit.oak.api.Type;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.jackrabbit.oak.plugins.segment.SegmentNodeStore;
import org.apache.jackrabbit.oak.plugins.segment.SegmentStore;
import org.apache.jackrabbit.oak.plugins.segment.file.FileStore;
import org.apache.jackrabbit.oak.security.SecurityProviderImpl;
import org.apache.jackrabbit.oak.spi.state.NodeBuilder;
import org.apache.jackrabbit.oak.util.NodeUtil;
import org.apache.jackrabbit.value.BinaryValue;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Calendar;

/**
 * JCR Add File Node test
 */
public class AddNodeTests
{
    public static void main(String[] args)
    {
        AddNodeTests addNodeTests = new AddNodeTests();
    }


    public AddNodeTests()
    {


        try {
            boolean useJcrRepo = false;

            SegmentStore store = getNodeStore();
            //Oak oak = getOak(store);

            if( useJcrRepo ){
                Repository repository = getRepository(store);
                System.out.println("1. #######################");
                listTree(repository, null);

                System.out.println("2. #######################");
                Session session = getSession(repository);
                addTestFolders(session);
                //addTestFilesJCR(session);
                addTestFilesWithJCRUtils(session);
                listTree(repository, session);
                session.logout();

                System.out.println("3. #######################");
                listTree(repository, null);

            }else{
                ContentRepository repository = getContentRepository(store);
                System.out.println("1. #######################");
                listTree(repository, null);
                System.out.println("2. #######################");
                //Session session = getSession(repository);
                ContentSession session = getSession(repository);
                addTestFolders(session);
                addTestFilesWithBuilder(session, store);
                //addTestFilesWithNodeUtil(contentSession, store);
                listTree(repository, session);
                System.out.println("3. #######################");
                listTree(repository, null);
            }

        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

    }


    private void listTree(Repository repository, Session session) throws Exception
    {
        if( session == null ) {
            session = getSession(repository);
        }
        Node rootNode = session.getNode("/");
        Iterable<Node> nodes = JcrUtils.getChildNodes(rootNode);
        for (Node node : nodes) {
            if( node.isNodeType("mix:referenceable") ) {
                System.out.println(node.getName() + " | " + node.getUUID() + " | " + node.getPath());
            }else{
                System.out.println(node.getName() + " | " + node.getPath());
            }

            Iterable<Node> nodes2 = JcrUtils.getChildNodes(node);
            for (Node node2 : nodes2) {
                if( node2.isNodeType("mix:referenceable") ) {
                    System.out.println("---" + node2.getName() + " | " + node2.getUUID() + " | " + node2.getPath());
                }else {
                    System.out.println("---" + node2.getName()  + " | " + node2.getPath());
                }
            }
        }
    }

    private void listTree(ContentRepository repository, ContentSession session) throws Exception
    {
        if( session == null ) {
            session = getSession(repository);
        }
        Root rootNode = session.getLatestRoot();
        Tree rootTree = rootNode.getTree("/");

        Iterable<Tree> nodes = rootTree.getChildren();
        for (Tree node : nodes) {
            System.out.println(node.getName() + " | " + node.getPath());

            Iterable<Tree> nodes2 = node.getChildren();
            for (Tree node2 : nodes2) {
                System.out.println(node2.getName() + " | " + node2.getPath());
            }

        }
    }


    private void addTestFolders(Session session) throws Exception
    {
        Node root = session.getNode("/");
        JcrUtils.getOrAddFolder(root, "test1");
        JcrUtils.getOrAddFolder(root, "test1/test1a");
        JcrUtils.getOrAddFolder(root, "test2");
        JcrUtils.getOrAddFolder(root, "test2/test2a");
        session.save();
    }


    private void addTestFolders(ContentSession session) throws Exception
    {
        Root root = session.getLatestRoot();
        Tree tree = root.getTree("/");
        tree.addChild("test1");
        tree.addChild("test1/test1a");
        tree.addChild("test2");
        tree.addChild("test2/test2a");
        session.getLatestRoot().commit();
    }


    private void addTestFilesJCR(Session session) throws Exception
    {
        //InputStream is1 = this.getClass().getClassLoader().getResourceAsStream("image1.jpg");
        //InputStream is2 = this.getClass().getClassLoader().getResourceAsStream("image2.jpg");

        File file = new File("/Users/mnimer/Desktop/ScreenShot.png");
        InputStream is1 = new FileInputStream(file);

        Node rootNode = session.getNode("/test1");
        if( !session.nodeExists("/file1") ) {

            //create the file node - see section 6.7.22.6 of the spec
            String fileName = file.getName() +"_" +System.currentTimeMillis();
            Node fileNode = rootNode.addNode (fileName, "nt:file");
            fileNode.addMixin("mix:referenceable");

            //create the mandatory child node - jcr:content
            Node resNode = fileNode.addNode ("jcr:content", "nt:resource");
            resNode.setProperty ("jcr:mimeType", "image/png");
            resNode.setProperty ("jcr:data", new FileInputStream (file));
            Calendar lastModified = Calendar.getInstance();
            lastModified.setTimeInMillis (file.lastModified ());
            resNode.setProperty ("jcr:lastModified", lastModified);


            //Node img1 = JcrUtils.putFile(rootNode, "file1", "image/png", is1);
            System.out.println(fileNode.getName() +" | " +fileNode.getUUID() +" | " +fileNode.getPath());
        }

        session.save();
    }



    private void addTestFilesWithJCRUtils(Session session) throws Exception
    {
        session.refresh(true);
        //InputStream is1 = this.getClass().getClassLoader().getResourceAsStream("image1.jpg");
        //InputStream is2 = this.getClass().getClassLoader().getResourceAsStream("image2.jpg");

        File file = new File("/Users/mnimer/Desktop/ScreenShot.png");
        InputStream is1 = new FileInputStream(file);

        Node rootNode = session.getNode("/test1");
        if( !session.nodeExists("/file1") ) {

            String fileName = file.getName() +"_" +System.currentTimeMillis();
            Node fileNode = JcrUtils.putFile(rootNode, fileName, "image/png", new BufferedInputStream(new FileInputStream(file)) );

            //Node img1 = JcrUtils.putFile(rootNode, "file1", "image/png", is1);
            if( fileNode.hasNode("mix:referenceable") ) {
                System.out.println(fileNode.getName() + " | " + fileNode.getUUID() + " | " + fileNode.getPath());
            }else{
                System.out.println(fileNode.getName()  + " | " + fileNode.getPath());
            }
        }

        session.save();
    }


    private void addTestFilesWithBuilder(ContentSession session, SegmentStore store) throws Exception
    {
        Root root = session.getLatestRoot();
        Tree rootTree = root.getTree("/");

        NodeBuilder builder = store.getHead().builder();

        File file = new File("/Users/mnimer/Desktop/ScreenShot.png");
        //InputStream is1 = new FileInputStream(file);

        String fileName = file.getName() +"_" +System.currentTimeMillis();
        NodeBuilder fileNode = null;
        if( builder.hasChildNode(fileName) ) {
            fileNode = builder.getChildNode(fileName);
        }else{
            fileNode = builder.child(fileName);
        }


        fileNode.setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FILE, Type.NAME);
        NodeBuilder contentNode = fileNode.child(JcrConstants.JCR_CONTENT);
        contentNode.setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_RESOURCE);

        // set file contents
        //Value[] binaryContent = new Value[1];
        InputStream is = new FileInputStream(file);
        //BinaryValue binaryValue = new BinaryValue(is);
        Blob blob = contentNode.createBlob(is);
        fileNode.setProperty(JcrConstants.JCR_CONTENT, blob, Type.BINARY);

        //content.setProperty(JcrConstants.JCR_DATA, blobId);
        session.getLatestRoot().commit();
        //session.getLatestRoot().commit();

    }


    private void addTestFilesWithNodeUtil(ContentSession session, SegmentStore store) throws Exception
    {
        Root root = session.getLatestRoot();
        Tree rootTree = root.getTree("/");
        NodeUtil nodeUtil = new NodeUtil(rootTree);

        File file = new File("/Users/mnimer/Desktop/ScreenShot.png");
        //InputStream is = new FileInputStream(file);

        String fileName = file.getName() +"_" +System.currentTimeMillis();
        NodeUtil fileNode = nodeUtil.getOrAddChild(fileName, JcrConstants.NT_FILE);

        fileNode.setString(JcrConstants.JCR_MIMETYPE, "image/png");

        Calendar lastModified = Calendar.getInstance();
        lastModified.setTimeInMillis(file.lastModified());
        fileNode.setDate(JcrConstants.JCR_LASTMODIFIED, lastModified.getTimeInMillis());


        NodeUtil contentNode = fileNode.getOrAddChild(JcrConstants.JCR_CONTENT, JcrConstants.NT_RESOURCE);
        // set file contents
        InputStream is = new FileInputStream(file);
        Value[] binaryContent = new Value[1];
        binaryContent[0] = new BinaryValue(is);
        contentNode.setValues(JcrConstants.JCR_DATA, binaryContent);

        session.getLatestRoot().commit();
    }





    private FileStore getNodeStore() throws Exception
    {
        File directory = new File("jcrTestRepo", "tarmk-" +System.currentTimeMillis());
        //File directory = new File("jcrTestRepo", "tarmk5");
        final FileStore fileStore = new FileStore(directory, 1, false);
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                fileStore.close();
            }
        }));

        return fileStore;
    }

    private Oak getOak(SegmentStore store) throws Exception
    {
        Oak oak = new Oak(new SegmentNodeStore(store)).with(new SecurityProviderImpl());
        return oak;
        //return new Oak(new SegmentNodeStore(store));
        //return new Oak(new SegmentNodeStore(memoryStore));
    }

    private Repository getRepository(SegmentStore store)
    {
        Oak oak = new Oak(new SegmentNodeStore(store));//.with(new SecurityProviderImpl());
        return new Jcr(oak).createRepository();
    }

    private ContentRepository getContentRepository(SegmentStore store)
    {
        Oak oak = new Oak(new SegmentNodeStore(store)).with(new SecurityProviderImpl());
        return oak.createContentRepository();
    }


    private Session getSession(Repository repository) throws Exception
    {
        SimpleCredentials credentials = new SimpleCredentials("admin", "admin".toCharArray());
        return repository.login(credentials, null);
    }

    private ContentSession getSession(ContentRepository repository) throws Exception
    {
        SimpleCredentials credentials = new SimpleCredentials("admin", "admin".toCharArray());
        //GuestCredentials credentials = new GuestCredentials();
        return repository.login(credentials, "default");
    }

}
