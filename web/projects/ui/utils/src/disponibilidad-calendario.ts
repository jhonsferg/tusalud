import {
  DIAS_SEMANA,
  DisponibilidadDiaResponse,
  HorarioResponse,
  parseIsoDate,
} from '@tusalud/core';
import { CalHueco, HorarioDia } from '@tusalud/ui/calendario';

const PASO_POR_DEFECTO = 30;

export function construirHuecos(dias: readonly DisponibilidadDiaResponse[]): CalHueco[] {
  const resultado: CalHueco[] = [];
  for (const dia of dias) {
    const base = parseIsoDate(dia.fecha);
    if (!base) {
      continue;
    }
    const inicios = dia.horariosDisponibles.map((hora) => {
      const [h, m] = hora.split(':').map(Number);
      return new Date(base.getFullYear(), base.getMonth(), base.getDate(), h, m);
    });
    let paso = PASO_POR_DEFECTO;
    for (let i = 1; i < inicios.length; i++) {
      const diferencia = (inicios[i].getTime() - inicios[i - 1].getTime()) / 60_000;
      if (diferencia > 0) {
        paso = Math.min(paso, diferencia);
      }
    }
    inicios.forEach((inicio) =>
      resultado.push({ inicio, fin: new Date(inicio.getTime() + paso * 60_000) }),
    );
  }
  return resultado;
}

export function horarioLaboral(horarios: readonly HorarioResponse[]): HorarioDia[] {
  return horarios.map((horario) => ({
    dia: DIAS_SEMANA.indexOf(horario.diaSemana) + 1,
    inicio: horario.horaInicio.slice(0, 5),
    fin: horario.horaFin.slice(0, 5),
  }));
}
