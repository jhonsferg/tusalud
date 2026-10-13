package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.request.HorarioRequest;
import com.codesoftlabs.tusalud.app.dto.request.MedicoCreateRequest;
import com.codesoftlabs.tusalud.app.dto.response.HorarioResponse;
import com.codesoftlabs.tusalud.app.dto.response.MedicoResponse;
import java.util.List;

public interface MedicoService {

    List<MedicoResponse> listar(Long especialidadId);

    MedicoResponse obtener(Long id);

    MedicoResponse crear(MedicoCreateRequest request);

    List<HorarioResponse> listarHorarios(Long medicoId);

    HorarioResponse agregarHorario(Long medicoId, HorarioRequest request);

    void eliminarHorario(Long medicoId, Long horarioId);
}
