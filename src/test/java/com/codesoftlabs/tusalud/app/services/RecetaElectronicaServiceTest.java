package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.request.RecetaRequests.AlertasRequest;
import com.codesoftlabs.tusalud.app.dto.request.RecetaRequests.EmitirRecetaRequest;
import com.codesoftlabs.tusalud.app.dto.request.RecetaRequests.InteraccionRequest;
import com.codesoftlabs.tusalud.app.dto.request.RecetaRequests.ItemEmision;
import com.codesoftlabs.tusalud.app.dto.request.RecetaRequests.ItemPrescripcion;
import com.codesoftlabs.tusalud.app.dto.request.RecetaRequests.JustificacionAlerta;
import com.codesoftlabs.tusalud.app.dto.request.RecetaRequests.MedicamentoRequest;
import com.codesoftlabs.tusalud.app.dto.request.RecetaRequests.PrincipioActivoRequest;
import com.codesoftlabs.tusalud.app.dto.response.RecetaResponses.AlertaResponse;
import com.codesoftlabs.tusalud.app.dto.response.RecetaResponses.AlertasResponse;
import com.codesoftlabs.tusalud.app.dto.response.RecetaResponses.ImportacionCatalogoResponse;
import com.codesoftlabs.tusalud.app.dto.response.RecetaResponses.RecetaResponse;
import com.codesoftlabs.tusalud.app.entities.Atencion;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.RecetaElectronica;
import com.codesoftlabs.tusalud.app.entities.RecetaItem;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.RecetaEnums.CategoriaControl;
import com.codesoftlabs.tusalud.app.entities.enums.RecetaEnums.CategoriaEmbarazo;
import com.codesoftlabs.tusalud.app.entities.enums.RecetaEnums.CompatibilidadLactancia;
import com.codesoftlabs.tusalud.app.entities.enums.RecetaEnums.EstadoRecetaElectronica;
import com.codesoftlabs.tusalud.app.entities.enums.RecetaEnums.SeveridadAlerta;
import com.codesoftlabs.tusalud.app.entities.enums.RecetaEnums.SeveridadInteraccion;
import com.codesoftlabs.tusalud.app.entities.enums.RecetaEnums.TipoAlertaReceta;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.repositories.AtencionRepository;
import com.codesoftlabs.tusalud.app.repositories.PacienteRepository;
import com.codesoftlabs.tusalud.app.repositories.RecetaElectronicaRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.receta.CatalogoMedicamentosService;
import com.codesoftlabs.tusalud.app.services.receta.RecetaDispensaciones;
import com.codesoftlabs.tusalud.app.services.receta.RecetaElectronicaService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
class RecetaElectronicaServiceTest {

    @Autowired
    private RecetaElectronicaService recetas;

    @Autowired
    private CatalogoMedicamentosService catalogo;

    @Autowired
    private RecetaDispensaciones dispensaciones;

    @Autowired
    private RecetaElectronicaRepository recetaRepository;

    @Autowired
    private AtencionRepository atencionRepository;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private ServiceTestFixture fixture;

    private Medico medico;
    private UsuarioPrincipal principalMedico;
    private Paciente paciente;
    private UsuarioPrincipal principalPaciente;
    private Atencion atencion;

    private UUID amoxicilina;
    private UUID cefalexina;
    private UUID ibuprofeno;
    private UUID warfarina;
    private UUID paracetamol;
    private UUID paracetamolGotas;
    private UUID diazepam;
    private UUID atorvastatina;
    private int sufijo;

