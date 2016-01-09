/*
 * Copyright (c) 2016  Mike Nimer & 11:58 Labs
 */

package com.familydam.core.models;

/**
 * Created by mnimer on 1/7/16.
 */
public class DamImage extends File
{
    private Double width;
    private Double height;


    public Double getWidth()
    {
        return width;
    }


    public void setWidth(Double width)
    {
        this.width = width;
    }


    public Double getHeight()
    {
        return height;
    }


    public void setHeight(Double height)
    {
        this.height = height;
    }
}
