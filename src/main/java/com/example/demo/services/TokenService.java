package com.example.demo.services;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.example.demo.entities.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

@Service
public class TokenService {
    @Value("${jwt.secret}")
    private String jwtSecret;
    @Value("${token.expiration}")
    private long tokenExpiration;
    @Value("${timezone.offset}")
    private String timezoneOffSet;

    public String generateToken(User user) {
        return JWT.create()
                .withIssuer("JWT Issuer")
                .withSubject(user.getEmail())
                .withClaim("id", user.getId().toString())
                .withIssuedAt(new Date(System.currentTimeMillis()))
                .withExpiresAt(Date.from(LocalDateTime.now()
                        .plusSeconds(tokenExpiration)
                        .toInstant(ZoneOffset.of(timezoneOffSet))))
                .sign(Algorithm.HMAC256(jwtSecret));
    }

    public String getSubject(String token) {
        return JWT.require(Algorithm.HMAC256(jwtSecret))
                .withIssuer("JWT Issuer")
                .build().verify(token).getSubject();
    }
}
