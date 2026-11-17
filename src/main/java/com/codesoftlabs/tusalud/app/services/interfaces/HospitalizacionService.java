package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.request.AltaHospRequest;
import com.codesoftlabs.tusalud.app.dto.request.CamaAsignacionRequest;
import com.codesoftlabs.tusalud.app.dto.request.CamaEstadoRequest;
import com.codesoftlabs.tusalud.app.dto.request.CamaRequest;
import com.codesoftlabs.tusalud.app.dto.request.EmergenciaRequest;
import com.codesoftlabs.tusalud.app.dto.request.EvolucionHospRequest;
import com.codesoftlabs.tusalud.app.dto.request.IngresoHospCreateRequest;
import com.codesoftlabs.tusalud.app.dto.response.CamaResponse;
import com.codesoftlabs.tusalud.app.dto.response.EvolucionHospResponse;
import com.codesoftlabs.tusalud.app.dto.response.HospResumenResponse;
import com.codesoftlabs.tusalud.app.dto.response.IngresoHospResponse;
import com.codesoftlabs.tusalud.app.dto.response.MapaCamasResponse;
import com.codesoftlabs.tusalud.app.entities.enums.AreaCama;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCama;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoIngresoHosp;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.util.List;
import java.util.UUID;

public interface HospitalizacionService {

    List<CamaResponse> listarCamas(AreaCama area, EstadoCama estado);

    CamaResponse crearCama(CamaRequest request);

    CamaResponse actualizarCama(UUID id, CamaRequest request);

    CamaResponse cambiarEstadoCama(UUID id, CamaEstadoRequest request);

    IngresoHospResponse crearIngreso(UsuarioPrincipal actor, IngresoHospCreateRequest request);

    List<IngresoHospResponse> listarIngresos(EstadoIngresoHosp estado, String busqueda);

    IngresoHospResponse obtenerIngreso(UsuarioPrincipal actor, UUID id);

    IngresoHospResponse trasladar(UUID id, CamaAsignacionRequest request);

    IngresoHospResponse asignarCama(UUID id, CamaAsignacionRequest request);

    IngresoHospResponse darAlta(UUID id, AltaHospRequest request);

    IngresoHospResponse cancelarIngreso(UUID id);

    EvolucionHospResponse registrarEvolucion(UsuarioPrincipal actor, UUID ingresoId, EvolucionHospRequest request);

    List<EvolucionHospResponse> listarEvoluciones(UsuarioPrincipal actor, UUID ingresoId);

    IngresoHospResponse registrarEmergencia(UsuarioPrincipal actor, EmergenciaRequest request);

    List<IngresoHospResponse> listarEmergencias();

    MapaCamasResponse mapa();

    HospResumenResponse resumen();

    List<IngresoHospResponse> misIngresos(UsuarioPrincipal actor);
}
