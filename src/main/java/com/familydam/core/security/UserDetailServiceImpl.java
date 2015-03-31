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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

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


    public UserDetails loadUserById(String id_) throws UsernameNotFoundException
    {
        CustomUserDetails user = userDao.getUserById(id_);
        return user;
    }

}
