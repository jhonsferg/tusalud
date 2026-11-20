package com.codesoftlabs.tusalud.app.services.pediatria;

import static org.assertj.core.api.Assertions.assertThat;

import com.codesoftlabs.tusalud.app.entities.enums.PediatriaEnums.EstadoDosis;
import com.codesoftlabs.tusalud.app.services.pediatria.CalculadoraEsquemaVacunacion.DosisAplicada;
import com.codesoftlabs.tusalud.app.services.pediatria.CalculadoraEsquemaVacunacion.DosisCalculada;
import com.codesoftlabs.tusalud.app.services.pediatria.CalculadoraEsquemaVacunacion.DosisEsquema;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CalculadoraEsquemaVacunacionTest {

    private static final UUID BCG = UUID.randomUUID();
    private static final UUID PENTA = UUID.randomUUID();
    private static final UUID ROTA = UUID.randomUUID();
    private static final LocalDate NACIMIENTO = LocalDate.of(2025, 1, 10);

    private static final List<DosisEsquema> ESQUEMA = List.of(
            new DosisEsquema(BCG, "BCG", "BCG", 1, "Única", 0, 0, 365, null),
            new DosisEsquema(PENTA, "PENTA", "Pentavalente", 1, "Primera", 42, 60, null, null),
            new DosisEsquema(PENTA, "PENTA", "Pentavalente", 2, "Segunda", 70, 120, null, 28),
            new DosisEsquema(PENTA, "PENTA", "Pentavalente", 3, "Tercera", 98, 180, null, 28),
            new DosisEsquema(ROTA, "ROTA", "Rotavirus", 1, "Primera", 42, 60, 105, null));

    private static DosisCalculada de(List<DosisCalculada> lista, UUID vacuna, int dosis) {
        return lista.stream()
                .filter(c -> c.dosis().vacunaId().equals(vacuna) && c.dosis().numeroDosis() == dosis)
                .findFirst()
                .orElseThrow();
    }

    @Test
    void unRecienNacidoTieneLaBcgComoProximaYElRestoPendiente() {
        List<DosisCalculada> r = CalculadoraEsquemaVacunacion.calcular(NACIMIENTO, NACIMIENTO, ESQUEMA, List.of());

        assertThat(de(r, BCG, 1).estado()).isEqualTo(EstadoDosis.PROXIMA);
        assertThat(de(r, PENTA, 1).estado()).isEqualTo(EstadoDosis.PENDIENTE);
        assertThat(de(r, PENTA, 2).estado()).isEqualTo(EstadoDosis.PENDIENTE);
        assertThat(CalculadoraEsquemaVacunacion.proxima(r).dosis().vacunaId()).isEqualTo(BCG);
    }

    @Test
    void laDosisSeVuelveProximaCuandoFaltaMenosDeUnaVentana() {
        LocalDate hoy = NACIMIENTO.plusDays(40);

        List<DosisCalculada> r = CalculadoraEsquemaVacunacion.calcular(
                NACIMIENTO, hoy, ESQUEMA, List.of(new DosisAplicada(BCG, 1, NACIMIENTO, UUID.randomUUID())));

        assertThat(de(r, BCG, 1).estado()).isEqualTo(EstadoDosis.APLICADA);
        assertThat(de(r, PENTA, 1).estado()).isEqualTo(EstadoDosis.PROXIMA);
        assertThat(de(r, PENTA, 1).fechaRecomendada()).isEqualTo(NACIMIENTO.plusDays(60));
    }

    @Test
    void sinAplicarPasadaLaToleranciaQuedaAtrasadaConLosDiasDeAtraso() {
        LocalDate hoy = NACIMIENTO.plusDays(60 + 45);

        List<DosisCalculada> r = CalculadoraEsquemaVacunacion.calcular(NACIMIENTO, hoy, ESQUEMA, List.of());

        DosisCalculada penta = de(r, PENTA, 1);
        assertThat(penta.estado()).isEqualTo(EstadoDosis.ATRASADA);
        assertThat(penta.atrasoDias()).isEqualTo(45);
        assertThat(de(r, PENTA, 2).estado()).isEqualTo(EstadoDosis.PENDIENTE);
    }

    @Test
    void dentroDeLaToleranciaNoSeConsideraAtrasada() {
        LocalDate hoy = NACIMIENTO.plusDays(60 + 30);

        List<DosisCalculada> r = CalculadoraEsquemaVacunacion.calcular(NACIMIENTO, hoy, ESQUEMA, List.of());

        assertThat(de(r, PENTA, 1).estado()).isEqualTo(EstadoDosis.PROXIMA);
    }

    @Test
    void laSegundaDosisRespetaElIntervaloMinimoDesdeLaPrimera() {
        LocalDate aplicada = NACIMIENTO.plusDays(100);
        LocalDate hoy = NACIMIENTO.plusDays(110);

        List<DosisCalculada> r = CalculadoraEsquemaVacunacion.calcular(
                NACIMIENTO, hoy, ESQUEMA, List.of(new DosisAplicada(PENTA, 1, aplicada, UUID.randomUUID())));

        DosisCalculada segunda = de(r, PENTA, 2);
        assertThat(segunda.fechaRecomendada()).isEqualTo(aplicada.plusDays(28));
        assertThat(segunda.fechaMinima()).isEqualTo(aplicada.plusDays(28));
        assertThat(segunda.estado()).isEqualTo(EstadoDosis.PROXIMA);
    }

    @Test
    void unaDosisConEdadMaximaSuperadaQuedaFueraDeEdad() {
        LocalDate hoy = NACIMIENTO.plusDays(200);

        List<DosisCalculada> r = CalculadoraEsquemaVacunacion.calcular(NACIMIENTO, hoy, ESQUEMA, List.of());

        assertThat(de(r, ROTA, 1).estado()).isEqualTo(EstadoDosis.FUERA_DE_EDAD);
        assertThat(de(r, ROTA, 1).fechaLimite()).isEqualTo(NACIMIENTO.plusDays(105));
    }

    @Test
    void laProximaDosisEsLaDeFechaRecomendadaMasAntigua() {
        LocalDate hoy = NACIMIENTO.plusDays(200);

        List<DosisCalculada> r = CalculadoraEsquemaVacunacion.calcular(
                NACIMIENTO, hoy, ESQUEMA, List.of(new DosisAplicada(BCG, 1, NACIMIENTO, UUID.randomUUID())));

        DosisCalculada proxima = CalculadoraEsquemaVacunacion.proxima(r);
        assertThat(proxima.dosis().vacunaId()).isEqualTo(PENTA);
        assertThat(proxima.dosis().numeroDosis()).isEqualTo(1);
        assertThat(proxima.estado()).isEqualTo(EstadoDosis.ATRASADA);
    }

    @Test
    void sinDosisPorHacerNoHayProxima() {
        List<DosisEsquema> solo = List.of(ESQUEMA.get(0));
        List<DosisCalculada> r = CalculadoraEsquemaVacunacion.calcular(
                NACIMIENTO, NACIMIENTO.plusDays(10), solo, List.of(new DosisAplicada(BCG, 1, NACIMIENTO, null)));

        assertThat(CalculadoraEsquemaVacunacion.proxima(r)).isNull();
    }

    @Test
    void calculaLaEdadEnDiasYEnMeses() {
        assertThat(CalculadoraEsquemaVacunacion.edadEnDias(NACIMIENTO, NACIMIENTO.plusDays(60)))
                .isEqualTo(60);
        assertThat(CalculadoraEsquemaVacunacion.edadEnMeses(NACIMIENTO, LocalDate.of(2026, 1, 10)))
                .isEqualTo(12);
    }
}
