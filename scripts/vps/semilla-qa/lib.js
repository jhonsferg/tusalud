const fs = require("fs");
const path = require("path");

const DIR = __dirname;
const SALIDA = path.join(DIR, "salida");
fs.mkdirSync(SALIDA, { recursive: true });

const BASE = process.env.TUSALUD_API;
const RUTA_SECRETOS =
  process.env.TUSALUD_SECRETOS || path.join(DIR, "secretos.local.json");
const RUTA_TOKENS = path.join(SALIDA, "tokens.local.json");
const RUTA_ESTADO = path.join(DIR, "estado.json");
const PAUSA_MS = Number(process.env.TUSALUD_PAUSA_MS || 220);
const PAUSA_LOGIN_MS = 3200;

function leerJson(ruta, defecto) {
  try {
    return JSON.parse(fs.readFileSync(ruta, "utf8"));
  } catch (e) {
    return defecto;
  }
}

function escribirJson(ruta, valor) {
  fs.writeFileSync(ruta, JSON.stringify(valor, null, 2), "utf8");
}

const secretos = leerJson(RUTA_SECRETOS, {});
let tokens = leerJson(RUTA_TOKENS, {});
let estado = leerJson(RUTA_ESTADO, {});
let ultimoLogin = 0;
let ultimaLlamada = 0;
const metricas = { llamadas: 0, ms: 0, porEstado: {} };

const dormir = (ms) => new Promise((r) => setTimeout(r, ms));

function claveDe(email) {
  const e = email.toLowerCase();
  if (secretos.porCorreo && secretos.porCorreo[e]) return secretos.porCorreo[e];
  let k = "personal";
  if (e === "admin.qa@tusalud.com") k = "adminQa";
  else if (e === "admin@tusalud.com") k = "admin";
  else if (/^(dr|dra)\./.test(e) || /^qa\.medico/.test(e)) k = "medico";
  else if (e.endsWith("@correo.com")) k = "paciente";
  const v = process.env["TUSALUD_CLAVE_" + k.toUpperCase()] || secretos[k];
  if (!v)
    throw new Error(
      'Falta la clave "' +
        k +
        '" en ' +
        RUTA_SECRETOS +
        " o en TUSALUD_CLAVE_" +
        k.toUpperCase(),
    );
  return v;
}

function exp(token) {
  try {
    return (
      JSON.parse(Buffer.from(token.split(".")[1], "base64url").toString()).exp *
      1000
    );
  } catch (e) {
    return 0;
  }
}

async function crudo(metodo, ruta, token, cuerpo) {
  const t0 = Date.now();
  const cabeceras = { Accept: "application/json" };
  if (token) cabeceras.Authorization = "Bearer " + token;
  let body;
  if (cuerpo !== undefined) {
    cabeceras["Content-Type"] = "application/json";
    body = JSON.stringify(cuerpo);
  }
  const res = await fetch(BASE + ruta, {
    method: metodo,
    headers: cabeceras,
    body,
  });
  const texto = await res.text();
  let data = null;
  try {
    data = texto ? JSON.parse(texto) : null;
  } catch (e) {
    data = texto;
  }
  const ms = Date.now() - t0;
  metricas.llamadas++;
  metricas.ms += ms;
  metricas.porEstado[res.status] = (metricas.porEstado[res.status] || 0) + 1;
  return { status: res.status, data, ms, headers: res.headers };
}

async function login(email, forzar) {
  const e = email.toLowerCase();
  const t = tokens[e];
  if (!forzar && t && exp(t.token) - Date.now() > 90000) return t.token;
  for (let intento = 0; intento < 6; intento++) {
    const espera = PAUSA_LOGIN_MS - (Date.now() - ultimoLogin);
    if (espera > 0) await dormir(espera);
    ultimoLogin = Date.now();
    let r;
    try {
      r = await crudo("POST", "/auth/login", null, {
        email,
        password: claveDe(email),
      });
    } catch (err) {
      await dormir(5000);
      continue;
    }
    if (r.status === 429) {
      await dormir(20000);
      continue;
    }
    if (r.status !== 200)
      throw new Error(
        "Login " +
          email +
          " respondió " +
          r.status +
          " " +
          JSON.stringify(r.data).slice(0, 200),
      );
    tokens[e] = {
      token: r.data.token || r.data.accessToken,
      refresh: r.data.refreshToken,
      roles: r.data.roles,
      id: r.data.id || r.data.usuarioId,
    };
    escribirJson(RUTA_TOKENS, tokens);
    return tokens[e].token;
  }
  throw new Error("Login " + email + " agotó reintentos por 429");
}

