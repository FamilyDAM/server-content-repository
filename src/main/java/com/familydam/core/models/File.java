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

package com.familydam.core.models;

import java.util.Collection;

/**
 * Created by mnimer on 2/18/15.
 */
public class File implements INode
{
    public String _class = this.getClass().getCanonicalName();
    
    private String id;
    private String name;
    private String path;
    private String parent;
    private int order = 0;
    private Boolean isReadOnly = true;
    private String contentType = "unknown";
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


    public String getContentType()
    {
        return contentType;
    }


    public void setContentType(String contentType)
    {
        this.contentType = contentType;
    }


    public Collection<INode> getChildren()
    {
        return children;
    }


    public void setChildren(Collection<INode> children)
    {
        this.children = children;
    }


    public Collection<String> getMixins()
    {
        return mixins;
    }


    public void setMixins(Collection<String> mixins)
    {
        this.mixins = mixins;
    }
}
