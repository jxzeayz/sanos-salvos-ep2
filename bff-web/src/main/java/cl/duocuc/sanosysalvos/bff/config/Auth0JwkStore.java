package cl.duocuc.sanosysalvos.bff.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
import java.util.concurrent.ConcurrentHashMap;

/**
 * Obtiene y cachea las claves públicas (JWKS) del dominio de Auth0 para
 * validar la firma RS256 de los access tokens emitidos por el IDaaS.
 */
@Component
public class Auth0JwkStore {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ConcurrentHashMap<String, PublicKey> cache = new ConcurrentHashMap<>();

    @Value("${auth0.domain}")
    private String domain;

    /**
     * Devuelve la clave pública correspondiente al "kid" del token,
     * descargando el JWKS desde Auth0 si aún no está en caché.
     */
    public PublicKey findKeyByKid(String kid) {
        if (kid == null || kid.isBlank()) {
            return null;
        }
        PublicKey key = cache.get(kid);
        if (key != null) {
            return key;
        }
        loadKeys();
        return cache.get(kid);
    }

    private void loadKeys() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://" + domain + "/.well-known/jwks.json"))
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
                    cache.put(kid, key);
                }
            }
        } catch (Exception ignored) {
            // El JWKS no está disponible transitoriamente; se reintentará en la siguiente petición.
        }
    }

    private PublicKey rsaPublicKey(String modulus, String exponent) {
        try {
            BigInteger n = new BigInteger(1, b64UrlDecode(modulus));
            BigInteger e = new BigInteger(1, b64UrlDecode(exponent));
            RSAPublicKeySpec spec = new RSAPublicKeySpec(n, e);
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception ex) {
            return null;
        }
    }

    private static byte[] b64UrlDecode(String value) {
        String padded = value;
        int rem = value.length() % 4;
        if (rem == 2) {
            padded = value + "==";
        } else if (rem == 3) {
            padded = value + "=";
        } else if (rem == 1) {
            throw new IllegalArgumentException("Longitud base64url inválida");
        }
        return Base64.getUrlDecoder().decode(padded.getBytes(StandardCharsets.US_ASCII));
    }
}