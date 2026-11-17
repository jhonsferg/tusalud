package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.request.CancelarOrdenImagenRequest;
import com.codesoftlabs.tusalud.app.dto.request.EstudioImagenRequest;
import com.codesoftlabs.tusalud.app.dto.request.InformeImagenRequest;
import com.codesoftlabs.tusalud.app.dto.request.OrdenImagenCreateRequest;
import com.codesoftlabs.tusalud.app.dto.request.ProgramarImagenRequest;
import com.codesoftlabs.tusalud.app.dto.request.RealizarImagenRequest;
import com.codesoftlabs.tusalud.app.dto.response.AgendaImagenResponse;
import com.codesoftlabs.tusalud.app.dto.response.ArchivoImagenResponse;
import com.codesoftlabs.tusalud.app.dto.response.EstudioImagenResponse;
import com.codesoftlabs.tusalud.app.dto.response.ImagenesResumenResponse;
import com.codesoftlabs.tusalud.app.dto.response.InformeImagenResponse;
import com.codesoftlabs.tusalud.app.dto.response.MiFirmaImagenResponse;
import com.codesoftlabs.tusalud.app.dto.response.OrdenImagenResponse;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoOrdenImagen;
import com.codesoftlabs.tusalud.app.entities.enums.ModalidadImagen;
import com.codesoftlabs.tusalud.app.entities.enums.PrioridadOrden;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ImagenesService {

    record ArchivoEntrante(String nombreOriginal, byte[] contenido) {}

    record ArchivoDescarga(String nombre, String tipoContenido, byte[] contenido) {}

    List<EstudioImagenResponse> listarEstudios(String busqueda, ModalidadImagen modalidad, Boolean activo);

    EstudioImagenResponse obtenerEstudio(UUID id);

    EstudioImagenResponse crearEstudio(EstudioImagenRequest request);

    EstudioImagenResponse actualizarEstudio(UUID id, EstudioImagenRequest request);

    OrdenImagenResponse crearOrden(UsuarioPrincipal actor, OrdenImagenCreateRequest request);

    List<OrdenImagenResponse> listarOrdenes(
            UsuarioPrincipal actor, EstadoOrdenImagen estado, String busqueda, PrioridadOrden prioridad);

    List<OrdenImagenResponse> ordenesDePaciente(UsuarioPrincipal actor, UUID pacienteId);

    List<OrdenImagenResponse> misEstudios(UsuarioPrincipal actor);

    OrdenImagenResponse obtenerOrden(UsuarioPrincipal actor, UUID id);

    OrdenImagenResponse programar(UsuarioPrincipal actor, UUID id, ProgramarImagenRequest request);

    OrdenImagenResponse realizar(UsuarioPrincipal actor, UUID id, RealizarImagenRequest request);

    OrdenImagenResponse informar(UsuarioPrincipal actor, UUID id, InformeImagenRequest request);

    List<InformeImagenResponse> historialInformes(UsuarioPrincipal actor, UUID id);

    OrdenImagenResponse cancelar(UsuarioPrincipal actor, UUID id, CancelarOrdenImagenRequest request);

    List<ArchivoImagenResponse> subirArchivos(UsuarioPrincipal actor, UUID id, List<ArchivoEntrante> archivos);

    ArchivoDescarga descargarArchivo(UsuarioPrincipal actor, UUID ordenId, UUID archivoId);

    void eliminarArchivo(UsuarioPrincipal actor, UUID ordenId, UUID archivoId);

    List<AgendaImagenResponse> agenda(LocalDate desde, LocalDate hasta);

    ImagenesResumenResponse resumen();

    MiFirmaImagenResponse miFirma(UsuarioPrincipal actor);
}
