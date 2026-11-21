const L = require("./lib");

const CALIDAD = "calidad@tusalud.com";

function haceLocal(h) {
  const d = new Date(Date.now() - h * 3600000);
  return d.toISOString().slice(0, 19);
}

const EVENTOS = [
  {
    k: "E01",
    quien: "enfermeria@tusalud.com",
    tipo: "EVENTO_ADVERSO",
    sev: "MODERADO",
    area: "Hospitalización",
    desc: "Caída de un paciente al levantarse de la cama sin acompañante, sin lesión grave",
    h: 120,
    destino: "CERRADO",
  },
  {
    k: "E02",
    quien: "enfermeria2@tusalud.com",
    tipo: "CASI_INCIDENTE",
    sev: "LEVE",
    area: "Farmacia",
    desc: "Etiqueta equivocada de un frasco detectada a tiempo antes de entregar al paciente",
    h: 96,
    destino: "EN_ANALISIS",
  },
  {
    k: "E03",
    quien: "enfermeria@tusalud.com",
    tipo: "CASI_INCIDENTE",
    sev: "LEVE",
    area: "Emergencia",
    desc: "Reporte anónimo: se encontró un carro de paro sin el sello de seguridad intacto",
    h: 72,
    anonimo: true,
    destino: "NOTIFICADO",
  },
  {
    k: "E04",
    quien: "farmacia@tusalud.com",
    tipo: "INCIDENTE_SIN_DANO",
    sev: "LEVE",
    area: "Farmacia",
    desc: "Se dispensó una presentación distinta a la recetada, corregida antes de que el paciente se retirara",
    h: 60,
    destino: "NOTIFICADO",
  },
  {
    k: "E05",
    quien: "dr.garcia@tusalud.com",
    tipo: "EVENTO_ADVERSO",
    sev: "GRAVE",
    area: "Hospitalización",
    desc: "Reacción adversa a un medicamento con rash generalizado y necesidad de observación",
    h: 48,
    destino: "EN_ACCION",
  },
  {
    k: "E06",
    quien: "lab@tusalud.com",
    tipo: "CASI_INCIDENTE",
    sev: "MODERADO",
    area: "Laboratorio",
    desc: "Muestra rotulada con el nombre de otro paciente detectada al momento del procesamiento",
    h: 40,
    destino: "EN_ANALISIS",
  },
  {
    k: "E07",
    quien: "enfermeria3@tusalud.com",
    tipo: "EVENTO_ADVERSO",
    sev: "GRAVE",
    area: "Quirófano",
    desc: "Discrepancia en el conteo de gasas al cierre de una cirugía, resuelta con radiografía de control",
    h: 30,
    destino: "EN_ACCION_HECHA",
  },
  {
    k: "E08",
    quien: "imagenes@tusalud.com",
    tipo: "CASI_INCIDENTE",
    sev: "LEVE",
    area: "Imágenes",
    desc: "Orden de radiografía con lateralidad equivocada identificada antes de realizar el estudio",
    h: 20,
    destino: "NOTIFICADO",
  },
  {
    k: "E09",
    quien: "enfermeria2@tusalud.com",
    tipo: "INCIDENTE_SIN_DANO",
    sev: "MODERADO",
    area: "Emergencia",
    desc: "Bomba de infusión con alarma silenciada, detectada en la ronda y sin consecuencias para el paciente",
    h: 10,
    destino: "NOTIFICADO",
  },
  {
    k: "E10",
    quien: "caja@tusalud.com",
    tipo: "CASI_INCIDENTE",
    sev: "LEVE",
    area: "Admisión",
    desc: "Dos pacientes con el mismo apellido y nombre casi intercambian sus historias en el mostrador",
    h: 5,
    destino: "NOTIFICADO",
  },
];

