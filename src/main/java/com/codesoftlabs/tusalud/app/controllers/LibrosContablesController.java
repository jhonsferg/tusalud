package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.BalanceComprobacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.EstadoResultadosResponse;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.EstadoSituacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.FilaDiario;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.LibroElectronico;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.LibroMayorResponse;
import com.codesoftlabs.tusalud.app.services.contabilidad.LibrosContablesService;
import com.codesoftlabs.tusalud.app.services.contabilidad.LibrosElectronicosService;
import com.codesoftlabs.tusalud.app.services.contabilidad.LibrosElectronicosService.Libro;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/contabilidad/libros")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Contabilidad - Libros",
        description = "Mayor, balance de comprobación, estados financieros y libros electrónicos")
public class LibrosContablesController {

    private static final String CONSULTA = "hasAnyRole('FINANZAS', 'GERENCIA')";

    private final LibrosContablesService libros;
    private final LibrosElectronicosService electronicos;

    @GetMapping("/diario")
    @PreAuthorize(CONSULTA)
    public ResponseEntity<List<FilaDiario>> diario(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(libros.diario(desde, hasta));
    }

    @GetMapping("/mayor/{cuentaId}")
    @PreAuthorize(CONSULTA)
    public ResponseEntity<LibroMayorResponse> mayor(
            @PathVariable UUID cuentaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(libros.mayor(cuentaId, desde, hasta));
    }

    @GetMapping("/balance-comprobacion")
    @PreAuthorize(CONSULTA)
    public ResponseEntity<BalanceComprobacionResponse> balance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(libros.balanceComprobacion(desde, hasta));
    }

    @GetMapping("/estado-situacion")
    @PreAuthorize(CONSULTA)
    public ResponseEntity<EstadoSituacionResponse> situacion(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate al) {
        return ResponseEntity.ok(libros.estadoSituacion(al));
    }

    @GetMapping("/estado-resultados")
    @PreAuthorize(CONSULTA)
    public ResponseEntity<EstadoResultadosResponse> resultados(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(libros.estadoResultados(desde, hasta));
    }

    @GetMapping("/electronicos/{libro}")
    @PreAuthorize(CONSULTA)
    public ResponseEntity<LibroElectronico> electronico(
            @PathVariable Libro libro,
            @RequestParam @Min(2000) @Max(2100) int anio,
            @RequestParam @Min(1) @Max(12) int mes) {
        return ResponseEntity.ok(electronicos.generar(libro, anio, mes));
    }
}
