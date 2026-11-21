const L = require("./lib");
const C = require("./clin");

const LO = "dra.lopez@tusalud.com";
const MONTOYA = "dra.montoya@tusalud.com";
const CAJA = "caja@tusalud.com";
const ADMIN = "admin@tusalud.com";

const NINOS = [
  {
    email: "emma.soto@correo.com",
    nombre: "Emma",
    apellido: "Soto",
    dni: "79100001",
    sexo: "F",
    dias: 190,
    cortePorEdad: 130,
    med: [
      [30, 4.3, 54, 37],
      [61, 5.2, 58, 39],
      [91, 6.1, 61, 40.5],
      [122, 6.8, 63.5, 41.5],
      [152, 7.3, 65.5, 42.3],
    ],
  },
  {
    email: "thiago.rios@correo.com",
    nombre: "Thiago",
    apellido: "Ríos",
    dni: "79100002",
    sexo: "M",
    dias: 820,
    cortePorEdad: 100,
    med: [
      [365, 9.6, 75, 46],
      [455, 10.3, 79, 47],
      [545, 10.9, 82, 47.5],
      [640, 11.6, 85, 48],
      [730, 12.4, 87.5, 48.5],
      [820, 13.0, 90, 49],
    ],
  },
  {
    email: "gael.mamani@correo.com",
    nombre: "Gael",
    apellido: "Mamani",
    dni: "79100003",
    sexo: "M",
    dias: 4020,
    corteDiasAtras: 120,
    med: [
      [3000, 38, 132],
      [3365, 42, 136],
      [3730, 47, 139],
      [4000, 52, 141],
    ],
  },
  {
    email: "isabella.cruz@correo.com",
    nombre: "Isabella",
    apellido: "Cruz",
    dni: "79100004",
    sexo: "F",
    dias: 460,
    corteDiasAtras: 0,
    med: [
      [183, 7.8, 68, 43],
      [274, 9.0, 73, 44.5],
      [365, 9.8, 77, 46],
      [450, 10.4, 80, 47],
    ],
  },
];

function iso(d) {
  return new Date(d).toISOString().slice(0, 10);
}

