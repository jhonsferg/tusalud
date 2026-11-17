package com.codesoftlabs.tusalud.app.repositories;

import java.time.LocalDate;

public interface StockProyeccion {

    Long getProductoId();

    Long getStock();

    LocalDate getProximo();
}
