import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  afterNextRender,
  computed,
  effect,
  inject,
  input,
  model,
  output,
  signal,
  untracked,
  viewChild,
} from '@angular/core';

import {
  CalCategoria,
  CalEvento,
  CalHueco,
  CambioEvento,
  CrearEvento,
  EventoPosicionado,
  HorarioDia,
  RangoVisible,
  VistaCalendario,
  ajustarMinutos,
  contarCruces,
  diasDeRango,
  distribuir,
  duracionMinutos,
  fechaConMinutos,
  inicioDelDia,
  mismoDia,
  minutosDeHora,
  minutosDesde,
  moverFecha,
  numeroDeSemana,
  rangoVisible,
  recortarAlDia,
  solapan,
  sumarDias,
} from './calendario.utils';

interface EventoDibujado {
  readonly evento: CalEvento;
  readonly top: number;
  readonly alto: number;
  readonly izquierda: number;
  readonly ancho: number;
  readonly conflicto: boolean;
  readonly editable: boolean;
}

interface HuecoDibujado {
  readonly hueco: CalHueco;
  readonly top: number;
  readonly alto: number;
  readonly elegido: boolean;
}

interface BloqueNoLaboral {
  readonly top: number;
  readonly alto: number;
}

interface ColumnaDia {
  readonly fecha: Date;
  readonly esHoy: boolean;
  readonly todoElDia: CalEvento[];
  readonly eventos: EventoDibujado[];
  readonly huecos: HuecoDibujado[];
  readonly noLaboral: BloqueNoLaboral[];
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

interface GrupoAgenda {
  readonly fecha: Date;
  readonly eventos: CalEvento[];
}

interface Arrastre {
  readonly tipo: 'crear' | 'mover' | 'redimensionar';
  readonly evento: CalEvento | null;
  readonly columna: number;
  readonly desde: number;
  readonly hasta: number;
  readonly duracion: number;
  readonly desfase: number;
  readonly origenX: number;
  readonly origenY: number;
  readonly movido: boolean;
}

const ALTO_HORA = 56;
const MAX_EVENTOS_MES = 3;
const UMBRAL_ARRASTRE_PX = 5;
const DIAS_CORTOS = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'];

@Component({
  selector: 'app-calendario',
  imports: [DatePipe],
  templateUrl: './calendario.html',
  styleUrl: './calendario.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '(keydown)': 'atajo($event)' },
})
export class Calendario {
  readonly eventos = input<readonly CalEvento[]>([]);
  readonly huecos = input<readonly CalHueco[] | null>(null);
  readonly categorias = input<readonly CalCategoria[]>([]);
  readonly horarioLaboral = input<readonly HorarioDia[] | null>(null);
  readonly vista = model<VistaCalendario>('semana');
  readonly fecha = model<Date>(new Date());
  readonly seleccion = model<Date | null>(null);
  readonly horaInicio = input(7);
  readonly horaFin = input(21);
  readonly cargando = input(false);
  readonly vistas = input<readonly VistaCalendario[]>(['mes', 'semana', 'dia']);
  readonly bloquearPasado = input(true);
  readonly editable = input(false);
  readonly lateral = input(true);
  readonly pasoMinutos = input(15);
  readonly textoVacio = input('No hay eventos en este periodo.');

  readonly eventoElegido = output<CalEvento>();
  readonly huecoElegido = output<Date>();
  readonly rangoCambiado = output<RangoVisible>();
  readonly crear = output<CrearEvento>();
  readonly eventoCambiado = output<CambioEvento>();

  protected readonly altoHora = ALTO_HORA;
  protected readonly diasCortos = DIAS_CORTOS;
  protected readonly etiquetaVista: Readonly<Record<VistaCalendario, string>> = {
    mes: 'Mes',
    semana: 'Semana',
    dia: 'Día',
    agenda: 'Agenda',
  };

  private readonly cuerpo = viewChild<ElementRef<HTMLElement>>('cuerpo');
  private readonly desplazable = viewChild<ElementRef<HTMLElement>>('desplazable');

  private readonly ahoraState = signal(new Date());
  protected readonly ahora = this.ahoraState.asReadonly();
  protected readonly ocultas = signal<ReadonlySet<string>>(new Set());
  protected readonly arrastre = signal<Arrastre | null>(null);

  protected readonly modoSeleccion = computed(() => this.huecos() !== null);
  protected readonly puedeEditar = computed(() => this.editable() && !this.modoSeleccion());
  protected readonly rango = computed(() => rangoVisible(this.vista(), this.fecha()));

