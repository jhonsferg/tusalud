package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.response.ReporteCalidadResponse;
import com.codesoftlabs.tusalud.app.dto.response.ReporteEjecutivoResponse;
import com.codesoftlabs.tusalud.app.dto.response.ReporteFinancieroResponse;
import com.codesoftlabs.tusalud.app.dto.response.ReporteOperativoResponse;
import com.codesoftlabs.tusalud.app.entities.enums.AgrupacionReporte;
import java.time.LocalDate;

public interface ReporteService {

    ReporteEjecutivoResponse ejecutivo(LocalDate desde, LocalDate hasta, AgrupacionReporte agrupacion);

    ReporteFinancieroResponse financiero(LocalDate desde, LocalDate hasta, AgrupacionReporte agrupacion);

    ReporteOperativoResponse operativo(LocalDate desde, LocalDate hasta, AgrupacionReporte agrupacion);

    ReporteCalidadResponse calidad(LocalDate desde, LocalDate hasta, AgrupacionReporte agrupacion);
}
