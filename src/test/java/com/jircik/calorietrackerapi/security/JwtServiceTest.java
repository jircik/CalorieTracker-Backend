package com.jircik.calorietrackerapi.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET =
            "VGhpc0lzQVZlcnlMb25nU2VjcmV0S2V5VXNlZEZvclRlc3RpbmdQdXJwb3Nlc09ubHkxMjM0NTY3OA==";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", 60_000L);
    }

    @Test
    @DisplayName("generateToken — deve produzir token válido com email e userId")
    void generateToken_shouldProduceValidToken() {
        String token = jwtService.generateToken("user@email.com", 42L);

        assertThat(token).isNotBlank();
        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractEmail(token)).isEqualTo("user@email.com");
        assertThat(jwtService.extractUserId(token)).isEqualTo(42L);
    }

    @Test
    @DisplayName("isTokenValid — deve retornar false quando token está malformado")
    void isTokenValid_shouldReturnFalseForMalformedToken() {
        assertThat(jwtService.isTokenValid("not-a-jwt")).isFalse();
    }

    @Test
    @DisplayName("isTokenValid — deve retornar false quando token está expirado")
    void isTokenValid_shouldReturnFalseForExpiredToken() {
        ReflectionTestUtils.setField(jwtService, "expirationMs", -1_000L);
        String expiredToken = jwtService.generateToken("user@email.com", 1L);

        assertThat(jwtService.isTokenValid(expiredToken)).isFalse();
    }

    @Test
    @DisplayName("isTokenValid — deve retornar false quando assinatura é de outra chave")
    void isTokenValid_shouldReturnFalseForWrongSignature() {
        SecretKey otherKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(
                "T3RoZXJTZWNyZXRLZXlGb3JUZXN0aW5nUHVycG9zZXNPbmx5QUJDRDEyMzQ1Njc4OQ=="));
        String foreignToken = Jwts.builder()
                .subject("user@email.com")
                .claim("userId", 1L)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(otherKey)
                .compact();

        assertThat(jwtService.isTokenValid(foreignToken)).isFalse();
    }

    @Test
    @DisplayName("isTokenValid — deve retornar false quando token é nulo")
    void isTokenValid_shouldReturnFalseForNullToken() {
        assertThat(jwtService.isTokenValid(null)).isFalse();
    }
}
