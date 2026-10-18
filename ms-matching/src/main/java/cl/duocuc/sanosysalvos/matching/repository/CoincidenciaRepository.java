package cl.duocuc.sanosysalvos.matching.repository;

import cl.duocuc.sanosysalvos.matching.model.Coincidencia;
import cl.duocuc.sanosysalvos.matching.model.EstadoCoincidencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CoincidenciaRepository extends JpaRepository<Coincidencia, Long> {
    List<Coincidencia> findByMascotaPerdidaId(Long mascotaPerdidaId);
    List<Coincidencia> findByMascotaEncontradaId(Long mascotaEncontradaId);
    List<Coincidencia> findByMascotaPerdidaIdOrMascotaEncontradaId(Long mascotaPerdidaId, Long mascotaEncontradaId);
    List<Coincidencia> findByEstado(EstadoCoincidencia estado);
    boolean existsByMascotaPerdidaIdAndMascotaEncontradaId(Long perdidaId, Long encontradaId);
    boolean existsByMascotaEncontradaIdAndMascotaPerdidaId(Long encontradaId, Long perdidaId);
}
