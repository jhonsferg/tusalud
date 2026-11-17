import { createReadStream, existsSync, readdirSync, statSync } from 'node:fs';
import http from 'node:http';
import { extname, join, normalize, resolve, sep } from 'node:path';
import { fileURLToPath } from 'node:url';

const raiz = resolve(fileURLToPath(new URL('..', import.meta.url)), 'dist');
const puerto = Number(process.env.PORT ?? 4300);
const destinoApi = new URL(process.env.API_UPSTREAM ?? 'http://localhost:8080');

const tipos = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.mjs': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.ico': 'image/x-icon',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
  '.ttf': 'font/ttf',
  '.map': 'application/json',
  '.txt': 'text/plain; charset=utf-8',
};

const apps = readdirSync(raiz)
  .filter((nombre) => existsSync(join(raiz, nombre, 'browser', 'index.html')))
  .map((nombre) => ({ nombre, dir: join(raiz, nombre, 'browser') }));

const portal = apps.find((app) => app.nombre === 'portal');
const modulos = apps.filter((app) => app.nombre !== 'portal');

function archivo(dir, ruta) {
  const destino = normalize(join(dir, ruta));
  if (destino !== dir && !destino.startsWith(dir + sep)) {
    return null;
  }
  try {
    return statSync(destino).isFile() ? destino : null;
  } catch {
    return null;
  }
}

function enviar(res, ruta, sinCache) {
  const tipo = tipos[extname(ruta).toLowerCase()] ?? 'application/octet-stream';
  res.writeHead(200, {
    'Content-Type': tipo,
    'Cache-Control': sinCache ? 'no-cache' : 'public, max-age=31536000, immutable',
  });
  createReadStream(ruta).pipe(res);
}

function servir(res, app, ruta) {
  const exacto = ruta === '' || ruta.endsWith('/') ? null : archivo(app.dir, ruta);
  if (exacto) {
    enviar(res, exacto, exacto.endsWith('.html'));
    return;
  }
  if (extname(ruta) && extname(ruta) !== '.html') {
    res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
    res.end('No encontrado');
    return;
  }
  enviar(res, join(app.dir, 'index.html'), true);
}

function sinOrigen(cabeceras) {
  const { origin, referer, ...resto } = cabeceras;
  return { ...resto, host: destinoApi.host };
}

function proxy(req, res) {
  const entrante = http.request(
    {
      hostname: destinoApi.hostname,
      port: destinoApi.port || 80,
      path: req.url,
      method: req.method,
      headers: sinOrigen(req.headers),
    },
    (respuesta) => {
      const cabeceras = { ...respuesta.headers };
      if ((cabeceras['content-type'] ?? '').includes('text/event-stream')) {
        cabeceras['cache-control'] = 'no-cache';
        cabeceras['x-accel-buffering'] = 'no';
      }
      res.writeHead(respuesta.statusCode ?? 502, cabeceras);
      respuesta.pipe(res);
    },
  );
  entrante.on('error', () => {
    if (!res.headersSent) {
      res.writeHead(502, { 'Content-Type': 'text/plain; charset=utf-8' });
    }
    res.end('API no disponible');
  });
  res.on('close', () => entrante.destroy());
  req.pipe(entrante);
}

const servidor = http.createServer((req, res) => {
  const url = new URL(req.url ?? '/', 'http://localhost');
  const ruta = decodeURIComponent(url.pathname);
  if (ruta.startsWith('/api/') || ruta === '/api') {
    proxy(req, res);
    return;
  }
  const primero = ruta.split('/').filter(Boolean)[0];
  const modulo = modulos.find((app) => app.nombre === primero);
  if (modulo) {
    if (ruta === `/${modulo.nombre}`) {
      res.writeHead(301, { Location: `/${modulo.nombre}/${url.search}` });
      res.end();
      return;
    }
    servir(res, modulo, ruta.slice(modulo.nombre.length + 2));
    return;
  }
  if (!portal) {
    res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
    res.end('Falta dist/portal');
    return;
  }
  servir(res, portal, ruta.replace(/^\/+/, ''));
});

servidor.requestTimeout = 0;
servidor.listen(puerto, () => {
  console.log(
    `TuSalud sirviendo ${apps.map((app) => app.nombre).join(', ')} en http://localhost:${puerto}`,
  );
});
