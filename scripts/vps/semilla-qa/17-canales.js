const L = require("./lib");

const ADMIN = "admin@tusalud.com";

const CASOS = [
  { email: "juan.perez@correo.com", canales: ["SMS", "WHATSAPP"] },
  { email: "maria.rodriguez@correo.com", canales: ["SMS"] },
  { email: "carlos.mendoza@correo.com", canales: ["WHATSAPP"] },
];

async function codigoDe(canal, destino) {
  const r = await L.api(ADMIN, "GET", "/admin/canales/simulacion");
  const l = L.lista(r.data);
  const texto = JSON.stringify(l.length ? l : r.data);
  const filas = (Array.isArray(l) && l.length ? l : []).filter((x) =>
    JSON.stringify(x).includes(destino),
  );
  const ultimo = filas.length ? filas[0] : null;
  const m = JSON.stringify(ultimo || r.data).match(/\b(\d{6})\b/);
  return { codigo: m && m[1], muestra: texto.slice(0, 200) };
}

async function main() {
  for (const c of CASOS) {
    const reg = L.E.get("canales:" + c.email) || { activos: [] };
    const perfil = (await L.api(c.email, "GET", "/perfil")).data;
    const mis = await L.api(c.email, "GET", "/canales/mis-canales");
    console.log(
      c.email,
      "mis-canales",
      mis.status,
      JSON.stringify(mis.data).slice(0, 300),
    );
    for (const canal of c.canales) {
      if (reg.activos.includes(canal)) continue;
      const s = await L.api(
        c.email,
        "POST",
        "/canales/contactos/" + canal + "/solicitar",
        { destino: perfil.telefono, consentimiento: true },
      );
      if (s.status !== 200 && s.status !== 201 && s.status !== 204) {
        console.log(
          "FALLO solicitar",
          c.email,
          canal,
          s.status,
          JSON.stringify(s.data).slice(0, 250),
        );
        continue;
      }
      await L.dormir(1500);
      const { codigo, muestra } = await codigoDe(canal, perfil.telefono);
      if (!codigo) {
        console.log("no se halló el código en la bandeja simulada", muestra);
        continue;
      }
      const v = await L.api(
        c.email,
        "POST",
        "/canales/contactos/" + canal + "/verificar",
        { codigo },
      );
      if (v.status === 200 || v.status === 204) {
        reg.activos.push(canal);
        L.E.set("canales:" + c.email, reg);
      } else
        console.log(
          "FALLO verificar",
          c.email,
          canal,
          v.status,
          JSON.stringify(v.data).slice(0, 250),
        );
    }
    if (reg.activos.length && !reg.preferencias) {
      const pr = await L.api(c.email, "PUT", "/canales/preferencias", {
        preferencias: reg.activos.map((canal) => ({
          tipo: "CIRUGIA_PROGRAMADA",
          canal,
          activo: true,
        })),
      });
      if (pr.status === 200 || pr.status === 204) {
        reg.preferencias = true;
        L.E.set("canales:" + c.email, reg);
      } else
        console.log(
          "FALLO preferencias",
          c.email,
          pr.status,
          JSON.stringify(pr.data).slice(0, 250),
        );
    }
    const post = await L.api(c.email, "GET", "/canales/mis-canales");
    console.log(c.email, "después", JSON.stringify(post.data).slice(0, 500));
  }
  console.log(JSON.stringify(L.resumen()));
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
