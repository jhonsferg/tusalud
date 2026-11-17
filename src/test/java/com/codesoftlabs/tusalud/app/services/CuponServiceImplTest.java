package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.request.CuponRequest;
import com.codesoftlabs.tusalud.app.dto.response.CuponResponse;
import com.codesoftlabs.tusalud.app.dto.response.ValidacionCuponResponse;
import com.codesoftlabs.tusalud.app.entities.enums.TipoCupon;
import com.codesoftlabs.tusalud.app.exceptions.BusinessLogicException;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.services.interfaces.CuponService;
import java.math.BigDecimal;
import java.time.LocalDate;
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
class CuponServiceImplTest {

    @Autowired
    private CuponService cuponService;

    private static String codigoUnico() {
        return "CUP" + ServiceTestFixture.siguiente();
    }

    private static CuponRequest porcentaje(String codigo, String valor, LocalDate inicio, LocalDate fin, Integer usos) {
        return new CuponRequest(codigo, "Prueba", TipoCupon.PORCENTAJE, new BigDecimal(valor), inicio, fin, usos, true);
    }

    @Test
    void crearNormalizaElCodigoAMayusculasYLoMarcaVigente() {
        String codigo = codigoUnico();

        CuponResponse cupon = cuponService.crear(porcentaje(
                "  " + codigo.toLowerCase() + " ",
                "10",
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(30),
                null));

        assertThat(cupon.codigo()).isEqualTo(codigo);
        assertThat(cupon.vigente()).isTrue();
        assertThat(cupon.usosActuales()).isZero();
    }

    @Test
    void crearConCodigoDuplicadoLanzaConflict() {
        String codigo = codigoUnico();
        cuponService.crear(
                porcentaje(codigo, "10", LocalDate.now(), LocalDate.now().plusDays(5), null));

        assertThatThrownBy(() -> cuponService.crear(porcentaje(
                        codigo.toLowerCase(),
                        "15",
                        LocalDate.now(),
                        LocalDate.now().plusDays(5),
                        null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void porcentajeFueraDeRangoLanzaDatosInvalidos() {
        assertThatThrownBy(() -> cuponService.crear(porcentaje(
                        codigoUnico(), "150", LocalDate.now(), LocalDate.now().plusDays(5), null)))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("entre 1 y 100");
    }

    @Test
    void fechaFinAnteriorALaDeInicioLanzaDatosInvalidos() {
        assertThatThrownBy(() -> cuponService.crear(porcentaje(
                        codigoUnico(), "10", LocalDate.now(), LocalDate.now().minusDays(1), null)))
                .isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void validarCalculaDescuentoYTotal() {
        String codigo = codigoUnico();
        cuponService.crear(porcentaje(
                codigo, "10", LocalDate.now().minusDays(1), LocalDate.now().plusDays(30), null));

        ValidacionCuponResponse resultado = cuponService.validar(codigo, new BigDecimal("150.00"));

        assertThat(resultado.valido()).isTrue();
        assertThat(resultado.descuento()).isEqualByComparingTo("15.00");
        assertThat(resultado.total()).isEqualByComparingTo("135.00");
    }

    @Test
    void validarCuponVencidoDevuelveMotivoSinDescuento() {
        String codigo = codigoUnico();
        cuponService.crear(porcentaje(
                codigo, "10", LocalDate.now().minusDays(30), LocalDate.now().minusDays(1), null));

        ValidacionCuponResponse resultado = cuponService.validar(codigo, new BigDecimal("150.00"));

        assertThat(resultado.valido()).isFalse();
        assertThat(resultado.mensaje()).contains("venció");
        assertThat(resultado.descuento()).isEqualByComparingTo("0");
        assertThat(resultado.total()).isEqualByComparingTo("150.00");
    }

    @Test
    void validarCodigoInexistenteNoEsValido() {
        ValidacionCuponResponse resultado = cuponService.validar(codigoUnico(), new BigDecimal("100.00"));

        assertThat(resultado.valido()).isFalse();
        assertThat(resultado.mensaje()).isEqualTo("El cupón no existe");
    }

    @Test
    void montoFijoNoSuperaElMontoDeLaCita() {
        String codigo = codigoUnico();
        cuponService.crear(new CuponRequest(
                codigo,
                null,
                TipoCupon.MONTO_FIJO,
                new BigDecimal("50"),
                LocalDate.now(),
                LocalDate.now().plusDays(10),
                null,
                true));

        ValidacionCuponResponse resultado = cuponService.validar(codigo, new BigDecimal("30.00"));

        assertThat(resultado.valido()).isTrue();
        assertThat(resultado.descuento()).isEqualByComparingTo("30.00");
        assertThat(resultado.total()).isEqualByComparingTo("0.00");
    }

    @Test
    void consumirIncrementaUsosYRespetaElLimite() {
        String codigo = codigoUnico();
        cuponService.crear(
                porcentaje(codigo, "20", LocalDate.now(), LocalDate.now().plusDays(10), 1));

        assertThat(cuponService.consumir(codigo).getUsosActuales()).isEqualTo(1);
        assertThatThrownBy(() -> cuponService.consumir(codigo))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessageContaining("límite de usos");
    }

    @Test
    void eliminarCuponUsadoLanzaConflict() {
        String codigo = codigoUnico();
        CuponResponse cupon = cuponService.crear(
                porcentaje(codigo, "20", LocalDate.now(), LocalDate.now().plusDays(10), null));
        cuponService.consumir(codigo);

        assertThatThrownBy(() -> cuponService.eliminar(cupon.id())).isInstanceOf(ConflictException.class);
    }

    @Test
    void eliminarCuponSinUsosLoQuita() {
        String codigo = codigoUnico();
        CuponResponse cupon = cuponService.crear(
                porcentaje(codigo, "20", LocalDate.now(), LocalDate.now().plusDays(10), null));

        cuponService.eliminar(cupon.id());

        assertThat(cuponService.validar(codigo, new BigDecimal("10.00")).valido())
                .isFalse();
    }

    @Test
    void actualizarCambiaElValorDelCupon() {
        String codigo = codigoUnico();
        CuponResponse cupon = cuponService.crear(
                porcentaje(codigo, "10", LocalDate.now(), LocalDate.now().plusDays(10), null));

        CuponResponse actualizado = cuponService.actualizar(
                cupon.id(),
                porcentaje(codigo, "25", LocalDate.now(), LocalDate.now().plusDays(10), null));

        assertThat(actualizado.valor()).isEqualByComparingTo("25.00");
        assertThat(cuponService.validar(codigo, new BigDecimal("100.00")).descuento())
                .isEqualByComparingTo("25.00");
    }
}
