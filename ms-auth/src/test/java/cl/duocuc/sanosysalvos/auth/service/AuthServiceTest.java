package cl.duocuc.sanosysalvos.auth.service;

import cl.duocuc.sanosysalvos.auth.model.RolUsuario;
import cl.duocuc.sanosysalvos.auth.model.Usuario;
import cl.duocuc.sanosysalvos.auth.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    @InjectMocks private AuthService authService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id(1L)
                .nombre("Juan Pérez")
                .email("juan@test.cl")
                .passwordHash("$2a$12$hash")
                .rol(RolUsuario.DUEÑO)
                .activo(true)
                .eliminado(false)
                .build();
    }

    @Test
    void sincronizarUsuario_nuevoUsuario_creaYRetornaPerfil() {
        when(usuarioRepository.findByEmailAndEliminadoFalse("juan@test.cl"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hash");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        Map<String, Object> perfil = authService.sincronizarUsuario(
                "juan@test.cl", Map.of("nombre", "Juan Pérez", "rol", "DUEÑO"));

        assertThat(perfil.get("email")).isEqualTo("juan@test.cl");
        assertThat(perfil.get("rol")).isEqualTo(RolUsuario.DUEÑO);
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void sincronizarUsuario_usuarioExistente_actualizaRolYNombre() {
        when(usuarioRepository.findByEmailAndEliminadoFalse("juan@test.cl"))
                .thenReturn(Optional.of(usuario))
                .thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        authService.sincronizarUsuario("juan@test.cl", Map.of("nombre", "Juan Actualizado", "rol", "ADMIN"));

        assertThat(usuario.getRol()).isEqualTo(RolUsuario.ADMIN);
        assertThat(usuario.getNombre()).isEqualTo("Juan Actualizado");
    }

    @Test
    void actualizarPerfil_cambiaDatos_retornaPerfil() {
        when(usuarioRepository.findByEmailAndEliminadoFalse("juan@test.cl")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        Object resultado = authService.actualizarPerfil("juan@test.cl", Map.of("telefono", "+56999999999"));

        assertThat(resultado).isInstanceOf(Map.class);
        assertThat(usuario.getTelefono()).isEqualTo("+56999999999");
    }

    @Test
    void actualizarPerfil_emailDuplicado_lanzaExcepcion() {
        when(usuarioRepository.findByEmailAndEliminadoFalse("juan@test.cl")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByEmail("otro@test.cl")).thenReturn(true);

        assertThatThrownBy(() -> authService.actualizarPerfil("juan@test.cl", Map.of("email", "otro@test.cl")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya está en uso");
    }

    @Test
    void cambiarPassword_passwordCorta_lanzaExcepcion() {
        assertThatThrownBy(() -> authService.cambiarPassword("juan@test.cl",
                Map.of("currentPassword", "password123", "newPassword", "corta")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("al menos 8");
    }

    @Test
    void cambiarPassword_actualIncorrecta_lanzaExcepcion() {
        when(usuarioRepository.findByEmailAndEliminadoFalse("juan@test.cl")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("incorrecta", "$2a$12$hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.cambiarPassword("juan@test.cl",
                Map.of("currentPassword", "incorrecta", "newPassword", "nuevaPassword123")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("actual es incorrecta");
    }

    @Test
    void listarUsuarios_retornaListaNoVacia() {
        Usuario admin = Usuario.builder()
                .id(2L).nombre("Admin User").email("admin@test.cl")
                .rol(RolUsuario.ADMIN).activo(true).eliminado(false).build();

        when(usuarioRepository.findByEliminadoFalse()).thenReturn(List.of(usuario, admin));

        List<Map<String, Object>> resultado = authService.listarUsuarios();

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).get("nombre")).isEqualTo("Juan Pérez");
        assertThat(resultado.get(1).get("rol")).isEqualTo(RolUsuario.ADMIN);
        verify(usuarioRepository).findByEliminadoFalse();
    }

    @Test
    void listarUsuarios_sinUsuarios_retornaListaVacia() {
        when(usuarioRepository.findByEliminadoFalse()).thenReturn(List.of());

        List<Map<String, Object>> resultado = authService.listarUsuarios();

        assertThat(resultado).isEmpty();
    }
}
