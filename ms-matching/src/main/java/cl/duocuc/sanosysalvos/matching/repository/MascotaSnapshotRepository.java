package cl.duocuc.sanosysalvos.matching.repository;

import cl.duocuc.sanosysalvos.matching.model.MascotaSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MascotaSnapshotRepository extends JpaRepository<MascotaSnapshot, Long> {
    List<MascotaSnapshot> findByEstado(String estado);
    List<MascotaSnapshot> findByEspecieAndEstado(String especie, String estado);
}
