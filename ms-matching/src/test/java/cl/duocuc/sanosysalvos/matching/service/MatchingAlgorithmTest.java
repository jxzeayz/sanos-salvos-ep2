package cl.duocuc.sanosysalvos.matching.service;

import cl.duocuc.sanosysalvos.matching.model.MascotaSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class MatchingAlgorithmTest {

    private MatchingAlgorithm algorithm;

    @BeforeEach
    void setUp() {
        algorithm = new MatchingAlgorithm();
    }

    private MascotaSnapshot snapshot(String especie, String raza, String color, String tamano,
                                      Double lat, Double lon, LocalDateTime fecha) {
        return MascotaSnapshot.builder()
                .especie(especie).raza(raza).color(color).tamano(tamano)
                .latitud(lat).longitud(lon).fechaReporte(fecha)
                .build();
    }

    @Test
    void calcularScore_todasCaracteristicasCoinciden_retornaScoreAlto() {
        LocalDateTime hoy = LocalDateTime.now();
        MascotaSnapshot a = snapshot("PERRO", "Labrador", "dorado", "GRANDE", -33.45, -70.64, hoy);
        MascotaSnapshot b = snapshot("PERRO", "Labrador", "dorado", "GRANDE", -33.45, -70.64, hoy);

        double score = algorithm.calcularScore(a, b);

        assertThat(score).isGreaterThanOrEqualTo(0.90);
        assertThat(score).isLessThanOrEqualTo(1.0);
    }

    @Test
    void calcularScore_especieDiferente_retornaCero() {
        MascotaSnapshot perro = snapshot("PERRO", "Labrador", "dorado", "GRANDE", -33.45, -70.64, null);
        MascotaSnapshot gato  = snapshot("GATO",  "Siamés",  "blanco", "PEQUEÑO", -33.45, -70.64, null);

        assertThat(algorithm.calcularScore(perro, gato)).isEqualTo(0.0);
    }

    @Test
    void calcularScore_especieNula_retornaCero() {
        MascotaSnapshot a = snapshot(null,   "Labrador", "dorado", "GRANDE", null, null, null);
        MascotaSnapshot b = snapshot("PERRO","Labrador", "dorado", "GRANDE", null, null, null);

        assertThat(algorithm.calcularScore(a, b)).isEqualTo(0.0);
    }

    @Test
    void calcularScore_mismEspecieDiferentesAtributos_retornaScoreMedio() {
        MascotaSnapshot a = snapshot("PERRO", "Labrador", "dorado", "GRANDE",  -33.45, -70.64, null);
        MascotaSnapshot b = snapshot("PERRO", "Poodle",   "negro",  "PEQUEÑO", -33.46, -70.65, null);

        double score = algorithm.calcularScore(a, b);

        assertThat(score).isBetween(0.30, 0.80);
    }

    @Test
    void calcularScore_razaNula_usaValorParcial() {
        MascotaSnapshot a = snapshot("PERRO", null, "dorado", "GRANDE", null, null, null);
        MascotaSnapshot b = snapshot("PERRO", null, "dorado", "GRANDE", null, null, null);

        double score = algorithm.calcularScore(a, b);

        // Especie (0.30) + raza nula (0.5*0.25) + color igual (0.20) + tamaño igual (0.10)
        assertThat(score).isGreaterThan(0.50);
    }

    @Test
    void calcularScore_colorParecido_retornaScoreParcial() {
        MascotaSnapshot a = snapshot("PERRO", "Labrador", "dorado oscuro", "GRANDE", null, null, null);
        MascotaSnapshot b = snapshot("PERRO", "Labrador", "dorado",        "GRANDE", null, null, null);

        double score = algorithm.calcularScore(a, b);

        // "dorado oscuro" contiene "dorado" → scoreColor = 0.6
        assertThat(score).isGreaterThan(0.50);
    }

    @Test
    void calcularScore_ubicacionLejos_noContribuyeAlScore() {
        LocalDateTime hoy = LocalDateTime.now();
        // Santiago vs. Valparaíso: más de 10 km
        MascotaSnapshot a = snapshot("PERRO", "Labrador", "dorado", "GRANDE", -33.45, -70.64, hoy);
        MascotaSnapshot b = snapshot("PERRO", "Labrador", "dorado", "GRANDE", -33.05, -71.62, hoy);

        MascotaSnapshot c = snapshot("PERRO", "Labrador", "dorado", "GRANDE", null, null, hoy);

        double scoreLejos     = algorithm.calcularScore(a, b);
        double scoreSinCoords = algorithm.calcularScore(a, c);

        // La ubicación lejana no aporta nada (igual que sin coordenadas)
        assertThat(scoreLejos).isLessThanOrEqualTo(scoreSinCoords + 0.001);
    }

    @Test
    void calcularScore_fechaMuyLejana_noContribuyeAlScore() {
        MascotaSnapshot a = snapshot("PERRO", "Labrador", "dorado", "GRANDE", null, null,
                LocalDateTime.now());
        MascotaSnapshot b = snapshot("PERRO", "Labrador", "dorado", "GRANDE", null, null,
                LocalDateTime.now().minusDays(60));

        MascotaSnapshot c = snapshot("PERRO", "Labrador", "dorado", "GRANDE", null, null, null);

        double scoreFechaLejana = algorithm.calcularScore(a, b);
        double scoreSinFecha    = algorithm.calcularScore(a, c);

        // Fecha más de 30 días → aporte 0; igual a no tener fecha
        assertThat(scoreFechaLejana).isLessThanOrEqualTo(scoreSinFecha + 0.001);
    }
}