    @BeforeEach
    void setUp() {
        sufijo = ServiceTestFixture.siguiente();
        medico = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        principalMedico = fixture.principal(medico.getUsuario());
        paciente = fixture.paciente();
        principalPaciente = fixture.principal(paciente.getUsuario());
        atencion = atencionConReceta(paciente, 5);

        amoxicilina = medicamento(
                "Amoxicilina",
                "Antibióticos betalactámicos",
                "PENICILINAS",
                "penicilina,amoxicilina,penicilinas",
                "3000",
                "90",
                CategoriaEmbarazo.B,
                CompatibilidadLactancia.COMPATIBLE,
                "500 mg",
                CategoriaControl.ANTIBIOTICO);
        cefalexina = medicamento(
                "Cefalexina",
                "Antibióticos betalactámicos",
                "CEFALOSPORINAS",
                "cefalosporinas,cefalexina",
                "4000",
                "100",
                CategoriaEmbarazo.B,
                CompatibilidadLactancia.COMPATIBLE,
                "500 mg",
                CategoriaControl.ANTIBIOTICO);
        ibuprofeno = medicamento(
                "Ibuprofeno",
                "Antiinflamatorios no esteroideos",
                "AINE",
                "ibuprofeno,antiinflamatorios,aine",
                "2400",
                "40",
                CategoriaEmbarazo.C,
                CompatibilidadLactancia.COMPATIBLE,
                "400 mg",
                CategoriaControl.NINGUNA);
        warfarina = medicamento(
                "Warfarina",
                "Anticoagulantes",
                null,
                "warfarina",
                "10",
                null,
                CategoriaEmbarazo.X,
                CompatibilidadLactancia.COMPATIBLE,
                "5 mg",
                CategoriaControl.NINGUNA);
        paracetamol = medicamento(
                "Paracetamol",
                "Analgésicos y antipiréticos",
                null,
                "paracetamol",
                "4000",
                "75",
                CategoriaEmbarazo.B,
                CompatibilidadLactancia.COMPATIBLE,
                "500 mg",
                CategoriaControl.NINGUNA);
        diazepam = medicamento(
                "Diazepam",
                "Ansiolíticos y sedantes",
                "BENZODIAZEPINAS",
                "benzodiazepinas,diazepam",
                "40",
                null,
                CategoriaEmbarazo.D,
                CompatibilidadLactancia.PRECAUCION,
                "10 mg",
                CategoriaControl.PSICOTROPICO);
        atorvastatina = medicamento(
                "Atorvastatina",
                "Hipolipemiantes",
                null,
                "atorvastatina,estatinas",
                "80",
                null,
                CategoriaEmbarazo.X,
                CompatibilidadLactancia.CONTRAINDICADO,
                "20 mg",
                CategoriaControl.NINGUNA);
        paracetamolGotas = catalogo.crearMedicamento(new MedicamentoRequest(
                        idPrincipio("Paracetamol"),
                        null,
                        "Gotas",
                        "100 mg/ml",
                        "Oral",
                        null,
                        CategoriaControl.NINGUNA,
                        false,
                        null,
                        null,
                        true,
                        true))
                .id();
    }

    private UUID idPrincipio(String dci) {
        return catalogo.listarPrincipios().stream()
                .filter(p -> p.dci().equals(dci + " " + sufijo))
                .findFirst()
                .orElseThrow()
                .id();
    }

    private UUID medicamento(
            String dci,
            String clase,
            String grupo,
            String alias,
            String maxAdulto,
            String maxPed,
            CategoriaEmbarazo embarazo,
            CompatibilidadLactancia lactancia,
            String concentracion,
            CategoriaControl control) {
        UUID principio = catalogo.crearPrincipio(new PrincipioActivoRequest(
                        dci + " " + sufijo,
                        clase,
                        grupo,
                        alias,
                        new BigDecimal(maxAdulto),
                        maxPed == null ? null : new BigDecimal(maxPed),
                        embarazo,
                        lactancia,
                        null))
                .id();
        return catalogo.crearMedicamento(new MedicamentoRequest(
                        principio, null, "Tableta", concentracion, "Oral", null, control, true, null, null, true, true))
                .id();
    }

    private Atencion atencionConReceta(Paciente destino, int lineas) {
        var cita = fixture.cita(medico, destino, LocalDateTime.now().minusHours(1), EstadoCita.ATENDIDA);
        List<RecetaItem> receta = new ArrayList<>();
        for (int i = 0; i < lineas; i++) {
            receta.add(RecetaItem.builder()
                    .medicamento("Línea " + i)
                    .dosis("1 tableta")
                    .frecuencia("Cada 8 horas")
                    .duracion("5 días")
                    .build());
        }
        return atencionRepository.save(Atencion.builder()
                .cita(cita)
                .paciente(destino)
                .medico(medico)
                .fecha(LocalDateTime.now())
                .diagnostico("Diagnóstico")
                .receta(receta)
                .build());
    }

    private AlertasResponse alertas(UUID... medicamentos) {
        List<ItemPrescripcion> items = new ArrayList<>();
        for (UUID m : medicamentos) {
            items.add(new ItemPrescripcion(m, null, null));
        }
        return recetas.evaluarAlertas(principalMedico, new AlertasRequest(paciente.getId(), items, false, false));
    }

