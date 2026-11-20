package com.codesoftlabs.tusalud.app.services.hce;

import com.codesoftlabs.tusalud.app.documentos.CodigoDocumentoClinico;
import com.codesoftlabs.tusalud.app.dto.request.HceRequests.AdendaRequest;
import com.codesoftlabs.tusalud.app.dto.request.HceRequests.CorreccionRequest;
import com.codesoftlabs.tusalud.app.dto.response.HceResponses.CierreResponse;
import com.codesoftlabs.tusalud.app.dto.response.HceResponses.ContenidoClinico;
import com.codesoftlabs.tusalud.app.dto.response.HceResponses.HistorialAtencionResponse;
import com.codesoftlabs.tusalud.app.dto.response.HceResponses.VersionResponse;
import com.codesoftlabs.tusalud.app.entities.Atencion;
import com.codesoftlabs.tusalud.app.entities.AtencionCierre;
import com.codesoftlabs.tusalud.app.entities.AtencionDiagnostico;
import com.codesoftlabs.tusalud.app.entities.AtencionVersion;
import com.codesoftlabs.tusalud.app.entities.RecetaItem;
import com.codesoftlabs.tusalud.app.entities.enums.HceEnums.CampoCorregible;
import com.codesoftlabs.tusalud.app.entities.enums.HceEnums.TipoVersion;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.exceptions.BusinessLogicException;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.AtencionCierreRepository;
import com.codesoftlabs.tusalud.app.repositories.AtencionRepository;
import com.codesoftlabs.tusalud.app.repositories.AtencionVersionRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.AuditoriaClinicaService;
import com.codesoftlabs.tusalud.app.services.interfaces.RelacionAsistencialService;
import com.codesoftlabs.tusalud.app.services.receta.RecetaFirma;
import com.codesoftlabs.tusalud.app.services.receta.RecetaFirma.Firmada;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HceService {

    private static final String ADVERTENCIA_FIRMA =
            "Firma electrónica de prueba, sin validez legal como firma digital acreditada";

    private final AtencionRepository atenciones;
    private final AtencionCierreRepository cierres;
    private final AtencionVersionRepository versiones;
    private final UsuarioRepository usuarios;
    private final RecetaFirma firma;
    private final AuditoriaClinicaService auditoria;
    private final RelacionAsistencialService relacion;
    private final EntityManager entityManager;

    @Transactional
    public CierreResponse cerrar(UsuarioPrincipal actor, UUID atencionId) {
        Atencion atencion = cargar(atencionId);
        exigirAutor(actor, atencion);
        if (cierres.existsByAtencionId(atencionId)) {
            throw new ConflictException("La atención ya está cerrada y firmada");
        }
        String canonico = canonico(atencion);
        Firmada firmada = firma.firmar(canonico);
        AtencionCierre cierre = cierres.save(AtencionCierre.builder()
                .atencion(atencion)
                .cerradoPor(usuarios.getReferenceById(actor.getId()))
                .cerradoEn(LocalDateTime.now())
                .contenidoCanonico(canonico)
                .hash(CodigoDocumentoClinico.sha256(canonico))
                .firma(firmada.firma())
                .firmaAlgoritmo(firmada.algoritmo())
                .certificadoHuella(firmada.huella())
                .certificadoSujeto(firmada.sujeto())
                .certificadoDer(firmada.certificadoDer())
                .certificadoDePrueba(firmada.dePrueba())
                .build());
        auditoria.registrar(
                actor, atencion.getPaciente().getId(), "ATENCION_CERRADA", "Atención " + atencionId + " cerrada");
        return respuestaCierre(atencionId, cierre);
    }

    public CierreResponse estado(UsuarioPrincipal actor, UUID atencionId) {
        Atencion atencion = cargar(atencionId);
        exigirLectura(actor, atencion);
        return cierres.findByAtencionId(atencionId)
                .map(c -> respuestaCierre(atencionId, c))
                .orElse(new CierreResponse(false, atencionId, null, null, null, false, false, null));
    }

    @Transactional
    public VersionResponse adenda(UsuarioPrincipal actor, UUID atencionId, AdendaRequest request) {
        Atencion atencion = cargar(atencionId);
        exigirAutor(actor, atencion);
        exigirCerrada(atencionId);
        bloquear(atencion);
        AtencionVersion version = versiones.save(AtencionVersion.builder()
                .atencion(atencion)
                .numero(versiones.ultimoNumero(atencionId) + 1)
                .tipo(TipoVersion.ADENDA)
                .valorNuevo(request.texto().strip())
                .motivo(request.motivo().strip())
                .autor(usuarios.getReferenceById(actor.getId()))
                .creadoEn(LocalDateTime.now())
                .build());
        auditoria.registrar(actor, atencion.getPaciente().getId(), "ATENCION_ADENDA", "Adenda " + version.getNumero());
        return aVersion(version, actor.getNombreCompleto());
    }

    @Transactional
    public VersionResponse corregir(UsuarioPrincipal actor, UUID atencionId, CorreccionRequest request) {
        Atencion atencion = cargar(atencionId);
        exigirAutor(actor, atencion);
        exigirCerrada(atencionId);
        bloquear(atencion);
        List<AtencionVersion> previas = versiones.historial(atencionId);
        String anterior = vigente(atencion, previas).valor(request.campo());
        String nuevo = request.valorNuevo().strip();
        if (nuevo.equals(anterior == null ? "" : anterior)) {
            throw new BusinessLogicException("El valor nuevo es igual al vigente");
        }
        AtencionVersion version = versiones.save(AtencionVersion.builder()
                .atencion(atencion)
                .numero(versiones.ultimoNumero(atencionId) + 1)
                .tipo(TipoVersion.CORRECCION)
                .campo(request.campo())
                .valorAnterior(anterior)
                .valorNuevo(nuevo)
                .motivo(request.motivo().strip())
                .autor(usuarios.getReferenceById(actor.getId()))
                .creadoEn(LocalDateTime.now())
                .build());
        auditoria.registrar(
                actor,
                atencion.getPaciente().getId(),
                "ATENCION_CORRECCION",
                "Corrección " + version.getNumero() + " de " + request.campo());
        return aVersion(version, actor.getNombreCompleto());
    }

    public HistorialAtencionResponse historial(UsuarioPrincipal actor, UUID atencionId) {
        Atencion atencion = cargar(atencionId);
        exigirLectura(actor, atencion);
        List<AtencionVersion> lista = versiones.historial(atencionId);
        CierreResponse cierre = cierres.findByAtencionId(atencionId)
                .map(c -> respuestaCierre(atencionId, c))
                .orElse(new CierreResponse(false, atencionId, null, null, null, false, false, null));
        return new HistorialAtencionResponse(
                atencionId,
                atencion.getFecha(),
                cierre.cerrada(),
                original(atencion).aRespuesta(),
                vigente(atencion, lista).aRespuesta(),
                lista.stream()
                        .map(v -> aVersion(v, v.getAutor().getNombreCompleto()))
                        .toList(),
                cierre);
    }

    public ContenidoClinico contenidoVigente(Atencion atencion) {
        return vigente(atencion, versiones.historial(atencion.getId())).aRespuesta();
    }

    public List<VersionResponse> versionesDe(UUID atencionId) {
        return versiones.historial(atencionId).stream()
                .map(v -> aVersion(v, v.getAutor().getNombreCompleto()))
                .toList();
    }

    public boolean cerrada(UUID atencionId) {
        return cierres.existsByAtencionId(atencionId);
    }

    public CierreResponse cierreDe(UUID atencionId) {
        return cierres.findByAtencionId(atencionId)
                .map(c -> respuestaCierre(atencionId, c))
                .orElse(new CierreResponse(false, atencionId, null, null, null, false, false, null));
    }

    private void bloquear(Atencion atencion) {
        entityManager.lock(atencion, LockModeType.PESSIMISTIC_WRITE);
    }

    private void exigirCerrada(UUID atencionId) {
        if (!cierres.existsByAtencionId(atencionId)) {
            throw new BusinessLogicException("La atención aún no está cerrada: edítela directamente");
        }
    }

    private Atencion cargar(UUID atencionId) {
        return atenciones
                .findByIdConDetalle(atencionId)
                .orElseThrow(() -> new ResourceNotFoundException("Atención", "id", atencionId));
    }

    private void exigirAutor(UsuarioPrincipal actor, Atencion atencion) {
        if (!atencion.getMedico().getUsuario().getId().equals(actor.getId())) {
            throw new ForbiddenOperationException("Solo el médico de la atención puede cerrarla o rectificarla");
        }
    }

    private void exigirLectura(UsuarioPrincipal actor, Atencion atencion) {
        if (actor.getRoles().contains(RolNombre.ROLE_PACIENTE.name())) {
            if (!atencion.getPaciente().getUsuario().getId().equals(actor.getId())) {
                throw new ForbiddenOperationException("Solo el titular consulta su historia");
            }
            return;
        }
        if (atencion.getMedico().getUsuario().getId().equals(actor.getId())) {
            return;
        }
        relacion.exigirRelacion(
                actor, atencion.getPaciente().getId(), "No tiene relación asistencial con este paciente");
    }

    private CierreResponse respuestaCierre(UUID atencionId, AtencionCierre cierre) {
        boolean valida = CodigoDocumentoClinico.sha256(cierre.getContenidoCanonico())
                        .equals(cierre.getHash())
                && firma.verificar(cierre.getContenidoCanonico(), cierre.getFirma(), cierre.getCertificadoDer());
        return new CierreResponse(
                true,
                atencionId,
                cierre.getCerradoEn(),
                cierre.getCerradoPor().getNombreCompleto(),
                cierre.getHash().substring(0, 16),
                valida,
                cierre.isCertificadoDePrueba(),
                cierre.isCertificadoDePrueba() ? ADVERTENCIA_FIRMA : null);
    }

    private static VersionResponse aVersion(AtencionVersion v, String autor) {
        return new VersionResponse(
                v.getId(),
                v.getNumero(),
                v.getTipo().name(),
                v.getCampo() == null ? null : v.getCampo().name(),
                v.getValorAnterior(),
                v.getValorNuevo(),
                v.getMotivo(),
                autor,
                v.getCreadoEn());
    }

    private static Contenido original(Atencion a) {
        Contenido c = new Contenido();
        c.valores.put(CampoCorregible.DIAGNOSTICO, a.getDiagnostico());
        c.valores.put(CampoCorregible.TRATAMIENTO, a.getTratamiento());
        c.valores.put(CampoCorregible.INDICACIONES, a.getIndicaciones());
        c.valores.put(CampoCorregible.OBSERVACIONES, a.getObservaciones());
        return c;
    }

    private static Contenido vigente(Atencion a, List<AtencionVersion> lista) {
        Contenido c = original(a);
        for (AtencionVersion v : lista) {
            if (v.getTipo() == TipoVersion.CORRECCION) {
                c.valores.put(v.getCampo(), v.getValorNuevo());
            }
        }
        return c;
    }

    private static String canonico(Atencion a) {
        StringBuilder texto = new StringBuilder();
        texto.append("atencion=").append(a.getId()).append('\n');
        texto.append("fecha=").append(a.getFecha()).append('\n');
        texto.append("paciente=").append(a.getPaciente().getId()).append('\n');
        texto.append("medico=").append(a.getMedico().getId()).append('\n');
        texto.append("diagnostico=").append(nulo(a.getDiagnostico())).append('\n');
        texto.append("tratamiento=").append(nulo(a.getTratamiento())).append('\n');
        texto.append("indicaciones=").append(nulo(a.getIndicaciones())).append('\n');
        texto.append("observaciones=").append(nulo(a.getObservaciones())).append('\n');
        texto.append("peso=").append(a.getPesoKg()).append('\n');
        texto.append("talla=").append(a.getTallaCm()).append('\n');
        texto.append("presion=").append(nulo(a.getPresionArterial())).append('\n');
        texto.append("temperatura=").append(a.getTemperaturaC()).append('\n');
        texto.append("frecuencia=").append(a.getFrecuenciaCardiaca()).append('\n');
        List<AtencionDiagnostico> diagnosticos = new ArrayList<>(a.getDiagnosticos());
        for (AtencionDiagnostico d : diagnosticos) {
            texto.append("dx=")
                    .append(d.getCodigo())
                    .append('|')
                    .append(d.getTipo())
                    .append('|')
                    .append(d.isPrincipal())
                    .append('\n');
        }
        for (RecetaItem item : a.getReceta()) {
            texto.append("rx=")
                    .append(nulo(item.getMedicamento()))
                    .append('|')
                    .append(nulo(item.getDosis()))
                    .append('|')
                    .append(nulo(item.getFrecuencia()))
                    .append('|')
                    .append(nulo(item.getDuracion()))
                    .append('\n');
        }
        return texto.toString();
    }

    private static String nulo(String valor) {
        return valor == null ? "" : valor;
    }

    private static final class Contenido {

        private final Map<CampoCorregible, String> valores = new EnumMap<>(CampoCorregible.class);

        String valor(CampoCorregible campo) {
            return valores.get(campo);
        }

        ContenidoClinico aRespuesta() {
            return new ContenidoClinico(
                    valores.get(CampoCorregible.DIAGNOSTICO),
                    valores.get(CampoCorregible.TRATAMIENTO),
                    valores.get(CampoCorregible.INDICACIONES),
                    valores.get(CampoCorregible.OBSERVACIONES));
        }
    }
}
