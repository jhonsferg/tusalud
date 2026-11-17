import {
  CalEvento,
  ajustarMinutos,
  fechaConMinutos,
  minutosDeHora,
  numeroDeSemana,
  recortarAlDia,
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

  it('calcula el rango de la vista de agenda', () => {
    const rango = rangoVisible('agenda', new Date(2026, 10, 18));
    expect(rango.inicio.getDate()).toBe(18);
    expect(Math.round((rango.fin.getTime() - rango.inicio.getTime()) / 86_400_000)).toBe(29);
    expect(moverFecha('agenda', new Date(2026, 10, 18), 1).getMonth()).toBe(11);
  });

  it('recorta los eventos que cruzan la medianoche', () => {
    const noche = {
      ...evento(1, '22:00', '23:00'),
      fin: new Date('2026-11-19T02:00:00'),
    };
    const primero = recortarAlDia(noche, new Date(2026, 10, 18))!;
    const segundo = recortarAlDia(noche, new Date(2026, 10, 19))!;
    expect(primero.fin.getHours()).toBe(0);
    expect(primero.fin.getDate()).toBe(19);
    expect(segundo.inicio.getHours()).toBe(0);
    expect(segundo.fin.getHours()).toBe(2);
    expect(recortarAlDia(noche, new Date(2026, 10, 20))).toBeNull();
  });

  it('ajusta minutos al paso y construye fechas desde los minutos del día', () => {
    expect(ajustarMinutos(67, 15)).toBe(60);
    expect(ajustarMinutos(68, 15)).toBe(75);
    const fecha = fechaConMinutos(new Date(2026, 10, 18), 14 * 60 + 30);
    expect([fecha.getHours(), fecha.getMinutes()]).toEqual([14, 30]);
    expect(minutosDeHora('08:15')).toBe(495);
  });

  it('numera las semanas del año según ISO 8601', () => {
    expect(numeroDeSemana(new Date(2026, 0, 1))).toBe(1);
    expect(numeroDeSemana(new Date(2026, 10, 18))).toBe(47);
  });
});
