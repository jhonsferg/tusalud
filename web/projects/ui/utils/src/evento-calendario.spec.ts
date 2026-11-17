import { EventoCalendarioResponse } from '@tusalud/core';
import { categoriasPara, eventoApiACalendario, tiposPermitidos } from './evento-calendario';

const base: EventoCalendarioResponse = {
  id: u(3),
  titulo: 'Revisión de resultados',
  tipo: 'REUNION',
  inicio: '2026-11-18T10:00:00',
  fin: '2026-11-18T11:00:00',
  todoElDia: false,
  completado: false,
};

function u(n: number): string {
  return `00000000-0000-4000-8000-${String(n).padStart(12, '0')}`;
}

describe('eventoApiACalendario', () => {
  it('usa el color del tipo cuando el evento no define uno', () => {
    expect(eventoApiACalendario(base)?.color).toBe('purple');
    expect(eventoApiACalendario({ ...base, color: 'ROJO' })?.color).toBe('danger');
  });

  it('conserva la categoría, la edición y las fechas', () => {
    const evento = eventoApiACalendario(base)!;
    expect(evento.id).toBe(`e-${u(3)}`);
    expect(evento.categoria).toBe('REUNION');
    expect(evento.editable).toBe(true);
    expect(evento.fin.getHours()).toBe(11);
  });

  it('descarta fechas inválidas y corrige duraciones vacías', () => {
    expect(eventoApiACalendario({ ...base, inicio: 'no-es-fecha' })).toBeNull();
    const corto = eventoApiACalendario({ ...base, fin: base.inicio })!;
    expect(corto.fin.getTime() - corto.inicio.getTime()).toBe(30 * 60_000);
  });

  it('limita los tipos de evento según el rol', () => {
    expect(tiposPermitidos(['ROLE_PACIENTE'])).toEqual(['PERSONAL', 'RECORDATORIO']);
    expect(tiposPermitidos(['ROLE_ADMIN'])).toEqual([
      'REUNION',
      'TAREA',
      'RECORDATORIO',
      'PERSONAL',
    ]);
    expect(tiposPermitidos(['ROLE_MEDICO'])).toEqual([
      'REUNION',
      'TAREA',
      'RECORDATORIO',
      'PERSONAL',
      'BLOQUEO',
    ]);
    expect(tiposPermitidos(['ROLE_PACIENTE', 'ROLE_ADMIN'])).toContain('REUNION');
  });

  it('ofrece calendarios acordes al rol', () => {
    expect(categoriasPara(['ROLE_PACIENTE']).map((c) => c.clave)).toEqual([
      'CITA',
      'RECORDATORIO',
      'PERSONAL',
    ]);
    expect(categoriasPara(['ROLE_ADMIN']).map((c) => c.clave)).not.toContain('CITA');
    expect(categoriasPara(['ROLE_MEDICO']).map((c) => c.clave)).toContain('BLOQUEO');
  });
});
