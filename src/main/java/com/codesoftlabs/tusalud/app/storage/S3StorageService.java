package com.codesoftlabs.tusalud.app.storage;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Service
@ConditionalOnProperty(name = "app.storage.tipo", havingValue = "s3")
public class S3StorageService implements StorageService {

    private final S3Client cliente;
    private final S3Presigner firmador;
    private final boolean prefirmadoDisponible;

    public S3StorageService(S3Client cliente, S3Presigner firmador, StorageProperties propiedades) {
        this.cliente = cliente;
        this.firmador = firmador;
        this.prefirmadoDisponible = propiedades.endpointPublico() != null
                && !propiedades.endpointPublico().isBlank();
    }

    @Override
    public void guardar(String bucket, String clave, InputStream contenido, long tamano, String tipoContenido) {
        try {
            cliente.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(clave)
                            .contentType(tipoContenido)
                            .contentLength(tamano)
                            .build(),
                    RequestBody.fromInputStream(contenido, tamano));
        } catch (SdkException ex) {
            throw new AlmacenamientoException("No se pudo guardar el objeto en el almacén", ex);
        }
    }

    @Override
    public ObjetoAlmacenado abrir(String bucket, String clave) {
        return leer(GetObjectRequest.builder().bucket(bucket).key(clave).build());
    }

    @Override
    public ObjetoAlmacenado abrir(String bucket, String clave, long desde, long hasta) {
        return leer(GetObjectRequest.builder()
                .bucket(bucket)
                .key(clave)
                .range("bytes=" + desde + "-" + hasta)
                .build());
    }

    @Override
    public void eliminar(String bucket, String clave) {
        try {
            cliente.deleteObject(
                    DeleteObjectRequest.builder().bucket(bucket).key(clave).build());
        } catch (SdkException ex) {
            throw new AlmacenamientoException("No se pudo eliminar el objeto del almacén", ex);
        }
    }

    @Override
    public boolean existe(String bucket, String clave) {
        return metadatos(bucket, clave).isPresent();
    }

    @Override
    public Optional<MetadatosObjeto> metadatos(String bucket, String clave) {
        try {
            var respuesta = cliente.headObject(
                    HeadObjectRequest.builder().bucket(bucket).key(clave).build());
            return Optional.of(new MetadatosObjeto(respuesta.contentLength(), respuesta.contentType()));
        } catch (NoSuchKeyException ex) {
            return Optional.empty();
        } catch (S3Exception ex) {
            if (ex.statusCode() == 404) {
                return Optional.empty();
            }
            throw new AlmacenamientoException("No se pudo consultar el objeto del almacén", ex);
        } catch (SdkException ex) {
            throw new AlmacenamientoException("No se pudo consultar el objeto del almacén", ex);
        }
    }

    @Override
    public Optional<URI> urlPrefirmada(String bucket, String clave, Duration vigencia) {
        if (!prefirmadoDisponible) {
            return Optional.empty();
        }
        try {
            var solicitud = GetObjectPresignRequest.builder()
                    .signatureDuration(vigencia)
                    .getObjectRequest(
                            GetObjectRequest.builder().bucket(bucket).key(clave).build())
                    .build();
            return Optional.of(firmador.presignGetObject(solicitud).url().toURI());
        } catch (SdkException | java.net.URISyntaxException ex) {
            throw new AlmacenamientoException("No se pudo firmar la dirección del objeto", ex);
        }
    }

    private ObjetoAlmacenado leer(GetObjectRequest solicitud) {
        try {
            ResponseInputStream<GetObjectResponse> flujo = cliente.getObject(solicitud);
            GetObjectResponse respuesta = flujo.response();
            return new ObjetoAlmacenado(flujo, respuesta.contentLength(), respuesta.contentType());
        } catch (NoSuchKeyException ex) {
            throw new ObjetoNoEncontradoException("El objeto no existe en el almacén", ex);
        } catch (SdkException ex) {
            throw new AlmacenamientoException("No se pudo leer el objeto del almacén", ex);
        }
    }
}
