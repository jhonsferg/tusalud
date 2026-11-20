package com.codesoftlabs.tusalud.app.services.receta;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrdenInteraccionesTest {

    private static final UUID ALTO = UUID.fromString("f0000000-0000-4000-8000-000000000000");
    private static final UUID BAJO = UUID.fromString("0f000000-0000-4000-8000-000000000000");

    @Test
    void elOrdenCoincideConElDeUuidEnPostgresSinSignoEnElBitAlto() {
        assertThat(ALTO.compareTo(BAJO)).isLessThan(0);

        assertThat(CatalogoMedicamentosService.enOrden(BAJO, ALTO)).isTrue();
        assertThat(CatalogoMedicamentosService.enOrden(ALTO, BAJO)).isFalse();
    }

    @Test
    void dosIdentificadoresIgualesNoEstanEnOrden() {
        assertThat(CatalogoMedicamentosService.enOrden(ALTO, ALTO)).isFalse();
    }

    @Test
    void comparaLosBytesDeIzquierdaADerecha() {
        UUID a = UUID.fromString("00000000-0000-4000-8000-000000000001");
        UUID b = UUID.fromString("00000000-0000-4000-8000-000000000002");

        assertThat(CatalogoMedicamentosService.enOrden(a, b)).isTrue();
        assertThat(CatalogoMedicamentosService.enOrden(b, a)).isFalse();
    }
}
