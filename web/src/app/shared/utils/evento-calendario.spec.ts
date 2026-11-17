import { EventoCalendarioResponse } from '@tusalud/app/core/models';
import { eventoApiACalendario } from './evento-calendario';

const base: EventoCalendarioResponse = {
  id: 3,
  titulo: 'Revisión de resultados',
  tipo: 'REUNION',
  inicio: '2026-11-18T10:00:00',
  fin: '2026-11-18T11:00:00',
  todoElDia: false,
  completado: false,
};

describe('eventoApiACalendario', () => {
  it('usa el color del tipo cuando el evento no define uno', () => {
    expect(eventoApiACalendario(base)?.color).toBe('purple');
    expect(eventoApiACalendario({ ...base, color: 'ROJO' })?.color).toBe('danger');
  });

  it('conserva la categoría, la edición y las fechas', () => {
    const evento = eventoApiACalendario(base)!;
    expect(evento.id).toBe('e-3');
    expect(evento.categoria).toBe('REUNION');
    expect(evento.editable).toBe(true);
    expect(evento.fin.getHours()).toBe(11);
  });

  it('descarta fechas inválidas y corrige duraciones vacías', () => {
    expect(eventoApiACalendario({ ...base, inicio: 'no-es-fecha' })).toBeNull();
    const corto = eventoApiACalendario({ ...base, fin: base.inicio })!;
    expect(corto.fin.getTime() - corto.inicio.getTime()).toBe(30 * 60_000);
  });
});