    private ItemEmision item(int posicion, UUID medicamento, int cantidad) {
        return new ItemEmision(posicion, medicamento, cantidad, null, null, null);
    }

    private EmitirRecetaRequest emision(List<ItemEmision> items, List<JustificacionAlerta> justificaciones) {
        return new EmitirRecetaRequest(
                atencion.getId(), null, "Tomar con agua", null, null, null, items, justificaciones);
    }

    private boolean hay(AlertasResponse r, TipoAlertaReceta tipo, SeveridadAlerta severidad) {
        return r.alertas().stream().anyMatch(a -> a.tipo() == tipo && a.severidad() == severidad);
    }

    @Test
    void laAlergiaDirectaALaPenicilinaContraindicaLaAmoxicilina() {
        paciente.setAlergias("Penicilina");
        pacienteRepository.save(paciente);

        AlertasResponse r = alertas(amoxicilina);

        assertThat(hay(r, TipoAlertaReceta.ALERGIA, SeveridadAlerta.CONTRAINDICADA))
                .isTrue();
        assertThat(r.hayContraindicadas()).isTrue();
        assertThat(r.pendientesDeJustificar()).isEqualTo(1);
    }

    @Test
    void laReaccionCruzadaDePenicilinaConCefalosporinaEsGrave() {
        paciente.setAlergias("Penicilina");
        pacienteRepository.save(paciente);

        AlertasResponse r = alertas(cefalexina);

        assertThat(hay(r, TipoAlertaReceta.ALERGIA, SeveridadAlerta.GRAVE)).isTrue();
        assertThat(hay(r, TipoAlertaReceta.ALERGIA, SeveridadAlerta.CONTRAINDICADA))
                .isFalse();
    }

    @Test
    void unAntiinflamatorioNoSeAlertaPorUnaAlergiaNoRelacionada() {
        paciente.setAlergias("Polen, mariscos");
        pacienteRepository.save(paciente);

        assertThat(alertas(ibuprofeno, paracetamol).alertas()).noneMatch(a -> a.tipo() == TipoAlertaReceta.ALERGIA);
    }

    @Test
    void detectaLaInteraccionGraveEntreWarfarinaEIbuprofenoSinImportarElOrden() {
        UUID principioWarfarina = idPrincipio("Warfarina");
        UUID principioIbuprofeno = idPrincipio("Ibuprofeno");
        catalogo.crearInteraccion(new InteraccionRequest(
                principioIbuprofeno,
                principioWarfarina,
                SeveridadInteraccion.GRAVE,
                "Mayor riesgo de sangrado.",
                "Evitar."));

        AlertasResponse r = alertas(warfarina, ibuprofeno);

        AlertaResponse interaccion = r.alertas().stream()
                .filter(a -> a.tipo() == TipoAlertaReceta.INTERACCION)
                .findFirst()
                .orElseThrow();
        assertThat(interaccion.severidad()).isEqualTo(SeveridadAlerta.GRAVE);
        assertThat(interaccion.requiereJustificacion()).isTrue();
        assertThat(interaccion.minimoJustificacion()).isEqualTo(10);
    }

    @Test
    void laInteraccionLeveEsSoloInformativa() {
        catalogo.crearInteraccion(new InteraccionRequest(
                idPrincipio("Paracetamol"), idPrincipio("Warfarina"), SeveridadInteraccion.LEVE, "Efecto leve.", null));

        AlertasResponse r = alertas(paracetamol, warfarina);

        assertThat(hay(r, TipoAlertaReceta.INTERACCION, SeveridadAlerta.INFORMATIVA))
                .isTrue();
        assertThat(r.pendientesDeJustificar()).isZero();
    }

    @Test
    void alertaLaDosisDiariaQueSuperaElMaximoDelAdulto() {
        AlertasResponse r = recetas.evaluarAlertas(
                principalMedico,
                new AlertasRequest(
                        paciente.getId(),
                        List.of(new ItemPrescripcion(paracetamol, new BigDecimal("1000"), 5)),
                        false,
                        false));

        assertThat(hay(r, TipoAlertaReceta.DOSIS_MAXIMA, SeveridadAlerta.GRAVE)).isTrue();
    }

