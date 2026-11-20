import { etiquetaPeriodo, inicioDeMes, minutosAHoras, rangoDelMes } from './planilla.utils';

describe('utilidades de planilla', () => {
  it('formatea los minutos como horas y minutos', () => {
    expect(minutosAHoras(0)).toBe('0 h 00 min');
    expect(minutosAHoras(95)).toBe('1 h 35 min');
    expect(minutosAHoras(480)).toBe('8 h 00 min');
  });

  it('etiqueta el periodo con el mes en español', () => {
    expect(etiquetaPeriodo(2026, 3)).toBe('Marzo 2026');
    expect(etiquetaPeriodo(2026, 12)).toBe('Diciembre 2026');
  });

  it('el rango del mes empieza el día uno', () => {
    expect(inicioDeMes(new Date(2026, 5, 18)).getDate()).toBe(1);
    expect(rangoDelMes(new Date(2026, 5, 18)).desde).toBe('2026-06-01');
  });
});
