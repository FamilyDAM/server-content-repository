/*
 * Copyright (c) 2015  Mike Nimer & 11:58 Labs
 */

package com.familydam.core.exceptions;

/**
 * Created by mnimer on 2/18/15.
 **/
public class UnknownINodeException extends Exception
{
    public UnknownINodeException()
    {
        super();
    }


    public UnknownINodeException(String message)
    {
        super(message);
    }


    public UnknownINodeException(String message, Throwable cause)
    {
        super(message, cause);
    }


    public UnknownINodeException(Throwable cause)
    {
        super(cause);
    }


    protected UnknownINodeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
