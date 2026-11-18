package com.codesoftlabs.tusalud.app.documentos;

import java.util.UUID;

public record DocumentoPreparado(FichaDocumento ficha, UUID pacienteId, String referencia, String nombreBase) {}
