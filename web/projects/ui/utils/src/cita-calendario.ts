import { CitaResponse, EstadoCita } from '@tusalud/core';
import { CalEvento, ColorEvento } from '@tusalud/ui/calendario';

const COLOR_ESTADO: Readonly<Record<EstadoCita, ColorEvento>> = {
  PENDIENTE: 'warn',
  CONFIRMADA: 'info',
  ATENDIDA: 'success',
  CANCELADA: 'secondary',
};

const DURACION_POR_DEFECTO = 30;

export function citaAEvento(cita: CitaResponse, mostrar: 'medico' | 'paciente'): CalEvento {
  const inicio = new Date(cita.fechaHora);
  const minutos = cita.duracionMinutos ?? DURACION_POR_DEFECTO;
  return {
    id: cita.id,
    inicio,
    fin: new Date(inicio.getTime() + minutos * 60_000),
    titulo: mostrar === 'medico' ? cita.medicoNombre : cita.pacienteNombre,
    detalle: cita.especialidadNombre ?? cita.especialidad,
    color: COLOR_ESTADO[cita.estado],
    ignoraCruce: cita.estado === 'CANCELADA' || cita.estado === 'ATENDIDA',
    datos: cita,
  };
}
