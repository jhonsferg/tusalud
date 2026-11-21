const L = require("./lib");
const C = require("./clin");

const ADMIN = "admin@tusalud.com";
const FARMACIA = "farmacia@tusalud.com";

const M = {
  PARA: "238eb0b3-c264-489c-9c0e-e88e02c75b67",
  IBU: "954576a3-6ddb-4ea1-99ea-737feef68ab4",
  AMOX: "14f855e1-ca36-4bb1-88f0-62db21bf3185",
  LOSA: "86e088d4-d7cf-4f3b-ab11-bddb0a0225c8",
  METF: "bfae3887-c330-4c98-9128-d4d3e16990c5",
  OMEP: "4f860440-a7fb-4034-adad-69bdb554ca8c",
  LORA: "2162a761-06c4-41c6-8f6c-885cf3309b80",
  SALB: "20534c94-dbad-4733-b1f7-1b594216ed72",
  ENAL: "e0c14f4b-e492-41d5-9d1a-040e2c989584",
  NAPR: "66cdebaf-f9f8-4be1-869a-55046a820026",
  DICL: "918f3019-7138-4ec7-aa80-be2d399f4453",
  VITD: "d3ace556-937f-48fa-ab30-370cf330d706",
  DIAZ: "c005fe38-606a-4562-a7da-4be9a3a0bbc0",
};

const R = {
  PARA: {
    m: M.PARA,
    nombre: "Paracetamol 500 mg",
    dosis: "1 tableta",
    frecuencia: "Cada 8 horas",
    duracion: "5 días",
    cant: 15,
  },
  IBU: {
    m: M.IBU,
    nombre: "Ibuprofeno 400 mg",
    dosis: "1 tableta",
    frecuencia: "Cada 8 horas",
    duracion: "3 días",
    cant: 9,
  },
  AMOX: {
    m: M.AMOX,
    nombre: "Amoxicilina 500 mg",
    dosis: "1 cápsula",
    frecuencia: "Cada 8 horas",
    duracion: "7 días",
    cant: 21,
  },
  LOSA: {
    m: M.LOSA,
    nombre: "Losartán 50 mg",
    dosis: "1 tableta",
    frecuencia: "Cada 24 horas",
    duracion: "30 días",
    cant: 30,
  },
  METF: {
    m: M.METF,
    nombre: "Metformina 850 mg",
    dosis: "1 tableta",
    frecuencia: "Cada 12 horas",
    duracion: "30 días",
    cant: 60,
  },
  OMEP: {
    m: M.OMEP,
    nombre: "Omeprazol 20 mg",
    dosis: "1 cápsula",
    frecuencia: "Cada 24 horas",
    duracion: "14 días",
    cant: 14,
  },
  LORA: {
    m: M.LORA,
    nombre: "Loratadina 10 mg",
    dosis: "1 tableta",
    frecuencia: "Cada 24 horas",
    duracion: "7 días",
    cant: 7,
  },
  SALB: {
    m: M.SALB,
    nombre: "Salbutamol inhalador 100 mcg",
    dosis: "2 inhalaciones",
    frecuencia: "Cada 6 horas si hay silbidos",
    duracion: "30 días",
    cant: 1,
  },
  ENAL: {
    m: M.ENAL,
    nombre: "Enalapril 10 mg",
    dosis: "1 tableta",
    frecuencia: "Cada 12 horas",
    duracion: "30 días",
    cant: 60,
  },
  NAPR: {
    m: M.NAPR,
    nombre: "Naproxeno 550 mg",
    dosis: "1 tableta",
    frecuencia: "Cada 12 horas",
    duracion: "5 días",
    cant: 10,
  },
  DICL: {
    m: M.DICL,
    nombre: "Diclofenaco gel 1 %",
    dosis: "Aplicar capa fina",
    frecuencia: "Cada 8 horas",
    duracion: "7 días",
    cant: 1,
  },
  VITD: {
    m: M.VITD,
    nombre: "Vitamina D3 1000 UI",
    dosis: "1 cápsula",
    frecuencia: "Cada 24 horas",
    duracion: "60 días",
    cant: 60,
  },
  DIAZ: {
    m: M.DIAZ,
    nombre: "Diazepam 10 mg tableta",
    dosis: "1/2 tableta",
    frecuencia: "Cada noche",
    duracion: "7 días",
    cant: 4,
  },
};

