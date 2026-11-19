package com.codesoftlabs.tusalud.app.canales;

import com.codesoftlabs.tusalud.app.config.CanalesProperties;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.Canal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CanalesConfig {

    @Bean
    public CanalNotificacion canalSms(CanalesProperties propiedades, ProveedorSimulado simulado) {
        return new CanalMensajeria(Canal.SMS, proveedor(propiedades.sms(), simulado));
    }

    @Bean
    public CanalNotificacion canalWhatsapp(CanalesProperties propiedades, ProveedorSimulado simulado) {
        return new CanalMensajeria(Canal.WHATSAPP, proveedor(propiedades.whatsapp(), simulado));
    }

    private static ProveedorMensajeria proveedor(
            CanalesProperties.Mensajeria configuracion, ProveedorSimulado simulado) {
        return configuracion.simulado() ? simulado : new ProveedorHttpGenerico(configuracion.http());
    }
}
