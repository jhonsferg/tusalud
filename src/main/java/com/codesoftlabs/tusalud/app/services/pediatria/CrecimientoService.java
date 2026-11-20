package com.codesoftlabs.tusalud.app.services.pediatria;

import com.codesoftlabs.tusalud.app.dto.request.PediatriaRequests.MedicionRequest;
import com.codesoftlabs.tusalud.app.dto.response.PediatriaResponses.AlertaCrecimiento;
import com.codesoftlabs.tusalud.app.dto.response.PediatriaResponses.CrecimientoResponse;
import com.codesoftlabs.tusalud.app.dto.response.PediatriaResponses.CurvaCrecimiento;
import com.codesoftlabs.tusalud.app.dto.response.PediatriaResponses.MedicionResponse;
import com.codesoftlabs.tusalud.app.dto.response.PediatriaResponses.PuntoCrecimiento;
import com.codesoftlabs.tusalud.app.dto.response.PediatriaResponses.ReferenciaPunto;
import com.codesoftlabs.tusalud.app.entities.Atencion;
import com.codesoftlabs.tusalud.app.entities.MedicionCrecimiento;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.ReferenciaCrecimiento;
import com.codesoftlabs.tusalud.app.entities.enums.PediatriaEnums.IndicadorCrecimiento;
import com.codesoftlabs.tusalud.app.entities.enums.PediatriaEnums.NivelAlerta;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.AtencionRepository;
import com.codesoftlabs.tusalud.app.repositories.MedicionCrecimientoRepository;
import com.codesoftlabs.tusalud.app.repositories.ReferenciaCrecimientoRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.AuditoriaClinicaService;
import com.codesoftlabs.tusalud.app.services.pediatria.CalculadoraCrecimiento.Evaluacion;
import com.codesoftlabs.tusalud.app.services.pediatria.CalculadoraCrecimiento.PuntoLms;
import com.codesoftlabs.tusalud.app.services.pediatria.CalculadoraCrecimiento.Resultado;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CrecimientoService {

    public static final String ACCION_CONSULTADO = "CRECIMIENTO_CONSULTADO";
    public static final String ACCION_REGISTRADO = "MEDICION_REGISTRADA";
    public static final String ACCION_ANULADA = "MEDICION_ANULADA";

    private final MedicionCrecimientoRepository mediciones;
    private final ReferenciaCrecimientoRepository referencias;
    private final AtencionRepository atenciones;
    private final UsuarioRepository usuarios;
    private final AccesoPaciente acceso;
    private final AuditoriaClinicaService auditoria;

    @Transactional
    public MedicionResponse registrar(UsuarioPrincipal actor, MedicionRequest request) {
        Paciente paciente = acceso.exigirEscritura(actor, request.pacienteId());
        if (request.pesoKg() == null && request.tallaCm() == null && request.perimetroCefalicoCm() == null) {
            throw new DatosInvalidosException("Indique al menos el peso, la talla o el perímetro cefálico");
        }
        if (paciente.getFechaNacimiento() == null || paciente.getSexo() == null) {
            throw new DatosInvalidosException("El paciente necesita fecha de nacimiento y sexo para las curvas");
        }
        if (request.fecha().isBefore(paciente.getFechaNacimiento())) {
            throw new DatosInvalidosException("La fecha de la medición es anterior al nacimiento");
        }
        long meses = ChronoUnit.MONTHS.between(paciente.getFechaNacimiento(), request.fecha());
        if (meses >= CalculadoraCrecimiento.EDAD_MAXIMA_MESES) {
            throw new DatosInvalidosException("Las curvas de crecimiento aplican a menores de 19 años");
        }
        if (request.perimetroCefalicoCm() != null && meses > 60) {
            throw new DatosInvalidosException("El perímetro cefálico se registra hasta los 5 años");
        }
        Atencion atencion = null;
        if (request.atencionId() != null) {
            atencion = atenciones
                    .findById(request.atencionId())
                    .filter(a -> a.getPaciente().getId().equals(paciente.getId()))
                    .orElseThrow(() -> new DatosInvalidosException("La atención indicada no corresponde al paciente"));
        }
        MedicionCrecimiento guardada = mediciones.save(MedicionCrecimiento.builder()
                .paciente(paciente)
                .fecha(request.fecha())
                .pesoKg(request.pesoKg())
                .tallaCm(request.tallaCm())
                .perimetroCefalicoCm(request.perimetroCefalicoCm())
                .atencion(atencion)
                .registradoPor(usuarios.getReferenceById(actor.getId()))
                .observaciones(request.observaciones())
                .build());
        auditoria.registrar(actor, paciente.getId(), ACCION_REGISTRADO, "Medición del " + request.fecha());
        return medicion(guardada, actor.getNombreCompleto());
    }

    @Transactional
    public MedicionResponse anular(UsuarioPrincipal actor, UUID id, String motivo) {
        MedicionCrecimiento medicion =
                mediciones.findById(id).orElseThrow(() -> new ResourceNotFoundException("Medición", "id", id));
        acceso.exigirEscritura(actor, medicion.getPaciente().getId());
        if (medicion.isAnulada()) {
            throw new ConflictException("La medición ya está anulada");
        }
        medicion.setAnulada(true);
        medicion.setMotivoAnulacion(motivo.strip());
        auditoria.registrar(
                actor, medicion.getPaciente().getId(), ACCION_ANULADA, "Medición del " + medicion.getFecha());
        return medicion(medicion, medicion.getRegistradoPor().getNombreCompleto());
    }

    @Transactional
    public CrecimientoResponse curvas(UsuarioPrincipal actor, UUID pacienteId) {
        Paciente paciente = acceso.exigirLectura(actor, pacienteId);
        if (!acceso.esPaciente(actor)) {
            auditoria.registrar(actor, pacienteId, ACCION_CONSULTADO, "Curvas de crecimiento");
        }
        return construir(paciente);
    }

    public CrecimientoResponse curvasMias(UsuarioPrincipal actor) {
        return construir(acceso.propio(actor));
    }

    private CrecimientoResponse construir(Paciente paciente) {
        LocalDate hoy = LocalDate.now();
        LocalDate nacimiento = paciente.getFechaNacimiento();
        Integer edadMeses = nacimiento == null ? null : (int) ChronoUnit.MONTHS.between(nacimiento, hoy);
        boolean aplica = nacimiento != null
                && paciente.getSexo() != null
                && edadMeses < CalculadoraCrecimiento.EDAD_MAXIMA_MESES;
        String nombre = paciente.getUsuario().getNombreCompleto();
        String sexo = paciente.getSexo() == null ? null : paciente.getSexo().name();
        if (nacimiento == null || paciente.getSexo() == null) {
            return new CrecimientoResponse(
                    paciente.getId(), nombre, sexo, edadMeses, false, false, List.of(), List.of(), List.of());
        }
        List<MedicionCrecimiento> vigentes = mediciones.vigentesDelPaciente(paciente.getId());
        List<CurvaCrecimiento> curvas = new ArrayList<>();
        List<AlertaCrecimiento> alertas = new ArrayList<>();
        boolean porValidar = false;
        for (IndicadorCrecimiento indicador : IndicadorCrecimiento.values()) {
            List<ReferenciaCrecimiento> filas =
                    referencias.findByIndicadorAndSexoOrderByEdadMeses(indicador, paciente.getSexo());
            porValidar = porValidar || filas.stream().anyMatch(ReferenciaCrecimiento::isPorValidar);
            List<PuntoLms> tabla = filas.stream()
                    .map(f -> new PuntoLms(
                            f.getEdadMeses(),
                            f.getL().doubleValue(),
                            f.getM().doubleValue(),
                            f.getS().doubleValue()))
                    .toList();
            List<PuntoCrecimiento> puntos = puntos(indicador, vigentes, nacimiento, tabla);
            curvas.add(new CurvaCrecimiento(
                    indicador, indicador.etiqueta(), indicador.unidad(), puntos, referencia(tabla)));
            if (!puntos.isEmpty()) {
                PuntoCrecimiento ultimo = puntos.get(puntos.size() - 1);
                if (ultimo.nivel() != NivelAlerta.NORMAL && ultimo.z() != null) {
                    alertas.add(new AlertaCrecimiento(
                            indicador,
                            indicador.etiqueta(),
                            ultimo.nivel(),
                            ultimo.mensaje(),
                            ultimo.z(),
                            ultimo.fecha()));
                }
            }
        }
        List<MedicionResponse> lista = vigentes.stream()
                .map(m -> medicion(m, m.getRegistradoPor().getNombreCompleto()))
                .toList();
        return new CrecimientoResponse(
                paciente.getId(), nombre, sexo, edadMeses, aplica, porValidar, curvas, alertas, lista);
    }

    private List<PuntoCrecimiento> puntos(
            IndicadorCrecimiento indicador,
            List<MedicionCrecimiento> vigentes,
            LocalDate nacimiento,
            List<PuntoLms> tabla) {
        Function<MedicionCrecimiento, Double> valor = switch (indicador) {
            case PESO_EDAD -> m -> m.getPesoKg() == null ? null : m.getPesoKg().doubleValue();
            case TALLA_EDAD ->
                m -> m.getTallaCm() == null ? null : m.getTallaCm().doubleValue();
            case PC_EDAD ->
                m -> m.getPerimetroCefalicoCm() == null
                        ? null
                        : m.getPerimetroCefalicoCm().doubleValue();
            case IMC_EDAD ->
                m -> {
                    BigDecimal imc = imc(m);
                    return imc == null ? null : imc.doubleValue();
                };
        };
        List<PuntoCrecimiento> puntos = new ArrayList<>();
        Double zAnterior = null;
        for (MedicionCrecimiento m : vigentes) {
            Double v = valor.apply(m);
            if (v == null) {
                continue;
            }
            double edad = CalculadoraCrecimiento.edadEnMeses(ChronoUnit.DAYS.between(nacimiento, m.getFecha()));
            PuntoLms lms = CalculadoraCrecimiento.interpolar(tabla, edad);
            if (lms == null) {
                puntos.add(new PuntoCrecimiento(
                        m.getId(),
                        m.getFecha(),
                        edad,
                        v,
                        null,
                        null,
                        NivelAlerta.NORMAL,
                        "Sin referencia para esta edad"));
                continue;
            }
            Resultado r = CalculadoraCrecimiento.evaluar(lms, v);
            Evaluacion evaluacion = CalculadoraCrecimiento.clasificar(indicador, r.z());
            NivelAlerta nivel = evaluacion.nivel();
            String mensaje = evaluacion.mensaje();
            if (zAnterior != null) {
                Evaluacion cambio = CalculadoraCrecimiento.cambioBrusco(zAnterior, r.z());
                if (cambio != null && nivel == NivelAlerta.NORMAL) {
                    nivel = cambio.nivel();
                    mensaje = cambio.mensaje();
                }
            }
            zAnterior = r.z();
            puntos.add(new PuntoCrecimiento(
                    m.getId(), m.getFecha(), edad, v, redondear(r.z()), redondear(r.percentil()), nivel, mensaje));
        }
        return puntos;
    }

    private static List<ReferenciaPunto> referencia(List<PuntoLms> tabla) {
        List<ReferenciaPunto> resultado = new ArrayList<>();
        for (PuntoLms p : tabla) {
            double[] v = new double[CalculadoraCrecimiento.Z_PERCENTILES.length];
            boolean valido = true;
            for (int i = 0; i < v.length; i++) {
                double z = CalculadoraCrecimiento.Z_PERCENTILES[i];
                if (1 + p.l() * p.s() * z <= 0) {
                    valido = false;
                    break;
                }
                v[i] = redondear(CalculadoraCrecimiento.valorEnZ(p, z));
            }
            if (valido) {
                resultado.add(new ReferenciaPunto(p.edadMeses(), v[0], v[1], v[2], v[3], v[4]));
            }
        }
        return resultado;
    }

    private static BigDecimal imc(MedicionCrecimiento m) {
        if (m.getPesoKg() == null || m.getTallaCm() == null || m.getTallaCm().signum() <= 0) {
            return null;
        }
        BigDecimal metros = m.getTallaCm().divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
        return m.getPesoKg().divide(metros.multiply(metros), 1, RoundingMode.HALF_UP);
    }

    private static double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    private static MedicionResponse medicion(MedicionCrecimiento m, String registradoPor) {
        return new MedicionResponse(
                m.getId(),
                m.getFecha(),
                m.getPesoKg(),
                m.getTallaCm(),
                m.getPerimetroCefalicoCm(),
                imc(m),
                registradoPor,
                m.getObservaciones());
    }
}
