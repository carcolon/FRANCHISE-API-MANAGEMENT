package com.franchise.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final FranchiseSecurityProperties securityProperties;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public List<String> extractRoles(String token) {
        Object roles = extractAllClaims(token).get("roles");
        if (roles instanceof Collection<?> collection) {
            return collection.stream().map(Object::toString).toList();
        }
        return List.of();
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = Map.of(
                "roles", userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList()
        );
        return buildToken(extraClaims, userDetails);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equalsIgnoreCase(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        long expiration = securityProperties.getJwt().getExpirationMs();
        Date now = new Date(System.currentTimeMillis());
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSigningKey() {
        String secret = securityProperties.getJwt().getSecret();
        byte[] keyBytes = decodeSecret(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public long getExpirationInMs() {
        return securityProperties.getJwt().getExpirationMs();
    }

    private byte[] decodeSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret must be configured");
        }
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException ex) {
            keyBytes = tryBase64UrlOrPlain(secret);
        }
        return ensureMinimumLength(keyBytes);
    }

    private byte[] tryBase64UrlOrPlain(String secret) {
        try {
            return Decoders.BASE64URL.decode(secret);
        } catch (IllegalArgumentException ex) {
            return secret.getBytes(StandardCharsets.UTF_8);
        }
    }

    private byte[] ensureMinimumLength(byte[] input) {
        final int MIN_BYTES = 32;
        if (input.length >= MIN_BYTES) {
            return input;
        }
        byte[] padded = new byte[MIN_BYTES];
        System.arraycopy(input, 0, padded, 0, input.length);
        for (int i = input.length; i < MIN_BYTES; i++) {
            padded[i] = input[i % input.length];
        }
        return padded;
    }
}