async function loginCompleto(email) {
  await login(email);
  return tokens[email.toLowerCase()];
}

async function api(email, metodo, ruta, cuerpo, opciones) {
  const op = opciones || {};
  let token = await login(email);
  for (let intento = 0; intento < 4; intento++) {
    const espera = PAUSA_MS - (Date.now() - ultimaLlamada);
    if (espera > 0) await dormir(espera);
    let r;
    try {
      r = await crudo(metodo, ruta, token, cuerpo);
    } catch (err) {
      ultimaLlamada = Date.now();
      await dormir(2000);
      continue;
    }
    ultimaLlamada = Date.now();
    if (r.status === 401 && intento < 2) {
      token = await login(email, true);
      continue;
    }
    if (r.status === 429) {
      await dormir(15000);
      continue;
    }
    if (r.status >= 500) {
      fs.appendFileSync(
        path.join(SALIDA, "errores5xx.ndjson"),
        JSON.stringify({
          cuando: new Date().toISOString(),
          email,
          metodo,
          ruta,
          status: r.status,
          cuerpo,
          respuesta: r.data,
        }) + "\n",
      );
    }
    if (op.esperado && !op.esperado.includes(r.status)) {
      r.inesperado = true;
    }
    return r;
  }
  throw new Error("Sin respuesta para " + metodo + " " + ruta);
}

async function ok(email, metodo, ruta, cuerpo, aceptados) {
  const r = await api(email, metodo, ruta, cuerpo);
  const buenos = aceptados || [200, 201, 202, 204];
  if (!buenos.includes(r.status)) {
    const e = new Error(
      metodo +
        " " +
        ruta +
        " -> " +
        r.status +
        " " +
        JSON.stringify(r.data).slice(0, 400),
    );
    e.respuesta = r;
    throw e;
  }
  return r.data;
}

async function intentar(email, metodo, ruta, cuerpo) {
  const r = await api(email, metodo, ruta, cuerpo);
  const bien = r.status >= 200 && r.status < 300;
  return { bien, status: r.status, data: r.data, ms: r.ms };
}

const E = {
  get(k) {
    return estado[k];
  },
  set(k, v) {
    estado[k] = v;
    escribirJson(RUTA_ESTADO, estado);
  },
  todo() {
    return estado;
  },
  recargar() {
    estado = leerJson(RUTA_ESTADO, {});
  },
};

function lista(d) {
  if (Array.isArray(d)) return d;
  if (d && Array.isArray(d.content)) return d.content;
  if (d && Array.isArray(d.items)) return d.items;
  return [];
}

const DESFASE_LIMA_MS = 5 * 3600000;

function ahoraLima() {
  return new Date(Date.now() - DESFASE_LIMA_MS);
}

function hace(horas) {
  return new Date(Date.now() - DESFASE_LIMA_MS - horas * 3600000)
    .toISOString()
    .slice(0, 19);
}

function fecha(dias) {
  const d = new Date(Date.now() - DESFASE_LIMA_MS + dias * 86400000);
  return d.toISOString().slice(0, 10);
}

function fechaHora(dias, hora) {
  const d = new Date(Date.now() - DESFASE_LIMA_MS + dias * 86400000);
  return d.toISOString().slice(0, 10) + "T" + (hora || "10:00") + ":00";
}

function resumen() {
  return {
    llamadas: metricas.llamadas,
    msTotales: metricas.ms,
    msPromedio: metricas.llamadas
      ? Math.round(metricas.ms / metricas.llamadas)
      : 0,
    porEstado: metricas.porEstado,
  };
}

module.exports = {
  hace,
  ahoraLima,
  api,
  ok,
  intentar,
  login,
  loginCompleto,
  claveDe,
  E,
  lista,
  fecha,
  fechaHora,
  dormir,
  resumen,
  leerJson,
  escribirJson,
  SALIDA,
  DIR,
  BASE,
  crudo,
};
