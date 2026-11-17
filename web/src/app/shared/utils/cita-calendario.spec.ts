import { CitaResponse } from '@tusalud/app/core/models';
import { citaAEvento } from './cita-calendario';

const cita = {
  id: u(7),
  fechaHora: '2026-11-18T09:00:00',
  estado: 'PENDIENTE',
  medicoNombre: 'Luis García',
  pacienteNombre: 'Juan Pérez',
  especialidad: 'Cardiología',
  especialidadNombre: 'Cardiología',
  duracionMinutos: 45,
} as CitaResponse;

function u(n: number): string {
  return `00000000-0000-4000-8000-${String(n).padStart(12, '0')}`;
}

describe('citaAEvento', () => {
  it('usa la duración de la cita y el nombre según quién la mira', () => {
    const paraPaciente = citaAEvento(cita, 'medico');
    expect(paraPaciente.titulo).toBe('Luis García');
    expect(paraPaciente.fin.getTime() - paraPaciente.inicio.getTime()).toBe(45 * 60_000);
    expect(citaAEvento(cita, 'paciente').titulo).toBe('Juan Pérez');
  });

  it('asume 30 minutos y no cuenta para cruces las citas canceladas', () => {
    const cancelada = citaAEvento(
      { ...cita, estado: 'CANCELADA', duracionMinutos: undefined },
      'medico',
    );
    expect(cancelada.fin.getTime() - cancelada.inicio.getTime()).toBe(30 * 60_000);
    expect(cancelada.ignoraCruce).toBe(true);
    expect(citaAEvento(cita, 'medico').ignoraCruce).toBe(false);
  });
});
