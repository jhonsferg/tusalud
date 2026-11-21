const L = require("./lib");

const ADMIN = "admin@tusalud.com";
const RRHH = "rrhh@tusalud.com";

const PERSONAL = [
  {
    email: "admin.qa@tusalud.com",
    nombre: "Administrador",
    apellido: "QA",
    roles: ["ROLE_ADMIN"],
    nota: "Administrador exclusivo del equipo de QA",
    area: null,
  },
  {
    email: "recepcion2@tusalud.com",
    nombre: "Daniela",
    apellido: "Huamán",
    roles: ["ROLE_RECEPCION"],
    area: "RECEPCION",
    cargo: "Recepcionista",
    sueldo: 2100,
  },
  {
    email: "caja2@tusalud.com",
    nombre: "Raúl",
    apellido: "Chávez",
    roles: ["ROLE_CAJA"],
    area: "CAJA",
    cargo: "Cajero",
    sueldo: 2300,
  },
  {
    email: "enfermeria2@tusalud.com",
    nombre: "Gabriela",
    apellido: "Paz",
    roles: ["ROLE_ENFERMERIA"],
    area: "ENFERMERIA",
    cargo: "Enfermera de quirófano",
    sueldo: 3200,
  },
  {
    email: "enfermeria3@tusalud.com",
    nombre: "Milagros",
    apellido: "Tello",
    roles: ["ROLE_ENFERMERIA"],
    area: "ENFERMERIA",
    cargo: "Enfermera instrumentista",
    sueldo: 3200,
  },
  {
    email: "lab2@tusalud.com",
    nombre: "Sandra",
    apellido: "Ortiz",
    roles: ["ROLE_LABORATORIO"],
    area: "LABORATORIO",
    cargo: "Tecnóloga de laboratorio",
    sueldo: 3300,
  },
  {
    email: "imagenes2@tusalud.com",
    nombre: "Bruno",
    apellido: "Lazo",
    roles: ["ROLE_IMAGENES"],
    area: "MEDICA",
    cargo: "Tecnólogo de imágenes",
    sueldo: 3400,
  },
  {
    email: "farmacia2@tusalud.com",
    nombre: "Elena",
    apellido: "Rivas",
    roles: ["ROLE_FARMACIA"],
    area: "FARMACIA",
    cargo: "Químico farmacéutico",
    sueldo: 3800,
  },
  {
    email: "almacen2@tusalud.com",
    nombre: "Víctor",
    apellido: "Gamarra",
    roles: ["ROLE_ALMACEN"],
    area: "ALMACEN",
    cargo: "Jefe de almacén",
    sueldo: 3000,
  },
  {
    email: "gerencia2@tusalud.com",
    nombre: "Natalia",
    apellido: "Bustamante",
    roles: ["ROLE_GERENCIA"],
    area: "GERENCIA",
    cargo: "Subgerente",
    sueldo: 9000,
  },
  {
    email: "finanzas2@tusalud.com",
    nombre: "Iván",
    apellido: "Zegarra",
    roles: ["ROLE_FINANZAS"],
    area: "FINANZAS",
    cargo: "Tesorero",
    sueldo: 4500,
  },
  {
    email: "finanzas3@tusalud.com",
    nombre: "Claudia",
    apellido: "Espinoza",
    roles: ["ROLE_FINANZAS"],
    area: "FINANZAS",
    cargo: "Analista de cuentas por pagar",
    sueldo: 3900,
  },
  {
    email: "operaciones2@tusalud.com",
    nombre: "Fabiola",
    apellido: "Neyra",
    roles: ["ROLE_OPERACIONES"],
    area: "OPERACIONES",
    cargo: "Analista de operaciones",
    sueldo: 3600,
  },
  {
    email: "calidad2@tusalud.com",
    nombre: "Omar",
    apellido: "Salas",
    roles: ["ROLE_CALIDAD"],
    area: "CALIDAD",
    cargo: "Analista de calidad",
    sueldo: 3700,
  },
  {
    email: "rrhh2@tusalud.com",
    nombre: "Patricia",
    apellido: "Lara",
    roles: ["ROLE_RRHH"],
    area: "RRHH",
    cargo: "Analista de planillas",
    sueldo: 3800,
  },
];

const MEDICOS = [
  {
    email: "dr.cirujano@tusalud.com",
    nombre: "Héctor",
    apellido: "Maldonado",
    colegiatura: "CMP-91001",
    telefono: "987100101",
    consultorio: "Consultorio 501",
    esp: "Cirugía General",
  },
  {
    email: "dr.cirujano2@tusalud.com",
    nombre: "Julio",
    apellido: "Ccopa",
    colegiatura: "CMP-91002",
    telefono: "987100102",
    consultorio: "Consultorio 502",
    esp: "Cirugía General",
  },
  {
    email: "dra.anestesia@tusalud.com",
    nombre: "Elena",
    apellido: "Arana",
    colegiatura: "CMP-91003",
    telefono: "987100103",
    consultorio: "Sala de anestesia",
    esp: "Anestesiología",
  },
];

