/*
 * Copyright (c) 2015  Mike Nimer & 11:58 Labs
 */

package com.familydam.core.security;

import org.apache.jackrabbit.oak.security.SecurityProviderImpl;
import org.apache.jackrabbit.oak.spi.security.ConfigurationParameters;
import org.apache.jackrabbit.oak.spi.security.authentication.AuthenticationConfiguration;
import org.apache.jackrabbit.oak.spi.security.authentication.token.TokenConfiguration;
import org.apache.jackrabbit.oak.spi.security.authorization.AuthorizationConfiguration;
import org.apache.jackrabbit.oak.spi.security.principal.PrincipalConfiguration;
import org.apache.jackrabbit.oak.spi.security.privilege.PrivilegeConfiguration;
import org.apache.jackrabbit.oak.spi.security.user.UserConfiguration;

/**
 * Created by mnimer on 6/2/15.
 */
public class SecurityProvider extends SecurityProviderImpl
{
    public SecurityProvider()
    {
        super();
    }


    public SecurityProvider(ConfigurationParameters configuration)
    {
        super(configuration);
    }


    @Override protected void bindPrincipalConfiguration(PrincipalConfiguration reference)
    {
        super.bindPrincipalConfiguration(reference);
    }


    @Override protected void bindTokenConfiguration(TokenConfiguration reference)
    {
        super.bindTokenConfiguration(reference);
    }


    @Override public <T> T getConfiguration(Class<T> configClass)
    {
        return super.getConfiguration(configClass);
    }


    @Override protected void bindAuthorizationConfiguration(AuthorizationConfiguration authorizationConfiguration)
    {
        super.bindAuthorizationConfiguration(authorizationConfiguration);
    }


    @Override protected void bindAuthenticationConfiguration(AuthenticationConfiguration authenticationConfiguration)
    {
        super.bindAuthenticationConfiguration(authenticationConfiguration);
    }


    @Override protected void bindPrivilegeConfiguration(PrivilegeConfiguration privilegeConfiguration)
    {
        super.bindPrivilegeConfiguration(privilegeConfiguration);
    }


    @Override protected void bindUserConfiguration(UserConfiguration userConfiguration)
    {
        super.bindUserConfiguration(userConfiguration);
    }
}
