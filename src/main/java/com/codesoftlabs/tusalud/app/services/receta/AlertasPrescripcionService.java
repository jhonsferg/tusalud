package com.codesoftlabs.tusalud.app.services.receta;

import com.codesoftlabs.tusalud.app.dto.request.RecetaRequests.ItemPrescripcion;
import com.codesoftlabs.tusalud.app.dto.response.RecetaResponses.AlertaResponse;
import com.codesoftlabs.tusalud.app.entities.Atencion;
import com.codesoftlabs.tusalud.app.entities.InteraccionMedicamentosa;
import com.codesoftlabs.tusalud.app.entities.Medicamento;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.PrincipioActivo;
import com.codesoftlabs.tusalud.app.entities.RecetaElectronica;
import com.codesoftlabs.tusalud.app.entities.RecetaElectronicaItem;
import com.codesoftlabs.tusalud.app.entities.enums.RecetaEnums.CategoriaEmbarazo;
import com.codesoftlabs.tusalud.app.entities.enums.RecetaEnums.CompatibilidadLactancia;
import com.codesoftlabs.tusalud.app.entities.enums.RecetaEnums.EstadoRecetaElectronica;
import com.codesoftlabs.tusalud.app.entities.enums.RecetaEnums.EstadoVenta;
import com.codesoftlabs.tusalud.app.entities.enums.RecetaEnums.SeveridadAlerta;
import com.codesoftlabs.tusalud.app.entities.enums.RecetaEnums.TipoAlertaReceta;
import com.codesoftlabs.tusalud.app.entities.enums.SexoPaciente;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.repositories.AtencionRepository;
import com.codesoftlabs.tusalud.app.repositories.InteraccionMedicamentosaRepository;
import com.codesoftlabs.tusalud.app.repositories.MedicamentoRepository;
import com.codesoftlabs.tusalud.app.repositories.PrincipioActivoRepository;
import com.codesoftlabs.tusalud.app.repositories.RecetaElectronicaRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlertasPrescripcionService {

    public static final int JUSTIFICACION_GRAVE = 10;
    public static final int JUSTIFICACION_CONTRAINDICADA = 30;

    private static final Map<String, Set<String>> REACCIONES_CRUZADAS = Map.of(
            "PENICILINAS", Set.of("CEFALOSPORINAS", "CARBAPENEMICOS"),
            "CEFALOSPORINAS", Set.of("PENICILINAS"),
            "AINE", Set.of("AINE"),
            "SULFAS", Set.of("SULFAS"),
            "MACROLIDOS", Set.of("MACROLIDOS"),
            "QUINOLONAS", Set.of("QUINOLONAS"),
            "OPIOIDES", Set.of("OPIOIDES"));

    private final MedicamentoRepository medicamentos;
    private final InteraccionMedicamentosaRepository interacciones;
    private final RecetaElectronicaRepository recetas;
    private final AtencionRepository atenciones;
    private final PrincipioActivoRepository principiosActivos;

    public record Alerta(
            String clave,
            TipoAlertaReceta tipo,
            SeveridadAlerta severidad,
            String mensaje,
            String recomendacion,
            String medicamento) {

        public boolean requiereJustificacion() {
            return severidad == SeveridadAlerta.GRAVE || severidad == SeveridadAlerta.CONTRAINDICADA;
        }

        public int minimoJustificacion() {
            return switch (severidad) {
                case CONTRAINDICADA -> JUSTIFICACION_CONTRAINDICADA;
                case GRAVE -> JUSTIFICACION_GRAVE;
                default -> 0;
            };
        }
    }

    public List<Alerta> evaluar(
            Paciente paciente, List<ItemPrescripcion> items, boolean embarazada, boolean lactando, UUID excluirReceta) {
        Map<UUID, Medicamento> catalogo = cargar(items);
        List<Alerta> alertas = new ArrayList<>();
        Set<String> claves = new HashSet<>();
        LocalDate hoy = LocalDate.now();
        Integer edad = paciente.getFechaNacimiento() == null
                ? null
                : Period.between(paciente.getFechaNacimiento(), hoy).getYears();
        List<RecetaElectronicaItem> tratamiento = tratamientoVigente(paciente.getId(), excluirReceta);

        Map<UUID, PrincipioActivo> principiosDeLaReceta = new LinkedHashMap<>();
        for (ItemPrescripcion item : items) {
            PrincipioActivo principio = catalogo.get(item.medicamentoId()).getPrincipioActivo();
            principiosDeLaReceta.putIfAbsent(principio.getId(), principio);
        }

        alergias(paciente, items, catalogo, principiosDeLaReceta.values(), alertas, claves);
        duplicidad(items, catalogo, tratamiento, alertas, claves);
        interaccionesMedicamentosas(items, catalogo, tratamiento, alertas, claves);
        dosisMaxima(paciente, edad, items, catalogo, alertas, claves);
        embarazoYLactancia(paciente, edad, embarazada, lactando, items, catalogo, alertas, claves);
        avisoRenal(items, catalogo, alertas, claves);

        alertas.sort(Comparator.comparing(Alerta::severidad)
                .reversed()
                .thenComparing(Alerta::tipo)
                .thenComparing(Alerta::clave));
        return alertas;
    }

    public AlertaResponse aRespuesta(Alerta a) {
        return new AlertaResponse(
                a.clave(),
                a.tipo(),
                a.severidad(),
                a.mensaje(),
                a.recomendacion(),
                a.medicamento(),
                a.requiereJustificacion(),
                a.minimoJustificacion());
    }

    private Map<UUID, Medicamento> cargar(List<ItemPrescripcion> items) {
        Set<UUID> ids = items.stream().map(ItemPrescripcion::medicamentoId).collect(Collectors.toSet());
        Map<UUID, Medicamento> catalogo = new HashMap<>();
        for (Medicamento m : medicamentos.findConPrincipioPorIds(ids)) {
            catalogo.put(m.getId(), m);
        }
        for (UUID id : ids) {
            Medicamento m = catalogo.get(id);
            if (m == null) {
                throw new DatosInvalidosException("El medicamento " + id + " no existe en el catálogo");
            }
            if (!m.isActivo() || m.getEstadoVenta() != EstadoVenta.VIGENTE) {
                throw new DatosInvalidosException(
                        "El medicamento " + m.nombreCompleto() + " no está disponible para prescribir");
            }
        }
        return catalogo;
    }

    private List<RecetaElectronicaItem> tratamientoVigente(UUID pacienteId, UUID excluirReceta) {
        List<RecetaElectronicaItem> resultado = new ArrayList<>();
        for (RecetaElectronica r : recetas.activasDelPaciente(
                pacienteId,
                EnumSet.of(EstadoRecetaElectronica.EMITIDA, EstadoRecetaElectronica.DISPENSADA_PARCIAL),
                LocalDateTime.now())) {
            if (r.getId().equals(excluirReceta)) {
                continue;
            }
            for (RecetaElectronicaItem item : r.getItems()) {
                if (item.getMedicamento() != null) {
                    resultado.add(item);
                }
            }
        }
        return resultado;
    }

    private void alergias(
            Paciente paciente,
            List<ItemPrescripcion> items,
            Map<UUID, Medicamento> catalogo,
            java.util.Collection<PrincipioActivo> principios,
            List<Alerta> alertas,
            Set<String> claves) {
        List<String> tokens = tokensDeAlergia(paciente.getAlergias());
        if (tokens.isEmpty()) {
            return;
        }
        List<PrincipioActivo> todos = principiosActivos.findAllByOrderByDciAsc();
        for (String token : tokens) {
            Set<String> gruposAlergicos = new HashSet<>();
            for (PrincipioActivo candidato : todos) {
                if (coincideDirecto(token, candidato) && candidato.getGrupoAlergia() != null) {
                    gruposAlergicos.add(candidato.getGrupoAlergia());
                }
            }
            String grupoNombrado = token.toUpperCase(Locale.ROOT);
            if (REACCIONES_CRUZADAS.containsKey(grupoNombrado)) {
                gruposAlergicos.add(grupoNombrado);
            }
            Set<UUID> yaMarcados = new HashSet<>();
            for (ItemPrescripcion item : items) {
                Medicamento m = catalogo.get(item.medicamentoId());
                PrincipioActivo p = m.getPrincipioActivo();
                if (!yaMarcados.add(p.getId())) {
                    continue;
                }
                if (coincideDirecto(token, p)
                        || (p.getGrupoAlergia() != null
                                && normalizar(p.getGrupoAlergia()).equals(token))) {
                    agregar(
                            alertas,
                            claves,
                            new Alerta(
                                    "ALERGIA:" + p.getId() + ":" + token,
                                    TipoAlertaReceta.ALERGIA,
                                    SeveridadAlerta.CONTRAINDICADA,
                                    "El paciente tiene registrada una alergia a " + token + " y se prescribe "
                                            + p.getDci() + ".",
                                    "No prescribir salvo que el médico documente una justificación.",
                                    m.nombreCompleto()));
                } else if (p.getGrupoAlergia() != null && cruzada(gruposAlergicos, p.getGrupoAlergia())) {
                    agregar(
                            alertas,
                            claves,
                            new Alerta(
                                    "ALERGIA:" + p.getId() + ":" + token,
                                    TipoAlertaReceta.ALERGIA,
                                    SeveridadAlerta.GRAVE,
                                    "Posible reacción cruzada: el paciente tiene registrada una alergia a " + token
                                            + " y " + p.getDci() + " pertenece al grupo " + p.getGrupoAlergia() + ".",
                                    "Valorar una alternativa o documentar la justificación.",
                                    m.nombreCompleto()));
                }
            }
        }
    }

    private boolean cruzada(Set<String> gruposAlergicos, String grupoPrescrito) {
        for (String alergico : gruposAlergicos) {
            Set<String> cruzados = REACCIONES_CRUZADAS.get(alergico);
            if (cruzados != null && cruzados.contains(grupoPrescrito)) {
                return true;
            }
        }
        return false;
    }

    private boolean coincideDirecto(String token, PrincipioActivo p) {
        String dci = normalizar(p.getDci());
        if (token.equals(dci)) {
            return true;
        }
        if (token.length() >= 5 && (dci.contains(token) || token.contains(dci))) {
            return true;
        }
        if (p.getAliasAlergia() != null) {
            for (String alias : p.getAliasAlergia().split(",")) {
                String a = normalizar(alias);
                if (!a.isEmpty()
                        && (a.equals(token) || (token.length() >= 5 && (a.contains(token) || token.contains(a))))) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<String> tokensDeAlergia(String alergias) {
        if (alergias == null || alergias.isBlank()) {
            return List.of();
        }
        List<String> tokens = new ArrayList<>();
        for (String parte : alergias.split("[,;/\\n]+| y ")) {
            String t = normalizar(parte);
            if (t.length() >= 3 && !t.startsWith("niega") && !t.equals("ninguna") && !t.equals("no refiere")) {
                tokens.add(t);
            }
        }
        return tokens;
    }

    private void duplicidad(
            List<ItemPrescripcion> items,
            Map<UUID, Medicamento> catalogo,
            List<RecetaElectronicaItem> tratamiento,
            List<Alerta> alertas,
            Set<String> claves) {
        Map<UUID, List<Medicamento>> porPrincipio = new LinkedHashMap<>();
        for (ItemPrescripcion item : items) {
            Medicamento m = catalogo.get(item.medicamentoId());
            porPrincipio
                    .computeIfAbsent(m.getPrincipioActivo().getId(), k -> new ArrayList<>())
                    .add(m);
        }
        for (Map.Entry<UUID, List<Medicamento>> e : porPrincipio.entrySet()) {
            Medicamento primero = e.getValue().get(0);
            if (e.getValue().size() > 1) {
                agregar(
                        alertas,
                        claves,
                        new Alerta(
                                "DUPLICIDAD:" + e.getKey(),
                                TipoAlertaReceta.DUPLICIDAD,
                                SeveridadAlerta.GRAVE,
                                "El principio activo "
                                        + primero.getPrincipioActivo().getDci()
                                        + " aparece más de una vez en la receta.",
                                "Retire el duplicado o documente la justificación.",
                                primero.nombreCompleto()));
            }
            for (RecetaElectronicaItem previo : tratamiento) {
                if (previo.getMedicamento().getPrincipioActivo().getId().equals(e.getKey())) {
                    agregar(
                            alertas,
                            claves,
                            new Alerta(
                                    "DUPLICIDAD:" + e.getKey() + ":tratamiento",
                                    TipoAlertaReceta.DUPLICIDAD,
                                    SeveridadAlerta.MODERADA,
                                    primero.getPrincipioActivo().getDci() + " ya figura en el tratamiento vigente "
                                            + previo.getReceta().getNumero() + ".",
                                    "Confirme que no se trata de una prescripción repetida.",
                                    primero.nombreCompleto()));
                    break;
                }
            }
        }
        Map<String, List<PrincipioActivo>> porClase = new LinkedHashMap<>();
        for (Map.Entry<UUID, List<Medicamento>> e : porPrincipio.entrySet()) {
            PrincipioActivo p = e.getValue().get(0).getPrincipioActivo();
            if (p.getClaseTerapeutica() != null) {
                porClase.computeIfAbsent(p.getClaseTerapeutica(), k -> new ArrayList<>())
                        .add(p);
            }
        }
        for (Map.Entry<String, List<PrincipioActivo>> e : porClase.entrySet()) {
            if (e.getValue().size() > 1) {
                String nombres =
                        e.getValue().stream().map(PrincipioActivo::getDci).collect(Collectors.joining(", "));
                agregar(
                        alertas,
                        claves,
                        new Alerta(
                                "DUPLICIDAD:clase:" + normalizar(e.getKey()),
                                TipoAlertaReceta.DUPLICIDAD,
                                SeveridadAlerta.MODERADA,
                                "Duplicidad terapéutica en la clase " + e.getKey() + ": " + nombres + ".",
                                "Valore si se necesitan ambos medicamentos.",
                                nombres));
            }
        }
    }

    private void interaccionesMedicamentosas(
            List<ItemPrescripcion> items,
            Map<UUID, Medicamento> catalogo,
            List<RecetaElectronicaItem> tratamiento,
            List<Alerta> alertas,
            Set<String> claves) {
        Set<UUID> recetados = new HashSet<>();
        Map<UUID, String> nombres = new HashMap<>();
        for (ItemPrescripcion item : items) {
            PrincipioActivo p = catalogo.get(item.medicamentoId()).getPrincipioActivo();
            recetados.add(p.getId());
            nombres.put(p.getId(), p.getDci());
        }
        Set<UUID> activos = new HashSet<>();
        for (RecetaElectronicaItem previo : tratamiento) {
            PrincipioActivo p = previo.getMedicamento().getPrincipioActivo();
            activos.add(p.getId());
            nombres.putIfAbsent(p.getId(), p.getDci());
        }
        Set<UUID> todos = new HashSet<>(recetados);
        todos.addAll(activos);
        if (todos.size() < 2) {
            return;
        }
        for (InteraccionMedicamentosa i : interacciones.entre(todos)) {
            UUID a = i.getPrincipioA().getId();
            UUID b = i.getPrincipioB().getId();
            if (!recetados.contains(a) && !recetados.contains(b)) {
                continue;
            }
            SeveridadAlerta severidad = switch (i.getSeveridad()) {
                case CONTRAINDICADA -> SeveridadAlerta.CONTRAINDICADA;
                case GRAVE -> SeveridadAlerta.GRAVE;
                case MODERADA -> SeveridadAlerta.MODERADA;
                case LEVE -> SeveridadAlerta.INFORMATIVA;
            };
            boolean contraTratamiento = !(recetados.contains(a) && recetados.contains(b));
            agregar(
                    alertas,
                    claves,
                    new Alerta(
                            "INTERACCION:" + a + ":" + b,
                            TipoAlertaReceta.INTERACCION,
                            severidad,
                            "Interacción " + i.getSeveridad().name().toLowerCase(Locale.ROOT) + " entre "
                                    + nombres.get(a) + " y " + nombres.get(b)
                                    + (contraTratamiento ? ", este último del tratamiento vigente" : "") + ". "
                                    + i.getDescripcion(),
                            i.getRecomendacion(),
                            nombres.get(a) + " + " + nombres.get(b)));
        }
    }

    private void dosisMaxima(
            Paciente paciente,
            Integer edad,
            List<ItemPrescripcion> items,
            Map<UUID, Medicamento> catalogo,
            List<Alerta> alertas,
            Set<String> claves) {
        Map<UUID, BigDecimal> totales = new LinkedHashMap<>();
        for (ItemPrescripcion item : items) {
            if (item.dosisUnitariaMg() == null || item.vecesPorDia() == null) {
                continue;
            }
            UUID principio =
                    catalogo.get(item.medicamentoId()).getPrincipioActivo().getId();
            totales.merge(
                    principio,
                    item.dosisUnitariaMg().multiply(BigDecimal.valueOf(item.vecesPorDia())),
                    BigDecimal::add);
        }
        BigDecimal peso = null;
        boolean pediatrico = edad != null && edad < 12;
        if (pediatrico) {
            peso = atenciones
                    .findFirstByPacienteIdAndPesoKgIsNotNullOrderByFechaDesc(paciente.getId())
                    .map(Atencion::getPesoKg)
                    .orElse(null);
        }
        for (Map.Entry<UUID, BigDecimal> e : totales.entrySet()) {
            PrincipioActivo p = items.stream()
                    .map(i -> catalogo.get(i.medicamentoId()).getPrincipioActivo())
                    .filter(x -> x.getId().equals(e.getKey()))
                    .findFirst()
                    .orElseThrow();
            BigDecimal total = e.getValue();
            if (pediatrico && p.getDosisMaxPediatricaMgKgDia() != null) {
                if (peso == null) {
                    agregar(
                            alertas,
                            claves,
                            new Alerta(
                                    "DOSIS:" + p.getId() + ":sinpeso",
                                    TipoAlertaReceta.DOSIS_MAXIMA,
                                    SeveridadAlerta.INFORMATIVA,
                                    "No hay un peso registrado para validar la dosis pediátrica de " + p.getDci() + ".",
                                    "Registre el peso del paciente en la atención.",
                                    p.getDci()));
                    continue;
                }
                BigDecimal limite =
                        p.getDosisMaxPediatricaMgKgDia().multiply(peso).setScale(2, RoundingMode.HALF_UP);
                if (total.compareTo(limite) > 0) {
                    agregar(
                            alertas,
                            claves,
                            new Alerta(
                                    "DOSIS:" + p.getId(),
                                    TipoAlertaReceta.DOSIS_MAXIMA,
                                    SeveridadAlerta.GRAVE,
                                    "La dosis diaria de " + p.getDci() + " es "
                                            + total.stripTrailingZeros().toPlainString()
                                            + " mg y supera el máximo pediátrico de "
                                            + limite.stripTrailingZeros().toPlainString() + " mg para "
                                            + peso.stripTrailingZeros().toPlainString() + " kg.",
                                    "Reduzca la dosis o documente la justificación.",
                                    p.getDci()));
                }
            } else if (p.getDosisMaxAdultoMgDia() != null && total.compareTo(p.getDosisMaxAdultoMgDia()) > 0) {
                agregar(
                        alertas,
                        claves,
                        new Alerta(
                                "DOSIS:" + p.getId(),
                                TipoAlertaReceta.DOSIS_MAXIMA,
                                SeveridadAlerta.GRAVE,
                                "La dosis diaria de " + p.getDci() + " es "
                                        + total.stripTrailingZeros().toPlainString()
                                        + " mg y supera el máximo de "
                                        + p.getDosisMaxAdultoMgDia()
                                                .stripTrailingZeros()
                                                .toPlainString() + " mg al día.",
                                "Reduzca la dosis o documente la justificación.",
                                p.getDci()));
            }
        }
    }

    private void embarazoYLactancia(
            Paciente paciente,
            Integer edad,
            boolean embarazada,
            boolean lactando,
            List<ItemPrescripcion> items,
            Map<UUID, Medicamento> catalogo,
            List<Alerta> alertas,
            Set<String> claves) {
        boolean edadFertil = paciente.getSexo() == SexoPaciente.F && edad != null && edad >= 12 && edad <= 50;
        Set<UUID> vistos = new HashSet<>();
        for (ItemPrescripcion item : items) {
            Medicamento m = catalogo.get(item.medicamentoId());
            PrincipioActivo p = m.getPrincipioActivo();
            if (!vistos.add(p.getId())) {
                continue;
            }
            CategoriaEmbarazo cat = p.getCategoriaEmbarazo();
            if (embarazada) {
                SeveridadAlerta s = switch (cat) {
                    case X -> SeveridadAlerta.CONTRAINDICADA;
                    case D -> SeveridadAlerta.GRAVE;
                    case C -> SeveridadAlerta.MODERADA;
                    default -> null;
                };
                if (s != null) {
                    agregar(
                            alertas,
                            claves,
                            new Alerta(
                                    "EMBARAZO:" + p.getId(),
                                    TipoAlertaReceta.EMBARAZO,
                                    s,
                                    p.getDci() + " tiene categoría " + cat.name() + " en el embarazo.",
                                    "Valore el riesgo y beneficio antes de prescribir.",
                                    m.nombreCompleto()));
                }
            } else if (edadFertil && (cat == CategoriaEmbarazo.X || cat == CategoriaEmbarazo.D)) {
                agregar(
                        alertas,
                        claves,
                        new Alerta(
                                "EMBARAZO:" + p.getId() + ":confirmar",
                                TipoAlertaReceta.EMBARAZO,
                                SeveridadAlerta.INFORMATIVA,
                                p.getDci() + " tiene categoría " + cat.name()
                                        + " en el embarazo y la paciente está en edad fértil.",
                                "Confirme que no está embarazada y márquelo en la receta.",
                                m.nombreCompleto()));
            }
            if (lactando) {
                CompatibilidadLactancia l = p.getLactancia();
                SeveridadAlerta s = switch (l) {
                    case CONTRAINDICADO -> SeveridadAlerta.GRAVE;
                    case PRECAUCION -> SeveridadAlerta.MODERADA;
                    default -> null;
                };
                if (s != null) {
                    agregar(
                            alertas,
                            claves,
                            new Alerta(
                                    "LACTANCIA:" + p.getId(),
                                    TipoAlertaReceta.LACTANCIA,
                                    s,
                                    p.getDci() + " "
                                            + (l == CompatibilidadLactancia.CONTRAINDICADO
                                                    ? "está contraindicado durante la lactancia."
                                                    : "requiere precaución durante la lactancia."),
                                    "Valore una alternativa compatible.",
                                    m.nombreCompleto()));
                }
            }
        }
    }

    private void avisoRenal(
            List<ItemPrescripcion> items, Map<UUID, Medicamento> catalogo, List<Alerta> alertas, Set<String> claves) {
        for (ItemPrescripcion item : items) {
            Medicamento m = catalogo.get(item.medicamentoId());
            PrincipioActivo p = m.getPrincipioActivo();
            if (p.getAvisoRenal() != null && !p.getAvisoRenal().isBlank()) {
                agregar(
                        alertas,
                        claves,
                        new Alerta(
                                "RENAL:" + p.getId(),
                                TipoAlertaReceta.RENAL,
                                SeveridadAlerta.INFORMATIVA,
                                p.getAvisoRenal(),
                                null,
                                m.nombreCompleto()));
            }
        }
    }

    private void agregar(List<Alerta> alertas, Set<String> claves, Alerta alerta) {
        if (claves.add(alerta.clave())) {
            alertas.add(alerta);
        }
    }

    static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }
}
