package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.request.PediatriaRequests.DosisRequest;
import com.codesoftlabs.tusalud.app.dto.request.PediatriaRequests.EsquemaDosisRequest;
import com.codesoftlabs.tusalud.app.dto.request.PediatriaRequests.EsquemaRequest;
import com.codesoftlabs.tusalud.app.dto.request.PediatriaRequests.MedicionRequest;
import com.codesoftlabs.tusalud.app.dto.response.DocumentosMedicosResponses.VerificacionPublicaResponse;
import com.codesoftlabs.tusalud.app.dto.response.PediatriaResponses.CarneResponse;
import com.codesoftlabs.tusalud.app.dto.response.PediatriaResponses.CrecimientoResponse;
import com.codesoftlabs.tusalud.app.dto.response.PediatriaResponses.RegistroVacunaResponse;
import com.codesoftlabs.tusalud.app.entities.EsquemaDosis;
import com.codesoftlabs.tusalud.app.entities.EsquemaVacunacion;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.ReferenciaCrecimiento;
import com.codesoftlabs.tusalud.app.entities.Vacuna;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.PediatriaEnums.EstadoDosis;
import com.codesoftlabs.tusalud.app.entities.enums.PediatriaEnums.EstadoRegistroVacunacion;
import com.codesoftlabs.tusalud.app.entities.enums.PediatriaEnums.IndicadorCrecimiento;
import com.codesoftlabs.tusalud.app.entities.enums.PediatriaEnums.NivelAlerta;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.SexoPaciente;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.repositories.EsquemaDosisRepository;
import com.codesoftlabs.tusalud.app.repositories.EsquemaVacunacionRepository;
import com.codesoftlabs.tusalud.app.repositories.PacienteRepository;
import com.codesoftlabs.tusalud.app.repositories.ReferenciaCrecimientoRepository;
import com.codesoftlabs.tusalud.app.repositories.VacunaRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.documentosmedicos.VerificacionPublicaService;
import com.codesoftlabs.tusalud.app.services.pediatria.CrecimientoService;
import com.codesoftlabs.tusalud.app.services.pediatria.VacunacionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(ServiceTestFixture.class)
class PediatriaServiceTest {

    @Autowired
    private VacunacionService vacunacion;

    @Autowired
    private CrecimientoService crecimiento;

    @Autowired
    private VerificacionPublicaService verificacion;

    @Autowired
    private VacunaRepository vacunas;

    @Autowired
    private EsquemaVacunacionRepository esquemas;

    @Autowired
    private EsquemaDosisRepository dosis;

    @Autowired
    private ReferenciaCrecimientoRepository referencias;

    @Autowired
    private PacienteRepository pacientes;

    @Autowired
    private ServiceTestFixture fixture;

    private Vacuna bcg;
    private Vacuna penta;
    private Medico medico;
    private Paciente bebe;
    private UsuarioPrincipal principalMedico;
    private UsuarioPrincipal principalBebe;
    private UsuarioPrincipal principalEnfermera;

    @BeforeEach
    void setUp() {
        bcg = vacunas.save(
                Vacuna.builder().codigo("BCG-T").nombre("BCG de prueba").build());
        penta = vacunas.save(Vacuna.builder()
                .codigo("PENTA-T")
                .nombre("Pentavalente de prueba")
                .build());
        EsquemaVacunacion esquema = esquemas.save(EsquemaVacunacion.builder()
                .nombre("Esquema de prueba")
                .vigenteDesde(LocalDate.of(2000, 1, 1))
                .build());
        dosis.saveAll(List.of(
                dosis(esquema, bcg, 1, "Única", 0, 0, 365, null),
                dosis(esquema, penta, 1, "Primera", 42, 60, null, null),
                dosis(esquema, penta, 2, "Segunda", 70, 120, null, 28)));
        for (int edad : new int[] {0, 12, 24, 60}) {
            referencias.save(referencia(IndicadorCrecimiento.PESO_EDAD, edad, 3.3464 + edad * 0.5));
        }
        medico = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        bebe = pacientes.save(Paciente.builder()
                .dni("8" + String.format("%07d", ServiceTestFixture.siguiente()))
                .historialClinicoNumero("PED-HC-" + ServiceTestFixture.siguiente())
                .fechaNacimiento(LocalDate.now().minusDays(100))
                .sexo(SexoPaciente.M)
                .usuario(fixture.usuario("bebe", RolNombre.ROLE_PACIENTE))
                .build());
        fixture.cita(medico, bebe, LocalDateTime.now().plusDays(2), EstadoCita.CONFIRMADA);
        principalMedico = fixture.principal(medico.getUsuario());
        principalBebe = fixture.principal(bebe.getUsuario());
        principalEnfermera = fixture.principal(fixture.usuario("enf", RolNombre.ROLE_ENFERMERIA));
    }

