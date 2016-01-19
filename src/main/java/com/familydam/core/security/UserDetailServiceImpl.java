/*
 * Copyright (c) 2015  Mike Nimer & 11:58 Labs
 */

package com.familydam.core.security;

import com.familydam.core.dao.UserDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
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


    @Cacheable()
    @Override
    public UserDetails loadUserByUsername(String principal_) throws UsernameNotFoundException
    {
        CustomUserDetails user = userDao.getUserByPrincipal(principal_);
        return user;
    }


    public UserDetails loadUser(String principal_, String password_) throws UsernameNotFoundException
    {
        CustomUserDetails user = userDao.getUser(principal_, password_);
        return user;
    }


}
