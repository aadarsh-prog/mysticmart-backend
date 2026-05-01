package com.mysticmart.auth.security;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component @Slf4j
public class JwtUtil {
    @Value("${app.jwt.secret}") private String jwtSecret;
    @Value("${app.jwt.expiration-ms}") private long jwtExpirationMs;
    @Value("${app.jwt.refresh-expiration-ms}") private long refreshExpirationMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }
    public String generateAccessToken(UserDetails ud) {
        return build(Map.of("type","access"), ud.getUsername(), jwtExpirationMs);
    }
    public String generateRefreshToken(UserDetails ud) {
        return build(Map.of("type","refresh"), ud.getUsername(), refreshExpirationMs);
    }
    private String build(Map<String,Object> claims, String subject, long exp) {
        return Jwts.builder().claims(claims).subject(subject)
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis()+exp))
                .signWith(getSigningKey()).compact();
    }
    public String extractUsername(String token) { return extractClaim(token, Claims::getSubject); }
    public <T> T extractClaim(String token, Function<Claims,T> resolver) {
        return resolver.apply(Jwts.parser().verifyWith(getSigningKey()).build()
                .parseSignedClaims(token).getPayload());
    }
    public boolean isTokenValid(String token, UserDetails ud) {
        try { return extractUsername(token).equals(ud.getUsername()) && !isExpired(token); }
        catch (JwtException e) { log.warn("JWT invalid: {}", e.getMessage()); return false; }
    }
    private boolean isExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }
}
