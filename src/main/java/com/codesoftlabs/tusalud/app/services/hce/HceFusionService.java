package com.codesoftlabs.tusalud.app.services.hce;

import com.codesoftlabs.tusalud.app.dto.request.HceRequests.FusionPacientesRequest;
import com.codesoftlabs.tusalud.app.dto.response.HceResponses.DuplicadoResponse;
import com.codesoftlabs.tusalud.app.dto.response.HceResponses.FusionResponse;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.PacienteFusion;
import com.codesoftlabs.tusalud.app.exceptions.BusinessLogicException;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.PacienteFusionRepository;
import com.codesoftlabs.tusalud.app.repositories.PacienteRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.AuditoriaClinicaService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HceFusionService {

    private static final Pattern IDENTIFICADOR = Pattern.compile("^[a-z][a-z0-9_]*$");

    private final PacienteRepository pacientes;
    private final PacienteFusionRepository fusiones;
    private final UsuarioRepository usuarios;
    private final AuditoriaClinicaService auditoria;
    private final EntityManager entityManager;

    public List<DuplicadoResponse> duplicados() {
        @SuppressWarnings("unchecked")
        List<Object[]> filas = entityManager.createNativeQuery("""
                        SELECT p1.id, u1.nombre, u1.apellido, p1.dni, p1.historial_clinico_numero,
                               p2.id, u2.nombre, u2.apellido, p2.dni, p2.historial_clinico_numero
                        FROM pacientes p1
                        JOIN usuarios u1 ON u1.id = p1.usuario_id
                        JOIN pacientes p2 ON p1.id < p2.id
                        JOIN usuarios u2 ON u2.id = p2.usuario_id
                        WHERE p1.fecha_nacimiento IS NOT NULL
                          AND p1.fecha_nacimiento = p2.fecha_nacimiento
                          AND LOWER(u1.nombre) = LOWER(u2.nombre)
                          AND LOWER(u1.apellido) = LOWER(u2.apellido)
                        ORDER BY u1.apellido, u1.nombre
                        """).getResultList();
        List<DuplicadoResponse> resultado = new ArrayList<>();
        for (Object[] f : filas) {
            resultado.add(new DuplicadoResponse(
                    uuid(f[0]),
                    f[1] + " " + f[2],
                    String.valueOf(f[3]),
                    String.valueOf(f[4]),
                    uuid(f[5]),
                    f[6] + " " + f[7],
                    String.valueOf(f[8]),
                    String.valueOf(f[9]),
                    "Mismo nombre, apellido y fecha de nacimiento"));
        }
        return resultado;
    }

    public List<FusionResponse> historial() {
        return fusiones.recientes().stream().map(HceFusionService::aRespuesta).toList();
    }

    @Transactional
    public FusionResponse fusionar(UsuarioPrincipal actor, FusionPacientesRequest request) {
        if (request.origenId().equals(request.destinoId())) {
            throw new BusinessLogicException("El paciente de origen y el de destino deben ser distintos");
        }
        Paciente origen = pacientes
                .findById(request.origenId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", "id", request.origenId()));
        Paciente destino = pacientes
                .findById(request.destinoId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", "id", request.destinoId()));
        String nombreOrigen = origen.getUsuario().getNombreCompleto();
        String historialOrigen = origen.getHistorialClinicoNumero();
        String dniOrigen = origen.getDni();
        UUID usuarioOrigen = origen.getUsuario().getId();
        entityManager.flush();
        Map<String, Integer> movidos = new TreeMap<>();
        try {
            for (String tabla : tablasConPaciente()) {
                int cantidad = entityManager
                        .createNativeQuery(
                                "UPDATE " + tabla + " SET paciente_id = :destino WHERE paciente_id = :origen")
                        .setParameter("destino", request.destinoId())
                        .setParameter("origen", request.origenId())
                        .executeUpdate();
                if (cantidad > 0) {
                    movidos.put(tabla, cantidad);
                }
            }
            PacienteFusion fusion = fusiones.save(PacienteFusion.builder()
                    .destino(destino)
                    .origenHistorial(historialOrigen)
                    .origenDni(dniOrigen)
                    .origenNombre(nombreOrigen)
                    .motivo(request.motivo().strip())
                    .resumen(movidos.toString())
                    .realizadoPor(usuarios.getReferenceById(actor.getId()))
                    .realizadoEn(LocalDateTime.now())
                    .build());
            entityManager.flush();
            entityManager
                    .createNativeQuery("UPDATE usuarios SET estado = 'INACTIVO' WHERE id = :usuario")
                    .setParameter("usuario", usuarioOrigen)
                    .executeUpdate();
            entityManager.detach(origen);
            entityManager
                    .createNativeQuery("DELETE FROM pacientes WHERE id = :origen")
                    .setParameter("origen", request.origenId())
                    .executeUpdate();
            auditoria.registrar(
                    actor,
                    request.destinoId(),
                    "PACIENTE_FUSIONADO",
                    "Fusionó la historia " + historialOrigen + " en esta historia");
            return aRespuesta(fusion);
        } catch (PersistenceException ex) {
            throw new ConflictException(
                    "No se pudo fusionar: hay registros del paciente de origen que chocan con los del destino");
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> tablasConPaciente() {
        List<Object> nombres = entityManager.createNativeQuery("""
                        SELECT DISTINCT LOWER(table_name) FROM information_schema.columns
                        WHERE LOWER(column_name) = 'paciente_id' AND LOWER(table_schema) = 'public'
                        ORDER BY 1
                        """).getResultList();
        List<String> tablas = new ArrayList<>();
        for (Object nombre : nombres) {
            String tabla = String.valueOf(nombre);
            if (IDENTIFICADOR.matcher(tabla).matches()) {
                tablas.add(tabla);
            }
        }
        return tablas;
    }

    private static UUID uuid(Object valor) {
        return valor instanceof UUID u ? u : UUID.fromString(String.valueOf(valor));
    }

    private static FusionResponse aRespuesta(PacienteFusion f) {
        return new FusionResponse(
                f.getId(),
                f.getDestino().getId(),
                f.getDestino().getUsuario().getNombreCompleto(),
                f.getOrigenHistorial(),
                f.getOrigenDni(),
                f.getOrigenNombre(),
                f.getMotivo(),
                f.getResumen(),
                f.getRealizadoPor().getNombreCompleto(),
                f.getRealizadoEn());
    }
}
