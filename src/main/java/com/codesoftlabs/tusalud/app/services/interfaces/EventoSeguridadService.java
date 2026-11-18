package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.response.PaginaEventosSeguridadResponse;
import com.codesoftlabs.tusalud.app.dto.response.ResumenSeguridadResponse;
import com.codesoftlabs.tusalud.app.entities.enums.SeveridadSeguridad;
import com.codesoftlabs.tusalud.app.entities.enums.TipoEventoSeguridad;
import com.codesoftlabs.tusalud.app.security.ContextoConexion;
import java.time.LocalDate;
import java.util.UUID;

public interface EventoSeguridadService {

    void registrar(TipoEventoSeguridad tipo, UUID usuarioId, UUID actorId, String detalle);

    void registrar(TipoEventoSeguridad tipo, UUID usuarioId, UUID actorId, String detalle, ContextoConexion contexto);

    void registrarLoginFallido(UUID usuarioId, String detalle, ContextoConexion contexto);

    void registrarAccesoDenegado(String metodo, String ruta);

    void registrarDescarga(UUID usuarioId);

    PaginaEventosSeguridadResponse listar(
            TipoEventoSeguridad tipo,
            SeveridadSeguridad severidad,
            LocalDate desde,
            LocalDate hasta,
            UUID usuarioId,
            int pagina,
            int tamano);

    ResumenSeguridadResponse resumen();

    int purgar();
}
