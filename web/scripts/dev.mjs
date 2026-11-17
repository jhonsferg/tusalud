import { spawn } from 'node:child_process';

const todas = [
  'portal',
  'paciente',
  'medico',
  'admin',
  'recepcion',
  'caja',
  'enfermeria',
  'laboratorio',
  'farmacia',
  'almacen',
];

const pedidas = (process.env.WATCH ?? '')
  .split(',')
  .map((nombre) => nombre.trim())
  .filter(Boolean);
const vigiladas = pedidas.includes('all') ? todas : todas.filter((app) => pedidas.includes(app));
const sondeo = process.env.POLL ?? '1000';

const hijos = [];

function lanzar(nombre, argumentos, esperarFin = false) {
  return new Promise((resolver) => {
    const hijo = spawn('npx', argumentos, { stdio: ['ignore', 'pipe', 'pipe'], shell: false });
    hijos.push(hijo);
    let avisado = false;
    const escuchar = (flujo, destino) => {
      flujo.on('data', (trozo) => {
        const texto = trozo.toString();
        destino.write(texto.replace(/^/gm, `[${nombre}] `));
        if (!esperarFin && !avisado && /bundle generation complete|ERROR/i.test(texto)) {
          avisado = true;
          resolver();
        }
      });
    };
    escuchar(hijo.stdout, process.stdout);
    escuchar(hijo.stderr, process.stderr);
    hijo.on('exit', (codigo) => {
      if (!avisado) {
        avisado = true;
        resolver();
      }
      console.log(`[${nombre}] terminó con código ${codigo}`);
    });
  });
}

function cerrar() {
  for (const hijo of hijos) {
    hijo.kill('SIGTERM');
  }
  process.exit(0);
}
process.on('SIGTERM', cerrar);
process.on('SIGINT', cerrar);

lanzar('gateway', ['node', 'scripts/serve-dist.mjs']);

for (const app of todas.filter((nombre) => !vigiladas.includes(nombre))) {
  await lanzar(app, ['ng', 'build', app, '--configuration', 'local'], true);
}

for (const app of vigiladas) {
  await lanzar(app, ['ng', 'build', app, '--configuration', 'local', '--watch', '--poll', sondeo]);
}

console.log(
  vigiladas.length
    ? `Aplicaciones compiladas. En recarga: ${vigiladas.join(', ')}`
    : 'Aplicaciones compiladas. Sin recarga: define WEB_WATCH para vigilar alguna.',
);
