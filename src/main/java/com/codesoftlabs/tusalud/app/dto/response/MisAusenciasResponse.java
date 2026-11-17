package com.codesoftlabs.tusalud.app.dto.response;

import java.util.List;

public record MisAusenciasResponse(SaldoVacacionesResponse saldo, List<AusenciaResponse> ausencias) {}