    private static EsquemaDosis dosis(
            EsquemaVacunacion esquema,
            Vacuna vacuna,
            int numero,
            String etiqueta,
            int minima,
            int recomendada,
            Integer maxima,
            Integer intervalo) {
        return EsquemaDosis.builder()
                .esquema(esquema)
                .vacuna(vacuna)
                .numeroDosis(numero)
                .etiqueta(etiqueta)
                .edadMinimaDias(minima)
                .edadRecomendadaDias(recomendada)
                .edadMaximaDias(maxima)
                .intervaloMinimoDias(intervalo)
                .build();
    }

    private static ReferenciaCrecimiento referencia(IndicadorCrecimiento indicador, int edad, double mediana) {
        return ReferenciaCrecimiento.builder()
                .indicador(indicador)
                .sexo(SexoPaciente.M)
                .edadMeses(edad)
                .l(new BigDecimal("-0.0506"))
                .m(BigDecimal.valueOf(mediana))
                .s(new BigDecimal("0.11316"))
                .fuente("Prueba")
                .build();
    }

    private DosisRequest aplicar(Vacuna vacuna, int numero, LocalDate fecha) {
        return new DosisRequest(bebe.getId(), vacuna.getId(), numero, fecha, "lote-01", null, null, null);
    }

    @Test
    void elMedicoConRelacionRegistraUnaDosisYApareceEnElCarne() {
        RegistroVacunaResponse registro = vacunacion.registrarDosis(
                principalMedico, aplicar(bcg, 1, LocalDate.now().minusDays(90)));

        assertThat(registro.lote()).isEqualTo("LOTE-01");
        assertThat(registro.establecimiento()).isEqualTo("TuSalud");
        assertThat(registro.responsableNombre()).isEqualTo(medico.getUsuario().getNombreCompleto());
        CarneResponse carne = vacunacion.carne(principalMedico, bebe.getId());
        assertThat(carne.aplicadas()).isEqualTo(1);
        assertThat(carne.dosis().stream()
                        .filter(d -> d.vacunaCodigo().equals("BCG-T"))
                        .findFirst()
                        .orElseThrow()
                        .estado())
                .isEqualTo(EstadoDosis.APLICADA);
        assertThat(carne.esquemaPorValidar()).isTrue();
    }

    @Test
    void calculaAtrasosYLaProximaDosisDelCarne() {
        CarneResponse carne = vacunacion.carne(principalMedico, bebe.getId());

        assertThat(carne.atrasadas()).isGreaterThanOrEqualTo(1);
        assertThat(carne.proximaDosis()).isNotNull();
        assertThat(carne.proximaDosis().estado()).isEqualTo(EstadoDosis.ATRASADA);
    }

    @Test
    void noPermiteRegistrarDosDosisIgualesNiSaltarseLaDosisAnterior() {
        vacunacion.registrarDosis(
                principalMedico, aplicar(penta, 1, LocalDate.now().minusDays(40)));

        assertThatThrownBy(() -> vacunacion.registrarDosis(
                        principalMedico, aplicar(penta, 1, LocalDate.now().minusDays(30))))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> vacunacion.registrarDosis(
                        principalMedico,
                        new DosisRequest(bebe.getId(), bcg.getId(), 2, LocalDate.now(), "L1", null, null, null)))
                .isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void exigeElIntervaloMinimoYLaEdadMinimaDeLaDosis() {
        vacunacion.registrarDosis(
                principalMedico, aplicar(penta, 1, LocalDate.now().minusDays(50)));

        assertThatThrownBy(() -> vacunacion.registrarDosis(
                        principalMedico, aplicar(penta, 2, LocalDate.now().minusDays(25))))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("intervalo");
        Paciente muyPequeno = pacientes.save(Paciente.builder()
                .dni("7" + String.format("%07d", ServiceTestFixture.siguiente()))
                .historialClinicoNumero("PED-HC-" + ServiceTestFixture.siguiente())
                .fechaNacimiento(LocalDate.now().minusDays(10))
                .sexo(SexoPaciente.F)
                .usuario(fixture.usuario("peq", RolNombre.ROLE_PACIENTE))
                .build());
        fixture.cita(medico, muyPequeno, LocalDateTime.now().plusDays(1), EstadoCita.CONFIRMADA);
        assertThatThrownBy(() -> vacunacion.registrarDosis(
                        principalMedico,
                        new DosisRequest(
                                muyPequeno.getId(), penta.getId(), 1, LocalDate.now(), "L1", null, null, null)))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("edad mínima");
    }

