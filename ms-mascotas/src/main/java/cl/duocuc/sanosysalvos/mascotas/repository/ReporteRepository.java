package cl.duocuc.sanosysalvos.mascotas.repository;

import cl.duocuc.sanosysalvos.mascotas.model.Reporte;
import cl.duocuc.sanosysalvos.mascotas.model.TipoReporte;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReporteRepository extends JpaRepository<Reporte, Long> {
    List<Reporte> findByMascotaId(Long mascotaId);
    List<Reporte> findByUsuarioId(Long usuarioId);
    List<Reporte> findByTipo(TipoReporte tipo);
}
