package com.codesoftlabs.tusalud.app.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

class S3StorageServiceTest {

    private S3Client cliente;
    private S3Presigner firmador;

    private S3StorageService servicio(String endpointPublico) {
        return new S3StorageService(
                cliente,
                firmador,
                new StorageProperties(
                        StorageProperties.Tipo.S3,
                        "http://storage:8333",
                        endpointPublico,
                        "us-east-1",
                        "k",
                        "s",
                        true,
                        null,
                        new StorageProperties.Buckets("a", "b", "c", "d", "e"),
                        Duration.ofMinutes(5)));
    }

    @BeforeEach
    void setUp() {
        cliente = mock(S3Client.class);
        firmador = mock(S3Presigner.class);
    }

    @Test
    void guardaConElTipoYElTamanoIndicados() {
        byte[] bytes = {1, 2, 3};

        servicio(null).guardar("imagenes", "clave-1", new ByteArrayInputStream(bytes), 3, "image/png");

        ArgumentCaptor<PutObjectRequest> solicitud = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(cliente).putObject(solicitud.capture(), any(RequestBody.class));
        assertThat(solicitud.getValue().bucket()).isEqualTo("imagenes");
        assertThat(solicitud.getValue().key()).isEqualTo("clave-1");
        assertThat(solicitud.getValue().contentType()).isEqualTo("image/png");
        assertThat(solicitud.getValue().contentLength()).isEqualTo(3);
    }

    @Test
    void conviertePermisosYCaidasEnErroresDeAlmacenamiento() {
        when(cliente.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(SdkClientException.create("sin conexión"));

        assertThatThrownBy(() -> servicio(null)
                        .guardar("imagenes", "k", new ByteArrayInputStream(new byte[] {1}), 1, "image/png"))
                .isInstanceOf(AlmacenamientoException.class);
    }

    @Test
    void abreUnObjetoConSuLongitudYTipo() throws Exception {
        GetObjectResponse respuesta = GetObjectResponse.builder()
                .contentLength(3L)
                .contentType("application/pdf")
                .build();
        when(cliente.getObject(any(GetObjectRequest.class)))
                .thenReturn(new ResponseInputStream<>(
                        respuesta, AbortableInputStream.create(new ByteArrayInputStream(new byte[] {7, 8, 9}))));

        try (var objeto = servicio(null).abrir("documentos", "k")) {
            assertThat(objeto.flujo().readAllBytes()).containsExactly(7, 8, 9);
            assertThat(objeto.longitud()).isEqualTo(3);
            assertThat(objeto.tipoContenido()).isEqualTo("application/pdf");
        }
    }

    @Test
    void pideElRangoExactoAlAlmacen() throws Exception {
        GetObjectResponse respuesta =
                GetObjectResponse.builder().contentLength(2L).build();
        when(cliente.getObject(any(GetObjectRequest.class)))
                .thenReturn(new ResponseInputStream<>(
                        respuesta, AbortableInputStream.create(new ByteArrayInputStream(new byte[] {1, 2}))));

        servicio(null).abrir("documentos", "k", 10, 11).close();

        ArgumentCaptor<GetObjectRequest> solicitud = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(cliente).getObject(solicitud.capture());
        assertThat(solicitud.getValue().range()).isEqualTo("bytes=10-11");
    }

    @Test
    void distingueUnObjetoAusenteDeUnaFalla() {
        when(cliente.getObject(any(GetObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().build());

        assertThatThrownBy(() -> servicio(null).abrir("documentos", "k"))
                .isInstanceOf(ObjetoNoEncontradoException.class);
    }

    @Test
    void consultaLosMetadatosYTratalo404ComoAusente() {
        when(cliente.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder()
                        .contentLength(42L)
                        .contentType("image/png")
                        .build());
        assertThat(servicio(null).metadatos("imagenes", "k"))
                .hasValueSatisfying(meta -> assertThat(meta.tamano()).isEqualTo(42));

        when(cliente.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(404).build());
        assertThat(servicio(null).metadatos("imagenes", "x")).isEmpty();
        assertThat(servicio(null).existe("imagenes", "x")).isFalse();

        when(cliente.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(403).build());
        assertThatThrownBy(() -> servicio(null).metadatos("imagenes", "x")).isInstanceOf(AlmacenamientoException.class);
    }

    @Test
    void eliminaElObjetoYReportaLasFallas() {
        servicio(null).eliminar("reportes", "k");
        verify(cliente).deleteObject(Mockito.any(software.amazon.awssdk.services.s3.model.DeleteObjectRequest.class));

        when(cliente.deleteObject(Mockito.any(software.amazon.awssdk.services.s3.model.DeleteObjectRequest.class)))
                .thenThrow(AwsServiceException.builder().message("denegado").build());
        assertThatThrownBy(() -> servicio(null).eliminar("reportes", "k")).isInstanceOf(AlmacenamientoException.class);
    }

    @Test
    void laUrlPrefirmadaSoloExisteConEndpointPublico() throws Exception {
        assertThat(servicio(null).urlPrefirmada("documentos", "k", Duration.ofMinutes(1)))
                .isEmpty();

        PresignedGetObjectRequest firmada = mock(PresignedGetObjectRequest.class);
        when(firmada.url()).thenReturn(new URL("https://archivos.ejemplo.pe/documentos/k?X-Amz-Signature=abc"));
        when(firmador.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(firmada);

        assertThat(servicio("https://archivos.ejemplo.pe").urlPrefirmada("documentos", "k", Duration.ofMinutes(1)))
                .hasValue(URI.create("https://archivos.ejemplo.pe/documentos/k?X-Amz-Signature=abc"));
    }
}
