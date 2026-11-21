const fs = require("fs");
const L = require("./lib");
const C = require("./clin");

const FIRMA = fs.readFileSync(L.DIR + "/firma-ejemplo.txt", "utf8").trim();
const ADMIN = "admin@tusalud.com";
const CIR = "dr.cirujano@tusalud.com";
const ANES = "dra.anestesia@tusalud.com";
const PROG = "enfermeria2@tusalud.com";
const INSTR = "enfermeria3@tusalud.com";

const LISTAS = {
  ANTES_INDUCCION: [
    "IDENTIDAD_CONFIRMADA",
    "SITIO_MARCADO",
    "CONSENTIMIENTOS_VERIFICADOS",
    "EQUIPO_ANESTESIA_REVISADO",
    "ALERGIAS_REVISADAS",
  ],
  ANTES_INCISION: [
    "EQUIPO_SE_PRESENTO",
    "PROCEDIMIENTO_CONFIRMADO",
    "ANTIBIOTICO_ADMINISTRADO",
    "ESTERILIDAD_CONFIRMADA",
    "IMAGENES_DISPONIBLES",
  ],
  ANTES_SALIDA: [
    "PROCEDIMIENTO_REGISTRADO",
    "CONTEO_INSTRUMENTAL_COMPLETO",
    "MUESTRAS_ETIQUETADAS",
    "PROBLEMAS_DEL_EQUIPO_REVISADOS",
    "PLAN_POSTOPERATORIO_DEFINIDO",
  ],
};

const CASOS = [
  {
    k: "K1",
    pac: "sofia.ramirez@correo.com",
    proc: "Colecistectomía laparoscópica",
    dx: "K80.2",
    anest: "GENERAL",
    hasta: "SOLICITADA",
  },
  {
    k: "K2",
    pac: "diego.herrera@correo.com",
    proc: "Artroscopía de rodilla",
    dx: "S83.4",
    anest: "REGIONAL",
    hasta: "PROGRAMADA",
    dia: 3,
    hora: "08:00",
    qx: 0,
  },
  {
    k: "K3",
    pac: "luis.vargas@correo.com",
    proc: "Hernioplastia inguinal",
    dx: "K40.9",
    anest: "REGIONAL",
    hasta: "EN_PREPARACION",
    dia: 2,
    hora: "10:00",
    qx: 0,
  },
  {
    k: "K4",
    pac: "jorge.medina@correo.com",
    proc: "Apendicectomía abierta",
    dx: "K35.8",
    anest: "GENERAL",
    hasta: "EN_CURSO",
    dia: 1,
    hora: "09:00",
    qx: 1,
  },
  {
    k: "K5",
    pac: "camila.torres@correo.com",
    proc: "Herniorrafia umbilical",
    dx: "K42.9",
    anest: "LOCAL",
    hasta: "FINALIZADA",
    dia: 1,
    hora: "14:00",
    qx: 0,
  },
  {
    k: "K6",
    pac: "ana.paredes@correo.com",
    proc: "Extirpación de lipoma de espalda",
    dx: "D17.9",
    anest: "LOCAL",
    hasta: "SUSPENDIDA",
    dia: 4,
    hora: "11:00",
    qx: 1,
  },
  {
    k: "K7",
    pac: "valeria.castillo@correo.com",
    proc: "Extirpación de nevo en el hombro",
    dx: "D22.9",
    anest: "LOCAL",
    hasta: "CANCELADA",
  },
];

