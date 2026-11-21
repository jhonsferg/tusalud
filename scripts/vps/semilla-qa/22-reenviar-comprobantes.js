const L = require("./lib");

const ADMIN = "admin@tusalud.com";

async function leer() {
  const r = await L.api(ADMIN, "GET", "/facturacion/documentos?size=100");
  return L.lista(r.data);
}

(async () => {
  const documentos = await leer();
  const conError = documentos.filter((d) => d.estado === "ERROR");
  console.log("documentos", documentos.length, "con error", conError.length);
  for (const d of conError) {
    const r = await L.api(
      ADMIN,
      "POST",
      "/facturacion/documentos/" + d.id + "/reenviar",
    );
    console.log(
      d.numeroCompleto || d.id,
      r.status,
      JSON.stringify(r.data).slice(0, 160),
    );
  }
  const cuenta = {};
  (await leer()).forEach((d) => {
    cuenta[d.estado] = (cuenta[d.estado] || 0) + 1;
  });
  console.log(JSON.stringify(cuenta));
})();