async function pediatria() {
  for (const n of NINOS) {
    let reg = L.E.get("nino:" + n.email) || {};
    const nac = iso(Date.now() - n.dias * 86400000);
    if (!reg.registrado) {
      const r = await L.api(n.email, "GET", "/perfil").catch(() => null);
      if (!r || r.status !== 200) {
        const rr = await L.crudo("POST", "/auth/register", null, {
          nombre: n.nombre,
          apellido: n.apellido,
          email: n.email,
          password: L.claveDe(n.email),
          telefono: "9" + n.dni,
          dni: n.dni,
          fechaNacimiento: nac,
          sexo: n.sexo,
        });
        console.log(
          "registro",
          n.email,
          rr.status,
          JSON.stringify(rr.data).slice(0, 160),
        );
        await L.dormir(3200);
        if (rr.status !== 201 && rr.status !== 200) continue;
      }
      reg.registrado = true;
      reg.nacimiento = nac;
      L.E.set("nino:" + n.email, reg);
    }
    if (!reg.cita) {
      const med = await C.medicoDe(LO, ADMIN);
      const cita = await C.crearCita(
        n.email,
        LO,
        med,
        "Control del niño sano y vacunación",
        "Pediatría",
        1,
      );
      const c = await L.api(LO, "PUT", "/citas/" + cita.id + "/confirmar", {});
      reg.cita = cita.id;
      reg.pacienteId = cita.pacienteId;
      L.E.set("nino:" + n.email, reg);
      console.log("cita pediátrica", n.email, c.status);
    }
    if (!reg.dosis) {
      const c0 = await L.api(
        LO,
        "GET",
        "/vacunacion/pacientes/" + reg.pacienteId + "/carne",
      );
      if (c0.status !== 200) {
        console.log(
          "carné",
          n.email,
          c0.status,
          JSON.stringify(c0.data).slice(0, 200),
        );
        continue;
      }
      const corte = n.cortePorEdad
        ? new Date(reg.nacimiento).getTime() + n.cortePorEdad * 86400000
        : Date.now() - n.corteDiasAtras * 86400000;
      let ok = 0;
      for (const d of c0.data.dosis) {
        if (d.estado === "APLICADA") continue;
        const rec = new Date(d.fechaRecomendada).getTime();
        if (rec > corte) continue;
        const fecha = iso(Math.min(Date.now() - 86400000, rec + 2 * 86400000));
        const r = await L.api(LO, "POST", "/vacunacion/dosis", {
          pacienteId: reg.pacienteId,
          vacunaId: d.vacunaId,
          numeroDosis: d.numeroDosis,
          fechaAplicacion: fecha,
          lote: "QA-" + d.vacunaCodigo + "-" + String(ok + 100),
          observaciones: "Dosis de datos de ejemplo de QA",
        });
        if (r.status === 201 || r.status === 200) ok++;
        else
          console.log(
            "FALLO dosis",
            n.email,
            d.vacunaCodigo,
            d.numeroDosis,
            r.status,
            JSON.stringify(r.data).slice(0, 160),
          );
      }
      reg.dosis = ok;
      L.E.set("nino:" + n.email, reg);
    }
    if (!reg.sexo) {
      const p = await L.api(n.email, "PUT", "/perfil", {
        nombre: n.nombre,
        apellido: n.apellido,
        telefono: "9" + n.dni,
        fechaNacimiento: reg.nacimiento,
        sexo: n.sexo,
      });
      if (p.status === 200) reg.sexo = n.sexo;
      else
        console.log(
          "FALLO sexo",
          n.email,
          p.status,
          JSON.stringify(p.data).slice(0, 200),
        );
      L.E.set("nino:" + n.email, reg);
    }
    if (!reg.crecimiento) {
      let ok = 0;
      for (const m of n.med) {
        const body = {
          pacienteId: reg.pacienteId,
          fecha: iso(new Date(reg.nacimiento).getTime() + m[0] * 86400000),
          pesoKg: m[1],
          tallaCm: m[2],
          observaciones: "Control de crecimiento de datos de ejemplo de QA",
        };
        if (m[3]) body.perimetroCefalicoCm = m[3];
        const r = await L.api(LO, "POST", "/crecimiento/mediciones", body);
        if (r.status === 201 || r.status === 200) ok++;
        else
          console.log(
            "FALLO crecimiento",
            n.email,
            r.status,
            JSON.stringify(r.data).slice(0, 160),
          );
      }
      reg.crecimiento = ok;
      L.E.set("nino:" + n.email, reg);
    }
    const c1 = await L.api(
      LO,
      "GET",
      "/vacunacion/pacientes/" + reg.pacienteId + "/carne",
    );
    reg.resumenCarne =
      c1.status === 200
        ? {
            aplicadas: c1.data.aplicadas,
            atrasadas: c1.data.atrasadas,
            proximas: c1.data.proximas,
            edadMeses: c1.data.edadMeses,
          }
        : c1.status;
    L.E.set("nino:" + n.email, reg);
    console.log(n.email, JSON.stringify(reg));
  }
}

