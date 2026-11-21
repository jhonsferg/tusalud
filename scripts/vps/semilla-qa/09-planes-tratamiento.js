const L = require("./lib");

const G = "dr.garcia@tusalud.com";
const SA = "dr.salazar@tusalud.com";

const HTA = {
  programa: "HIPERTENSION",
  dx: "I10",
  objetivos: "Mantener la presión arterial por debajo de 130/85 mmHg",
  intervenciones:
    "Dieta baja en sodio, caminata de 30 minutos al día y control semanal de la presión en casa",
  medicacion: "Según la receta vigente",
  metas: [
    {
      parametro: "PRESION_SISTOLICA",
      valorMin: 90,
      valorMax: 130,
      unidad: "mmHg",
    },
    {
      parametro: "PRESION_DIASTOLICA",
      valorMin: 60,
      valorMax: 85,
      unidad: "mmHg",
    },
  ],
  series: {
    PRESION_SISTOLICA: [154, 148, 143, 139, 133, 128],
    PRESION_DIASTOLICA: [98, 94, 91, 89, 86, 82],
  },
};
const DIA = {
  programa: "DIABETES",
  dx: "E11.9",
  objetivos:
    "Lograr una glucosa en ayunas entre 70 y 110 mg/dl y una hemoglobina glicosilada menor de 7 por ciento",
  intervenciones:
    "Plan de alimentación, actividad física y automonitoreo de glucosa en ayunas",
  medicacion: "Metformina según la receta vigente",
  metas: [
    {
      parametro: "GLUCOSA_AYUNO",
      valorMin: 70,
      valorMax: 110,
      unidad: "mg/dl",
    },
    {
      parametro: "HEMOGLOBINA_GLICOSILADA",
      valorMin: 4,
      valorMax: 7,
      unidad: "%",
    },
  ],
  series: {
    GLUCOSA_AYUNO: [172, 158, 149, 136, 124, 116],
    HEMOGLOBINA_GLICOSILADA: [8.6, 8.1, 7.6],
  },
};
const ASMA = {
  programa: "ASMA",
  dx: "J45.9",
  objetivos:
    "Mantener el flujo espiratorio máximo sobre 350 l/min y evitar crisis nocturnas",
  intervenciones:
    "Evitar desencadenantes, técnica de inhalación y registro semanal del flujo espiratorio",
  medicacion: "Salbutamol de rescate según la receta vigente",
  metas: [
    {
      parametro: "FLUJO_ESPIRATORIO",
      valorMin: 350,
      valorMax: 600,
      unidad: "l/min",
    },
  ],
  series: { FLUJO_ESPIRATORIO: [290, 310, 335, 360, 345, 380] },
};
const OTRO = {
  programa: "OTRO",
  dx: "E66.9",
  objetivos:
    "Reducir el peso corporal hasta un máximo de 85 kg de forma gradual",
  intervenciones:
    "Plan nutricional, ejercicio aeróbico y control de peso cada dos semanas",
  medicacion: "Sin medicación",
  metas: [{ parametro: "PESO_KG", valorMin: 60, valorMax: 85, unidad: "kg" }],
  series: { PESO_KG: [98.5, 97.2, 95.8, 94.1, 92.9, 91.4] },
};

const PLANES = [
  {
    k: "P01",
    pac: "juan.perez@correo.com",
    med: G,
    t: HTA,
    semanas: 6,
    freq: 14,
    cant: 6,
    cumplidos: 2,
    medidor: "paciente",
  },
  {
    k: "P02",
    pac: "carlos.mendoza@correo.com",
    med: G,
    t: HTA,
    semanas: 8,
    freq: 14,
    cant: 8,
    cumplidos: 1,
    medidor: "paciente",
  },
  {
    k: "P03",
    pac: "luis.vargas@correo.com",
    med: G,
    t: HTA,
    semanas: 4,
    freq: 30,
    cant: 4,
    cumplidos: 0,
    medidor: "medico",
  },
  {
    k: "P04",
    pac: "camila.torres@correo.com",
    med: G,
    t: HTA,
    semanas: 3,
    freq: 7,
    cant: 6,
    cumplidos: 1,
    medidor: "paciente",
  },
  {
    k: "P05",
    pac: "jorge.medina@correo.com",
    med: SA,
    t: DIA,
    semanas: 6,
    freq: 14,
    cant: 6,
    cumplidos: 2,
    medidor: "paciente",
  },
  {
    k: "P06",
    pac: "maria.rodriguez@correo.com",
    med: SA,
    t: DIA,
    semanas: 5,
    freq: 21,
    cant: 4,
    cumplidos: 1,
    medidor: "medico",
  },
  {
    k: "P07",
    pac: "sofia.ramirez@correo.com",
    med: SA,
    t: ASMA,
    semanas: 6,
    freq: 14,
    cant: 6,
    cumplidos: 3,
    medidor: "paciente",
  },
  {
    k: "P08",
    pac: "ana.paredes@correo.com",
    med: SA,
    t: ASMA,
    semanas: 2,
    freq: 14,
    cant: 4,
    cumplidos: 0,
    medidor: "paciente",
  },
  {
    k: "P09",
    pac: "diego.herrera@correo.com",
    med: G,
    t: OTRO,
    semanas: 6,
    freq: 14,
    cant: 6,
    cumplidos: 2,
    medidor: "medico",
  },
  {
    k: "P10",
    pac: "juan.perez@correo.com",
    med: SA,
    t: DIA,
    semanas: 2,
    freq: 30,
    cant: 3,
    cumplidos: 0,
    medidor: "paciente",
    estadoFinal: "SUSPENDIDO",
  },
];

