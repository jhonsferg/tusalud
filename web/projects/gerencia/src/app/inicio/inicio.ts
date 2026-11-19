import { CurrencyPipe } from '@angular/common';
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
  ReporteEjecutivoResponse,
  ReportesApiService,
} from '@tusalud/core';
import { PageHeader } from '@tusalud/ui/page-header';
import {
  AccionRapida,
  BarChart,
  PuntoBarra,
  QuickActions,
  SectionCard,
  SelectorPeriodo,
  StatCard,
  fechaCorta,
  dec,
  fechaLarga,
  guardarPeriodo,
  hoyIso,
  inicioIso,
  leerPeriodo,
  saludoDelDia,
} from '@tusalud/ui/tablero';

@Component({
  selector: 'app-inicio-gerencia',
  imports: [
    CurrencyPipe,
    PageHeader,
    SectionCard,
    StatCard,
    BarChart,
    QuickActions,
    SelectorPeriodo,
  ],
  template: `
    <app-page-header [title]="saludo()" [subtitle]="fecha()" icon="pi-briefcase">
      <app-selector-periodo [valor]="dias()" (cambio)="cambiarPeriodo($event)" />
    </app-page-header>

    @if (reporte.error(); as error) {
      <p class="error" role="alert">{{ error.message }}</p>
    }

    @if (reporte.value(); as r) {
      <div class="fila-stats stats">
        <app-stat-card
          etiqueta="Ingresos"
          [valor]="(r.ingresos.actual | currency: 'PEN' : 'symbol-narrow') ?? ''"
          icono="pi-dollar"
          tono="exito"
          [tendencia]="r.ingresos.variacionPct ?? null"
          [serie]="serieIngresos()"
          detalle="vs. periodo anterior"
          enlace="/reportes/financiero"
        />
        <app-stat-card
          etiqueta="Citas"
          [valor]="r.citas.actual"
          icono="pi-calendar"
          [tendencia]="r.citas.variacionPct ?? null"
          [serie]="serieCitas()"
          [detalle]="r.atendidas.actual + ' atendidas'"
          enlace="/reportes/ejecutivo"
        />
        <app-stat-card
          etiqueta="Ocupación hospitalaria"
          [valor]="dec(r.ocupacionHospitalariaPct.actual) + '%'"
          icono="pi-building"
          [tendencia]="r.ocupacionHospitalariaPct.variacionPct ?? null"
          enlace="/reportes/operativo"
        />
        <app-stat-card
          etiqueta="Satisfacción"
          [valor]="dec(r.valoracionPromedio.actual) + ' / 5'"
          icono="pi-star"
          tono="info"
          [tendencia]="r.valoracionPromedio.variacionPct ?? null"
          enlace="/reportes/calidad"
        />
        <app-stat-card
          etiqueta="Ticket promedio"
          [valor]="(r.ticketPromedio.actual | currency: 'PEN' : 'symbol-narrow') ?? ''"
          icono="pi-receipt"
          [tendencia]="r.ticketPromedio.variacionPct ?? null"
        />
        <app-stat-card
          etiqueta="Espera promedio"
          [valor]="dec(r.esperaPromedioMin.actual) + ' min'"
          icono="pi-clock"
          [tono]="r.esperaPromedioMin.actual > 30 ? 'aviso' : 'neutro'"
          [tendencia]="r.esperaPromedioMin.variacionPct ?? null"
        />
        <app-stat-card
          etiqueta="Inasistencia"
          [valor]="dec(r.tasaAusenciaPct.actual) + '%'"
          icono="pi-user-minus"
          [tono]="r.tasaAusenciaPct.actual > 15 ? 'aviso' : 'neutro'"
          [tendencia]="r.tasaAusenciaPct.variacionPct ?? null"
        />
        <app-stat-card
          etiqueta="Citas canceladas"
          [valor]="r.canceladas.actual"
          icono="pi-ban"
          [tendencia]="r.canceladas.variacionPct ?? null"
        />
      </div>
    }

    <div class="tablero">
      <div class="c6">
        <app-section-card
          titulo="Ingresos por día"
          icono="pi-chart-bar"
          [cargando]="reporte.loading() && !reporte.hasValue()"
          [vacio]="barrasIngresos().length === 0 && reporte.hasValue()"
          textoVacio="No hay ingresos en el periodo."
        >
          <app-bar-chart
            [datos]="barrasIngresos()"
            titulo="Ingresos por día en soles"
            [formato]="formatoSoles"
          />
        </app-section-card>
      </div>

      <div class="c6">
        <app-section-card
          titulo="Citas por día"
          icono="pi-calendar"
          [cargando]="reporte.loading() && !reporte.hasValue()"
          [vacio]="barrasCitas().length === 0 && reporte.hasValue()"
          textoVacio="No hay citas en el periodo."
        >
          <app-bar-chart [datos]="barrasCitas()" titulo="Citas por día" />
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
export class InicioGerencia implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly api = inject(ReportesApiService);

  protected readonly dec = dec;
  protected readonly reporte = new AsyncState<ReporteEjecutivoResponse>();
  protected readonly dias = signal(leerPeriodo());
  protected readonly formatoSoles = (valor: number): string => `S/ ${valor.toFixed(0)}`;

  protected readonly saludo = computed(() => saludoDelDia(this.auth.user()?.nombreCompleto ?? ''));
  protected readonly fecha = computed(() => {
    const texto = fechaLarga();
    return texto.charAt(0).toUpperCase() + texto.slice(1);
  });

  protected readonly acciones: readonly AccionRapida[] = [
    {
      label: 'Reporte ejecutivo',
      icono: 'pi-chart-line',
      enlace: '/reportes/ejecutivo',
      detalle: 'Indicadores del periodo',
    },
    {
      label: 'Financiero',
      icono: 'pi-money-bill',
      enlace: '/reportes/financiero',
      detalle: 'Ingresos y caja',
    },
    {
      label: 'Morbilidad',
      icono: 'pi-heart',
      enlace: '/reportes/morbilidad',
      detalle: 'Diagnósticos más frecuentes',
    },
    {
      label: 'Operativo',
      icono: 'pi-sitemap',
      enlace: '/reportes/operativo',
      detalle: 'Ocupación y esperas',
    },
    {
      label: 'Calidad',
      icono: 'pi-verified',
      enlace: '/reportes/calidad',
      detalle: 'Satisfacción y tiempos',
    },
    {
      label: 'Recursos humanos',
      icono: 'pi-id-card',
      enlace: '/rrhh',
      detalle: 'Resumen de plantilla',
    },
  ];

  protected readonly serieIngresos = computed(() =>
    (this.reporte.value()?.serie ?? []).map((p) => p.ingresos),
  );
  protected readonly serieCitas = computed(() =>
    (this.reporte.value()?.serie ?? []).map((p) => p.citas),
  );
  protected readonly barrasIngresos = computed<PuntoBarra[]>(() =>
    (this.reporte.value()?.serie ?? []).map((p) => ({
      etiqueta: fechaCorta(p.fecha),
      valor: p.ingresos,
    })),
  );
  protected readonly barrasCitas = computed<PuntoBarra[]>(() =>
    (this.reporte.value()?.serie ?? []).map((p) => ({
      etiqueta: fechaCorta(p.fecha),
      valor: p.citas,
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
      this.api.ejecutivo({
        desde: inicioIso(this.dias() - 1),
        hasta: hoyIso(),
        agrupacion: this.dias() > 30 ? 'SEMANA' : 'DIA',
      }),
    );
  }
}
