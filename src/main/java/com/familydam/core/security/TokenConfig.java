/*
 * Copyright (c) 2015  Mike Nimer & 11:58 Labs
 */

package com.familydam.core.security;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.oak.api.Root;
import org.apache.jackrabbit.oak.spi.security.ConfigurationBase;
import org.apache.jackrabbit.oak.spi.security.ConfigurationParameters;
import org.apache.jackrabbit.oak.spi.security.SecurityConfiguration;
import org.apache.jackrabbit.oak.spi.security.authentication.token.TokenConfiguration;
import org.apache.jackrabbit.oak.spi.security.authentication.token.TokenInfo;
import org.apache.jackrabbit.oak.spi.security.authentication.token.TokenProvider;

import javax.jcr.Credentials;
import java.util.Map;

/**
 * Created by mnimer on 12/22/15.
 */
@Component()
@Service({TokenConfiguration.class, SecurityConfiguration.class})
public class TokenConfig extends ConfigurationBase implements TokenConfiguration {

    public TokenConfig() {
        super();
    }

    public TokenConfig(SecurityProvider securityProvider) {
        super(securityProvider, securityProvider.getParameters(NAME));
    }

    @Activate
    private void activate(Map<String, Object> properties) {
        setParameters(ConfigurationParameters.of(properties));
    }

    //----------------------------------------------< SecurityConfiguration >---
    @Override
    public String getName() {
        return NAME;
    }

    //-------------------------------------------------< TokenConfiguration >---
    @Override
    public TokenProvider getTokenProvider(Root root) {
        return new TokenProvider()
        {
            @Override public boolean doCreateToken(Credentials credentials)
            {
                return false;
            }


            @Override public TokenInfo createToken(Credentials credentials)
            {
                return null;
            }


            @Override public TokenInfo createToken(String userId, Map<String, ?> attributes)
            {
                return null;
            }


            @Override public TokenInfo getTokenInfo(String token)
            {
                return null;
            }
        };
    }
}