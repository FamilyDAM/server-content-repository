/*
 * Copyright (c) 2015  Mike Nimer & 11:58 Labs
 */

package com.familydam.core.models;

import java.util.Calendar;
import java.util.Collection;

/**
 * Created by mnimer on 2/18/15.
 */
public class Directory implements INode
{
    public String _class = this.getClass().getCanonicalName();
    private String id;
    private String name;
    private String path;
    private String parent;
    private Calendar dataCreated;
    private int order = 0;
    private Boolean loading = false;
    private Boolean isReadOnly = true;
    private String fileType = "unknown";
    private Collection<INode> children;
    private Collection<String> mixins;


    @Override public String getId()
    {
        return id;
    }


    @Override public void setId(String id)
    {
        this.id = id;
    }


    @Override public String getName()
    {
        return name;
    }


    @Override public void setName(String name)
    {
        this.name = name;
    }


    @Override public Calendar getDateCreated()
    {
        return dataCreated;
    }


    @Override public void setDateCreated(Calendar date)
    {
        dataCreated = date;
    }


    public String getPath()
    {
        return path;
    }


    public void setPath(String path)
    {
        this.path = path;
    }


    public String getParent()
    {
        return parent;
    }


    public void setParent(String parent)
    {
        this.parent = parent;
    }


    public boolean getLoading()
    {
        return loading;
    }


    public void setLoading(boolean loading)
    {
        this.loading = loading;
    }


    @Override public int getOrder()
    {
        return order;
    }


    @Override public void setOrder(int order)
    {
        this.order = order;
    }


    public Boolean isReadOnly()
    {
        return isReadOnly;
    }


    public void setIsReadOnly(Boolean isReadOnly)
    {
        this.isReadOnly = isReadOnly;
    }


    public String getFileType()
    {
        return fileType;
    }


    public void setFileType(String fileType)
    {
        this.fileType = fileType;
    }



    @Override public Collection<INode> getChildren()
    {
        return children;
    }


    @Override public void setChildren(Collection<INode> children)
    {
        this.children = children;
    }


    @Override public Collection<String> getMixins()
    {
        return mixins;
    }


    @Override public void setMixins(Collection<String> mixins)
    {
        this.mixins = mixins;
    }

}