async function esterilizacion() {
  const sets = L.lista((await L.api(PROG, "GET", "/esterilizacion/sets")).data);
  const est = L.lista(
    (await L.api(PROG, "GET", "/esterilizacion/esterilizadores")).data,
  );
  const setPor = (c) => sets.find((s) => s.codigo === c).id;
  const hace = L.hace;
  const ciclos = [
    {
      k: "C1",
      set: "SET-MAY",
      cant: 2,
      h: 5,
      q: "CONFORME",
      b: "CONFORME",
      obs: "Carga de QA con indicadores conformes",
    },
    {
      k: "C2",
      set: "SET-LAP",
      cant: 1,
      h: 4,
      q: "CONFORME",
      b: "NO_CONFORME",
      obs: "Esporas viables en el indicador biológico",
    },
    {
      k: "C3",
      set: "SET-MEN",
      cant: 1,
      h: 13 * 24,
      q: "CONFORME",
      b: "CONFORME",
      obs: "Lote cercano a su vencimiento",
    },
    {
      k: "C4",
      set: "SET-MEN",
      cant: 1,
      h: 1,
      q: null,
      b: null,
      obs: "En espera del indicador biológico",
    },
  ];
  for (const c of ciclos) {
    const reg = L.E.get("ciclo:" + c.k) || {};
    if (!reg.id) {
      const r = await L.api(PROG, "POST", "/esterilizacion/ciclos", {
        esterilizadorId: est[0].id,
        iniciadoEn: hace(c.h),
        temperaturaC: 134,
        presionKpa: 210,
        duracionMin: 30,
        observaciones: c.obs,
        sets: [{ setId: setPor(c.set), cantidad: c.cant }],
      });
      if (r.status !== 201 && r.status !== 200) {
        console.log(
          "FALLO ciclo",
          c.k,
          r.status,
          JSON.stringify(r.data).slice(0, 250),
        );
        continue;
      }
      reg.id = r.data.id;
      reg.numero = r.data.numero;
      reg.lotes = (r.data.lotes || []).map((l) => ({
        id: l.id,
        codigo: l.codigo,
      }));
      L.E.set("ciclo:" + c.k, reg);
    }
    if (c.q && !reg.cerrado) {
      const r = await L.api(
        PROG,
        "PUT",
        "/esterilizacion/ciclos/" + reg.id + "/indicadores",
        { quimico: c.q, biologico: c.b, observaciones: c.obs },
      );
      if (r.status === 200) {
        reg.cerrado = c.b;
        reg.lotes = (r.data.lotes || reg.lotes).map((l) => ({
          id: l.id,
          codigo: l.codigo,
          estado: l.estado,
        }));
      } else
        console.log(
          "FALLO indicadores",
          c.k,
          r.status,
          JSON.stringify(r.data).slice(0, 250),
        );
      L.E.set("ciclo:" + c.k, reg);
    }
    console.log("ciclo", c.k, JSON.stringify(reg));
  }
}

