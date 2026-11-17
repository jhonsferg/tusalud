package com.codesoftlabs.tusalud.app.repositories;

import java.time.LocalDate;
import java.util.UUID;

public interface StockProyeccion {

    UUID getProductoId();

    Long getStock();

    LocalDate getProximo();
}
