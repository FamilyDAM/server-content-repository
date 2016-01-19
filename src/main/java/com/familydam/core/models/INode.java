/*
 * Copyright (c) 2015  Mike Nimer & 11:58 Labs
 */

package com.familydam.core.models;

import java.util.Calendar;
import java.util.Collection;

/**
 * Created by mnimer on 2/18/15.
 */
public interface INode
{
    String getId();
    void setId( String id );
    
    String getName();
    void setName( String name );

    Calendar getDateCreated();
    void setDateCreated( Calendar date );
    
    int getOrder();
    void setOrder( int order );

    Collection<INode> getChildren();
    void setChildren(Collection<INode> children);

    Collection<String> getMixins();
    void setMixins(Collection<String> mixins);
}
