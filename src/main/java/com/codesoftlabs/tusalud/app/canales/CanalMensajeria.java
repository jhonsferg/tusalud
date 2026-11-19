package com.codesoftlabs.tusalud.app.canales;

import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.Canal;

public class CanalMensajeria implements CanalNotificacion {

    private final Canal canal;
    private final ProveedorMensajeria proveedor;

    public CanalMensajeria(Canal canal, ProveedorMensajeria proveedor) {
        this.canal = canal;
        this.proveedor = proveedor;
    }

    @Override
    public Canal canal() {
        return canal;
    }

    @Override
    public String enviar(MensajeSaliente mensaje) {
        return proveedor.enviar(mensaje);
    }
}
