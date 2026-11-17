import {
  CalEvento,
  contarCruces,
  distribuir,
  inicioDeSemana,
  moverFecha,
  rangoVisible,
  solapan,
} from './calendario.utils';

function evento(
  id: number,
  inicio: string,
  fin: string,
  extra: Partial<CalEvento> = {},
): CalEvento {
  return {
    id,
    inicio: new Date(`2026-11-18T${inicio}:00`),
    fin: new Date(`2026-11-18T${fin}:00`),
    titulo: `Cita ${id}`,
    color: 'info',
    ...extra,
  };
}

describe('calendario.utils', () => {
  it('calcula el inicio de semana en lunes', () => {
    expect(inicioDeSemana(new Date(2026, 10, 18)).getDate()).toBe(16);
    expect(inicioDeSemana(new Date(2026, 10, 22)).getDate()).toBe(16);
    expect(inicioDeSemana(new Date(2026, 10, 16)).getDate()).toBe(16);
  });

  it('calcula el rango visible de cada vista', () => {
    const fecha = new Date(2026, 10, 18);
    expect(rangoVisible('dia', fecha).inicio.getDate()).toBe(18);
    const semana = rangoVisible('semana', fecha);
    expect([semana.inicio.getDate(), semana.fin.getDate()]).toEqual([16, 22]);
    const mes = rangoVisible('mes', fecha);
    expect(mes.inicio.getTime()).toBe(new Date(2026, 9, 26).getTime());
    expect(mes.fin.getTime()).toBe(new Date(2026, 11, 6).getTime());
  });

  it('mueve la fecha según la vista', () => {
    const fecha = new Date(2026, 10, 18);
    expect(moverFecha('dia', fecha, 1).getDate()).toBe(19);
    expect(moverFecha('semana', fecha, -1).getDate()).toBe(11);
    expect(moverFecha('mes', fecha, 1).getMonth()).toBe(11);
  });

  it('distingue solapes parciales, exactos y adyacentes', () => {
    expect(solapan(evento(1, '09:00', '09:30'), evento(2, '09:00', '09:30'))).toBe(true);
    expect(solapan(evento(1, '09:00', '09:30'), evento(2, '09:15', '09:45'))).toBe(true);
    expect(solapan(evento(1, '09:00', '09:30'), evento(2, '09:30', '10:00'))).toBe(false);
  });

  it('reparte columnas y marca los cruces', () => {
    const resultado = distribuir([
      evento(1, '09:00', '09:30'),
      evento(2, '09:15', '09:45'),
      evento(3, '10:00', '10:30'),
    ]);
    const porId = new Map(resultado.map((item) => [item.evento.id, item]));
    expect(porId.get(1)).toMatchObject({ columna: 0, columnas: 2, conflicto: true });
    expect(porId.get(2)).toMatchObject({ columna: 1, columnas: 2, conflicto: true });
    expect(porId.get(3)).toMatchObject({ columna: 0, columnas: 1, conflicto: false });
  });

  it('ignora los eventos que no cuentan para el cruce', () => {
    const eventos = [
      evento(1, '09:00', '09:30'),
      evento(2, '09:00', '09:30', { ignoraCruce: true }),
    ];
    expect(contarCruces(eventos)).toBe(0);
    expect(contarCruces([evento(1, '09:00', '10:00'), evento(2, '09:30', '10:30')])).toBe(2);
  });
});
