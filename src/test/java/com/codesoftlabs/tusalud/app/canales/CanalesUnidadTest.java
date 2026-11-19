package com.codesoftlabs.tusalud.app.canales;

import static org.assertj.core.api.Assertions.assertThat;

import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.Canal;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.services.canales.DespachadorCanales;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CanalesUnidadTest {

    private static final String SECRETO = "secreto-de-pruebas";

    @Test
    void enmascaraCorreosYTelefonos() {
        assertThat(EnmascaradorContacto.enmascarar(Canal.CORREO, "maria.lopez@clinica.pe"))
                .isEqualTo("m***@clinica.pe");
        assertThat(EnmascaradorContacto.enmascarar(Canal.SMS, "+51987654321")).isEqualTo("***321");
        assertThat(EnmascaradorContacto.enmascarar(Canal.WHATSAPP, "12")).isEqualTo("***");
        assertThat(EnmascaradorContacto.enmascarar(Canal.CORREO, "sinarroba")).isEqualTo("***");
        assertThat(EnmascaradorContacto.enmascarar(Canal.SMS, null)).isEmpty();
    }

    @Test
    void elTokenDeBajaVaFirmadoYDetectaAlteraciones() {
        UUID usuario = UUID.randomUUID();
        String token = FirmasCanales.crearTokenBaja(SECRETO, usuario, Canal.WHATSAPP);
        assertThat(FirmasCanales.leerTokenBaja(SECRETO, token)).hasValueSatisfying(datos -> {
            assertThat(datos.usuarioId()).isEqualTo(usuario);
            assertThat(datos.canal()).isEqualTo(Canal.WHATSAPP);
        });
        assertThat(FirmasCanales.leerTokenBaja("otro", token)).isEmpty();
        assertThat(FirmasCanales.leerTokenBaja(SECRETO, token.substring(1))).isEmpty();
        assertThat(FirmasCanales.leerTokenBaja(SECRETO, null)).isEmpty();
        assertThat(FirmasCanales.leerTokenBaja(SECRETO, "a.b.c")).isEmpty();
        assertThat(FirmasCanales.leerTokenBaja(SECRETO, "x".repeat(400))).isEmpty();
    }

    @Test
    void laComparacionDeFirmasNoFallaConNulos() {
        assertThat(FirmasCanales.iguales("a", "a")).isTrue();
        assertThat(FirmasCanales.iguales("a", "b")).isFalse();
        assertThat(FirmasCanales.iguales(null, "b")).isFalse();
        assertThat(FirmasCanales.hmacHex(SECRETO, "x".getBytes())).hasSize(64);
    }

    @Test
    void elRenderizadorSoloReemplazaVariablesPermitidasYEscapaElHtml() {
        assertThat(RenderizadorMensaje.variablesDesconocidas("Hola {{nombre}} {{ diagnostico }} {{enlace}} {{otra}}"))
                .containsExactly("diagnostico", "otra");
        assertThat(RenderizadorMensaje.renderizar(
                        "Hola {{nombre}}, ingrese a {{ enlace }}",
                        Map.of("nombre", "Ana $1", "enlace", "https://app.test")))
                .isEqualTo("Hola Ana $1, ingrese a https://app.test");
        String html = RenderizadorMensaje.html(
                "Asunto <b>",
                "Hola <script>alert(1)</script>\n\nVisite https://app.test/x",
                "TuSalud",
                "https://app.test/baja?t=1");
        assertThat(html)
                .doesNotContain("<script>")
                .contains("&lt;script&gt;")
                .contains("<a href=\"https://app.test/x\"")
                .contains("Dejar de recibir");
        assertThat(RenderizadorMensaje.textoCorreo("Cuerpo", "https://app.test/baja"))
                .contains("Para dejar de recibir");
    }

    @Test
    void laPoliticaMarcaLasAlertasDeSeguridadComoObligatoriasYUrgentes() {
        assertThat(PoliticaCanales.de(TipoNotificacion.SESION_SOSPECHOSA)).hasValueSatisfying(p -> {
            assertThat(p.obligatoria()).isTrue();
            assertThat(p.urgente()).isTrue();
        });
        assertThat(PoliticaCanales.de(TipoNotificacion.CITA_CREADA))
                .hasValueSatisfying(p -> assertThat(p.obligatoria()).isFalse());
        assertThat(PoliticaCanales.externa(TipoNotificacion.STOCK_BAJO)).isFalse();
        assertThat(PoliticaCanales.activoPorDefecto(TipoNotificacion.DOCUMENTO_EMITIDO, Canal.CORREO))
                .isTrue();
        assertThat(PoliticaCanales.activoPorDefecto(TipoNotificacion.DOCUMENTO_EMITIDO, Canal.SMS))
                .isFalse();
        assertThat(PoliticaCanales.activoPorDefecto(TipoNotificacion.RECORDATORIO_CITA, Canal.WHATSAPP))
                .isTrue();
    }

    @Test
    void todasLasPlantillasPorDefectoUsanSoloVariablesPermitidasYNoCitanDatosClinicos() {
        for (TipoNotificacion tipo : PoliticaCanales.tiposExternos()) {
            for (Canal canal : Canal.values()) {
                var contenido = PlantillasPorDefecto.de(tipo, canal);
                assertThat(contenido).as("%s %s", tipo, canal).isNotNull();
                assertThat(RenderizadorMensaje.variablesDesconocidas(contenido.cuerpo()))
                        .isEmpty();
                assertThat(contenido.cuerpo()).contains("{{enlace}}");
                if (canal == Canal.CORREO) {
                    assertThat(contenido.asunto()).isNotBlank();
                } else {
                    assertThat(contenido.cuerpo().length()).isLessThanOrEqualTo(320);
                }
            }
        }
        assertThat(PlantillasPorDefecto.de(TipoNotificacion.STOCK_BAJO, Canal.CORREO))
                .isNull();
    }

    @Test
    void laVentanaDeSilencioNocturnaYDiurnaCalculaElSiguienteEnvio() {
        LocalTime desde = LocalTime.of(22, 0);
        LocalTime hasta = LocalTime.of(7, 0);
        LocalDateTime dia = LocalDateTime.of(2026, 3, 10, 12, 0);
        assertThat(DespachadorCanales.despuesDelSilencio(dia, desde, hasta)).isEqualTo(dia);
        LocalDateTime noche = LocalDateTime.of(2026, 3, 10, 23, 0);
        assertThat(DespachadorCanales.despuesDelSilencio(noche, desde, hasta))
                .isEqualTo(LocalDateTime.of(2026, 3, 11, 7, 0));
        LocalDateTime madrugada = LocalDateTime.of(2026, 3, 10, 3, 0);
        assertThat(DespachadorCanales.despuesDelSilencio(madrugada, desde, hasta))
                .isEqualTo(LocalDateTime.of(2026, 3, 10, 7, 0));
        LocalDateTime justoAlFin = LocalDateTime.of(2026, 3, 10, 7, 0);
        assertThat(DespachadorCanales.despuesDelSilencio(justoAlFin, desde, hasta))
                .isEqualTo(justoAlFin);
        assertThat(DespachadorCanales.despuesDelSilencio(
                        LocalDateTime.of(2026, 3, 10, 13, 30), LocalTime.of(13, 0), LocalTime.of(15, 0)))
                .isEqualTo(LocalDateTime.of(2026, 3, 10, 15, 0));
    }

    @Test
    void elProveedorSimuladoRechazaDestinosDePruebaYGuardaLaBandeja() {
        ProveedorSimulado proveedor = new ProveedorSimulado();
        MensajeSaliente ok = new MensajeSaliente(Canal.SMS, "+51911111111", null, "hola", null, "x", "k1");
        assertThat(proveedor.enviar(ok)).startsWith("sim-");
        assertThat(proveedor.ultimos(5)).hasSize(1);
        MensajeSaliente transitorio = new MensajeSaliente(Canal.SMS, "+51900000000", null, "hola", null, "x", "k2");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> proveedor.enviar(transitorio))
                .isInstanceOfSatisfying(
                        EnvioException.class, e -> assertThat(e.permanente()).isFalse());
        MensajeSaliente permanente = new MensajeSaliente(Canal.SMS, "+51999999999", null, "hola", null, "x", "k3");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> proveedor.enviar(permanente))
                .isInstanceOfSatisfying(
                        EnvioException.class, e -> assertThat(e.permanente()).isTrue());
        proveedor.vaciar();
        assertThat(proveedor.ultimos(5)).isEmpty();
    }
}
