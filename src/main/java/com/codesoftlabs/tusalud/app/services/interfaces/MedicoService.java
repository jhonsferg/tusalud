package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.request.HorarioRequest;
import com.codesoftlabs.tusalud.app.dto.request.MedicoCreateRequest;
import com.codesoftlabs.tusalud.app.dto.request.MedicoEspecialidadesRequest;
import com.codesoftlabs.tusalud.app.dto.response.HorarioResponse;
import com.codesoftlabs.tusalud.app.dto.response.MedicoResponse;
import java.util.List;
import java.util.UUID;

public interface MedicoService {

    List<MedicoResponse> listar(UUID especialidadId);

    MedicoResponse obtener(UUID id);

    MedicoResponse crear(MedicoCreateRequest request);

    MedicoResponse actualizarEspecialidades(UUID medicoId, MedicoEspecialidadesRequest request);

    List<HorarioResponse> listarHorarios(UUID medicoId);

    HorarioResponse agregarHorario(UUID medicoId, HorarioRequest request);

    void eliminarHorario(UUID medicoId, UUID horarioId);
}
