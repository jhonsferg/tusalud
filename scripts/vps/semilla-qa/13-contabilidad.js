const L = require("./lib");

const FIN = "finanzas@tusalud.com";
const FIN2 = "finanzas2@tusalud.com";
const GER = "gerencia@tusalud.com";

function idDe(d) {
  return (
    d && (d.id || (d.resumen && d.resumen.id) || (d.asiento && d.asiento.id))
  );
}

async function asiento(
  clave,
  usuario,
  fecha,
  glosa,
  lineas,
  accion,
  motivoReversa,
) {
  const reg = L.E.get("asiento:" + clave) || {};
  if (!reg.id) {
    const r = await L.api(usuario, "POST", "/contabilidad/asientos", {
      fecha,
      glosa,
      lineas,
    });
    if (r.status !== 201 && r.status !== 200) {
      console.log(
        "FALLO asiento",
        clave,
        r.status,
        JSON.stringify(r.data).slice(0, 300),
      );
      return reg;
    }
    reg.id = idDe(r.data);
    reg.numero = r.data.numero || (r.data.resumen && r.data.resumen.numero);
    if (!reg.id) console.log("forma asiento", Object.keys(r.data).join(","));
    L.E.set("asiento:" + clave, reg);
  }
  if (["CONTABILIZAR", "REVERSAR"].includes(accion) && !reg.contabilizado) {
    const r = await L.api(
      usuario,
      "POST",
      "/contabilidad/asientos/" + reg.id + "/contabilizar",
      {},
    );
    if (r.status === 200 || r.status === 201) {
      reg.contabilizado = true;
      reg.numero =
        r.data.numero ||
        (r.data.resumen && r.data.resumen.numero) ||
        reg.numero;
    } else
      console.log(
        "FALLO contabilizar",
        clave,
        r.status,
        JSON.stringify(r.data).slice(0, 300),
      );
    L.E.set("asiento:" + clave, reg);
  }
  if (accion === "REVERSAR" && reg.contabilizado && !reg.reversado) {
    const r = await L.api(
      usuario,
      "POST",
      "/contabilidad/asientos/" + reg.id + "/reversar",
      { motivo: motivoReversa },
    );
    if (r.status === 200 || r.status === 201) reg.reversado = true;
    else
      console.log(
        "FALLO reversar",
        clave,
        r.status,
        JSON.stringify(r.data).slice(0, 300),
      );
    L.E.set("asiento:" + clave, reg);
  }
  console.log("asiento", clave, JSON.stringify(reg));
  return reg;
}