const G = "dr.garcia@tusalud.com";
const LO = "dra.lopez@tusalud.com";
const SA = "dr.salazar@tusalud.com";
const VE = "dra.vega@tusalud.com";
const QU = "dr.quispe@tusalud.com";
const FE = "dra.fernandez@tusalud.com";
const DE = "dr.delgado@tusalud.com";
const p = (n) => n + "@correo.com";

const ENC = [
  {
    k: "R01",
    pac: p("juan.perez"),
    med: G,
    motivo: "Control de presión arterial",
    dx: ["I10"],
    diagnostico: "Hipertensión arterial en tratamiento",
    vit: {
      presionArterial: "148/94",
      pesoKg: 78,
      tallaCm: 174,
      frecuenciaCardiaca: 76,
    },
    rx: [R.LOSA, R.VITD],
    accion: "total",
  },
  {
    k: "R02",
    pac: p("juan.perez"),
    med: SA,
    motivo: "Dolor de garganta con fiebre",
    dx: ["J02.9"],
    diagnostico: "Faringitis aguda",
    vit: { temperaturaC: 38.2 },
    rx: [R.AMOX, R.PARA],
    accion: "parcial",
    justifica: true,
  },
  {
    k: "R03",
    pac: p("carlos.mendoza"),
    med: G,
    motivo: "Seguimiento de hipertensión",
    dx: ["I10"],
    diagnostico: "Hipertensión arterial",
    vit: { presionArterial: "142/90", pesoKg: 88 },
    rx: [R.ENAL],
    accion: "total",
  },
  {
    k: "R04",
    pac: p("carlos.mendoza"),
    med: SA,
    motivo: "Resfriado persistente",
    dx: ["J06.9"],
    diagnostico: "Infección respiratoria aguda",
    rx: [R.PARA, R.LORA, R.IBU],
    accion: "parcial",
    justifica: true,
  },
  {
    k: "R05",
    pac: p("sofia.ramirez"),
    med: SA,
    motivo: "Crisis de silbidos al respirar",
    dx: ["J45.9"],
    diagnostico: "Asma bronquial",
    rx: [R.SALB],
    accion: null,
  },
  {
    k: "R06",
    pac: p("sofia.ramirez"),
    med: VE,
    motivo: "Picazón y erupción en la piel",
    dx: ["L30.9"],
    diagnostico: "Dermatitis",
    rx: [R.LORA],
    accion: "total",
  },
  {
    k: "R07",
    pac: p("maria.rodriguez"),
    med: SA,
    motivo: "Insomnio y ansiedad",
    dx: ["F41.1", "F51.0"],
    diagnostico: "Ansiedad generalizada con insomnio",
    rx: [R.DIAZ],
    accion: null,
  },
  {
    k: "R08",
    pac: p("maria.rodriguez"),
    med: FE,
    motivo: "Molestias gástricas",
    dx: ["K29.7"],
    diagnostico: "Gastritis",
    rx: [R.OMEP],
    accion: "total",
  },
  {
    k: "R09",
    pac: p("valeria.castillo"),
    med: VE,
    motivo: "Control de acné",
    dx: ["L70.0"],
    diagnostico: "Acné vulgar",
    rx: [R.VITD],
    accion: null,
  },
  {
    k: "R10",
    pac: p("diego.herrera"),
    med: QU,
    motivo: "Dolor de rodilla tras lesión deportiva",
    dx: ["M25.5"],
    diagnostico: "Dolor articular de rodilla",
    rx: [R.DICL, R.NAPR],
    accion: "total",
  },
  {
    k: "R11",
    pac: p("diego.herrera"),
    med: G,
    motivo: "Chequeo cardiológico preventivo",
    dx: ["Z01.1"],
    diagnostico: "Chequeo cardiológico sin hallazgos",
    rx: [R.VITD],
    accion: null,
  },
  {
    k: "R12",
    pac: p("luis.vargas"),
    med: QU,
    motivo: "Dolor lumbar persistente",
    dx: ["M54.5"],
    diagnostico: "Lumbalgia",
    rx: [R.NAPR, R.OMEP],
    accion: "parcial",
  },
  {
    k: "R13",
    pac: p("luis.vargas"),
    med: G,
    motivo: "Control de colesterol y presión",
    dx: ["E78.5", "I10"],
    diagnostico: "Dislipidemia e hipertensión",
    vit: { presionArterial: "138/88" },
    rx: [R.LOSA],
    accion: null,
  },
  {
    k: "R14",
    pac: p("jorge.medina"),
    med: SA,
    motivo: "Control de glucosa",
    dx: ["E11.9"],
    diagnostico: "Diabetes mellitus tipo 2",
    vit: { pesoKg: 92 },
    rx: [R.METF],
    accion: "total",
  },
  {
    k: "R15",
    pac: p("jorge.medina"),
    med: G,
    motivo: "Control de colesterol",
    dx: ["E78.5"],
    diagnostico: "Hipercolesterolemia",
    rx: [R.OMEP],
    accion: null,
  },
  {
    k: "R16",
    pac: p("camila.torres"),
    med: FE,
    motivo: "Control ginecológico",
    dx: ["N94.6"],
    diagnostico: "Dismenorrea",
    rx: [R.IBU],
    accion: "anular",
  },
  {
    k: "R17",
    pac: p("camila.torres"),
    med: G,
    motivo: "Dolor en el pecho al caminar rápido",
    dx: ["R07.4"],
    diagnostico: "Dolor torácico en estudio",
    rx: [R.PARA],
    accion: "reemplazar",
  },
  {
    k: "R18",
    pac: p("ana.paredes"),
    med: SA,
    motivo: "Cefalea frecuente",
    dx: ["R51"],
    diagnostico: "Cefalea tensional",
    rx: [R.PARA, R.NAPR],
    accion: "total",
  },
  {
    k: "R19",
    pac: p("valeria.castillo"),
    med: FE,
    motivo: "Control prenatal",
    dx: ["Z34.9"],
    diagnostico: "Embarazo normal",
    rx: [R.VITD],
    accion: null,
  },
  {
    k: "R20",
    pac: p("maria.rodriguez"),
    med: DE,
    motivo: "Fatiga visual",
    dx: ["H52.4"],
    diagnostico: "Presbicia",
    rx: [R.PARA],
    accion: null,
  },
  {
    k: "R21",
    pac: p("sofia.ramirez"),
    med: G,
    motivo: "Palpitaciones ocasionales",
    dx: ["R00.2"],
    diagnostico: "Palpitaciones",
    rx: [R.PARA],
    accion: null,
  },
  {
    k: "R22",
    pac: p("juan.perez"),
    med: VE,
    motivo: "Dermatitis en las manos",
    dx: ["L23.9"],
    diagnostico: "Dermatitis alérgica de contacto",
    rx: [R.LORA],
    accion: "total",
  },
];

