import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Button } from 'primeng/button';

import {
  AsyncState,
  AuthService,
  CitaHoyResponse,
  RecepcionApiService,
  ResumenRecepcionResponse,
  SegurosStore,
  TurnoResponse,
} from '@tusalud/core';
import { HeroPortada } from '@tusalud/ui/hero-portada';
import {
  AccionRapida,
  ItemLineaTiempo,
  ItemTarea,
  QuickActions,
  SectionCard,
  StatCard,
  TaskList,
  Timeline,
  fechaLarga,
  horaCorta,
  saludoDelDia,
} from '@tusalud/ui/tablero';

@Component({
  selector: 'app-inicio-recepcion',
  imports: [
    FormsModule,
    RouterLink,
    Button,
    HeroPortada,
    SectionCard,
    StatCard,
    Timeline,
    TaskList,
    QuickActions,
  ],
  template: `
    <app-hero-portada app="recepcion" [title]="saludo()" [subtitle]="fecha()" />

    <form
      class="buscador"
      role="search"
      (submit)="$event.preventDefault(); buscar()"
      autocomplete="off"
    >
      <i class="pi pi-search" aria-hidden="true"></i>
      <input
        type="search"
        name="busqueda-paciente"
        aria-label="Buscar paciente por DNI o nombre"
        placeholder="Buscar paciente por DNI o nombre y presionar Enter"
        autocomplete="off"
        [ngModel]="busqueda()"
        (ngModelChange)="busqueda.set($event)"
        [ngModelOptions]="{ standalone: true }"
      />
      <p-button type="submit" label="Buscar" icon="pi pi-search" />
    </form>

    <div class="fila-stats stats">
      <app-stat-card
        etiqueta="Por llegar hoy"
        [valor]="resumen.value()?.citasPendientesDeLlegada ?? 0"
        icono="pi-calendar"
        enlace="/recepcion"
      />
      <app-stat-card
        etiqueta="En espera"
        [valor]="resumen.value()?.enEspera ?? 0"
        icono="pi-clock"
        [tono]="(resumen.value()?.enEspera ?? 0) > 5 ? 'aviso' : 'neutro'"
        enlace="/recepcion"
      />
      <app-stat-card
        etiqueta="Llamados"
        [valor]="resumen.value()?.llamados ?? 0"
        icono="pi-megaphone"
        tono="info"
        enlace="/recepcion/pantalla"
      />
      <app-stat-card
        etiqueta="Atendidos hoy"
        [valor]="resumen.value()?.atendidosHoy ?? 0"
        icono="pi-check-circle"
        tono="exito"
        [detalle]="(resumen.value()?.ausentesHoy ?? 0) + ' ausentes'"
      />
      <app-stat-card
        etiqueta="Espera promedio"
        [valor]="(resumen.value()?.esperaPromedioMinutos ?? 0) + ' min'"
        icono="pi-stopwatch"
        [tono]="(resumen.value()?.esperaPromedioMinutos ?? 0) > 30 ? 'aviso' : 'neutro'"
      />
      @if (seguros.resumen.value(); as s) {
        <app-stat-card
          etiqueta="Pólizas por vencer"
          [valor]="s.polizasPorVencer ?? 0"
          icono="pi-id-card"
          [tono]="(s.polizasPorVencer ?? 0) > 0 ? 'aviso' : 'neutro'"
          detalle="En los próximos 30 días"
          enlace="/seguros/polizas"
        />
        <app-stat-card
          etiqueta="Preautorizaciones por resolver"
          [valor]="s.preautorizacionesPendientes ?? 0"
          icono="pi-verified"
          [tono]="(s.preautorizacionesPendientes ?? 0) > 0 ? 'aviso' : 'neutro'"
          enlace="/seguros/preautorizaciones"
        />
      }
    </div>

    <div class="tablero">
      <div class="c7">
        <app-section-card
          titulo="Citas por registrar llegada"
          icono="pi-calendar-clock"
          [cargando]="citas.loading() && !citas.hasValue()"
          [error]="citas.error()?.message ?? null"
          [vacio]="porLlegar().length === 0 && citas.hasValue()"
          textoVacio="No quedan citas por registrar hoy."
        >
          <app-timeline [items]="lineaCitas()" />
          <a pie routerLink="/recepcion">Registrar llegadas</a>
        </app-section-card>
      </div>

      <div class="c5">
        <app-section-card
          titulo="Cola de espera"
          icono="pi-users"
          [cargando]="cola.loading() && !cola.hasValue()"
          [vacio]="lineaCola().length === 0 && cola.hasValue()"
          textoVacio="No hay pacientes en espera."
        >
          <app-task-list [items]="lineaCola()" />
          <a pie routerLink="/recepcion/pantalla">Abrir sala de espera</a>
        </app-section-card>
      </div>

      <div class="c12">
        <app-quick-actions [acciones]="acciones" />
      </div>
    </div>
  `,
  styles: `
    .buscador {
      display: flex;
      align-items: center;
      gap: var(--sp-3);
      margin-bottom: var(--sp-4);
      padding: var(--sp-2) var(--sp-2) var(--sp-2) var(--sp-4);
      border: 1px solid var(--p-content-border-color);
      border-radius: var(--radio-md);
      background: var(--p-content-background);
      box-shadow: var(--sombra-1);

      i {
        color: var(--p-text-muted-color);
      }

      input {
        flex: 1;
        min-width: 0;
        min-height: 2.75rem;
        border: 0;
        outline: 0;
        background: transparent;
        color: var(--p-text-color);
        font: inherit;
        font-size: 1.05rem;
      }

      &:focus-within {
        border-color: var(--p-primary-color);
        box-shadow: 0 0 0 3px var(--app-primary-soft);
      }
    }

    .stats {
      margin-bottom: var(--sp-4);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InicioRecepcion implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly api = inject(RecepcionApiService);
  private readonly router = inject(Router);
  protected readonly seguros = inject(SegurosStore);

  protected readonly resumen = new AsyncState<ResumenRecepcionResponse>();
  protected readonly citas = new AsyncState<CitaHoyResponse[]>();
  protected readonly cola = new AsyncState<TurnoResponse[]>();
  protected readonly busqueda = signal('');

  protected readonly saludo = computed(() => saludoDelDia(this.auth.user()?.nombreCompleto ?? ''));
  protected readonly fecha = computed(() => {
    const texto = fechaLarga();
    return texto.charAt(0).toUpperCase() + texto.slice(1);
  });

  protected readonly acciones: readonly AccionRapida[] = [
    {
      label: 'Registrar llegadas',
      icono: 'pi-check-square',
      enlace: '/recepcion',
      detalle: 'Citas y cola de turnos',
    },
    {
      label: 'Pacientes',
      icono: 'pi-id-card',
      enlace: '/recepcion/pacientes',
      detalle: 'Buscar o registrar',
    },
    {
      label: 'Reservar en mostrador',
      icono: 'pi-calendar-plus',
      enlace: '/recepcion/citas/nueva',
      detalle: 'Nueva cita',
    },
    {
      label: 'Sala de espera',
      icono: 'pi-desktop',
      enlace: '/recepcion/pantalla',
      detalle: 'Pantalla de turnos',
    },
    {
      label: 'Mapa de camas',
      icono: 'pi-building',
      enlace: '/hospitalizacion',
      detalle: 'Solo consulta',
    },
  ];

  protected readonly porLlegar = computed(() =>
    (this.citas.value() ?? [])
      .filter(
        (cita) => !cita.llego && (cita.estado === 'PENDIENTE' || cita.estado === 'CONFIRMADA'),
      )
      .sort((a, b) => a.fechaHora.localeCompare(b.fechaHora)),
  );

  protected readonly lineaCitas = computed<ItemLineaTiempo[]>(() =>
    this.porLlegar()
      .slice(0, 8)
      .map((cita) => ({
        id: cita.id,
        hora: horaCorta(cita.fechaHora),
        titulo: cita.pacienteNombre,
        detalle: `${cita.medicoNombre}${cita.especialidadNombre ? ' · ' + cita.especialidadNombre : ''}`,
        estado: cita.estado === 'CONFIRMADA' ? 'Confirmada' : 'Pendiente',
        tono: cita.estado === 'CONFIRMADA' ? 'info' : 'aviso',
        sensible: true,
      })),
  );

  protected readonly lineaCola = computed<ItemTarea[]>(() =>
    (this.cola.value() ?? [])
      .filter((turno) => turno.estado === 'EN_ESPERA' || turno.estado === 'LLAMADO')
      .slice(0, 8)
      .map((turno) => ({
        id: turno.id,
        titulo: `${turno.numero} · ${turno.pacienteNombre}`,
        detalle:
          turno.estado === 'LLAMADO'
            ? `Llamado${turno.consultorio ? ' a ' + turno.consultorio : ''}`
            : `Espera de ${turno.esperaMinutos} min`,
        cantidad: null,
        tono:
          turno.prioridad === 'URGENTE'
            ? 'peligro'
            : turno.prioridad === 'PREFERENTE'
              ? 'aviso'
              : 'neutro',
        icono: 'pi-user',
        sensible: true,
      })),
  );

  ngOnInit(): void {
    this.seguros.cargarResumen();
    this.resumen.load(this.api.resumen());
    this.citas.load(this.api.citasHoy());
    this.cola.load(this.api.cola());
  }

  protected buscar(): void {
    const q = this.busqueda().trim();
    void this.router.navigate(['/recepcion/pacientes'], { queryParams: q ? { q } : {} });
  }
}
