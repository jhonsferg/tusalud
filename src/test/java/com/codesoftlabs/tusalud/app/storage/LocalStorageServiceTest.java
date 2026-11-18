package com.codesoftlabs.tusalud.app.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.storage.StorageService.ObjetoAlmacenado;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalStorageServiceTest {

    @TempDir
    Path directorio;

    private LocalStorageService almacen;

    @BeforeEach
    void setUp() {
        almacen = new LocalStorageService(new StorageProperties(
                StorageProperties.Tipo.LOCAL,
                null,
                null,
                "us-east-1",
                null,
                null,
                true,
                directorio.toString(),
                new StorageProperties.Buckets("avatares", "imagenes", "documentos", "reportes", "plantillas"),
                Duration.ofMinutes(5)));
    }

    private void guardar(String clave, String texto) {
        byte[] bytes = texto.getBytes(StandardCharsets.UTF_8);
        almacen.guardar("documentos", clave, new ByteArrayInputStream(bytes), bytes.length, "application/pdf");
    }

    @Test
    void guardaYLeeUnObjetoConSuTipo() throws IOException {
        guardar("abc-123", "contenido de prueba");

        try (ObjetoAlmacenado objeto = almacen.abrir("documentos", "abc-123")) {
            assertThat(new String(objeto.flujo().readAllBytes(), StandardCharsets.UTF_8))
                    .isEqualTo("contenido de prueba");
            assertThat(objeto.tipoContenido()).isEqualTo("application/pdf");
            assertThat(objeto.longitud()).isEqualTo(19);
        }
        assertThat(almacen.existe("documentos", "abc-123")).isTrue();
        assertThat(almacen.metadatos("documentos", "abc-123"))
                .hasValueSatisfying(meta -> assertThat(meta.tamano()).isEqualTo(19));
    }

    @Test
    void sirveRangosDeBytes() throws IOException {
        guardar("rango", "0123456789");

        try (ObjetoAlmacenado objeto = almacen.abrir("documentos", "rango", 2, 5)) {
            assertThat(new String(objeto.flujo().readAllBytes(), StandardCharsets.UTF_8))
                    .isEqualTo("2345");
            assertThat(objeto.longitud()).isEqualTo(4);
        }
        try (ObjetoAlmacenado objeto = almacen.abrir("documentos", "rango", 7, 500)) {
            assertThat(new String(objeto.flujo().readAllBytes(), StandardCharsets.UTF_8))
                    .isEqualTo("789");
        }
        assertThatThrownBy(() -> almacen.abrir("documentos", "rango", 50, 60))
                .isInstanceOf(AlmacenamientoException.class);
    }

    @Test
    void eliminaYReportaLosObjetosAusentes() {
        guardar("borrar", "x");

        almacen.eliminar("documentos", "borrar");

        assertThat(almacen.existe("documentos", "borrar")).isFalse();
        assertThat(almacen.metadatos("documentos", "borrar")).isEmpty();
        assertThatThrownBy(() -> almacen.abrir("documentos", "borrar")).isInstanceOf(ObjetoNoEncontradoException.class);
    }

    @Test
    void rechazaClavesConRecorridoDeDirectorio() {
        assertThatThrownBy(() -> guardar("../fuera", "x")).isInstanceOf(AlmacenamientoException.class);
        assertThatThrownBy(() -> guardar("a/b", "x")).isInstanceOf(AlmacenamientoException.class);
        assertThatThrownBy(() -> guardar("..", "x")).isInstanceOf(AlmacenamientoException.class);
        assertThatThrownBy(() -> almacen.abrir("../otro", "abc")).isInstanceOf(AlmacenamientoException.class);
        assertThatThrownBy(() -> almacen.existe("documentos", "a\\b")).isInstanceOf(AlmacenamientoException.class);
    }

    @Test
    void noOfreceUrlsPrefirmadas() {
        assertThat(almacen.urlPrefirmada("documentos", "abc", Duration.ofMinutes(1)))
                .isEmpty();
    }
}
