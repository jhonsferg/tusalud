package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.dto.response.EstadoSistemaResponse;
import com.codesoftlabs.tusalud.app.dto.response.EstadoSistemaResponse.ComponenteEstado;
import com.codesoftlabs.tusalud.app.dto.response.EstadoSistemaResponse.Migraciones;
import com.codesoftlabs.tusalud.app.dto.response.UsoAlmacenamientoResponse;
import com.codesoftlabs.tusalud.app.dto.response.UsoAlmacenamientoResponse.UsoBucket;
import com.codesoftlabs.tusalud.app.dto.response.UsoAlmacenamientoResponse.UsoProposito;
import com.codesoftlabs.tusalud.app.repositories.ArchivoRepository;
import com.codesoftlabs.tusalud.app.services.interfaces.SistemaService;
import com.codesoftlabs.tusalud.app.storage.StorageProperties;
import com.codesoftlabs.tusalud.app.storage.StorageService;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class SistemaServiceImpl implements SistemaService {

    private final JdbcTemplate jdbc;
    private final StorageService almacen;
    private final StorageProperties propiedades;
    private final ArchivoRepository archivoRepository;
    private final String version;

    public SistemaServiceImpl(
            JdbcTemplate jdbc,
            StorageService almacen,
            StorageProperties propiedades,
            ArchivoRepository archivoRepository,
            @Value("${app.version:desarrollo}") String version) {
        this.jdbc = jdbc;
        this.almacen = almacen;
        this.propiedades = propiedades;
        this.archivoRepository = archivoRepository;
        this.version = version;
    }

    @Override
    public EstadoSistemaResponse estado() {
        List<ComponenteEstado> componentes = new ArrayList<>();
        componentes.add(comprobarBaseDatos());
        componentes.add(comprobarAlmacen());
        boolean todoBien = componentes.stream().allMatch(c -> "OK".equals(c.estado()));
        long inicioMs = ManagementFactory.getRuntimeMXBean().getStartTime();
        LocalDateTime iniciadoEn = LocalDateTime.ofInstant(Instant.ofEpochMilli(inicioMs), ZoneId.systemDefault());
        long segundos = Math.max(0, (System.currentTimeMillis() - inicioMs) / 1000);
        return new EstadoSistemaResponse(
                todoBien ? "OK" : "DEGRADADO", version, iniciadoEn, segundos, componentes, migraciones());
    }

    @Override
    @Transactional(readOnly = true)
    public UsoAlmacenamientoResponse usoAlmacenamiento() {
        Map<String, List<UsoProposito>> propositos = new LinkedHashMap<>();
        Map<String, long[]> totales = new LinkedHashMap<>();
        long objetos = 0;
        long bytes = 0;
        for (Object[] fila : archivoRepository.usoPorBucketYProposito()) {
            String bucket = (String) fila[0];
            String proposito = fila[1].toString();
            long cantidad = (Long) fila[2];
            long tamano = ((Number) fila[3]).longValue();
            propositos
                    .computeIfAbsent(bucket, clave -> new ArrayList<>())
                    .add(new UsoProposito(proposito, cantidad, tamano));
            long[] acumulado = totales.computeIfAbsent(bucket, clave -> new long[2]);
            acumulado[0] += cantidad;
            acumulado[1] += tamano;
            objetos += cantidad;
            bytes += tamano;
        }
        List<UsoBucket> buckets = new ArrayList<>();
        for (Map.Entry<String, long[]> entrada : totales.entrySet()) {
            buckets.add(new UsoBucket(
                    entrada.getKey(), entrada.getValue()[0], entrada.getValue()[1], propositos.get(entrada.getKey())));
        }
        return new UsoAlmacenamientoResponse(objetos, bytes, buckets);
    }

    private ComponenteEstado comprobarBaseDatos() {
        long inicio = System.nanoTime();
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
            return new ComponenteEstado("Base de datos", "OK", milisDesde(inicio), null);
        } catch (RuntimeException ex) {
            log.warn(
                    "La comprobación de la base de datos falló: {}",
                    ex.getClass().getSimpleName());
            return new ComponenteEstado("Base de datos", "CAIDO", milisDesde(inicio), "Sin respuesta");
        }
    }

    private ComponenteEstado comprobarAlmacen() {
        long inicio = System.nanoTime();
        try {
            almacen.existe(propiedades.buckets().avatares(), "sistema-salud");
            return new ComponenteEstado("Almacén de objetos", "OK", milisDesde(inicio), null);
        } catch (RuntimeException ex) {
            log.warn("La comprobación del almacén falló: {}", ex.getClass().getSimpleName());
            return new ComponenteEstado("Almacén de objetos", "CAIDO", milisDesde(inicio), "Sin respuesta");
        }
    }

    private Migraciones migraciones() {
        try {
            Long aplicadas =
                    jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success = true", Long.class);
            List<String> ultima = jdbc.queryForList(
                    "SELECT version FROM flyway_schema_history WHERE success = true AND version IS NOT NULL"
                            + " ORDER BY installed_rank DESC LIMIT 1",
                    String.class);
            return new Migraciones(true, aplicadas == null ? 0 : aplicadas, ultima.isEmpty() ? null : ultima.get(0));
        } catch (RuntimeException ex) {
            return new Migraciones(false, 0, null);
        }
    }

    private static long milisDesde(long inicioNanos) {
        return (System.nanoTime() - inicioNanos) / 1_000_000;
    }
}
