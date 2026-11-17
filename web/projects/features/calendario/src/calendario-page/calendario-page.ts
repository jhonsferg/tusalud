import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  effect,
  inject,
  signal,
  untracked,
} from '@angular/core';
import { Router } from '@angular/router';
import { Button } from 'primeng/button';

import {
  AuthService,
  CalendarioStore,
  CitaResponse,
  CitasStore,
  EventoCalendarioResponse,
  MedicosStore,
  PerfilStore,
  toIsoDate,
  toIsoDateTime,
} from '@tusalud/core';
import {
  Calendario,
  CalEvento,
  CambioEvento,
  CrearEvento,
  RangoVisible,
  VistaCalendario,
} from '@tusalud/ui/calendario';
import {
  categoriasPara,
  citaAEvento,
  DatosEvento,
  eventoApiACalendario,
  horarioLaboral,
  tiposPermitidos,
} from '@tusalud/ui/utils';
import { EventoDialog } from '@tusalud/ui/evento-dialog';
import { PageHeader } from '@tusalud/ui/page-header';

interface DatosCita {
  readonly origen: 'cita';
  readonly cita: CitaResponse;
}

@Component({
  selector: 'app-calendario-page',
  imports: [Button, Calendario, EventoDialog, PageHeader],
  templateUrl: './calendario-page.html',
  styleUrl: './calendario-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CalendarioPage implements OnInit {
  protected readonly auth = inject(AuthService);
  protected readonly store = inject(CalendarioStore);
  private readonly citas = inject(CitasStore);
  private readonly medicos = inject(MedicosStore);
  private readonly perfil = inject(PerfilStore);
  private readonly router = inject(Router);

  protected readonly vista = signal<VistaCalendario>('semana');
  protected readonly fecha = signal(new Date());
  private readonly rango = signal<RangoVisible | null>(null);

  protected readonly dialogoVisible = signal(false);
  protected readonly seleccionado = signal<EventoCalendarioResponse | null>(null);
  protected readonly borrador = signal<CrearEvento | null>(null);

  protected readonly vistas: readonly VistaCalendario[] = ['mes', 'semana', 'dia', 'agenda'];

  protected readonly tipos = computed(() => tiposPermitidos(this.auth.roles()));
  protected readonly categorias = computed(() => categoriasPara(this.auth.roles()));

  protected readonly eventos = computed<CalEvento[]>(() => {
    const deCitas = this.auth.isMedico()
      ? (this.citas.agendaRango.value() ?? [])
      : this.auth.isPaciente()
        ? this.citas.citas()
        : [];
    const mostrar = this.auth.isMedico() ? 'paciente' : 'medico';
    const citas = deCitas.map<CalEvento>((cita) => ({
      ...citaAEvento(cita, mostrar),
      categoria: 'CITA',
      editable: false,
      datos: { origen: 'cita', cita } satisfies DatosCita,
    }));
    const personales = (this.store.eventos.value() ?? [])
      .map((evento) => eventoApiACalendario(evento))
      .filter((evento): evento is CalEvento => evento !== null);
    return [...citas, ...personales];
  });

  protected readonly laboral = computed(() =>
    this.auth.isMedico() ? horarioLaboral(this.medicos.horarios.value() ?? []) : null,
  );

  constructor() {
    effect(() => {
      const rango = this.rango();
      if (!rango) {
        return;
      }
      untracked(() => {
        const inicio = toIsoDate(rango.inicio);
        const fin = toIsoDate(rango.fin);
        this.store.cargar(inicio, fin);
        if (this.auth.isMedico()) {
          this.citas.loadAgendaRango(inicio, fin);
        }
      });
    });
    effect(() => {
      const medicoId = this.perfil.perfil.value()?.medicoId;
      if (medicoId && this.auth.isMedico()) {
        untracked(() => this.medicos.loadHorarios(medicoId));
      }
    });
  }

  ngOnInit(): void {
    if (this.auth.isPaciente()) {
      this.citas.loadMisCitas();
    }
    if (this.auth.isMedico()) {
      this.perfil.load();
    }
  }

  protected cargarRango(rango: RangoVisible): void {
    this.rango.set(rango);
  }

  protected nuevo(): void {
    const inicio = new Date();
    inicio.setHours(inicio.getHours() + 1, 0, 0, 0);
    this.abrirNuevo({ inicio, fin: new Date(inicio.getTime() + 60 * 60_000), todoElDia: false });
  }

  protected abrirNuevo(borrador: CrearEvento): void {
    this.seleccionado.set(null);
    this.borrador.set(borrador);
    this.dialogoVisible.set(true);
  }

  protected abrir(evento: CalEvento): void {
    const datos = evento.datos as DatosCita | DatosEvento | undefined;
    if (datos?.origen === 'cita') {
      void this.router.navigate(['/citas', datos.cita.id]);
    } else if (datos?.origen === 'evento') {
      this.borrador.set(null);
      this.seleccionado.set(datos.evento);
      this.dialogoVisible.set(true);
    }
  }

  protected cambiar(cambio: CambioEvento): void {
    const datos = cambio.evento.datos as DatosEvento | undefined;
    if (datos?.origen !== 'evento') {
      return;
    }
    const original = datos.evento;
    const inicio = toIsoDateTime(cambio.inicio);
    const fin = toIsoDateTime(cambio.fin);
    this.store.reemplazarLocal({ ...original, inicio, fin });
    this.store.mover(original.id, { inicio, fin, todoElDia: original.todoElDia }).subscribe({
      error: () => this.store.reemplazarLocal(original),
    });
  }
}
