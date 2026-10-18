package cl.duocuc.sanosysalvos.bff.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;

/**
 * Valida los access tokens emitidos por Auth0 (IDaaS) verificando:
 * <ul>
 *   <li>Firma RS256 contra el JWKS del tenant (clave pública).</li>
 *   <li>Emisor (iss) igual al dominio de Auth0.</li>
 *   <li>Audiencia (aud) igual a la API configurada.</li>
 *   <li>Vigencia (exp).</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class JwtValidator {

    private final Auth0JwkStore jwkStore;

    @Value("${auth0.domain}")
    private String domain;

    @Value("${auth0.audience}")
    private String audience;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public Claims validate(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(publicKeyFor(token))
                .requireIssuer("https://" + domain + "/")
                .build()
                .parseSignedClaims(token)
                .getPayload();

        if (!isTokenVigente(claims)) {
            throw new io.jsonwebtoken.JwtException("Token expirado");
        }
        validarAudience(claims);
        return claims;
    }

    public boolean isValid(String token) {
        try {
            validate(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private PublicKey publicKeyFor(String token) {
        try {
            String headerJson = new String(decodificarSegmento(token.split("\\.")[0]), StandardCharsets.UTF_8);
            String kid = MAPPER.readTree(headerJson).path("kid").asText();
            PublicKey key = jwkStore.findKeyByKid(kid);
            if (key == null) {
                throw new io.jsonwebtoken.security.SecurityException("Clave pública (kid=" + kid + ") no encontrada en JWKS");
            }
            return key;
        } catch (Exception e) {
            throw new io.jsonwebtoken.security.SecurityException("No se pudo resolver la clave pública del token", e);
        }
    }

    private boolean isTokenVigente(Claims claims) {
        Date exp = claims.getExpiration();
        return exp != null && exp.after(new Date());
    }

    private void validarAudience(Claims claims) {
        Object aud = claims.get("aud");
        if (aud == null) {
            throw new io.jsonwebtoken.JwtException("El token no contiene audiencia");
        }
        boolean ok = audience.equals(aud.toString())
                || (aud instanceof Collection<?> lista && lista.contains(audience));
        if (!ok) {
            throw new io.jsonwebtoken.JwtException("Audiencia inválida");
        }
    }

    private static byte[] decodificarSegmento(String segment) {
        String padded = segment;
        int rem = segment.length() % 4;
        if (rem == 2) {
            padded = segment + "==";
        } else if (rem == 3) {
            padded = segment + "=";
        } else if (rem == 1) {
            throw new IllegalArgumentException("Longitud base64url inválida");
        }
        return Base64.getUrlDecoder().decode(padded.getBytes(StandardCharsets.US_ASCII));
    }
}