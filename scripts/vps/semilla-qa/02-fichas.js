const L = require("./lib");

const RRHH = "rrhh@tusalud.com";

const POR_ROL = {
  ROLE_MEDICO: { area: "MEDICA", cargo: "Médico asistente", sueldo: 6500 },
  ROLE_RECEPCION: { area: "RECEPCION", cargo: "Recepcionista", sueldo: 2100 },
  ROLE_CAJA: { area: "CAJA", cargo: "Cajero", sueldo: 2300 },
  ROLE_ENFERMERIA: { area: "ENFERMERIA", cargo: "Enfermera", sueldo: 3200 },
  ROLE_LABORATORIO: {
    area: "LABORATORIO",
    cargo: "Tecnóloga de laboratorio",
    sueldo: 3300,
  },
  ROLE_IMAGENES: {
    area: "MEDICA",
    cargo: "Tecnólogo de imágenes",
    sueldo: 3400,
  },
  ROLE_FARMACIA: {
    area: "FARMACIA",
    cargo: "Químico farmacéutico",
    sueldo: 3800,
  },
  ROLE_ALMACEN: {
    area: "ALMACEN",
    cargo: "Encargado de almacén",
    sueldo: 3000,
  },
  ROLE_GERENCIA: { area: "GERENCIA", cargo: "Subgerente", sueldo: 9000 },
  ROLE_FINANZAS: {
    area: "FINANZAS",
    cargo: "Analista financiero",
    sueldo: 4200,
  },
  ROLE_OPERACIONES: {
    area: "OPERACIONES",
    cargo: "Analista de operaciones",
    sueldo: 3600,
  },
  ROLE_CALIDAD: { area: "CALIDAD", cargo: "Analista de calidad", sueldo: 3700 },
  ROLE_RRHH: {
    area: "RRHH",
    cargo: "Analista de recursos humanos",
    sueldo: 3800,
  },
};

async function main() {
  const disp = await L.api(RRHH, "GET", "/rrhh/usuarios-disponibles");
  const lista = L.lista(disp.data);
  console.log("sin ficha:", lista.length);
  let n = 0;
  for (const u of lista) {
    const rol = (u.roles || []).find((r) => POR_ROL[r]);
    if (!rol) {
      console.log("omitido (sin rol de personal)", u.email, u.roles);
      continue;
    }
    const d = POR_ROL[rol];
    const r = await L.api(RRHH, "POST", "/rrhh/empleados", {
      usuarioId: u.usuarioId,
      cargo: d.cargo,
      area: d.area,
      tipoContrato: "INDEFINIDO",
      fechaIngreso: "2025-03-03",
      sueldo: d.sueldo,
      contactoEmergenciaNombre: "Contacto de " + u.nombreCompleto,
      contactoEmergenciaTelefono: "987654321",
      diasVacacionesAnuales: 30,
    });
    if (r.status === 201 || r.status === 200) n++;
    else
      console.log(
        "FALLO ficha",
        u.email,
        r.status,
        JSON.stringify(r.data).slice(0, 200),
      );
  }
  console.log("fichas creadas:", n);
  const em = await L.api(RRHH, "GET", "/rrhh/empleados?size=200");
  const empleados = L.lista(em.data);
  console.log(
    "empleados:",
    empleados.length,
    JSON.stringify(empleados[0]).slice(0, 300),
  );
  const co = await L.api(RRHH, "GET", "/planilla/contratos");
  console.log(
    "contratos:",
    L.lista(co.data).length,
    JSON.stringify(L.lista(co.data)[0] || co.data).slice(0, 300),
  );
  console.log(JSON.stringify(L.resumen()));
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
