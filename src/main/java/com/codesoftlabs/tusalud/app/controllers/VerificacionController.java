package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.response.DocumentosMedicosResponses.VerificacionPublicaResponse;
import com.codesoftlabs.tusalud.app.security.ContextoConexion;
import com.codesoftlabs.tusalud.app.services.documentosmedicos.VerificacionPublicaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/verificacion")
@RequiredArgsConstructor
@Tag(name = "Verificación pública", description = "Comprueba la validez de un documento sin exponer datos del paciente")
public class VerificacionController {

    private final VerificacionPublicaService servicio;

    @GetMapping("/{codigo}")
    @Operation(
            summary = "Verificar un documento por su código",
            description =
                    "Responde si el código es válido o anulado, con el tipo, la fecha, el médico y su colegiatura."
                            + " Nunca incluye datos del paciente. Los códigos inexistentes devuelven la misma forma de respuesta.")
    public ResponseEntity<VerificacionPublicaResponse> verificar(
            @PathVariable String codigo, HttpServletRequest request) {
        VerificacionPublicaResponse respuesta =
                servicio.verificar(ContextoConexion.desde(request).ip(), codigo);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(respuesta);
    }
}
