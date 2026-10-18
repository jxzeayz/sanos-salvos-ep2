package cl.duocuc.sanosysalvos.auth.service;

import cl.duocuc.sanosysalvos.auth.model.Usuario;
import cl.duocuc.sanosysalvos.auth.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import cl.duocuc.sanosysalvos.auth.model.RolUsuario;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public String extractEmailFromHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Token no proporcionado");
        }
        return jwtService.extractEmail(authHeader.substring(7));
    }

    /**
     * Sincroniza el perfil local del usuario a partir de la identidad de
     * Auth0 (IDaaS). Si el email no existe en la BD local, lo crea con el
     * rol indicado (o DUEÑO por defecto). No gestiona contraseñas: la
     * autenticación la realiza Auth0.
     */
    public Map<String, Object> sincronizarUsuario(String email, Map<String, Object> body) {
        String nombre = body.get("nombre") != null ? body.get("nombre").toString() : null;
        String telefono = body.get("telefono") != null ? body.get("telefono").toString() : null;
        String rolTexto = body.get("rol") != null ? body.get("rol").toString() : "DUEÑO";

        Optional<Usuario> existente = usuarioRepository.findByEmailAndEliminadoFalse(email);
        if (existente.isPresent()) {
            Usuario usuario = existente.get();
            RolUsuario rol = parsearRol(rolTexto);
            if (!usuario.getRol().equals(rol)) {
                usuario.setRol(rol);
            }
            if (nombre != null && !nombre.isBlank()) {
                usuario.setNombre(nombre);
            }
            if (telefono != null) {
                usuario.setTelefono(telefono);
            }
            usuarioRepository.save(usuario);
        } else {
            RolUsuario rol = parsearRol(rolTexto);
            Usuario usuario = Usuario.builder()
                    .nombre(nombre != null && !nombre.isBlank() ? nombre : email)
                    .email(email)
                    .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .telefono(telefono)
                    .rol(rol)
                    .activo(true)
                    .build();
            usuarioRepository.save(usuario);
        }

        return obtenerPerfil(email);
    }

    private RolUsuario parsearRol(String texto) {
        try {
            return RolUsuario.valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return RolUsuario.DUEÑO;
        }
    }

    public Map<String, Object> obtenerPerfil(String email) {
        Usuario usuario = usuarioRepository.findByEmailAndEliminadoFalse(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        Map<String, Object> perfil = new HashMap<>();
        perfil.put("usuarioId", usuario.getId());
        perfil.put("nombre", usuario.getNombre());
        perfil.put("email", usuario.getEmail());
        perfil.put("telefono", usuario.getTelefono());
        perfil.put("rol", usuario.getRol());
        return perfil;
    }

    public Object actualizarPerfil(String email, Map<String, String> body) {
        Usuario usuario = usuarioRepository.findByEmailAndEliminadoFalse(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (body.containsKey("nombre")) usuario.setNombre(body.get("nombre"));
        if (body.containsKey("email")) {
            String nuevoEmail = body.get("email");
            if (!nuevoEmail.equals(usuario.getEmail()) && usuarioRepository.existsByEmail(nuevoEmail)) {
                throw new IllegalArgumentException("El email ya est\u00e1 en uso");
            }
            usuario.setEmail(nuevoEmail);
        }
        if (body.containsKey("telefono")) usuario.setTelefono(body.get("telefono"));

        usuarioRepository.save(usuario);

        return obtenerPerfil(usuario.getEmail());
    }

    public void cambiarPassword(String email, Map<String, String> body) {
        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");

        if (currentPassword == null || newPassword == null) {
            throw new IllegalArgumentException("Debe proporcionar contrase\u00f1a actual y nueva");
        }

        if (newPassword.length() < 8) {
            throw new IllegalArgumentException("La nueva contrase\u00f1a debe tener al menos 8 caracteres");
        }

        Usuario usuario = usuarioRepository.findByEmailAndEliminadoFalse(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (!passwordEncoder.matches(currentPassword, usuario.getPasswordHash())) {
            throw new IllegalArgumentException("La contrase\u00f1a actual es incorrecta");
        }

        usuario.setPasswordHash(passwordEncoder.encode(newPassword));
        usuarioRepository.save(usuario);
    }

    public void eliminarUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (usuario.isEliminado()) {
            throw new IllegalArgumentException("Usuario ya eliminado");
        }

        anonimizarDatos(usuario);
        usuario.setEliminado(true);
        usuario.setFechaEliminacion(LocalDateTime.now());
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
    }

    public List<Map<String, Object>> listarUsuarios() {
        return usuarioRepository.findByEliminadoFalse().stream()
                .map(u -> {
                    Map<String, Object> info = new HashMap<>();
                    info.put("id", u.getId());
                    info.put("nombre", u.getNombre());
                    info.put("email", u.getEmail());
                    info.put("telefono", u.getTelefono());
                    info.put("rol", u.getRol());
                    info.put("activo", u.isActivo());
                    info.put("createdAt", u.getCreatedAt());
                    return info;
                })
                .toList();
    }

    private void anonimizarDatos(Usuario usuario) {
        usuario.setEmail("anonimo" + usuario.getId() + "@eliminado");
        usuario.setNombre("Usuario Eliminado");
        usuario.setTelefono("0000000000");
    }
}