async function emitir(spec, reg, extra) {
  const items = spec.rx.map((x, i) => ({
    atencionPosicion: i,
    medicamentoId: x.m,
    cantidad: x.cant,
  }));
  const al = await L.api(spec.med, "POST", "/recetas/alertas", {
    pacienteId: reg.pacienteId,
    items,
  });
  const justificaciones = [];
  for (const a of (al.data && al.data.alertas) || []) {
    if (a.requiereJustificacion)
      justificaciones.push({
        clave: a.clave,
        texto:
          "Beneficio mayor al riesgo; antecedente evaluado con el paciente, que tolera el medicamento y será vigilado en el control.",
      });
  }
  const cuerpo = {
    atencionId: reg.atencionId,
    indicaciones:
      spec.indicaciones ||
      "Tomar según indicación y acudir a control si persisten los síntomas",
    items,
    justificaciones,
    ...(extra || {}),
  };
  const r = await L.api(spec.med, "POST", "/recetas", cuerpo);
  if (r.status !== 201 && r.status !== 200)
    throw new Error(
      "Receta " + r.status + " " + JSON.stringify(r.data).slice(0, 300),
    );
  return r.data;
}

async function main() {
  const limite = Number(process.argv[2] || ENC.length);
  const out = [];
  let firmaBloqueada = false;
  for (const spec of ENC.slice(0, limite)) {
    const t0 = Date.now();
    const estado = L.E.get("rx:" + spec.k) || {};
    try {
      const reg = await C.atender(
        spec.k,
        { ...spec, especialidad: spec.esp },
        ADMIN,
      );
      if (!estado.recetaId && !firmaBloqueada) {
        let rec;
        try {
          rec = await emitir(spec, reg);
        } catch (e) {
          if (/Receta 500/.test(e.message)) {
            firmaBloqueada = true;
            L.E.set("bloqueo:firma", {
              desde: new Date().toISOString(),
              detalle:
                "POST /recetas responde 500: sin certificado digital configurado en el servidor",
            });
            out.push(
              spec.k + " atención creada, receta bloqueada por el certificado",
            );
            console.log(out[out.length - 1]);
            continue;
          }
          throw e;
        }
        estado.recetaId = rec.id;
        estado.numero = rec.numero;
        estado.codigo = rec.codigoVerificacion || rec.codigo;
        estado.items = (rec.items || []).map((i) => ({
          id: i.id,
          pos: i.atencionPosicion,
          cant: i.cantidad,
        }));
        estado.estado = rec.estado;
        L.E.set("rx:" + spec.k, estado);
      }
      if (spec.accion && !estado.accionHecha && estado.recetaId) {
        const rid = estado.recetaId;
        if (spec.accion === "total") {
          const items = estado.items.map((i) => ({
            itemId: i.id,
            cantidad: i.cant,
          }));
          const d = await L.api(
            FARMACIA,
            "POST",
            "/recetas/farmacia/" + rid + "/dispensar",
            { items, metodoPago: "EFECTIVO" },
          );
          estado.dispensacion = d.status;
          if (d.status >= 400)
            estado.dispError = JSON.stringify(d.data).slice(0, 200);
        } else if (spec.accion === "parcial") {
          const it = estado.items[0];
          const d = await L.api(
            FARMACIA,
            "POST",
            "/recetas/farmacia/" + rid + "/dispensar",
            {
              items: [
                {
                  itemId: it.id,
                  cantidad: Math.max(1, Math.floor(it.cant / 3)),
                },
              ],
              metodoPago: "EFECTIVO",
            },
          );
          estado.dispensacion = d.status;
          if (d.status >= 400)
            estado.dispError = JSON.stringify(d.data).slice(0, 200);
        } else if (spec.accion === "anular" || spec.accion === "reemplazar") {
          const a = await L.api(
            spec.med,
            "POST",
            "/recetas/" + rid + "/anular",
            {
              motivo:
                "Error en la cantidad indicada, se corrige la prescripción",
            },
          );
          estado.anulada = a.status;
          if (spec.accion === "reemplazar" && a.status < 300) {
            const reg = L.E.get("enc:" + spec.k);
            const rec2 = await emitir(
              { ...spec, rx: spec.rx.map((x) => ({ ...x, cant: 5 })) },
              reg,
              { reemplazaId: rid },
            );
            estado.reemplazoId = rec2.id;
            estado.reemplazoNumero = rec2.numero;
          }
        }
        estado.accionHecha = spec.accion;
        L.E.set("rx:" + spec.k, estado);
      }
      out.push(
        spec.k +
          " ok " +
          (estado.numero || "") +
          " " +
          (estado.dispError || "") +
          " " +
          (Date.now() - t0) +
          "ms",
      );
    } catch (e) {
      out.push(spec.k + " FALLO " + e.message.slice(0, 300));
    }
    console.log(out[out.length - 1]);
  }
  console.log(JSON.stringify(L.resumen()));
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
