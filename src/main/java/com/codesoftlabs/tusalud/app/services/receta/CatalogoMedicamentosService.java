package com.codesoftlabs.tusalud.app.services.receta;

import com.codesoftlabs.tusalud.app.dto.request.RecetaRequests.InteraccionRequest;
import com.codesoftlabs.tusalud.app.dto.request.RecetaRequests.MedicamentoRequest;
import com.codesoftlabs.tusalud.app.dto.request.RecetaRequests.PrincipioActivoRequest;
import com.codesoftlabs.tusalud.app.dto.response.RecetaResponses.ImportacionCatalogoResponse;
import com.codesoftlabs.tusalud.app.dto.response.RecetaResponses.InteraccionResponse;
import com.codesoftlabs.tusalud.app.dto.response.RecetaResponses.MedicamentoResponse;
import com.codesoftlabs.tusalud.app.dto.response.RecetaResponses.PrincipioActivoResponse;
import com.codesoftlabs.tusalud.app.entities.InteraccionMedicamentosa;
import com.codesoftlabs.tusalud.app.entities.Medicamento;
import com.codesoftlabs.tusalud.app.entities.PrincipioActivo;
import com.codesoftlabs.tusalud.app.entities.Producto;
import com.codesoftlabs.tusalud.app.entities.enums.RecetaEnums.CategoriaControl;
import com.codesoftlabs.tusalud.app.entities.enums.RecetaEnums.CategoriaEmbarazo;
import com.codesoftlabs.tusalud.app.entities.enums.RecetaEnums.CompatibilidadLactancia;
import com.codesoftlabs.tusalud.app.entities.enums.RecetaEnums.EstadoVenta;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.InteraccionMedicamentosaRepository;
import com.codesoftlabs.tusalud.app.repositories.MedicamentoRepository;
import com.codesoftlabs.tusalud.app.repositories.PrincipioActivoRepository;
import com.codesoftlabs.tusalud.app.repositories.ProductoRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogoMedicamentosService {

    public static final String CABECERA_CSV =
            "dci;clase_terapeutica;grupo_alergia;alias_alergia;dosis_max_adulto_mg_dia;dosis_max_pediatrica_mg_kg_dia;"
                    + "embarazo;lactancia;aviso_renal;forma;concentracion;via;presentacion;nombre_comercial;control;"
                    + "requiere_receta;codigo_producto";

    private static final int MAXIMO_FILAS = 5000;
    private static final int LIMITE_BUSQUEDA = 40;

    private final PrincipioActivoRepository principios;
    private final MedicamentoRepository medicamentos;
    private final InteraccionMedicamentosaRepository interacciones;
    private final ProductoRepository productos;

    public List<MedicamentoResponse> buscar(String q) {
        String termino = q == null || q.isBlank() ? null : "%" + q.strip().toLowerCase(Locale.ROOT) + "%";
        return medicamentos.buscar(termino, PageRequest.of(0, LIMITE_BUSQUEDA)).stream()
                .map(this::aRespuesta)
                .toList();
    }

    public MedicamentoResponse obtener(UUID id) {
        return aRespuesta(medicamentos
                .findConPrincipio(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medicamento", "id", id)));
    }

    public List<PrincipioActivoResponse> listarPrincipios() {
        return principios.findAllByOrderByDciAsc().stream()
                .map(this::aRespuesta)
                .toList();
    }

    @Transactional
    public PrincipioActivoResponse crearPrincipio(PrincipioActivoRequest r) {
        String dci = limpiar(r.dci());
        if (principios.findByDciIgnoreCase(dci).isPresent()) {
            throw new ConflictException("Ya existe el principio activo " + dci);
        }
        PrincipioActivo p = PrincipioActivo.builder().dci(dci).build();
        aplicar(p, r);
        return aRespuesta(principios.save(p));
    }

    @Transactional
    public PrincipioActivoResponse actualizarPrincipio(UUID id, PrincipioActivoRequest r) {
        PrincipioActivo p =
                principios.findById(id).orElseThrow(() -> new ResourceNotFoundException("Principio activo", "id", id));
        String dci = limpiar(r.dci());
        principios
                .findByDciIgnoreCase(dci)
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new ConflictException("Ya existe el principio activo " + dci);
                });
        p.setDci(dci);
        aplicar(p, r);
        return aRespuesta(p);
    }

    @Transactional
    public MedicamentoResponse crearMedicamento(MedicamentoRequest r) {
        PrincipioActivo principio = principios
                .findById(r.principioActivoId())
                .orElseThrow(() -> new ResourceNotFoundException("Principio activo", "id", r.principioActivoId()));
        String forma = limpiar(r.formaFarmaceutica());
        String concentracion = limpiar(r.concentracion());
        if (medicamentos.existeCombinacion(principio.getId(), forma, concentracion)) {
            throw new ConflictException("Ya existe " + principio.getDci() + " " + concentracion + " " + forma);
        }
        Medicamento m = Medicamento.builder().principioActivo(principio).build();
        aplicar(m, r, principio);
        return aRespuesta(medicamentos.save(m));
    }

    @Transactional
    public MedicamentoResponse actualizarMedicamento(UUID id, MedicamentoRequest r) {
        Medicamento m = medicamentos
                .findConPrincipio(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medicamento", "id", id));
        PrincipioActivo principio = principios
                .findById(r.principioActivoId())
                .orElseThrow(() -> new ResourceNotFoundException("Principio activo", "id", r.principioActivoId()));
        String forma = limpiar(r.formaFarmaceutica());
        String concentracion = limpiar(r.concentracion());
        medicamentos
                .findCombinacion(principio.getId(), forma, concentracion)
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new ConflictException("Ya existe " + principio.getDci() + " " + concentracion + " " + forma);
                });
        aplicar(m, r, principio);
        return aRespuesta(m);
    }

    public List<InteraccionResponse> listarInteracciones() {
        return interacciones.listar().stream().map(this::aRespuesta).toList();
    }

    @Transactional
    public InteraccionResponse crearInteraccion(InteraccionRequest r) {
        if (r.principioAId().equals(r.principioBId())) {
            throw new DatosInvalidosException("Elija dos principios activos distintos");
        }
        boolean ordenado = enOrden(r.principioAId(), r.principioBId());
        UUID idA = ordenado ? r.principioAId() : r.principioBId();
        UUID idB = ordenado ? r.principioBId() : r.principioAId();
        if (interacciones.findByPrincipioAIdAndPrincipioBId(idA, idB).isPresent()) {
            throw new ConflictException("La interacción entre esos principios activos ya está registrada");
        }
        PrincipioActivo a = principios
                .findById(idA)
                .orElseThrow(() -> new ResourceNotFoundException("Principio activo", "id", idA));
        PrincipioActivo b = principios
                .findById(idB)
                .orElseThrow(() -> new ResourceNotFoundException("Principio activo", "id", idB));
        InteraccionMedicamentosa i = InteraccionMedicamentosa.builder()
                .principioA(a)
                .principioB(b)
                .severidad(r.severidad())
                .descripcion(limpiar(r.descripcion()))
                .recomendacion(vacioANulo(r.recomendacion()))
                .build();
        return aRespuesta(interacciones.save(i));
    }

    @Transactional
    public void eliminarInteraccion(UUID id) {
        InteraccionMedicamentosa i =
                interacciones.findById(id).orElseThrow(() -> new ResourceNotFoundException("Interacción", "id", id));
        interacciones.delete(i);
    }

    static boolean enOrden(UUID primero, UUID segundo) {
        return primero.toString().compareTo(segundo.toString()) < 0;
    }

    public String plantillaCsv() {
        return CABECERA_CSV + "\n"
                + "Amoxicilina;Penicilinas;PENICILINAS;penicilina,amoxicilina;3000;90;B;COMPATIBLE;;Cápsula;500 mg;Oral;"
                + "Caja x 100;;ANTIBIOTICO;true;\n";
    }

    @Transactional
    public ImportacionCatalogoResponse importar(byte[] contenido, boolean soloValidar) {
        String texto = new String(contenido, StandardCharsets.UTF_8);
        if (!texto.isEmpty() && texto.charAt(0) == '﻿') {
            texto = texto.substring(1);
        }
        List<List<String>> registros = CsvSimple.leer(texto);
        List<String> errores = new ArrayList<>();
        if (registros.isEmpty()) {
            return new ImportacionCatalogoResponse(0, 0, List.of("El archivo está vacío"));
        }
        Map<String, Integer> columnas = CsvSimple.columnas(registros.get(0));
        for (String obligatoria : List.of("dci", "forma", "concentracion", "via")) {
            if (!columnas.containsKey(obligatoria)) {
                return new ImportacionCatalogoResponse(0, 0, List.of("Falta la columna obligatoria " + obligatoria));
            }
        }
        if (registros.size() - 1 > MAXIMO_FILAS) {
            return new ImportacionCatalogoResponse(
                    0, 0, List.of("El archivo supera el máximo de " + MAXIMO_FILAS + " filas"));
        }
        int creados = 0;
        int actualizados = 0;
        for (int i = 1; i < registros.size(); i++) {
            List<String> celdas = registros.get(i);
            if (celdas.stream().allMatch(String::isBlank)) {
                continue;
            }
            int fila = i + 1;
            try {
                String dci = CsvSimple.valor(celdas, columnas, "dci");
                String forma = CsvSimple.valor(celdas, columnas, "forma");
                String concentracion = CsvSimple.valor(celdas, columnas, "concentracion");
                String via = CsvSimple.valor(celdas, columnas, "via");
                if (dci.isEmpty() || forma.isEmpty() || concentracion.isEmpty() || via.isEmpty()) {
                    throw new DatosInvalidosException("Denominación, forma, concentración y vía son obligatorias");
                }
                PrincipioActivo principio = principios.findByDciIgnoreCase(dci).orElse(null);
                boolean principioNuevo = principio == null;
                if (principioNuevo) {
                    principio = PrincipioActivo.builder().dci(dci).build();
                }
                leerPrincipio(principio, celdas, columnas);
                if (!soloValidar) {
                    principio = principios.save(principio);
                }
                Medicamento medicamento = principioNuevo || soloValidar
                        ? null
                        : medicamentos
                                .findCombinacion(principio.getId(), forma, concentracion)
                                .orElse(null);
                boolean medicamentoNuevo = medicamento == null;
                if (medicamentoNuevo) {
                    medicamento = Medicamento.builder()
                            .principioActivo(principio)
                            .formaFarmaceutica(forma)
                            .concentracion(concentracion)
                            .viaAdministracion(via)
                            .build();
                }
                medicamento.setViaAdministracion(via);
                medicamento.setPresentacion(vacioANulo(CsvSimple.valor(celdas, columnas, "presentacion")));
                medicamento.setNombreComercial(vacioANulo(CsvSimple.valor(celdas, columnas, "nombre_comercial")));
                String control = CsvSimple.valor(celdas, columnas, "control");
                medicamento.setCategoriaControl(
                        control.isEmpty()
                                ? CategoriaControl.NINGUNA
                                : enumerado(CategoriaControl.class, control, "control"));
                String requiere = CsvSimple.valor(celdas, columnas, "requiere_receta");
                medicamento.setRequiereReceta(
                        requiere.isEmpty() || Boolean.parseBoolean(requiere.toLowerCase(Locale.ROOT)));
                String codigo = CsvSimple.valor(celdas, columnas, "codigo_producto");
                if (!codigo.isEmpty()) {
                    Producto producto = productos
                            .findByCodigo(codigo)
                            .orElseThrow(() -> new DatosInvalidosException("No existe el producto " + codigo));
                    medicamento.setProductoId(producto.getId());
                }
                if (!soloValidar) {
                    medicamentos.save(medicamento);
                }
                if (medicamentoNuevo) {
                    creados++;
                } else {
                    actualizados++;
                }
            } catch (DatosInvalidosException ex) {
                errores.add("Fila " + fila + ": " + ex.getMessage());
            }
        }
        return new ImportacionCatalogoResponse(creados, actualizados, errores);
    }

    private void leerPrincipio(PrincipioActivo p, List<String> celdas, Map<String, Integer> columnas) {
        String clase = CsvSimple.valor(celdas, columnas, "clase_terapeutica");
        if (!clase.isEmpty()) {
            p.setClaseTerapeutica(clase);
        }
        String grupo = CsvSimple.valor(celdas, columnas, "grupo_alergia");
        if (!grupo.isEmpty()) {
            p.setGrupoAlergia(grupo.toUpperCase(Locale.ROOT));
        }
        String alias = CsvSimple.valor(celdas, columnas, "alias_alergia");
        if (!alias.isEmpty()) {
            p.setAliasAlergia(alias);
        }
        String adulto = CsvSimple.valor(celdas, columnas, "dosis_max_adulto_mg_dia");
        if (!adulto.isEmpty()) {
            p.setDosisMaxAdultoMgDia(decimal(adulto, "dosis_max_adulto_mg_dia"));
        }
        String pediatrica = CsvSimple.valor(celdas, columnas, "dosis_max_pediatrica_mg_kg_dia");
        if (!pediatrica.isEmpty()) {
            p.setDosisMaxPediatricaMgKgDia(decimal(pediatrica, "dosis_max_pediatrica_mg_kg_dia"));
        }
        String embarazo = CsvSimple.valor(celdas, columnas, "embarazo");
        if (!embarazo.isEmpty()) {
            p.setCategoriaEmbarazo(enumerado(CategoriaEmbarazo.class, embarazo, "embarazo"));
        }
        String lactancia = CsvSimple.valor(celdas, columnas, "lactancia");
        if (!lactancia.isEmpty()) {
            p.setLactancia(enumerado(CompatibilidadLactancia.class, lactancia, "lactancia"));
        }
        String renal = CsvSimple.valor(celdas, columnas, "aviso_renal");
        if (!renal.isEmpty()) {
            p.setAvisoRenal(renal);
        }
    }

    private static <E extends Enum<E>> E enumerado(Class<E> tipo, String texto, String columna) {
        try {
            return Enum.valueOf(tipo, texto.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new DatosInvalidosException("Valor no válido en la columna " + columna + ": " + texto);
        }
    }

    private static BigDecimal decimal(String texto, String columna) {
        try {
            return new BigDecimal(texto.strip().replace(',', '.'));
        } catch (NumberFormatException ex) {
            throw new DatosInvalidosException("La columna " + columna + " debe ser numérica");
        }
    }

    private void aplicar(PrincipioActivo p, PrincipioActivoRequest r) {
        p.setClaseTerapeutica(vacioANulo(r.claseTerapeutica()));
        p.setGrupoAlergia(
                r.grupoAlergia() == null || r.grupoAlergia().isBlank()
                        ? null
                        : r.grupoAlergia().strip().toUpperCase(Locale.ROOT));
        p.setAliasAlergia(vacioANulo(r.aliasAlergia()));
        p.setDosisMaxAdultoMgDia(r.dosisMaxAdultoMgDia());
        p.setDosisMaxPediatricaMgKgDia(r.dosisMaxPediatricaMgKgDia());
        p.setCategoriaEmbarazo(
                r.categoriaEmbarazo() == null ? CategoriaEmbarazo.SIN_CLASIFICAR : r.categoriaEmbarazo());
        p.setLactancia(r.lactancia() == null ? CompatibilidadLactancia.SIN_CLASIFICAR : r.lactancia());
        p.setAvisoRenal(vacioANulo(r.avisoRenal()));
    }

    private void aplicar(Medicamento m, MedicamentoRequest r, PrincipioActivo principio) {
        m.setPrincipioActivo(principio);
        m.setNombreComercial(vacioANulo(r.nombreComercial()));
        m.setFormaFarmaceutica(limpiar(r.formaFarmaceutica()));
        m.setConcentracion(limpiar(r.concentracion()));
        m.setViaAdministracion(limpiar(r.viaAdministracion()));
        m.setPresentacion(vacioANulo(r.presentacion()));
        m.setCategoriaControl(r.categoriaControl() == null ? CategoriaControl.NINGUNA : r.categoriaControl());
        m.setRequiereReceta(r.requiereReceta() == null || r.requiereReceta());
        m.setEstadoVenta(r.estadoVenta() == null ? EstadoVenta.VIGENTE : r.estadoVenta());
        if (r.productoId() != null && !productos.existsById(r.productoId())) {
            throw new DatosInvalidosException("El producto de farmacia indicado no existe");
        }
        m.setProductoId(r.productoId());
        m.setEnPetitorio(r.enPetitorio() == null || r.enPetitorio());
        m.setActivo(r.activo() == null || r.activo());
    }

    private PrincipioActivoResponse aRespuesta(PrincipioActivo p) {
        return new PrincipioActivoResponse(
                p.getId(),
                p.getDci(),
                p.getClaseTerapeutica(),
                p.getGrupoAlergia(),
                p.getAliasAlergia(),
                p.getDosisMaxAdultoMgDia(),
                p.getDosisMaxPediatricaMgKgDia(),
                p.getCategoriaEmbarazo(),
                p.getLactancia(),
                p.getAvisoRenal(),
                p.isActivo());
    }

    private MedicamentoResponse aRespuesta(Medicamento m) {
        PrincipioActivo p = m.getPrincipioActivo();
        return new MedicamentoResponse(
                m.getId(),
                p.getId(),
                p.getDci(),
                p.getClaseTerapeutica(),
                m.getNombreComercial(),
                m.getFormaFarmaceutica(),
                m.getConcentracion(),
                m.getViaAdministracion(),
                m.getPresentacion(),
                m.getCategoriaControl(),
                m.isRequiereReceta(),
                m.getEstadoVenta(),
                m.getProductoId(),
                m.isEnPetitorio(),
                m.isActivo(),
                m.nombreCompleto(),
                true);
    }

    private InteraccionResponse aRespuesta(InteraccionMedicamentosa i) {
        return new InteraccionResponse(
                i.getId(),
                i.getPrincipioA().getId(),
                i.getPrincipioA().getDci(),
                i.getPrincipioB().getId(),
                i.getPrincipioB().getDci(),
                i.getSeveridad(),
                i.getDescripcion(),
                i.getRecomendacion());
    }

    private static String limpiar(String texto) {
        return texto == null ? "" : texto.strip();
    }

    private static String vacioANulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }
}
