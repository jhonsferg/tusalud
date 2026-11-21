const L = require("./lib");

const CAJA = "caja@tusalud.com";
const FIN = "finanzas@tusalud.com";

async function cobrar(clave, cuerpo, usuario) {
  const prev = L.E.get("cobro:" + clave);
  if (prev) return prev;
  const r = await L.api(usuario || CAJA, "POST", "/caja/cobros", cuerpo);
  if (r.status !== 201 && r.status !== 200) {
    console.log(
      "FALLO cobro",
      clave,
      r.status,
      JSON.stringify(r.data).slice(0, 300),
    );
    return null;
  }
  const reg = {
    id: r.data.id,
    numero:
      r.data.numero ||
      (r.data.serie ? r.data.serie + "-" + r.data.correlativo : undefined),
    tipo: cuerpo.tipoComprobante,
    total: r.data.total,
    estado: r.data.estado,
    cliente: cuerpo.clienteNombre,
  };
  L.E.set("cobro:" + clave, reg);
  return reg;
}

async function main() {
  const act = await L.api(CAJA, "GET", "/caja/actual");
  console.log(
    "caja actual",
    act.status,
    JSON.stringify(act.data).slice(0, 200),
  );
  if (act.status === 404 || !act.data || !act.data.id) {
    const a = await L.api(CAJA, "POST", "/caja/aperturas", {
      montoApertura: 200,
    });
    console.log("apertura", a.status, JSON.stringify(a.data).slice(0, 160));
  }

  const pacientes = [
    ["juan", "juan.perez@correo.com"],
    ["maria", "maria.rodriguez@correo.com"],
    ["carlos", "carlos.mendoza@correo.com"],
    ["diego", "diego.herrera@correo.com"],
  ];
  for (const [k, email] of pacientes) {
    const per =
      L.E.get("perfil:" + email) || (await L.api(email, "GET", "/perfil")).data;
    L.E.set("perfil:" + email, { dni: per.dni, nombre: per.nombreCompleto });
    const enc = Object.entries(L.E.todo()).find(
      ([kk, v]) => kk.startsWith("enc:") && v.paciente === email,
    );
    const body = {
      origen: "SERVICIO",
      tipoComprobante: "BOLETA",
      clienteTipoDoc: "DNI",
      clienteDoc: per.dni,
      clienteNombre: per.nombreCompleto,
      metodoPago: k === "maria" ? "TRANSFERENCIA" : "EFECTIVO",
      items: [
        {
          descripcion: "Consulta ambulatoria",
          cantidad: 1,
          precioUnitario: 90 + (k === "juan" ? 60 : 0),
        },
      ],
    };
    if (enc) body.pacienteId = enc[1].pacienteId;
    const reg = await cobrar("boleta-" + k, body);
    if (!reg) break;
  }

  await cobrar("boleta-mostrador", {
    origen: "SERVICIO",
    tipoComprobante: "BOLETA",
    clienteTipoDoc: "SIN_DOC",
    clienteNombre: "Cliente de mostrador",
    metodoPago: "EFECTIVO",
    items: [
      { descripcion: "Curación simple", cantidad: 2, precioUnitario: 40 },
    ],
  });
  await cobrar("factura-aceptada", {
    origen: "SERVICIO",
    tipoComprobante: "FACTURA",
    clienteTipoDoc: "RUC",
    clienteDoc: "20100070970",
    clienteNombre: "Supermercados Peruanos SA",
    clienteDireccion: "Av. Industrial 500, Lima",
    metodoPago: "TRANSFERENCIA",
    items: [
      { descripcion: "Chequeo ocupacional", cantidad: 3, precioUnitario: 118 },
    ],
  });
  await cobrar("factura-observada", {
    origen: "SERVICIO",
    tipoComprobante: "FACTURA",
    clienteTipoDoc: "RUC",
    clienteDoc: "20100070970",
    clienteNombre: "Distribuidora Andina SAC",
    metodoPago: "TRANSFERENCIA",
    items: [
      { descripcion: "Examen de aptitud", cantidad: 1, precioUnitario: 118 },
    ],
  });
  await cobrar("factura-rechazada", {
    origen: "SERVICIO",
    tipoComprobante: "FACTURA",
    clienteTipoDoc: "RUC",
    clienteDoc: "20123456789",
    clienteNombre: "Empresa con RUC errado SAC",
    metodoPago: "TRANSFERENCIA",
    items: [
      {
        descripcion: "Examen de laboratorio ocupacional",
        cantidad: 1,
        precioUnitario: 236,
      },
    ],
  });
  const anul = await cobrar("boleta-anulada", {
    origen: "SERVICIO",
    tipoComprobante: "BOLETA",
    clienteTipoDoc: "SIN_DOC",
    clienteNombre: "Cliente de mostrador",
    metodoPago: "EFECTIVO",
    items: [
      {
        descripcion: "Servicio cobrado por error",
        cantidad: 1,
        precioUnitario: 40,
      },
    ],
  });
  if (anul && !anul.anulada) {
    const a = await L.api(
      CAJA,
      "PUT",
      "/caja/comprobantes/" + anul.id + "/anular",
      { motivo: "Cobro duplicado por error de digitación" },
    );
    if (a.status === 200) {
      anul.anulada = true;
      L.E.set("cobro:boleta-anulada", anul);
    } else
      console.log("anular", a.status, JSON.stringify(a.data).slice(0, 200));
  }

  const q = await L.api(FIN, "POST", "/facturacion/cola/procesar", {});
  console.log("procesar cola", q.status, JSON.stringify(q.data).slice(0, 200));
  await L.dormir(12000);
  const q2 = await L.api(FIN, "POST", "/facturacion/cola/procesar", {});
  console.log(
    "procesar cola",
    q2.status,
    JSON.stringify(q2.data).slice(0, 200),
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
  ]) {
    const reg = L.E.get("cobro:" + k);
    if (!reg) continue;
    const d = await L.api(
      FIN,
      "GET",
      "/facturacion/documentos/por-comprobante/" + reg.id,
    );
    reg.cpe =
      d.status === 200
        ? {
            id: d.data.id,
            numero: d.data.numero || d.data.serie + "-" + d.data.correlativo,
            estado: d.data.estado,
          }
        : { error: d.status };
    L.E.set("cobro:" + k, reg);
    console.log(k, JSON.stringify(reg.cpe));
  }

  const fa = L.E.get("cobro:factura-aceptada");
  if (
    fa &&
    fa.cpe &&
    fa.cpe.id &&
    !fa.nota &&
    /ACEPT/.test(fa.cpe.estado || "")
  ) {
    const n = await L.api(FIN, "POST", "/facturacion/notas", {
      documentoId: fa.cpe.id,
      tipo: "NOTA_CREDITO",
      motivoCodigo: "09",
      descripcion: "Disminución en el valor por descuento comercial",
      items: [
        { descripcion: "Descuento comercial", cantidad: 1, precioUnitario: 50 },
      ],
    });
    console.log(
      "nota de crédito",
      n.status,
      JSON.stringify(n.data).slice(0, 200),
    );
    if (n.status === 200 || n.status === 201) {
      fa.nota = n.data.numero || n.data.id;
      L.E.set("cobro:factura-aceptada", fa);
    }
  }
  console.log(JSON.stringify(L.resumen()));
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
