const fs = require("fs");
const path = require("path");
const L = require("./lib");

const RAIZ = path.resolve(L.DIR, "..", "..", "..");
const DESTINO = path.join(RAIZ, "credenciales-qa.txt");
const ANTERIOR = path.join(RAIZ, "credenciales-qa.anterior.txt");
const PROPIETARIO = process.env.PROPIETARIO_EMAIL || "";

const APP = {
  ROLE_ADMIN: "/admin/",
  ROLE_MEDICO: "/medico/",
  ROLE_PACIENTE: "/paciente/",
  ROLE_RECEPCION: "/recepcion/",
  ROLE_CAJA: "/caja/",
  ROLE_ENFERMERIA: "/enfermeria/",
  ROLE_LABORATORIO: "/laboratorio/",
  ROLE_IMAGENES: "/imagenes/",
  ROLE_FARMACIA: "/farmacia/",
  ROLE_ALMACEN: "/almacen/",
  ROLE_GERENCIA: "/gerencia/",
  ROLE_FINANZAS: "/finanzas/",
  ROLE_OPERACIONES: "/operaciones/",
  ROLE_CALIDAD: "/calidad/",
  ROLE_RRHH: "/rrhh/",
};
const ROLES = Object.keys(APP);
const SEGREGACION = {
  "finanzas2@tusalud.com":
    "paga la planilla que aprobó finanzas, aprueba o registra en lugar de finanzas",
  "finanzas3@tusalud.com":
    "registra comprobantes de proveedor y pagos, distinto de quien aprueba la orden",
  "rrhh2@tusalud.com":
    "segundo usuario de recursos humanos para calcular planillas",
  "enfermeria2@tusalud.com":
    "programa cirugías y registra ciclos de esterilización",
  "enfermeria3@tusalud.com": "instrumentista del equipo quirúrgico",
  "almacen2@tusalud.com":
    "recibe órdenes de compra, distinto del que las genera",
  "farmacia2@tusalud.com": "segundo solicitante de requisiciones y dispensador",
  "caja2@tusalud.com": "segunda caja, para cobros y cierres independientes",
  "calidad2@tusalud.com":
    "segundo analista de eventos adversos y solicitudes ARCO",
  "recepcion2@tusalud.com": "segunda recepción, para pruebas de visibilidad",
  "lab2@tusalud.com": "segundo técnico, ingresa resultados que valida otro",
  "imagenes2@tusalud.com": "segundo tecnólogo y radiólogo, toma e informa",
  "gerencia2@tusalud.com": "cierra periodos y planillas en lugar de gerencia",
  "operaciones2@tusalud.com": "segundo usuario de operaciones",
  "dr.cirujano@tusalud.com": "cirujano del equipo quirúrgico, Cirugía General",
  "dr.cirujano2@tusalud.com": "segundo cirujano o ayudante, Cirugía General",
  "dra.anestesia@tusalud.com":
    "anestesióloga del equipo quirúrgico, Anestesiología",
};

const PW = {
  admin: L.claveDe("admin@tusalud.com"),
  medico: L.claveDe("dr.garcia@tusalud.com"),
  paciente: L.claveDe("juan.perez@correo.com"),
  personal: L.claveDe("caja@tusalud.com"),
};
const pad = (s, n) => String(s).padEnd(n, " ");
const out = [];
const w = (s) => out.push(s === undefined ? "" : s);
const titulo = (t) => {
  w();
  w(t);
  w("=".repeat(t.length));
};
const sub = (t) => {
  w();
  w(t);
  w("-".repeat(t.length));
};

