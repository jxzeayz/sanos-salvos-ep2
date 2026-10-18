package com.sanosysalvos.msarchivos.test.service;

import com.sanosysalvos.msarchivos.model.ArchivoFoto;
import com.sanosysalvos.msarchivos.repository.ArchivoFotoRepository;
import com.sanosysalvos.msarchivos.service.ArchivoFotoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArchivoFotoServiceTest {

    @Mock
    private ArchivoFotoRepository repository;

    @InjectMocks
    private ArchivoFotoService service;

    @Test
    void testListarArchivosPorMascota() {
        ArchivoFoto archivo1 = new ArchivoFoto(1L, "foto1.jpg", "http://url1", "image/jpeg", 1024L, 1L, LocalDateTime.now());
        ArchivoFoto archivo2 = new ArchivoFoto(2L, "foto2.jpg", "http://url2", "image/jpeg", 2048L, 1L, LocalDateTime.now());
        when(repository.findByMascotaIdOrderBySubidoEnDesc(1L)).thenReturn(Arrays.asList(archivo1, archivo2));

        List<ArchivoFoto> result = service.listarArchivosPorMascota(1L);

        assertEquals(2, result.size());
        verify(repository).findByMascotaIdOrderBySubidoEnDesc(1L);
    }
}