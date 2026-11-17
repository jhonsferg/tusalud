package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.dto.request.PasswordChangeRequest;
import com.codesoftlabs.tusalud.app.dto.request.PerfilUpdateRequest;
import com.codesoftlabs.tusalud.app.dto.response.PerfilResponse;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.exceptions.BusinessLogicException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.MedicoRepository;
import com.codesoftlabs.tusalud.app.repositories.PacienteRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.PerfilService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerfilServiceImpl implements PerfilService {

    private final UsuarioRepository usuarioRepository;
    private final PacienteRepository pacienteRepository;
    private final MedicoRepository medicoRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public PerfilResponse obtener(UsuarioPrincipal principal) {
        Usuario usuario = cargarUsuario(principal);
        return construir(usuario, principal);
    }

    @Override
    @Transactional
    public PerfilResponse actualizar(UsuarioPrincipal principal, PerfilUpdateRequest request) {
        Usuario usuario = cargarUsuario(principal);
        usuario.setNombre(request.nombre().trim());
        usuario.setApellido(request.apellido().trim());
        usuario.setTelefono(vacioANulo(request.telefono()));

        pacienteRepository.findByUsuarioEmail(usuario.getEmail()).ifPresent(paciente -> {
            if (request.fechaNacimiento() != null) {
                paciente.setFechaNacimiento(request.fechaNacimiento());
            }
        });
        medicoRepository
                .findByUsuarioEmail(usuario.getEmail())
                .ifPresent(medico -> medico.setConsultorio(vacioANulo(request.consultorio())));

        log.info("Perfil actualizado para {}", usuario.getEmail());
        return construir(usuario, principal);
    }

    @Override
    @Transactional
    public void cambiarPassword(UsuarioPrincipal principal, PasswordChangeRequest request) {
        Usuario usuario = cargarUsuario(principal);
        if (!passwordEncoder.matches(request.passwordActual(), usuario.getPassword())) {
            throw new BusinessLogicException("La contraseña actual no es correcta");
        }
        if (request.passwordActual().equals(request.passwordNueva())) {
            throw new BusinessLogicException("La nueva contraseña debe ser distinta de la actual");
        }
        usuario.setPassword(passwordEncoder.encode(request.passwordNueva()));
        log.info("Contraseña actualizada para {}", usuario.getEmail());
    }

    private Usuario cargarUsuario(UsuarioPrincipal principal) {
        return usuarioRepository
                .findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id " + principal.getId()));
    }

    private PerfilResponse construir(Usuario usuario, UsuarioPrincipal principal) {
        Paciente paciente =
                pacienteRepository.findByUsuarioEmail(usuario.getEmail()).orElse(null);
        Medico medico = medicoRepository.findByUsuarioEmail(usuario.getEmail()).orElse(null);
        return PerfilResponse.from(usuario, principal.getRoles(), paciente, medico);
    }

    private static String vacioANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