    @Test
    void anularUnaDosisLaSacaDelCarneYPermiteRegistrarlaDeNuevo() {
        RegistroVacunaResponse registro = vacunacion.registrarDosis(
                principalMedico, aplicar(bcg, 1, LocalDate.now().minusDays(90)));

        RegistroVacunaResponse anulado = vacunacion.anular(principalMedico, registro.id(), "Registro por error");

        assertThat(anulado.estado()).isEqualTo(EstadoRegistroVacunacion.ANULADO);
        assertThat(vacunacion.carne(principalMedico, bebe.getId()).aplicadas()).isZero();
        assertThatThrownBy(() -> vacunacion.anular(principalMedico, registro.id(), "Otra vez"))
                .isInstanceOf(ConflictException.class);
        assertThat(vacunacion.registrarDosis(
                        principalMedico, aplicar(bcg, 1, LocalDate.now().minusDays(80))))
                .isNotNull();
    }

    @Test
    void laEnfermeriaSoloAtiendeAPacientesConCitaOIngresoVigente() {
        assertThatThrownBy(() -> vacunacion.registrarDosis(
                        principalEnfermera, aplicar(bcg, 1, LocalDate.now().minusDays(90))))
                .isInstanceOf(ForbiddenOperationException.class);
        fixture.cita(medico, bebe, LocalDateTime.now().plusHours(2), EstadoCita.CONFIRMADA);

        assertThat(vacunacion.registrarDosis(
                        principalEnfermera, aplicar(bcg, 1, LocalDate.now().minusDays(90))))
                .isNotNull();
        assertThat(vacunacion.pacientes(principalEnfermera, "").stream().map(p -> p.id()))
                .contains(bebe.getId());
    }

