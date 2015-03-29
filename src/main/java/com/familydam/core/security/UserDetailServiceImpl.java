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

import com.familydam.core.dao.UserDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by mnimer on 1/26/15.
 */
@Service
public class UserDetailServiceImpl implements UserDetailsService
{

    @Autowired private UserDao userDao;
    @Autowired private TokenHandler tokenHandler;


    @Override
    public UserDetails loadUserByUsername(String principal_) throws UsernameNotFoundException
    {
        CustomUserDetails user = userDao.getUserByPrincipal(principal_);
        return user;
    }


    
    
    /**
     * TODO replace with a proper spring security filter
     * @param request_
     * @param authentication_
     * @return
     */
    public User validateUser(HttpServletRequest request_, Authentication authentication_)
    {
        if( authentication_ != null ) {
            return (User) loadUserByUsername((String) authentication_.getPrincipal());
        }else{
            //String _token = request_.getHeader("X-AUTH-TOKEN");
            //todo use token to authenticate the user and pull up their data
            throw new SecurityException();
        }
    }


    /**
     * Generate a JWT token we can use for future requests
     * TODO we might want to replace with a proper spring security filter
     * @param user_
     * @return
     */
    public String generateToken(User user_)
    {
        String _token = tokenHandler.createTokenForUser(user_);
        return _token;
    }
}
