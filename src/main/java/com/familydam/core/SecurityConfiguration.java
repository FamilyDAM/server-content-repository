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

package com.familydam.core;

import com.familydam.core.security.TokenAuthFilter;
import com.familydam.core.security.TokenHandler;
import org.apache.commons.collections.map.HashedMap;
import org.apache.jackrabbit.oak.api.Root;
import org.apache.jackrabbit.oak.spi.security.ConfigurationParameters;
import org.apache.jackrabbit.oak.spi.security.authentication.token.TokenProvider;
import org.apache.jackrabbit.oak.spi.security.user.UserConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.jaas.AuthorityGranter;
import org.springframework.security.authentication.jaas.DefaultJaasAuthenticationProvider;
import org.springframework.security.authentication.jaas.JaasAuthenticationProvider;
import org.springframework.security.authentication.jaas.memory.InMemoryConfiguration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.servlet.configuration.EnableWebMvcSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.security.auth.login.AppConfigurationEntry;
import java.util.Collections;
import java.util.Map;

/**
 * Created by mnimer on 1/26/15.
 */
@Configuration
@EnableWebMvcSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfiguration extends WebSecurityConfigurerAdapter
{

    @Autowired private TokenHandler tokenHandler;

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                // Allow anonymous resource requests
                .antMatchers("/").permitAll()
                .antMatchers("/favicon.ico").permitAll()
                .antMatchers("**/*.html").permitAll()
                .antMatchers("**/*.css").permitAll()
                .antMatchers("**/*.js").permitAll()
                .antMatchers("/api/users/**").permitAll()
                //.antMatchers(actuatorEndpoints()).hasRole(backendAdminRole)
                .anyRequest().authenticated();

        http.addFilterBefore(new TokenAuthFilter(tokenHandler), BasicAuthenticationFilter.class);

    }



    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth, UserDetailsService userDetailService) throws Exception
    {
        JaasAuthenticationProvider jaasAuthenticationProvider = new JaasAuthenticationProvider();

        auth
                //.authenticationProvider(authenticationProvider)
                .authenticationProvider(defaultJaasAuthenticationProvider())
                .userDetailsService(userDetailService)
                .passwordEncoder(new BCryptPasswordEncoder());

    }

    @Bean
    public AuthenticationEntryPoint unauthorizedEntryPoint() {
        return null;
        //return (request, response, authException) => response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }


    @Bean
    public DefaultJaasAuthenticationProvider defaultJaasAuthenticationProvider(){

        //AppConfigurationEntry entry0 = new AppConfigurationEntry("org.apache.jackrabbit.oak.security.authentication.token.TokenLoginModule", AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, Collections.EMPTY_MAP);
        AppConfigurationEntry entry1 = new AppConfigurationEntry("org.apache.jackrabbit.oak.security.authentication.token.TokenLoginModule", AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT, Collections.EMPTY_MAP);
        AppConfigurationEntry entry2 = new AppConfigurationEntry("org.apache.jackrabbit.oak.security.authentication.user.LoginModuleImpl", AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, Collections.EMPTY_MAP);
        AppConfigurationEntry[] entries = new AppConfigurationEntry[]{entry1,entry2};


        AuthorityGranter[] authorityGranters = new AuthorityGranter[0];


        Map<String, AppConfigurationEntry[]> configEntries = new HashedMap();
        configEntries.put("SPRINGSECURITY", entries);

        InMemoryConfiguration configuration = new InMemoryConfiguration(configEntries);


        DefaultJaasAuthenticationProvider provider = new DefaultJaasAuthenticationProvider();
        provider.setConfiguration(configuration);
        provider.setAuthorityGranters(authorityGranters);
        return provider;
    }


}
