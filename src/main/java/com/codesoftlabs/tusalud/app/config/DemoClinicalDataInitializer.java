package com.codesoftlabs.tusalud.app.config;

import com.codesoftlabs.tusalud.app.entities.Atencion;
import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.Cupon;
import com.codesoftlabs.tusalud.app.entities.MetodoPago;
import com.codesoftlabs.tusalud.app.entities.Notificacion;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.Pago;
import com.codesoftlabs.tusalud.app.entities.RecetaItem;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.Valoracion;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoPago;
import com.codesoftlabs.tusalud.app.entities.enums.MarcaTarjeta;
import com.codesoftlabs.tusalud.app.entities.enums.TipoCupon;
import com.codesoftlabs.tusalud.app.entities.enums.TipoMetodoPago;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.repositories.AtencionRepository;
import com.codesoftlabs.tusalud.app.repositories.CitaRepository;
import com.codesoftlabs.tusalud.app.repositories.CuponRepository;
import com.codesoftlabs.tusalud.app.repositories.MetodoPagoRepository;
import com.codesoftlabs.tusalud.app.repositories.NotificacionRepository;
import com.codesoftlabs.tusalud.app.repositories.PacienteRepository;
import com.codesoftlabs.tusalud.app.repositories.PagoRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.repositories.ValoracionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(200)
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DemoClinicalDataInitializer implements CommandLineRunner {

    private record DatosMedicos(String email, String grupo, String alergias, String antecedentes) {}

    private record AtencionSeed(
            String diagnostico,
            String tratamiento,
            String indicaciones,
            String pesoKg,
            String tallaCm,
            String presion,
            String temperatura,
            Integer frecuencia,
            List<RecetaItem> receta) {}

    private record Valorar(String motivo, int puntuacion, String comentario) {}

    private static RecetaItem item(String medicamento, String dosis, String frecuencia, String duracion) {
        return RecetaItem.builder()
                .medicamento(medicamento)
                .dosis(dosis)
                .frecuencia(frecuencia)
                .duracion(duracion)
                .build();
    }

    private static final List<DatosMedicos> DATOS_MEDICOS = List.of(
            new DatosMedicos("juan.perez@correo.com", "O+", "Penicilina", "Hipertensión leve diagnosticada en 2022"),
            new DatosMedicos("maria.rodriguez@correo.com", "A+", "Ninguna conocida", "Asma estacional"),
            new DatosMedicos("sofia.ramirez@correo.com", "B+", "Polen y ácaros", "Sin antecedentes relevantes"),
            new DatosMedicos("carlos.mendoza@correo.com", "O-", "Ibuprofeno", "Hipertensión arterial en tratamiento"),
            new DatosMedicos("diego.herrera@correo.com", "A-", "Ninguna conocida", "Lesión de rodilla en 2024"),
            new DatosMedicos(
                    "jorge.medina@correo.com",
                    "AB+",
                    "Mariscos",
                    "Colesterol alto y antecedentes familiares cardíacos"));

    private static final Map<String, AtencionSeed> ATENCIONES = Map.of(
            "Chequeo general anual",
            new AtencionSeed(
                    "Paciente sana, sin hallazgos patológicos",
                    "No requiere tratamiento",
                    "Mantener actividad física regular y control anual",
                    "62.0",
                    "165.0",
                    "110/70",
                    "36.6",
                    72,
                    List.of(item("Vitamina D", "1000 UI", "1 vez al día", "60 días"))),
            "Seguimiento de hipertensión",
            new AtencionSeed(
                    "Hipertensión arterial controlada",
                    "Continuar con enalapril y dieta baja en sodio",
                    "Controlar la presión en casa dos veces por semana",
                    "84.5",
                    "174.0",
                    "130/85",
                    "36.5",
                    78,
                    List.of(item("Enalapril", "10 mg", "1 vez al día", "30 días"))),
            "Control de acné",
            new AtencionSeed(
                    "Acné vulgar moderado",
                    "Tratamiento tópico y limpieza facial suave",
                    "Evitar manipular las lesiones y usar protector solar",
                    "58.0",
                    "160.0",
                    "100/65",
                    "36.4",
                    70,
                    List.of(
                            item("Adapaleno gel 0.1 %", "Capa fina", "Por la noche", "8 semanas"),
                            item("Gel limpiador facial", "Aplicar", "2 veces al día", "Continuo"))),
            "Dolor de rodilla tras una lesión deportiva",
            new AtencionSeed(
                    "Esguince de rodilla grado I",
                    "Reposo relativo, hielo local y fisioterapia",
                    "Evitar deportes de impacto por 3 semanas",
                    "76.0",
                    "178.0",
                    "120/78",
                    "36.6",
                    68,
                    List.of(
                            item("Naproxeno", "550 mg", "Cada 12 horas", "5 días"),
                            item("Diclofenaco gel", "Aplicar", "3 veces al día", "7 días"))),
            "Control ginecológico anual",
            new AtencionSeed(
                    "Control ginecológico normal",
                    "No requiere tratamiento",
                    "Repetir el Papanicolaou en un año",
                    "55.0",
                    "158.0",
                    "105/68",
                    "36.5",
                    74,
                    List.of()),
            "Revisión de la vista por fatiga visual",
            new AtencionSeed(
                    "Fatiga visual por uso prolongado de pantallas",
                    "Lágrimas artificiales y pausas visuales",
                    "Regla 20-20-20 y revisión de la graduación en 6 meses",
                    "80.0",
                    "172.0",
                    "125/80",
                    "36.6",
                    75,
                    List.of(item("Lágrimas artificiales", "1 gota por ojo", "4 veces al día", "15 días"))),
            "Revisión de manchas en la piel",
            new AtencionSeed(
                    "Léntigos solares benignos",
                    "Fotoprotección diaria",
                    "Usar protector solar factor 50 y revisar los lunares cada 6 meses",
                    "62.0",
                    "165.0",
                    "110/70",
                    "36.5",
                    71,
                    List.of(item("Protector solar FPS 50", "Aplicar", "Cada 3 horas al sol", "Continuo"))));

    private static final List<Valorar> VALORACIONES = List.of(
            new Valorar("Chequeo general anual", 5, "Muy atento y explicó todo con claridad."),
            new Valorar("Seguimiento de hipertensión", 4, "Buena atención, aunque esperé un poco."),
            new Valorar("Control de acné", 5, "La doctora fue amable y el tratamiento está funcionando."),
            new Valorar(
                    "Dolor de rodilla tras una lesión deportiva", 5, "Excelente diagnóstico y plan de recuperación."),
            new Valorar("Control ginecológico anual", 4, null));

    private final CuponRepository cuponRepository;
    private final PacienteRepository pacienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final MetodoPagoRepository metodoPagoRepository;
    private final CitaRepository citaRepository;
    private final AtencionRepository atencionRepository;
    private final PagoRepository pagoRepository;
    private final ValoracionRepository valoracionRepository;
    private final NotificacionRepository notificacionRepository;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Cargando datos clínicos y comerciales de demostración...");
        int cupones = sembrarCupones();
        int fichas = sembrarDatosMedicos();
        int tarjetas = sembrarMetodosPago();
        int atenciones = sembrarAtenciones();
        int pagos = sembrarPagos();
        int valoraciones = sembrarValoraciones();
        int avisos = sembrarNotificaciones();
        log.info(
                "Demostración clínica: {} cupones, {} fichas, {} tarjetas, {} atenciones, {} pagos, {} valoraciones y {} notificaciones nuevos",
                cupones,
                fichas,
                tarjetas,
                atenciones,
                pagos,
                valoraciones,
                avisos);
    }

    private int sembrarCupones() {
        LocalDate hoy = LocalDate.now();
        List<Cupon> cupones = List.of(
                Cupon.builder()
                        .codigo("BIENVENIDA10")
                        .descripcion("10 % de descuento en tu primera consulta")
                        .tipo(TipoCupon.PORCENTAJE)
                        .valor(new BigDecimal("10"))
                        .fechaInicio(hoy.minusDays(30))
                        .fechaFin(hoy.plusYears(1))
                        .build(),
                Cupon.builder()
                        .codigo("SALUD20")
                        .descripcion("Campaña de salud preventiva")
                        .tipo(TipoCupon.PORCENTAJE)
                        .valor(new BigDecimal("20"))
                        .fechaInicio(hoy.minusDays(7))
                        .fechaFin(hoy.plusMonths(3))
                        .usosMaximos(100)
                        .build(),
                Cupon.builder()
                        .codigo("VERANO50")
                        .descripcion("Descuento fijo de verano, ya finalizado")
                        .tipo(TipoCupon.MONTO_FIJO)
                        .valor(new BigDecimal("50"))
                        .fechaInicio(hoy.minusDays(120))
                        .fechaFin(hoy.minusDays(30))
                        .build());
        int nuevos = 0;
        for (Cupon cupon : cupones) {
            if (!cuponRepository.existsByCodigo(cupon.getCodigo())) {
                cuponRepository.save(cupon);
                nuevos++;
            }
        }
        return nuevos;
    }

    private int sembrarDatosMedicos() {
        int actualizados = 0;
        for (DatosMedicos datos : DATOS_MEDICOS) {
            Paciente paciente =
                    pacienteRepository.findByUsuarioEmail(datos.email()).orElse(null);
            if (paciente != null && paciente.getGrupoSanguineo() == null) {
                paciente.setGrupoSanguineo(datos.grupo());
                paciente.setAlergias(datos.alergias());
                paciente.setAntecedentes(datos.antecedentes());
                actualizados++;
            }
        }
        return actualizados;
    }

    private int sembrarMetodosPago() {
        record Tarjeta(String email, String titular, MarcaTarjeta marca, String ultimos4) {}
        int anio = LocalDate.now().getYear() + 3;
        int nuevos = 0;
        for (Tarjeta tarjeta : List.of(
                new Tarjeta("juan.perez@correo.com", "JUAN PEREZ", MarcaTarjeta.VISA, "4242"),
                new Tarjeta("maria.rodriguez@correo.com", "MARIA RODRIGUEZ", MarcaTarjeta.MASTERCARD, "4444"),
                new Tarjeta("sofia.ramirez@correo.com", "SOFIA RAMIREZ", MarcaTarjeta.VISA, "1881"))) {
            Usuario usuario =
                    usuarioRepository.findByEmailWithRoles(tarjeta.email()).orElse(null);
            if (usuario == null
                    || !metodoPagoRepository
                            .findByUsuarioIdOrderByPredeterminadoDescIdDesc(usuario.getId())
                            .isEmpty()) {
                continue;
            }
            metodoPagoRepository.save(MetodoPago.builder()
                    .usuario(usuario)
                    .tipo(TipoMetodoPago.TARJETA_CREDITO)
                    .titular(tarjeta.titular())
                    .marca(tarjeta.marca())
                    .ultimos4(tarjeta.ultimos4())
                    .vencimientoMes(12)
                    .vencimientoAnio(anio)
                    .predeterminado(true)
                    .build());
            nuevos++;
        }
        return nuevos;
    }

    private int sembrarAtenciones() {
        int nuevas = 0;
        for (Cita cita : citaRepository.findAll()) {
            AtencionSeed seed = ATENCIONES.get(cita.getMotivo());
            if (cita.getEstado() != EstadoCita.ATENDIDA
                    || seed == null
                    || atencionRepository.existsByCitaId(cita.getId())) {
                continue;
            }
            atencionRepository.save(Atencion.builder()
                    .cita(cita)
                    .paciente(cita.getPaciente())
                    .medico(cita.getMedico())
                    .fecha(cita.getFechaHora().plusMinutes(25))
                    .diagnostico(seed.diagnostico())
                    .tratamiento(seed.tratamiento())
                    .indicaciones(seed.indicaciones())
                    .pesoKg(new BigDecimal(seed.pesoKg()))
                    .tallaCm(new BigDecimal(seed.tallaCm()))
                    .presionArterial(seed.presion())
                    .temperaturaC(new BigDecimal(seed.temperatura()))
                    .frecuenciaCardiaca(seed.frecuencia())
                    .receta(new ArrayList<>(seed.receta()))
                    .build());
            nuevas++;
        }
        return nuevas;
    }

    private int sembrarPagos() {
        Cupon bienvenida = cuponRepository.findByCodigo("BIENVENIDA10").orElse(null);
        long correlativo = pagoRepository.count();
        int nuevos = 0;
        int conCupon = 0;
        for (Cita cita : citaRepository.findAll()) {
            boolean pagable = cita.getEstado() == EstadoCita.ATENDIDA || cita.getEstado() == EstadoCita.CONFIRMADA;
            if (!pagable || cita.getPago() != null) {
                continue;
            }
            BigDecimal monto = cita.getMonto();
            boolean usaCupon = bienvenida != null && conCupon < 2 && cita.getEstado() == EstadoCita.ATENDIDA;
            BigDecimal descuento = usaCupon
                    ? monto.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2);
            MetodoPago metodo =
                    metodoPagoRepository
                            .findByUsuarioIdOrderByPredeterminadoDescIdDesc(
                                    cita.getPaciente().getUsuario().getId())
                            .stream()
                            .findFirst()
                            .orElse(null);
            String descripcion = metodo == null
                    ? "Efectivo en caja"
                    : metodo.getMarca().name() + " terminada en " + metodo.getUltimos4();
            correlativo++;
            Pago pago = Pago.builder()
                    .cita(cita)
                    .metodoPago(metodo)
                    .metodoDescripcion(descripcion)
                    .monto(monto)
                    .descuento(descuento)
                    .total(monto.subtract(descuento))
                    .codigoCupon(usaCupon ? bienvenida.getCodigo() : null)
                    .estado(EstadoPago.PAGADO)
                    .comprobante(String.format("B001-%06d", correlativo))
                    .fechaPago(
                            cita.getEstado() == EstadoCita.ATENDIDA
                                    ? cita.getFechaHora().minusDays(1)
                                    : LocalDateTime.now().minusHours(2))
                    .build();
            pagoRepository.save(pago);
            cita.setPago(pago);
            if (usaCupon) {
                bienvenida.setUsosActuales(bienvenida.getUsosActuales() + 1);
                conCupon++;
            }
            nuevos++;
        }
        return nuevos;
    }

    private int sembrarValoraciones() {
        int nuevas = 0;
        for (Valorar valorar : VALORACIONES) {
            for (Cita cita : citaRepository.findAll()) {
                if (cita.getEstado() == EstadoCita.ATENDIDA
                        && cita.getMotivo().equals(valorar.motivo())
                        && !valoracionRepository.existsByCitaId(cita.getId())) {
                    valoracionRepository.save(Valoracion.builder()
                            .cita(cita)
                            .medico(cita.getMedico())
                            .paciente(cita.getPaciente())
                            .puntuacion(valorar.puntuacion())
                            .comentario(valorar.comentario())
                            .build());
                    nuevas++;
                }
            }
        }
        return nuevas;
    }

    private int sembrarNotificaciones() {
        record Aviso(
                String email, TipoNotificacion tipo, String titulo, String mensaje, String enlace, boolean leida) {}
        List<Aviso> avisos = List.of(
                new Aviso(
                        "juan.perez@correo.com",
                        TipoNotificacion.RECORDATORIO_CITA,
                        "Recordatorio de cita",
                        "Tienes una cita de cardiología pronto. Llega 10 minutos antes.",
                        "/citas",
                        false),
                new Aviso(
                        "juan.perez@correo.com",
                        TipoNotificacion.CITA_CONFIRMADA,
                        "Cita confirmada",
                        "Tu cita fue confirmada por el médico.",
                        "/citas",
                        false),
                new Aviso(
                        "juan.perez@correo.com",
                        TipoNotificacion.INICIO_SESION,
                        "Nuevo inicio de sesión",
                        "Se inició sesión desde Chrome en Windows con la IP 192.168.1.20.",
                        "/perfil",
                        true),
                new Aviso(
                        "dr.garcia@tusalud.com",
                        TipoNotificacion.CITA_CREADA,
                        "Nueva cita registrada",
                        "Un paciente reservó una cita contigo.",
                        "/agenda",
                        false),
                new Aviso(
                        "dr.garcia@tusalud.com",
                        TipoNotificacion.SISTEMA,
                        "Bienvenido a TuSalud",
                        "Revisa tu agenda y registra la atención de tus pacientes.",
                        "/agenda",
                        true),
                new Aviso(
                        "admin@tusalud.com",
                        TipoNotificacion.SISTEMA,
                        "Panel de control disponible",
                        "Consulta los indicadores de la clínica desde el panel de administración.",
                        "/admin/panel",
                        false),
                new Aviso(
                        "admin@tusalud.com",
                        TipoNotificacion.PAGO_REGISTRADO,
                        "Pagos del día",
                        "Se registraron nuevos pagos de consultas.",
                        "/admin/pagos",
                        true));
        int nuevas = 0;
        Map<String, Boolean> sinAvisos = new HashMap<>();
        for (Aviso aviso : avisos) {
            Usuario usuario =
                    usuarioRepository.findByEmailWithRoles(aviso.email()).orElse(null);
            if (usuario == null) {
                continue;
            }
            boolean vacio = sinAvisos.computeIfAbsent(
                    aviso.email(),
                    email -> notificacionRepository
                            .findByUsuarioIdOrderByCreatedAtDescIdDesc(usuario.getId(), PageRequest.of(0, 1))
                            .isEmpty());
            if (!vacio) {
                continue;
            }
            notificacionRepository.save(Notificacion.builder()
                    .usuario(usuario)
                    .tipo(aviso.tipo())
                    .titulo(aviso.titulo())
                    .mensaje(aviso.mensaje())
                    .enlace(aviso.enlace())
                    .leida(aviso.leida())
                    .build());
            nuevas++;
        }
        return nuevas;
    }
}
