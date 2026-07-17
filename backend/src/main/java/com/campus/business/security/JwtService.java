package com.campus.business.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private final SecretKey key;
  private final long minutes;

  public JwtService(
      @Value("${campus.jwt.secret}") String secret,
      @Value("${campus.jwt.expiration-minutes}") long minutes) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.minutes = minutes;
  }

  public String create(LoginUser u) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(u.username())
        .claim("uid", u.id())
        .claim("name", u.realName())
        .claim("role", u.role())
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusSeconds(minutes * 60)))
        .signWith(key)
        .compact();
  }

  public LoginUser parse(String token) {
    Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    return new LoginUser(
        c.get("uid", Long.class),
        c.getSubject(),
        c.get("name", String.class),
        c.get("role", String.class));
  }
}
