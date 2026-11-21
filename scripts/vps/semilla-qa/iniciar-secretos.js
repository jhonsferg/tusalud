const fs = require("fs");
const path = require("path");
const crypto = require("crypto");

const ruta =
  process.env.TUSALUD_SECRETOS || path.join(__dirname, "secretos.local.json");
let actual = {};
try {
  actual = JSON.parse(fs.readFileSync(ruta, "utf8"));
} catch (e) {
  actual = {};
}

function generar() {
  const mayus = "ABCDEFGHJKLMNPQRSTUVWXYZ";
  const minus = "abcdefghijkmnopqrstuvwxyz";
  const nums = "23456789";
  const simb = "#$%&*+-=?@";
  const todos = mayus + minus + nums;
  const pick = (s) => s[crypto.randomInt(s.length)];
  const c = [pick(mayus), pick(minus), pick(nums), pick(simb)];
  while (c.length < 20) c.push(pick(todos));
  for (let i = c.length - 1; i > 0; i--) {
    const j = crypto.randomInt(i + 1);
    [c[i], c[j]] = [c[j], c[i]];
  }
  return c.join("");
}

const entrada = {
  admin: process.env.CLAVE_ADMIN_DEMO,
  medico: process.env.CLAVE_MEDICO_DEMO,
  paciente: process.env.CLAVE_PACIENTE_DEMO,
  personal: process.env.CLAVE_PERSONAL_DEMO,
};
for (const k of Object.keys(entrada)) {
  if (entrada[k]) actual[k] = entrada[k];
}
if (!actual.adminQa) actual.adminQa = generar();
fs.writeFileSync(ruta, JSON.stringify(actual, null, 2), "utf8");
console.log("claves definidas:", Object.keys(actual).join(", "));
