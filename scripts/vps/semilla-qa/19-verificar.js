const L = require("./lib");

const PROPIETARIO = process.env.PROPIETARIO_EMAIL || "";
const ADMIN = "admin@tusalud.com";

const HOME = {
  ROLE_ADMIN: "/admin/seguridad/resumen",
  ROLE_MEDICO: "/medicos/me/agenda",
  ROLE_PACIENTE: "/citas/mis-citas",
  ROLE_RECEPCION: "/recepcion/resumen",
  ROLE_CAJA: "/caja/resumen",
  ROLE_ENFERMERIA: "/hospitalizacion/resumen",
  ROLE_LABORATORIO: "/laboratorio/resumen",
  ROLE_IMAGENES: "/imagenes/resumen",
  ROLE_FARMACIA: "/farmacia/resumen",
  ROLE_ALMACEN: "/almacen/resumen",
  ROLE_GERENCIA: "/reportes/ejecutivo",
  ROLE_FINANZAS: "/reportes/financiero",
  ROLE_OPERACIONES: "/reportes/operativo",
  ROLE_CALIDAD: "/reportes/calidad",
  ROLE_RRHH: "/rrhh/resumen",
};

const ORDEN = Object.keys(HOME);

async function todosLosUsuarios() {
  const r = await L.api(ADMIN, "GET", "/admin/usuarios");
  const mapa = new Map();
  for (const u of L.lista(r.data)) mapa.set(u.email.toLowerCase(), u);
  return [...mapa.values()];
}

