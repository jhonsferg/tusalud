package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.CanalesRequests.PreferenciasCanalesRequest;
import com.codesoftlabs.tusalud.app.dto.request.CanalesRequests.SolicitudContactoRequest;
import com.codesoftlabs.tusalud.app.dto.request.CanalesRequests.VerificacionContactoRequest;
import com.codesoftlabs.tusalud.app.dto.response.CanalesResponses.ContactoCanalResponse;
import com.codesoftlabs.tusalud.app.dto.response.CanalesResponses.MisCanalesResponse;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.Canal;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.canales.ContactosCanalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/canales")
@RequiredArgsConstructor
@Validated
@PreAuthorize("isAuthenticated()")
@Tag(name = "Canales de notificación", description = "Correo, SMS y WhatsApp del usuario autenticado")
public class CanalesController {

    private final ContactosCanalService servicio;

    @GetMapping("/mis-canales")
    @Operation(
            summary = "Mis canales y preferencias",
            description =
                    "Devuelve el estado de cada canal, las preferencias por tipo de aviso y la ventana de silencio. Los destinos van enmascarados.")
    public ResponseEntity<MisCanalesResponse> mis(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(servicio.mis(actor));
    }

    @PostMapping("/contactos/{canal}/solicitar")
    @Operation(
            summary = "Activar un canal",
            description =
                    "Registra el consentimiento explícito y envía un código de verificación al destino. El correo es siempre el de la cuenta.")
    public ResponseEntity<ContactoCanalResponse> solicitar(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor,
            @PathVariable Canal canal,
            @Valid @RequestBody SolicitudContactoRequest request) {
        return ResponseEntity.ok(servicio.solicitar(actor, canal, request));
    }

    @PostMapping("/contactos/{canal}/verificar")
    @Operation(summary = "Verificar un canal con el código recibido")
    public ResponseEntity<ContactoCanalResponse> verificar(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor,
            @PathVariable Canal canal,
            @Valid @RequestBody VerificacionContactoRequest request) {
        return ResponseEntity.ok(servicio.verificar(actor, canal, request.codigo()));
    }

    @DeleteMapping("/contactos/{canal}")
    @Operation(
            summary = "Dar de baja un canal",
            description = "Revoca el consentimiento y detiene los envíos por ese canal.")
    public ResponseEntity<ContactoCanalResponse> revocar(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor, @PathVariable Canal canal) {
        return ResponseEntity.ok(servicio.revocar(actor, canal));
    }

    @PutMapping("/preferencias")
    @Operation(
            summary = "Guardar mis preferencias de canales",
            description =
                    "Activa o desactiva cada tipo de aviso por canal y define la ventana de silencio. Las alertas de seguridad no se pueden desactivar.")
    public ResponseEntity<MisCanalesResponse> preferencias(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor,
            @Valid @RequestBody PreferenciasCanalesRequest request) {
        return ResponseEntity.ok(servicio.actualizarPreferencias(actor, request));
    }
}
