import { OrdenImagenResponse } from '@tusalud/core';

export type Severidad = 'success' | 'info' | 'warn' | 'danger' | 'secondary';

export const SEVERIDAD_IMAGEN: Readonly<Record<string, Severidad>> = {
  SOLICITADA: 'warn',
  PROGRAMADA: 'info',
  REALIZADA: 'warn',
  INFORMADA: 'success',
  CANCELADA: 'secondary',
};

export const ICONO_ESTADO_IMAGEN: Readonly<Record<string, string>> = {
  SOLICITADA: 'pi-clock',
  PROGRAMADA: 'pi-calendar',
  REALIZADA: 'pi-hourglass',
  INFORMADA: 'pi-check-circle',
  CANCELADA: 'pi-times-circle',
};

export function tamanoLegible(bytes: number): string {
  if (bytes < 1024) {
    return `${bytes} B`;
  }
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`;
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export function resumenEstudios(orden: OrdenImagenResponse): string {
  return orden.estudios.map((estudio) => estudio.nombre).join(', ');
}

function escapar(texto: string | null | undefined): string {
  return (texto ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function fechaLarga(iso: string | null | undefined): string {
  if (!iso) {
    return '';
  }
  return new Date(iso).toLocaleString('es-PE', { dateStyle: 'long', timeStyle: 'short' });
}

export function construirHtmlInforme(orden: OrdenImagenResponse): string {
  const informe = orden.informe;
  const estudios = orden.estudios.map((estudio) => `<li>${escapar(estudio.nombre)}</li>`).join('');
  const critico = orden.hallazgoCritico
    ? '<p class="critico">Hallazgo crítico comunicado al médico solicitante.</p>'
    : '';
  return `<!doctype html>
<html lang="es">
<head>
<meta charset="utf-8">
<title>Informe ${escapar(orden.codigo)}</title>
<style>
body{font-family:Arial,Helvetica,sans-serif;color:#0f172a;margin:2rem;line-height:1.5}
h1{font-size:1.3rem;margin:0 0 .25rem}
h2{font-size:1rem;margin:1.5rem 0 .4rem;border-bottom:1px solid #cbd5e1;padding-bottom:.2rem}
.cabecera{display:flex;justify-content:space-between;border-bottom:2px solid #0f172a;padding-bottom:.6rem}
.datos{display:grid;grid-template-columns:1fr 1fr;gap:.2rem 2rem;font-size:.9rem}
.critico{color:#b91c1c;font-weight:700}
.firma{margin-top:3rem}
.firma div{border-top:1px solid #0f172a;display:inline-block;padding-top:.3rem;min-width:16rem}
pre{white-space:pre-wrap;font:inherit;margin:0}
</style>
</head>
<body>
<div class="cabecera"><div><h1>TuSalud · Informe de imágenes diagnósticas</h1><div>${escapar(orden.codigo)}</div></div><div>${escapar(fechaLarga(orden.informadaEn))}</div></div>
<div class="datos">
<div><strong>Paciente:</strong> ${escapar(orden.pacienteNombre)}</div>
<div><strong>Médico solicitante:</strong> ${escapar(orden.medicoNombre)}</div>
<div><strong>Fecha del estudio:</strong> ${escapar(fechaLarga(orden.realizadaEn ?? orden.programadaPara))}</div>
<div><strong>Versión del informe:</strong> ${informe?.version ?? ''}</div>
</div>
<h2>Estudios</h2><ul>${estudios}</ul>
${orden.indicacionClinica ? `<h2>Indicación clínica</h2><p>${escapar(orden.indicacionClinica)}</p>` : ''}
<h2>Hallazgos</h2><pre>${escapar(informe?.hallazgos)}</pre>
<h2>Conclusión</h2><pre>${escapar(informe?.conclusion)}</pre>
${critico}
<div class="firma"><div>${escapar(informe?.firmaNombre)}<br>Colegiatura ${escapar(informe?.firmaColegiatura)}</div></div>
</body>
</html>`;
}

export function imprimirInforme(orden: OrdenImagenResponse): void {
  const url = URL.createObjectURL(
    new Blob([construirHtmlInforme(orden)], { type: 'text/html;charset=utf-8' }),
  );
  const ventana = window.open(url, '_blank');
  if (!ventana) {
    URL.revokeObjectURL(url);
    return;
  }
  ventana.addEventListener('load', () => {
    ventana.print();
    setTimeout(() => URL.revokeObjectURL(url), 60_000);
  });
}
