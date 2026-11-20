import {
  ETIQUETA_ESTADO_EVENTO,
  ETIQUETA_SEVERIDAD,
  barraProporcional,
} from '@tusalud/features/seguridad-paciente';

describe('seguridad del paciente', () => {
  it('calcula el porcentaje de una barra sin dividir por cero', () => {
    expect(barraProporcional(1, 4)).toBe(25);
    expect(barraProporcional(3, 3)).toBe(100);
    expect(barraProporcional(0, 0)).toBe(0);
  });

  it('etiqueta todos los estados y severidades', () => {
    expect(Object.keys(ETIQUETA_ESTADO_EVENTO)).toEqual([
      'NOTIFICADO',
      'EN_ANALISIS',
      'EN_ACCION',
      'CERRADO',
    ]);
    expect(ETIQUETA_SEVERIDAD.CENTINELA).toBe('Centinela');
  });
});
