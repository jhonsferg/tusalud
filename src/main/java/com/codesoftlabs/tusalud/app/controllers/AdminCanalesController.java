package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.CanalesRequests.PlantillaMensajeRequest;
import com.codesoftlabs.tusalud.app.dto.response.CanalesResponses.MensajeSimuladoResponse;
import com.codesoftlabs.tusalud.app.dto.response.CanalesResponses.PlantillaMensajeResponse;
import com.codesoftlabs.tusalud.app.dto.response.CanalesResponses.PlantillasMensajeResponse;
import com.codesoftlabs.tusalud.app.dto.response.CanalesResponses.ProveedoresResponse;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.Canal;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.canales.EntregasService;
import com.codesoftlabs.tusalud.app.services.canales.PlantillasMensajeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/canales")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administración de canales", description = "Plantillas de mensajes y estado de los proveedores")
public class AdminCanalesController {

    private final PlantillasMensajeService plantillas;
    private final EntregasService entregas;

    @GetMapping("/plantillas")
    @Operation(
            summary = "Listar las plantillas de mensajes",
            description =
                    "Una por tipo de aviso y canal, con la versión vigente. Las no personalizadas devuelven la versión 0 predeterminada.")
    public ResponseEntity<PlantillasMensajeResponse> listar() {
        return ResponseEntity.ok(plantillas.listar());
    }

    @PutMapping("/plantillas/{tipo}/{canal}")
    @Operation(
            summary = "Publicar una versión nueva de una plantilla",
            description =
                    "Solo admite las variables nombre, enlace y centro. Los mensajes externos nunca llevan datos clínicos.")
    public ResponseEntity<PlantillaMensajeResponse> actualizar(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor,
            @PathVariable TipoNotificacion tipo,
            @PathVariable Canal canal,
            @Valid @RequestBody PlantillaMensajeRequest request) {
        return ResponseEntity.ok(plantillas.actualizar(actor, tipo, canal, request));
    }

    @DeleteMapping("/plantillas/{tipo}/{canal}")
    @Operation(summary = "Volver a la plantilla predeterminada")
    public ResponseEntity<PlantillaMensajeResponse> restaurar(
            @PathVariable TipoNotificacion tipo, @PathVariable Canal canal) {
        return ResponseEntity.ok(plantillas.restaurar(tipo, canal));
    }

    @GetMapping("/plantillas/{tipo}/{canal}/historial")
    @Operation(summary = "Historial de versiones de una plantilla")
    public ResponseEntity<List<PlantillaMensajeResponse>> historial(
            @PathVariable TipoNotificacion tipo, @PathVariable Canal canal) {
        return ResponseEntity.ok(plantillas.historial(tipo, canal));
    }

    @GetMapping("/proveedores")
    @Operation(summary = "Proveedor activo de cada canal y estado de la cola")
    public ResponseEntity<ProveedoresResponse> proveedores() {
        return ResponseEntity.ok(entregas.proveedores());
    }

    @GetMapping("/simulacion")
    @Operation(
            summary = "Mensajes del proveedor simulado",
            description =
                    "Solo hay mensajes cuando un canal usa el proveedor simulado, pensado para desarrollo y pruebas.")
    public ResponseEntity<List<MensajeSimuladoResponse>> simulacion(@RequestParam(defaultValue = "20") int limite) {
        return ResponseEntity.ok(entregas.mensajesSimulados(limite));
    }

    @DeleteMapping("/simulacion")
    @Operation(summary = "Vaciar la bandeja del proveedor simulado")
    public ResponseEntity<Void> vaciarSimulacion() {
        entregas.vaciarMensajesSimulados();
        return ResponseEntity.noContent().build();
    }
}
