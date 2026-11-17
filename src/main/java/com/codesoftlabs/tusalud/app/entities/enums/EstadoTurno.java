package com.codesoftlabs.tusalud.app.entities.enums;

import java.util.EnumSet;
import java.util.Set;

public enum EstadoTurno {
    EN_ESPERA,
    LLAMADO,
    EN_ATENCION,
    ATENDIDO,
    AUSENTE,
    CANCELADO;

    public static final Set<EstadoTurno> ABIERTOS = EnumSet.of(EN_ESPERA, LLAMADO, EN_ATENCION);
}
