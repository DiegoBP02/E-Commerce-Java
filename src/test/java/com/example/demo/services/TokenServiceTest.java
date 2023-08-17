package com.example.demo.services;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.demo.ApplicationConfigTest;
import com.example.demo.entities.user.User;
import com.example.demo.utils.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class TokenServiceTest extends ApplicationConfigTest {

    @Autowired
    private TokenService tokenService;
    @Value("${jwt.secret}")
    private String jwtSecret;
    @Value("${token.expiration}")
    private long tokenExpiration;
    @Value("${timezone.offset}")
    private String timezoneOffSet;

    private User user = TestDataBuilder.buildUser();

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
    }

    @Test
    void givenUser_whenGenerateToken_thenGenerateToken() {
        String token = tokenService.generateToken(user);

        assertThat(token).isNotNull();
    }

    @Test
    void givenCorrectClaims_whenGenerateToken_thenGenerateTokenWithValidClaims() {
        String token = tokenService.generateToken(user);

        DecodedJWT decodedToken = JWT.decode(token);

        assertThat(decodedToken.getSubject()).isEqualTo(user.getEmail());
        assertThat(decodedToken.getClaim("id").asString())
                .isEqualTo(user.getId().toString());
    }

    @Test
    void givenCorrectExpiration_whenGenerateToken_thenGenerateTokenWithValidClaims() {
        String token = tokenService.generateToken(user);

        DecodedJWT decodedToken = JWT.decode(token);
        Date expiration = decodedToken.getExpiresAt();
        Date expectedExpiration = Date.from(LocalDateTime.now().plusSeconds(tokenExpiration)
                .toInstant(ZoneOffset.of(timezoneOffSet)));

        long toleranceMilliseconds = 1000;
        long expirationTime = expiration.getTime();
        long expectedExpirationTime = expectedExpiration.getTime();

        assertThat(expirationTime).isCloseTo(expectedExpirationTime, within(toleranceMilliseconds));
    }

    @Test
    void givenCorrectSignature_whenGenerateToken_thenGenerateTokenWithValidClaims() {
        String token = tokenService.generateToken(user);

        Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
        JWTVerifier verifier = JWT.require(algorithm).build();
        verifier.verify(token);
    }

    @Test
    void givenUser_whenGetSubject_thenReturnTheSubject() {
        String token = JWT.create()
                .withIssuer("JWT Issuer")
                .withSubject(user.getEmail())
                .withClaim("id", user.getId().toString())
                .withExpiresAt(LocalDateTime.now()
                        .plusDays(1)
                        .toInstant(ZoneOffset.of(timezoneOffSet))
                ).sign(Algorithm.HMAC256(jwtSecret));

        String retrievedSubject = tokenService.getSubject(token);
        assertThat(retrievedSubject).isEqualTo(user.getEmail());
    }
}