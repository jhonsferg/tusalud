package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.entities.AuditoriaRrhh;
import com.codesoftlabs.tusalud.app.repositories.AuditoriaRrhhRepository;
import com.codesoftlabs.tusalud.app.repositories.EmpleadoRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.AuditoriaRrhhService;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditoriaRrhhServiceImpl implements AuditoriaRrhhService {

    private final AuditoriaRrhhRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final EmpleadoRepository empleadoRepository;

    @Override
    @Transactional
    public void registrar(UsuarioPrincipal actor, UUID empleadoId, String accion, String detalle) {
        repository.save(AuditoriaRrhh.builder()
                .usuario(usuarioRepository.getReferenceById(actor.getId()))
                .empleado(empleadoId == null ? null : empleadoRepository.getReferenceById(empleadoId))
                .accion(accion)
                .detalle(detalle)
                .fecha(LocalDateTime.now())
                .build());
    }
}
