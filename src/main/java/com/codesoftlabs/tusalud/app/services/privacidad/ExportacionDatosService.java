package com.codesoftlabs.tusalud.app.services.privacidad;

import com.codesoftlabs.tusalud.app.canales.EnmascaradorContacto;
import com.codesoftlabs.tusalud.app.entities.ContactoCanal;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.repositories.ContactoCanalRepository;
import com.codesoftlabs.tusalud.app.repositories.PacienteRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExportacionDatosService {

    private final UsuarioRepository usuarios;
    private final PacienteRepository pacientes;
    private final ContactoCanalRepository contactos;
    private final ConsentimientosPrivacidadService consentimientos;
    private final ObjectMapper mapper;

    @PersistenceContext
    private EntityManager em;

    public byte[] generar(UUID usuarioId) {
        Usuario usuario = usuarios.findById(usuarioId).orElseThrow();
        Map<String, Object> raiz = new LinkedHashMap<>();
        raiz.put("generadoEn", LocalDateTime.now());
        raiz.put("finalidad", "Atención del derecho de acceso a los datos personales");
        Map<String, Object> perfil = new LinkedHashMap<>();
        perfil.put("nombre", usuario.getNombre());
        perfil.put("apellido", usuario.getApellido());
        perfil.put("correo", usuario.getEmail());
        perfil.put("telefono", usuario.getTelefono());
        perfil.put("estado", usuario.getEstado());
        perfil.put("registradoEn", usuario.getCreatedAt());
        raiz.put("perfil", perfil);
        Paciente paciente = pacientes.findByUsuarioId(usuarioId).orElse(null);
        if (paciente != null) {
            Map<String, Object> datos = new LinkedHashMap<>();
            datos.put("dni", paciente.getDni());
            datos.put("historiaClinica", paciente.getHistorialClinicoNumero());
            datos.put("fechaNacimiento", paciente.getFechaNacimiento());
            datos.put("sexo", paciente.getSexo());
            datos.put("grupoSanguineo", paciente.getGrupoSanguineo());
            datos.put("alergias", paciente.getAlergias());
            datos.put("antecedentes", paciente.getAntecedentes());
            raiz.put("paciente", datos);
            raiz.put("citas", citas(paciente.getId()));
            raiz.put("atenciones", atenciones(paciente.getId()));
            raiz.put("comprobantes", comprobantes(paciente.getId()));
            raiz.put("documentosMedicos", documentosMedicos(paciente.getId()));
            raiz.put("consentimientosInformados", consentimientosInformados(paciente.getId()));
        }
        raiz.put("canalesDeNotificacion", canales(usuarioId));
        raiz.put("consentimientosDeTratamiento", consentimientos.historial(usuarioId));
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(raiz);
    }

    private List<Map<String, Object>> citas(UUID pacienteId) {
        return filas(
                """
                SELECT c.fechaHora, c.especialidad.nombre, c.medico.usuario.nombre, c.medico.usuario.apellido,
                       c.estado, c.modalidad, c.motivo, c.monto
                FROM Cita c WHERE c.paciente.id = :id ORDER BY c.fechaHora DESC
                """,
                pacienteId,
                "fecha",
                "especialidad",
                "medicoNombre",
                "medicoApellido",
                "estado",
                "modalidad",
                "motivo",
                "monto");
    }

    private List<Map<String, Object>> atenciones(UUID pacienteId) {
        return filas(
                """
                SELECT a.fecha, a.medico.usuario.nombre, a.medico.usuario.apellido, a.diagnostico, a.tratamiento,
                       a.indicaciones, a.observaciones, a.presionArterial, a.pesoKg, a.tallaCm
                FROM Atencion a WHERE a.paciente.id = :id ORDER BY a.fecha DESC
                """,
                pacienteId,
                "fecha",
                "medicoNombre",
                "medicoApellido",
                "diagnostico",
                "tratamiento",
                "indicaciones",
                "observaciones",
                "presionArterial",
                "pesoKg",
                "tallaCm");
    }

    private List<Map<String, Object>> comprobantes(UUID pacienteId) {
        return filas("""
                SELECT c.codigo, c.fecha, c.tipo, c.total, c.estado, c.metodoPago
                FROM Comprobante c WHERE c.paciente.id = :id ORDER BY c.fecha DESC
                """, pacienteId, "codigo", "fecha", "tipo", "total", "estado", "metodoPago");
    }

    private List<Map<String, Object>> documentosMedicos(UUID pacienteId) {
        return filas("""
                SELECT d.tipo, d.serie, d.anio, d.numero, d.estado, d.emitidoEn
                FROM DocumentoMedico d WHERE d.paciente.id = :id ORDER BY d.creadoEn DESC
                """, pacienteId, "tipo", "serie", "anio", "numero", "estado", "emitidoEn");
    }

    private List<Map<String, Object>> consentimientosInformados(UUID pacienteId) {
        return filas("""
                SELECT c.procedimiento, c.estado, c.firmadoEn, c.venceEn
                FROM Consentimiento c WHERE c.paciente.id = :id ORDER BY c.creadoEn DESC
                """, pacienteId, "procedimiento", "estado", "firmadoEn", "venceEn");
    }

    private List<Map<String, Object>> canales(UUID usuarioId) {
        List<Map<String, Object>> lista = new ArrayList<>();
        for (ContactoCanal c : contactos.findByUsuarioId(usuarioId)) {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("canal", c.getCanal());
            fila.put("destino", EnmascaradorContacto.enmascarar(c.getCanal(), c.getDestino()));
            fila.put("verificadoEn", c.getVerificadoEn());
            fila.put("consentimientoEn", c.getConsentimientoEn());
            fila.put("bajaEn", c.getBajaEn());
            lista.add(fila);
        }
        return lista;
    }

    private List<Map<String, Object>> filas(String jpql, UUID id, String... columnas) {
        List<Object[]> resultado = em.createQuery(jpql, Object[].class)
                .setParameter("id", id)
                .setMaxResults(5000)
                .getResultList();
        List<Map<String, Object>> lista = new ArrayList<>();
        for (Object[] fila : resultado) {
            Map<String, Object> mapa = new LinkedHashMap<>();
            for (int i = 0; i < columnas.length; i++) {
                mapa.put(columnas[i], fila[i]);
            }
            lista.add(mapa);
        }
        return lista;
    }
}
