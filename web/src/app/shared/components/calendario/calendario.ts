import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  effect,
  inject,
  input,
  model,
  output,
  signal,
} from '@angular/core';

import {
  CalEvento,
  CalHueco,
  EventoPosicionado,
  RangoVisible,
  VistaCalendario,
  contarCruces,
  diasDeRango,
  distribuir,
  duracionMinutos,
  inicioDelDia,
  mismoDia,
  minutosDesde,
  moverFecha,
  rangoVisible,
  solapan,
} from './calendario.utils';

interface EventoDibujado {
  readonly evento: CalEvento;
  readonly top: number;
  readonly alto: number;
  readonly izquierda: number;
  readonly ancho: number;
  readonly conflicto: boolean;
}

interface HuecoDibujado {
  readonly hueco: CalHueco;
  readonly top: number;
  readonly alto: number;
  readonly elegido: boolean;
}

interface ColumnaDia {
  readonly fecha: Date;
  readonly esHoy: boolean;
  readonly eventos: EventoDibujado[];
  readonly huecos: HuecoDibujado[];
}

interface CeldaMes {
  readonly fecha: Date;
  readonly delMes: boolean;
  readonly esHoy: boolean;
  readonly eventos: CalEvento[];
  readonly extra: number;
  readonly libres: number;
  readonly cruces: number;
}

const ALTO_HORA = 56;
const MAX_EVENTOS_MES = 3;
const DIAS_CORTOS = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'];

