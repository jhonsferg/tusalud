import { AlertaReceta } from '@tusalud/core';
import {
  cantidadPendiente,
  diasRestantes,
  dosisDiariaMg,
  justificacionesFaltantes,
  ordenarAlertas,
  textoVigencia,
} from './recetas.utils';

function alerta(
  clave: string,
  severidad: AlertaReceta['severidad'],
  tipo: AlertaReceta['tipo'] = 'INTERACCION',
): AlertaReceta {
  return {
    clave,
    tipo,
    severidad,
    mensaje: clave,
    recomendacion: null,
    medicamento: 'Medicamento',
    requiereJustificacion: severidad === 'GRAVE' || severidad === 'CONTRAINDICADA',
    minimoJustificacion: severidad === 'CONTRAINDICADA' ? 30 : severidad === 'GRAVE' ? 10 : 0,
  };
}

describe('recetas.utils', () => {
  it('ordena las alertas de la más a la menos severa', () => {
    const ordenadas = ordenarAlertas([
      alerta('c', 'INFORMATIVA'),
      alerta('a', 'GRAVE'),
      alerta('d', 'MODERADA'),
      alerta('b', 'CONTRAINDICADA'),
    ]);

    expect(ordenadas.map((a) => a.severidad)).toEqual([
      'CONTRAINDICADA',
      'GRAVE',
      'MODERADA',
      'INFORMATIVA',
    ]);
  });

  it('no modifica la lista original al ordenar', () => {
    const original = [alerta('a', 'INFORMATIVA'), alerta('b', 'GRAVE')];

    ordenarAlertas(original);

    expect(original[0].clave).toBe('a');
  });

  it('exige justificación solo en las alertas graves y contraindicadas con el mínimo de caracteres', () => {
    const alertas = [
      alerta('leve', 'INFORMATIVA'),
      alerta('moderada', 'MODERADA'),
      alerta('grave', 'GRAVE'),
      alerta('contra', 'CONTRAINDICADA'),
    ];

    expect(justificacionesFaltantes(alertas, {}).map((a) => a.clave)).toEqual(['grave', 'contra']);
    expect(
      justificacionesFaltantes(alertas, { grave: 'Control del INR semanal', contra: 'corto' }).map(
        (a) => a.clave,
      ),
    ).toEqual(['contra']);
    expect(
      justificacionesFaltantes(alertas, {
        grave: 'Control del INR semanal',
        contra: 'Alergia descartada con una prueba cutánea negativa reciente',
      }),
    ).toEqual([]);
  });

  it('ignora los espacios al medir la justificación', () => {
    const alertas = [alerta('grave', 'GRAVE')];

    expect(justificacionesFaltantes(alertas, { grave: '          ab        ' })).toHaveLength(1);
  });

  it('calcula la dosis diaria total solo con datos válidos', () => {
    expect(dosisDiariaMg(500, 3)).toBe(1500);
    expect(dosisDiariaMg(0.25, 3)).toBe(0.75);
    expect(dosisDiariaMg(null, 3)).toBeNull();
    expect(dosisDiariaMg(500, undefined)).toBeNull();
    expect(dosisDiariaMg(0, 3)).toBeNull();
  });

  it('cuenta los días restantes de vigencia sin bajar de cero', () => {
    const ahora = new Date('2026-11-20T10:00:00');

    expect(diasRestantes('2026-11-25T10:00:00', ahora)).toBe(5);
    expect(diasRestantes('2026-11-19T10:00:00', ahora)).toBe(0);
  });

  it('describe la vigencia en palabras', () => {
    const ahora = new Date('2026-11-20T10:00:00');

    expect(textoVigencia(true, '2026-11-27T10:00:00', ahora)).toBe('Vigente 7 días más');
    expect(textoVigencia(true, '2026-11-20T20:00:00', ahora)).toBe('Vence hoy o mañana');
    expect(textoVigencia(false, '2026-11-10T10:00:00', ahora)).toBe('Vencida');
  });

  it('suma lo pendiente de dispensar de todos los ítems', () => {
    const receta = {
      items: [{ pendiente: 4 }, { pendiente: 0 }, { pendiente: 6 }],
    } as unknown as Parameters<typeof cantidadPendiente>[0];

    expect(cantidadPendiente(receta)).toBe(10);
  });
});
