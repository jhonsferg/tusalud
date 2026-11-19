package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.response.Cie10ImportacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.Cie10Response;
import com.codesoftlabs.tusalud.app.dto.response.Cie10ResumenResponse;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.SexoPaciente;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.Cie10CatalogoRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.Cie10Service;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(ServiceTestFixture.class)
class Cie10ServiceTest {

    @Autowired
    private Cie10Service cie10Service;

    @Autowired
    private Cie10CatalogoRepository catalogoRepository;

    @Autowired
    private ServiceTestFixture fixture;

    private UsuarioPrincipal admin() {
        return fixture.principal(fixture.usuario("admin", RolNombre.ROLE_ADMIN));
    }

    private static MockMultipartFile csv(String contenido) {
        return new MockMultipartFile("archivo", "cie10.csv", "text/csv", contenido.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void buscaPorTextoSinDistinguirTildesNiMayusculas() {
        for (String consulta : List.of("hipertension", "HIPERTENSIÓN", "  Hipertensión  esencial ")) {
            assertThat(cie10Service.buscar(consulta, 10))
                    .extracting(Cie10Response::codigo)
                    .contains("I10");
        }
        assertThat(cie10Service.buscar("insuficiencia cardiaca", 10))
                .extracting(Cie10Response::codigo)
                .contains("I50.0");
    }

    @Test
    void buscaPorCodigoYPonePrimeroLaCoincidenciaExacta() {
        List<Cie10Response> resultado = cie10Service.buscar("i10", 10);
        assertThat(resultado).isNotEmpty();
        assertThat(resultado.get(0).codigo()).isEqualTo("I10");
        assertThat(cie10Service.buscar("e11.9", 10))
                .extracting(Cie10Response::codigo)
                .containsExactly("E11.9");
    }

    @Test
    void exigeAlMenosDosCaracteresYRespetaElLimite() {
        assertThat(cie10Service.buscar("h", 10)).isEmpty();
        assertThat(cie10Service.buscar("", 10)).isEmpty();
        assertThat(cie10Service.buscar("%", 10)).isEmpty();
        assertThat(cie10Service.buscar("a", 10)).isEmpty();
        assertThat(cie10Service.buscar("de", 1)).hasSizeLessThanOrEqualTo(1);
    }

    @Test
    void variasPalabrasDebenCoincidirTodas() {
        assertThat(cie10Service.buscar("diarrea gastroenteritis", 10))
                .extracting(Cie10Response::codigo)
                .contains("A09");
        assertThat(cie10Service.buscar("diarrea cardiaca", 10)).isEmpty();
    }

    @Test
    void noDevuelveLosDiagnosticosInactivos() {
        assertThat(cie10Service.buscar("gastritis", 10)).isEmpty();
        assertThatThrownBy(() -> cie10Service.exigirActivo("K29.7"))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("inactivo");
        assertThatThrownBy(() -> cie10Service.exigirActivo("Z99.9"))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("no existe");
        assertThatThrownBy(() -> cie10Service.obtener("Z99.9")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void exponeLasRestriccionesDeSexoYEdadDelCodigo() {
        Cie10Response prostata = cie10Service.obtener("n40");
        assertThat(prostata.sexo()).isEqualTo(SexoPaciente.M);
        assertThat(prostata.edadMinima()).isEqualTo(40);
        assertThat(cie10Service.obtener("O80").sexo()).isEqualTo(SexoPaciente.F);
    }

    @Test
    void laVistaPreviaCuentaAltasCambiosYErroresSinModificarElCatalogo() {
        long antes = catalogoRepository.count();
        String contenido = "codigo,descripcion,capitulo,grupo,sexo,edad_minima,edad_maxima\n"
                + "Z99.9,Diagnóstico nuevo de prueba,Capítulo nuevo,Grupo nuevo,F,10,50\n"
                + "z98.8,Otro diagnóstico nuevo,,,,,\n"
                + "I10,Hipertensión esencial (primaria),Capítulo de prueba,Grupo de prueba,,,\n"
                + "ZZ,Código inválido,,,,,\n"
                + "Z99.9,Repetido,,,,,\n"
                + "Z97.1,Sexo inválido,,,X,,\n"
                + "Z97.2,Edades invertidas,,,,60,10\n";

        Cie10ImportacionResponse vista = cie10Service.importar(csv(contenido), null, false, false, true, admin());

        assertThat(vista.aplicada()).isFalse();
        assertThat(vista.filas()).isEqualTo(7);
        assertThat(vista.validas()).isEqualTo(3);
        assertThat(vista.altas()).isEqualTo(2);
        assertThat(vista.totalErrores()).isEqualTo(4);
        assertThat(vista.errores())
                .extracting(Cie10ImportacionResponse.Error::fila)
                .contains(5, 6, 7, 8);
        assertThat(vista.muestra()).isNotEmpty();
        assertThat(catalogoRepository.count()).isEqualTo(antes);
    }

    @Test
    void laImportacionEsIdempotentePorCodigoYRegistraUnaVersion() {
        String contenido = "codigo;descripcion;capitulo;grupo;sexo;edad_minima;edad_maxima\n"
                + "Z99.9;Diagnóstico nuevo de prueba;Capítulo nuevo;Grupo nuevo;F;10;50\n"
                + "Z98.8;Otro diagnóstico nuevo;;;;;\n";

        Cie10ImportacionResponse primera =
                cie10Service.importar(csv(contenido), "Versión de prueba", true, false, false, admin());
        assertThat(primera.aplicada()).isTrue();
        assertThat(primera.altas()).isEqualTo(2);
        assertThat(primera.cambios()).isZero();
        assertThat(catalogoRepository.findByCodigo("Z99.9")).isPresent();
        assertThat(cie10Service.buscar("otro diagnostico nuevo", 5))
                .extracting(Cie10Response::codigo)
                .contains("Z98.8");
        assertThat(cie10Service.obtener("Z99.9").sexo()).isEqualTo(SexoPaciente.F);

        Cie10ImportacionResponse segunda =
                cie10Service.importar(csv(contenido), "Versión de prueba 2", true, false, false, admin());
        assertThat(segunda.altas()).isZero();
        assertThat(segunda.cambios()).isZero();
        assertThat(segunda.sinCambios()).isEqualTo(2);

        Cie10ResumenResponse resumen = cie10Service.resumen();
        assertThat(resumen.versionVigente().etiqueta()).isEqualTo("Versión de prueba 2");
        assertThat(resumen.versiones()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(resumen.versiones().stream().filter(v -> v.activa()).count()).isEqualTo(1);
    }

    @Test
    void importarCambiaLaDescripcionYPuedeDesactivarLosAusentes() {
        String contenido = "codigo,descripcion\nI10,Hipertensión arterial esencial\n";
        Cie10ImportacionResponse resultado =
                cie10Service.importar(csv(contenido), "Solo hipertensión", true, true, false, admin());
        assertThat(resultado.cambios()).isEqualTo(1);
        assertThat(resultado.desactivados()).isGreaterThan(0);
        assertThat(cie10Service.obtener("I10").descripcion()).isEqualTo("Hipertensión arterial esencial");
        assertThat(cie10Service.buscar("arterial esencial", 5))
                .extracting(Cie10Response::codigo)
                .contains("I10");
        assertThat(cie10Service.buscar("diarrea", 5)).isEmpty();
        assertThat(cie10Service.obtener("I10").muestra()).isFalse();
    }

    @Test
    void unArchivoConErroresNoSeAplicaSalvoQueSeOmitan() {
        String contenido = "codigo,descripcion\nZ99.9,Válido\nMAL,Inválido\n";
        assertThatThrownBy(() -> cie10Service.importar(csv(contenido), "Con errores", true, false, false, admin()))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("errores");
        Cie10ImportacionResponse parcial = cie10Service.importar(csv(contenido), "Parcial", true, false, true, admin());
        assertThat(parcial.altas()).isEqualTo(1);
        assertThat(parcial.totalErrores()).isEqualTo(1);
    }

    @Test
    void rechazaArchivosVaciosSinCabeceraOSinEtiqueta() {
        assertThatThrownBy(() -> cie10Service.importar(csv(""), "Vacío", true, false, false, admin()))
                .isInstanceOf(DatosInvalidosException.class);
        assertThat(cie10Service
                        .importar(csv("nombre,otro\nA,B\n"), null, false, false, true, admin())
                        .totalErrores())
                .isPositive();
        assertThatThrownBy(() -> cie10Service.importar(
                        csv("codigo,descripcion\nZ99.9,Válido\n"), " ", true, false, false, admin()))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("etiqueta");
    }

    @Test
    void laPlantillaTrAeLaCabeceraEsperada() {
        String plantilla = new String(cie10Service.plantillaCsv(), StandardCharsets.UTF_8);
        assertThat(plantilla)
                .contains("codigo,descripcion,capitulo,grupo,sexo,edad_minima,edad_maxima")
                .contains("I10");
    }
}
