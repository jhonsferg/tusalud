package com.codesoftlabs.tusalud.app.storage;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NombresArchivoTest {

    @Test
    void quitaRutasYRecorridosDeDirectorio() {
        assertThat(NombresArchivo.sanitizar("../../etc/passwd.png")).isEqualTo("passwd.png");
        assertThat(NombresArchivo.sanitizar("C:\\Windows\\system32\\evil.png")).isEqualTo("evil.png");
        assertThat(NombresArchivo.sanitizar("a..b...c.png")).isEqualTo("a.b.c.png");
    }

    @Test
    void reemplazaCaracteresPeligrososYConservaAcentos() {
        assertThat(NombresArchivo.sanitizar("radiografía <tórax>;rm.png")).isEqualTo("radiografía _tórax__rm.png");
        assertThat(NombresArchivo.sanitizar("a\u0000b\r\n.png")).doesNotContain("\u0000", "\r", "\n");
    }

    @Test
    void evitaNombresOcultosYVacios() {
        assertThat(NombresArchivo.sanitizar(".htaccess")).isEqualTo("htaccess");
        assertThat(NombresArchivo.sanitizar("   ")).isEqualTo("archivo");
        assertThat(NombresArchivo.sanitizar(null)).isEqualTo("archivo");
        assertThat(NombresArchivo.sanitizar("///")).isEqualTo("archivo");
    }

    @Test
    void limitaLaLongitudConservandoLaExtension() {
        String largo = "x".repeat(300) + ".pdf";

        String limpio = NombresArchivo.sanitizar(largo);

        assertThat(limpio).hasSizeLessThanOrEqualTo(120).endsWith(".pdf");
    }

    @Test
    void extraeLaExtensionEnMinusculas() {
        assertThat(NombresArchivo.extension("Foto.PNG")).isEqualTo("png");
        assertThat(NombresArchivo.extension("sin-extension")).isEmpty();
        assertThat(NombresArchivo.extension("termina.")).isEmpty();
    }
}