async function main() {
  const manifiesto = L.leerJson(L.DIR + "/manifiesto.json", { usuarios: [] });
  const existentes = new Map();
  for (const q of ["", "tusalud.com"]) {
    for (let p = 0; p < 20; p++) {
      const r = await L.api(
        ADMIN,
        "GET",
        "/admin/usuarios?page=" + p + "&size=100" + (q ? "&q=" + q : ""),
      );
      const l = L.lista(r.data);
      for (const u of l) existentes.set(u.email.toLowerCase(), u);
      if (l.length < 100) break;
    }
    if (existentes.size > 0) break;
  }
  console.log("usuarios existentes leídos:", existentes.size);

  const registrar = (e) => {
    const i = manifiesto.usuarios.findIndex((x) => x.email === e.email);
    if (i >= 0) manifiesto.usuarios[i] = { ...manifiesto.usuarios[i], ...e };
    else manifiesto.usuarios.push(e);
  };

  for (const p of PERSONAL) {
    let u = existentes.get(p.email);
    let creado = false;
    if (!u) {
      const r = await L.api(ADMIN, "POST", "/admin/personal", {
        nombre: p.nombre,
        apellido: p.apellido,
        email: p.email,
        password: L.claveDe(p.email),
        roles: p.roles,
      });
      if (r.status === 201 || r.status === 200) {
        u = r.data;
        creado = true;
      } else {
        console.log(
          "FALLO personal",
          p.email,
          r.status,
          JSON.stringify(r.data).slice(0, 200),
        );
        continue;
      }
    }
    registrar({
      email: p.email,
      nombre: p.nombre + " " + p.apellido,
      roles: p.roles,
      id: u.id,
      creadoPorApi: creado || undefined,
      nota: p.nota,
      ficha: false,
    });
    console.log(creado ? "creado" : "ya existe", p.email);
  }

  const esp = {};
  const re = await L.api(ADMIN, "GET", "/especialidades");
  for (const e of L.lista(re.data)) esp[e.nombre] = e.id;
  for (const nombre of ["Cirugía General", "Anestesiología"]) {
    if (!esp[nombre]) {
      const r = await L.api(ADMIN, "POST", "/especialidades", {
        nombre,
        descripcion: "Especialidad quirúrgica para el QA",
        precioConsulta: nombre === "Anestesiología" ? 180 : 170,
      });
      console.log("especialidad", nombre, r.status);
      if (r.status === 201 || r.status === 200) esp[nombre] = r.data.id;
    }
  }
  L.E.set("especialidades", esp);

  for (const m of MEDICOS) {
    let u = existentes.get(m.email);
    let creado = false;
    let medicoId;
    if (!u) {
      const r = await L.api(ADMIN, "POST", "/medicos", {
        nombre: m.nombre,
        apellido: m.apellido,
        email: m.email,
        password: L.claveDe(m.email),
        telefono: m.telefono,
        colegiatura: m.colegiatura,
        consultorio: m.consultorio,
        especialidadIds: [esp[m.esp]],
      });
      if (r.status === 201 || r.status === 200) {
        u = r.data;
        medicoId = r.data.id;
        creado = true;
        for (const dia of [
          "MONDAY",
          "TUESDAY",
          "WEDNESDAY",
          "THURSDAY",
          "FRIDAY",
        ]) {
          await L.api(ADMIN, "POST", "/medicos/" + medicoId + "/horarios", {
            diaSemana: dia,
            horaInicio: "08:00",
            horaFin: "13:00",
            duracionCitaMinutos: 30,
          });
        }
      } else {
        console.log(
          "FALLO médico",
          m.email,
          r.status,
          JSON.stringify(r.data).slice(0, 300),
        );
        continue;
      }
    }
    registrar({
      email: m.email,
      nombre: m.nombre + " " + m.apellido,
      roles: ["ROLE_MEDICO"],
      id: u.id,
      medicoId,
      colegiatura: m.colegiatura,
      especialidad: m.esp,
      creadoPorApi: creado || undefined,
    });
    console.log(
      creado ? "creado" : "ya existe",
      m.email,
      JSON.stringify(u).slice(0, 160),
    );
  }

  L.escribirJson(L.DIR + "/manifiesto.json", manifiesto);

  const disp = await L.api(RRHH, "GET", "/rrhh/usuarios-disponibles");
  const sinFicha = new Map(
    L.lista(disp.data).map((u) => [u.email ? u.email.toLowerCase() : u.id, u]),
  );
  console.log(
    "usuarios sin ficha:",
    sinFicha.size,
    JSON.stringify(L.lista(disp.data)[0] || disp.data).slice(0, 200),
  );
  L.escribirJson(L.SALIDA + "/sin-ficha.json", L.lista(disp.data));

  console.log(JSON.stringify(L.resumen()));
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
