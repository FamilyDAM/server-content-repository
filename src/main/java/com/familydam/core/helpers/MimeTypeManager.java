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

package com.familydam.core.helpers;

/**
 * User: mikenimer
 * Date: 11/17/13
 */
public enum MimeTypeManager
{
    // Known Image Formats
    JPG("jpg", "image/jpeg"),
    JPE("jpe", "image/jpeg"),
    JPEG("jpeg", "image/jpeg"),
    PNG("png", "image/png"),
    GIF("gif", "image/gif"),
    TIF("tif", "image/tiff"),
    TIFF("tiff", "image/tiff"),
    PSD("psd", "image/vnd.adobe.photoshop"),
    SVG("svg", "image/svg+xml"),
    NEF("nef", "image/x-nikon-nef"),

    // Music, todo: find more
    MP3("mp3", "audio/mp3"), //todo get the right mime type
    OGG("ogg", "audio/ogg"), //todo get the right mime type

    // videos, todo: find more
    MPG("mpg", "video/mpeg"), //todo get the right mime type
    MPEG("mpeg", "video/mpeg"), //todo get the right mime type
    M4V("m4v", "video/m4v"), //todo get the right mime type
    MP4("mp4", "video/mp4"), //todo get the right mime type
    RAW("raw", "video/mp4"), // Special type returned from Facebook. //todo get the right mime type

    //todo, find more
    PDF("pdf", "document/"), //todo get the right mime type
    DOC("doc", "document/"), //todo get the right mime type
    EXCEL("", "document/"), //todo get the right mime type
    PAGES("pages", "document/"), //todo get the right mime type
    KEY("key", "document/"), //todo get the right mime type

    //Special Handling file formats
    PST("pst", "application/outlook"); //todo get the right mime type

    private String extension;
    private String mimeType;

    MimeTypeManager(String extension, String mimeType)
    {
        this.extension = extension;
        this.mimeType = mimeType;
    }



    public static boolean isSupportedExtension(String extension)
    {
        return true;
    }

    public static boolean isSupportedMimeType(String extension)
    {
        for (MimeTypeManager mimeTypeManager : MimeTypeManager.values() )
        {
            if( mimeTypeManager.mimeType.equalsIgnoreCase(extension) || mimeTypeManager.extension.equalsIgnoreCase(extension) )
            {
                return true;
            }
        }
        return false;
    }

    public static boolean isSupportedImageMimeType(String extension)
    {
        for (MimeTypeManager mimeTypeManager : MimeTypeManager.values() )
        {
            if( mimeTypeManager.mimeType.equalsIgnoreCase(extension) || mimeTypeManager.extension.equalsIgnoreCase(extension) )
            {
                return mimeTypeManager.mimeType.startsWith("image");
            }
        }
        return false;
    }

    public static boolean isSupportedMusicMimeType(String extension)
    {
        for (MimeTypeManager mimeTypeManager : MimeTypeManager.values() )
        {
            if( mimeTypeManager.mimeType.equalsIgnoreCase(extension) || mimeTypeManager.extension.equalsIgnoreCase(extension) )
            {
                return mimeTypeManager.mimeType.startsWith("audio");
            }
        }
        return false;
    }

    public static String getMimeType(String pathOrExt)
    {
        int fileSep = pathOrExt.lastIndexOf(".");
        int quoteSep = pathOrExt.lastIndexOf("?");
        if( fileSep > -1 && quoteSep > -1)
        {
            pathOrExt = pathOrExt.substring(fileSep+1, quoteSep);
        }
        else if( fileSep > -1 )
        {
            pathOrExt = pathOrExt.substring(fileSep+1);
        }

        for (MimeTypeManager mimeTypeManager : MimeTypeManager.values() )
        {
            if( mimeTypeManager.extension.equalsIgnoreCase(pathOrExt)  )
            {
                return mimeTypeManager.mimeType;
            }
        }

        return "application/octet-stream";// default unknown
    }


    public static String getMimeTypeForContentType(String contentType_)
    {
        for (MimeTypeManager mimeTypeManager : MimeTypeManager.values() )
        {
            if( mimeTypeManager.mimeType.equalsIgnoreCase(contentType_)  )
            {
                return mimeTypeManager.mimeType;
            }
        }

        return "application/octet-stream";// default unknown
    }


    public static boolean isImage(String path)
    {

        String prefix = "image";
        return checkType(path, prefix);

    }


    public static boolean isMusic(String path)
    {
        String prefix = "music";
        return checkType(path, prefix);
    }


    public static boolean isMovie(String path)
    {
        String prefix = "video";
        return checkType(path, prefix);
    }


    private static boolean checkType(String path, String prefix)
    {
        int pos = path.lastIndexOf(".");
        String ext = path.substring(pos+1);

        int slash = ext.indexOf("/");
        if( slash > -1 )
        {
            ext = ext.substring(0, slash);
        }

        for (MimeTypeManager mimeTypeManager : MimeTypeManager.values() )
        {
            if( mimeTypeManager.extension.equalsIgnoreCase(ext) && mimeTypeManager.mimeType.startsWith(prefix) )
            {
                return true;
            }
        }
        return false;
    }

}
