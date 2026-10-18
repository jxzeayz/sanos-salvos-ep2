package cl.duocuc.sanosysalvos.mascotas.service;

import cl.duocuc.sanosysalvos.mascotas.dto.MascotaRequest;
import cl.duocuc.sanosysalvos.mascotas.event.MascotaEventPublisher;
import cl.duocuc.sanosysalvos.mascotas.model.EstadoMascota;
import cl.duocuc.sanosysalvos.mascotas.model.Mascota;
import cl.duocuc.sanosysalvos.mascotas.repository.MascotaRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MascotaService {

    private final MascotaRepository mascotaRepository;
    private final MascotaEventPublisher eventPublisher;

    @Transactional
    public Mascota registrar(MascotaRequest req) {
        Mascota mascota = Mascota.builder()
                .nombre(req.getNombre())
                .especie(req.getEspecie())
                .raza(req.getRaza())
                .color(req.getColor())
                .descripcion(req.getDescripcion())
                .tamano(req.getTamano())
                .estado(req.getEstado())
                .latitud(req.getLatitud())
                .longitud(req.getLongitud())
                .usuarioId(req.getUsuarioId())
                .build();

        mascota = mascotaRepository.save(mascota);
        publicarEvento(mascota);
        return mascota;
    }

    @Transactional
    public Mascota actualizar(Long id, MascotaRequest req) {
        Mascota mascota = obtenerPorId(id);
        if (req.getUsuarioId() != null && !mascota.getUsuarioId().equals(req.getUsuarioId())) {
            throw new IllegalArgumentException("No tienes permiso para editar esta mascota");
        }
        mascota.setNombre(req.getNombre());
        mascota.setEspecie(req.getEspecie());
        mascota.setRaza(req.getRaza());
        mascota.setColor(req.getColor());
        mascota.setDescripcion(req.getDescripcion());
        mascota.setTamano(req.getTamano());
        mascota.setEstado(req.getEstado());
        mascota.setLatitud(req.getLatitud());
        mascota.setLongitud(req.getLongitud());
        mascota = mascotaRepository.save(mascota);
        publicarEventoActualizacion(mascota);
        return mascota;
    }

    @Transactional
    public void eliminar(Long id, Long usuarioId) {
        Mascota mascota = obtenerPorId(id);
        if (!mascota.getUsuarioId().equals(usuarioId)) {
            throw new IllegalArgumentException("No tienes permiso para eliminar esta mascota");
        }
        publicarEventoEliminacion(mascota);
        mascotaRepository.delete(mascota);
    }

    @CircuitBreaker(name = "matching-service", fallbackMethod = "publishFallback")
    public void publicarEvento(Mascota mascota) {
        eventPublisher.publishMascotaRegistrada(mascota);
    }

    @CircuitBreaker(name = "matching-service", fallbackMethod = "publishFallback")
    public void publicarEventoActualizacion(Mascota mascota) {
        eventPublisher.publishMascotaActualizada(mascota);
    }

    @CircuitBreaker(name = "matching-service", fallbackMethod = "publishFallback")
    public void publicarEventoEliminacion(Mascota mascota) {
        eventPublisher.publishMascotaEliminada(mascota);
    }

    private void publishFallback(Mascota mascota, Exception ex) {
        log.warn("Circuit breaker activo: no se pudo publicar evento para mascota {}. Error: {}", mascota.getId(), ex.getMessage());
    }

    public List<Mascota> listarTodas() {
        return mascotaRepository.findAll();
    }

    public List<Mascota> listarPorEstado(EstadoMascota estado) {
        return mascotaRepository.findByEstado(estado);
    }

    public Mascota obtenerPorId(Long id) {
        return mascotaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mascota no encontrada con id: " + id));
    }

    @Transactional
    public Mascota actualizarEstado(Long id, EstadoMascota nuevoEstado, Long usuarioId) {
        Mascota mascota = obtenerPorId(id);
        if (usuarioId != null && !mascota.getUsuarioId().equals(usuarioId)) {
            throw new IllegalArgumentException("No tienes permiso para cambiar el estado de esta mascota");
        }
        mascota.setEstado(nuevoEstado);
        mascota = mascotaRepository.save(mascota);
        eventPublisher.publishMascotaEstadoActualizado(mascota);
        return mascota;
    }

    public List<Mascota> listarPorUsuario(Long usuarioId) {
        return mascotaRepository.findByUsuarioId(usuarioId);
    }
}