@Component({
  selector: 'app-calendario',
  imports: [DatePipe],
  templateUrl: './calendario.html',
  styleUrl: './calendario.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Calendario {
  readonly eventos = input<readonly CalEvento[]>([]);
  readonly huecos = input<readonly CalHueco[] | null>(null);
  readonly vista = model<VistaCalendario>('semana');
  readonly fecha = model<Date>(new Date());
  readonly seleccion = model<Date | null>(null);
  readonly horaInicio = input(7);
  readonly horaFin = input(21);
  readonly cargando = input(false);
  readonly vistas = input<readonly VistaCalendario[]>(['mes', 'semana', 'dia']);
  readonly bloquearPasado = input(true);

  readonly eventoElegido = output<CalEvento>();
  readonly huecoElegido = output<Date>();
  readonly rangoCambiado = output<RangoVisible>();

  protected readonly altoHora = ALTO_HORA;
  protected readonly diasCortos = DIAS_CORTOS;
  protected readonly etiquetaVista: Readonly<Record<VistaCalendario, string>> = {
    mes: 'Mes',
    semana: 'Semana',
    dia: 'Día',
  };

  private readonly ahoraState = signal(new Date());
  protected readonly ahora = this.ahoraState.asReadonly();

  protected readonly rango = computed(() => rangoVisible(this.vista(), this.fecha()));
  protected readonly modoSeleccion = computed(() => this.huecos() !== null);

  protected readonly huecosLibres = computed(() => {
    const huecos = this.huecos();
    if (huecos === null) {
      return [];
    }
    const ocupados = this.eventos().filter((evento) => !evento.ignoraCruce);
    const limite = this.bloquearPasado() ? this.ahora().getTime() : Number.NEGATIVE_INFINITY;
    return huecos.filter(
      (hueco) =>
        hueco.inicio.getTime() > limite && !ocupados.some((evento) => solapan(hueco, evento)),
    );
  });

  protected readonly cruces = computed(() => {
    const { inicio, fin } = this.rango();
    const visibles = this.eventos().filter(
      (evento) => evento.inicio >= inicio && evento.inicio < new Date(fin.getTime() + 86_400_000),
    );
    return contarCruces(visibles);
  });

  protected readonly titulo = computed(() => {
    const fecha = this.fecha();
    const { inicio, fin } = this.rango();
    const vista = this.vista();
    if (vista === 'mes') {
      return { tipo: 'mes' as const, inicio: fecha, fin: fecha };
    }
    return { tipo: vista, inicio, fin };
  });

  protected readonly horasVisibles = computed(() => {
    let minimo = this.horaInicio();
    let maximo = this.horaFin();
    const { inicio, fin } = this.rango();
    const limiteFin = new Date(fin.getTime() + 86_400_000);
    const considerar = (desde: Date, hasta: Date) => {
      if (desde >= inicio && desde < limiteFin) {
        minimo = Math.min(minimo, desde.getHours());
        maximo = Math.max(maximo, hasta.getMinutes() > 0 ? hasta.getHours() + 1 : hasta.getHours());
      }
    };
    const ahora = this.ahora();
    if (ahora >= inicio && ahora < limiteFin) {
      minimo = Math.min(minimo, ahora.getHours());
      maximo = Math.max(maximo, ahora.getHours() + 1);
    }
    this.eventos().forEach((evento) => considerar(evento.inicio, evento.fin));
    this.huecosLibres().forEach((hueco) => considerar(hueco.inicio, hueco.fin));
    const horas = Array.from({ length: Math.max(1, maximo - minimo) }, (_, i) => minimo + i);
    return { minimo, maximo: Math.min(24, Math.max(maximo, minimo + 1)), horas };
  });

  protected readonly columnas = computed<ColumnaDia[]>(() => {
    if (this.vista() === 'mes') {
      return [];
    }
    const { inicio } = this.rango();
    const dias = diasDeRango(inicio, this.vista() === 'dia' ? 1 : 7);
    const base = this.horasVisibles().minimo;
    const seleccion = this.seleccion();
    return dias.map((fecha) => {
      const delDia = this.eventos().filter((evento) => mismoDia(evento.inicio, fecha));
      const eventos = distribuir(delDia).map((item) => this.dibujar(item, base));
      const huecos = this.huecosLibres()
        .filter((hueco) => mismoDia(hueco.inicio, fecha))
        .map<HuecoDibujado>((hueco) => ({
          hueco,
          top: (minutosDesde(hueco.inicio, base) * ALTO_HORA) / 60,
          alto: Math.max(18, (duracionMinutos(hueco) * ALTO_HORA) / 60 - 2),
          elegido: seleccion !== null && seleccion.getTime() === hueco.inicio.getTime(),
        }));
      return { fecha, esHoy: mismoDia(fecha, this.ahora()), eventos, huecos };
    });
  });

  protected readonly celdas = computed<CeldaMes[]>(() => {
    if (this.vista() !== 'mes') {
      return [];
    }
    const { inicio, fin } = this.rango();
    const total = Math.round((fin.getTime() - inicio.getTime()) / 86_400_000) + 1;
    const mes = this.fecha().getMonth();
    return diasDeRango(inicio, total).map((fecha) => {
      const delDia = this.eventos()
        .filter((evento) => mismoDia(evento.inicio, fecha))
        .sort((a, b) => a.inicio.getTime() - b.inicio.getTime());
      return {
        fecha,
        delMes: fecha.getMonth() === mes,
        esHoy: mismoDia(fecha, this.ahora()),
        eventos: delDia.slice(0, MAX_EVENTOS_MES),
        extra: Math.max(0, delDia.length - MAX_EVENTOS_MES),
        libres: this.huecosLibres().filter((hueco) => mismoDia(hueco.inicio, fecha)).length,
        cruces: contarCruces(delDia),
      };
    });
  });

  protected readonly lineaAhora = computed(() => {
    const ahora = this.ahora();
    const { minimo, maximo } = this.horasVisibles();
    const minutos = minutosDesde(ahora, minimo);
    if (minutos < 0 || ahora.getHours() >= maximo) {
      return null;
    }
    return (minutos * ALTO_HORA) / 60;
  });

  constructor() {
    const temporizador = setInterval(() => this.ahoraState.set(new Date()), 30_000);
    inject(DestroyRef).onDestroy(() => clearInterval(temporizador));
    effect(() => this.rangoCambiado.emit(this.rango()));
  }

  protected mover(pasos: number): void {
    this.fecha.set(moverFecha(this.vista(), this.fecha(), pasos));
  }

  protected irAHoy(): void {
    this.fecha.set(new Date());
  }

  protected cambiarVista(vista: VistaCalendario): void {
    this.vista.set(vista);
  }

  protected abrirDia(fecha: Date): void {
    this.fecha.set(inicioDelDia(fecha));
    this.vista.set('dia');
  }

  protected elegirHueco(hueco: CalHueco): void {
    this.seleccion.set(hueco.inicio);
    this.huecoElegido.emit(hueco.inicio);
  }

  protected claveHora(hora: number): string {
    return `${String(hora).padStart(2, '0')}:00`;
  }

  private dibujar(item: EventoPosicionado, base: number): EventoDibujado {
    const ancho = 100 / item.columnas;
    return {
      evento: item.evento,
      top: (minutosDesde(item.evento.inicio, base) * ALTO_HORA) / 60,
      alto: Math.max(22, (duracionMinutos(item.evento) * ALTO_HORA) / 60 - 2),
      izquierda: item.columna * ancho,
      ancho,
      conflicto: item.conflicto,
    };
  }
}