async function main() {
  await esterilizacion();

  const qxs = L.lista(
    (await L.api(PROG, "GET", "/quirofano/quirofanos")).data,
  ).filter((q) => q.estado === "DISPONIBLE" && q.activo);
  const uCir = (await C.medicoDe(CIR, ADMIN)).usuarioId;
  const uAnes = (await C.medicoDe(ANES, ADMIN)).usuarioId;
  const uInstr = (await L.loginCompleto(INSTR)).id;
  const medCir = await C.medicoDe(CIR, ADMIN);
  const insumos = L.lista(
    (await L.api(PROG, "GET", "/quirofano/insumos")).data,
  );
  console.log(
    "insumos disponibles:",
    insumos.length,
    JSON.stringify(insumos.slice(0, 2)).slice(0, 300),
  );
  const lote1 = ((L.E.get("ciclo:C1") || {}).lotes || [])[0];
  const lote1b = ((L.E.get("ciclo:C1") || {}).lotes || [])[1];

  for (const c of CASOS) {
    let reg = L.E.get("cir:" + c.k) || {};
    if (!reg.cita) {
      const cita = await C.crearCita(
        c.pac,
        CIR,
        medCir,
        "Evaluación quirúrgica",
        "Cirugía General",
        1,
      );
      await L.api(CIR, "PUT", "/citas/" + cita.id + "/confirmar", {});
      reg.cita = cita.id;
      reg.pacienteId = cita.pacienteId;
      L.E.set("cir:" + c.k, reg);
    }
    if (!reg.id) {
      const ag = L.lista(
        (
          await L.api(
            CIR,
            "GET",
            "/quirofano/cirugias/agenda?desde=2020-01-01T00:00:00&hasta=2040-01-01T00:00:00",
          )
        ).data,
      );
      const ya = ag.find(
        (x) => x.pacienteId === reg.pacienteId && x.procedimiento === c.proc,
      );
      if (ya) {
        reg.id = ya.id;
        reg.codigo = ya.codigo;
        L.E.set("cir:" + c.k, reg);
      }
    }
    if (!reg.id) {
      const r = await L.api(CIR, "POST", "/quirofano/cirugias", {
        pacienteId: reg.pacienteId,
        diagnosticoCodigo: c.dx,
        procedimiento: c.proc,
        tipo: "ELECTIVA",
        tipoAnestesia: c.anest,
        indicacionesPreoperatorias:
          "Ayuno de 8 horas y examen preoperatorio completo",
      });
      if (r.status !== 201 && r.status !== 200) {
        console.log(
          "FALLO solicitud",
          c.k,
          r.status,
          JSON.stringify(r.data).slice(0, 250),
        );
        continue;
      }
      reg.id =
        r.data.id ||
        (r.data.resumen && r.data.resumen.id) ||
        (r.data.cirugia && r.data.cirugia.id);
      reg.codigo =
        r.data.codigo ||
        r.data.numero ||
        (r.data.resumen && r.data.resumen.codigo);
      if (!reg.id)
        console.log("forma de la solicitud:", Object.keys(r.data).join(","));
      L.E.set("cir:" + c.k, reg);
    }
    const orden = [
      "SOLICITADA",
      "PROGRAMADA",
      "EN_PREPARACION",
      "EN_CURSO",
      "FINALIZADA",
    ];
    const meta =
      c.hasta === "SUSPENDIDA"
        ? "EN_PREPARACION"
        : c.hasta === "CANCELADA"
          ? "SOLICITADA"
          : c.hasta;
    const nivel = orden.indexOf(meta);
    const base = "/quirofano/cirugias/" + reg.id;
    const paso = async (nombre, usuario, metodo, ruta, cuerpo) => {
      if (reg["p_" + nombre]) return true;
      const r = await L.api(usuario, metodo, ruta, cuerpo);
      if (r.status >= 200 && r.status < 300) {
        reg["p_" + nombre] = true;
        L.E.set("cir:" + c.k, reg);
        return r;
      }
      console.log(
        "FALLO",
        c.k,
        nombre,
        r.status,
        JSON.stringify(r.data).slice(0, 300),
      );
      return false;
    };
    if (nivel >= 1) {
      const q = qxs[c.qx || 0];
      const inicio = L.fecha(c.dia) + "T" + c.hora + ":00";
      const r = await paso("programar", PROG, "POST", base + "/programar", {
        quirofanoId: q.id,
        inicio,
        duracionMin: 120,
        equipo: [
          { usuarioId: uCir, rol: "CIRUJANO" },
          { usuarioId: uAnes, rol: "ANESTESIOLOGO" },
          { usuarioId: uInstr, rol: "INSTRUMENTISTA" },
        ],
      });
      if (r && r !== true) reg.quirofano = q.codigo;
      reg.inicio = inicio;
    }
    if (nivel >= 2) {
      const r = await paso(
        "consentimientos",
        CIR,
        "POST",
        base + "/consentimientos",
        {},
      );
      if (r && r !== true)
        console.log(
          "consentimientos",
          c.k,
          JSON.stringify(r.data).slice(0, 300),
        );
      await paso("preparar", PROG, "POST", base + "/preparar", {});
    }
    if (nivel >= 3) {
      const mis = L.lista(
        (await L.api(c.pac, "GET", "/consentimientos/mios")).data,
      ).filter(
        (x) =>
          x.estado === "PENDIENTE_FIRMA" &&
          (x.procedimiento || "").includes(c.proc),
      );
      const perfil =
        L.E.get("perfil:" + c.pac) ||
        (await L.api(c.pac, "GET", "/perfil")).data;
      L.E.set("perfil:" + c.pac, {
        dni: perfil.dni,
        nombre: perfil.nombreCompleto || perfil.nombre,
      });
      for (const k of mis) {
        const body = {
          firmaPng: FIRMA,
          firmanteTipo: "PACIENTE",
          nombre: perfil.nombreCompleto || perfil.nombre,
          dni: perfil.dni,
          via: "APP_PACIENTE",
        };
        let r = await L.api(
          c.pac,
          "POST",
          "/consentimientos/" + k.id + "/firma",
          body,
        );
        if (r.status >= 400) {
          r = await L.api(CIR, "POST", "/consentimientos/" + k.id + "/firma", {
            ...body,
            via: "CONSULTORIO",
            testigoNombre: "Gabriela Paz",
            testigoDni: "40506072",
          });
        }
        console.log(
          "firma consentimiento",
          c.k,
          k.id,
          r.status,
          r.status >= 400 ? JSON.stringify(r.data).slice(0, 200) : "",
        );
      }
      await paso("lista1", INSTR, "POST", base + "/checklist", {
        momento: "ANTES_INDUCCION",
        items: LISTAS.ANTES_INDUCCION,
        observaciones: "Todo en orden",
      });
      const ini = await paso("iniciar", CIR, "POST", base + "/iniciar", {});
      if (ini && c.hasta === "EN_CURSO") {
        await paso("lista2parcial", INSTR, "POST", base + "/checklist", {
          momento: "ANTES_INCISION",
          items: LISTAS.ANTES_INCISION.slice(0, 3),
        });
      }
    }
    if (nivel >= 4 && reg.p_iniciar) {
      if (lote1)
        await paso("set", INSTR, "POST", base + "/sets", { loteId: lote1.id });
      const ins =
        insumos.find((i) => /GASA/i.test(i.codigo || "")) || insumos[0];
      if (ins)
        await paso("insumo1", INSTR, "POST", base + "/insumos", {
          productoId: ins.productoId || ins.id,
          cantidad: 4,
        });
      const gu = insumos.find((i) => /GUANTES/i.test(i.codigo || ""));
      if (gu)
        await paso("insumo2", INSTR, "POST", base + "/insumos", {
          productoId: gu.productoId || gu.id,
          cantidad: 6,
        });
      await paso("lista2", INSTR, "POST", base + "/checklist", {
        momento: "ANTES_INCISION",
        items: LISTAS.ANTES_INCISION,
      });
      await paso("lista3", INSTR, "POST", base + "/checklist", {
        momento: "ANTES_SALIDA",
        items: LISTAS.ANTES_SALIDA,
      });
      await paso("nota", CIR, "PUT", base + "/nota-operatoria", {
        hallazgos:
          "Defecto de pared abdominal sin compromiso de asas intestinales",
        tecnica: "Reparación primaria con malla, sin incidentes",
        complicaciones: null,
        sangradoMl: 25,
        diagnosticoPostoperatorio: "Hernia umbilical reparada",
        indicacionesPostoperatorias:
          "Analgesia oral, control de herida en 7 días",
      });
      const t0 = (reg.inicio || L.fecha(1) + "T14:00:00").slice(0, 11);
      await paso("anestesia", ANES, "PUT", base + "/registro-anestesico", {
        asa: 2,
        inicio: t0 + "14:00:00",
        fin: t0 + "15:30:00",
        observaciones: "Sin incidentes anestésicos",
      });
      await paso("finalizar", CIR, "POST", base + "/finalizar", {});
    }
    if (c.hasta === "SUSPENDIDA")
      await paso("suspender", CIR, "POST", base + "/suspender", {
        motivo: "Paciente con fiebre el día de la cirugía, se reprograma",
      });
    if (c.hasta === "CANCELADA")
      await paso("cancelar", CIR, "POST", base + "/cancelar", {
        motivo: "El paciente decidió no operarse por ahora",
      });
    const det = await L.api(CIR, "GET", base);
    reg.estado = det.data && det.data.resumen && det.data.resumen.estado;
    reg.codigo =
      (det.data && det.data.resumen && det.data.resumen.codigo) || reg.codigo;
    L.E.set("cir:" + c.k, reg);
    console.log(c.k, reg.codigo, reg.estado, "esperado", c.hasta);
  }
  console.log(JSON.stringify(L.resumen()));
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
