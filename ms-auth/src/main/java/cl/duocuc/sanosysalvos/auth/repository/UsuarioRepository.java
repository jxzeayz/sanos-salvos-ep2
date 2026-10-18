package cl.duocuc.sanosysalvos.auth.repository;

import cl.duocuc.sanosysalvos.auth.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByEmailAndEliminadoFalse(String email);
    boolean existsByEmail(String email);
    List<Usuario> findByEliminadoFalse();
}
