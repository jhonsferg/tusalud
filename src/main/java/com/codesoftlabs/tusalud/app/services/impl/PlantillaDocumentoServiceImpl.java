package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.documentos.MotorPlantillas;
import com.codesoftlabs.tusalud.app.documentos.ProveedorPlantillas;
import com.codesoftlabs.tusalud.app.documentos.TipoDocumento;
import com.codesoftlabs.tusalud.app.dto.response.PlantillaResponse;
import com.codesoftlabs.tusalud.app.dto.response.PlantillaVersionResponse;
import com.codesoftlabs.tusalud.app.entities.Archivo;
import com.codesoftlabs.tusalud.app.entities.PlantillaDocumento;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.PlantillaDocumentoRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.ArchivoService;
import com.codesoftlabs.tusalud.app.services.interfaces.PlantillaDocumentoService;
import com.codesoftlabs.tusalud.app.storage.PropositoArchivo;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlantillaDocumentoServiceImpl implements PlantillaDocumentoService {

    private static final Map<String, String> NOMBRES = Map.ofEntries(
            Map.entry("comprobante", "Comprobante de caja"),
            Map.entry("receta", "Receta médica"),
            Map.entry("resultados-laboratorio", "Resultados de laboratorio"),
            Map.entry("informe-imagen", "Informe de imágenes diagnósticas"),
            Map.entry("constancia", "Constancia de atención"),
            Map.entry("certificado-medico", "Certificado médico"),
            Map.entry("descanso-medico", "Descanso médico"),
            Map.entry("informe-medico", "Informe médico"),
            Map.entry("referencia-medica", "Referencia"),
            Map.entry("contrarreferencia-medica", "Contrarreferencia"),
            Map.entry("epicrisis", "Epicrisis"),
            Map.entry("consentimiento", "Consentimiento informado"),
            Map.entry("resumen-historia", "Resumen de historia clínica"),
            Map.entry("reporte-ejecutivo", "Reporte ejecutivo"),
            Map.entry("reporte-financiero", "Reporte financiero"),
            Map.entry("reporte-operativo", "Reporte operativo"),
            Map.entry("reporte-calidad", "Reporte de calidad"),
            Map.entry("reporte-morbilidad", "Reporte de morbilidad"));
    private static final String REFERENCIA = "PLANTILLA_";
    private static final int MOTIVO_MAXIMO = 200;

    private final PlantillaDocumentoRepository plantillaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ArchivoService archivoService;
    private final MotorPlantillas motor;
    private final ProveedorPlantillas proveedor;

    @Override
    @Transactional(readOnly = true)
    public List<PlantillaResponse> listar() {
        List<PlantillaResponse> respuesta = new ArrayList<>();
        for (TipoDocumento tipo : TipoDocumento.values()) {
            if (tipo.plantilla() != null) {
                respuesta.add(describir(tipo.plantilla()));
            }
        }
        return respuesta;
    }

    @Override
    public PlantillaContenido descargar(String codigo) {
        validar(codigo);
        return new PlantillaContenido(proveedor.contenido(codigo), codigo + ".jrxml");
    }

    @Override
    @Transactional
    public PlantillaResponse reemplazar(UsuarioPrincipal actor, String codigo, byte[] contenido, String motivo) {
        validar(codigo);
        motor.validarCompilando(contenido);
        Usuario usuario = usuarioRepository
                .findById(actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", actor.getId()));
        int version = plantillaRepository.maximaVersion(codigo) + 1;
        Archivo archivo = archivoService.almacenar(
                usuario,
                null,
                PropositoArchivo.PLANTILLA,
                codigo + "-v" + version + ".jrxml",
                contenido,
                REFERENCIA + codigo,
                null);
        desactivarVigente(codigo);
        plantillaRepository.save(PlantillaDocumento.builder()
                .codigo(codigo)
                .version(version)
                .archivo(archivo)
                .activa(true)
                .restauracion(false)
                .motivo(recortar(motivo))
                .creadoPor(usuario)
                .createdAt(LocalDateTime.now())
                .build());
        motor.invalidar(codigo);
        return describir(codigo);
    }

    @Override
    @Transactional
    public PlantillaResponse restaurar(UsuarioPrincipal actor, String codigo, String motivo) {
        validar(codigo);
        Optional<PlantillaDocumento> vigente = plantillaRepository.findActiva(codigo);
        if (vigente.isEmpty() || vigente.get().isRestauracion()) {
            throw new ConflictException("La plantilla ya es la predeterminada");
        }
        Usuario usuario = usuarioRepository
                .findById(actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", actor.getId()));
        desactivarVigente(codigo);
        plantillaRepository.save(PlantillaDocumento.builder()
                .codigo(codigo)
                .version(plantillaRepository.maximaVersion(codigo) + 1)
                .archivo(null)
                .activa(true)
                .restauracion(true)
                .motivo(recortar(
                        motivo == null || motivo.isBlank() ? "Restauración de la plantilla predeterminada" : motivo))
                .creadoPor(usuario)
                .createdAt(LocalDateTime.now())
                .build());
        motor.invalidar(codigo);
        return describir(codigo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlantillaVersionResponse> historial(String codigo) {
        validar(codigo);
        return plantillaRepository.findHistorial(codigo).stream()
                .map(PlantillaVersionResponse::from)
                .toList();
    }

    private void desactivarVigente(String codigo) {
        plantillaRepository.findActiva(codigo).ifPresent(vigente -> {
            vigente.setActiva(false);
            plantillaRepository.saveAndFlush(vigente);
        });
    }

    private PlantillaResponse describir(String codigo) {
        Optional<PlantillaDocumento> activa = plantillaRepository.findActiva(codigo);
        String nombre = NOMBRES.getOrDefault(codigo, codigo);
        if (activa.isEmpty()) {
            return new PlantillaResponse(codigo, nombre, 0, false, null, null, null);
        }
        PlantillaDocumento plantilla = activa.get();
        return new PlantillaResponse(
                codigo,
                nombre,
                plantilla.getVersion(),
                !plantilla.isRestauracion(),
                plantilla.getCreadoPor().getNombreCompleto(),
                plantilla.getCreatedAt(),
                plantilla.getMotivo());
    }

    private static void validar(String codigo) {
        for (TipoDocumento tipo : TipoDocumento.values()) {
            if (codigo != null && codigo.equals(tipo.plantilla())) {
                return;
            }
        }
        throw new ResourceNotFoundException("Plantilla", "código", codigo);
    }

    private static String recortar(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            return null;
        }
        String limpio = motivo.strip();
        if (limpio.length() > MOTIVO_MAXIMO) {
            throw new DatosInvalidosException("El motivo no puede superar los 200 caracteres");
        }
        return limpio;
    }
}
