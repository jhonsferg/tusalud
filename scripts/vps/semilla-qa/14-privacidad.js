const L = require("./lib");

const CAL = "calidad@tusalud.com";

const SOLICITUDES = [
  {
    k: "S1",
    pac: "juan.perez@correo.com",
    tipo: "ACCESO",
    desc: "Solicito una copia de todos los datos personales que la clínica conserva sobre mí",
    fin: "RECIBIDA",
  },
  {
    k: "S2",
    pac: "maria.rodriguez@correo.com",
    tipo: "ACCESO",
    desc: "Solicito conocer qué datos de mi historia clínica se han compartido con terceros",
    fin: "EN_REVISION",
  },
  {
    k: "S3",
    pac: "carlos.mendoza@correo.com",
    tipo: "ACCESO",
    desc: "Solicito mi información personal y el registro de quién la ha consultado",
    fin: "ATENDIDA",
  },
  {
    k: "S4",
    pac: "jorge.medina@correo.com",
    tipo: "ACCESO",
    desc: "Solicito acceso a mis datos de contacto y de facturación registrados",
    fin: "PARCIAL",
  },
  {
    k: "S5",
    pac: "camila.torres@correo.com",
    tipo: "ACCESO",
    desc: "Solicito los datos personales de mi hermana menor que figuran en mi cuenta",
    fin: "RECHAZADA",
  },
  {
    k: "S6",
    pac: "sofia.ramirez@correo.com",
    tipo: "RECTIFICACION",
    desc: "Mi número de teléfono cambió y necesito actualizarlo en la historia",
    campo: "TELEFONO",
    valor: "911223399",
    fin: "ATENDIDA",
  },
  {
    k: "S7",
    pac: "diego.herrera@correo.com",
    tipo: "CANCELACION",
    desc: "Solicito la cancelación de mis datos que no se necesiten por obligación legal",
    fin: "EN_REVISION",
  },
];

async function main() {
  for (const s of SOLICITUDES) {
    const reg = L.E.get("arco:" + s.k) || {};
    if (!reg.id) {
      const b = { tipo: s.tipo, descripcion: s.desc };
      if (s.campo) {
        b.campo = s.campo;
        b.valorPropuesto = s.valor;
      }
      const r = await L.api(s.pac, "POST", "/privacidad/solicitudes", b);
      if (r.status !== 201 && r.status !== 200) {
        console.log(
          "FALLO solicitud",
          s.k,
          r.status,
          JSON.stringify(r.data).slice(0, 300),
        );
        continue;
      }
      reg.id = r.data.id || (r.data.resumen && r.data.resumen.id);
      reg.codigo = r.data.codigo || r.data.numero;
      if (!reg.id)
        console.log("forma solicitud", Object.keys(r.data).join(","));
      L.E.set("arco:" + s.k, reg);
    }
    if (
      ["EN_REVISION", "ATENDIDA", "PARCIAL", "RECHAZADA"].includes(s.fin) &&
      !reg.tomada
    ) {
      const r = await L.api(
        CAL,
        "POST",
        "/privacidad/gestion/solicitudes/" + reg.id + "/tomar",
        {},
      );
      if (r.status === 200 || r.status === 201) reg.tomada = true;
      else
        console.log(
          "FALLO tomar",
          s.k,
          r.status,
          JSON.stringify(r.data).slice(0, 200),
        );
      L.E.set("arco:" + s.k, reg);
    }
    if (["ATENDIDA", "PARCIAL", "RECHAZADA"].includes(s.fin) && !reg.resuelta) {
      const decision =
        s.fin === "ATENDIDA"
          ? "ATENDER"
          : s.fin === "PARCIAL"
            ? "ATENDER_PARCIALMENTE"
            : "RECHAZAR";
      const texto =
        s.fin === "ATENDIDA"
          ? "Se adjunta la copia de sus datos personales y el registro de accesos"
          : s.fin === "PARCIAL"
            ? "Se entregan los datos de contacto, los datos de facturación se conservan por obligación tributaria"
            : "No se puede entregar información de terceros sin la autorización de su titular";
      const r = await L.api(
        CAL,
        "POST",
        "/privacidad/gestion/solicitudes/" + reg.id + "/resolver",
        { decision, resolucion: texto },
      );
      if (r.status === 200 || r.status === 201) reg.resuelta = s.fin;
      else
        console.log(
          "FALLO resolver",
          s.k,
          r.status,
          JSON.stringify(r.data).slice(0, 200),
        );
      L.E.set("arco:" + s.k, reg);
    }
    console.log(s.k, s.tipo, s.fin, JSON.stringify(reg));
  }

  const incidentes = [
    {
      k: "I1",
      body: {
        tipo: "ACCESO_NO_AUTORIZADO",
        severidad: "ALTA",
        descripcion:
          "Un usuario del personal consultó historias clínicas de pacientes sin relación asistencial",
        datosAfectados: "Historias clínicas",
        titularesAfectados: 3,
        detectadoEn: L.hace(30),
        contencion: "Se suspendió la cuenta mientras se revisa el caso",
      },
      fin: "EN_CONTENCION",
    },
    {
      k: "I2",
      body: {
        tipo: "PERDIDA",
        severidad: "MEDIA",
        descripcion:
          "Se extravió un listado impreso de citas del día con nombres y teléfonos",
        datosAfectados: "Nombres y teléfonos",
        titularesAfectados: 12,
        detectadoEn: L.hace(100),
        contencion: "Se recuperó el listado en el área de archivo",
      },
      fin: "CERRADO",
    },
  ];
  for (const i of incidentes) {
    const reg = L.E.get("incidente:" + i.k) || {};
    if (!reg.id) {
      const r = await L.api(
        CAL,
        "POST",
        "/privacidad/gestion/incidentes",
        i.body,
      );
      if (r.status !== 201 && r.status !== 200) {
        console.log(
          "FALLO incidente",
          i.k,
          r.status,
          JSON.stringify(r.data).slice(0, 300),
        );
        continue;
      }
      reg.id = r.data.id || (r.data.resumen && r.data.resumen.id);
      reg.codigo = r.data.codigo;
      L.E.set("incidente:" + i.k, reg);
    }
    if (i.fin !== "ABIERTO" && !reg.estado) {
      const r = await L.api(
        CAL,
        "PUT",
        "/privacidad/gestion/incidentes/" + reg.id,
        {
          estado: i.fin,
          contencion: i.body.contencion,
          causaRaiz:
            i.fin === "CERRADO"
              ? "Falta de una política de impresión de listados"
              : null,
          autoridadNotificada: false,
          titularesNotificados: false,
          responsableId: null,
        },
      );
      if (r.status === 200) reg.estado = i.fin;
      else
        console.log(
          "FALLO actualizar incidente",
          i.k,
          r.status,
          JSON.stringify(r.data).slice(0, 300),
        );
      L.E.set("incidente:" + i.k, reg);
    }
    console.log(i.k, JSON.stringify(reg));
  }
  console.log(JSON.stringify(L.resumen()));
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
