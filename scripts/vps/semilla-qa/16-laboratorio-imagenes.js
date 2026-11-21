const L = require("./lib");

const LAB = "lab@tusalud.com";
const LAB2 = "lab2@tusalud.com";
const IMG = "imagenes@tusalud.com";
const IMG2 = "imagenes2@tusalud.com";

function enc(k) {
  return L.E.get("enc:" + k);
}

function idDe(d) {
  return d && (d.id || (d.resumen && d.resumen.id) || (d.orden && d.orden.id));
}

async function laboratorio() {
  const ex = L.lista(
    (await L.api(LAB, "GET", "/laboratorio/examenes?activo=true")).data,
  );
  console.log("exámenes", ex.length, JSON.stringify(ex[0]).slice(0, 300));
  const exGlu = ex.find((e) => /gluc/i.test(e.nombre)) || ex[0];
  const exHem = ex.find((e) => /hemo/i.test(e.nombre)) || ex[1] || ex[0];
  const casos = [
    { k: "L1", enc: "R01", ex: [exHem], fin: "SOLICITADA", pri: "NORMAL" },
    { k: "L2", enc: "R03", ex: [exGlu], fin: "MUESTRA", pri: "URGENTE" },
    { k: "L3", enc: "R05", ex: [exHem], fin: "RESULTADOS", pri: "NORMAL" },
    {
      k: "L4",
      enc: "R14",
      ex: [exGlu],
      fin: "VALIDADA",
      pri: "NORMAL",
      valor: "168",
    },
    {
      k: "L5",
      enc: "R12",
      ex: [exHem],
      fin: "VALIDADA",
      pri: "NORMAL",
      valor: "13.5",
    },
  ];
  for (const c of casos) {
    const e = enc(c.enc);
    const reg = L.E.get("lab:" + c.k) || {};
    if (!reg.id) {
      const r = await L.api(e.medico, "POST", "/laboratorio/ordenes", {
        pacienteId: e.pacienteId,
        prioridad: c.pri,
        indicaciones: "Control de datos de ejemplo de QA",
        examenIds: c.ex.map((x) => x.id),
      });
      if (r.status !== 201 && r.status !== 200) {
        console.log(
          "FALLO orden lab",
          c.k,
          r.status,
          JSON.stringify(r.data).slice(0, 300),
        );
        continue;
      }
      reg.id = idDe(r.data);
      reg.numero = r.data.numero || r.data.codigo;
      L.E.set("lab:" + c.k, reg);
    }
    const base = "/laboratorio/ordenes/" + reg.id;
    if (["MUESTRA", "RESULTADOS", "VALIDADA"].includes(c.fin) && !reg.muestra) {
      const r = await L.api(LAB, "PUT", base + "/muestra", {});
      if (r.status === 200) reg.muestra = true;
      else
        console.log(
          "FALLO muestra",
          c.k,
          r.status,
          JSON.stringify(r.data).slice(0, 300),
        );
      L.E.set("lab:" + c.k, reg);
    }
    if (
      ["RESULTADOS", "VALIDADA"].includes(c.fin) &&
      reg.muestra &&
      !reg.resultados
    ) {
      const det = await L.api(LAB2, "GET", base);
      const it = (det.data.items || [])[0];
      const pars = (it && (it.resultados || it.parametros)) || [];
      if (!it)
        console.log(
          "detalle sin items",
          JSON.stringify(det.data).slice(0, 400),
        );
      const r = await L.api(
        LAB2,
        "PUT",
        base + "/items/" + it.id + "/resultados",
        {
          resultados: pars.map((p) => ({
            parametroId: p.parametroId || p.id,
            valor: c.valor || "95",
          })),
          observaciones: "Resultado de datos de ejemplo de QA",
        },
      );
      if (r.status === 200) reg.resultados = true;
      else
        console.log(
          "FALLO resultados",
          c.k,
          r.status,
          JSON.stringify(r.data).slice(0, 300),
        );
      L.E.set("lab:" + c.k, reg);
    }
    if (c.fin === "VALIDADA" && reg.resultados && !reg.validada) {
      const r = await L.api(LAB, "PUT", base + "/validar", {});
      if (r.status === 200) reg.validada = true;
      else
        console.log(
          "FALLO validar",
          c.k,
          r.status,
          JSON.stringify(r.data).slice(0, 300),
        );
      L.E.set("lab:" + c.k, reg);
    }
    console.log(c.k, c.fin, JSON.stringify(reg));
  }
}

