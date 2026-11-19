import { ChangeDetectionStrategy, Component, OnInit, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import {
  AsyncState,
  AuthService,
  Cie10ApiService,
  Cie10FrecuenteResponse,
  CitaResponse,
  ImagenesApiService,
  LaboratorioApiService,
  MedicosApiService,
  OrdenImagenResponse,
  OrdenResponse,
  RecepcionApiService,
  TurnoResponse,
} from '@tusalud/core';
import { ProximaTeleconsulta } from '@tusalud/features/telemedicina';
import { PageHeader } from '@tusalud/ui/page-header';
import {
  AccionRapida,
  ItemLineaTiempo,
  ItemTarea,
  QuickActions,
  SectionCard,
  StatCard,
  TaskList,
  Timeline,
  TonoTablero,
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
  PENDIENTE: 'Por confirmar',
  CONFIRMADA: 'Confirmada',
  ATENDIDA: 'Atendida',
  CANCELADA: 'Cancelada',
};

function hora(fecha: string): string {
  return new Date(fecha).toLocaleTimeString('es-PE', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  });
}

@Component({
  selector: 'app-inicio-medico',
  imports: [
    RouterLink,
    ProximaTeleconsulta,
    PageHeader,
    SectionCard,
    StatCard,
    Timeline,
    QuickActions,
    TaskList,
  ],
  template: `
    <app-page-header [title]="saludo()" [subtitle]="fecha()" icon="pi-heart" />
    <app-proxima-teleconsulta />

    <div class="fila-stats stats">
      <app-stat-card
        etiqueta="Citas de hoy"
        [valor]="citasHoy().length"
        icono="pi-calendar"
        [detalle]="porAtender() + ' por atender'"
        enlace="/agenda"
      />
      <app-stat-card
        etiqueta="Pacientes en espera"
        [valor]="enEspera().length"
        icono="pi-users"
        [tono]="enEspera().length > 0 ? 'aviso' : 'exito'"
        [detalle]="llamados() + ' llamados'"
        enlace="/cola"
      />
      <app-stat-card
        etiqueta="Resultados de laboratorio"
        [valor]="resultadosLab().length"
        icono="pi-filter"
        tono="info"
        detalle="Completados de tus órdenes"
        enlace="/laboratorio"
      />
      <app-stat-card
        etiqueta="Estudios de imagen"
        [valor]="informados().length"
        icono="pi-image"
        [tono]="criticos().length > 0 ? 'peligro' : 'info'"
        [detalle]="criticos().length + ' con hallazgo crítico'"
        enlace="/imagenes"
      />
    </div>

    <div class="tablero">
      <div class="c7">
        <app-section-card
          titulo="Agenda de hoy"
          icono="pi-calendar-clock"
          [cargando]="agenda.loading() && !agenda.hasValue()"
          [error]="agenda.error()?.message ?? null"
          [vacio]="citasHoy().length === 0 && agenda.hasValue()"
          textoVacio="No tienes citas programadas para hoy."
        >
          <app-timeline [items]="lineaAgenda()" />
          <a pie routerLink="/agenda">Abrir mi agenda</a>
        </app-section-card>
      </div>

      <div class="c5">
        <app-section-card
          titulo="Cola de pacientes"
          icono="pi-users"
          subtitulo="Pacientes que llegaron al consultorio"
          [cargando]="cola.loading() && !cola.hasValue()"
          [vacio]="enCola().length === 0 && cola.hasValue()"
          textoVacio="No hay pacientes en espera."
        >
          <app-task-list [items]="lineaCola()" />
          <a pie routerLink="/cola">Ir a la cola</a>
        </app-section-card>
      </div>

      <div class="c12">
        <app-section-card
          titulo="Mis diagnósticos frecuentes"
          icono="pi-book"
          subtitulo="Códigos CIE-10 que más registras"
          [cargando]="frecuentes.loading() && !frecuentes.hasValue()"
          [vacio]="listaFrecuentes().length === 0 && frecuentes.hasValue()"
          textoVacio="Aún no registras diagnósticos codificados."
        >
          <ul class="frecuentes" aria-label="Diagnósticos más frecuentes">
            @for (d of listaFrecuentes(); track d.codigo) {
              <li>
                <span class="cod">{{ d.codigo }}</span>
                <span>{{ d.descripcion }}</span>
                <strong>{{ d.casos }}</strong>
              </li>
            }
          </ul>
        </app-section-card>
      </div>

      <div class="c12">
        <app-section-card
          titulo="Pendientes clínicos"
          icono="pi-exclamation-circle"
          [vacio]="pendientes().length === 0 && ordenes.hasValue()"
          textoVacio="No tienes pendientes clínicos."
        >
          <app-task-list [items]="pendientes()" />
        </app-section-card>
      </div>

      <div class="c12">
        <app-quick-actions [acciones]="acciones" />
      </div>
    </div>
  `,
  styles: `
    .stats {
      margin-bottom: var(--sp-4);
    }

    .frecuentes {
      display: flex;
      flex-direction: column;
      gap: 0.4rem;
      margin: 0;
      padding: 0;
      list-style: none;
    }

    .frecuentes li {
      display: grid;
      grid-template-columns: 3.6rem minmax(0, 1fr) auto;
      gap: 0.6rem;
    }

    .cod {
      color: var(--app-primary-text, var(--p-primary-color));
      font-family: ui-monospace, 'Cascadia Mono', Consolas, monospace;
      font-weight: 700;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InicioMedico implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly medicosApi = inject(MedicosApiService);
  private readonly recepcionApi = inject(RecepcionApiService);
  private readonly laboratorioApi = inject(LaboratorioApiService);
  private readonly imagenesApi = inject(ImagenesApiService);
  private readonly cie10Api = inject(Cie10ApiService);

  protected readonly agenda = new AsyncState<CitaResponse[]>();
  protected readonly cola = new AsyncState<TurnoResponse[]>();
  protected readonly ordenes = new AsyncState<OrdenResponse[]>();
  protected readonly estudios = new AsyncState<OrdenImagenResponse[]>();
  protected readonly frecuentes = new AsyncState<Cie10FrecuenteResponse[]>();
  protected readonly listaFrecuentes = computed(() => this.frecuentes.value() ?? []);

  protected readonly saludo = computed(() => saludoDelDia(this.auth.user()?.nombreCompleto ?? ''));
  protected readonly fecha = computed(() => {
    const texto = fechaLarga();
    return texto.charAt(0).toUpperCase() + texto.slice(1);
  });

  protected readonly acciones: readonly AccionRapida[] = [
    { label: 'Mi agenda', icono: 'pi-calendar', enlace: '/agenda', detalle: 'Citas del día' },
    { label: 'Cola de pacientes', icono: 'pi-users', enlace: '/cola', detalle: 'Llamar y atender' },
    {
      label: 'Mis pacientes',
      icono: 'pi-id-card',
      enlace: '/pacientes',
      detalle: 'Historias clínicas',
    },
    {
      label: 'Documentos médicos',
      icono: 'pi-file-check',
      enlace: '/documentos',
      detalle: 'Certificados y descansos',
    },
    {
      label: 'Consentimientos',
      icono: 'pi-pencil',
      enlace: '/consentimientos',
      detalle: 'Firma del paciente',
    },
    {
      label: 'Solicitar exámenes',
      icono: 'pi-send',
      enlace: '/laboratorio/solicitar',
      detalle: 'Laboratorio',
    },
    {
      label: 'Solicitar estudio',
      icono: 'pi-image',
      enlace: '/imagenes/solicitar',
      detalle: 'Imágenes',
    },
    {
      label: 'Mapa de camas',
      icono: 'pi-building',
      enlace: '/hospitalizacion',
      detalle: 'Hospitalización',
    },
  ];

  protected readonly citasHoy = computed(() =>
    (this.agenda.value() ?? [])
      .filter((cita) => cita.estado !== 'CANCELADA')
      .sort((a, b) => a.fechaHora.localeCompare(b.fechaHora)),
  );
  protected readonly porAtender = computed(
    () => this.citasHoy().filter((cita) => cita.estado !== 'ATENDIDA').length,
  );
  protected readonly enCola = computed(() =>
    (this.cola.value() ?? []).filter((t) => t.estado === 'EN_ESPERA' || t.estado === 'LLAMADO'),
  );
  protected readonly enEspera = computed(() =>
    this.enCola().filter((t) => t.estado === 'EN_ESPERA'),
  );
  protected readonly llamados = computed(
    () => this.enCola().filter((t) => t.estado === 'LLAMADO').length,
  );
  protected readonly resultadosLab = computed(() =>
    (this.ordenes.value() ?? []).filter((orden) => orden.estado === 'COMPLETADA'),
  );
  protected readonly informados = computed(() =>
    (this.estudios.value() ?? []).filter((estudio) => estudio.estado === 'INFORMADA'),
  );
  protected readonly criticos = computed(() =>
    this.informados().filter((estudio) => estudio.hallazgoCritico),
  );

  protected readonly lineaAgenda = computed<ItemLineaTiempo[]>(() => {
    const ahora = Date.now();
    let marcada = false;
    return this.citasHoy().map((cita) => {
      const futura = new Date(cita.fechaHora).getTime() >= ahora - 15 * 60_000;
      const resaltado = futura && !marcada && cita.estado !== 'ATENDIDA';
      marcada = marcada || resaltado;
      return {
        id: cita.id,
        hora: hora(cita.fechaHora),
        titulo: cita.pacienteNombre,
        detalle: cita.motivo,
        estado: ETIQUETA_CITA[cita.estado],
        tono: TONO_CITA[cita.estado],
        enlace: ['/citas', cita.id],
        resaltado,
        sensible: true,
      };
    });
  });

  protected readonly lineaCola = computed<ItemTarea[]>(() =>
    this.enCola()
      .slice(0, 6)
      .map((turno) => ({
        id: turno.id,
        titulo: `${turno.numero} · ${turno.pacienteNombre}`,
        detalle: turno.estado === 'LLAMADO' ? 'Llamado' : `Espera de ${turno.esperaMinutos} min`,
        tono:
          turno.prioridad === 'URGENTE'
            ? 'peligro'
            : turno.prioridad === 'PREFERENTE'
              ? 'aviso'
              : 'neutro',
        icono: 'pi-user',
        enlace: '/cola',
        sensible: true,
      })),
  );

  protected readonly pendientes = computed<ItemTarea[]>(() => {
    const tareas: ItemTarea[] = [];
    if (this.criticos().length > 0) {
      tareas.push({
        id: 'criticos',
        titulo: 'Estudios de imagen con hallazgo crítico',
        detalle: 'Revísalos y contacta al paciente cuanto antes',
        cantidad: this.criticos().length,
        tono: 'peligro',
        icono: 'pi-exclamation-triangle',
        enlace: '/imagenes',
      });
    }
    if (this.resultadosLab().length > 0) {
      tareas.push({
        id: 'laboratorio',
        titulo: 'Resultados de laboratorio publicados',
        cantidad: this.resultadosLab().length,
        tono: 'info',
        icono: 'pi-filter',
        enlace: '/laboratorio',
      });
    }
    const otros = this.informados().length - this.criticos().length;
    if (otros > 0) {
      tareas.push({
        id: 'imagenes',
        titulo: 'Informes de imagen disponibles',
        cantidad: otros,
        tono: 'info',
        icono: 'pi-image',
        enlace: '/imagenes',
      });
    }
    return tareas;
  });

  ngOnInit(): void {
    this.agenda.load(this.medicosApi.miAgenda());
    this.cola.load(this.recepcionApi.cola());
    this.ordenes.load(this.laboratorioApi.listarOrdenes({ estado: 'COMPLETADA' }));
    this.estudios.load(this.imagenesApi.listarOrdenes({ estado: 'INFORMADA' }));
    this.frecuentes.load(this.cie10Api.frecuentes(5));
  }
}
