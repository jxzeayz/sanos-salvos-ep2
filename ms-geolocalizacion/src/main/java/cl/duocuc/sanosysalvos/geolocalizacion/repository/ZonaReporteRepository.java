package cl.duocuc.sanosysalvos.geolocalizacion.repository;

import cl.duocuc.sanosysalvos.geolocalizacion.model.ZonaReporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ZonaReporteRepository extends JpaRepository<ZonaReporte, Long> {

    List<ZonaReporte> findByTipoReporte(String tipoReporte);

    @Query(value = """
        SELECT * FROM zonas_reporte
        WHERE ST_DWithin(
            CAST(ubicacion AS geography),
            CAST(ST_SetSRID(ST_MakePoint(:longitud, :latitud), 4326) AS geography),
            :radioMetros
        )
        """, nativeQuery = true)
    List<ZonaReporte> findByRadio(
            @Param("latitud") double latitud,
            @Param("longitud") double longitud,
            @Param("radioMetros") double radioMetros);

    @Query(value = """
        SELECT latitud, longitud, COUNT(*) as cantidad
        FROM zonas_reporte
        GROUP BY latitud, longitud
        ORDER BY cantidad DESC
        LIMIT 10
        """, nativeQuery = true)
    List<Object[]> findZonasCalientes();
}
