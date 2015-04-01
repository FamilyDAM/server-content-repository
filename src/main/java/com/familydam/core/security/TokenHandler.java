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

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Created by mnimer on 3/19/15.
 */
@Service
public class TokenHandler
{
    @Value("${token-secret}")
    private String secret;
    @Autowired
    private UserDetailServiceImpl userService;

    public TokenHandler( ) {
    }

    public UserDetails parseUserFromToken(String token) {
        Jws<Claims> _token = Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token);

        //return userService.loadUserByUsername(username);

        String _username = (String)_token.getBody().get("u");
        String _password = (String)_token.getBody().get("p");
        return userService.loadUser(_username, _password);
    }


    public String createTokenForUser(User user) {
        long issuedAt = System.currentTimeMillis() / 1000L;
        long expiresAt = issuedAt + 3600L;//1 hour

        // todo: change email to webid
        return Jwts.builder()
                .claim("typ", "JWT")
                .claim( "jti", UUID.randomUUID().toString() )
                .claim( "iss", "familydam.com" )
                .claim( "iat", issuedAt )
                .claim( "exp", expiresAt )
                .claim( "u", user.getPrincipalName()) //todo remove
                .claim( "p", user.getPassword()) //todo, remove when we get TokenLoginModule working - this is a really bad thing to include in a jwt token.
                .setSubject(user.getPrincipalName())
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }
}
