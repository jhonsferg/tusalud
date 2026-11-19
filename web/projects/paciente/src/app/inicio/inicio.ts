import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';

import {
  AsyncState,
  AuthService,
  CitaResponse,
  CitasApiService,
  ClinicaApiService,
  HistoriaClinicaResponse,
  ImagenesApiService,
  LaboratorioApiService,
  OrdenImagenResponse,
  OrdenResponse,
} from '@tusalud/core';
import { ProximaTeleconsulta } from '@tusalud/features/telemedicina';
import { PageHeader } from '@tusalud/ui/page-header';
import {
  AccionRapida,
  ItemLineaTiempo,
  ItemTarea,
  QuickActions,
  SectionCard,
  StatusBadge,
  TaskList,
  TonoTablero,
  Timeline,
  fechaLarga,
  saludoDelDia,
} from '@tusalud/ui/tablero';

const TONO_CITA: Readonly<Record<string, TonoTablero>> = {
  PENDIENTE: 'aviso',
  CONFIRMADA: 'info',
  ATENDIDA: 'exito',
  CANCELADA: 'neutro',
};

const ETIQUETA_CITA: Readonly<Record<string, string>> = {
  PENDIENTE: 'Pendiente',
  CONFIRMADA: 'Confirmada',
  ATENDIDA: 'Atendida',
  CANCELADA: 'Cancelada',
};