    @Test
    void unMedicoSinRelacionNoVeNiRegistraNada() {
        UsuarioPrincipal ajeno = fixture.principal(
                fixture.medicoConHorarioLunes(new BigDecimal("80.00")).getUsuario());

        assertThatThrownBy(() -> vacunacion.carne(ajeno, bebe.getId())).isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> vacunacion.registrarDosis(
                        ajeno, aplicar(bcg, 1, LocalDate.now().minusDays(90))))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThat(vacunacion.pacientes(ajeno, "")).isEmpty();
    }

    @Test
    void elPacienteVeSoloSuCarneYNoPuedeRegistrarDosis() {
        Paciente otro = fixture.paciente();

        assertThat(vacunacion.carneMio(principalBebe).pacienteId()).isEqualTo(bebe.getId());
        assertThat(vacunacion.carne(principalBebe, bebe.getId())).isNotNull();
        assertThatThrownBy(() -> vacunacion.carne(principalBebe, otro.getId()))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> vacunacion.registrarDosis(
                        principalBebe, aplicar(bcg, 1, LocalDate.now().minusDays(90))))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void elPdfDelCarneSeVerificaPorSuCodigoSinExponerDatosDelPaciente() {
        vacunacion.registrarDosis(
                principalMedico, aplicar(bcg, 1, LocalDate.now().minusDays(90)));

        var pdf = vacunacion.pdf(principalMedico, bebe.getId());

        assertThat(pdf.contenido()).startsWith("%PDF".getBytes());
        assertThat(pdf.nombre()).startsWith("carne-vacunacion-");
        var codigos = jdbcCodigos();
        assertThat(codigos).hasSize(1);
        VerificacionPublicaResponse respuesta = verificacion.verificar("10.0.0.1", codigos.get(0));
        assertThat(respuesta.resultado()).isEqualTo("VALIDO");
        assertThat(respuesta.tipo()).isEqualTo("Carné de vacunación");
        assertThat(respuesta.toString()).doesNotContain(bebe.getDni());
    }

    @Test
    void creaUnEsquemaNuevoQueCierraLaVigenciaDelAnteriorYValidaSuConsistencia() {
        EsquemaDosisRequest bcgDosis = new EsquemaDosisRequest("BCG-T", 1, "Única", 0, 0, 365, null);
        EsquemaRequest request =
                new EsquemaRequest("Esquema 2030", LocalDate.of(2030, 1, 1), true, "Fuente", List.of(bcgDosis));
        var admin = fixture.principal(fixture.usuario("adm", RolNombre.ROLE_ADMIN));

        var nuevo = vacunacion.crearEsquema(admin, request);

        assertThat(nuevo.dosis()).hasSize(1);
        assertThat(vacunacion.listarEsquemas().get(1).vigenteHasta()).isEqualTo(LocalDate.of(2029, 12, 31));
        assertThatThrownBy(() -> vacunacion.crearEsquema(admin, request)).isInstanceOf(ConflictException.class);
        EsquemaRequest sinPrevia = new EsquemaRequest(
                "Roto",
                LocalDate.of(2031, 1, 1),
                true,
                null,
                List.of(new EsquemaDosisRequest("BCG-T", 2, "Segunda", 10, 20, null, null)));
        assertThatThrownBy(() -> vacunacion.crearEsquema(admin, sinPrevia)).isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void enviaUnSoloRecordatorioPorDosisProgramada() {
        int primeros = vacunacion.enviarRecordatorios(LocalDate.now());
        int segundos = vacunacion.enviarRecordatorios(LocalDate.now());

        assertThat(primeros).isGreaterThanOrEqualTo(1);
        assertThat(segundos).isZero();
    }

    @Test
    void registraMedicionesYEvaluaPercentilesYAlertas() {
        Paciente nino = pacientes.save(Paciente.builder()
                .dni("6" + String.format("%07d", ServiceTestFixture.siguiente()))
                .historialClinicoNumero("PED-HC-" + ServiceTestFixture.siguiente())
                .fechaNacimiento(LocalDate.now().minusMonths(12))
                .sexo(SexoPaciente.M)
                .usuario(fixture.usuario("nino", RolNombre.ROLE_PACIENTE))
                .build());
        fixture.cita(medico, nino, LocalDateTime.now().plusDays(1), EstadoCita.CONFIRMADA);

        crecimiento.registrar(
                principalMedico,
                new MedicionRequest(
                        nino.getId(), LocalDate.now().minusDays(1), new BigDecimal("9.35"), null, null, null, null));
        CrecimientoResponse normal = crecimiento.curvas(principalMedico, nino.getId());
        crecimiento.registrar(
                principalMedico,
                new MedicionRequest(nino.getId(), LocalDate.now(), new BigDecimal("6.50"), null, null, null, null));
        CrecimientoResponse bajo = crecimiento.curvas(principalMedico, nino.getId());

        assertThat(normal.aplica()).isTrue();
        assertThat(normal.referenciaPorValidar()).isTrue();
        var curvaPeso = normal.curvas().stream()
                .filter(c -> c.indicador() == IndicadorCrecimiento.PESO_EDAD)
                .findFirst()
                .orElseThrow();
        assertThat(curvaPeso.referencia()).isNotEmpty();
        assertThat(curvaPeso.puntos().get(0).nivel()).isEqualTo(NivelAlerta.NORMAL);
        var puntosBajo = bajo.curvas().stream()
                .filter(c -> c.indicador() == IndicadorCrecimiento.PESO_EDAD)
                .findFirst()
                .orElseThrow()
                .puntos();
        assertThat(puntosBajo.get(1).z()).isLessThan(-2.0);
        assertThat(puntosBajo.get(1).nivel()).isNotEqualTo(NivelAlerta.NORMAL);
        assertThat(bajo.alertas()).isNotEmpty();
    }

    @Test
    void rechazaMedicionesVaciasDeAdultosYDeFechasAnterioresAlNacimiento() {
        Paciente adulto = fixture.paciente();
        fixture.cita(medico, adulto, LocalDateTime.now().plusDays(1), EstadoCita.CONFIRMADA);

        assertThatThrownBy(() -> crecimiento.registrar(
                        principalMedico,
                        new MedicionRequest(bebe.getId(), LocalDate.now(), null, null, null, null, null)))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> crecimiento.registrar(
                        principalMedico,
                        new MedicionRequest(
                                adulto.getId(), LocalDate.now(), new BigDecimal("70"), null, null, null, null)))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> crecimiento.registrar(
                        principalMedico,
                        new MedicionRequest(
                                bebe.getId(),
                                LocalDate.now().minusDays(400),
                                new BigDecimal("5"),
                                null,
                                null,
                                null,
                                null)))
                .isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void anularUnaMedicionLaQuitaDeLasCurvasYElPacienteSoloVeLasSuyas() {
        var medicion = crecimiento.registrar(
                principalMedico,
                new MedicionRequest(bebe.getId(), LocalDate.now(), new BigDecimal("6.0"), null, null, null, null));

        assertThat(crecimiento.curvasMias(principalBebe).mediciones()).hasSize(1);
        crecimiento.anular(principalMedico, medicion.id(), "Error de digitación");
        assertThat(crecimiento.curvasMias(principalBebe).mediciones()).isEmpty();
        assertThatThrownBy(() -> crecimiento.anular(principalMedico, medicion.id(), "Otra vez"))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() ->
                        crecimiento.curvas(principalBebe, fixture.paciente().getId()))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    private List<String> jdbcCodigos() {
        return carnesRepo.findAll().stream().map(c -> c.getCodigoVerificacion()).toList();
    }

    @Autowired
    private com.codesoftlabs.tusalud.app.repositories.CarneVacunacionRepository carnesRepo;
}
