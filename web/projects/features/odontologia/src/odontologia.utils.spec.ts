import { PiezaOdontograma } from '@tusalud/core';
import {
  FILAS_PERMANENTES,
  FILAS_TEMPORALES,
  descripcionPieza,
  esTemporal,
  estadoDeSuperficie,
  estadoVisible,
  fechaLegible,
  piezaValida,
  poligonosDiente,
  soles,
  vecina,
} from './odontologia.utils';

function pieza(parcial: Partial<PiezaOdontograma>): PiezaOdontograma {
  return {
    pieza: 16,
    dentadura: 'PERMANENTE',
    estado: null,
    observacion: null,
    superficies: {},
    ultimoCambio: null,
    ...parcial,
  };
}

describe('utilidades de odontología', () => {
  it('valida la numeración FDI de las dos denticiones', () => {
    expect([11, 18, 21, 38, 48, 51, 55, 65, 85].every(piezaValida)).toBe(true);
    expect([10, 19, 56, 66, 86, 90, 9].some(piezaValida)).toBe(false);
    expect(esTemporal(55)).toBe(true);
    expect(esTemporal(26)).toBe(false);
  });

  it('las filas del odontograma contienen exactamente las piezas válidas sin repetir', () => {
    const permanentes = FILAS_PERMANENTES.flat();
    const temporales = FILAS_TEMPORALES.flat();

    expect(permanentes).toHaveLength(32);
    expect(temporales).toHaveLength(20);
    expect(new Set([...permanentes, ...temporales]).size).toBe(52);
    expect([...permanentes, ...temporales].every(piezaValida)).toBe(true);
  });

  it('describe una pieza con sus estados para lectores de pantalla', () => {
    expect(descripcionPieza(16, undefined)).toBe('Pieza 16, sin registros');
    expect(descripcionPieza(16, pieza({}))).toBe('Pieza 16, sano');
    expect(descripcionPieza(55, pieza({ pieza: 55, estado: 'AUSENTE' }))).toBe(
      'Pieza 55, temporal, Ausente',
    );
    expect(descripcionPieza(16, pieza({ superficies: { O: 'CARIES', M: 'SANO' } }))).toBe(
      'Pieza 16, Oclusal o incisal caries',
    );
  });

  it('el estado visible y el de superficie recurren a sano cuando no hay registros', () => {
    expect(estadoVisible(undefined)).toBe('SANO');
    expect(estadoVisible(pieza({ estado: 'CORONA' }))).toBe('CORONA');
    expect(estadoDeSuperficie(undefined, 'V')).toBe('SANO');
    expect(estadoDeSuperficie(pieza({ superficies: { V: 'OBTURADO' } }), 'V')).toBe('OBTURADO');
  });

  it('navega entre piezas con las flechas sin salirse de la fila', () => {
    expect(vecina(16, FILAS_PERMANENTES, 1, 0)).toBe(15);
    expect(vecina(11, FILAS_PERMANENTES, 1, 0)).toBe(21);
    expect(vecina(18, FILAS_PERMANENTES, -1, 0)).toBeNull();
    expect(vecina(28, FILAS_PERMANENTES, 1, 0)).toBeNull();
    expect(vecina(16, FILAS_PERMANENTES, 0, 1)).toBe(46);
    expect(vecina(46, FILAS_PERMANENTES, 0, 1)).toBeNull();
    expect(vecina(46, FILAS_PERMANENTES, 0, -1)).toBe(16);
    expect(vecina(99, FILAS_PERMANENTES, 1, 0)).toBeNull();
  });

  it('orienta mesial y distal hacia la línea media según el cuadrante', () => {
    const q1 = poligonosDiente(16);
    const q2 = poligonosDiente(26);

    expect(q1.M).toBe(q2.D);
    expect(q1.D).toBe(q2.M);
    expect(poligonosDiente(16).V).not.toBe(poligonosDiente(46).V);
    expect(poligonosDiente(16).O).toBe('12,12 28,12 28,28 12,28');
  });

  it('da formato a fechas y montos', () => {
    expect(fechaLegible('2026-11-03T10:00:00')).toBe('03/11/2026');
    expect(fechaLegible(null)).toBe('');
    expect(soles(1234.5)).toBe('S/ 1234.50');
  });
});
