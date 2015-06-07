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

import com.familydam.core.security.CustomAuthenticationProvider;
import com.familydam.core.security.TokenAuthFilter;
import com.familydam.core.security.TokenHandler;
import com.familydam.core.security.UserDetailServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

import javax.jcr.Repository;

/**
 * Created by mnimer on 1/26/15.
 */
@Configuration
@EnableWebMvcSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfiguration extends WebSecurityConfigurerAdapter
{
    @Autowired private CustomAuthenticationProvider authenticationProvider;

    @Autowired private TokenHandler tokenHandler;

    @Autowired UserDetailServiceImpl userDetailsService;
    @Autowired Repository repository;

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                //.antMatchers(actuatorEndpoints()).hasRole(backendAdminRole)
                // Allow anonymous resource requests
                .antMatchers("/").permitAll()
                .antMatchers("/**").permitAll()
                .antMatchers("/favicon.ico").permitAll()
                .antMatchers("/public/**").permitAll()
                .antMatchers("/static/**").permitAll()
                .antMatchers("/api/users/**").permitAll()
                .antMatchers("/api/**").authenticated();

        http.addFilterBefore(new TokenAuthFilter(tokenHandler, userDetailsService, repository, null), BasicAuthenticationFilter.class);

    }



    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth, UserDetailsService userDetailService) throws Exception
    {
        auth
                .authenticationProvider(authenticationProvider)
                .userDetailsService(userDetailService)
                .passwordEncoder(new BCryptPasswordEncoder());

    }

    @Bean
    public AuthenticationEntryPoint unauthorizedEntryPoint() {
        return null;
        //return (request, response, authException) => response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }
}
