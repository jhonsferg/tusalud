import { CirugiaResumen } from '@tusalud/core';
import {
  ITEMS_CHECKLIST,
  aIsoLocal,
  cirugiaAEvento,
  esRolMedico,
  faltantesChecklist,
  momentoDisponible,
  puedeCancelarse,
  puedeProgramarse,
  puedeSuspenderse,
  tituloEvento,
} from './quirofano.utils';

function cirugia(parcial: Partial<CirugiaResumen> = {}): CirugiaResumen {
  return {
    id: 'c1',
    codigo: 'CIR-000001',
    estado: 'PROGRAMADA',
    tipo: 'ELECTIVA',
    quirofanoId: 'q1',
    quirofanoNombre: 'Quirófano 1',
    inicio: '2030-01-10T08:00:00',
    fin: '2030-01-10T10:00:00',
    detalleVisible: true,
    pacienteId: 'p1',
    pacienteNombre: 'Ana Ruiz',
    procedimiento: 'Apendicectomía',
    solicitanteNombre: 'Luis García',
    enMiEquipo: false,
    ...parcial,
  };
}

describe('utilidades de quirófano', () => {
  it('distingue los roles que corresponden a médicos', () => {
    expect(esRolMedico('CIRUJANO')).toBe(true);
    expect(esRolMedico('ANESTESIOLOGO')).toBe(true);
    expect(esRolMedico('INSTRUMENTISTA')).toBe(false);
    expect(esRolMedico('CIRCULANTE')).toBe(false);
  });

  it('muestra reservado cuando no hay permiso para ver el detalle', () => {
    expect(tituloEvento(cirugia())).toBe('Quirófano 1 · Apendicectomía');
    expect(tituloEvento(cirugia({ detalleVisible: false, procedimiento: null }))).toBe(
      'Quirófano 1 · Reservado',
    );
    expect(tituloEvento(cirugia({ quirofanoNombre: null }))).toContain('Sin quirófano');
  });

  it('convierte una cirugía programada en evento y omite la que no tiene horario', () => {
    const evento = cirugiaAEvento(cirugia());
    expect(evento?.color).toBe('info');
    expect(evento?.inicio.getHours()).toBe(8);
    expect(cirugiaAEvento(cirugia({ inicio: null, fin: null }))).toBeNull();
  });

  it('formatea la fecha local sin zona horaria', () => {
    expect(aIsoLocal(new Date(2030, 0, 5, 7, 5))).toBe('2030-01-05T07:05:00');
  });

  it('detecta los puntos que faltan en cada lista de verificación', () => {
    const todos = new Set(ITEMS_CHECKLIST.ANTES_INCISION);
    expect(faltantesChecklist('ANTES_INCISION', todos)).toEqual([]);
    const casi = new Set(ITEMS_CHECKLIST.ANTES_INCISION.slice(1));
    expect(faltantesChecklist('ANTES_INCISION', casi)).toEqual([ITEMS_CHECKLIST.ANTES_INCISION[0]]);
  });

  it('habilita cada lista solo en su momento y en orden', () => {
    const ninguno = new Set<'ANTES_INDUCCION' | 'ANTES_INCISION' | 'ANTES_SALIDA'>();
    expect(momentoDisponible('ANTES_INDUCCION', 'EN_PREPARACION', ninguno)).toBe(true);
    expect(momentoDisponible('ANTES_INDUCCION', 'PROGRAMADA', ninguno)).toBe(false);
    expect(momentoDisponible('ANTES_INCISION', 'EN_CURSO', ninguno)).toBe(false);
    expect(momentoDisponible('ANTES_INCISION', 'EN_CURSO', new Set(['ANTES_INDUCCION']))).toBe(
      true,
    );
    expect(momentoDisponible('ANTES_SALIDA', 'EN_CURSO', new Set(['ANTES_INDUCCION']))).toBe(false);
    expect(
      momentoDisponible('ANTES_SALIDA', 'EN_CURSO', new Set(['ANTES_INDUCCION', 'ANTES_INCISION'])),
    ).toBe(true);
    expect(momentoDisponible('ANTES_INDUCCION', 'EN_CURSO', new Set(['ANTES_INDUCCION']))).toBe(
      false,
    );
  });

  it('aplica las reglas de estado para programar, suspender y cancelar', () => {
    expect(puedeProgramarse('SOLICITADA')).toBe(true);
    expect(puedeProgramarse('EN_CURSO')).toBe(false);
    expect(puedeSuspenderse('EN_CURSO')).toBe(true);
    expect(puedeSuspenderse('SOLICITADA')).toBe(false);
    expect(puedeCancelarse('SUSPENDIDA')).toBe(true);
    expect(puedeCancelarse('EN_CURSO')).toBe(false);
    expect(puedeCancelarse('FINALIZADA')).toBe(false);
  });
});
