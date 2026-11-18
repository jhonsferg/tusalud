package com.codesoftlabs.tusalud.app.storage;

import java.net.URI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@ConditionalOnProperty(name = "app.storage.tipo", havingValue = "s3")
public class StorageConfig {

    @Bean(destroyMethod = "close")
    S3Client clienteS3(StorageProperties propiedades) {
        exigirConfiguracion(propiedades);
        return S3Client.builder()
                .endpointOverride(URI.create(propiedades.endpoint()))
                .region(Region.of(propiedades.region()))
                .credentialsProvider(credenciales(propiedades))
                .forcePathStyle(propiedades.pathStyle())
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .build();
    }

    @Bean(destroyMethod = "close")
    S3Presigner firmadorS3(StorageProperties propiedades) {
        exigirConfiguracion(propiedades);
        String publico = propiedades.endpointPublico();
        String endpoint = publico == null || publico.isBlank() ? propiedades.endpoint() : publico;
        return S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(propiedades.region()))
                .credentialsProvider(credenciales(propiedades))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(propiedades.pathStyle())
                        .build())
                .build();
    }

    private static StaticCredentialsProvider credenciales(StorageProperties propiedades) {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(propiedades.accessKey(), propiedades.secretKey()));
    }

    private static void exigirConfiguracion(StorageProperties propiedades) {
        if (vacio(propiedades.endpoint()) || vacio(propiedades.accessKey()) || vacio(propiedades.secretKey())) {
            throw new IllegalStateException(
                    "El almacén S3 exige STORAGE_ENDPOINT, STORAGE_ACCESS_KEY y STORAGE_SECRET_KEY");
        }
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