function enc(email, med) {
  const e = Object.entries(L.E.todo()).find(
    ([k, v]) =>
      k.startsWith("enc:") && v.paciente === email && v.medico === med,
  );
  return e && e[1];
}

function haceDias(d, hora) {
  const f = new Date(Date.now() - d * 86400000);
  return f.toISOString().slice(0, 10) + "T" + (hora || "08:00") + ":00";
}

async function main() {
  for (const p of PLANES) {
    let reg = L.E.get("plan:" + p.k) || {};
    const e = enc(p.pac, p.med);
    if (!e) {
      console.log("sin relación para", p.k, p.pac, p.med);
      continue;
    }
    if (!reg.id) {
      const inicio = haceDias(p.semanas * 7).slice(0, 10);
      const r = await L.api(p.med, "POST", "/planes-tratamiento", {
        pacienteId: e.pacienteId,
        programa: p.t.programa,
        diagnosticoCodigo: p.t.dx,
        objetivos: p.t.objetivos,
        intervenciones: p.t.intervenciones,
        medicacion: p.t.medicacion,
        inicio,
        frecuenciaControlDias: p.freq,
        cantidadControles: p.cant,
        metas: p.t.metas,
      });
      if (r.status !== 201 && r.status !== 200) {
        console.log(
          "FALLO plan",
          p.k,
          r.status,
          JSON.stringify(r.data).slice(0, 300),
        );
        continue;
      }
      reg = {
        id: r.data.id,
        paciente: p.pac,
        medico: p.med,
        programa: p.t.programa,
        inicio,
        controles: (r.data.controles || []).length,
      };
      L.E.set("plan:" + p.k, reg);
    }
    if (!reg.mediciones) {
      let n = 0;
      for (const [param, valores] of Object.entries(p.t.series)) {
        const total = Math.min(valores.length, p.semanas);
        const usados = valores.slice(valores.length - total);
        for (let i = 0; i < total; i++) {
          const diasAtras = (total - i) * 7 - 4;
          const cuerpo = {
            parametro: param,
            valor: usados[i],
            medidoEn: haceDias(
              diasAtras,
              param === "GLUCOSA_AYUNO" ? "07:00" : "08:15",
            ),
          };
          const usuario = p.medidor === "paciente" ? p.pac : p.med;
          const r = await L.api(
            usuario,
            "POST",
            "/planes-tratamiento/" + reg.id + "/mediciones",
            cuerpo,
          );
          if (r.status === 201 || r.status === 200) n++;
          else
            console.log(
              "FALLO medición",
              p.k,
              param,
              r.status,
              JSON.stringify(r.data).slice(0, 200),
            );
        }
      }
      reg.mediciones = n;
      L.E.set("plan:" + p.k, reg);
    }
    if (p.cumplidos && !reg.cumplidosHechos) {
      const det = await L.api(p.med, "GET", "/planes-tratamiento/" + reg.id);
      const ctrls = (det.data.controles || [])
        .filter((c) => c.estado === "PENDIENTE")
        .sort((a, b) => a.numero - b.numero);
      let n = 0;
      for (const c of ctrls.slice(0, p.cumplidos)) {
        const r = await L.api(
          p.med,
          "POST",
          "/planes-tratamiento/" + reg.id + "/controles/" + c.id + "/cumplir",
          {},
        );
        if (r.status === 200 || r.status === 201) n++;
        else
          console.log(
            "FALLO cumplir",
            p.k,
            r.status,
            JSON.stringify(r.data).slice(0, 200),
          );
      }
      reg.cumplidosHechos = n;
      L.E.set("plan:" + p.k, reg);
    }
    if (p.estadoFinal && !reg.estadoFinal) {
      const r = await L.api(
        p.med,
        "PUT",
        "/planes-tratamiento/" + reg.id + "/estado",
        { estado: p.estadoFinal },
      );
      if (r.status === 200) reg.estadoFinal = p.estadoFinal;
      L.E.set("plan:" + p.k, reg);
    }
    console.log(p.k, JSON.stringify(reg));
  }
  const t = await L.api(G, "GET", "/planes-tratamiento/tablero");
  console.log("tablero garcia", t.status, JSON.stringify(t.data).slice(0, 500));
  console.log(JSON.stringify(L.resumen()));
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
