package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.Canal;
import com.codesoftlabs.tusalud.app.services.canales.BajaCanalService;
import com.codesoftlabs.tusalud.app.services.canales.WebhookCanalesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/canales")
@RequiredArgsConstructor
@Tag(name = "Canales públicos", description = "Baja por enlace firmado y eventos de estado de los proveedores")
public class CanalesPublicoController {

    private final BajaCanalService baja;
    private final WebhookCanalesService webhook;

    @PostMapping("/baja")
    @Operation(
            summary = "Darse de baja de un canal desde el enlace de un mensaje",
            description = "No requiere sesión. El enlace va firmado y la operación es idempotente.")
    public ResponseEntity<Map<String, String>> baja(@RequestParam("t") String token) {
        Canal canal = baja.baja(token);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(Map.of("canal", canal.name(), "etiqueta", canal.etiqueta()));
    }

    @PostMapping("/webhook/{canal}")
    @Operation(
            summary = "Recibir el estado de entrega de un proveedor",
            description =
                    "Requiere las cabeceras X-Marca-Tiempo y X-Firma con el formato sha256= y el HMAC de marca de tiempo, punto y cuerpo.")
    public ResponseEntity<Void> webhook(
            @PathVariable Canal canal,
            @RequestHeader(value = "X-Marca-Tiempo", required = false) String marcaTiempo,
            @RequestHeader(value = "X-Firma", required = false) String firma,
            @RequestBody byte[] cuerpo) {
        webhook.procesar(canal, marcaTiempo, firma, cuerpo);
        return ResponseEntity.noContent().build();
    }
}
