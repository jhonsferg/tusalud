package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.request.CancelarOrdenLabRequest;
import com.codesoftlabs.tusalud.app.dto.request.ExamenLabRequest;
import com.codesoftlabs.tusalud.app.dto.request.OrdenLabCreateRequest;
import com.codesoftlabs.tusalud.app.dto.request.ResultadosLabRequest;
import com.codesoftlabs.tusalud.app.dto.response.ExamenLabResponse;
import com.codesoftlabs.tusalud.app.dto.response.LabResumenResponse;
import com.codesoftlabs.tusalud.app.dto.response.OrdenLabResponse;
import com.codesoftlabs.tusalud.app.entities.enums.CategoriaExamen;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoOrdenLab;
import com.codesoftlabs.tusalud.app.entities.enums.PrioridadOrden;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.util.List;
import java.util.UUID;

public interface LaboratorioService {

    List<ExamenLabResponse> listarExamenes(String busqueda, CategoriaExamen categoria, Boolean activo);

    ExamenLabResponse obtenerExamen(UUID id);

    ExamenLabResponse crearExamen(ExamenLabRequest request);

    ExamenLabResponse actualizarExamen(UUID id, ExamenLabRequest request);

    OrdenLabResponse crearOrden(UsuarioPrincipal actor, OrdenLabCreateRequest request);

    List<OrdenLabResponse> listarOrdenes(
            UsuarioPrincipal actor, EstadoOrdenLab estado, String busqueda, PrioridadOrden prioridad);

    OrdenLabResponse obtenerOrden(UsuarioPrincipal actor, UUID id);

    OrdenLabResponse tomarMuestra(UsuarioPrincipal actor, UUID ordenId);

    OrdenLabResponse registrarResultados(
            UsuarioPrincipal actor, UUID ordenId, UUID itemId, ResultadosLabRequest request);

    OrdenLabResponse validar(UsuarioPrincipal actor, UUID ordenId);

    OrdenLabResponse cancelar(UsuarioPrincipal actor, UUID ordenId, CancelarOrdenLabRequest request);

    List<OrdenLabResponse> misOrdenes(UsuarioPrincipal actor);

    LabResumenResponse resumen();
}
