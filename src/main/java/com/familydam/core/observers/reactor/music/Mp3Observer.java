/*
 * Copyright (c) 2015  Mike Nimer & 11:58 Labs
 */

package com.familydam.core.observers.reactor.music;

import com.familydam.core.FamilyDAMConstants;
import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import reactor.spring.context.annotation.Consumer;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mnimer on 5/26/15.
 */
@Consumer
public class Mp3Observer
{
    private Log log = LogFactory.getLog(this.getClass());


    public Node execute(Session session, Node node) throws RepositoryException, IOException, InterruptedException, UnsupportedTagException, com.mpatric.mp3agic.InvalidDataException
    {
        log.debug("{mp3.metadata Observer} " +node.getPath());


        Mp3File mp3file = null;

        InputStream is = JcrUtils.readFile(node);
        Map<String, Object> md = new HashMap<String, Object>();

        //Create temp file for the mp3parser to open
        File tmpFile = File.createTempFile(node.getIdentifier(), ".mp3");
        // write the inputStream to a FileOutputStream
        OutputStream outputStream = new FileOutputStream(tmpFile);

        int read = 0;
        byte[] bytes = new byte[1024];

        while ((read = is.read(bytes)) != -1) {
            outputStream.write(bytes, 0, read);
        }

        tmpFile.deleteOnExit();

        try
        {
            mp3file = new Mp3File(tmpFile.getAbsolutePath());
            Node metadataNode = JcrUtils.getOrAddNode(node, FamilyDAMConstants.METADATA, JcrConstants.NT_UNSTRUCTURED);

            if (mp3file.hasId3v1Tag())
            {

                ID3v1 id3v1Tag = mp3file.getId3v1Tag();
                setMetadata(metadataNode, FamilyDAMConstants.TITLE, id3v1Tag.getTitle());
                setMetadata(metadataNode, FamilyDAMConstants.TRACK, id3v1Tag.getTrack());
                setMetadata(metadataNode, FamilyDAMConstants.ARTIST, id3v1Tag.getArtist());
                setMetadata(metadataNode, FamilyDAMConstants.ALBUM, id3v1Tag.getAlbum());
                setMetadata(metadataNode, FamilyDAMConstants.YEAR, id3v1Tag.getYear());
                setMetadata(metadataNode, FamilyDAMConstants.GENRE_CODE, new Integer(id3v1Tag.getGenre()).toString());
                setMetadata(metadataNode, FamilyDAMConstants.GENRE, id3v1Tag.getGenreDescription());
                setMetadata(metadataNode, FamilyDAMConstants.COMMENT, id3v1Tag.getComment());
                setMetadata(metadataNode, FamilyDAMConstants.VERSION, id3v1Tag.getVersion());
            }
            
            if (mp3file.hasId3v2Tag()) {
                ID3v2 id3v2Tag = mp3file.getId3v2Tag();

                setMetadata(metadataNode, FamilyDAMConstants.TITLE, id3v2Tag.getTitle());
                setMetadata(metadataNode, FamilyDAMConstants.TRACK, id3v2Tag.getTrack());
                setMetadata(metadataNode, FamilyDAMConstants.ARTIST, id3v2Tag.getArtist());
                setMetadata(metadataNode, FamilyDAMConstants.ALBUM, id3v2Tag.getAlbum());
                setMetadata(metadataNode, FamilyDAMConstants.ALBUM_ARTIST, id3v2Tag.getAlbumArtist());
                setMetadata(metadataNode, FamilyDAMConstants.ALBUM_IMAGE_MIMETYPE, id3v2Tag.getAlbumImageMimeType());
                setMetadata(metadataNode, FamilyDAMConstants.YEAR, id3v2Tag.getYear());
                setMetadata(metadataNode, FamilyDAMConstants.GENRE_CODE, new Integer(id3v2Tag.getGenre()).toString());
                setMetadata(metadataNode, FamilyDAMConstants.GENRE, id3v2Tag.getGenreDescription());
                setMetadata(metadataNode, FamilyDAMConstants.COMMENT, id3v2Tag.getComment());
                setMetadata(metadataNode, FamilyDAMConstants.VERSION, id3v2Tag.getVersion());
                setMetadata(metadataNode, FamilyDAMConstants.COMMENT, id3v2Tag.getComment());
                //node.setProperty(FamilyDAMConstants.CHAPTERS, id3v2Tag.getChapters());
                //node.setProperty(FamilyDAMConstants.CHAPTER_TOC, id3v2Tag.getChapterTOC());
                setMetadata(metadataNode, FamilyDAMConstants.COMPOSER, id3v2Tag.getComposer());
                setMetadata(metadataNode, FamilyDAMConstants.COPYRIGHT, id3v2Tag.getCopyright());
                setMetadata(metadataNode, FamilyDAMConstants.ENCODER, id3v2Tag.getEncoder());
                setMetadata(metadataNode, FamilyDAMConstants.COMMENT, id3v2Tag.getComment());
                setMetadata(metadataNode, FamilyDAMConstants.ITUNES_COMMENT, id3v2Tag.getItunesComment());
                setMetadata(metadataNode, FamilyDAMConstants.ORIGINAL_ARTIST, id3v2Tag.getOriginalArtist());
                setMetadata(metadataNode, FamilyDAMConstants.PUBLISHER, id3v2Tag.getPublisher());

                //add rendition
                //JcrUtils.getOrAddNode(node, FamilyDAMConstants.ALBUM_IMAGE, JcrConstants.JCR_CONTENT);
                //node.setProperty(FamilyDAMConstants.ALBUM_IMAGE, id3v2Tag.getAlbumImage());
            }

            session.save();
        }

        finally
        {
            if( tmpFile != null )
            {
                tmpFile.delete();
            }
        }

        return null;
    }


    private void setMetadata(Node metadataNode, String prop, String value)
    {
        try {
            if (!metadataNode.hasProperty(prop)) {
                metadataNode.setProperty(prop, value);
            }else if( metadataNode.hasProperty(prop) && value != null ){
                metadataNode.setProperty(prop, value);
            }
        }catch(RepositoryException re){
            re.printStackTrace();
        }
    }


}
