package com.codesoftlabs.tusalud.app.services.privacidad;

import com.codesoftlabs.tusalud.app.dto.request.PrivacidadRequests.BancoDatosRequest;
import com.codesoftlabs.tusalud.app.dto.request.PrivacidadRequests.EncargadoRequest;
import com.codesoftlabs.tusalud.app.dto.request.PrivacidadRequests.FinalidadRequest;
import com.codesoftlabs.tusalud.app.dto.response.PrivacidadResponses.BancoDatosResponse;
import com.codesoftlabs.tusalud.app.dto.response.PrivacidadResponses.EncargadoResponse;
import com.codesoftlabs.tusalud.app.dto.response.PrivacidadResponses.EstadoPrivacidadResponse;
import com.codesoftlabs.tusalud.app.dto.response.PrivacidadResponses.FinalidadResponse;
import com.codesoftlabs.tusalud.app.dto.response.PrivacidadResponses.RegistroTratamientosResponse;
import com.codesoftlabs.tusalud.app.entities.BancoDatos;
import com.codesoftlabs.tusalud.app.entities.EncargadoTratamiento;
import com.codesoftlabs.tusalud.app.entities.FinalidadTratamiento;
import com.codesoftlabs.tusalud.app.entities.PoliticaRetencion;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.facturacion.ValidadorDocumentos;
import com.codesoftlabs.tusalud.app.repositories.BancoDatosRepository;
import com.codesoftlabs.tusalud.app.repositories.EncargadoTratamientoRepository;
import com.codesoftlabs.tusalud.app.repositories.FinalidadTratamientoRepository;
import com.codesoftlabs.tusalud.app.repositories.PoliticaPrivacidadRepository;
import com.codesoftlabs.tusalud.app.repositories.PoliticaRetencionRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegistroTratamientosService {

    private final BancoDatosRepository bancos;
    private final FinalidadTratamientoRepository finalidades;
    private final EncargadoTratamientoRepository encargados;
    private final PoliticaPrivacidadRepository politicas;
    private final PoliticaRetencionRepository retenciones;
    private final SolicitudArcoService solicitudes;
    private final IncidentesDatosService incidentes;

    public RegistroTratamientosResponse registro() {
        return new RegistroTratamientosResponse(
                bancos.findAllByOrderByNombreAsc().stream()
                        .map(BancoDatosResponse::de)
                        .toList(),
                finalidades.todas().stream().map(FinalidadResponse::de).toList(),
                encargados.findAllByOrderByRazonSocialAsc().stream()
                        .map(EncargadoResponse::de)
                        .toList());
    }

    public List<FinalidadResponse> finalidadesActivas() {
        return finalidades.activas().stream().map(FinalidadResponse::de).toList();
    }

    public EstadoPrivacidadResponse estado() {
        LocalDateTime ultima = retenciones.findAllByOrderByNombreAsc().stream()
                .map(PoliticaRetencion::getUltimaEjecucion)
                .filter(f -> f != null)
                .max(Comparator.naturalOrder())
                .orElse(null);
        return new EstadoPrivacidadResponse(
                solicitudes.abiertas(),
                solicitudes.vencidas(),
                incidentes.abiertos(),
                incidentes.vencidos(),
                politicas.vigente(LocalDate.now()).map(p -> p.getVersion()).orElse(null),
                ultima);
    }

    @Transactional
    public BancoDatosResponse guardarBanco(UUID id, BancoDatosRequest r) {
        BancoDatos b;
        if (id == null) {
            if (bancos.existsByCodigo(r.codigo().trim())) {
                throw new ConflictException("Ya existe un banco con ese código");
            }
            b = new BancoDatos();
        } else {
            b = bancos.findById(id).orElseThrow(() -> new ResourceNotFoundException("Banco de datos", "id", id));
        }
        b.setCodigo(r.codigo().trim());
        b.setNombre(r.nombre().trim());
        b.setDescripcion(r.descripcion().trim());
        b.setFinalidadPrincipal(r.finalidadPrincipal().trim());
        b.setCategoriasDatos(r.categoriasDatos().trim());
        b.setContieneSensibles(r.contieneSensibles());
        b.setResponsable(r.responsable().trim());
        b.setUbicacion(r.ubicacion().trim());
        b.setMedidasSeguridad(r.medidasSeguridad().trim());
        b.setTransferenciaTransfronteriza(r.transferenciaTransfronteriza());
        b.setNumeroInscripcion(vacioANulo(r.numeroInscripcion()));
        b.setFechaInscripcion(r.fechaInscripcion());
        b.setActivo(r.activo());
        b.setActualizadoEn(LocalDateTime.now());
        return BancoDatosResponse.de(bancos.save(b));
    }

    @Transactional
    public FinalidadResponse guardarFinalidad(UUID id, FinalidadRequest r) {
        FinalidadTratamiento f;
        if (id == null) {
            if (finalidades.existsByCodigo(r.codigo().trim())) {
                throw new ConflictException("Ya existe una finalidad con ese código");
            }
            f = new FinalidadTratamiento();
        } else {
            f = finalidades.findById(id).orElseThrow(() -> new ResourceNotFoundException("Finalidad", "id", id));
        }
        f.setCodigo(r.codigo().trim());
        f.setNombre(r.nombre().trim());
        f.setDescripcion(r.descripcion().trim());
        f.setBanco(
                r.bancoId() == null
                        ? null
                        : bancos.findById(r.bancoId())
                                .orElseThrow(() -> new ResourceNotFoundException("Banco de datos", "id", r.bancoId())));
        f.setBaseLegal(r.baseLegal().trim());
        f.setObligatoria(r.obligatoria());
        f.setCategoriaDatos(r.categoriaDatos().trim());
        f.setPlazoRetencionDias(r.plazoRetencionDias());
        f.setActiva(r.activa());
        return FinalidadResponse.de(finalidades.save(f));
    }

    @Transactional
    public EncargadoResponse guardarEncargado(UUID id, EncargadoRequest r) {
        if (r.ruc() != null
                && !r.ruc().isBlank()
                && !ValidadorDocumentos.rucValido(r.ruc().trim())) {
            throw new DatosInvalidosException("El RUC del encargado no es válido");
        }
        EncargadoTratamiento e;
        if (id == null) {
            e = new EncargadoTratamiento();
        } else {
            e = encargados.findById(id).orElseThrow(() -> new ResourceNotFoundException("Encargado", "id", id));
        }
        e.setRazonSocial(r.razonSocial().trim());
        e.setRuc(vacioANulo(r.ruc()));
        e.setServicio(r.servicio().trim());
        e.setCategoriasDatos(r.categoriasDatos().trim());
        e.setPais(r.pais().trim());
        e.setTransfronterizo(r.transfronterizo());
        e.setContratoVigenteHasta(r.contratoVigenteHasta());
        e.setContacto(vacioANulo(r.contacto()));
        e.setActivo(r.activo());
        e.setActualizadoEn(LocalDateTime.now());
        return EncargadoResponse.de(encargados.save(e));
    }

    private static String vacioANulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
