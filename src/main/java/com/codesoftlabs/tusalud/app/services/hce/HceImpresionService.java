package com.codesoftlabs.tusalud.app.services.hce;

import com.codesoftlabs.tusalud.app.documentos.CodigoDocumentoClinico;
import com.codesoftlabs.tusalud.app.documentos.FichaDocumento;
import com.codesoftlabs.tusalud.app.documentos.Formato;
import com.codesoftlabs.tusalud.app.documentos.MotorPlantillas;
import com.codesoftlabs.tusalud.app.documentos.QrGenerador;
import com.codesoftlabs.tusalud.app.documentos.TipoDocumento;
import com.codesoftlabs.tusalud.app.dto.response.HceResponses.CierreResponse;
import com.codesoftlabs.tusalud.app.dto.response.HceResponses.ContenidoClinico;
import com.codesoftlabs.tusalud.app.dto.response.HceResponses.VersionResponse;
import com.codesoftlabs.tusalud.app.entities.Atencion;
import com.codesoftlabs.tusalud.app.entities.AtencionDiagnostico;
import com.codesoftlabs.tusalud.app.entities.HceImpresion;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.RecetaItem;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.AtencionRepository;
import com.codesoftlabs.tusalud.app.repositories.HceImpresionRepository;
import com.codesoftlabs.tusalud.app.repositories.PacienteRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.AuditoriaClinicaService;
import com.codesoftlabs.tusalud.app.services.interfaces.RelacionAsistencialService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HceImpresionService {

    private final PacienteRepository pacientes;
    private final AtencionRepository atenciones;
    private final HceImpresionRepository impresiones;
    private final UsuarioRepository usuarios;
    private final HceService hce;
    private final MotorPlantillas motor;
    private final RelacionAsistencialService relacion;
    private final AuditoriaClinicaService auditoria;

    @Value("${app.documentos-medicos.url-verificacion:http://localhost:4600/verificar/}")
    private String urlVerificacion;

    @Transactional
    public byte[] imprimirPropia(UsuarioPrincipal actor) {
        Paciente propio = pacientes
                .findByUsuarioId(actor.getId())
                .orElseThrow(() -> new ForbiddenOperationException("Solo un paciente imprime su propia historia"));
        return imprimir(actor, propio.getId());
    }

    @Transactional
    public byte[] imprimir(UsuarioPrincipal actor, UUID pacienteId) {
        Paciente paciente = pacientes
                .findById(pacienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", "id", pacienteId));
        exigirAcceso(actor, paciente);
        List<Atencion> lista = new ArrayList<>(atenciones.findByPacienteIdConDetalle(pacienteId));
        lista.sort(Comparator.comparing(Atencion::getFecha).thenComparing(Atencion::getId));

        FichaDocumento ficha = FichaDocumento.de(TipoDocumento.INFORME_MEDICO);
        StringBuilder contenido = new StringBuilder();
        ficha.titulo("Historia clínica", paciente.getHistorialClinicoNumero());
        ficha.par("Establecimiento", "TuSalud")
                .par("Paciente", paciente.getUsuario().getNombreCompleto())
                .par("DNI", paciente.getDni())
                .par("Historia clínica", paciente.getHistorialClinicoNumero())
                .par(
                        "Nacimiento",
                        paciente.getFechaNacimiento() == null
                                ? "-"
                                : paciente.getFechaNacimiento().toString())
                .par("Grupo sanguíneo", valor(paciente.getGrupoSanguineo()));
        ficha.seccion("Antecedentes y alergias");
        ficha.fila("Alergias", valor(paciente.getAlergias()));
        ficha.fila("Antecedentes", valor(paciente.getAntecedentes()));
        contenido.append(paciente.getId()).append(paciente.getDni());
        int numero = 0;
        for (Atencion a : lista) {
            numero++;
            ContenidoClinico vigente = hce.contenidoVigente(a);
            CierreResponse cierre = hce.cierreDe(a.getId());
            ficha.seccion("Atención " + numero + " del " + Formato.fechaHora(a.getFecha()));
            ficha.fila(
                    "Médico",
                    a.getMedico().getUsuario().getNombreCompleto() + " CMP "
                            + a.getMedico().getColegiatura());
            ficha.fila("Diagnóstico", valor(vigente.diagnostico()));
            for (AtencionDiagnostico d : a.getDiagnosticos()) {
                ficha.fila(d.getCodigo(), d.getDescripcion());
            }
            ficha.fila("Tratamiento", valor(vigente.tratamiento()));
            ficha.fila("Indicaciones", valor(vigente.indicaciones()));
            ficha.fila("Observaciones", valor(vigente.observaciones()));
            ficha.fila("Signos vitales", signos(a));
            for (RecetaItem item : a.getReceta()) {
                ficha.fila(
                        "Receta",
                        item.getMedicamento() + " " + valor(item.getDosis()) + " " + valor(item.getFrecuencia()));
            }
            for (VersionResponse v : hce.versionesDe(a.getId())) {
                ficha.fila(
                        v.tipo().equals("ADENDA")
                                ? "Adenda " + v.numero()
                                : "Corrección " + v.numero() + " de " + v.campo(),
                        v.valorNuevo() + " Motivo: " + v.motivo() + ". " + v.autor() + ", "
                                + Formato.fechaHora(v.creadoEn()));
            }
            ficha.fila(
                    "Firma",
                    cierre.cerrada()
                            ? "Cerrada el " + Formato.fechaHora(cierre.cerradoEn()) + " por " + cierre.cerradoPor()
                                    + ", huella " + cierre.hashCorto()
                                    + (cierre.firmaValida() ? ", válida" : ", no verificable")
                            : "Sin cerrar");
            contenido.append(a.getId()).append(vigente).append(cierre.hashCorto());
        }
        String codigo = nuevoCodigo();
        String hash = CodigoDocumentoClinico.sha256(contenido.toString().getBytes(StandardCharsets.UTF_8));
        impresiones.save(HceImpresion.builder()
                .paciente(paciente)
                .codigoVerificacion(codigo)
                .contenidoHash(hash)
                .atenciones(lista.size())
                .emitidoEn(LocalDateTime.now())
                .emitidoPor(usuarios.getReferenceById(actor.getId()))
                .build());
        ficha.firma("TuSalud", "Historia clínica impresa");
        ficha.parametros().put("generadoPor", actor.getNombreCompleto());
        ficha.parametros().put("generadoEn", Formato.fechaHora(LocalDateTime.now()));
        ficha.parametros().put("codigo", codigo);
        ficha.parametros().put("urlVerificacion", urlVerificacion + codigo);
        ficha.parametros().put("hashCorto", hash.substring(0, 16));
        ficha.grafico(2, QrGenerador.generar(urlVerificacion + codigo, 240));
        auditoria.registrar(actor, pacienteId, "HISTORIA_IMPRESA", "Código " + codigo);
        return motor.renderizarPdf(ficha);
    }

    private void exigirAcceso(UsuarioPrincipal actor, Paciente paciente) {
        if (actor.getRoles().contains(RolNombre.ROLE_PACIENTE.name())) {
            if (!paciente.getUsuario().getId().equals(actor.getId())) {
                throw new ForbiddenOperationException("Solo el titular imprime su historia");
            }
            return;
        }
        relacion.exigirRelacion(actor, paciente.getId(), "No tiene relación asistencial con este paciente");
    }

    private String nuevoCodigo() {
        String codigo = CodigoDocumentoClinico.nuevo();
        return impresiones.findByCodigoVerificacion(codigo).isPresent() ? CodigoDocumentoClinico.nuevo() : codigo;
    }

    private static String signos(Atencion a) {
        List<String> partes = new ArrayList<>();
        if (a.getPresionArterial() != null) {
            partes.add("PA " + a.getPresionArterial());
        }
        if (a.getFrecuenciaCardiaca() != null) {
            partes.add("FC " + a.getFrecuenciaCardiaca());
        }
        if (a.getTemperaturaC() != null) {
            partes.add("T " + a.getTemperaturaC() + " C");
        }
        if (a.getPesoKg() != null) {
            partes.add("Peso " + a.getPesoKg() + " kg");
        }
        if (a.getTallaCm() != null) {
            partes.add("Talla " + a.getTallaCm() + " cm");
        }
        return partes.isEmpty() ? "-" : String.join(", ", partes);
    }

    private static String valor(String texto) {
        return texto == null || texto.isBlank() ? "-" : texto;
    }
}
