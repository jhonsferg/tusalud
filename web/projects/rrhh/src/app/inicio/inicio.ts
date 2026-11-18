import { ChangeDetectionStrategy, Component, OnInit, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import {
  AsyncState,
  AuthService,
  AusenciaResponse,
  ResumenRrhhResponse,
  RrhhApiService,
} from '@tusalud/core';
import { PageHeader } from '@tusalud/ui/page-header';
import {
  AccionRapida,
  BarList,
  ItemBarra,
  ItemTarea,
  QuickActions,
  SectionCard,
  StatCard,
  TaskList,
  fechaLarga,
  saludoDelDia,
} from '@tusalud/ui/tablero';

const AREAS: Readonly<Record<string, string>> = {
  MEDICA: 'Médica',
  ENFERMERIA: 'Enfermería',
  LABORATORIO: 'Laboratorio',
  IMAGENES: 'Imágenes',
  FARMACIA: 'Farmacia',
  ALMACEN: 'Almacén',
  RECEPCION: 'Recepción',
  CAJA: 'Caja',
  ADMINISTRACION: 'Administración',
  FINANZAS: 'Finanzas',
  OPERACIONES: 'Operaciones',
  RRHH: 'Recursos humanos',
  CALIDAD: 'Calidad',
  GERENCIA: 'Gerencia',
};

@Component({
  selector: 'app-inicio-rrhh',
  imports: [RouterLink, PageHeader, SectionCard, StatCard, TaskList, BarList, QuickActions],
  template: `
    <app-page-header [title]="saludo()" [subtitle]="fecha()" icon="pi-id-card" />

    <div class="fila-stats stats">
      <app-stat-card
        etiqueta="Solicitudes por resolver"
        [valor]="resumen.value()?.solicitudesPendientes ?? 0"
        icono="pi-inbox"
        [tono]="(resumen.value()?.solicitudesPendientes ?? 0) > 0 ? 'aviso' : 'exito'"
        enlace="/rrhh/ausencias"
      />
      <app-stat-card
        etiqueta="Plantilla activa"
        [valor]="resumen.value()?.activos ?? 0"
        icono="pi-users"
        [detalle]="'de ' + (resumen.value()?.totalEmpleados ?? 0) + ' fichas'"
        enlace="/rrhh/empleados"
      />
      <app-stat-card
        etiqueta="Ausentes hoy"
        [valor]="resumen.value()?.ausentesHoy ?? 0"
        icono="pi-user-minus"
        tono="info"
        enlace="/rrhh/ausencias"
      />
      <app-stat-card
        etiqueta="En licencia"
        [valor]="resumen.value()?.enLicencia ?? 0"
        icono="pi-heart"
      />
    </div>

    <div class="tablero">
      <div class="c6">
        <app-section-card
          titulo="Ausencias por resolver"
          icono="pi-calendar-minus"
          [cargando]="ausencias.loading() && !ausencias.hasValue()"
          [error]="ausencias.error()?.message ?? null"
          [vacio]="listaAusencias().length === 0 && ausencias.hasValue()"
          textoVacio="No hay solicitudes pendientes."
        >
          <app-task-list [items]="listaAusencias()" />
          <a pie routerLink="/rrhh/ausencias">Abrir la bandeja</a>
        </app-section-card>
      </div>

      <div class="c6">
        <app-section-card
          titulo="Contratos por vencer"
          icono="pi-file-edit"
          [cargando]="resumen.loading() && !resumen.hasValue()"
          [vacio]="contratos().length === 0 && resumen.hasValue()"
          textoVacio="No hay contratos por vencer."
        >
          <app-task-list [items]="contratos()" />
          <a pie routerLink="/rrhh/empleados">Ver empleados</a>
        </app-section-card>
      </div>

      <div class="c6">
        <app-section-card
          titulo="Plantilla por área"
          icono="pi-sitemap"
          [vacio]="barras().length === 0 && resumen.hasValue()"
        >
          <app-bar-list [items]="barras()" etiqueta="Empleados por área" />
        </app-section-card>
      </div>

      <div class="c6">
        <app-section-card titulo="Accesos" icono="pi-bolt">
          <app-quick-actions [acciones]="acciones" />
        </app-section-card>
      </div>
    </div>
  `,
  styles: `
    .stats {
      margin-bottom: var(--sp-4);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InicioRrhh implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly api = inject(RrhhApiService);

  protected readonly resumen = new AsyncState<ResumenRrhhResponse>();
  protected readonly ausencias = new AsyncState<AusenciaResponse[]>();

  protected readonly saludo = computed(() => saludoDelDia(this.auth.user()?.nombreCompleto ?? ''));
  protected readonly fecha = computed(() => {
    const texto = fechaLarga();
    return texto.charAt(0).toUpperCase() + texto.slice(1);
  });

  protected readonly acciones: readonly AccionRapida[] = [
    {
      label: 'Empleados',
      icono: 'pi-users',
      enlace: '/rrhh/empleados',
      detalle: 'Fichas laborales',
    },
    {
      label: 'Turnos',
      icono: 'pi-clock',
      enlace: '/rrhh/turnos',
      detalle: 'Planificación semanal',
    },
    {
      label: 'Ausencias',
      icono: 'pi-calendar-minus',
      enlace: '/rrhh/ausencias',
      detalle: 'Aprobar o rechazar',
    },
  ];

  protected readonly listaAusencias = computed<ItemTarea[]>(() =>
    (this.ausencias.value() ?? [])
      .filter((a) => a.estado === 'SOLICITADA')
      .slice(0, 8)
      .map((a): ItemTarea => ({
        id: a.id,
        titulo: a.empleadoNombre,
        detalle: `${AREAS[a.area] ?? a.area} · ${a.dias} ${a.dias === 1 ? 'día' : 'días'} desde ${a.fechaInicio}`,
        tono: 'aviso',
        icono: 'pi-calendar',
        enlace: '/rrhh/ausencias',
        sensible: true,
      })),
  );

  protected readonly contratos = computed<ItemTarea[]>(() =>
    (this.resumen.value()?.contratosPorVencer ?? []).slice(0, 8).map((c): ItemTarea => ({
      id: c.empleadoId,
      titulo: c.nombreCompleto,
      detalle: `${c.cargo} · vence el ${c.fechaFin}`,
      cantidad: c.diasRestantes,
      tono: c.diasRestantes <= 15 ? 'peligro' : 'aviso',
      icono: 'pi-file-edit',
      enlace: ['/rrhh/empleados'],
      sensible: true,
    })),
  );

  protected readonly barras = computed<ItemBarra[]>(() =>
    (this.resumen.value()?.porArea ?? [])
      .filter((a) => a.total > 0)
      .sort((a, b) => b.total - a.total)
      .map((a): ItemBarra => ({ etiqueta: AREAS[a.area] ?? a.area, valor: a.total })),
  );

  ngOnInit(): void {
    this.resumen.load(this.api.resumen());
    this.ausencias.load(this.api.ausencias({ estado: 'SOLICITADA' }));
  }
}
