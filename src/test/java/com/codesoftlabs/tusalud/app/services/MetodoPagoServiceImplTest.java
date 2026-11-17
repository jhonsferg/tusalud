package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.request.MetodoPagoRequest;
import com.codesoftlabs.tusalud.app.dto.response.MetodoPagoResponse;
import com.codesoftlabs.tusalud.app.entities.enums.MarcaTarjeta;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoMetodoPago;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.MetodoPagoService;
import java.time.YearMonth;
import java.util.List;
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
class MetodoPagoServiceImplTest {

    @Autowired
    private MetodoPagoService metodoPagoService;

    @Autowired
    private ServiceTestFixture fixture;

    private static int anioVigente() {
        return YearMonth.now().plusYears(2).getYear();
    }

    private static MetodoPagoRequest tarjeta(String numero, int mes, int anio, Boolean predeterminado) {
        return new MetodoPagoRequest(
                TipoMetodoPago.TARJETA_CREDITO, "JUAN PEREZ", numero, mes, anio, "123", predeterminado);
    }

    @Test
    void primerMetodoQuedaPredeterminadoYSoloGuardaLosUltimosCuatroDigitos() {
        UsuarioPrincipal actor = fixture.principal(fixture.usuario("mp1", RolNombre.ROLE_PACIENTE));

        MetodoPagoResponse metodo =
                metodoPagoService.crear(actor, tarjeta("4111 1111 1111 1111", 12, anioVigente(), null));

        assertThat(metodo.marca()).isEqualTo(MarcaTarjeta.VISA);
        assertThat(metodo.ultimos4()).isEqualTo("1111");
        assertThat(metodo.predeterminado()).isTrue();
        assertThat(metodo.vencida()).isFalse();
    }

    @Test
    void segundoMetodoNoEsPredeterminadoSalvoQueSeaPedido() {
        UsuarioPrincipal actor = fixture.principal(fixture.usuario("mp2", RolNombre.ROLE_PACIENTE));
        metodoPagoService.crear(actor, tarjeta("4111111111111111", 12, anioVigente(), null));

        MetodoPagoResponse segundo =
                metodoPagoService.crear(actor, tarjeta("5555555555554444", 6, anioVigente(), null));

        assertThat(segundo.marca()).isEqualTo(MarcaTarjeta.MASTERCARD);
        assertThat(segundo.predeterminado()).isFalse();
        assertThat(metodoPagoService.listar(actor)).hasSize(2);
        assertThat(metodoPagoService.listar(actor).get(0).ultimos4()).isEqualTo("1111");
    }

    @Test
    void detectaAmexYMarcaOtraParaNumerosNoReconocidos() {
        UsuarioPrincipal actor = fixture.principal(fixture.usuario("mp3", RolNombre.ROLE_PACIENTE));

        MetodoPagoResponse amex = metodoPagoService.crear(actor, tarjeta("378282246310005", 3, anioVigente(), null));
        MetodoPagoResponse otra = metodoPagoService.crear(actor, tarjeta("6011111111111117", 3, anioVigente(), null));

        assertThat(amex.marca()).isEqualTo(MarcaTarjeta.AMEX);
        assertThat(amex.ultimos4()).isEqualTo("0005");
        assertThat(otra.marca()).isEqualTo(MarcaTarjeta.OTRA);
    }

    @Test
    void numeroQueNoPasaLuhnLanzaDatosInvalidos() {
        UsuarioPrincipal actor = fixture.principal(fixture.usuario("mp4", RolNombre.ROLE_PACIENTE));

        assertThatThrownBy(() -> metodoPagoService.crear(actor, tarjeta("4111111111111112", 12, anioVigente(), null)))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("no es válido");
    }

    @Test
    void numeroConLongitudInvalidaLanzaDatosInvalidos() {
        UsuarioPrincipal actor = fixture.principal(fixture.usuario("mp5", RolNombre.ROLE_PACIENTE));

        assertThatThrownBy(() -> metodoPagoService.crear(actor, tarjeta("4111", 12, anioVigente(), null)))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("13 y 19");
    }

    @Test
    void tarjetaVencidaLanzaDatosInvalidos() {
        UsuarioPrincipal actor = fixture.principal(fixture.usuario("mp6", RolNombre.ROLE_PACIENTE));

        assertThatThrownBy(() -> metodoPagoService.crear(actor, tarjeta("4111111111111111", 1, 2020, null)))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("vencida");
    }

    @Test
    void eliminarPredeterminadoPasaElSiguienteAPredeterminado() {
        UsuarioPrincipal actor = fixture.principal(fixture.usuario("mp7", RolNombre.ROLE_PACIENTE));
        MetodoPagoResponse primero =
                metodoPagoService.crear(actor, tarjeta("4111111111111111", 12, anioVigente(), null));
        MetodoPagoResponse segundo =
                metodoPagoService.crear(actor, tarjeta("5555555555554444", 12, anioVigente(), null));

        metodoPagoService.eliminar(actor, primero.id());

        List<MetodoPagoResponse> restantes = metodoPagoService.listar(actor);
        assertThat(restantes).singleElement().satisfies(m -> {
            assertThat(m.id()).isEqualTo(segundo.id());
            assertThat(m.predeterminado()).isTrue();
        });
    }

    @Test
    void marcarPredeterminadoCambiaElMetodoActivo() {
        UsuarioPrincipal actor = fixture.principal(fixture.usuario("mp8", RolNombre.ROLE_PACIENTE));
        metodoPagoService.crear(actor, tarjeta("4111111111111111", 12, anioVigente(), null));
        MetodoPagoResponse segundo =
                metodoPagoService.crear(actor, tarjeta("5555555555554444", 12, anioVigente(), null));

        MetodoPagoResponse marcado = metodoPagoService.marcarPredeterminado(actor, segundo.id());

        assertThat(marcado.predeterminado()).isTrue();
        assertThat(metodoPagoService.listar(actor))
                .filteredOn(MetodoPagoResponse::predeterminado)
                .singleElement()
                .satisfies(m -> assertThat(m.id()).isEqualTo(segundo.id()));
    }

    @Test
    void metodoDeOtroUsuarioNoEsAccesible() {
        UsuarioPrincipal dueno = fixture.principal(fixture.usuario("mp9", RolNombre.ROLE_PACIENTE));
        UsuarioPrincipal otro = fixture.principal(fixture.usuario("mp10", RolNombre.ROLE_PACIENTE));
        MetodoPagoResponse metodo =
                metodoPagoService.crear(dueno, tarjeta("4111111111111111", 12, anioVigente(), null));

        assertThatThrownBy(() -> metodoPagoService.eliminar(otro, metodo.id()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> metodoPagoService.marcarPredeterminado(otro, metodo.id()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
