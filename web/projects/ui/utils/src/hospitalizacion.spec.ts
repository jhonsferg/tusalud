import { NIVELES_TRIAJE, colorCama } from './hospitalizacion';

describe('hospitalizacion utils', () => {
  it('asigna un color por estado de cama', () => {
    expect(colorCama('LIBRE')).toContain('green');
    expect(colorCama('OCUPADA')).toContain('red');
    expect(colorCama('LIMPIEZA')).toContain('orange');
    expect(colorCama('MANTENIMIENTO')).toContain('surface');
  });

  it('define los cinco niveles de triaje en orden', () => {
    expect(NIVELES_TRIAJE.map((n) => n.nivel)).toEqual([1, 2, 3, 4, 5]);
    expect(NIVELES_TRIAJE[0].clasificacion).toBe('ROJO');
    expect(NIVELES_TRIAJE[4].clasificacion).toBe('AZUL');
  });
});
