package cl.duocuc.sanosysalvos.bff.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifica que los códigos de error del BFF se devuelvan con el estado HTTP
 * correcto y un cuerpo JSON con mensaje legible.
 */
class ErroresTest {

    @Test
    void noAutorizadoDevuelve401ConMensaje() {
        ResponseEntity<Map> respuesta = Errores.<Map>noAutorizado().block();

        assertNotNull(respuesta);
        assertEquals(HttpStatus.UNAUTHORIZED, respuesta.getStatusCode());
        assertEquals("Token inválido o expirado", respuesta.getBody().get("mensaje"));
    }

    @Test
    void prohibidoDevuelve403ConMensaje() {
        ResponseEntity<Map> respuesta = Errores.<Map>prohibido("Acceso denegado").block();

        assertNotNull(respuesta);
        assertEquals(HttpStatus.FORBIDDEN, respuesta.getStatusCode());
        assertEquals("Acceso denegado", respuesta.getBody().get("mensaje"));
    }
}