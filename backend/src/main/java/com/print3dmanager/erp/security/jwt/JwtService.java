package com.print3dmanager.erp.security.jwt;

import com.print3dmanager.erp.config.JwtProperties;
import com.print3dmanager.erp.user.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Emissão e validação de access tokens JWT (HMAC-SHA256, jjwt 0.12).
 * O subject é o e-mail do usuário; id e role viajam como claims.
 */
@Service
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
    }

    public String gerarAccessToken(User usuario) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .subject(usuario.getEmail())
                .claim("uid", usuario.getId())
                .claim("role", usuario.getRole().name())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plusMillis(properties.accessTokenExpiration())))
                .signWith(key)
                .compact();
    }

    /**
     * Extrai o e-mail (subject) se o token for válido e não expirado;
     * caso contrário retorna vazio — nunca lança exceção.
     */
    public Optional<String> extrairEmail(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.ofNullable(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /** Validade do access token em segundos (para o corpo da resposta de login). */
    public long accessTokenValidadeSegundos() {
        return properties.accessTokenExpiration() / 1000;
    }

    /** Validade do refresh token em milissegundos. */
    public long refreshTokenValidadeMillis() {
        return properties.refreshTokenExpiration();
    }
}