@Component({
  selector: 'app-inicio-paciente',
  imports: [
    ProximaTeleconsulta,
    DatePipe,
    RouterLink,
    Button,
    PageHeader,
    SectionCard,
    StatusBadge,
    Timeline,
    QuickActions,
    TaskList,
  ],
  template: `
    <app-page-header [title]="saludo()" [subtitle]="fecha()" icon="pi-heart" />
    <app-proxima-teleconsulta />

    <div class="tablero">
      <div class="c8">
        <app-section-card
          titulo="Tu próxima cita"
          icono="pi-calendar-clock"
          [cargando]="citas.loading() && !citas.hasValue()"
          [error]="citas.error()?.message ?? null"
          [vacio]="!proxima() && citas.hasValue()"
          textoVacio="No tienes citas próximas. Reserva una cuando la necesites."
        >
          <div acciones>
            <p-button
              label="Reservar cita"
              icon="pi pi-plus"
              size="small"
              routerLink="/citas/nueva"
            />
          </div>
          @if (proxima(); as cita) {
            <div class="proxima">
              <div class="fecha-grande" aria-hidden="true">
                <span class="mes">{{ cita.fechaHora | date: 'MMM' }}</span>
                <strong>{{ cita.fechaHora | date: 'd' }}</strong>
                <span class="dia">{{ cita.fechaHora | date: 'EEE' }}</span>
              </div>
              <div class="detalle">
                <h3>{{ cita.especialidadNombre }} con {{ cita.medicoNombre }}</h3>
                <p class="hora">
                  <i class="pi pi-clock" aria-hidden="true"></i>
                  {{ cita.fechaHora | date: 'EEEE d MMMM, HH:mm' }}
                  @if (cita.consultorio) {
                    · {{ cita.consultorio }}
                  }
                </p>
                <p class="motivo">{{ cita.motivo }}</p>
                <div class="estados">
                  <app-status-badge
                    [etiqueta]="etiquetaCita[cita.estado]"
                    [tono]="tonoCita[cita.estado]"
                  />
                  @if (cita.pagado === false) {
                    <app-status-badge etiqueta="Pago pendiente" tono="aviso" icono="pi-wallet" />
                  }
                </div>
              </div>
              <div class="botones">
                <p-button
                  label="Ver detalle"
                  icon="pi pi-eye"
                  [outlined]="true"
                  size="small"
                  [routerLink]="['/citas', cita.id]"
                />
                <p-button
                  label="Reprogramar"
                  icon="pi pi-calendar"
                  severity="secondary"
                  [outlined]="true"
                  size="small"
                  [routerLink]="['/citas', cita.id, 'reprogramar']"
                />
              </div>
            </div>
          }
        </app-section-card>
      </div>

      <div class="c4">
        <app-section-card
          titulo="Mi salud"
          icono="pi-heart-fill"
          [cargando]="historia.loading() && !historia.hasValue()"
        >
          @if (historia.value()?.paciente; as paciente) {
            <dl class="salud">
              <div>
                <dt>Grupo sanguíneo</dt>
                <dd>{{ paciente.grupoSanguineo || 'Sin registrar' }}</dd>
              </div>
              <div>
                <dt>Alergias</dt>
                <dd>{{ paciente.alergias || 'Sin alergias registradas' }}</dd>
              </div>
              <div>
                <dt>Historia clínica</dt>
                <dd>N.º {{ paciente.historialClinicoNumero }}</dd>
              </div>
            </dl>
            <p class="nota text-muted">
              Estos datos los registra tu equipo de salud. Si algo no es correcto, avísanos en el
              centro.
            </p>
          }
        </app-section-card>
      </div>

      <div class="c12">
        <app-quick-actions [acciones]="acciones" />
      </div>

      <div class="c6">
        <app-section-card
          titulo="Pendientes"
          icono="pi-bell"
          subtitulo="Lo que requiere tu atención"
          [vacio]="pendientes().length === 0 && citas.hasValue()"
          textoVacio="Estás al día. No tienes pendientes."
        >
          <app-task-list [items]="pendientes()" />
        </app-section-card>
      </div>

      <div class="c6">
        <app-section-card
          titulo="Próximas citas"
          icono="pi-calendar"
          [vacio]="agenda().length === 0 && citas.hasValue()"
          textoVacio="Sin citas programadas."
        >
          <app-timeline [items]="agenda()" />
          <a pie routerLink="/citas">Ver todas mis citas</a>
        </app-section-card>
      </div>
    </div>
  `,
  styles: `
    .proxima {
      display: grid;
      grid-template-columns: auto minmax(0, 1fr) auto;
      align-items: center;
      gap: var(--sp-5);

      @media (max-width: 760px) {
        grid-template-columns: auto minmax(0, 1fr);

        .botones {
          grid-column: 1 / -1;
        }
      }
    }

    .fecha-grande {
      display: flex;
      flex-direction: column;
      align-items: center;
      min-width: 5rem;
      padding: var(--sp-3);
      border-radius: var(--radio-md);
      background: var(--app-primary-soft);
      color: var(--app-primary-text);
      line-height: 1.1;

      strong {
        font-size: 2.2rem;
      }

      .mes,
      .dia {
        font-size: 0.8125rem;
        font-weight: 700;
        text-transform: uppercase;
        letter-spacing: 0.06em;
      }
    }

    .detalle h3 {
      margin: 0 0 var(--sp-1);
      font-size: 1.15rem;
    }

    .hora,
    .motivo {
      margin: 0 0 var(--sp-1);
    }

    .hora {
      color: var(--p-text-muted-color);
      text-transform: capitalize;
    }

    .estados {
      display: flex;
      flex-wrap: wrap;
      gap: var(--sp-2);
      margin-top: var(--sp-2);
    }

    .botones {
      display: flex;
      flex-direction: column;
      gap: var(--sp-2);
    }

    .salud {
      display: grid;
      gap: var(--sp-3);
      margin: 0;

      dt {
        color: var(--p-text-muted-color);
        font-size: 0.8125rem;
      }

      dd {
        margin: 0;
        font-weight: 600;
      }
    }

    .nota {
      margin: var(--sp-3) 0 0;
      font-size: 0.8125rem;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InicioPaciente implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly citasApi = inject(CitasApiService);
  private readonly clinicaApi = inject(ClinicaApiService);
  private readonly laboratorioApi = inject(LaboratorioApiService);
  private readonly imagenesApi = inject(ImagenesApiService);

  protected readonly citas = new AsyncState<CitaResponse[]>();
  protected readonly historia = new AsyncState<HistoriaClinicaResponse>();
  protected readonly ordenes = new AsyncState<OrdenResponse[]>();
  protected readonly estudios = new AsyncState<OrdenImagenResponse[]>();

  protected readonly etiquetaCita = ETIQUETA_CITA;
  protected readonly tonoCita = TONO_CITA;
  protected readonly saludo = computed(() => saludoDelDia(this.auth.user()?.nombreCompleto ?? ''));
  protected readonly fecha = computed(() => {
    const texto = fechaLarga();
    return texto.charAt(0).toUpperCase() + texto.slice(1);
  });

  protected readonly acciones: readonly AccionRapida[] = [
    {
      label: 'Reservar cita',
      icono: 'pi-calendar-plus',
      enlace: '/citas/nueva',
      detalle: 'Elige médico y horario',
    },
    {
      label: 'Mis resultados',
      icono: 'pi-filter',
      enlace: '/mis-resultados',
      detalle: 'Laboratorio',
    },
    {
      label: 'Mis estudios',
      icono: 'pi-image',
      enlace: '/mis-estudios',
      detalle: 'Imágenes diagnósticas',
    },
    {
      label: 'Mis medicamentos',
      icono: 'pi-heart',
      enlace: '/mis-medicamentos',
      detalle: 'Recetas entregadas',
    },
    { label: 'Mis pagos', icono: 'pi-wallet', enlace: '/pagos', detalle: 'Comprobantes y métodos' },
    {
      label: 'Historia clínica',
      icono: 'pi-folder-open',
      enlace: '/historia-clinica',
      detalle: 'Atenciones',
    },
  ];

  protected readonly activas = computed(() =>
    (this.citas.value() ?? [])
      .filter(
        (cita) =>
          (cita.estado === 'PENDIENTE' || cita.estado === 'CONFIRMADA') &&
          new Date(cita.fechaHora).getTime() >= Date.now() - 30 * 60_000,
      )
      .sort((a, b) => a.fechaHora.localeCompare(b.fechaHora)),
  );

  protected readonly proxima = computed(() => this.activas()[0] ?? null);

  protected readonly agenda = computed<ItemLineaTiempo[]>(() =>
    this.activas()
      .slice(0, 5)
      .map((cita, indice) => ({
        id: cita.id,
        hora: new Date(cita.fechaHora).toLocaleDateString('es-PE', {
          day: 'numeric',
          month: 'short',
        }),
        titulo: `${cita.especialidadNombre} · ${cita.medicoNombre}`,
        detalle: new Date(cita.fechaHora).toLocaleTimeString('es-PE', {
          hour: '2-digit',
          minute: '2-digit',
          hour12: false,
        }),
        estado: ETIQUETA_CITA[cita.estado],
        tono: TONO_CITA[cita.estado],
        enlace: ['/citas', cita.id],
        resaltado: indice === 0,
      })),
  );

  protected readonly pendientes = computed<ItemTarea[]>(() => {
    const tareas: ItemTarea[] = [];
    const porPagar = this.activas().filter((cita) => cita.pagado === false);
    if (porPagar.length > 0) {
      tareas.push({
        id: 'pagos',
        titulo: 'Citas con pago pendiente',
        detalle: 'Paga con tarjeta, Yape o Plin',
        cantidad: porPagar.length,
        tono: 'aviso',
        icono: 'pi-wallet',
        enlace: '/pagos',
      });
    }
    const resultados = (this.ordenes.value() ?? []).filter((o) => o.estado === 'COMPLETADA');
    if (resultados.length > 0) {
      tareas.push({
        id: 'laboratorio',
        titulo: 'Resultados de laboratorio disponibles',
        cantidad: resultados.length,
        tono: 'info',
        icono: 'pi-filter',
        enlace: '/mis-resultados',
      });
    }
    const informados = (this.estudios.value() ?? []).filter((e) => e.estado === 'INFORMADA');
    if (informados.length > 0) {
      tareas.push({
        id: 'imagenes',
        titulo: 'Estudios de imagen informados',
        cantidad: informados.length,
        tono: 'info',
        icono: 'pi-image',
        enlace: '/mis-estudios',
      });
    }
    return tareas;
  });

  ngOnInit(): void {
    this.citas.load(this.citasApi.misCitas());
    this.historia.load(this.clinicaApi.miHistoria());
    this.ordenes.load(this.laboratorioApi.misOrdenes());
    this.estudios.load(this.imagenesApi.misEstudios());
  }
}
