/*
 * Copyright (c) 2016  Mike Nimer & 11:58 Labs
 */

package com.familydam.core.models;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by mnimer on 1/19/16.
 */
public class Group
{
    private String value;
    private String label;
    private Collection children = new ArrayList();


    public String getValue()
    {
        return value;
    }


    public void setValue(String value)
    {
        this.value = value;
    }


    public String getLabel()
    {
        return label;
    }


    public void setLabel(String label)
    {
        this.label = label;
    }


    public Collection getChildren()
    {
        return children;
    }


    public void setChildren(Collection children)
    {
        this.children = children;
    }
}