async function imagenes() {
  const est = L.lista(
    (await L.api(IMG, "GET", "/imagenes/estudios")).data,
  ).filter((e) => e.activo !== false);
  console.log("estudios", est.length);
  const porMod = (m) => est.find((e) => e.modalidad === m) || est[0];
  const casos = [
    { k: "I1", enc: "R01", est: porMod("RX"), fin: "SOLICITADA" },
    { k: "I2", enc: "R10", est: porMod("RM"), fin: "PROGRAMADA" },
    { k: "I3", enc: "R12", est: porMod("TC"), fin: "REALIZADA" },
    { k: "I4", enc: "R03", est: porMod("ECO"), fin: "INFORMADA" },
  ];
  for (const c of casos) {
    const e = enc(c.enc);
    const reg = L.E.get("img:" + c.k) || {};
    if (!reg.id) {
      const r = await L.api(e.medico, "POST", "/imagenes/ordenes", {
        pacienteId: e.pacienteId,
        prioridad: "NORMAL",
        indicacionClinica:
          "Evaluación solicitada en los datos de ejemplo de QA",
        estudioIds: [c.est.id],
      });
      if (r.status !== 201 && r.status !== 200) {
        console.log(
          "FALLO orden imagen",
          c.k,
          r.status,
          JSON.stringify(r.data).slice(0, 300),
        );
        continue;
      }
      reg.id = idDe(r.data);
      reg.numero = r.data.numero || r.data.codigo;
      L.E.set("img:" + c.k, reg);
    }
    const base = "/imagenes/ordenes/" + reg.id;
    if (
      ["PROGRAMADA", "REALIZADA", "INFORMADA"].includes(c.fin) &&
      !reg.programada
    ) {
      const r = await L.api(IMG, "PUT", base + "/programar", {
        programadaPara: L.fecha(1) + "T09:30:00",
        sala: "Sala de imágenes 1",
      });
      if (r.status === 200) reg.programada = true;
      else
        console.log(
          "FALLO programar",
          c.k,
          r.status,
          JSON.stringify(r.data).slice(0, 300),
        );
      L.E.set("img:" + c.k, reg);
    }
    if (
      ["REALIZADA", "INFORMADA"].includes(c.fin) &&
      reg.programada &&
      !reg.realizada
    ) {
      const r = await L.api(IMG2, "PUT", base + "/realizar", {
        observaciones: "Sin incidencias",
      });
      if (r.status === 200) reg.realizada = true;
      else
        console.log(
          "FALLO realizar",
          c.k,
          r.status,
          JSON.stringify(r.data).slice(0, 300),
        );
      L.E.set("img:" + c.k, reg);
    }
    if (c.fin === "INFORMADA" && reg.realizada && !reg.informada) {
      const r = await L.api(IMG2, "PUT", base + "/informe", {
        hallazgos:
          "Estudio sin hallazgos patológicos evidentes en las estructuras evaluadas.",
        conclusion: "Sin alteraciones evidentes.",
        hallazgoCritico: false,
        firmaColegiatura: "CMP-45210",
      });
      if (r.status === 200) reg.informada = true;
      else
        console.log(
          "FALLO informe",
          c.k,
          r.status,
          JSON.stringify(r.data).slice(0, 300),
        );
      L.E.set("img:" + c.k, reg);
    }
    console.log(c.k, c.fin, JSON.stringify(reg));
  }
}

async function main() {
  await laboratorio();
  await imagenes();
  console.log(JSON.stringify(L.resumen()));
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