async function main() {
  const cuentas = {};
  for (const c of L.lista(
    (await L.api(FIN, "GET", "/contabilidad/cuentas?size=200")).data,
  ))
    cuentas[c.codigo] = c.id;
  const per = L.lista((await L.api(FIN, "GET", "/contabilidad/periodos")).data);
  const hoy = L.fecha(0);
  const mesAct = Number(hoy.slice(5, 7));
  const anio = Number(hoy.slice(0, 4));
  const mesAnt = mesAct === 1 ? 12 : mesAct - 1;
  const anioAnt = mesAct === 1 ? anio - 1 : anio;
  let pAnt = per.find((p) => p.anio === anioAnt && p.mes === mesAnt);
  if (!pAnt) {
    const r = await L.api(FIN, "POST", "/contabilidad/periodos", {
      anio: anioAnt,
      mes: mesAnt,
    });
    console.log(
      "abrir periodo anterior",
      r.status,
      JSON.stringify(r.data).slice(0, 200),
    );
    pAnt = r.data;
  }
  const dAnt = anioAnt + "-" + String(mesAnt).padStart(2, "0") + "-";

  const L2 = (c, debe, haber) => ({
    cuentaId: cuentas[c],
    ...(debe ? { debe } : {}),
    ...(haber ? { haber } : {}),
  });
  await asiento(
    "AS-ALQUILER",
    FIN,
    dAnt + "05",
    "Pago del alquiler del local del mes",
    [L2("6311", 4500, 0), L2("1041", 0, 4500)],
    "CONTABILIZAR",
  );
  await asiento(
    "AS-EQUIPOS",
    FIN2,
    dAnt + "12",
    "Compra de equipos menores de oficina",
    [L2("33", 1800, 0), L2("1011", 0, 1800)],
    "CONTABILIZAR",
  );

  await asiento(
    "AS-SERVICIOS",
    FIN,
    L.fecha(-9),
    "Pago de servicios básicos de luz y agua",
    [L2("6311", 980, 0), L2("1011", 0, 980)],
    "CONTABILIZAR",
  );
  await asiento(
    "AS-GASTOS",
    FIN2,
    L.fecha(-6),
    "Gastos de gestión de mantenimiento del consultorio",
    [L2("6591", 640, 0), L2("1011", 0, 640)],
    "CONTABILIZAR",
  );
  await asiento(
    "AS-REVERSADO",
    FIN,
    L.fecha(-4),
    "Aporte de capital registrado en la cuenta equivocada",
    [L2("1041", 2500, 0), L2("5011", 0, 2500)],
    "REVERSAR",
    "Aporte registrado por error, se reversa para volver a contabilizarlo",
  );
  await asiento(
    "AS-BORRADOR",
    FIN,
    hoy,
    "Provisión de gastos de capacitación pendiente de revisión",
    [L2("6591", 1200, 0), L2("46", 0, 1200)],
    "BORRADOR",
  );
  await asiento(
    "AS-BORRADOR2",
    FIN2,
    hoy,
    "Ingreso por alquiler de consultorio a terceros pendiente",
    [L2("1011", 900, 0), L2("75", 0, 900)],
    "BORRADOR",
  );

  const pa = L.lista(
    (await L.api(GER, "GET", "/contabilidad/periodos")).data,
  ).find((p) => p.anio === anioAnt && p.mes === mesAnt);
  if (pa && pa.estado === "ABIERTO" && !L.E.get("periodo:cerrado")) {
    const r = await L.api(
      GER,
      "POST",
      "/contabilidad/periodos/" + pa.id + "/cerrar",
      {},
    );
    console.log(
      "cerrar periodo anterior",
      r.status,
      JSON.stringify(r.data).slice(0, 200),
    );
    if (r.status === 200)
      L.E.set("periodo:cerrado", { id: pa.id, etiqueta: pa.etiqueta });
  }

  const cta = L.lista(
    (await L.api(FIN, "GET", "/contabilidad/tesoreria/cuentas")).data,
  );
  const bcp = cta.find((c) => c.predeterminada) || cta[0];
  const caja = cta.find((c) => c.tipo === "CAJA");
  if (!L.E.get("tesoreria:aporte")) {
    const r = await L.api(FIN, "POST", "/contabilidad/tesoreria/movimientos", {
      cuentaId: bcp.id,
      fecha: hoy,
      tipo: "INGRESO",
      monto: 300000,
      descripcion: "Aporte de capital de los socios para capital de trabajo",
      referencia: "APORTE-QA-01",
      contrapartidaId: cuentas["5011"],
    });
    console.log("aporte", r.status, JSON.stringify(r.data).slice(0, 200));
    if (r.status === 201 || r.status === 200)
      L.E.set("tesoreria:aporte", { id: idDe(r.data) });
  }
  if (!L.E.get("tesoreria:egreso")) {
    const r = await L.api(FIN, "POST", "/contabilidad/tesoreria/movimientos", {
      cuentaId: bcp.id,
      fecha: hoy,
      tipo: "EGRESO",
      monto: 180.5,
      descripcion: "Comisiones y mantenimiento de la cuenta del banco",
      referencia: "COM-QA-01",
      contrapartidaId: cuentas["6591"],
    });
    console.log("egreso", r.status, JSON.stringify(r.data).slice(0, 200));
    if (r.status === 201 || r.status === 200)
      L.E.set("tesoreria:egreso", { id: idDe(r.data) });
  }
  if (caja && !L.E.get("tesoreria:cajaIngreso")) {
    const r = await L.api(FIN, "POST", "/contabilidad/tesoreria/movimientos", {
      cuentaId: caja.id,
      fecha: hoy,
      tipo: "INGRESO",
      monto: 1500,
      descripcion: "Fondo de caja chica para gastos menores",
      referencia: "FONDO-QA-01",
      contrapartidaId: cuentas["5011"],
    });
    console.log("fondo caja", r.status);
    if (r.status === 201 || r.status === 200)
      L.E.set("tesoreria:cajaIngreso", { id: idDe(r.data) });
  }

  if (!L.E.get("extracto:qa")) {
    const csv = [
      "fecha,descripcion,referencia,monto",
      hoy + ",Abono aporte de capital,APORTE-QA-01,300000.00",
      hoy + ",Comisiones y mantenimiento,COM-QA-01,-180.50",
      hoy + ",Cargo por transferencia interbancaria,,-35.00",
      hoy + ",Abono de cliente sin identificar,,412.30",
    ].join("\n");
    const r = await L.api(
      FIN,
      "POST",
      "/contabilidad/tesoreria/conciliacion/extractos",
      { cuentaId: bcp.id, archivo: "extracto-bcp-qa.csv", contenido: csv },
    );
    console.log("extracto", r.status, JSON.stringify(r.data).slice(0, 300));
    if (r.status === 201 || r.status === 200)
      L.E.set("extracto:qa", { archivo: "extracto-bcp-qa.csv" });
  }
  const sug = await L.api(
    FIN,
    "GET",
    "/contabilidad/tesoreria/conciliacion/" + bcp.id + "/sugerencias",
  );
  console.log(
    "sugerencias",
    sug.status,
    JSON.stringify(sug.data).slice(0, 500),
  );
  const conc = L.E.get("extracto:conciliado");
  if (!conc && sug.status === 200 && L.lista(sug.data).length) {
    const s = L.lista(sug.data)[0];
    const lineaId = s.lineaId || (s.linea && s.linea.id);
    const movId =
      s.movimientoId ||
      (s.movimiento && s.movimiento.id) ||
      (s.sugerencias &&
        s.sugerencias[0] &&
        (s.sugerencias[0].movimientoId || s.sugerencias[0].id));
    if (lineaId && movId) {
      const r = await L.api(
        FIN,
        "POST",
        "/contabilidad/tesoreria/conciliacion/conciliar",
        { lineaId, movimientoId: movId },
      );
      console.log("conciliar", r.status, JSON.stringify(r.data).slice(0, 200));
      if (r.status === 200) L.E.set("extracto:conciliado", { lineaId, movId });
    }
  }
  const rs = await L.api(
    FIN,
    "GET",
    "/contabilidad/tesoreria/conciliacion/" + bcp.id + "/resumen",
  );
  console.log(
    "resumen conciliación",
    rs.status,
    JSON.stringify(rs.data).slice(0, 300),
  );
  const fl = await L.api(
    GER,
    "GET",
    "/contabilidad/tesoreria/flujo-caja?semanas=8",
  );
  console.log(
    "flujo de caja (gerencia)",
    fl.status,
    JSON.stringify(fl.data).slice(0, 200),
  );
  console.log(JSON.stringify(L.resumen()));
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