async function odontologia() {
  const proc = L.lista(
    (await L.api(MONTOYA, "GET", "/odontologia/procedimientos")).data,
  ).filter((p) => p.activo !== false);
  console.log(
    "procedimientos:",
    proc
      .map(
        (p) =>
          p.codigo +
          (p.requierePieza ? "P" : "") +
          (p.requiereSuperficie ? "S" : "") +
          (p.requiereConsentimiento ? "C" : ""),
      )
      .join(" "),
  );
  const simple = proc.find(
    (p) => !p.requierePieza && !p.requiereConsentimiento,
  );
  const conPieza = proc.find(
    (p) =>
      p.requierePieza && !p.requiereConsentimiento && !p.requiereSuperficie,
  );
  const conSup = proc.find(
    (p) => p.requierePieza && p.requiereSuperficie && !p.requiereConsentimiento,
  );

  const casos = [
    {
      k: "maria",
      email: "maria.rodriguez@correo.com",
      cambios: [
        [26, "O", "CARIES"],
        [36, "O", "OBTURADO"],
        [48, null, "AUSENTE"],
        [11, null, "SANO"],
      ],
      estado: "PRESENTADO",
      items: [[simple], [conPieza, 26], [conSup, 26, "O"]],
    },
    {
      k: "carlos",
      email: "carlos.mendoza@correo.com",
      cambios: [
        [46, null, "CORONA"],
        [21, null, "ENDODONCIA"],
        [14, "M", "CARIES"],
        [24, "D", "OBTURADO"],
      ],
      estado: "ACEPTADO",
      items: [[simple], [conPieza, 14], [conSup, 14, "M"]],
      pago: 100,
    },
    {
      k: "sofia",
      email: "sofia.ramirez@correo.com",
      cambios: [
        [16, "V", "SELLANTE"],
        [17, "O", "CARIES"],
      ],
      estado: "BORRADOR",
      items: [[simple]],
    },
  ];
  const med = await C.medicoDe(MONTOYA, ADMIN);
  for (const c of casos) {
    let reg = L.E.get("odonto:" + c.k) || {};
    if (!reg.cita) {
      try {
        const cita = await C.crearCita(
          c.email,
          MONTOYA,
          med,
          "Evaluación dental y plan de tratamiento",
          "Odontología",
          1,
        );
        await L.api(MONTOYA, "PUT", "/citas/" + cita.id + "/confirmar", {});
        reg.cita = cita.id;
        reg.pacienteId = cita.pacienteId;
        L.E.set("odonto:" + c.k, reg);
      } catch (e) {
        console.log("FALLO cita odontológica", c.k, e.message.slice(0, 200));
        continue;
      }
    }
    if (!reg.cambios) {
      let n = 0;
      for (const [pieza, sup, estado] of c.cambios) {
        const body = {
          pieza,
          estado,
          observacion: "Hallazgo registrado en la evaluación de QA",
        };
        if (sup) body.superficie = sup;
        const r = await L.api(
          MONTOYA,
          "POST",
          "/odontologia/pacientes/" + reg.pacienteId + "/odontograma/cambios",
          body,
        );
        if (r.status === 201 || r.status === 200) n++;
        else
          console.log(
            "FALLO cambio",
            c.k,
            pieza,
            estado,
            r.status,
            JSON.stringify(r.data).slice(0, 160),
          );
      }
      reg.cambios = n;
      L.E.set("odonto:" + c.k, reg);
    }
    if (!reg.planId) {
      const r = await L.api(MONTOYA, "POST", "/odontologia/planes", {
        pacienteId: reg.pacienteId,
        notas: "Plan de tratamiento dental de datos de ejemplo de QA",
      });
      if (r.status === 201 || r.status === 200) reg.planId = r.data.id;
      else
        console.log(
          "FALLO plan dental",
          c.k,
          r.status,
          JSON.stringify(r.data).slice(0, 200),
        );
      L.E.set("odonto:" + c.k, reg);
    }
    if (reg.planId && !reg.items) {
      let n = 0;
      for (const [p, pieza, sup] of c.items) {
        if (!p) continue;
        const body = { procedimientoId: p.id };
        if (pieza) body.pieza = pieza;
        if (sup) body.superficie = sup;
        const r = await L.api(
          MONTOYA,
          "POST",
          "/odontologia/planes/" + reg.planId + "/items",
          body,
        );
        if (r.status === 201 || r.status === 200) n++;
        else
          console.log(
            "FALLO ítem",
            c.k,
            p.codigo,
            r.status,
            JSON.stringify(r.data).slice(0, 160),
          );
      }
      reg.items = n;
      L.E.set("odonto:" + c.k, reg);
    }
    if (
      reg.planId &&
      ["PRESENTADO", "ACEPTADO"].includes(c.estado) &&
      !reg.presentado
    ) {
      const r = await L.api(
        MONTOYA,
        "POST",
        "/odontologia/planes/" + reg.planId + "/presentar",
        {},
      );
      if (r.status === 200 || r.status === 201) reg.presentado = true;
      else
        console.log(
          "FALLO presentar",
          c.k,
          r.status,
          JSON.stringify(r.data).slice(0, 200),
        );
      L.E.set("odonto:" + c.k, reg);
    }
    if (reg.presentado && c.estado === "ACEPTADO" && !reg.aceptado) {
      const r = await L.api(
        c.email,
        "POST",
        "/odontologia/planes/" + reg.planId + "/aceptar",
        {},
      );
      if (r.status === 200 || r.status === 201) reg.aceptado = true;
      else
        console.log(
          "FALLO aceptar",
          c.k,
          r.status,
          JSON.stringify(r.data).slice(0, 200),
        );
      L.E.set("odonto:" + c.k, reg);
    }
    if (reg.aceptado && c.pago && !reg.pago) {
      const r = await L.api(
        CAJA,
        "POST",
        "/odontologia/planes/" + reg.planId + "/pagos",
        { monto: c.pago, metodo: "EFECTIVO" },
      );
      if (r.status === 200 || r.status === 201) reg.pago = c.pago;
      else
        console.log(
          "FALLO pago",
          c.k,
          r.status,
          JSON.stringify(r.data).slice(0, 200),
        );
      L.E.set("odonto:" + c.k, reg);
    }
    console.log("odonto", c.k, JSON.stringify(reg));
  }
}

async function main() {
  await pediatria();
  await odontologia();
  console.log(JSON.stringify(L.resumen()));
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
