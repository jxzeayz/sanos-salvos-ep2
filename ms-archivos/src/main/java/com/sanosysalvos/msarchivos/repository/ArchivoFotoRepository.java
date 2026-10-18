package com.sanosysalvos.msarchivos.repository;

import com.sanosysalvos.msarchivos.model.ArchivoFoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ArchivoFotoRepository extends JpaRepository<ArchivoFoto, Long> {
    List<ArchivoFoto> findByMascotaIdOrderBySubidoEnDesc(Long mascotaId);
}