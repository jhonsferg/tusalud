const L = require("./lib");
const C = require("./clin");

const ADMIN = "admin@tusalud.com";
const SA = "dr.salazar@tusalud.com";

const PARES = [
  {
    k: "D1",
    nombre: "Rosa",
    apellido: "Quispe Huamán",
    nac: "1988-04-12",
    sexo: "F",
    a: { email: "rosa.quispe@correo.com", dni: "78000011" },
    b: { email: "rosa.quispe2@correo.com", dni: "78000012" },
    fusionar: true,
  },
  {
    k: "D2",
    nombre: "Marcos",
    apellido: "Cárdenas Vela",
    nac: "1975-09-30",
    sexo: "M",
    a: { email: "marcos.cardenas@correo.com", dni: "78000021" },
    b: { email: "marcos.cardenas2@correo.com", dni: "78000022" },
    fusionar: false,
  },
];

async function registrar(p, c) {
  const ya = await L.api(c.email, "GET", "/perfil").catch(() => null);
  if (ya && ya.status === 200) return true;
  const r = await L.crudo("POST", "/auth/register", null, {
    nombre: p.nombre,
    apellido: p.apellido,
    email: c.email,
    password: L.claveDe(c.email),
    telefono: "9" + c.dni,
    dni: c.dni,
    fechaNacimiento: p.nac,
    sexo: p.sexo,
  });
  console.log("registro", c.email, r.status);
  await L.dormir(3200);
  return r.status === 201 || r.status === 200;
}

async function main() {
  const med = await C.medicoDe(SA, ADMIN);
  for (const p of PARES) {
    const reg = L.E.get("dup:" + p.k) || {};
    if (!reg.registrados) {
      const a = await registrar(p, p.a);
      const b = await registrar(p, p.b);
      if (!a || !b) continue;
      reg.registrados = true;
      L.E.set("dup:" + p.k, reg);
    }
    if (!reg.citas) {
      for (const c of [p.a, p.b]) {
        const cita = await C.crearCita(
          c.email,
          SA,
          med,
          "Control general de salud",
          "Medicina General",
          2,
        );
        await L.api(SA, "PUT", "/citas/" + cita.id + "/confirmar", {});
      }
      reg.citas = true;
      L.E.set("dup:" + p.k, reg);
    }
    const dup = await L.api(ADMIN, "GET", "/hce/pacientes/duplicados");
    const grupo = L.lista(dup.data).find(
      (g) =>
        JSON.stringify(g).includes(p.a.dni) ||
        JSON.stringify(g).includes(p.b.dni),
    );
    console.log(
      p.k,
      "grupo de duplicados",
      JSON.stringify(grupo).slice(0, 400),
    );
    if (p.fusionar && !reg.fusionado && grupo) {
      const esA = grupo.dniA === p.a.dni;
      const destino = { id: esA ? grupo.pacienteAId : grupo.pacienteBId };
      const origen = { id: esA ? grupo.pacienteBId : grupo.pacienteAId };
      if (origen && destino) {
        const r = await L.api(ADMIN, "POST", "/hce/pacientes/fusion", {
          origenId: origen.id,
          destinoId: destino.id,
          motivo:
            "Mismo paciente registrado dos veces con distinto documento, verificado en mostrador",
        });
        console.log("fusión", r.status, JSON.stringify(r.data).slice(0, 300));
        if (r.status === 200 || r.status === 201) reg.fusionado = true;
        L.E.set("dup:" + p.k, reg);
      }
    }
  }
  const f = await L.api(ADMIN, "GET", "/hce/pacientes/fusiones");
  console.log("fusiones", f.status, JSON.stringify(f.data).slice(0, 300));
  console.log(JSON.stringify(L.resumen()));
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
