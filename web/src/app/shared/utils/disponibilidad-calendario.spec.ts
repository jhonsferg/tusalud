import { construirHuecos, horarioLaboral } from './disponibilidad-calendario';

function u(n: number): string {
  return `00000000-0000-4000-8000-${String(n).padStart(12, '0')}`;
}

describe('disponibilidad para el calendario', () => {
  it('convierte las horas libres en huecos con el paso mínimo del día', () => {
    const huecos = construirHuecos([
      {
        fecha: '2026-11-18',
        horariosDisponibles: ['08:00:00', '08:30:00', '09:30:00'],
        totalHoras: 3,
      },
    ]);
    expect(huecos).toHaveLength(3);
    expect(huecos[0].inicio.getHours()).toBe(8);
    expect(huecos[0].fin.getTime() - huecos[0].inicio.getTime()).toBe(30 * 60_000);
  });

  it('asigna 30 minutos cuando solo hay una hora libre', () => {
    const [hueco] = construirHuecos([
      { fecha: '2026-11-18', horariosDisponibles: ['10:00:00'], totalHoras: 1 },
    ]);
    expect(hueco.fin.getTime() - hueco.inicio.getTime()).toBe(30 * 60_000);
  });

  it('traduce los horarios de atención a días ISO', () => {
    expect(
      horarioLaboral([
        {
          id: u(1),
          medicoId: u(1),
          diaSemana: 'MONDAY',
          horaInicio: '08:00:00',
          horaFin: '13:00:00',
          duracionCitaMinutos: 30,
        },
        {
          id: u(2),
          medicoId: u(1),
          diaSemana: 'SUNDAY',
          horaInicio: '09:00',
          horaFin: '10:00',
          duracionCitaMinutos: 30,
        },
      ]),
    ).toEqual([
      { dia: 1, inicio: '08:00', fin: '13:00' },
      { dia: 7, inicio: '09:00', fin: '10:00' },
    ]);
  });
});
