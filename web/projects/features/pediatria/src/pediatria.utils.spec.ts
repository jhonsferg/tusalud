import { CurvaCrecimiento, PuntoCrecimiento, ReferenciaPunto } from '@tusalud/core';
import {
  edadLegible,
  escalasDeCurva,
  fechaLegible,
  posicionX,
  posicionY,
  referenciaVisible,
  resumenAccesible,
  trazo,
} from './pediatria.utils';

const referencia: ReferenciaPunto[] = [0, 12, 24, 60].map((edad) => ({
  edadMeses: edad,
  p3: 2 + edad / 10,
  p15: 2.5 + edad / 10,
  p50: 3 + edad / 10,
  p85: 3.5 + edad / 10,
  p97: 4 + edad / 10,
}));

function curva(puntos: Partial<PuntoCrecimiento>[]): CurvaCrecimiento {
  return {
    indicador: 'PESO_EDAD',
    etiqueta: 'Peso para la edad',
    unidad: 'kg',
    referencia,
    puntos: puntos.map((p, i) => ({
      medicionId: `m${i}`,
      fecha: '2026-01-01',
      edadMeses: 0,
      valor: 3,
      z: 0,
      percentil: 50,
      nivel: 'NORMAL',
      mensaje: '',
      ...p,
    })),
  };
}

describe('utilidades de pediatría', () => {
  it('formatea la fecha ISO como día mes año', () => {
    expect(fechaLegible('2026-03-07')).toBe('07/03/2026');
    expect(fechaLegible('2026-03-07T10:20:00')).toBe('07/03/2026');
    expect(fechaLegible(null)).toBe('');
  });

  it('expresa la edad en meses o en años y meses', () => {
    expect(edadLegible(1)).toBe('1 mes');
    expect(edadLegible(8)).toBe('8 meses');
    expect(edadLegible(24)).toBe('2 años');
    expect(edadLegible(30)).toBe('2 años y 6 meses');
    expect(edadLegible(null)).toBe('Edad no registrada');
  });

  it('recorta la referencia a las edades visibles y conserva el siguiente punto', () => {
    expect(referenciaVisible(referencia, 14).map((r) => r.edadMeses)).toEqual([0, 12, 24]);
    expect(referenciaVisible(referencia, 100).map((r) => r.edadMeses)).toEqual([0, 12, 24, 60]);
  });

  it('calcula escalas que contienen todas las mediciones y la referencia', () => {
    const escalas = escalasDeCurva(
      curva([
        { edadMeses: 10, valor: 9.5 },
        { edadMeses: 11, valor: 1.2 },
      ]),
    );

    expect(escalas).not.toBeNull();
    expect(escalas!.xMin).toBe(0);
    expect(escalas!.yMin).toBeLessThan(1.2);
    expect(escalas!.yMax).toBeGreaterThan(9.5);
  });

  it('no hay escalas cuando la referencia tiene menos de dos puntos', () => {
    expect(escalasDeCurva({ ...curva([]), referencia: referencia.slice(0, 1) })).toBeNull();
  });

  it('proyecta valores dentro del lienzo y mantiene el eje vertical invertido', () => {
    const escalas = { xMin: 0, xMax: 10, yMin: 0, yMax: 100 };
    const lienzo = { ancho: 400, alto: 200, margen: 20 };

    expect(posicionX(0, escalas, lienzo)).toBe(20);
    expect(posicionX(10, escalas, lienzo)).toBe(380);
    expect(posicionY(0, escalas, lienzo)).toBe(180);
    expect(posicionY(100, escalas, lienzo)).toBe(20);
    expect(posicionY(50, escalas, lienzo)).toBe(100);
  });

  it('construye el trazo de una polilínea', () => {
    expect(
      trazo([
        { x: 1, y: 2 },
        { x: 3.456, y: 4 },
      ]),
    ).toBe('M1.0 2.0 L3.5 4.0');
    expect(trazo([])).toBe('');
  });

  it('resume la curva para lectores de pantalla', () => {
    expect(resumenAccesible(curva([]))).toContain('sin mediciones');
    expect(resumenAccesible(curva([{ valor: 9.4, percentil: 47.6 }]))).toBe(
      'Peso para la edad: 1 medición, la última de 9.4 kg, percentil 48',
    );
  });
});
