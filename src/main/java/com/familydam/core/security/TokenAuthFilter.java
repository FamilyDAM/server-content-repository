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

package com.familydam.core.security;

import com.familydam.core.FamilyDAMConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.api.security.authentication.token.TokenCredentials;
import org.apache.jackrabbit.oak.spi.security.SecurityProvider;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.security.sasl.AuthenticationException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

/**
 * Created by mnimer on 3/19/15.
 */
public class TokenAuthFilter implements Filter
{
    Log log = LogFactory.getLog(this.getClass());

    private TokenHandler tokenHandler;
    private UserDetailServiceImpl userService;
    private Repository repository;
    private SecurityProvider securityProvider;


    public TokenAuthFilter(TokenHandler tokenHandler, UserDetailServiceImpl userService, Repository repository, SecurityProvider securityProvider)
    {
        this.tokenHandler = tokenHandler;
        this.userService = userService;
        this.repository = repository;
        this.securityProvider = securityProvider;
    }


    @Override public void init(FilterConfig filterConfig) throws ServletException
    {

    }


    @Override public void destroy()
    {

    }


    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
    {
        HttpServletRequest httpRequest = (HttpServletRequest)request;
        HttpServletResponse httpResponse = (HttpServletResponse)response;

        Optional _tokenHeader = Optional.ofNullable(httpRequest.getHeader(FamilyDAMConstants.XAUTHTOKEN));
        Optional _tokenUrlParam = Optional.ofNullable(httpRequest.getParameter(FamilyDAMConstants.XAUTHTOKENPARAM));


        try {
            String token = null;

            if (_tokenHeader.isPresent() ) {
                token = (String)_tokenHeader.get();
                log.trace("Trying to authenticate user by X-Auth-Token method. Token: " + token);
            }else if( _tokenUrlParam.isPresent() ){
                token = (String)_tokenUrlParam.get();
                log.trace("Trying to authenticate user by token url parameter. Token: " + token);
            }


            if (token != null) {
                Authentication authentication = processTokenAuthentication(token);

                // set auth for request
                SecurityContextHolder.getContext().setAuthentication(authentication);



                // generate a new token, same data with new expire dates and return it.
                //String _newToken = tokenHandler.createTokenForUser(  ((UserAuthentication)authentication).getUser()  );
                //((HttpServletResponse) response).setHeader(FamilyDAMConstants.XAUTHTOKENREFRESH, _newToken);

                // call next filter
                chain.doFilter(request, response);

                //clear after request
                SecurityContextHolder.getContext().setAuthentication(null);
            }else {
                //log.debug("AuthenticationFilter is passing request down the filter chain");
                chain.doFilter(request, response);
            }
        }
        catch (InternalAuthenticationServiceException internalAuthenticationServiceException) {
            SecurityContextHolder.clearContext();
            log.error("Internal authentication service exception", internalAuthenticationServiceException);
            httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        catch (AuthenticationException authenticationException) {
            SecurityContextHolder.clearContext();
            httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, authenticationException.getMessage());
        }
        finally {
            
        }
    }


    private UserAuthentication  processTokenAuthentication(String token_) throws AuthenticationException
    {
        try {
            Credentials _credentials = new TokenCredentials(token_);
            ((TokenCredentials)_credentials).setAttribute(".token", "");
            Session session = repository.login(_credentials);
            CustomUserDetails user = (CustomUserDetails)userService.loadUserByUsername(session.getUserID());
            user.setSession(session);

            //reset token
            //Node root = session.getRootNode();
            //SecurityProvider sp = getSecurityProvider();
            //TokenConfiguration tc = securityProvider.getConfiguration(TokenConfiguration.class);
            //TokenProvider tp = tc.getTokenProvider(root);
            //TokenInfo tokenInfo = tp.createToken(_credentials);


            UserAuthentication authentication = new UserAuthentication(user);
            // save the authenticated user object in the Spring Security Context
            authentication.setCredentials(_credentials);


            return authentication;
        }catch(Exception ex){
            throw new AuthenticationException(ex.getMessage(), ex);
        }
    }

}


