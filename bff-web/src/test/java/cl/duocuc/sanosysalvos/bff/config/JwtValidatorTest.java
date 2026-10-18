package cl.duocuc.sanosysalvos.bff.config;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Pruebas básicas del validador de tokens de Auth0 (firma RS256, issuer,
 * audiencia y vigencia). Verifican el comportamiento esperado por la pauta:
 * "responder a pruebas básicas".
 */
@ExtendWith(MockitoExtension.class)
class JwtValidatorTest {

    private static final String DOMINIO = "dev-test.auth0.com";
    private static final String AUDIENCIA = "https://sanosysalvos/api";

    @Mock
    private Auth0JwkStore jwkStore;

    private static final KeyPair PAR_CLAVES;

    static {
        try {
            KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
            generador.initialize(2048);
            PAR_CLAVES = generador.generateKeyPair();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private JwtValidator crearValidador() {
        JwtValidator validador = new JwtValidator(jwkStore);
        ReflectionTestUtils.setField(validador, "domain", DOMINIO);
        ReflectionTestUtils.setField(validador, "audience", AUDIENCIA);
        return validador;
    }

    private void simularClave(String kid) {
        when(jwkStore.findKeyByKid(kid)).thenReturn(publicKey());
    }

    private PublicKey publicKey() {
        return PAR_CLAVES.getPublic();
    }

    private String firmarToken(String kid, Map<String, Object> claims, long expiracionOffsetMs) {
        return Jwts.builder()
                .header().keyId(kid).and()
                .claims(claims)
                .issuer("https://" + DOMINIO + "/")
                .audience().add(AUDIENCIA).and()
                .subject("test@example.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiracionOffsetMs))
                .signWith(PAR_CLAVES.getPrivate())
                .compact();
    }

    @Test
    void validaTokenAutenticoConClaims() {
        simularClave("k-1");
        String token = firmarToken("k-1",
                Map.of("email", "test@example.com", "https://sanosysalvos/rol", "CIUDADANO"),
                60_000);

        io.jsonwebtoken.Claims claims = crearValidador().validate(token);

        assertEquals("test@example.com", claims.get("email", String.class));
        assertEquals("CIUDADANO", claims.get("https://sanosysalvos/rol", String.class));
    }

    @Test
    void rechazaTokenConIssuerDistinto() {
        simularClave("k-2");
        String token = Jwts.builder()
                .header().keyId("k-2").and()
                .issuer("https://otro-tenant.auth0.com/")
                .audience().add(AUDIENCIA).and()
                .subject("test@example.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(PAR_CLAVES.getPrivate())
                .compact();

        assertThrows(Exception.class, () -> crearValidador().validate(token));
    }

    @Test
    void rechazaTokenConAudienciaDistinta() {
        simularClave("k-3");
        String token = Jwts.builder()
                .header().keyId("k-3").and()
                .issuer("https://" + DOMINIO + "/")
                .audience().add("https://otra-api").and()
                .subject("test@example.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(PAR_CLAVES.getPrivate())
                .compact();

        assertThrows(Exception.class, () -> crearValidador().validate(token));
    }

    @Test
    void rechazaTokenExpirado() {
        simularClave("k-4");
        String token = firmarToken("k-4", Map.of("email", "test@example.com"), -60_000);

        assertThrows(Exception.class, () -> crearValidador().validate(token));
    }

    @Test
    void rechazaTokenConClaveDesconocida() {
        when(jwkStore.findKeyByKid(anyString())).thenReturn(null);
        String token = firmarToken("k-no-existe", Map.of("email", "test@example.com"), 60_000);

        assertThrows(Exception.class, () -> crearValidador().validate(token));
    }

    @Test
    void devuelveFalseParaTokenInvalido() {
        simularClave("k-5");
        String token = firmarToken("k-5", Map.of("email", "test@example.com"), 60_000);

        JwtValidator validador = crearValidador();

        assertEquals(true, validador.isValid(token));
        assertFalse(validador.isValid("no-es-un-jwt"));
    }
}