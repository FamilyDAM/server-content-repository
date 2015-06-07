/*
 * Copyright (c) 2015  Mike Nimer & 11:58 Labs
 */

package com.familydam.core.plugins;

import org.apache.jackrabbit.oak.spi.lifecycle.WorkspaceInitializer;
import org.apache.jackrabbit.oak.spi.state.NodeBuilder;

/**
 * Created by mnimer on 2/6/15.
 */
public class InitialWorkspaceConfig implements WorkspaceInitializer
{
    @Override public void initialize(NodeBuilder builder, String workspaceName)
    {
        
    }
}
