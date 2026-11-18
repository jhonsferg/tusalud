package com.codesoftlabs.tusalud.app.documentos;

import java.util.List;

public record ReporteArmado(FichaDocumento ficha, List<HojaExcel> hojas, String titulo, String nombreBase) {}