  protected readonly eventosVisibles = computed(() => {
    const ocultas = this.ocultas();
    return this.eventos().filter((evento) => !evento.categoria || !ocultas.has(evento.categoria));
  });

  protected readonly huecosLibres = computed(() => {
    const huecos = this.huecos();
    if (huecos === null) {
      return [];
    }
    const ocupados = this.eventosVisibles().filter((evento) => !evento.ignoraCruce);
    const limite = this.bloquearPasado() ? this.ahora().getTime() : Number.NEGATIVE_INFINITY;
    return huecos.filter(
      (hueco) =>
        hueco.inicio.getTime() > limite && !ocupados.some((evento) => solapan(hueco, evento)),
    );
  });

  protected readonly cruces = computed(() => {
    const { inicio, fin } = this.rango();
    const limiteFin = sumarDias(fin, 1);
    return contarCruces(
      this.eventosVisibles().filter(
        (evento) => !evento.todoElDia && evento.inicio >= inicio && evento.inicio < limiteFin,
      ),
    );
  });

  protected readonly titulo = computed(() => {
    const vista = this.vista();
    const { inicio, fin } = this.rango();
    return { tipo: vista, inicio: vista === 'mes' ? this.fecha() : inicio, fin };
  });

  protected readonly semana = computed(() => numeroDeSemana(this.rango().inicio));

  protected readonly horasVisibles = computed(() => {
    let minimo = this.horaInicio();
    let maximo = this.horaFin();
    const { inicio, fin } = this.rango();
    const limiteFin = sumarDias(fin, 1);
    const ahora = this.ahora();
    if (ahora >= inicio && ahora < limiteFin) {
      minimo = Math.min(minimo, ahora.getHours());
      maximo = Math.max(maximo, ahora.getHours() + 1);
    }
    const considerar = (desde: Date, hasta: Date) => {
      if (desde >= inicio && desde < limiteFin) {
        minimo = Math.min(minimo, desde.getHours());
        maximo = Math.max(maximo, hasta.getMinutes() > 0 ? hasta.getHours() + 1 : hasta.getHours());
      }
    };
    this.eventosVisibles()
      .filter((evento) => !evento.todoElDia)
      .forEach((evento) => considerar(evento.inicio, evento.fin));
    this.huecosLibres().forEach((hueco) => considerar(hueco.inicio, hueco.fin));
    maximo = Math.min(24, Math.max(maximo, minimo + 1));
    return {
      minimo,
      maximo,
      horas: Array.from({ length: maximo - minimo }, (_, i) => minimo + i),
    };
  });

  protected readonly columnas = computed<ColumnaDia[]>(() => {
    const vista = this.vista();
    if (vista === 'mes' || vista === 'agenda') {
      return [];
    }
    const { inicio } = this.rango();
    const dias = diasDeRango(inicio, vista === 'dia' ? 1 : 7);
    const { minimo, maximo } = this.horasVisibles();
    const seleccion = this.seleccion();
    const laboral = this.horarioLaboral();
    const editable = this.puedeEditar();
    return dias.map((fecha) => {
      const delDia = this.eventosVisibles()
        .map((evento) => recortarAlDia(evento, fecha))
        .filter((evento): evento is CalEvento => evento !== null);
      const todoElDia = delDia.filter((evento) => evento.todoElDia);
      const conHora = delDia.filter((evento) => !evento.todoElDia);
      const eventos = distribuir(conHora).map((item) => this.dibujar(item, minimo, editable));
      const huecos = this.huecosLibres()
        .filter((hueco) => mismoDia(hueco.inicio, fecha))
        .map<HuecoDibujado>((hueco) => ({
          hueco,
          top: (minutosDesde(hueco.inicio, minimo) * ALTO_HORA) / 60,
          alto: Math.max(18, (duracionMinutos(hueco) * ALTO_HORA) / 60 - 2),
          elegido: seleccion !== null && seleccion.getTime() === hueco.inicio.getTime(),
        }));
      return {
        fecha,
        esHoy: mismoDia(fecha, this.ahora()),
        todoElDia,
        eventos,
        huecos,
        noLaboral: laboral ? this.sinAtencion(laboral, fecha, minimo, maximo) : [],
      };
    });
  });

  protected readonly hayTodoElDia = computed(() =>
    this.columnas().some((columna) => columna.todoElDia.length > 0),
  );

