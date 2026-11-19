package com.codesoftlabs.tusalud.app.canales;

import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.Canal;

public interface CanalNotificacion {

    Canal canal();

    String enviar(MensajeSaliente mensaje);
}
