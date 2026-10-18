package cl.duocuc.sanosysalvos.mascotas.repository;

import cl.duocuc.sanosysalvos.mascotas.model.EstadoMascota;
import cl.duocuc.sanosysalvos.mascotas.model.Mascota;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MascotaRepository extends JpaRepository<Mascota, Long> {
    List<Mascota> findByEstado(EstadoMascota estado);
    List<Mascota> findByUsuarioId(Long usuarioId);
    List<Mascota> findByEspecieAndEstado(String especie, EstadoMascota estado);
}
