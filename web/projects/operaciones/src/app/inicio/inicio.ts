import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';

import {
  AsyncState,
  AuthService,
  ReporteOperativoResponse,
  ReportesApiService,
} from '@tusalud/core';
import { PageHeader } from '@tusalud/ui/page-header';
import {
  AccionRapida,
  BarChart,
  BarList,
  ItemBarra,
  ItemTarea,
  PuntoBarra,
  QuickActions,
  SectionCard,
  SelectorPeriodo,
  StatCard,
  TaskList,
  fechaCorta,
  dec,
  fechaLarga,
  guardarPeriodo,
  hoyIso,
  inicioIso,
  leerPeriodo,
  saludoDelDia,
} from '@tusalud/ui/tablero';

const AREAS: Readonly<Record<string, string>> = {
  HOSPITALIZACION: 'Hospitalización',
  EMERGENCIA: 'Emergencia',
  UCI: 'Cuidados intensivos',
  OBSERVACION: 'Observación',
};

@Component({
  selector: 'app-inicio-operaciones',
  imports: [
    PageHeader,
    SectionCard,
    StatCard,
    BarChart,
    BarList,
    TaskList,
    QuickActions,
    SelectorPeriodo,
  ],
  template: `
    <app-page-header [title]="saludo()" [subtitle]="fecha()" icon="pi-sitemap">
      <app-selector-periodo [valor]="dias()" (cambio)="cambiarPeriodo($event)" />
    </app-page-header>

    @if (reporte.error(); as error) {
      <p class="error" role="alert">{{ error.message }}</p>
    }

    @if (reporte.value(); as r) {
      <div class="fila-stats stats">
        <app-stat-card
          etiqueta="Ocupación promedio"
          [valor]="dec(r.ocupacionPromedioPct) + '%'"
          icono="pi-building"
          [tono]="r.ocupacionPromedioPct > 85 ? 'aviso' : 'neutro'"
          enlace="/reportes/operativo"
        />
        <app-stat-card
          etiqueta="Espera promedio"
          [valor]="dec(r.esperas.promedioMin) + ' min'"
          icono="pi-clock"
          [tono]="r.esperas.promedioMin > 30 ? 'aviso' : 'neutro'"
          [detalle]="'Percentil 90: ' + dec(r.esperas.p90Min) + ' min'"
        />
        <app-stat-card
          etiqueta="Estancia promedio"
          [valor]="dec(r.estanciaPromedioDias) + ' días'"
          icono="pi-calendar"
          [detalle]="r.altas + ' altas'"
        />
        <app-stat-card
          etiqueta="Lotes por vencer"
          [valor]="r.inventario.lotesPorVencer"
          icono="pi-box"
          [tono]="r.inventario.lotesPorVencer > 0 ? 'aviso' : 'exito'"
          [detalle]="r.inventario.lotesVencidos + ' vencidos'"
        />
      </div>
    }

    <div class="tablero">
      <div class="c8">
        <app-section-card
          titulo="Ocupación hospitalaria"
          icono="pi-chart-line"
          [cargando]="reporte.loading() && !reporte.hasValue()"
          [vacio]="serie().length === 0 && reporte.hasValue()"
          textoVacio="No hay datos de ocupación en el periodo."
        >
          <app-bar-chart
            [datos]="serie()"
            titulo="Ocupación diaria en porcentaje"
            [formato]="formatoPorcentaje"
          />
        </app-section-card>
      </div>

      <div class="c4">
        <app-section-card
          titulo="Camas por área"
          icono="pi-th-large"
          [vacio]="areas().length === 0 && reporte.hasValue()"
        >
          <app-bar-list [items]="areas()" etiqueta="Ocupación actual por área" [maximo]="100" />
        </app-section-card>
      </div>

      <div class="c6">
        <app-section-card
          titulo="Emergencias por nivel de triaje"
          icono="pi-exclamation-circle"
          [vacio]="niveles().length === 0 && reporte.hasValue()"
          textoVacio="Sin emergencias en el periodo."
        >
          <app-bar-list [items]="niveles()" etiqueta="Emergencias por nivel" />
        </app-section-card>
      </div>

      <div class="c6">
        <app-section-card
          titulo="Inventario crítico"
          icono="pi-box"
          [vacio]="criticos().length === 0 && reporte.hasValue()"
          textoVacio="Ningún producto bajo el mínimo."
        >
          <app-task-list [items]="criticos()" />
        </app-section-card>
      </div>

      <div class="c12">
        <app-quick-actions [acciones]="acciones" etiqueta="Reportes" />
      </div>
    </div>
  `,
  styles: `
    .stats {
      margin-bottom: var(--sp-4);
    }

    .error {
      padding: var(--sp-3);
      border-radius: var(--radio-sm);
      background: var(--estado-peligro-fondo);
      color: var(--estado-peligro);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InicioOperaciones implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly api = inject(ReportesApiService);

  protected readonly dec = dec;
  protected readonly reporte = new AsyncState<ReporteOperativoResponse>();
  protected readonly dias = signal(leerPeriodo());
  protected readonly formatoPorcentaje = (valor: number): string => `${Math.round(valor)}%`;

  protected readonly saludo = computed(() => saludoDelDia(this.auth.user()?.nombreCompleto ?? ''));
  protected readonly fecha = computed(() => {
    const texto = fechaLarga();
    return texto.charAt(0).toUpperCase() + texto.slice(1);
  });

  protected readonly acciones: readonly AccionRapida[] = [
    {
      label: 'Reporte operativo',
      icono: 'pi-sitemap',
      enlace: '/reportes/operativo',
      detalle: 'Demanda, esperas y agenda',
    },
    {
      label: 'Morbilidad',
      icono: 'pi-heart',
      enlace: '/reportes/morbilidad',
      detalle: 'Diagnósticos más frecuentes',
    },
    {
      label: 'Respaldos',
      icono: 'pi-database',
      enlace: '/respaldos',
      detalle: 'Estado y prueba de restauración',
    },
  ];

  protected readonly serie = computed<PuntoBarra[]>(() =>
    (this.reporte.value()?.ocupacionSerie ?? []).map((p) => ({
      etiqueta: fechaCorta(p.fecha),
      valor: p.porcentaje,
    })),
  );

  protected readonly areas = computed<ItemBarra[]>(() =>
    (this.reporte.value()?.camasActuales ?? []).map((a): ItemBarra => ({
      etiqueta: AREAS[a.area] ?? a.area,
      valor: a.porcentaje,
      texto: `${a.ocupadas} de ${a.total} · ${Math.round(a.porcentaje)}%`,
      tono: a.porcentaje > 85 ? 'peligro' : a.porcentaje > 65 ? 'aviso' : 'neutro',
    })),
  );

  protected readonly niveles = computed<ItemBarra[]>(() =>
    (this.reporte.value()?.emergenciasPorNivel ?? []).map((n): ItemBarra => ({
      etiqueta: `Nivel ${n.nivel} · ${n.clasificacion.toLowerCase()}`,
      valor: n.cantidad,
      tono: n.nivel <= 2 ? 'peligro' : n.nivel === 3 ? 'aviso' : 'neutro',
    })),
  );

  protected readonly criticos = computed<ItemTarea[]>(() =>
    (this.reporte.value()?.inventario.bajoMinimo ?? []).slice(0, 8).map((p): ItemTarea => ({
      id: p.codigo,
      titulo: p.nombre,
      detalle: `Stock ${p.stock} de ${p.stockMinimo} mínimo`,
      cantidad: p.stock,
      tono: p.stock === 0 ? 'peligro' : 'aviso',
      icono: 'pi-exclamation-triangle',
    })),
  );

  ngOnInit(): void {
    this.cargar();
  }

  protected cambiarPeriodo(dias: number): void {
    this.dias.set(dias);
    guardarPeriodo(dias);
    this.cargar();
  }

  private cargar(): void {
    this.reporte.load(
      this.api.operativo({
        desde: inicioIso(this.dias() - 1),
        hasta: hoyIso(),
        agrupacion: 'DIA',
      }),
    );
  }
}
