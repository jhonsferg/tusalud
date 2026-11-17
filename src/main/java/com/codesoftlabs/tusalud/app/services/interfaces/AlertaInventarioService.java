package com.codesoftlabs.tusalud.app.services.interfaces;

import java.time.LocalDateTime;

public interface AlertaInventarioService {

    int notificarAlertas(LocalDateTime ahora);
}
