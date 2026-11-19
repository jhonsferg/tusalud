package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.dto.response.Cie10FrecuenteResponse;
import com.codesoftlabs.tusalud.app.dto.response.Cie10ImportacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.Cie10Response;
import com.codesoftlabs.tusalud.app.dto.response.Cie10ResumenResponse;
import com.codesoftlabs.tusalud.app.dto.response.Cie10VersionResponse;
import com.codesoftlabs.tusalud.app.entities.Cie10Catalogo;
import com.codesoftlabs.tusalud.app.entities.Cie10Version;
import com.codesoftlabs.tusalud.app.entities.enums.OrigenCie10;
import com.codesoftlabs.tusalud.app.entities.enums.TipoEventoSeguridad;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.AtencionDiagnosticoRepository;
import com.codesoftlabs.tusalud.app.repositories.Cie10CatalogoRepository;
import com.codesoftlabs.tusalud.app.repositories.Cie10VersionRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.cie10.Cie10CsvParser;
import com.codesoftlabs.tusalud.app.services.cie10.Cie10Normalizador;
import com.codesoftlabs.tusalud.app.services.interfaces.Cie10Service;
import com.codesoftlabs.tusalud.app.services.interfaces.EventoSeguridadService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class Cie10ServiceImpl implements Cie10Service {

    private static final int LIMITE_MAXIMO = 30;
    private static final int CANDIDATOS_MULTITERMINO = 300;
    private static final long TAMANO_MAXIMO_BYTES = 5L * 1024 * 1024;
    private static final int ERRORES_VISIBLES = 50;
    private static final int FILAS_DE_MUESTRA = 20;
    private static final int DIAS_FRECUENTES = 90;

    private final Cie10CatalogoRepository catalogoRepository;
    private final Cie10VersionRepository versionRepository;
    private final AtencionDiagnosticoRepository diagnosticoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EventoSeguridadService eventoSeguridadService;

    @Override
    public List<Cie10Response> buscar(String consulta, int limite) {
        String normalizada = Cie10Normalizador.sinComodines(Cie10Normalizador.texto(consulta));
        if (normalizada.length() < 2) {
            return List.of();
        }
        int tope = Math.max(1, Math.min(limite, LIMITE_MAXIMO));
        String[] terminos = normalizada.split(" ");
        String principal = terminos[0];
        for (String termino : terminos) {
            if (termino.length() > principal.length()) {
                principal = termino;
            }
        }
        String exacto = normalizada.replace(" ", "");
        int candidatos = terminos.length > 1 ? CANDIDATOS_MULTITERMINO : tope;
        List<Cie10Catalogo> encontrados = catalogoRepository.buscar(
                "%" + principal + "%", exacto, normalizada + "%", normalizada + "%", PageRequest.of(0, candidatos));
        List<Cie10Response> resultado = new ArrayList<>();
        for (Cie10Catalogo c : encontrados) {
            boolean coincide = true;
            for (String termino : terminos) {
                if (!c.getTextoBusqueda().contains(termino)) {
                    coincide = false;
                    break;
                }
            }
            if (coincide) {
                resultado.add(Cie10Response.from(c));
            }
            if (resultado.size() >= tope) {
                break;
            }
        }
        return resultado;
    }

    @Override
    public Cie10Response obtener(String codigo) {
        return Cie10Response.from(buscarCodigo(codigo));
    }

    @Override
    public List<Cie10FrecuenteResponse> frecuentes(UsuarioPrincipal medico, int limite) {
        int tope = Math.max(1, Math.min(limite, LIMITE_MAXIMO));
        return diagnosticoRepository
                .frecuentesDelMedico(
                        medico.getId(), LocalDateTime.now().minusDays(DIAS_FRECUENTES), PageRequest.of(0, tope))
                .stream()
                .map(f -> new Cie10FrecuenteResponse(f.getCodigo(), f.getDescripcion(), f.getCasos()))
                .toList();
    }

    @Override
    public Cie10Catalogo exigirActivo(String codigo) {
        Cie10Catalogo catalogo = catalogoRepository
                .findByCodigo(Cie10Normalizador.codigo(codigo))
                .orElseThrow(
                        () -> new DatosInvalidosException("El código CIE-10 " + codigo + " no existe en el catálogo"));
        if (!catalogo.isActivo()) {
            throw new DatosInvalidosException("El código CIE-10 " + catalogo.getCodigo() + " está inactivo");
        }
        return catalogo;
    }

    @Override
    public Cie10ResumenResponse resumen() {
        List<Cie10VersionResponse> versiones = versionRepository.findAllByOrderByCreadaEnDesc().stream()
                .map(Cie10VersionResponse::from)
                .toList();
        long activos = catalogoRepository.countByActivoTrue();
        long muestra = catalogoRepository.countByMuestraTrue();
        Cie10VersionResponse vigente = versiones.stream()
                .filter(Cie10VersionResponse::activa)
                .findFirst()
                .orElse(null);
        return new Cie10ResumenResponse(
                activos, muestra, muestra > 0 && muestra == catalogoRepository.count(), vigente, versiones);
    }

    @Override
    public byte[] plantillaCsv() {
        String contenido = Cie10CsvParser.CABECERA
                + "\nI10,Hipertensión esencial (primaria),Enfermedades del sistema circulatorio,I10-I15 Enfermedades hipertensivas,,,"
                + "\nN40,Hiperplasia de la próstata,Enfermedades del sistema genitourinario,N40-N51 Enfermedades de los órganos genitales masculinos,M,40,"
                + "\nO80,Parto único espontáneo,Embarazo parto y puerperio,O80-O84 Parto,F,10,55\n";
        return ("\uFEFF" + contenido).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    @Transactional
    public Cie10ImportacionResponse importar(
            MultipartFile archivo,
            String etiqueta,
            boolean aplicar,
            boolean desactivarAusentes,
            boolean omitirErrores,
            UsuarioPrincipal administrador) {
        if (archivo == null || archivo.isEmpty()) {
            throw new DatosInvalidosException("Adjunte un archivo CSV con el catálogo");
        }
        if (archivo.getSize() > TAMANO_MAXIMO_BYTES) {
            throw new DatosInvalidosException("El archivo supera el máximo de 5 MB");
        }
        byte[] contenido;
        try {
            contenido = archivo.getBytes();
        } catch (IOException e) {
            throw new DatosInvalidosException("No se pudo leer el archivo");
        }
        Cie10CsvParser.Resultado leido = Cie10CsvParser.parsear(contenido);
        List<Cie10CsvParser.Error> errores = leido.errores();
        if (aplicar) {
            String nombre = etiqueta == null ? "" : etiqueta.trim();
            if (nombre.isEmpty() || nombre.length() > 60) {
                throw new DatosInvalidosException("Indique la etiqueta de la versión, de hasta 60 caracteres");
            }
            if (!errores.isEmpty() && !omitirErrores) {
                throw new DatosInvalidosException("El archivo tiene " + errores.size()
                        + " filas con errores. Corríjalas o importe solo las filas válidas");
            }
            if (leido.validas().isEmpty()) {
                throw new DatosInvalidosException("El archivo no tiene filas válidas para importar");
            }
        }

        Map<String, Cie10Catalogo> existentes = new HashMap<>();
        List<String> codigos =
                leido.validas().stream().map(Cie10CsvParser.Fila::codigo).toList();
        for (int i = 0; i < codigos.size(); i += 500) {
            for (Cie10Catalogo c :
                    catalogoRepository.findByCodigoIn(codigos.subList(i, Math.min(codigos.size(), i + 500)))) {
                existentes.put(c.getCodigo(), c);
            }
        }

        Cie10Version version = null;
        if (aplicar) {
            versionRepository.findAll().forEach(v -> v.setActiva(false));
            version = versionRepository.save(Cie10Version.builder()
                    .etiqueta(etiqueta.trim())
                    .origen(OrigenCie10.IMPORTACION)
                    .creadaPor(usuarioRepository.getReferenceById(administrador.getId()))
                    .creadaEn(LocalDateTime.now())
                    .activa(true)
                    .build());
        }

        int altas = 0;
        int cambios = 0;
        int sinCambios = 0;
        List<Cie10ImportacionResponse.Fila> muestra = new ArrayList<>();
        List<Cie10Catalogo> nuevos = new ArrayList<>();
        for (Cie10CsvParser.Fila fila : leido.validas()) {
            Cie10Catalogo actual = existentes.get(fila.codigo());
            String accion;
            if (actual == null) {
                altas++;
                accion = "ALTA";
                if (aplicar) {
                    nuevos.add(Cie10Catalogo.builder()
                            .codigo(fila.codigo())
                            .descripcion(fila.descripcion())
                            .textoBusqueda(Cie10Normalizador.busqueda(fila.codigo(), fila.descripcion()))
                            .capitulo(fila.capitulo())
                            .grupo(fila.grupo())
                            .sexo(fila.sexo())
                            .edadMinima(fila.edadMinima())
                            .edadMaxima(fila.edadMaxima())
                            .activo(true)
                            .muestra(false)
                            .version(version)
                            .actualizadoEn(LocalDateTime.now())
                            .build());
                }
            } else if (difiere(actual, fila)) {
                cambios++;
                accion = "CAMBIO";
                if (aplicar) {
                    actual.setDescripcion(fila.descripcion());
                    actual.setTextoBusqueda(Cie10Normalizador.busqueda(fila.codigo(), fila.descripcion()));
                    actual.setCapitulo(fila.capitulo());
                    actual.setGrupo(fila.grupo());
                    actual.setSexo(fila.sexo());
                    actual.setEdadMinima(fila.edadMinima());
                    actual.setEdadMaxima(fila.edadMaxima());
                    actual.setActivo(true);
                    actual.setMuestra(false);
                    actual.setVersion(version);
                    actual.setActualizadoEn(LocalDateTime.now());
                }
            } else {
                sinCambios++;
                accion = "SIN_CAMBIO";
            }
            if (muestra.size() < FILAS_DE_MUESTRA) {
                muestra.add(new Cie10ImportacionResponse.Fila(
                        fila.codigo(),
                        fila.descripcion(),
                        fila.capitulo(),
                        fila.grupo(),
                        fila.sexo() == null ? null : fila.sexo().name(),
                        fila.edadMinima(),
                        fila.edadMaxima(),
                        accion));
            }
        }
        if (aplicar) {
            catalogoRepository.saveAll(nuevos);
        }

        int desactivados = 0;
        if (desactivarAusentes) {
            Set<String> enArchivo = new HashSet<>(codigos);
            for (Cie10Catalogo c : catalogoRepository.findAll()) {
                if (c.isActivo() && !enArchivo.contains(c.getCodigo())) {
                    desactivados++;
                    if (aplicar) {
                        c.setActivo(false);
                        c.setActualizadoEn(LocalDateTime.now());
                    }
                }
            }
        }

        if (aplicar) {
            catalogoRepository.flush();
            version.setAltas(altas);
            version.setCambios(cambios);
            version.setTotal((int) catalogoRepository.countByActivoTrue());
            eventoSeguridadService.registrar(
                    TipoEventoSeguridad.CATALOGO_CIE10_IMPORTADO,
                    null,
                    administrador.getId(),
                    "Versión " + version.getEtiqueta() + ": " + altas + " altas, " + cambios + " cambios, "
                            + desactivados + " desactivados");
            log.info("Catálogo CIE-10 importado: {} altas, {} cambios", altas, cambios);
        }

        List<Cie10ImportacionResponse.Error> visibles = errores.stream()
                .limit(ERRORES_VISIBLES)
                .map(e -> new Cie10ImportacionResponse.Error(e.fila(), e.mensaje()))
                .toList();
        return new Cie10ImportacionResponse(
                aplicar,
                leido.filas(),
                leido.validas().size(),
                altas,
                cambios,
                sinCambios,
                desactivados,
                errores.size(),
                visibles,
                muestra);
    }

    private static boolean difiere(Cie10Catalogo c, Cie10CsvParser.Fila f) {
        return !c.isActivo()
                || c.isMuestra()
                || !Objects.equals(c.getDescripcion(), f.descripcion())
                || !Objects.equals(c.getCapitulo(), f.capitulo())
                || !Objects.equals(c.getGrupo(), f.grupo())
                || c.getSexo() != f.sexo()
                || !Objects.equals(c.getEdadMinima(), f.edadMinima())
                || !Objects.equals(c.getEdadMaxima(), f.edadMaxima());
    }

    private Cie10Catalogo buscarCodigo(String codigo) {
        return catalogoRepository
                .findByCodigo(Cie10Normalizador.codigo(codigo))
                .orElseThrow(() -> new ResourceNotFoundException("Diagnóstico CIE-10", "código", codigo));
    }

    static Comparator<Cie10Response> porCodigo() {
        return Comparator.comparing(Cie10Response::codigo);
    }
}
