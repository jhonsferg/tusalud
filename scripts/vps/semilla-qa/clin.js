const L = require("./lib");

const cacheMedicos = {};

async function medicoDe(email, viewer) {
  if (cacheMedicos[email]) return cacheMedicos[email];
  const dir = L.E.get("dirMedicos");
  if (dir && dir[email]) {
    cacheMedicos[email] = dir[email];
    return dir[email];
  }
  const usuarios = {};
  const todos = [];
  for (let p = 0; p < 10; p++) {
    const r = await L.api(
      viewer,
      "GET",
      "/admin/usuarios?rol=ROLE_MEDICO&page=" + p + "&size=100",
    );
    const l = L.lista(r.data);
    l.forEach((u) => (usuarios[u.id] = u.email));
    if (l.length < 100) break;
  }
  const ms = L.lista((await L.api(viewer, "GET", "/medicos")).data);
  const map = {};
  for (const m of ms) {
    const em = usuarios[m.usuarioId];
    if (em)
      map[em] = {
        id: m.id,
        usuarioId: m.usuarioId,
        nombre: m.nombreCompleto,
        especialidades: m.especialidades.map((e) => ({
          id: e.id,
          nombre: e.nombre,
        })),
      };
  }
  L.E.set("dirMedicos", map);
  Object.assign(cacheMedicos, map);
  return cacheMedicos[email];
}

async function buscarHueco(emailPaciente, medicoId, desdeDia) {
  for (let d = desdeDia || 1; d < 45; d++) {
    const r = await L.api(
      emailPaciente,
      "GET",
      "/medicos/" + medicoId + "/disponibilidad?fecha=" + L.fecha(d),
    );
    const h = r.data && r.data.horariosDisponibles;
    if (h && h.length)
      return { fecha: L.fecha(d), hora: h[0], dia: d, resto: h };
  }
  return null;
}

async function crearCita(
  emailPaciente,
  emailMedico,
  medicoInfo,
  motivo,
  nombreEspecialidad,
  desdeDia,
) {
  const esp =
    (nombreEspecialidad &&
      medicoInfo.especialidades.find((e) => e.nombre === nombreEspecialidad)) ||
    medicoInfo.especialidades[0];
  let dia = desdeDia || 1;
  for (let intento = 0; intento < 6; intento++) {
    const h = await buscarHueco(emailPaciente, medicoInfo.id, dia);
    if (!h) throw new Error("Sin hueco para " + emailMedico);
    const r = await L.api(emailPaciente, "POST", "/citas", {
      medicoId: medicoInfo.id,
      especialidadId: esp.id,
      fechaHora: h.fecha + "T" + h.hora + ":00",
      motivo,
      modalidad: "PRESENCIAL",
    });
    if (r.status === 201 || r.status === 200) return r.data;
    if (r.status === 409 || r.status === 400) {
      dia = h.dia + 1;
      continue;
    }
    throw new Error(
      "Cita " + r.status + " " + JSON.stringify(r.data).slice(0, 200),
    );
  }
  throw new Error("No se pudo reservar cita");
}

async function atender(clave, spec, viewer) {
  const prev = L.E.get("enc:" + clave);
  if (prev && prev.atencionId) return prev;
  const med = await medicoDe(spec.med, viewer);
  let reg = prev || {};
  if (!reg.citaId) {
    const cita = await crearCita(
      spec.pac,
      spec.med,
      med,
      spec.motivo,
      spec.especialidad,
      spec.desdeDia,
    );
    reg = {
      citaId: cita.id,
      pacienteId: cita.pacienteId,
      fechaHora: cita.fechaHora,
      paciente: spec.pac,
      medico: spec.med,
      motivo: spec.motivo,
    };
    L.E.set("enc:" + clave, reg);
    const c = await L.api(
      spec.med,
      "PUT",
      "/citas/" + cita.id + "/confirmar",
      {},
    );
    if (![200, 204].includes(c.status))
      throw new Error(
        "Confirmar " + c.status + " " + JSON.stringify(c.data).slice(0, 200),
      );
  }
  const cuerpo = {
    diagnostico: spec.diagnostico,
    tratamiento: spec.tratamiento,
    indicaciones: spec.indicaciones,
    observaciones: spec.observaciones,
    diagnosticos: spec.dx.map((d, i) => ({
      codigo: d,
      tipo: "DEFINITIVO",
      principal: i === 0,
    })),
    receta: (spec.rx || []).map((x) => ({
      medicamento: x.nombre,
      dosis: x.dosis,
      frecuencia: x.frecuencia,
      duracion: x.duracion,
    })),
    ...(spec.vit || {}),
  };
  const a = await L.api(
    spec.med,
    "POST",
    "/citas/" + reg.citaId + "/atencion",
    cuerpo,
  );
  if (a.status !== 201 && a.status !== 200)
    throw new Error(
      "Atención " + a.status + " " + JSON.stringify(a.data).slice(0, 300),
    );
  reg.atencionId = a.data.id || (a.data.atencion && a.data.atencion.id);
  if (!reg.atencionId) {
    const g = await L.api(
      spec.med,
      "GET",
      "/citas/" + reg.citaId + "/atencion",
    );
    reg.atencionId = g.data && g.data.id;
  }
  L.E.set("enc:" + clave, reg);
  return reg;
}

module.exports = { medicoDe, buscarHueco, crearCita, atender };
