package com.codesoftlabs.tusalud.app.services.pagos;

import com.codesoftlabs.tusalud.app.entities.IntencionPago;
import com.codesoftlabs.tusalud.app.entities.enums.Billetera;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class BilleteraSimulada implements ProveedorBilletera {

    @Override
    public boolean soporta(Billetera billetera) {
        return billetera == Billetera.YAPE || billetera == Billetera.PLIN;
    }

    @Override
    public String generarQr(IntencionPago intencion) {
        return "tusalud-pay://" + intencion.getBilletera().name().toLowerCase(Locale.ROOT) + "?codigo="
                + intencion.getCodigo() + "&monto=" + intencion.getTotal().toPlainString() + "&ref=CITA-"
                + intencion.getCita().getId();
    }

    @Override
    public boolean confirmar(IntencionPago intencion) {
        return true;
    }
}
