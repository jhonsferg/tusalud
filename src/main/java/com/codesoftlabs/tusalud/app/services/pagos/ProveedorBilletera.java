package com.codesoftlabs.tusalud.app.services.pagos;

import com.codesoftlabs.tusalud.app.entities.IntencionPago;
import com.codesoftlabs.tusalud.app.entities.enums.Billetera;

public interface ProveedorBilletera {

    boolean soporta(Billetera billetera);

    String generarQr(IntencionPago intencion);

    boolean confirmar(IntencionPago intencion);
}
