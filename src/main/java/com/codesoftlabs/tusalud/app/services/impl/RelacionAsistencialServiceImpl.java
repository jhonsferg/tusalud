package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.repositories.CitaRepository;
import com.codesoftlabs.tusalud.app.repositories.IngresoHospitalarioRepository;
import com.codesoftlabs.tusalud.app.repositories.PacienteCompartidoRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.RelacionAsistencialService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RelacionAsistencialServiceImpl implements RelacionAsistencialService {

    private final CitaRepository citaRepository;
    private final PacienteCompartidoRepository compartidoRepository;
    private final IngresoHospitalarioRepository ingresoRepository;

    @Override
    public boolean tieneRelacion(UsuarioPrincipal medico, UUID pacienteId) {
        return citaRepository.existsRelacionMedicoPaciente(medico.getId(), pacienteId)
                || compartidoRepository.existsParaUsuario(pacienteId, medico.getId())
                || ingresoRepository.existsActivoDelMedico(medico.getId(), pacienteId);
    }

    @Override
    public void exigirRelacion(UsuarioPrincipal medico, UUID pacienteId, String mensaje) {
        if (!tieneRelacion(medico, pacienteId)) {
            throw new ForbiddenOperationException(mensaje);
        }
    }
}
