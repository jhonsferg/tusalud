const fs = require("fs");
const L = require("./lib");

const FIRMA = fs.readFileSync(L.DIR + "/firma-ejemplo.txt", "utf8").trim();

function enc(k) {
  return L.E.get("enc:" + k);
}

async function dniDe(email) {
  const k = "dni:" + email;
  if (L.E.get(k)) return L.E.get(k);
  const r = await L.api(email, "GET", "/perfil");
  L.E.set(k, r.data.dni);
  return r.data.dni;
}

async function crearEmitir(clave, medico, cuerpo) {
  const prev = L.E.get("doc:" + clave) || {};
  if (prev.emitido) return prev;
  if (!prev.id) {
    const r = await L.api(medico, "POST", "/documentos-medicos", cuerpo);
    if (r.status !== 201 && r.status !== 200) {
      console.log(
        "FALLO crear",
        clave,
        r.status,
        JSON.stringify(r.data).slice(0, 200),
      );
      return prev;
    }
    prev.id = (r.data.resumen && r.data.resumen.id) || r.data.id;
    if (!prev.id) {
      const lst = L.lista(
        (
          await L.api(
            medico,
            "GET",
            "/documentos-medicos?pacienteId=" + cuerpo.pacienteId,
          )
        ).data,
      ).filter((d) => d.tipo === cuerpo.tipo && d.estado === "BORRADOR");
      prev.id = lst.length ? lst[0].id : undefined;
      console.log(
        "forma de la respuesta de creación:",
        Object.keys(r.data || {}).join(","),
      );
    }
    L.E.set("doc:" + clave, prev);
  }
  const e = await L.api(
    medico,
    "POST",
    "/documentos-medicos/" + prev.id + "/emitir",
    {},
  );
  if (e.status === 200 || e.status === 201) {
    prev.emitido = true;
    prev.numero = e.data.numero || e.data.codigo;
    prev.codigo = e.data.codigoVerificacion || e.data.codigo;
  } else {
    console.log(
      "FALLO emitir",
      clave,
      e.status,
      JSON.stringify(e.data).slice(0, 200),
    );
  }
  L.E.set("doc:" + clave, prev);
  return prev;
}