  protected readonly celdas = computed<CeldaMes[]>(() => {
    if (this.vista() !== 'mes') {
      return [];
    }
    const { inicio, fin } = this.rango();
    const total = Math.round((fin.getTime() - inicio.getTime()) / 86_400_000) + 1;
    const mes = this.fecha().getMonth();
    return diasDeRango(inicio, total).map((fecha) => {
      const delDia = this.eventosVisibles()
        .filter((evento) => recortarAlDia(evento, fecha) !== null)
        .sort(
          (a, b) =>
            Number(!!b.todoElDia) - Number(!!a.todoElDia) ||
            a.inicio.getTime() - b.inicio.getTime(),
        );
      return {
        fecha,
        delMes: fecha.getMonth() === mes,
        esHoy: mismoDia(fecha, this.ahora()),
        eventos: delDia.slice(0, MAX_EVENTOS_MES),
        extra: Math.max(0, delDia.length - MAX_EVENTOS_MES),
        libres: this.huecosLibres().filter((hueco) => mismoDia(hueco.inicio, fecha)).length,
        cruces: contarCruces(delDia.filter((evento) => !evento.todoElDia)),
      };
    });
  });

  protected readonly agenda = computed<GrupoAgenda[]>(() => {
    if (this.vista() !== 'agenda') {
      return [];
    }
    const { inicio, fin } = this.rango();
    const total = Math.round((fin.getTime() - inicio.getTime()) / 86_400_000) + 1;
    return diasDeRango(inicio, total)
      .map((fecha) => ({
        fecha,
        eventos: this.eventosVisibles()
          .filter((evento) => recortarAlDia(evento, fecha) !== null)
          .sort(
            (a, b) =>
              Number(!!b.todoElDia) - Number(!!a.todoElDia) ||
              a.inicio.getTime() - b.inicio.getTime(),
          ),
      }))
      .filter((grupo) => grupo.eventos.length > 0);
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

  protected readonly fantasma = computed(() => {
    const arrastre = this.arrastre();
    if (!arrastre || !arrastre.movido) {
      return null;
    }
    const { minimo } = this.horasVisibles();
    return {
      columna: arrastre.columna,
      top: ((arrastre.desde - minimo * 60) * ALTO_HORA) / 60,
      alto: Math.max(14, ((arrastre.hasta - arrastre.desde) * ALTO_HORA) / 60),
      evento: arrastre.evento,
      desde: arrastre.desde,
      hasta: arrastre.hasta,
    };
  });

  constructor() {
    const temporizador = setInterval(() => this.ahoraState.set(new Date()), 30_000);
    inject(DestroyRef).onDestroy(() => clearInterval(temporizador));
    effect(() => this.rangoCambiado.emit(this.rango()));
    effect(() => {
      const vista = this.vista();
      if (vista === 'semana' || vista === 'dia') {
        untracked(() => this.irAHoraActual());
      }
    });
    afterNextRender(() => this.irAHoraActual());
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

  protected alternarCategoria(clave: string): void {
    this.ocultas.update((actual) => {
      const nueva = new Set(actual);
      if (nueva.has(clave)) {
        nueva.delete(clave);
      } else {
        nueva.add(clave);
      }
      return nueva;
    });
  }

  protected elegirHueco(hueco: CalHueco): void {
    this.seleccion.set(hueco.inicio);
    this.huecoElegido.emit(hueco.inicio);
  }

  protected clickEvento(evento: CalEvento, click: MouseEvent): void {
    if (this.puedeEditar() && click.detail > 0 && !evento.todoElDia) {
      return;
    }
    this.eventoElegido.emit(evento);
  }

  protected crearEnDia(fecha: Date): void {
    if (!this.puedeEditar()) {
      return;
    }
    const inicio = fechaConMinutos(fecha, 9 * 60);
    this.crear.emit({ inicio, fin: fechaConMinutos(fecha, 10 * 60), todoElDia: false });
  }

  protected crearTodoElDia(fecha: Date): void {
    if (this.puedeEditar()) {
      this.crear.emit({ inicio: inicioDelDia(fecha), fin: inicioDelDia(fecha), todoElDia: true });
    }
  }

  protected esHoy(fecha: Date): boolean {
    return mismoDia(fecha, this.ahora());
  }

  protected horaDe(minutos: number): string {
    const total = Math.max(0, minutos);
    return `${String(Math.floor(total / 60) % 24).padStart(2, '0')}:${String(total % 60).padStart(2, '0')}`;
  }

  protected claveHora(hora: number): string {
    return `${String(hora).padStart(2, '0')}:00`;
  }

  protected atajo(evento: KeyboardEvent): void {
    const objetivo = evento.target as HTMLElement | null;
    if (
      evento.ctrlKey ||
      evento.metaKey ||
      evento.altKey ||
      objetivo?.closest('input, textarea, select, [contenteditable]')
    ) {
      return;
    }
    const tecla = evento.key.toLowerCase();
    const vistas = this.vistas();
    const porTecla: Record<string, VistaCalendario> = {
      m: 'mes',
      s: 'semana',
      w: 'semana',
      d: 'dia',
      a: 'agenda',
    };
    if (tecla === 't') {
      this.irAHoy();
    } else if (tecla === 'j' || tecla === 'arrowleft') {
      this.mover(-1);
    } else if (tecla === 'k' || tecla === 'arrowright') {
      this.mover(1);
    } else if (porTecla[tecla] && vistas.includes(porTecla[tecla])) {
      this.vista.set(porTecla[tecla]);
    } else {
      return;
    }
    evento.preventDefault();
  }

  protected iniciarArrastre(evento: PointerEvent): void {
    if (!this.puedeEditar() || evento.button !== 0) {
      return;
    }
    const cuerpo = this.cuerpo()?.nativeElement;
    const objetivo = evento.target as HTMLElement;
    if (!cuerpo || objetivo.closest('.hueco, .ahora')) {
      return;
    }
    const lugar = this.posicion(evento);
    if (!lugar) {
      return;
    }
    const base = {
      columna: lugar.columna,
      origenX: evento.clientX,
      origenY: evento.clientY,
      movido: false,
    };
    const resize = objetivo.closest<HTMLElement>('.resize');
    const tarjeta = objetivo.closest<HTMLElement>('.evento');
    const elegido = tarjeta
      ? this.eventosVisibles().find((item) => String(item.id) === tarjeta.dataset['id'])
      : null;
    if (tarjeta && elegido) {
      if (elegido.editable === false) {
        return;
      }
      const inicio = minutosDesde(elegido.inicio, 0);
      const duracion = duracionMinutos(elegido);
      cuerpo.setPointerCapture(evento.pointerId);
      this.arrastre.set({
        ...base,
        tipo: resize ? 'redimensionar' : 'mover',
        evento: elegido,
        desde: inicio,
        hasta: inicio + duracion,
        duracion,
        desfase: lugar.minutos - inicio,
      });
      return;
    }
    const ancla = Math.floor(lugar.minutos / this.pasoMinutos()) * this.pasoMinutos();
    cuerpo.setPointerCapture(evento.pointerId);
    this.arrastre.set({
      ...base,
      tipo: 'crear',
      evento: null,
      desde: ancla,
      hasta: ancla,
      duracion: 60,
      desfase: ancla,
    });
  }

  protected moverArrastre(evento: PointerEvent): void {
    const actual = this.arrastre();
    if (!actual) {
      return;
    }
    const lugar = this.posicion(evento);
    if (!lugar) {
      return;
    }
    const movido =
      actual.movido ||
      Math.abs(evento.clientX - actual.origenX) > UMBRAL_ARRASTRE_PX ||
      Math.abs(evento.clientY - actual.origenY) > UMBRAL_ARRASTRE_PX;
    const paso = this.pasoMinutos();
    if (actual.tipo === 'mover') {
      const desde = Math.max(0, ajustarMinutos(lugar.minutos - actual.desfase, paso));
      this.arrastre.set({
        ...actual,
        movido,
        columna: lugar.columna,
        desde,
        hasta: desde + actual.duracion,
      });
    } else if (actual.tipo === 'redimensionar') {
      const origen = minutosDesde(actual.evento!.inicio, 0);
      const hasta = Math.max(origen + paso, ajustarMinutos(lugar.minutos, paso));
      this.arrastre.set({ ...actual, movido, desde: origen, hasta });
    } else {
      const fin = ajustarMinutos(lugar.minutos, paso);
      const ancla = actual.desfase;
      this.arrastre.set({
        ...actual,
        movido,
        columna: lugar.columna,
        desde: Math.min(ancla, fin),
        hasta: Math.max(ancla, fin, Math.min(ancla, fin) + paso),
      });
    }
  }

  protected terminarArrastre(evento: PointerEvent): void {
    const actual = this.arrastre();
    this.arrastre.set(null);
    this.cuerpo()?.nativeElement.releasePointerCapture?.(evento.pointerId);
    if (!actual) {
      return;
    }
    const columnas = this.columnas();
    const dia = columnas[Math.min(actual.columna, columnas.length - 1)]?.fecha;
    if (!dia) {
      return;
    }
    if (actual.tipo === 'crear') {
      const desde = actual.movido ? actual.desde : Math.floor(actual.desde / 30) * 30;
      const hasta = actual.movido ? actual.hasta : desde + 60;
      this.crear.emit({
        inicio: fechaConMinutos(dia, desde),
        fin: fechaConMinutos(dia, hasta),
        todoElDia: false,
      });
      return;
    }
    const objetivo = actual.evento!;
    if (!actual.movido) {
      this.eventoElegido.emit(objetivo);
      return;
    }
    const diaOriginal = inicioDelDia(objetivo.inicio);
    const diaFinal = actual.tipo === 'mover' ? dia : diaOriginal;
    const inicio = fechaConMinutos(diaFinal, actual.desde);
    const fin = fechaConMinutos(diaFinal, actual.hasta);
    if (
      inicio.getTime() !== objetivo.inicio.getTime() ||
      fin.getTime() !== objetivo.fin.getTime()
    ) {
      this.eventoCambiado.emit({ evento: objetivo, inicio, fin, todoElDia: false });
    }
  }

  protected cancelarArrastre(): void {
    this.arrastre.set(null);
  }

  private posicion(evento: PointerEvent): { columna: number; minutos: number } | null {
    const cuerpo = this.cuerpo()?.nativeElement;
    if (!cuerpo) {
      return null;
    }
    const columnas = Array.from(cuerpo.querySelectorAll<HTMLElement>('.columna'));
    if (columnas.length === 0) {
      return null;
    }
    let indice = columnas.findIndex((columna) => {
      const rect = columna.getBoundingClientRect();
      return evento.clientX >= rect.left && evento.clientX < rect.right;
    });
    if (indice === -1) {
      indice = evento.clientX < columnas[0].getBoundingClientRect().left ? 0 : columnas.length - 1;
    }
    const rect = cuerpo.getBoundingClientRect();
    const y = Math.min(Math.max(evento.clientY - rect.top, 0), rect.height);
    const { minimo } = this.horasVisibles();
    return { columna: indice, minutos: minimo * 60 + (y * 60) / ALTO_HORA };
  }

  private irAHoraActual(): void {
    queueMicrotask(() => {
      const contenedor = this.desplazable()?.nativeElement;
      const linea = this.lineaAhora();
      if (contenedor) {
        const objetivo = linea !== null ? linea - 140 : 0;
        contenedor.scrollTop = Math.max(0, objetivo);
      }
    });
  }

  private sinAtencion(
    laboral: readonly HorarioDia[],
    fecha: Date,
    minimo: number,
    maximo: number,
  ): BloqueNoLaboral[] {
    const diaIso = fecha.getDay() === 0 ? 7 : fecha.getDay();
    const tramos = laboral
      .filter((item) => item.dia === diaIso)
      .map((item) => [minutosDeHora(item.inicio), minutosDeHora(item.fin)] as const)
      .sort((a, b) => a[0] - b[0]);
    const bloques: BloqueNoLaboral[] = [];
    let cursor = minimo * 60;
    const tope = maximo * 60;
    const agregar = (desde: number, hasta: number) => {
      if (hasta > desde) {
        bloques.push({
          top: ((desde - minimo * 60) * ALTO_HORA) / 60,
          alto: ((hasta - desde) * ALTO_HORA) / 60,
        });
      }
    };
    for (const [desde, hasta] of tramos) {
      agregar(cursor, Math.min(desde, tope));
      cursor = Math.max(cursor, hasta);
    }
    agregar(cursor, tope);
    return bloques;
  }

  private dibujar(item: EventoPosicionado, base: number, editable: boolean): EventoDibujado {
    const ancho = 100 / item.columnas;
    return {
      evento: item.evento,
      top: (minutosDesde(item.evento.inicio, base) * ALTO_HORA) / 60,
      alto: Math.max(22, (duracionMinutos(item.evento) * ALTO_HORA) / 60 - 2),
      izquierda: item.columna * ancho,
      ancho,
      conflicto: item.conflicto,
      editable: editable && item.evento.editable !== false,
    };
  }
}
