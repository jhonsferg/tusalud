package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.entities.AuditoriaClinica;
import com.codesoftlabs.tusalud.app.repositories.AuditoriaClinicaRepository;
import com.codesoftlabs.tusalud.app.repositories.PacienteRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.AuditoriaClinicaService;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditoriaClinicaServiceImpl implements AuditoriaClinicaService {

    private final AuditoriaClinicaRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final PacienteRepository pacienteRepository;

    @Override
    @Transactional
    public void registrar(UsuarioPrincipal actor, UUID pacienteId, String accion, String detalle) {
        repository.save(AuditoriaClinica.builder()
                .usuario(usuarioRepository.getReferenceById(actor.getId()))
                .paciente(pacienteRepository.getReferenceById(pacienteId))
                .accion(accion)
                .detalle(detalle)
                .fecha(LocalDateTime.now())
                .build());
    }
}
