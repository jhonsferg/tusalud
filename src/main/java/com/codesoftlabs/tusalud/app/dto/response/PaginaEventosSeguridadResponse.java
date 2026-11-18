package com.codesoftlabs.tusalud.app.dto.response;

import java.util.List;

public record PaginaEventosSeguridadResponse(
        List<EventoSeguridadResponse> contenido, int pagina, int tamano, long total, int totalPaginas) {}