async function main() {
  const calidadId = (await L.loginCompleto(CALIDAD)).id;
  const gerenciaOk = await L.api(
    "gerencia@tusalud.com",
    "GET",
    "/seguridad-paciente/tablero",
  );
  console.log("tablero gerencia", gerenciaOk.status);

  for (const e of EVENTOS) {
    let reg = L.E.get("evento:" + e.k) || {};
    if (!reg.id) {
      const r = await L.api(e.quien, "POST", "/seguridad-paciente/eventos", {
        tipo: e.tipo,
        severidad: e.sev,
        area: e.area,
        descripcion: e.desc,
        anonimo: !!e.anonimo,
        ocurridoEn: L.hace(e.h),
      });
      if (r.status !== 201 && r.status !== 200) {
        console.log(
          "FALLO evento",
          e.k,
          e.quien,
          r.status,
          JSON.stringify(r.data).slice(0, 200),
        );
        continue;
      }
      reg = {
        id: r.data.id,
        codigo: r.data.codigo || r.data.numero,
        quien: e.quien,
        destino: e.destino,
        anonimo: !!e.anonimo,
        area: e.area,
        tipo: e.tipo,
        severidad: e.sev,
      };
      L.E.set("evento:" + e.k, reg);
    }
    if (
      ["EN_ANALISIS", "EN_ACCION", "EN_ACCION_HECHA", "CERRADO"].includes(
        e.destino,
      ) &&
      !reg.analisis
    ) {
      const a = await L.api(
        CALIDAD,
        "POST",
        "/seguridad-paciente/eventos/" + reg.id + "/analisis",
        {
          metodo: "CINCO_PORQUES",
          causas: [
            {
              categoria: "PORQUE",
              texto: "No existía una verificación doble en el proceso",
            },
            {
              categoria: "PORQUE",
              texto: "La carga de trabajo del turno era alta",
            },
          ],
          conclusion:
            "Falta de una barrera de verificación en el proceso del servicio",
          clasificacion: e.sev === "LEVE" ? "Sin daño" : "Con daño",
        },
      );
      if (a.status === 200 || a.status === 201) reg.analisis = true;
      else
        console.log(
          "FALLO análisis",
          e.k,
          a.status,
          JSON.stringify(a.data).slice(0, 200),
        );
      L.E.set("evento:" + e.k, reg);
    }
    if (
      ["EN_ACCION", "EN_ACCION_HECHA", "CERRADO"].includes(e.destino) &&
      reg.analisis &&
      !reg.accionId
    ) {
      const a = await L.api(
        CALIDAD,
        "POST",
        "/seguridad-paciente/eventos/" + reg.id + "/acciones",
        {
          descripcion:
            "Implementar una verificación doble antes de continuar el proceso",
          responsableId: calidadId,
          fechaLimite: L.fecha(30),
        },
      );
      if (a.status === 200 || a.status === 201) reg.accionId = "pendiente";
      else
        console.log(
          "FALLO acción",
          e.k,
          a.status,
          JSON.stringify(a.data).slice(0, 200),
        );
      L.E.set("evento:" + e.k, reg);
    }
    if (
      ["EN_ACCION_HECHA", "CERRADO"].includes(e.destino) &&
      reg.accionId &&
      !reg.accionHecha
    ) {
      const det = await L.api(
        CALIDAD,
        "GET",
        "/seguridad-paciente/eventos/" + reg.id,
      );
      const acc =
        (det.data.acciones || []).find((x) => x.estado === "PENDIENTE") ||
        (det.data.acciones || [])[0];
      if (acc) reg.accionId = acc.id;
      const c = await L.api(
        CALIDAD,
        "POST",
        "/seguridad-paciente/acciones/" + reg.accionId + "/completar",
        {},
      );
      if (c.status === 200 || c.status === 201) reg.accionHecha = true;
      else
        console.log(
          "FALLO completar",
          e.k,
          c.status,
          JSON.stringify(c.data).slice(0, 200),
        );
      L.E.set("evento:" + e.k, reg);
    }
    if (e.destino === "CERRADO" && reg.accionHecha && !reg.cerrado) {
      const c = await L.api(
        CALIDAD,
        "POST",
        "/seguridad-paciente/eventos/" + reg.id + "/cierre",
        {
          verificacion: "Sin nuevas caídas durante tres meses de seguimiento",
          accionEficaz: true,
        },
      );
      if (c.status === 200 || c.status === 201) reg.cerrado = true;
      else
        console.log(
          "FALLO cierre",
          e.k,
          c.status,
          JSON.stringify(c.data).slice(0, 200),
        );
      L.E.set("evento:" + e.k, reg);
    }
    console.log(
      e.k,
      e.destino,
      JSON.stringify({
        id: reg.id,
        analisis: reg.analisis,
        accion: !!reg.accionId,
        hecha: reg.accionHecha,
        cerrado: reg.cerrado,
      }),
    );
  }
  const l = await L.api(CALIDAD, "GET", "/seguridad-paciente/eventos?size=50");
  const lista = L.lista(l.data);
  const por = {};
  lista.forEach((x) => (por[x.estado] = (por[x.estado] || 0) + 1));
  console.log("eventos en total", lista.length, JSON.stringify(por));
  console.log(JSON.stringify(L.resumen()));
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
