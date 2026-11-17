import { horaCorta, lunesDe, severidadAusencia, severidadEmpleado, sumarDias } from './ausencia';

describe('utilidades de recursos humanos', () => {
  it('calcula el lunes de cualquier día de la semana', () => {
    expect(lunesDe(new Date(2026, 10, 18)).getDate()).toBe(16);
    expect(lunesDe(new Date(2026, 10, 22)).getDate()).toBe(16);
    expect(lunesDe(new Date(2026, 10, 16)).getDate()).toBe(16);
  });

  it('suma días sin alterar la fecha original', () => {
    const base = new Date(2026, 10, 28);
    expect(sumarDias(base, 5).getMonth()).toBe(11);
    expect(base.getDate()).toBe(28);
  });

  it('recorta la hora a horas y minutos', () => {
    expect(horaCorta('07:00:00')).toBe('07:00');
  });

  it('asigna una severidad por estado', () => {
    expect(severidadAusencia('APROBADA')).toBe('success');
    expect(severidadAusencia('SOLICITADA')).toBe('warn');
    expect(severidadAusencia('RECHAZADA')).toBe('danger');
    expect(severidadAusencia('CANCELADA')).toBe('secondary');
    expect(severidadEmpleado('ACTIVO')).toBe('success');
    expect(severidadEmpleado('LICENCIA')).toBe('warn');
    expect(severidadEmpleado('CESADO')).toBe('secondary');
  });
});
