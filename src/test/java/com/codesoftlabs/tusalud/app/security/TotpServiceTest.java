package com.codesoftlabs.tusalud.app.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.config.MfaProperties;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TotpServiceTest {

    private static final String SECRETO_RFC =
            TotpService.codificarBase32("12345678901234567890".getBytes(StandardCharsets.US_ASCII));

    private final TotpService totpService = new TotpService();

    @Test
    void generaLosCodigosDeReferenciaDelRfc6238() {
        assertThat(totpService.generarCodigo(SECRETO_RFC, Instant.ofEpochSecond(59)))
                .isEqualTo("287082");
        assertThat(totpService.generarCodigo(SECRETO_RFC, Instant.ofEpochSecond(1111111109L)))
                .isEqualTo("081804");
        assertThat(totpService.generarCodigo(SECRETO_RFC, Instant.ofEpochSecond(1234567890L)))
                .isEqualTo("005924");
    }

    @Test
    void aceptaLaVentanaDeUnPasoYRechazaElResto() {
        Instant ahora = Instant.ofEpochSecond(1_700_000_010L);
        String actual = totpService.generarCodigo(SECRETO_RFC, ahora);

        assertThat(totpService.verificar(SECRETO_RFC, actual, ahora)).isTrue();
        assertThat(totpService.verificar(SECRETO_RFC, actual, ahora.plusSeconds(30)))
                .isTrue();
        assertThat(totpService.verificar(SECRETO_RFC, actual, ahora.minusSeconds(30)))
                .isTrue();
        assertThat(totpService.verificar(SECRETO_RFC, actual, ahora.plusSeconds(120)))
                .isFalse();
        assertThat(totpService.verificar(SECRETO_RFC, "12345", ahora)).isFalse();
        assertThat(totpService.verificar(SECRETO_RFC, "abcdef", ahora)).isFalse();
        assertThat(totpService.verificar(SECRETO_RFC, null, ahora)).isFalse();
    }

    @Test
    void base32HaceElViajeDeIdaYVuelta() {
        String secreto = totpService.generarSecreto();

        assertThat(secreto).hasSize(32).matches("[A-Z2-7]+");
        assertThat(TotpService.codificarBase32(TotpService.decodificarBase32(secreto)))
                .isEqualTo(secreto);
    }

    @Test
    void construyeLaUriOtpauthParaLaAplicacion() {
        String uri = totpService.otpauthUri("juan.perez@correo.com", "ABCDEF234567");

        assertThat(uri)
                .startsWith("otpauth://totp/TuSalud%3Ajuan.perez%40correo.com?")
                .contains("secret=ABCDEF234567")
                .contains("issuer=TuSalud")
                .contains("digits=6")
                .contains("period=30");
    }

    @Test
    void cifraYDescifraSecretosConAesGcm() {
        CifradorSecretos cifrador = new CifradorSecretos(new MfaProperties("clave-de-prueba"));

        String cifrado = cifrador.cifrar("JBSWY3DPEHPK3PXP");

        assertThat(cifrado).isNotEqualTo("JBSWY3DPEHPK3PXP");
        assertThat(cifrador.cifrar("JBSWY3DPEHPK3PXP")).isNotEqualTo(cifrado);
        assertThat(cifrador.descifrar(cifrado)).isEqualTo("JBSWY3DPEHPK3PXP");
        assertThatThrownBy(() -> new CifradorSecretos(new MfaProperties("otra-clave")).descifrar(cifrado))
                .isInstanceOf(IllegalStateException.class);
    }
}
