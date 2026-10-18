package cl.duocuc.sanosysalvos.mascotas.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Valida los access tokens emitidos por Auth0 (IDaaS): firma RS256 contra
 * el JWKS del tenant, issuer, audience y vigencia (exp).
 */
@Service
public class JwtService {

    private static final String CLAIM_ROL = "https://sanosysalvos/rol";

    private final ObjectMapper mapper = new ObjectMapper();
    private final ConcurrentHashMap<String, PublicKey> jwksCache = new ConcurrentHashMap<>();

    @Value("${auth0.domain}")
    private String auth0Domain;

    @Value("${auth0.audience}")
    private String auth0Audience;

    public Claims extractAllClaims(String token) {
        return parseConAuth0(token);
    }

    /**
     * Email del usuario. Con Auth0 el email llega en el claim "email";
     * en tokens legacy se usa el subject.
     */
    public String extractEmail(String token) {
        Claims claims = extractAllClaims(token);
        Object email = claims.get("email");
        return email != null ? email.toString() : claims.getSubject();
    }

    /**
     * Rol del usuario. Con Auth0 se lee del claim custom de la Action
     * ("https://sanosysalvos/rol"); en tokens legacy se usa "rol".
     */
    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        Object rol = claims.get(CLAIM_ROL);
        if (rol != null) {
            return rol.toString();
        }
        rol = claims.get("rol");
        return rol != null ? rol.toString() : "DUEÑO";
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Date exp = claims.getExpiration();
            return exp != null && exp.after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseConAuth0(String token) {
        PublicKey key = publicKeyPara(token);
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer("https://" + auth0Domain + "/")
                .build()
                .parseSignedClaims(token)
                .getPayload();

        if (claims.getExpiration() == null || !claims.getExpiration().after(new Date())) {
            throw new JwtException("Token expirado");
        }
        validarAudience(claims);
        return claims;
    }

    private void validarAudience(Claims claims) {
        Object aud = claims.get("aud");
        if (aud == null) {
            throw new JwtException("El token no contiene audiencia");
        }
        boolean ok = auth0Audience.equals(aud.toString())
                || (aud instanceof Collection<?> lista && lista.contains(auth0Audience));
        if (!ok) {
            throw new JwtException("Audiencia inválida");
        }
    }

    private PublicKey publicKeyPara(String token) {
        try {
            String headerJson = new String(decodificarSegmento(token.split("\\.")[0]), StandardCharsets.UTF_8);
            String kid = mapper.readTree(headerJson).path("kid").asText();

            PublicKey key = jwksCache.get(kid);
            if (key == null) {
                cargarJwks();
                key = jwksCache.get(kid);
            }
            if (key == null) {
                throw new JwtException("Clave pública (kid=" + kid + ") no encontrada en el JWKS de Auth0");
            }
            return key;
        } catch (Exception e) {
            throw new JwtException("No se pudo resolver la clave pública del token", e);
        }
    }

    private void cargarJwks() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://" + auth0Domain + "/.well-known/jwks.json"))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return;
            }

            JsonNode keys = mapper.readTree(response.body()).path("keys");
            if (!keys.isArray()) {
                return;
            }

            for (JsonNode jwk : keys) {
                String kid = jwk.path("kid").asText();
                String kty = jwk.path("kty").asText();
                if (kid.isEmpty() || !"RSA".equalsIgnoreCase(kty)) {
                    continue;
                }
                PublicKey key = rsaPublicKey(jwk.path("n").asText(), jwk.path("e").asText());
                if (key != null) {
                    jwksCache.put(kid, key);
                }
            }
        } catch (Exception ignored) {
            // El JWKS no está disponible transitoriamente; se reintentará en la siguiente petición.
        }
    }

    private PublicKey rsaPublicKey(String modulus, String exponent) {
        try {
            BigInteger n = new BigInteger(1, decodificarSegmento(modulus));
            BigInteger e = new BigInteger(1, decodificarSegmento(exponent));
            RSAPublicKeySpec spec = new RSAPublicKeySpec(n, e);
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception ex) {
            return null;
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