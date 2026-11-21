const L = require("./lib");
const C = require("./clin");

const ADMIN = "admin@tusalud.com";
const FIN = "finanzas@tusalud.com";

const TELE = [
  {
    k: "T1",
    pac: "juan.perez@correo.com",
    med: "dr.garcia@tusalud.com",
    esp: "Cardiología",
    motivo: "Teleconsulta de control de presión arterial",
  },
  {
    k: "T2",
    pac: "sofia.ramirez@correo.com",
    med: "dr.salazar@tusalud.com",
    esp: "Medicina General",
    motivo: "Teleconsulta de seguimiento de asma",
  },
  {
    k: "T3",
    pac: "carlos.mendoza@correo.com",
    med: "dr.garcia@tusalud.com",
    esp: "Cardiología",
    motivo: "Teleconsulta para revisar resultados de laboratorio",
  },
];

async function telemedicina() {
  for (const t of TELE) {
    const reg = L.E.get("tele:" + t.k) || {};
    if (!reg.citaId) {
      const med = await C.medicoDe(t.med, ADMIN);
      const esp =
        med.especialidades.find((e) => e.nombre === t.esp) ||
        med.especialidades[0];
      let dia = 3;
      for (let i = 0; i < 5 && !reg.citaId; i++) {
        const h = await C.buscarHueco(t.pac, med.id, dia);
        if (!h) break;
        const r = await L.api(t.pac, "POST", "/citas", {
          medicoId: med.id,
          especialidadId: esp.id,
          fechaHora: h.fecha + "T" + h.hora + ":00",
          motivo: t.motivo,
          modalidad: "VIRTUAL",
        });
        if (r.status === 201 || r.status === 200) {
          reg.citaId = r.data.id;
          reg.fechaHora = r.data.fechaHora;
          const c = await L.api(
            t.med,
            "PUT",
            "/citas/" + r.data.id + "/confirmar",
            {},
          );
          reg.confirmada = c.status === 200;
        } else {
          console.log(
            "FALLO cita virtual",
            t.k,
            r.status,
            JSON.stringify(r.data).slice(0, 200),
          );
          dia = h.dia + 1;
        }
      }
      L.E.set("tele:" + t.k, reg);
    }
    const m = await L.api(t.pac, "GET", "/teleconsultas/mias");
    const mia = L.lista(m.data).find(
      (x) => x.citaId === reg.citaId || JSON.stringify(x).includes(reg.citaId),
    );
    if (mia) reg.teleconsultaId = mia.id;
    L.E.set("tele:" + t.k, reg);
    console.log(t.k, JSON.stringify(reg));
  }
}

async function seguros() {
  const casos = [
    {
      k: "PA1",
      enc: "R10",
      cat: "IMAGEN",
      desc: "Resonancia magnética de rodilla por dolor persistente",
      monto: 650,
      resolver: "aprobar",
    },
    {
      k: "PA2",
      enc: "R13",
      cat: "IMAGEN",
      desc: "Ecocardiograma de control por hipertensión",
      monto: 320,
      resolver: null,
    },
  ];
  for (const c of casos) {
    const e = L.E.get("enc:" + c.enc);
    const reg = L.E.get("preaut:" + c.k) || {};
    if (!reg.id) {
      const r = await L.api(e.medico, "POST", "/seguros/preautorizaciones", {
        pacienteId: e.pacienteId,
        categoria: c.cat,
        descripcion: c.desc,
        montoEstimado: c.monto,
      });
      if (r.status !== 201 && r.status !== 200) {
        console.log(
          "FALLO preautorización",
          c.k,
          r.status,
          JSON.stringify(r.data).slice(0, 250),
        );
        continue;
      }
      reg.id = r.data.id || (r.data.resumen && r.data.resumen.id);
      L.E.set("preaut:" + c.k, reg);
    }
    if (c.resolver === "aprobar" && !reg.aprobada) {
      const r = await L.api(
        FIN,
        "PUT",
        "/seguros/preautorizaciones/" + reg.id + "/aprobar",
        {},
      );
      if (r.status === 200) reg.aprobada = true;
      else
        console.log(
          "FALLO aprobar",
          c.k,
          r.status,
          JSON.stringify(r.data).slice(0, 250),
        );
      L.E.set("preaut:" + c.k, reg);
    }
    console.log(c.k, JSON.stringify(reg));
  }
}

async function main() {
  await telemedicina();
  await seguros();
  console.log(JSON.stringify(L.resumen()));
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
