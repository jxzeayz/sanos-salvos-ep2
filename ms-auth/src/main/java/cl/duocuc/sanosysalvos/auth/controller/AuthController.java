package cl.duocuc.sanosysalvos.auth.controller;

import cl.duocuc.sanosysalvos.auth.service.AuthService;
import cl.duocuc.sanosysalvos.auth.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    @GetMapping("/perfil")
    public ResponseEntity<Map<String, Object>> getPerfil(@RequestHeader("Authorization") String authHeader) {
        String email = authService.extractEmailFromHeader(authHeader);
        return ResponseEntity.ok(authService.obtenerPerfil(email));
    }

    /**
     * Sincroniza el perfil local del usuario desde la identidad de Auth0
     * (llamado por el BFF después del login con el IDaaS).
     */
    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> sync(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody(required = false) Map<String, Object> body) {
        String email = authService.extractEmailFromHeader(authHeader);
        return ResponseEntity.ok(authService.sincronizarUsuario(email, body == null ? Map.of() : body));
    }

    @PutMapping("/perfil")
    public ResponseEntity<?> updatePerfil(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> body) {
        String email = authService.extractEmailFromHeader(authHeader);
        return ResponseEntity.ok(authService.actualizarPerfil(email, body));
    }

    @PutMapping("/password")
    public ResponseEntity<Map<String, String>> changePassword(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> body) {
        String email = authService.extractEmailFromHeader(authHeader);
        authService.cambiarPassword(email, body);
        return ResponseEntity.ok(Map.of("mensaje", "Contraseña actualizada correctamente"));
    }

    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<Map<String, String>> eliminarUsuario(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {

        String token = authHeader != null && authHeader.startsWith("Bearer ") ? authHeader.substring(7) : null;
        if (token == null || !jwtService.isTokenValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("mensaje", "Token inválido"));
        }

        String rol = jwtService.extractRole(token);
        if (!"ADMIN".equals(rol)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("mensaje", "Acceso denegado: solo ADMIN"));
        }

        authService.eliminarUsuario(id);
        return ResponseEntity.ok(Map.of("mensaje", "Usuario eliminado correctamente"));
    }

    @GetMapping("/usuarios")
    public ResponseEntity<?> listarUsuarios(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader != null && authHeader.startsWith("Bearer ") ? authHeader.substring(7) : null;
        if (token == null || !jwtService.isTokenValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("mensaje", "Token inválido"));
        }

        String rol = jwtService.extractRole(token);
        if (!"ADMIN".equals(rol)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("mensaje", "Acceso denegado: solo ADMIN"));
        }

        List<Map<String, Object>> usuarios = authService.listarUsuarios();
        return ResponseEntity.ok(usuarios);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "ms-auth"));
    }

}
