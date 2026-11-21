const L = require("./lib");

const RRHH = "rrhh@tusalud.com";
const APRUEBA = "finanzas@tusalud.com";
const PAGA = "finanzas2@tusalud.com";

async function main() {
  const hoy = new Date();
  const anioAct = hoy.getUTCFullYear();
  const mesAct = hoy.getUTCMonth() + 1;
  const ant = new Date(Date.UTC(anioAct, mesAct - 2, 1));
  const anioAnt = ant.getUTCFullYear();
  const mesAnt = ant.getUTCMonth() + 1;

  const buscar = async () =>
    L.lista((await L.api(RRHH, "GET", "/planilla/planillas")).data);
  let todas = await buscar();
  console.log(
    "planillas existentes:",
    todas.map((p) => p.anio + "-" + p.mes + ":" + p.estado).join(" "),
  );
  const de = (a, m) => todas.find((p) => p.anio === a && p.mes === m);

  let pAnt = de(anioAnt, mesAnt);
  if (!pAnt || pAnt.estado === "BORRADOR") {
    const r = await L.api(RRHH, "POST", "/planilla/planillas/calcular", {
      anio: anioAnt,
      mes: mesAnt,
    });
    console.log(
      "calcular mes anterior",
      r.status,
      JSON.stringify(r.data).slice(0, 200),
    );
    todas = await buscar();
    pAnt = de(anioAnt, mesAnt);
  }
  if (pAnt && ["CALCULADA", "DEVUELTA"].includes(pAnt.estado)) {
    const r = await L.api(
      APRUEBA,
      "POST",
      "/planilla/planillas/" + pAnt.id + "/aprobar",
      {},
    );
    console.log("aprobar", r.status, JSON.stringify(r.data).slice(0, 200));
    todas = await buscar();
    pAnt = de(anioAnt, mesAnt);
  }
  if (pAnt && pAnt.estado === "APROBADA") {
    const r = await L.api(
      PAGA,
      "POST",
      "/planilla/planillas/" + pAnt.id + "/pagar",
      {
        fechaPago: L.fecha(0),
        referencia: "TRF-QA-" + anioAnt + String(mesAnt).padStart(2, "0"),
      },
    );
    console.log("pagar", r.status, JSON.stringify(r.data).slice(0, 200));
    todas = await buscar();
    pAnt = de(anioAnt, mesAnt);
  }

  let pAct = de(anioAct, mesAct);
  if (!pAct) {
    const r = await L.api(RRHH, "POST", "/planilla/planillas/calcular", {
      anio: anioAct,
      mes: mesAct,
    });
    console.log(
      "calcular mes actual",
      r.status,
      JSON.stringify(r.data).slice(0, 200),
    );
    todas = await buscar();
    pAct = de(anioAct, mesAct);
  }
  L.E.set("planillas", {
    anterior: pAnt && {
      id: pAnt.id,
      anio: pAnt.anio,
      mes: pAnt.mes,
      estado: pAnt.estado,
    },
    actual: pAct && {
      id: pAct.id,
      anio: pAct.anio,
      mes: pAct.mes,
      estado: pAct.estado,
    },
  });
  console.log(JSON.stringify(L.E.get("planillas")));
  console.log(JSON.stringify(L.resumen()));
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