async function main() {
  const salida = {
    fecha: new Date().toISOString(),
    logins: [],
    home: [],
    permisos: [],
    hallazgos: [],
  };
  const usuarios = await todosLosUsuarios();
  console.log("usuarios en el sistema:", usuarios.length);
  const propietarioPresente = usuarios.some(
    (u) => u.email.toLowerCase() === PROPIETARIO,
  );
  const volumen = usuarios.filter((u) =>
    /^qa\.paciente\d+@correo\.com$/.test(u.email),
  );
  const muestra = volumen.filter((u, i) => i % 22 === 0).slice(0, 12);
  const lista = usuarios
    .filter(
      (u) =>
        u.email.toLowerCase() !== PROPIETARIO &&
        !/^qa\.paciente\d+@correo\.com$/.test(u.email),
    )
    .concat(muestra);
  lista.sort((a, b) => a.email.localeCompare(b.email));
  console.log(
    "a verificar:",
    lista.length,
    "muestra de volumen:",
    muestra.length,
  );

  for (const u of lista) {
    const rolEsperado = (u.roles || [])[0];
    let res = { email: u.email, rolEsperado };
    try {
      const t0 = Date.now();
      const tk = await L.login(u.email, true);
      const info = await L.loginCompleto(u.email);
      res.status = 200;
      res.roles = info.roles;
      res.rolOk = (u.roles || []).every((r) => (info.roles || []).includes(r));
      res.ms = Date.now() - t0;
    } catch (e) {
      res.status = "ERROR";
      res.error = e.message.slice(0, 160);
    }
    salida.logins.push(res);
    if (res.status === 200 && res.rolOk) {
      const rol = ORDEN.find((r) => (u.roles || []).includes(r)) || rolEsperado;
      const ruta = HOME[rol];
      if (ruta) {
        const h = await L.api(u.email, "GET", ruta);
        salida.home.push({
          email: u.email,
          rol,
          ruta,
          status: h.status,
          ms: h.ms,
        });
        if (h.status >= 500 || h.status === 404)
          salida.hallazgos.push({
            tipo: "home",
            email: u.email,
            ruta,
            status: h.status,
            detalle: JSON.stringify(h.data).slice(0, 200),
          });
      }
    } else {
      salida.hallazgos.push({ tipo: "login", email: u.email, detalle: res });
    }
    console.log(
      u.email,
      res.status,
      res.rolOk,
      salida.home.length ? salida.home[salida.home.length - 1].status : "",
    );
  }

  const enc = (k) => L.E.get("enc:" + k);
  const plan = (k) => L.E.get("plan:" + k);
  const docDe = (k) => L.E.get("doc:" + k);
  const consDe = (k) => L.E.get("cons:" + k);
  const PRUEBAS = [
    [
      "recepcion@tusalud.com",
      "GET",
      "/recetas/paciente/" + enc("R01").pacienteId,
      [403],
      "Recepción no ve recetas de un paciente",
    ],
    [
      "recepcion@tusalud.com",
      "GET",
      "/hce/atenciones/" + enc("R01").atencionId + "/versiones",
      [403],
      "Recepción no ve el historial de la atención",
    ],
    [
      "recepcion@tusalud.com",
      "GET",
      "/planes-tratamiento/tablero",
      [403],
      "Recepción no ve el tablero de crónicos",
    ],
    [
      "recepcion@tusalud.com",
      "GET",
      "/documentos-medicos?pacienteId=" + enc("R03").pacienteId,
      [403],
      "Recepción no lista documentos médicos",
    ],
    [
      "recepcion@tusalud.com",
      "GET",
      "/seguridad-paciente/eventos",
      [403],
      "Recepción no ve eventos adversos",
    ],
    [
      "caja@tusalud.com",
      "GET",
      "/quirofano/cirugias/" + L.E.get("cir:K4").id,
      [403],
      "Caja no abre el detalle de una cirugía",
    ],
    [
      "caja@tusalud.com",
      "GET",
      "/privacidad/gestion/solicitudes",
      [403],
      "Caja no ve solicitudes ARCO",
    ],
    [
      "enfermeria@tusalud.com",
      "GET",
      "/planes-tratamiento/tablero",
      [403],
      "Enfermería no ve el tablero de crónicos",
    ],
    [
      "enfermeria@tusalud.com",
      "GET",
      "/seguridad-paciente/tablero",
      [403],
      "Enfermería no ve el tablero de calidad",
    ],
    [
      "lab@tusalud.com",
      "GET",
      "/planilla/planillas",
      [403],
      "Laboratorio no ve la planilla",
    ],
    [
      "farmacia@tusalud.com",
      "GET",
      "/contabilidad/asientos",
      [403],
      "Farmacia no ve la contabilidad",
    ],
    [
      "dr.garcia@tusalud.com",
      "GET",
      "/planilla/planillas",
      [403],
      "El médico no ve la planilla de la clínica",
    ],
    [
      "gerencia@tusalud.com",
      "GET",
      "/planilla/boletas/" + "x",
      [400, 403, 404],
      "Gerencia no abre boletas individuales",
    ],
    [
      "admin@tusalud.com",
      "GET",
      "/contabilidad/asientos",
      [403],
      "El administrador no ve la contabilidad",
    ],
    [
      "admin@tusalud.com",
      "GET",
      "/hce/pacientes/" + enc("R01").pacienteId + "/impresion",
      [403],
      "El administrador no imprime historias clínicas",
    ],
    [
      "juan.perez@correo.com",
      "GET",
      "/planes-tratamiento/" + plan("P02").id,
      [403, 404],
      "Juan no ve el plan de Carlos",
    ],
    [
      "juan.perez@correo.com",
      "GET",
      "/documentos-medicos/" + docDe("cert-carlos").id,
      [403, 404],
      "Juan no ve el certificado de Carlos",
    ],
    [
      "juan.perez@correo.com",
      "GET",
      "/consentimientos/" + consDe("cons-luis").id,
      [403, 404],
      "Juan no ve el consentimiento de Luis",
    ],
    [
      "juan.perez@correo.com",
      "GET",
      "/citas/" + enc("R03").citaId,
      [403, 404],
      "Juan no ve la cita de Carlos",
    ],
    [
      "juan.perez@correo.com",
      "GET",
      "/privacidad/solicitudes/" + L.E.get("arco:S2").id,
      [403, 404],
      "Juan no ve la solicitud ARCO de María",
    ],
    [
      "juan.perez@correo.com",
      "GET",
      "/compras/ordenes",
      [403],
      "El paciente no ve las compras",
    ],
    [
      "maria.rodriguez@correo.com",
      "GET",
      "/vacunacion/pacientes/" +
        (L.E.get("nino:thiago.rios@correo.com") || {}).pacienteId +
        "/carne",
      [403, 404],
      "María no ve el carné de un niño ajeno",
    ],
    [
      "dr.quispe@tusalud.com",
      "GET",
      "/planes-tratamiento/" + plan("P01").id,
      [403, 404],
      "Un médico sin relación no ve el plan ajeno",
    ],
    [
      "dra.vega@tusalud.com",
      "GET",
      "/hce/atenciones/" + enc("R01").atencionId + "/versiones",
      [403, 404],
      "Un médico sin relación no ve el historial ajeno",
    ],
    [
      "finanzas@tusalud.com",
      "GET",
      "/quirofano/cirugias/" + L.E.get("cir:K4").id,
      [403],
      "Finanzas no abre el detalle de una cirugía",
    ],
    [
      "calidad@tusalud.com",
      "GET",
      "/planilla/mis-boletas",
      [200],
      "Calidad ve sus propias boletas",
    ],
    [
      "juan.perez@correo.com",
      "GET",
      "/planes-tratamiento/mios",
      [200],
      "Juan ve sus planes",
    ],
    [
      "juan.perez@correo.com",
      "GET",
      "/documentos-medicos/mios",
      [200],
      "Juan ve sus documentos",
    ],
    [
      "juan.perez@correo.com",
      "GET",
      "/consentimientos/mios",
      [200],
      "Juan ve sus consentimientos",
    ],
    [
      "dr.garcia@tusalud.com",
      "GET",
      "/planes-tratamiento/tablero",
      [200],
      "García ve su tablero de crónicos",
    ],
    [
      "dr.cirujano@tusalud.com",
      "GET",
      "/quirofano/cirugias/" + L.E.get("cir:K4").id,
      [200],
      "El cirujano abre su cirugía",
    ],
    [
      "finanzas@tusalud.com",
      "GET",
      "/planilla/planillas",
      [200],
      "Finanzas ve las planillas",
    ],
    [
      "calidad@tusalud.com",
      "GET",
      "/seguridad-paciente/eventos",
      [200],
      "Calidad ve los eventos",
    ],
    [
      "gerencia@tusalud.com",
      "GET",
      "/contabilidad/tesoreria/flujo-caja?semanas=8",
      [200],
      "Gerencia ve el flujo de caja",
    ],
    [
      "almacen2@tusalud.com",
      "GET",
      "/compras/ordenes",
      [200],
      "Almacén 2 ve las órdenes de compra",
    ],
  ];
  for (const [u, m, ruta, esperados, etiqueta] of PRUEBAS) {
    const r = await L.api(u, m, ruta);
    const ok = esperados.includes(r.status);
    salida.permisos.push({
      usuario: u,
      metodo: m,
      ruta: ruta.slice(0, 90),
      esperado: esperados,
      obtenido: r.status,
      ok,
      etiqueta,
    });
    if (!ok)
      salida.hallazgos.push({
        tipo: "permiso",
        etiqueta,
        usuario: u,
        ruta,
        esperado: esperados,
        obtenido: r.status,
        detalle: JSON.stringify(r.data).slice(0, 200),
      });
    console.log(ok ? "OK " : "XX ", r.status, etiqueta);
  }

  const adminQa = salida.logins.find((x) => x.email === "admin.qa@tusalud.com");
  salida.adminQaEntra = !!(adminQa && adminQa.status === 200 && adminQa.rolOk);
  salida.propietarioEnLista = false;
  salida.propietarioExisteEnSistema = propietarioPresente;
  salida.resumen = {
    usuariosVerificados: salida.logins.length,
    loginOk: salida.logins.filter((x) => x.status === 200 && x.rolOk).length,
    homeOk: salida.home.filter((x) => x.status === 200).length,
    homeTotal: salida.home.length,
    permisosOk: salida.permisos.filter((x) => x.ok).length,
    permisosTotal: salida.permisos.length,
    llamadas: L.resumen(),
  };
  L.escribirJson(L.SALIDA + "/verificacion.json", salida);
  console.log(JSON.stringify(salida.resumen));
  console.log("hallazgos:", salida.hallazgos.length);
  for (const h of salida.hallazgos)
    console.log(JSON.stringify(h).slice(0, 300));
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