async function main() {
  if (fs.existsSync(DESTINO)) fs.copyFileSync(DESTINO, ANTERIOR);

  const usuarios = L.lista(
    (await L.api("admin@tusalud.com", "GET", "/admin/usuarios")).data,
  );
  const mapa = new Map();
  for (const u of usuarios) mapa.set(u.email.toLowerCase(), u);
  const todos = [...mapa.values()].filter(
    (u) =>
      u.email.toLowerCase() !== PROPIETARIO &&
      (u.estado || "ACTIVO") === "ACTIVO",
  );
  const esVolumen = (u) => /^qa\.paciente\d+@correo\.com$/.test(u.email);
  const porRol = (r) => todos.filter((u) => (u.roles || []).includes(r));
  const dir = L.E.get("dirMedicos") || {};
  const E = (k) => L.E.get(k);
  const enc = (k) => E("enc:" + k) || {};

  w("CREDENCIALES DE PRUEBA, ENTORNO DE QA DE TUSALUD");
  w("=================================================");
  w();
  w(
    "AVISO: este archivo contiene contraseñas de acceso. Es solo para el equipo de QA, no lo compartas fuera del equipo, no lo subas a ningún repositorio ni lo pegues en chats o correos abiertos.",
  );
  w(
    "Todos los datos clínicos son ficticios. Actualizado el " +
      L.fecha(0) +
      ".",
  );

  titulo("1. DIRECCIONES");
  w("Aplicación web: " + (process.env.TUSALUD_WEB || ""));
  w(
    "API (prefijo /api/v1): " +
      (process.env.TUSALUD_API || "").replace("/api/v1", ""),
  );
  w(
    "Se entra por la raíz del sitio y cada usuario es llevado a la aplicación de su rol. La API se usa con un token Bearer que se obtiene en POST /api/v1/auth/login y dura 15 minutos.",
  );
  w(
    "Acceso: el entorno solo responde a conexiones originadas en Perú. Si se prueba desde otro país, o con una VPN que salga fuera del Perú, la conexión se rechaza o se corta antes de llegar al sitio. Quien necesite probar desde fuera debe pedirlo al equipo de desarrollo.",
  );
  w(
    "La ruta de inicio de sesión admite unas 30 peticiones por minuto por dirección IP y responde 429 si se excede. Inicia sesión una vez por usuario y reutiliza el token.",
  );

  titulo("2. RESUMEN POR ROL");
  w(pad("Rol", 20) + pad("Usuarios", 10) + "Aplicación");
  for (const r of ROLES) w(pad(r, 20) + pad(porRol(r).length, 10) + APP[r]);
  w(
    "Total: " +
      todos.length +
      " usuarios, de ellos " +
      todos.filter(esVolumen).length +
      " pacientes del lote de volumen.",
  );
  w(
    "Contraseñas comunes: " +
      PW.admin +
      " administrador de demostración, " +
      PW.medico +
      " médicos, " +
      PW.paciente +
      " pacientes y " +
      PW.personal +
      " el resto del personal. El administrador exclusivo de QA tiene una contraseña propia, más abajo.",
  );

  titulo("3. PERSONAL Y MÉDICOS");
  w(
    "Columnas: correo, nombre, rol, contraseña, aplicación. La marca [S] indica un usuario creado para probar la segregación de funciones.",
  );
  w();
  w("ADMINISTRADORES");
  for (const u of porRol("ROLE_ADMIN")) {
    const pw = L.claveDe(u.email);
    w(
      "  " +
        pad(u.email, 34) +
        pad(u.nombreCompleto, 28) +
        pad("ROLE_ADMIN", 18) +
        pad(pw, 24) +
        "/admin/" +
        (u.email === "admin.qa@tusalud.com"
          ? "   <- administrador exclusivo de QA, úsalo para todas las pruebas de administración"
          : ""),
    );
  }
  w();
  w("PERSONAL");
  for (const r of ROLES.filter(
    (x) => !["ROLE_ADMIN", "ROLE_MEDICO", "ROLE_PACIENTE"].includes(x),
  )) {
    for (const u of porRol(r).sort((a, b) => a.email.localeCompare(b.email))) {
      const s = SEGREGACION[u.email] ? " [S]" : "";
      w(
        "  " +
          pad(u.email, 34) +
          pad(u.nombreCompleto, 28) +
          pad(r, 18) +
          pad(L.claveDe(u.email), 16) +
          pad(APP[r], 16) +
          s,
      );
    }
  }
  w();
  w("MÉDICOS (contraseña " + PW.medico + ")");
  w(
    "  " +
      pad("Correo", 34) +
      pad("Nombre", 28) +
      pad("Colegiatura", 13) +
      "Especialidades",
  );
  for (const u of porRol("ROLE_MEDICO").sort((a, b) =>
    a.email.localeCompare(b.email),
  )) {
    const d = dir[u.email];
    const esp = d ? d.especialidades.map((e) => e.nombre).join(", ") : "";
    const s = SEGREGACION[u.email] ? " [S]" : "";
    const col =
      {
        "dr.garcia@tusalud.com": "CMP-12345",
        "dra.lopez@tusalud.com": "CMP-67890",
        "dr.salazar@tusalud.com": "CMP-23456",
        "dra.vega@tusalud.com": "CMP-34567",
        "dr.quispe@tusalud.com": "CMP-45678",
        "dra.fernandez@tusalud.com": "CMP-56789",
        "dr.delgado@tusalud.com": "CMP-67801",
        "dra.montoya@tusalud.com": "COP-78901",
        "dr.cirujano@tusalud.com": "CMP-91001",
        "dr.cirujano2@tusalud.com": "CMP-91002",
        "dra.anestesia@tusalud.com": "CMP-91003",
      }[u.email] ||
      (/^qa\.medico0(\d)@/.test(u.email)
        ? "CMP-9000" + u.email.match(/0(\d)@/)[1]
        : "");
    w(
      "  " +
        pad(u.email, 34) +
        pad(u.nombreCompleto, 28) +
        pad(col, 13) +
        (esp || (u.email === "dra.montoya@tusalud.com" ? "Odontología" : "")) +
        s,
    );
  }
  w(
    "  Los médicos qa.medico01 a qa.medico08 son los del lote de volumen: concentran la mayor parte de las citas y atenciones históricas.",
  );

  sub("Usuarios para probar la segregación de funciones");
  for (const [em, nota] of Object.entries(SEGREGACION))
    if (mapa.has(em)) w("  " + pad(em, 32) + nota);
  w("  Combinaciones previstas:");
  w(
    "  - Planilla: rrhh@ o rrhh2@ calcula, finanzas@ aprueba, finanzas2@ paga (quien aprobó no puede pagar), gerencia@ o gerencia2@ cierra.",
  );
  w(
    "  - Compras: farmacia@ o farmacia2@ solicita, almacen@ cotiza y genera la orden, finanzas@ aprueba hasta 5000 soles, gerencia@ aprueba las mayores, almacen2@ recibe, finanzas3@ registra el comprobante y paga.",
  );
  w(
    "  - Contabilidad: finanzas@ y finanzas2@ registran asientos, gerencia@ cierra y reabre los periodos (quien registró asientos de un periodo no lo cierra).",
  );
  w(
    "  - Quirófano: dr.cirujano@ solicita y opera, enfermeria2@ programa, dra.anestesia@ registra la anestesia, enfermeria3@ es instrumentista y completa las listas de verificación.",
  );
  w(
    "  - Esterilización: enfermeria2@ registra ciclos e indicadores, calidad@ consulta las alertas.",
  );

  sub("Contraseña del administrador exclusivo de QA");
  w("  admin.qa@tusalud.com   " + L.claveDe("admin.qa@tusalud.com"));

  titulo("4. PACIENTES");
  w(
    "Contraseña de todos los pacientes: " +
      PW.paciente +
      "   Aplicación: /paciente/",
  );
  sub("Pacientes de demostración");
  const filas = fs
    .readFileSync(path.join(L.DIR, "pacientes-demo.txt"), "utf8")
    .trim()
    .split("\n");
  w(
    "  " +
      pad("Correo", 32) +
      pad("Nombre", 24) +
      pad("DNI", 10) +
      pad("Nacimiento", 12) +
      pad("Edad", 9) +
      pad("Sangre", 7) +
      "Alergia o nota",
  );
  const hoy = new Date(L.fecha(0));
  const edad = (n) => {
    const d = new Date(n);
    let meses =
      (hoy.getFullYear() - d.getFullYear()) * 12 +
      hoy.getMonth() -
      d.getMonth();
    if (hoy.getDate() < d.getDate()) meses--;
    return meses < 24 ? meses + " meses" : Math.floor(meses / 12) + " años";
  };
  const NOTAS = {
    "luciana.ramos@correo.com":
      "lactante, carné con 3 dosis atrasadas, crecimiento",
    "mateo.castro@correo.com": "pediátrico, carné y crecimiento",
    "valentina.torres@correo.com":
      "pediátrica, alerta de IMC alto en crecimiento",
    "emma.soto@correo.com": "lactante, carné al día con dosis próximas",
    "thiago.rios@correo.com": "pediátrico, carné con 9 dosis atrasadas",
    "gael.mamani@correo.com": "pediátrico, carné completo, alerta de IMC alto",
    "isabella.cruz@correo.com": "pediátrica, carné al día",
    "rosa.quispe@correo.com":
      "resultado de la fusión de dos historias duplicadas, ya fusionada",
    "marcos.cardenas@correo.com":
      "duplicado pendiente de fusionar con marcos.cardenas2, para probar la fusión",
    "marcos.cardenas2@correo.com":
      "duplicado pendiente de fusionar con marcos.cardenas, para probar la fusión",
    "juan.perez@correo.com":
      "el paciente principal de las pruebas: alergia a penicilina, plan, cirugía, consentimientos",
  };
  for (const f of filas) {
    const c = f.split("|");
    if (c[0].startsWith("rosa.quispe2")) continue;
    w(
      "  " +
        pad(c[0], 32) +
        pad(c[1], 24) +
        pad(c[2], 10) +
        pad(c[3], 12) +
        pad(edad(c[3]), 9) +
        pad(c[6] || "", 7) +
        [c[5], NOTAS[c[0]]].filter(Boolean).join("; "),
    );
  }
  w(
    "  La cuenta rosa.quispe2@correo.com ya no representa a un paciente activo: su historia se fusionó en rosa.quispe@correo.com.",
  );
  sub("Lote de volumen");
  w(
    "  220 pacientes: qa.paciente001@correo.com hasta qa.paciente220@correo.com, contraseña " +
      PW.paciente,
  );
  w(
    "  Tienen citas, atenciones con diagnóstico CIE-10, pagos y exámenes de laboratorio con resultados históricos. Algunos tienen alergias registradas (penicilina, polen, mariscos). qa.paciente062 y qa.paciente124 son menores de 18 años.",
  );

  titulo("5. GUÍA DE PRUEBAS POR MÓDULO");
  w(
    "Los identificadores concretos salen de los datos de ejemplo cargados. Las fechas de las citas de ejemplo son de los próximos días y se atendieron de inmediato, por eso figuran como atendidas.",
  );

  sub("5.1 Recetas electrónicas y catálogo de medicamentos");
  w(
    "  Estado: la emisión de recetas NO funciona en este entorno (ver el apartado 6). Funcionan el catálogo, las alertas y las pantallas de consulta.",
  );
  w(
    "  admin.qa@: Catálogo de medicamentos en /admin/catalogo-medicamentos, 130 principios activos, 199 presentaciones y 75 interacciones.",
  );
  w(
    "  dr.garcia@, dr.salazar@: GET /api/v1/medicamentos?q=amoxicilina y POST /api/v1/recetas/alertas con el paciente y los medicamentos. juan.perez@ es alérgico a la penicilina: Amoxicilina da alerta contraindicada que exige justificación. carlos.mendoza@ es alérgico al ibuprofeno.",
  );
  w("  Atenciones listas para recetar, con su médico y su paciente:");
  for (const k of ["R01", "R02", "R03", "R04", "R05", "R07", "R14"]) {
    const e = enc(k);
    if (e.medico)
      w("    " + k + "  " + pad(e.medico, 28) + pad(e.paciente, 30) + e.motivo);
  }
  w(
    "  Hay 22 atenciones nuevas (R01 a R22) con receta clínica en texto: la receta electrónica se emitiría sobre ellas. R07 es diazepam, medicamento de control especial. farmacia@ y farmacia2@ dispensan desde /farmacia/recetas.",
  );

  sub("5.2 Historia clínica electrónica");
  w(
    "  Estado: Cerrar y firmar, las adendas y las correcciones funcionan con el certificado de prueba del entorno. Las atenciones R01 a R22 siguen abiertas para que QA las cierre y pruebe esas transiciones. También funcionan la línea de tiempo, el PDF de la historia, los plazos de conservación y la detección y fusión de duplicados.",
  );
  w(
    "  juan.perez@: descarga su historia en PDF desde Mi historia en PDF (GET /api/v1/hce/mi-historia/impresion). dr.garcia@: GET /api/v1/hce/pacientes/{id}/impresion.",
  );
  w(
    "  admin.qa@: Conservación y duplicados en /admin/hce. Duplicado pendiente de fusionar: marcos.cardenas@ y marcos.cardenas2@ (Marcos Cárdenas Vela, 30/09/1975). Fusión ya hecha como ejemplo: Rosa Quispe Huamán, historial HC-000239 fusionado en HC-000238.",
  );
  w(
    "  Atenciones abiertas para probar el cierre: R01 a R22 (cita de cada atención en la tabla del apartado 5.1).",
  );

  sub("5.3 Eventos adversos y guías clínicas");
  const eventos = [];
  for (let i = 1; i <= 10; i++) {
    const e = E("evento:E" + String(i).padStart(2, "0"));
    if (e) eventos.push(e);
  }
  w(
    "  Notifican: enfermeria@, enfermeria2@, enfermeria3@, farmacia@, lab@, imagenes@, caja@ y dr.garcia@ en su opción Notificar un evento. Gestionan: calidad@ y calidad2@ en /calidad/seguridad-paciente. Ven y exportan: gerencia@.",
  );
  for (const e of eventos)
    w(
      "    " +
        pad(e.codigo || "", 14) +
        pad(
          e.destino === "EN_ACCION_HECHA" ? "EN_ACCION (hecha)" : e.destino,
          20,
        ) +
        pad(e.tipo, 20) +
        pad(e.severidad, 10) +
        pad(e.area, 16) +
        "notificó " +
        (e.anonimo ? "de forma anónima" : e.quien),
    );
  w(
    "  Estados que se pueden recorrer: Notificado (varios sin análisis, uno anónimo), En análisis, En acción (con acción pendiente de completar por calidad) y Cerrado (" +
      ((E("evento:E01") || {}).codigo || "") +
      ", con análisis de cinco porqués, acción hecha y verificación de eficacia).",
  );
  w(
    "  Guías clínicas: 10 guías de demostración en /medico/guias y /calidad/guias, con versiones. calidad@ y admin.qa@ publican y versionan.",
  );

  sub("5.4 Planes de tratamiento y pacientes crónicos");
  w(
    "  Médicos: dr.garcia@ ve sus pacientes en /medico/pacientes-cronicos, dr.salazar@ los suyos. Pacientes: entran a Mi plan de tratamiento (/paciente/mi-plan). calidad, gerencia, enfermería y administración no tienen acceso.",
  );
  w(
    "  " +
      pad("Plan", 6) +
      pad("Paciente", 32) +
      pad("Médico", 26) +
      pad("Programa", 14) +
      "Notas",
  );
  const NP = {
    P01: "6 controles cada 14 días, 2 cumplidos, presión sistólica y diastólica de 6 semanas, mejora progresiva",
    P02: "controles vencidos, mediciones fuera de rango, adherencia baja",
    P03: "sin controles cumplidos, mediciones tomadas por el médico",
    P04: "plan reciente de controles semanales",
    P05: "glucosa en ayunas y hemoglobina glicosilada de 6 semanas",
    P06: "diabetes, mediciones del médico",
    P07: "asma con flujo espiratorio bajo la meta al inicio",
    P08: "plan reciente con 2 mediciones",
    P09: "peso corporal, 6 semanas",
    P10: "plan SUSPENDIDO, no admite mediciones",
  };
  for (let i = 1; i <= 10; i++) {
    const k = "P" + String(i).padStart(2, "0");
    const p = E("plan:" + k);
    if (p)
      w(
        "  " +
          pad(k, 6) +
          pad(p.paciente, 32) +
          pad(p.medico, 26) +
          pad(p.programa, 14) +
          NP[k],
      );
  }
  w(
    "  Transiciones a probar: registrar una medición del paciente, marcar un control como cumplido, suspender, completar y reactivar un plan. El tablero muestra riesgo, adherencia y controles vencidos.",
  );

  sub("5.5 Pediatría y odontología");
  w(
    "  Vacunación y crecimiento: dra.lopez@ en /medico/vacunacion y /medico/crecimiento, enfermeria@ en /enfermeria/ solo de pacientes con cita reciente o ingreso activo. Cada paciente ve Mi carné de vacunación y Mi crecimiento.",
  );
  for (const em of [
    "luciana.ramos@correo.com",
    "mateo.castro@correo.com",
    "valentina.torres@correo.com",
    "emma.soto@correo.com",
    "thiago.rios@correo.com",
    "gael.mamani@correo.com",
    "isabella.cruz@correo.com",
  ]) {
    const n = E("nino:" + em);
    w(
      "    " +
        pad(em, 32) +
        (n && n.resumenCarne && typeof n.resumenCarne === "object"
          ? "dosis aplicadas " +
            n.resumenCarne.aplicadas +
            ", atrasadas " +
            n.resumenCarne.atrasadas +
            ", próximas " +
            n.resumenCarne.proximas
          : NOTAS[em] || ""),
    );
  }
  w(
    "  Odontología: dra.montoya@ en /medico/odontograma y /medico/planes-odontologicos. Odontogramas con cambios de piezas para juan.perez@, maria.rodriguez@, carlos.mendoza@ y sofia.ramirez@.",
  );
  w(
    "  Planes odontológicos: juan.perez@ en curso con consentimiento y pago de 100 soles; maria.rodriguez@ presentado y pendiente de aceptar (el paciente lo acepta en Mi salud dental); carlos.mendoza@ aceptado con un cobro de 100 soles registrado por caja@; sofia.ramirez@ en borrador. caja@ o caja2@ cobran el saldo en /caja/cobros-odontologicos.",
  );

  sub("5.6 Quirófano y esterilización");
  w(
    "  Equipo: cirujano dr.cirujano@, anestesióloga dra.anestesia@, instrumentista enfermeria3@, programa enfermeria2@. Agenda de médicos en /medico/quirofano y de enfermería en /enfermeria/quirofano. El paciente firma en Mis consentimientos y ve su cirugía en /paciente/mis-cirugias.",
  );
  const CIRS = {
    K1: "solicitada, pendiente de programar",
    K2: "programada, con equipo y quirófano",
    K3: "en preparación, con consentimientos pendientes de firma: al intentar iniciarla el sistema la bloquea",
    K4: "en curso, con la lista antes de la inducción completa y la lista antes de la incisión incompleta",
    K5: "finalizada, con las tres listas, nota operatoria, registro anestésico, uso de set esterilizado e insumos descontados",
    K6: "suspendida con motivo",
    K7: "cancelada con motivo",
  };
  for (let i = 1; i <= 7; i++) {
    const c = E("cir:K" + i);
    if (c)
      w(
        "    " +
          pad(c.codigo || "", 12) +
          pad((c.estado || "").replace("_", " "), 16) +
          CIRS["K" + i],
      );
  }
  w(
    "  Pacientes de esas cirugías, en orden: sofia.ramirez, diego.herrera, luis.vargas, jorge.medina, camila.torres, ana.paredes y valeria.castillo. Además existen tres cirugías demo de juan.perez, maria.rodriguez y carlos.mendoza.",
  );
  w(
    "  Esterilización (enfermeria2@, consulta calidad@): ciclos CE-000004 conforme con 2 lotes de set de cirugía mayor, CE-000005 conforme con lote de cirugía menor que vence en un día (por vencer), CE-000006 con indicador biológico no conforme y lote bloqueado, CE-000007 en espera del indicador biológico. Más los 3 ciclos de demostración.",
  );

  sub("5.7 Planilla y asistencia");
  const pl = E("planillas") || {};
  w(
    "  rrhh@ o rrhh2@ en /rrhh/planilla, finanzas@ y finanzas2@ en /finanzas/planilla, gerencia@ en /gerencia/. Todo el personal y los médicos ven Mis boletas y Mi asistencia en el menú Cuenta.",
  );
  w(
    "  Planilla " +
      (pl.anterior
        ? "PLA-" +
          pl.anterior.anio +
          "-" +
          String(pl.anterior.mes).padStart(2, "0")
        : "") +
      ": mes anterior, calculada por rrhh@, aprobada por finanzas@ y pagada por finanzas2@, con 46 boletas visibles en Mis boletas. gerencia@ puede cerrarla.",
  );
  w(
    "  Planilla " +
      (pl.actual
        ? "PLA-" + pl.actual.anio + "-" + String(pl.actual.mes).padStart(2, "0")
        : "") +
      ": mes actual, calculada, pendiente de aprobar. Prueba la segregación: finanzas@ aprueba y no puede pagar, finanzas2@ paga.",
  );
  w(
    "  Asistencia: 18 empleados con marcaciones manuales de los últimos 6 días hábiles, incluidas tardanzas. rrhh@ consolida y ajusta en /rrhh/asistencia.",
  );

  sub("5.8 Compras y cuentas por pagar");
  const ordenes = L.lista(
    (await L.api("finanzas@tusalud.com", "GET", "/compras/ordenes")).data,
  );
  w(
    "  farmacia@ o farmacia2@ solicitan en /farmacia/compras/requisiciones. almacen@ cotiza y genera la orden, finanzas@ aprueba, almacen2@ recibe, finanzas3@ registra el comprobante de proveedor.",
  );
  for (const o of ordenes.slice(0, 12))
    w(
      "    " +
        pad(o.numero || o.codigo || "(sin número)", 18) +
        pad(o.estado, 22) +
        pad(o.proveedorNombre || o.proveedor || "", 38) +
        "total S/ " +
        (o.total !== undefined ? o.total : ""),
    );
  w(
    "  Hay una orden recibida en su totalidad con factura de proveedor y pago parcial de 40 soles, una con recepción parcial, una aprobada pendiente de recepción, una pendiente de aprobación de finanzas, una rechazada por gerencia con motivo, un borrador y una anulada.",
  );

  sub("5.9 Contabilidad y tesorería");
  w(
    "  finanzas@ y finanzas2@ registran asientos en /finanzas/contabilidad, gerencia@ ve libros, tesorería y flujo de caja, cierra y reabre periodos con motivo.",
  );
  const AS = [
    "AS-ALQUILER",
    "AS-EQUIPOS",
    "AS-SERVICIOS",
    "AS-GASTOS",
    "AS-REVERSADO",
    "AS-BORRADOR",
    "AS-BORRADOR2",
  ];
  for (const k of AS) {
    const a = E("asiento:" + k);
    if (a)
      w(
        "    " +
          pad(a.numero || "(borrador)", 18) +
          pad(k, 16) +
          (a.reversado
            ? "contabilizado y reversado"
            : a.contabilizado
              ? "contabilizado"
              : "borrador pendiente de contabilizar"),
      );
  }
  w(
    "  El periodo del mes anterior está CERRADO por gerencia@ (se puede reabrir con motivo) y el del mes actual está abierto. El banco BCP tiene un aporte de capital de 300000 soles, un egreso de comisiones conciliado y un extracto importado con 4 líneas, 3 pendientes de conciliar. El flujo de caja a 8 semanas es visible.",
  );

  sub("5.10 Facturación electrónica y caja");
  w(
    "  caja@ y caja2@ cobran en /caja/cobrar. finanzas@ y admin.qa@ configuran el emisor y las series, finanzas@ emite notas y comunica bajas. Estado: hay comprobantes aceptados, aceptados con observaciones, rechazado y anulado, con el proveedor simulado; los cobros se registran.",
  );
  for (const k of [
    "boleta-juan",
    "boleta-maria",
    "boleta-carlos",
    "boleta-diego",
    "boleta-mostrador",
    "factura-aceptada",
    "factura-observada",
    "factura-rechazada",
    "boleta-anulada",
  ]) {
    const c = E("cobro:" + k);
    if (c)
      w(
        "    " +
          pad(
            (c.tipo === "FACTURA" ? "F001-" : "B001-") +
              String((c.cpe && c.cpe.numero) || c.numero).padStart(8, "0"),
            16,
          ) +
          pad(k, 20) +
          pad(c.cliente || "", 32) +
          "S/ " +
          c.total +
          (c.anulada ? "  anulado" : ""),
      );
  }
  w(
    "  La factura-rechazada tiene el RUC 20123456789 con dígito verificador incorrecto, para probar la corrección del cliente. La factura-observada no tiene dirección. No hay nota de crédito emitida porque requiere un comprobante aceptado.",
  );

  sub("5.11 Documentos médicos y consentimientos");
  w(
    "  Médicos emiten en Documentos médicos, pacientes descargan y firman en Mis documentos y Mis consentimientos, cualquiera verifica el código en /verificar.",
  );
  const DOCS = [
    ["doc:cert-carlos", "Certificado", "carlos.mendoza@", "dr.garcia@"],
    [
      "doc:cert-jorge",
      "Certificado con diagnóstico",
      "jorge.medina@",
      "dr.salazar@",
    ],
    ["doc:cert-ana", "Certificado", "ana.paredes@", "dr.salazar@"],
    ["doc:descanso-juan", "Descanso médico", "juan.perez@", "dr.salazar@"],
    ["doc:descanso-diego", "Descanso médico", "diego.herrera@", "dr.quispe@"],
    ["doc:descanso-luis", "Descanso ANULADO", "luis.vargas@", "dr.quispe@"],
    [
      "doc:descanso-luis-reemplazo",
      "Descanso de reemplazo",
      "luis.vargas@",
      "dr.quispe@",
    ],
    [
      "doc:referencia-diego",
      "Referencia recibida",
      "diego.herrera@",
      "dr.quispe@",
    ],
    [
      "doc:contrarreferencia-diego",
      "Contrarreferencia",
      "diego.herrera@",
      "dr.quispe@",
    ],
    [
      "doc:referencia-maria",
      "Referencia emitida",
      "maria.rodriguez@",
      "dra.fernandez@",
    ],
  ];
  for (const [k, t, p, m] of DOCS) {
    const d = E(k);
    if (d)
      w(
        "    " +
          pad(t, 30) +
          pad(p + "correo.com", 30) +
          pad(m + "tusalud.com", 28) +
          "código " +
          (d.codigo || ""),
      );
  }
  w(
    "  Consentimientos: luis.vargas@ (pequeña cirugía, firmado en consultorio con testigo), maria.rodriguez@ (endoscopía, firmado con testigo), jorge.medina@ (procedimiento invasivo, firmado y REVOCADO), ana.paredes@ (anestesia local, PENDIENTE de firma: pruébalo con la firma del paciente).",
  );

  sub("5.12 Protección de datos");
  w(
    "  Pacientes en Mi privacidad (/paciente/privacidad); calidad@ y calidad2@ atienden en Solicitudes ARCO e Incidentes de datos.",
  );
  const ARCO = [
    ["S1", "RECIBIDA", "juan.perez"],
    ["S2", "EN REVISIÓN", "maria.rodriguez"],
    ["S3", "ATENDIDA", "carlos.mendoza"],
    ["S4", "PARCIAL", "jorge.medina"],
    ["S5", "RECHAZADA", "camila.torres"],
    ["S6", "ATENDIDA (rectificación de teléfono)", "sofia.ramirez"],
    ["S7", "EN REVISIÓN (cancelación)", "diego.herrera"],
  ];
  for (const [k, est, p] of ARCO) {
    const a = E("arco:" + k);
    if (a)
      w("    " + pad(a.codigo || "", 18) + pad(est, 38) + p + "@correo.com");
  }
  for (const k of ["I1", "I2"]) {
    const i = E("incidente:" + k);
    if (i)
      w(
        "    " +
          pad(i.codigo || "", 18) +
          (i.estado === "CERRADO"
            ? "incidente CERRADO, severidad media"
            : "incidente en contención, severidad alta, acceso no autorizado"),
      );
  }

  sub("5.13 Seguros, laboratorio, imágenes, hospitalización y telemedicina");
  w(
    "  Seguros: pólizas y convenios de demostración (SIS, EsSalud, Pacífico EPS, Rímac). Hay una preautorización nueva pendiente de luis.vargas@ solicitada por dr.garcia@ para que finanzas@ la apruebe o rechace. Una solicitud para diego.herrera@ se rechazó por el sistema porque su póliza está suspendida (regla correcta).",
  );
  w(
    "  Laboratorio e imágenes: se usan los datos de demostración existentes, 14 exámenes, 15 estudios y las órdenes demo de juan.perez@, carlos.mendoza@ y sofia.ramirez@. No se pudieron crear órdenes nuevas (apartado 6).",
  );
  w(
    "  Hospitalización: 14 camas y 5 ingresos de demostración. No se pudieron crear ingresos nuevos (apartado 6).",
  );
  for (const k of ["T1", "T2", "T3"]) {
    const t = E("tele:" + k);
    const T = {
      T1: "juan.perez@ con dr.garcia@",
      T2: "sofia.ramirez@ con dr.salazar@",
      T3: "carlos.mendoza@ con dr.garcia@",
    }[k];
    if (t)
      w(
        "  Teleconsulta " +
          T +
          " el " +
          String(t.fechaHora).replace("T", " ").slice(0, 16) +
          ", confirmada: entran desde Teleconsultas en /paciente/teleconsultas y /medico/teleconsultas.",
      );
  }

  sub("5.14 Notificaciones y canales");
  w(
    "  juan.perez@ tiene SMS y WhatsApp activos, maria.rodriguez@ SMS y carlos.mendoza@ WhatsApp, con preferencia de aviso de cirugía programada. Se activan en Mi perfil, pestaña Avisos. El administrador ve el código de verificación en la bandeja del proveedor simulado de Entregas de avisos.",
  );

  titulo("6. LIMITACIONES CONOCIDAS DEL ENTORNO DE QA");
  w(
    "  - SUNAT, los pagos con tarjeta, Yape y Plin, los SMS y WhatsApp y los respaldos son simulados. Un cobro con tarjeta, Yape o Plin no mueve dinero real.",
  );
  w(
    "  - No hay servidor de correo real: los correos no salen. Los códigos de verificación de correo no se pueden leer. Usa SMS o WhatsApp, cuyos códigos se leen en la bandeja del proveedor simulado.",
  );
  w(
    "  - La firma de recetas, documentos y comprobantes es electrónica del propio servidor y no equivale a una firma digital acreditada ante INDECOPI ni a un certificado reconocido por SUNAT.",
  );
  w(
    "  - Certificado digital: el entorno usa un certificado de prueba autofirmado. Las recetas, el cierre de atenciones y los comprobantes funcionan, pero la firma no es válida ante SUNAT ni equivale a una firma digital acreditada.",
  );
  w(
    "  - Órdenes de laboratorio e imágenes nuevas: ya funcionan. Si una orden de imágenes da 409, suele ser una regla de negocio legítima, como la preautorización del convenio o una sala ocupada en ese horario.",
  );
  w(
    "  - caja@ tiene su caja abierta; caja2@ no: debe abrir su caja antes de cobrar y, mientras no la abra, el resumen de su pantalla de inicio responde 409.",
  );
  w(
    "  - La telemedicina usa el servidor de relé del entorno y necesita cámara y micrófono, o un navegador con dispositivos simulados. La sala se habilita poco antes de la hora de la cita.",
  );
  w(
    "  - Los textos legales, las tablas de vacunas, las curvas de crecimiento, las interacciones medicamentosas, las políticas de conservación y los parámetros de planilla son de demostración y están marcados por validar.",
  );
  w(
    "  - Las citas de ejemplo de este lote se crearon en fechas futuras y se atendieron de inmediato para que existan atenciones recientes.",
  );
  w(
    "  - Fallos intermitentes posibles: respuestas 429 si se hacen muchos inicios de sesión seguidos, cortes breves cuando se actualiza el entorno y un primer acceso lento al abrir una aplicación.",
  );

  titulo("7. CÓMO REPORTAR");
  w(
    "  Para cada hallazgo anota: módulo y pantalla, usuario con el que entraste, pasos para reproducirlo, resultado esperado, resultado obtenido, hora aproximada con la zona horaria de Perú, y una captura de pantalla. Si es un error de la API, copia la petición, el código HTTP y el cuerpo de la respuesta sin incluir el token.",
  );
  w(
    "  Clasifica la severidad: bloqueante si impide un flujo de punta a punta, mayor si hay un resultado incorrecto con rodeo, menor si es de presentación. Entrega los reportes al responsable de QA del equipo, que los consolida y los traslada a desarrollo.",
  );
  w(
    "  No cambies las contraseñas de las cuentas de demostración ni borres los datos de ejemplo, para que los demás probadores encuentren el mismo estado.",
  );
  w();

  fs.writeFileSync(DESTINO, out.join("\n"), "utf8");
  console.log("credenciales escritas:", DESTINO, out.length, "líneas");
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