    @Test
    void laDosisDentroDelMaximoNoGeneraAlerta() {
        AlertasResponse r = recetas.evaluarAlertas(
                principalMedico,
                new AlertasRequest(
                        paciente.getId(),
                        List.of(new ItemPrescripcion(paracetamol, new BigDecimal("500"), 3)),
                        false,
                        false));

        assertThat(r.alertas()).noneMatch(a -> a.tipo() == TipoAlertaReceta.DOSIS_MAXIMA);
    }

    @Test
    void alertaLaDuplicidadDelMismoPrincipioActivo() {
        AlertasResponse r = alertas(paracetamol, paracetamolGotas);

        assertThat(hay(r, TipoAlertaReceta.DUPLICIDAD, SeveridadAlerta.GRAVE)).isTrue();
    }

    @Test
    void elEmbarazoMarcadoContraindicaLasCategoriasXYAlertaLaD() {
        AlertasResponse r = recetas.evaluarAlertas(
                principalMedico,
                new AlertasRequest(
                        paciente.getId(),
                        List.of(
                                new ItemPrescripcion(atorvastatina, null, null),
                                new ItemPrescripcion(diazepam, null, null)),
                        true,
                        false));

        assertThat(hay(r, TipoAlertaReceta.EMBARAZO, SeveridadAlerta.CONTRAINDICADA))
                .isTrue();
        assertThat(hay(r, TipoAlertaReceta.EMBARAZO, SeveridadAlerta.GRAVE)).isTrue();
    }

    @Test
    void laLactanciaContraindicadaEsUnaAlertaGrave() {
        AlertasResponse r = recetas.evaluarAlertas(
                principalMedico,
                new AlertasRequest(
                        paciente.getId(), List.of(new ItemPrescripcion(atorvastatina, null, null)), false, true));

        assertThat(hay(r, TipoAlertaReceta.LACTANCIA, SeveridadAlerta.GRAVE)).isTrue();
    }

    @Test
    void emiteUnaRecetaNumeradaYFirmadaCuyaFirmaSeVerifica() {
        RecetaResponse r = recetas.emitir(principalMedico, emision(List.of(item(0, paracetamol, 20)), null));

        assertThat(r.numero()).matches("RX-\\d{4}-\\d{6}");
        assertThat(r.estado()).isEqualTo(EstadoRecetaElectronica.EMITIDA);
        assertThat(r.firmaValida()).isTrue();
        assertThat(r.certificadoDePrueba()).isTrue();
        assertThat(r.codigoVerificacion()).matches("^[A-HJ-NP-Z2-9]{4}(-[A-HJ-NP-Z2-9]{4}){3}$");
        assertThat(r.vigenteHasta()).isAfter(r.emitidaEn());
        assertThat(r.items()).hasSize(1);
        assertThat(r.items().get(0).dosis()).isEqualTo("1 tableta");
        assertThat(recetas.verificarFirma(principalMedico, r.id()).firmaValida())
                .isTrue();
    }

    @Test
    void laNumeracionEsCorrelativaYNoSeRepite() {
        RecetaResponse primera = recetas.emitir(principalMedico, emision(List.of(item(0, paracetamol, 10)), null));
        Atencion otra = atencionConReceta(paciente, 1);
        RecetaResponse segunda = recetas.emitir(
                principalMedico,
                new EmitirRecetaRequest(
                        otra.getId(), null, null, null, null, null, List.of(item(0, paracetamol, 10)), null));

        int a = Integer.parseInt(primera.numero().substring(primera.numero().lastIndexOf('-') + 1));
        int b = Integer.parseInt(segunda.numero().substring(segunda.numero().lastIndexOf('-') + 1));
        assertThat(b).isEqualTo(a + 1);
    }

