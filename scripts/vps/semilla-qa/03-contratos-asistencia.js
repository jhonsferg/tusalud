const L = require("./lib");

const RRHH = "rrhh@tusalud.com";

function diasHabiles(n) {
  const r = [];
  let d = 1;
  while (r.length < n) {
    const f = new Date(Date.now() - d * 86400000);
    const w = f.getUTCDay();
    if (w !== 0 && w !== 6) r.push(f.toISOString().slice(0, 10));
    d++;
  }
  return r;
}

async function main() {
  const em = L.lista(
    (await L.api(RRHH, "GET", "/rrhh/empleados?size=300")).data,
  );
  const co = L.lista(
    (await L.api(RRHH, "GET", "/planilla/contratos?size=300")).data,
  );
  const conContrato = new Set(co.map((c) => c.empleadoId));
  let creados = 0;
  for (const e of em) {
    if (conContrato.has(e.id)) continue;
    const r = await L.api(RRHH, "POST", "/planilla/contratos", {
      empleadoId: e.id,
      regimen: "GENERAL",
      remuneracion: e.sueldo,
      asignacionFamiliar: false,
      sistemaPensionario: "ONP",
      horasSemanales: 48,
      banco: "Banco de Crédito",
      cuenta: "19" + String(Math.abs(hash(e.id)) % 1e10).padStart(10, "0"),
      cargo: e.cargo,
      fechaInicio: e.fechaIngreso || "2025-03-03",
    });
    if (r.status === 201 || r.status === 200) creados++;
    else
      console.log(
        "FALLO contrato",
        e.email,
        r.status,
        JSON.stringify(r.data).slice(0, 200),
      );
  }
  console.log("contratos creados:", creados);

  const sel = em.filter((e) => !/^qa\.medico/.test(e.email || "")).slice(0, 18);
  const dias = diasHabiles(6);
  const marcadas = L.E.get("marcaciones") || {};
  let n = 0;
  for (const e of sel) {
    for (let i = 0; i < dias.length; i++) {
      const dia = dias[i];
      const k = e.id + ":" + dia;
      if (marcadas[k]) continue;
      const tarde = (hash(e.id) + i) % 5 === 0;
      const entrada = dia + "T" + (tarde ? "08:27" : "07:5" + (i % 10)) + ":00";
      const salida = dia + "T17:0" + (i % 6) + ":00";
      const a = await L.api(RRHH, "POST", "/asistencia/marcaciones", {
        empleadoId: e.id,
        tipo: "ENTRADA",
        fechaHora: entrada,
        observacion: "Marcación de datos de ejemplo de QA",
      });
      const b = await L.api(RRHH, "POST", "/asistencia/marcaciones", {
        empleadoId: e.id,
        tipo: "SALIDA",
        fechaHora: salida,
        observacion: "Marcación de datos de ejemplo de QA",
      });
      if (
        [200, 201, 409].includes(a.status) &&
        [200, 201, 409].includes(b.status)
      ) {
        marcadas[k] = true;
        n++;
      } else
        console.log(
          "FALLO marcación",
          e.email,
          dia,
          a.status,
          b.status,
          JSON.stringify(a.data).slice(0, 160),
        );
    }
    L.E.set("marcaciones", marcadas);
  }
  console.log("días con marcación:", n);
  const c = await L.api(RRHH, "POST", "/asistencia/consolidar", {
    desde: dias[dias.length - 1],
    hasta: dias[0],
  });
  console.log("consolidar", c.status, JSON.stringify(c.data).slice(0, 200));
  console.log(JSON.stringify(L.resumen()));
}

function hash(s) {
  let h = 0;
  for (const c of s) h = (h * 31 + c.charCodeAt(0)) | 0;
  return h;
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
