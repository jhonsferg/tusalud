package com.codesoftlabs.tusalud.app;

import com.codesoftlabs.tusalud.app.dto.request.DiagnosticoRequest;
import com.codesoftlabs.tusalud.app.entities.Cie10Catalogo;
import com.codesoftlabs.tusalud.app.entities.enums.SexoPaciente;
import com.codesoftlabs.tusalud.app.entities.enums.TipoDiagnostico;
import com.codesoftlabs.tusalud.app.repositories.Cie10CatalogoRepository;
import com.codesoftlabs.tusalud.app.services.cie10.Cie10Normalizador;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

public final class Cie10Pruebas {

    private Cie10Pruebas() {}

    public static List<DiagnosticoRequest> principal() {
        return List.of(new DiagnosticoRequest("I10", TipoDiagnostico.DEFINITIVO, true, null));
    }

    public static List<DiagnosticoRequest> principalYSecundario() {
        return List.of(
                new DiagnosticoRequest("J06.9", TipoDiagnostico.PRESUNTIVO, true, "Cuadro de tres días"),
                new DiagnosticoRequest("R50.9", TipoDiagnostico.PRESUNTIVO, false, null));
    }

    public static Cie10Catalogo entrada(
            String codigo, String descripcion, SexoPaciente sexo, Integer minima, Integer maxima) {
        return Cie10Catalogo.builder()
                .codigo(codigo)
                .descripcion(descripcion)
                .textoBusqueda(Cie10Normalizador.busqueda(codigo, descripcion))
                .capitulo("Capítulo de prueba")
                .grupo("Grupo de prueba")
                .sexo(sexo)
                .edadMinima(minima)
                .edadMaxima(maxima)
                .activo(true)
                .muestra(true)
                .actualizadoEn(LocalDateTime.now())
                .build();
    }

    @Component
    static class Semilla implements ApplicationRunner {

        private final Cie10CatalogoRepository repositorio;

        Semilla(Cie10CatalogoRepository repositorio) {
            this.repositorio = repositorio;
        }

        @Override
        public void run(ApplicationArguments args) {
            List<Cie10Catalogo> filas = new ArrayList<>();
            filas.add(entrada("I10", "Hipertensión esencial (primaria)", null, null, null));
            filas.add(entrada(
                    "E11.9",
                    "Diabetes mellitus no insulinodependiente, sin mención de complicación",
                    null,
                    null,
                    null));
            filas.add(entrada(
                    "J06.9",
                    "Infección aguda de las vías respiratorias superiores, no especificada",
                    null,
                    null,
                    null));
            filas.add(entrada("R50.9", "Fiebre, no especificada", null, null, null));
            filas.add(entrada("Z00.0", "Examen médico general", null, null, null));
            filas.add(entrada("A09", "Diarrea y gastroenteritis de presunto origen infeccioso", null, null, null));
            filas.add(entrada("N40", "Hiperplasia de la próstata", SexoPaciente.M, 40, null));
            filas.add(entrada("O80", "Parto único espontáneo", SexoPaciente.F, 10, 55));
            filas.add(entrada("Z00.1", "Control de salud de rutina del niño", null, 0, 17));
            filas.add(entrada(
                    "S83.4",
                    "Esguinces y torceduras que comprometen el ligamento lateral de la rodilla",
                    null,
                    null,
                    null));
            filas.add(entrada("I50.0", "Insuficiencia cardíaca congestiva", null, null, null));
            Cie10Catalogo inactivo = entrada("K29.7", "Gastritis, no especificada", null, null, null);
            inactivo.setActivo(false);
            filas.add(inactivo);
            for (Cie10Catalogo fila : filas) {
                if (repositorio.findByCodigo(fila.getCodigo()).isEmpty()) {
                    repositorio.save(fila);
                }
            }
        }
    }
}