    @Test
    void unaAlertaGraveExigeJustificacionYQuedaRegistrada() {
        UUID principioWarfarina = idPrincipio("Warfarina");
        UUID principioIbuprofeno = idPrincipio("Ibuprofeno");
        catalogo.crearInteraccion(new InteraccionRequest(
                principioIbuprofeno,
                principioWarfarina,
                SeveridadInteraccion.GRAVE,
                "Mayor riesgo de sangrado.",
                "Evitar."));
        List<ItemEmision> items = List.of(item(0, warfarina, 10), item(1, ibuprofeno, 10));

        assertThatThrownBy(() -> recetas.emitir(principalMedico, emision(items, null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Faltan justificaciones");

        String clave = alertas(warfarina, ibuprofeno).alertas().stream()
                .filter(a -> a.tipo() == TipoAlertaReceta.INTERACCION)
                .findFirst()
                .orElseThrow()
                .clave();

        assertThatThrownBy(() -> recetas.emitir(
                        principalMedico, emision(items, List.of(new JustificacionAlerta(clave, "corto")))))
                .isInstanceOf(ConflictException.class);

        RecetaResponse r = recetas.emitir(
                principalMedico,
                emision(
                        items,
                        List.of(new JustificacionAlerta(clave, "Paciente con prótesis valvular, se vigila el INR"))));

        assertThat(r.alertas())
                .anyMatch(a -> a.clave().equals(clave)
                        && a.justificacion() != null
                        && a.justificadaPor() != null
                        && a.justificadaEn() != null);
    }

    @Test
    void laContraindicacionPorAlergiaExigeTreintaCaracteresDeJustificacion() {
        paciente.setAlergias("Penicilina");
        pacienteRepository.save(paciente);
        List<ItemEmision> items = List.of(item(0, amoxicilina, 21));
        String clave = alertas(amoxicilina).alertas().get(0).clave();

        assertThatThrownBy(() -> recetas.emitir(
                        principalMedico,
                        emision(items, List.of(new JustificacionAlerta(clave, "Tolera el medicamento")))))
                .isInstanceOf(ConflictException.class);

        RecetaResponse r = recetas.emitir(
                principalMedico,
                emision(
                        items,
                        List.of(new JustificacionAlerta(
                                clave, "Alergia descartada con prueba cutánea negativa reciente"))));
        assertThat(r.estado()).isEqualTo(EstadoRecetaElectronica.EMITIDA);
    }

    @Test
    void losMedicamentosDeControlEspecialLimitanLaVigenciaASieteDias() {
        assertThatThrownBy(() -> recetas.emitir(
                        principalMedico,
                        new EmitirRecetaRequest(
                                atencion.getId(), 30, null, null, null, null, List.of(item(0, diazepam, 10)), null)))
                .hasMessageContaining("7 días");

        RecetaResponse r = recetas.emitir(
                principalMedico,
                new EmitirRecetaRequest(
                        atencion.getId(), null, null, null, null, null, List.of(item(0, diazepam, 10)), null));

        assertThat(r.controlEspecial()).isTrue();
        assertThat(r.vigenteHasta()).isBefore(r.emitidaEn().plusDays(8));
    }

    @Test
    void noSePuedeEmitirDosVecesLaRecetaDeUnaAtencionHastaAnularla() {
        RecetaResponse primera = recetas.emitir(principalMedico, emision(List.of(item(0, paracetamol, 10)), null));

        assertThatThrownBy(() -> recetas.emitir(principalMedico, emision(List.of(item(0, paracetamol, 10)), null)))
                .isInstanceOf(ConflictException.class);

        assertThat(recetas.anular(principalMedico, primera.id(), "Error en la cantidad")
                        .estado())
                .isEqualTo(EstadoRecetaElectronica.ANULADA);

        RecetaResponse nueva = recetas.emitir(
                principalMedico,
                new EmitirRecetaRequest(
                        atencion.getId(),
                        null,
                        null,
                        null,
                        null,
                        primera.id(),
                        List.of(item(0, paracetamol, 5)),
                        null));
        assertThat(nueva.reemplazaNumero()).isEqualTo(primera.numero());
        assertThat(nueva.numero()).isNotEqualTo(primera.numero());
    }

    @Test
    void unaRecetaAnuladaNoSePuedeAnularNiReemplazarSiNoEstaAnulada() {
        RecetaResponse primera = recetas.emitir(principalMedico, emision(List.of(item(0, paracetamol, 10)), null));
        Atencion otra = atencionConReceta(paciente, 1);

        assertThatThrownBy(() -> recetas.emitir(
                        principalMedico,
                        new EmitirRecetaRequest(
                                otra.getId(),
                                null,
                                null,
                                null,
                                null,
                                primera.id(),
                                List.of(item(0, paracetamol, 5)),
                                null)))
                .isInstanceOf(ConflictException.class);

        recetas.anular(principalMedico, primera.id(), "Motivo valido");
        assertThatThrownBy(() -> recetas.anular(principalMedico, primera.id(), "Otro motivo"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void laModificacionDeLaRecetaDeLaAtencionQuedaBloqueadaConUnaRecetaVigente() {
        recetas.emitir(principalMedico, emision(List.of(item(0, paracetamol, 10)), null));

        assertThat(recetaRepository.existsByAtencionIdAndEstadoNot(atencion.getId(), EstadoRecetaElectronica.ANULADA))
                .isTrue();
    }

    @Test
    void siSeAlteraElContenidoLaFirmaYaNoEsValida() {
        RecetaResponse r = recetas.emitir(principalMedico, emision(List.of(item(0, paracetamol, 10)), null));
        RecetaElectronica entidad = recetaRepository.findDetalle(r.id()).orElseThrow();
        entidad.setContenidoCanonico(entidad.getContenidoCanonico().replace("|10\n", "|100\n"));
        recetaRepository.saveAndFlush(entidad);

        assertThat(recetas.verificarFirma(principalMedico, r.id()).firmaValida())
                .isFalse();
        assertThat(recetas.obtener(principalMedico, r.id()).firmaValida()).isFalse();
    }

    @Test
    void laDispensacionParcialDescuentaLoPendienteYBloqueaElExceso() {
        RecetaResponse r = recetas.emitir(
                principalMedico, emision(List.of(item(0, paracetamol, 10), item(2, ibuprofeno, 6)), null));
        Atencion enBase =
                atencionRepository.findByIdConDetalle(atencion.getId()).orElseThrow();
        UUID farmaciaId = fixture.usuario("farmacia", RolNombre.ROLE_FARMACIA).getId();

        dispensaciones.validar(enBase, 0, 4);
        boolean completo = dispensaciones.registrar(enBase, 0, 4, UUID.randomUUID(), farmaciaId);

        assertThat(completo).isFalse();
        assertThat(recetas.obtener(principalMedico, r.id()).estado())
                .isEqualTo(EstadoRecetaElectronica.DISPENSADA_PARCIAL);
        assertThatThrownBy(() -> dispensaciones.validar(enBase, 0, 7))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("pendiente");
        assertThatThrownBy(() -> dispensaciones.validar(enBase, 1, 1)).isInstanceOf(ConflictException.class);

        assertThat(dispensaciones.registrar(enBase, 0, 6, UUID.randomUUID(), farmaciaId))
                .isTrue();
        assertThat(dispensaciones.registrar(enBase, 2, 6, UUID.randomUUID(), farmaciaId))
                .isTrue();
        assertThat(recetas.obtener(principalMedico, r.id()).estado()).isEqualTo(EstadoRecetaElectronica.DISPENSADA);
        assertThatThrownBy(() -> dispensaciones.validar(enBase, 0, 1)).isInstanceOf(ConflictException.class);
    }

    @Test
    void revertirUnaDispensacionDevuelveLaCantidadPendiente() {
        RecetaResponse r = recetas.emitir(principalMedico, emision(List.of(item(0, paracetamol, 10)), null));
        Atencion enBase =
                atencionRepository.findByIdConDetalle(atencion.getId()).orElseThrow();
        UUID dispensacion = UUID.randomUUID();
        dispensaciones.registrar(enBase, 0, 10, dispensacion, UUID.randomUUID());
        assertThat(recetas.obtener(principalMedico, r.id()).estado()).isEqualTo(EstadoRecetaElectronica.DISPENSADA);

        dispensaciones.revertir(dispensacion);

        RecetaResponse despues = recetas.obtener(principalMedico, r.id());
        assertThat(despues.estado()).isEqualTo(EstadoRecetaElectronica.EMITIDA);
        assertThat(despues.items().get(0).pendiente()).isEqualTo(10);
    }

    @Test
    void unaRecetaAnuladaYUnaVencidaNoSeDispensan() {
        RecetaResponse r = recetas.emitir(principalMedico, emision(List.of(item(0, paracetamol, 10)), null));
        Atencion enBase =
                atencionRepository.findByIdConDetalle(atencion.getId()).orElseThrow();

        RecetaElectronica entidad = recetaRepository.findDetalle(r.id()).orElseThrow();
        entidad.setVigenteHasta(LocalDateTime.now().minusMinutes(1));
        recetaRepository.saveAndFlush(entidad);
        assertThatThrownBy(() -> dispensaciones.validar(enBase, 0, 1))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("venció");

        entidad.setVigenteHasta(LocalDateTime.now().plusDays(1));
        recetaRepository.saveAndFlush(entidad);
        recetas.anular(principalMedico, r.id(), "Emitida por error");
        assertThatThrownBy(() -> dispensaciones.validar(enBase, 0, 1))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("anulada");
    }

    @Test
    void soloElMedicoDeLaAtencionEmiteYSoloElQueEmitioAnula() {
        Medico otro = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        UsuarioPrincipal principalOtro = fixture.principal(otro.getUsuario());

        assertThatThrownBy(() -> recetas.emitir(principalOtro, emision(List.of(item(0, paracetamol, 10)), null)))
                .isInstanceOf(ForbiddenOperationException.class);

        RecetaResponse r = recetas.emitir(principalMedico, emision(List.of(item(0, paracetamol, 10)), null));
        assertThatThrownBy(() -> recetas.anular(principalOtro, r.id(), "Motivo valido"))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void unMedicoSinRelacionNoEvaluaAlertasNiVeLaReceta() {
        Medico ajeno = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        UsuarioPrincipal principalAjeno = fixture.principal(ajeno.getUsuario());
        RecetaResponse r = recetas.emitir(principalMedico, emision(List.of(item(0, paracetamol, 10)), null));

        assertThatThrownBy(() -> recetas.evaluarAlertas(
                        principalAjeno,
                        new AlertasRequest(
                                paciente.getId(),
                                List.of(new ItemPrescripcion(paracetamol, null, null)),
                                false,
                                false)))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> recetas.obtener(principalAjeno, r.id()))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> recetas.delPaciente(principalAjeno, paciente.getId()))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void elPacienteVeSusRecetasPeroNoLasAjenas() {
        RecetaResponse r = recetas.emitir(principalMedico, emision(List.of(item(0, paracetamol, 10)), null));
        Paciente otro = fixture.paciente();
        UsuarioPrincipal principalOtro = fixture.principal(otro.getUsuario());

        assertThat(recetas.misRecetas(principalPaciente)).extracting("numero").contains(r.numero());
        assertThat(recetas.obtener(principalPaciente, r.id()).numero()).isEqualTo(r.numero());
        assertThatThrownBy(() -> recetas.obtener(principalOtro, r.id()))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThat(recetas.misRecetas(principalOtro)).isEmpty();
    }

    @Test
    void generaElPdfDeLaReceta() {
        RecetaResponse r = recetas.emitir(principalMedico, emision(List.of(item(0, paracetamol, 10)), null));

        byte[] pdf = recetas.generarPdf(principalPaciente, r.id());

        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }

    @Test
    void elPacienteNoVeLasAlertasNiLasJustificacionesInternas() {
        paciente.setAlergias("Penicilina");
        pacienteRepository.save(paciente);
        String clave = alertas(amoxicilina).alertas().get(0).clave();
        RecetaResponse r = recetas.emitir(
                principalMedico,
                emision(
                        List.of(item(0, amoxicilina, 21)),
                        List.of(new JustificacionAlerta(
                                clave, "Alergia descartada con prueba cutánea negativa reciente"))));

        assertThat(recetas.obtener(principalMedico, r.id()).alertas()).isNotEmpty();
        assertThat(recetas.obtener(principalPaciente, r.id()).alertas()).isEmpty();
    }

    @Test
    void importaElCatalogoPorCsvYReportaLosErrores() {
        String csv = "dci;clase_terapeutica;forma;concentracion;via;embarazo;control\n"
                + "Importado " + sufijo + ";Clase de prueba;Tableta;10 mg;Oral;B;NINGUNA\n"
                + ";Clase;Tableta;10 mg;Oral;B;NINGUNA\n"
                + "Otro " + sufijo + ";Clase;Tableta;10 mg;Oral;Z;NINGUNA\n";

        ImportacionCatalogoResponse vista = catalogo.importar(csv.getBytes(StandardCharsets.UTF_8), true);
        assertThat(vista.creados()).isEqualTo(1);
        assertThat(vista.errores()).hasSize(2);
        assertThat(catalogo.buscar("Importado " + sufijo)).isEmpty();

        ImportacionCatalogoResponse real = catalogo.importar(csv.getBytes(StandardCharsets.UTF_8), false);
        assertThat(real.creados()).isEqualTo(1);
        assertThat(catalogo.buscar("Importado " + sufijo)).hasSize(1);

        ImportacionCatalogoResponse repetida = catalogo.importar(csv.getBytes(StandardCharsets.UTF_8), false);
        assertThat(repetida.creados()).isZero();
        assertThat(repetida.actualizados()).isEqualTo(1);
    }
}
