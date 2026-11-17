package com.codesoftlabs.tusalud.app.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Historia clínica del paciente con sus atenciones, las más recientes primero")
public record HistoriaClinicaResponse(PacienteHistoriaResponse paciente, List<AtencionResponse> atenciones) {}
