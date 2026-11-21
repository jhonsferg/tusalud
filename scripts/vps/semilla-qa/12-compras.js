const L = require("./lib");

const FARM = "farmacia@tusalud.com";
const ALM = "almacen@tusalud.com";
const ALM2 = "almacen2@tusalud.com";
const FIN = "finanzas@tusalud.com";
const FIN3 = "finanzas3@tusalud.com";
const GER = "gerencia@tusalud.com";

function idDe(d) {
  if (!d) return undefined;
  return (
    d.id ||
    (d.resumen && d.resumen.id) ||
    (d.orden && d.orden.id) ||
    (d.requisicion && d.requisicion.id)
  );
}

async function paso(clave, reg, nombre, usuario, metodo, ruta, cuerpo) {
  if (reg["p_" + nombre]) return reg["p_" + nombre];
  const r = await L.api(usuario, metodo, ruta, cuerpo);
  if (r.status >= 200 && r.status < 300) {
    reg["p_" + nombre] = true;
    L.E.set("compra:" + clave, reg);
    return r;
  }
  console.log(
    "FALLO",
    clave,
    nombre,
    r.status,
    JSON.stringify(r.data).slice(0, 300),
  );
  return null;
}

async function main() {
  const prods = {};
  for (const p of L.lista(
    (await L.api(ALM, "GET", "/almacen/productos?size=100")).data,
  ))
    prods[p.codigo] = p;
  const provs = L.lista(
    (await L.api(ALM, "GET", "/almacen/proveedores?size=50")).data,
  ).filter((p) => p.activo !== false);
  console.log(
    "productos",
    Object.keys(prods).length,
    "proveedores",
    provs.length,
    provs.map((p) => p.razonSocial).join(" | "),
  );
  const cfg = await L.api(FIN, "GET", "/compras/configuracion");
  console.log("configuración", JSON.stringify(cfg.data).slice(0, 200));
  const vence = L.fecha(400);

  const FLUJOS = [
    {
      k: "OC1",
      items: [
        ["MED-PARA500", 400, 0.18, 0.15],
        ["MED-IBU400", 300, 0.3, 0.26],
      ],
      hasta: "RECIBIDA_TOTAL",
      factura: true,
      motivo: "Reposición mensual de analgésicos",
    },
    {
      k: "OC2",
      items: [
        ["MED-AMOX500", 300, 0.65, 0.58],
        ["MED-OMEP20", 200, 0.4, 0.36],
      ],
      hasta: "RECIBIDA_PARCIAL",
      motivo: "Reposición de antibióticos y protectores gástricos",
    },
    {
      k: "OC3",
      items: [["MED-LOSA50", 500, 0.3, 0.27]],
      hasta: "ENVIADA_APROBACION",
      motivo: "Reposición de antihipertensivos",
    },
    {
      k: "OC4",
      items: [["MED-METF850", 300, 0.25, 0.22]],
      hasta: "APROBADA",
      motivo: "Reposición de antidiabéticos",
    },
  ];
  for (const f of FLUJOS) {
    const reg = L.E.get("compra:" + f.k) || {};
    if (!reg.reqId) {
      const r = await L.api(FARM, "POST", "/compras/requisiciones", {
        prioridad: f.k === "OC1" ? "URGENTE" : "NORMAL",
        motivo: f.motivo,
        sugerida: false,
        items: f.items.map((i) => ({
          productoId: prods[i[0]].id,
          cantidad: i[1],
          observacion: "Stock bajo",
        })),
      });
      if (r.status !== 201 && r.status !== 200) {
        console.log(
          "FALLO requisición",
          f.k,
          r.status,
          JSON.stringify(r.data).slice(0, 300),
        );
        continue;
      }
      reg.reqId = idDe(r.data);
      reg.reqNumero =
        r.data.numero ||
        r.data.codigo ||
        (r.data.resumen && r.data.resumen.numero);
      if (!reg.reqId)
        console.log("forma requisición", Object.keys(r.data).join(","));
      L.E.set("compra:" + f.k, reg);
    }
    await paso(
      f.k,
      reg,
      "enviarReq",
      FARM,
      "POST",
      "/compras/requisiciones/" + reg.reqId + "/enviar",
      {},
    );
    if (!reg.cot2) {
      const pv = [
        provs[(f.k.charCodeAt(2) + 0) % provs.length],
        provs[(f.k.charCodeAt(2) + 1) % provs.length],
      ];
      const ids = [];
      for (let j = 0; j < 2; j++) {
        const r = await L.api(
          ALM,
          "POST",
          "/compras/requisiciones/" + reg.reqId + "/cotizaciones",
          {
            proveedorId: pv[j].id,
            vigenciaHasta: L.fecha(20),
            plazoEntregaDias: 3 + j,
            condicionPagoDias: j === 0 ? 30 : 15,
            items: f.items.map((i) => ({
              productoId: prods[i[0]].id,
              precioUnitario: j === 0 ? i[3] : i[2],
            })),
          },
        );
        if (r.status >= 200 && r.status < 300)
          ids.push(idDe(r.data) || (r.data.resumen && r.data.resumen.id));
        else
          console.log(
            "FALLO cotización",
            f.k,
            j,
            r.status,
            JSON.stringify(r.data).slice(0, 300),
          );
      }
      if (ids.length === 2) {
        reg.cot1 = ids[0];
        reg.cot2 = ids[1];
        L.E.set("compra:" + f.k, reg);
      } else continue;
    }
    await paso(
      f.k,
      reg,
      "seleccionar",
      ALM,
      "POST",
      "/compras/cotizaciones/" + reg.cot1 + "/seleccionar",
      {},
    );
    if (!reg.ordenId) {
      const r = await L.api(
        ALM,
        "POST",
        "/compras/ordenes/desde-cotizacion/" + reg.cot1,
        {},
      );
      if (r.status >= 200 && r.status < 300) {
        reg.ordenId = idDe(r.data);
        reg.ordenNumero =
          r.data.numero ||
          r.data.codigo ||
          (r.data.resumen && r.data.resumen.numero);
        if (!reg.ordenId)
          console.log("forma orden", Object.keys(r.data).join(","));
        L.E.set("compra:" + f.k, reg);
      } else {
        console.log(
          "FALLO orden",
          f.k,
          r.status,
          JSON.stringify(r.data).slice(0, 300),
        );
        continue;
      }
    }
    if (
      [
        "ENVIADA_APROBACION",
        "APROBADA",
        "RECIBIDA_PARCIAL",
        "RECIBIDA_TOTAL",
      ].includes(f.hasta)
    )
      await paso(
        f.k,
        reg,
        "enviarAprob",
        ALM,
        "POST",
        "/compras/ordenes/" + reg.ordenId + "/enviar-aprobacion",
        {},
      );
    if (["APROBADA", "RECIBIDA_PARCIAL", "RECIBIDA_TOTAL"].includes(f.hasta))
      await paso(
        f.k,
        reg,
        "aprobar",
        FIN,
        "POST",
        "/compras/ordenes/" + reg.ordenId + "/aprobar",
        {},
      );
    if (["RECIBIDA_PARCIAL", "RECIBIDA_TOTAL"].includes(f.hasta)) {
      const det = await L.api(ALM2, "GET", "/compras/ordenes/" + reg.ordenId);
      const items =
        det.data.items || (det.data.orden && det.data.orden.items) || [];
      reg.total =
        det.data.total || (det.data.resumen && det.data.resumen.total);
      reg.subtotal =
        det.data.subtotal || (det.data.resumen && det.data.resumen.subtotal);
      reg.igv = det.data.igv || (det.data.resumen && det.data.resumen.igv);
      const parcial = f.hasta === "RECIBIDA_PARCIAL";
      const cuerpo = {
        guiaRemision: "T001-" + String(reg.ordenId).slice(0, 6).toUpperCase(),
        items: items
          .map((it, i) => ({
            ordenItemId: it.id,
            cantidadRecibida:
              parcial && i === 0
                ? Math.floor(it.cantidad / 2)
                : parcial
                  ? 0
                  : it.cantidad,
            cantidadRechazada: 0,
            ubicacion: "Anaquel B",
            numeroLote: "QA-" + f.k + "-" + i,
            fechaVencimiento: vence,
          }))
          .filter((x) => x.cantidadRecibida > 0),
      };
      await paso(
        f.k,
        reg,
        "recibir",
        ALM2,
        "POST",
        "/compras/ordenes/" + reg.ordenId + "/recepciones",
        cuerpo,
      );
    }
    if (f.factura && reg.p_recibir && !reg.comprobanteId) {
      const det = await L.api(FIN, "GET", "/compras/ordenes/" + reg.ordenId);
      const d = det.data.resumen || det.data;
      const prov =
        provs.find(
          (p) =>
            p.id ===
            (d.proveedorId || (det.data.proveedor && det.data.proveedor.id)),
        ) || provs[0];
      const subtotal = Number(
        d.subtotal || (Number(d.total) / 1.18).toFixed(2),
      );
      const igv = Number(d.igv || (Number(d.total) - subtotal).toFixed(2));
      const r = await L.api(FIN3, "POST", "/compras/comprobantes", {
        proveedorId: prov.id,
        ordenId: reg.ordenId,
        tipo: "FACTURA",
        serie: "F001",
        numero: String(7000 + Math.floor(Math.random() * 900)),
        rucEmisor: prov.ruc,
        fechaEmision: L.fecha(-3),
        subtotal,
        igv,
      });
      if (r.status >= 200 && r.status < 300) {
        reg.comprobanteId =
          idDe(r.data) || (r.data.resumen && r.data.resumen.id);
        reg.comprobanteTotal =
          r.data.total || (r.data.resumen && r.data.resumen.total);
        L.E.set("compra:" + f.k, reg);
      } else
        console.log(
          "FALLO comprobante",
          f.k,
          r.status,
          JSON.stringify(r.data).slice(0, 300),
        );
    }
    if (reg.comprobanteId && !reg.p_pago) {
      await paso(
        f.k,
        reg,
        "pago",
        FIN,
        "POST",
        "/compras/comprobantes/" + reg.comprobanteId + "/pagos",
        { monto: 40, metodo: "TRANSFERENCIA", referencia: "OP-QA-001" },
      );
    }
    console.log(f.k, JSON.stringify(reg));
  }

  const rec = L.E.get("compra:OC5") || {};
  if (!rec.ordenId) {
    const r = await L.api(ALM, "POST", "/compras/ordenes", {
      proveedorId: provs[0].id,
      afectaIgv: true,
      condicionPagoDias: 30,
      observaciones: "Compra grande de equipamiento para el almacén",
      items: [
        {
          productoId: prods["MED-PARA500"].id,
          cantidad: 10000,
          precioUnitario: 0.9,
        },
        {
          productoId: prods["MED-IBU400"].id,
          cantidad: 8000,
          precioUnitario: 0.8,
        },
        {
          productoId: prods["MED-AMOX500"].id,
          cantidad: 6000,
          precioUnitario: 1.1,
        },
      ],
    });
    if (r.status >= 200 && r.status < 300) {
      rec.ordenId = idDe(r.data);
      L.E.set("compra:OC5", rec);
    } else
      console.log("FALLO OC5", r.status, JSON.stringify(r.data).slice(0, 300));
  }
  if (rec.ordenId) {
    await paso(
      "OC5",
      rec,
      "enviarAprob",
      ALM,
      "POST",
      "/compras/ordenes/" + rec.ordenId + "/enviar-aprobacion",
      {},
    );
    await paso(
      "OC5",
      rec,
      "rechazar",
      GER,
      "POST",
      "/compras/ordenes/" + rec.ordenId + "/rechazar",
      {
        motivo: "Excede el presupuesto del mes, dividir la compra en entregas",
      },
    );
  }
  const bor = L.E.get("compra:OC6") || {};
  if (!bor.ordenId) {
    const r = await L.api(ALM2, "POST", "/compras/ordenes", {
      proveedorId: provs[1].id,
      afectaIgv: true,
      condicionPagoDias: 15,
      observaciones: "Borrador pendiente de enviar",
      items: [
        {
          productoId: prods["MED-LORA10"].id,
          cantidad: 200,
          precioUnitario: 0.2,
        },
      ],
    });
    if (r.status >= 200 && r.status < 300) {
      bor.ordenId = idDe(r.data);
      L.E.set("compra:OC6", bor);
    } else
      console.log("FALLO OC6", r.status, JSON.stringify(r.data).slice(0, 300));
  }
  const anu = L.E.get("compra:OC7") || {};
  if (!anu.ordenId) {
    const r = await L.api(ALM2, "POST", "/compras/ordenes", {
      proveedorId: provs[2].id,
      items: [
        {
          productoId: prods["MED-ENAL10"].id,
          cantidad: 100,
          precioUnitario: 0.25,
        },
      ],
    });
    if (r.status >= 200 && r.status < 300) {
      anu.ordenId = idDe(r.data);
      L.E.set("compra:OC7", anu);
    }
  }
  if (anu.ordenId)
    await paso(
      "OC7",
      anu,
      "anular",
      ALM2,
      "POST",
      "/compras/ordenes/" + anu.ordenId + "/anular",
      { motivo: "Orden duplicada" },
    );

  const lst = await L.api(FIN, "GET", "/compras/ordenes?size=50");
  const por = {};
  L.lista(lst.data).forEach((o) => (por[o.estado] = (por[o.estado] || 0) + 1));
  console.log("órdenes por estado", JSON.stringify(por));
  console.log(JSON.stringify(L.resumen()));
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
