package com.kronos.api.infra.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.kronos.api.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class TokenService {

    private final String secret;
    private final String issuer;
    private final long expirationHours;

    public TokenService(
            @Value("${api.security.token.secret:kronos-dev-secret}") String secret,
            @Value("${api.security.token.issuer:kronos-api}") String issuer,
            @Value("${api.security.token.expiration-hours:2}") long expirationHours
    ) {
        this.secret = secret;
        this.issuer = issuer;
        this.expirationHours = expirationHours;
    }

    public String generateToken(User user) {
        Instant now = Instant.now();

        return JWT.create()
                .withIssuer(issuer)
                .withSubject(user.getEmail())
                .withClaim("userUuid", user.getUuid().toString())
                .withClaim("name", user.getName())
                .withClaim("username", user.getUsername())
                .withIssuedAt(now)
                .withExpiresAt(now.plus(expirationHours, ChronoUnit.HOURS))
                .sign(Algorithm.HMAC256(secret));
    }

    public Optional<String> getSubject(String token) {
        try {
            String subject = JWT.require(Algorithm.HMAC256(secret))
                    .withIssuer(issuer)
                    .build()
                    .verify(token)
                    .getSubject();

            return Optional.ofNullable(subject);
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }
}
