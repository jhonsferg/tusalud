package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.enums.AgrupacionReporte;
import java.time.LocalDate;

public record PeriodoReporte(LocalDate desde, LocalDate hasta, AgrupacionReporte agrupacion, int dias) {}