async function main() {
  const hoy = L.fecha(0);
  const M = (k) => enc(k) && enc(k).medico;
  const P = (k) => enc(k) && enc(k).pacienteId;

  await crearEmitir("cert-carlos", M("R03"), {
    pacienteId: P("R03"),
    tipo: "CERTIFICADO",
    autorizaDiagnostico: false,
    contenido: {
      motivo: "Atención ambulatoria por control de presión arterial",
      vigenciaDias: 30,
    },
  });
  await crearEmitir("cert-jorge", M("R14"), {
    pacienteId: P("R14"),
    tipo: "CERTIFICADO",
    autorizaDiagnostico: true,
    atencionId: enc("R14").atencionId,
    contenido: {
      motivo: "Control de diabetes mellitus tipo 2",
      vigenciaDias: 30,
    },
  });
  await crearEmitir("cert-ana", M("R18"), {
    pacienteId: P("R18"),
    tipo: "CERTIFICADO",
    autorizaDiagnostico: false,
    contenido: { motivo: "Atención por cefalea", vigenciaDias: 7 },
  });

  const d1 = await crearEmitir("descanso-juan", M("R02"), {
    pacienteId: P("R02"),
    tipo: "DESCANSO",
    autorizaDiagnostico: false,
    contenido: {
      tipoDescanso: "ENFERMEDAD",
      desde: hoy,
      hasta: L.fecha(2),
      contingencia: "Enfermedad común",
      indicacion: "Reposo absoluto en domicilio",
    },
  });
  await crearEmitir("descanso-diego", M("R10"), {
    pacienteId: P("R10"),
    tipo: "DESCANSO",
    autorizaDiagnostico: false,
    contenido: {
      tipoDescanso: "ENFERMEDAD",
      desde: hoy,
      hasta: L.fecha(6),
      contingencia: "Accidente común",
      indicacion: "Reposo relativo con la rodilla en alto",
    },
  });
  const dl = await crearEmitir("descanso-luis", M("R12"), {
    pacienteId: P("R12"),
    tipo: "DESCANSO",
    autorizaDiagnostico: false,
    contenido: {
      tipoDescanso: "ENFERMEDAD",
      desde: hoy,
      hasta: L.fecha(3),
      contingencia: "Enfermedad común",
      indicacion: "Evitar esfuerzos y cargar peso",
    },
  });
  if (dl.emitido && !dl.anulado) {
    const a = await L.api(
      M("R12"),
      "POST",
      "/documentos-medicos/" + dl.id + "/anular",
      { motivo: "Fechas equivocadas en el descanso" },
    );
    if (a.status === 200) {
      dl.anulado = true;
      L.E.set("doc:descanso-luis", dl);
    }
  }
  if (dl.anulado) {
    await crearEmitir("descanso-luis-reemplazo", M("R12"), {
      pacienteId: P("R12"),
      tipo: "DESCANSO",
      autorizaDiagnostico: false,
      documentoOrigenId: dl.id,
      contenido: {
        tipoDescanso: "ENFERMEDAD",
        desde: hoy,
        hasta: L.fecha(4),
        contingencia: "Enfermedad común",
        indicacion: "Evitar esfuerzos y cargar peso",
      },
    });
  }

  const est = L.lista(
    (await L.api(M("R10"), "GET", "/establecimientos-salud")).data,
  ).filter((e) => e.activo !== false);
  const destino = est[0];
  const ref = await crearEmitir("referencia-diego", M("R10"), {
    pacienteId: P("R10"),
    tipo: "REFERENCIA",
    autorizaDiagnostico: false,
    contenido: {
      destinoId: destino.id,
      especialidad: "Traumatología",
      prioridad: "PREFERENTE",
      motivo: "Evaluación por resonancia de rodilla",
      resumenClinico:
        "Dolor de rodilla tras lesión deportiva, sin mejoría con analgésicos",
    },
  });
  if (ref.emitido && !ref.recibida) {
    const r = await L.api(
      M("R10"),
      "PUT",
      "/documentos-medicos/" + ref.id + "/referencia/estado",
      { estado: "RECIBIDA" },
    );
    if (r.status === 200) {
      ref.recibida = true;
      L.E.set("doc:referencia-diego", ref);
    }
  }
  if (ref.recibida) {
    const refDet = await L.api(
      M("R10"),
      "GET",
      "/documentos-medicos/" + ref.id,
    );
    const refId = refDet.data.resumen.referencia.id;
    await crearEmitir("contrarreferencia-diego", M("R10"), {
      pacienteId: P("R10"),
      tipo: "CONTRARREFERENCIA",
      autorizaDiagnostico: false,
      referenciaId: refId,
      contenido: {
        resumen:
          "Se atendió en el establecimiento de destino y se indicó fisioterapia y seguimiento ambulatorio",
      },
    });
  }
  await crearEmitir("referencia-maria", M("R08"), {
    pacienteId: P("R08"),
    tipo: "REFERENCIA",
    autorizaDiagnostico: false,
    contenido: {
      destinoId: destino.id,
      especialidad: "Gastroenterología",
      prioridad: "PREFERENTE",
      motivo: "Endoscopía digestiva alta",
      resumenClinico: "Gastritis con molestias persistentes",
    },
  });

  const plantillas = L.lista(
    (await L.api("dr.garcia@tusalud.com", "GET", "/consentimientos/plantillas"))
      .data,
  ).filter((p) => p.activa !== false);
  const pl = (cod) =>
    plantillas.find((p) => p.codigo === cod) ||
    plantillas.find((p) =>
      (p.nombre || "").toLowerCase().includes(cod.toLowerCase()),
    ) ||
    plantillas[0];
  console.log(
    "plantillas:",
    plantillas.map((p) => p.codigo + (p.requiereTestigo ? "*" : "")).join(" "),
  );

  const casos = [
    {
      k: "cons-luis",
      enc: "R12",
      pl: pl("PEQUENA_CIRUGIA"),
      proc: "Infiltración de rodilla con corticoide",
      firmar: true,
    },
    {
      k: "cons-maria",
      enc: "R08",
      pl: pl("ENDOSCOPIA"),
      proc: "Endoscopía digestiva alta",
      firmar: true,
    },
    {
      k: "cons-jorge",
      enc: "R14",
      pl: pl("PROCEDIMIENTO_INVASIVO"),
      proc: "Toma de muestra de sangre arterial",
      firmar: true,
      revocar: true,
    },
    {
      k: "cons-ana",
      enc: "R18",
      pl: pl("ANESTESIA_LOCAL"),
      proc: "Biopsia de piel con anestesia local",
      firmar: false,
    },
  ];
  for (const c of casos) {
    const reg = L.E.get("cons:" + c.k) || {};
    const medico = M(c.enc);
    if (!reg.id) {
      const r = await L.api(medico, "POST", "/consentimientos", {
        plantillaId: c.pl.id,
        pacienteId: P(c.enc),
        procedimiento: c.proc,
      });
      if (r.status !== 201 && r.status !== 200) {
        console.log(
          "FALLO consentimiento",
          c.k,
          r.status,
          JSON.stringify(r.data).slice(0, 200),
        );
        continue;
      }
      reg.id = r.data.id;
      reg.numero = r.data.numero || r.data.codigo;
      L.E.set("cons:" + c.k, reg);
    }
    if (c.firmar && !reg.firmado) {
      const pac = enc(c.enc).paciente;
      const dni = await dniDe(pac);
      const nombre = (await L.api(pac, "GET", "/perfil")).data.nombreCompleto;
      const body = {
        firmaPng: FIRMA,
        firmanteTipo: "PACIENTE",
        nombre,
        dni,
        via: "APP_PACIENTE",
      };
      if (c.pl.requiereTestigo) {
        body.via = "CONSULTORIO";
        body.testigoNombre = "Rosa Medina";
        body.testigoDni = "40506071";
      }
      const f = await L.api(
        c.pl.requiereTestigo ? medico : pac,
        "POST",
        "/consentimientos/" + reg.id + "/firma",
        body,
      );
      if (f.status === 200 || f.status === 201) {
        reg.firmado = true;
        L.E.set("cons:" + c.k, reg);
      } else
        console.log(
          "FALLO firma",
          c.k,
          f.status,
          JSON.stringify(f.data).slice(0, 200),
        );
    }
    if (c.revocar && reg.firmado && !reg.revocado) {
      const v = await L.api(
        enc(c.enc).paciente,
        "POST",
        "/consentimientos/" + reg.id + "/revocar",
        { motivo: "Decidí no realizarme el procedimiento" },
      );
      if (v.status === 200) {
        reg.revocado = true;
        L.E.set("cons:" + c.k, reg);
      } else
        console.log(
          "FALLO revocar",
          c.k,
          v.status,
          JSON.stringify(v.data).slice(0, 200),
        );
    }
  }
  console.log(JSON.stringify(L.resumen()));
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
