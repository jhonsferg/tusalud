package com.codesoftlabs.tusalud.app.dto.response;

import java.util.UUID;

public record SaldoVacacionesResponse(
        UUID empleadoId, int anio, int diasAnuales, int tomados, int pendientesAprobacion, int saldo) {}
