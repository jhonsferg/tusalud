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
  ETIQUETA_METODO_CAJA,
  ETIQUETA_ORIGEN_INGRESO,
  ReporteFinancieroResponse,
  ReportesApiService,
  SegurosStore,
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

const soles = (valor: number): string =>
  `S/ ${valor.toLocaleString('es-PE', { maximumFractionDigits: 0 })}`;

@Component({
  selector: 'app-inicio-finanzas',
  imports: [
    CurrencyPipe,
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
    <app-page-header [title]="saludo()" [subtitle]="fecha()" icon="pi-money-bill">
      <app-selector-periodo [valor]="dias()" (cambio)="cambiarPeriodo($event)" />
    </app-page-header>

    @if (reporte.error(); as error) {
      <p class="error" role="alert">{{ error.message }}</p>
    }

    @if (reporte.value(); as r) {
      <div class="fila-stats stats">
        <app-stat-card
          etiqueta="Total cobrado"
          [valor]="(r.totalCobrado | currency: 'PEN' : 'symbol-narrow') ?? ''"
          icono="pi-dollar"
          tono="exito"
          [serie]="serie()"
          [detalle]="r.transacciones + ' transacciones'"
          enlace="/reportes/financiero"
        />
        <app-stat-card
          etiqueta="Ticket promedio"
          [valor]="(r.ticketPromedio | currency: 'PEN' : 'symbol-narrow') ?? ''"
          icono="pi-receipt"
        />
        <app-stat-card
          etiqueta="Por cobrar"
          [valor]="(r.porCobrar.total | currency: 'PEN' : 'symbol-narrow') ?? ''"
          icono="pi-clock"
          [tono]="r.porCobrar.total > 0 ? 'aviso' : 'neutro'"
          [detalle]="r.porCobrar.citas.cantidad + r.porCobrar.laboratorio.cantidad + ' cuentas'"
        />
        <app-stat-card
          etiqueta="Anulación de comprobantes"
          [valor]="dec(r.comprobantes.tasaAnulacionPct) + '%'"
          icono="pi-ban"
          [tono]="r.comprobantes.tasaAnulacionPct > 5 ? 'aviso' : 'neutro'"
          [detalle]="
            r.comprobantes.anulados + ' de ' + (r.comprobantes.emitidos + r.comprobantes.anulados)
          "
        />
        <app-stat-card
          etiqueta="Diferencia neta de caja"
          [valor]="(r.cajas.neto | currency: 'PEN' : 'symbol-narrow') ?? ''"
          icono="pi-exclamation-triangle"
          [tono]="r.cajas.neto === 0 ? 'neutro' : 'aviso'"
          [detalle]="r.cajas.sesionesCerradas + ' cierres'"
        />
      </div>
    }

    @if (seguros.resumen.value(); as s) {
      <div class="fila-stats stats">
        <app-stat-card
          etiqueta="Cartera de aseguradoras"
          [valor]="(s.carteraPendiente ?? 0 | currency: 'PEN' : 'symbol-narrow') ?? ''"
          icono="pi-briefcase"
          [tono]="(s.carteraPendiente ?? 0) > 0 ? 'aviso' : 'neutro'"
          [detalle]="(s.cuentasPendientes ?? 0) + ' cuentas pendientes'"
          enlace="/seguros/cartera"
        />
        <app-stat-card
          etiqueta="Liquidaciones abiertas"
          [valor]="s.liquidacionesAbiertas ?? 0"
          icono="pi-file-edit"
          enlace="/seguros/liquidaciones"
        />
        <app-stat-card
          etiqueta="Preautorizaciones por resolver"
          [valor]="s.preautorizacionesPendientes ?? 0"
          icono="pi-verified"
          [tono]="(s.preautorizacionesPendientes ?? 0) > 0 ? 'aviso' : 'neutro'"
          enlace="/seguros/preautorizaciones"
        />
        <app-stat-card
          etiqueta="Pólizas por vencer"
          [valor]="s.polizasPorVencer ?? 0"
          icono="pi-clock"
          detalle="En los próximos 30 días"
        />
      </div>
    }
    <div class="tablero">
      <div class="c8">
        <app-section-card
          titulo="Cobros por día"
          icono="pi-chart-bar"
          [cargando]="reporte.loading() && !reporte.hasValue()"
          [vacio]="barras().length === 0 && reporte.hasValue()"
          textoVacio="No hay cobros en el periodo."
        >
          <app-bar-chart
            [datos]="barras()"
            titulo="Cobros por día en soles"
            [formato]="formatoSoles"
          />
        </app-section-card>
      </div>

      <div class="c4">
        <app-section-card
          titulo="Por método de pago"
          icono="pi-credit-card"
          [vacio]="porMetodo().length === 0 && reporte.hasValue()"
        >
          <app-bar-list [items]="porMetodo()" etiqueta="Total cobrado por método de pago" />
        </app-section-card>
      </div>

      <div class="c6">
        <app-section-card
          titulo="Por origen del ingreso"
          icono="pi-sitemap"
          [vacio]="porOrigen().length === 0 && reporte.hasValue()"
        >
          <app-bar-list [items]="porOrigen()" etiqueta="Total por origen" />
        </app-section-card>
      </div>

      <div class="c6">
        <app-section-card
          titulo="Diferencias de caja por cajero"
          icono="pi-wallet"
          [vacio]="cajeros().length === 0 && reporte.hasValue()"
          textoVacio="No hay cierres de caja en el periodo."
        >
          <app-task-list [items]="cajeros()" />
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
export class InicioFinanzas implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly api = inject(ReportesApiService);
  protected readonly seguros = inject(SegurosStore);

  protected readonly dec = dec;
  protected readonly reporte = new AsyncState<ReporteFinancieroResponse>();
  protected readonly dias = signal(leerPeriodo());
  protected readonly formatoSoles = soles;

  protected readonly saludo = computed(() => saludoDelDia(this.auth.user()?.nombreCompleto ?? ''));
  protected readonly fecha = computed(() => {
    const texto = fechaLarga();
    return texto.charAt(0).toUpperCase() + texto.slice(1);
  });

  protected readonly acciones: readonly AccionRapida[] = [
    {
      label: 'Reporte financiero',
      icono: 'pi-money-bill',
      enlace: '/reportes/financiero',
      detalle: 'Detalle completo del periodo',
    },
  ];

  protected readonly serie = computed(() =>
    (this.reporte.value()?.porDia ?? []).map((d) => d.total),
  );
  protected readonly barras = computed<PuntoBarra[]>(() =>
    (this.reporte.value()?.porDia ?? []).map((d) => ({
      etiqueta: fechaCorta(d.fecha),
      valor: d.total,
    })),
  );

  protected readonly porMetodo = computed<ItemBarra[]>(() =>
    (this.reporte.value()?.porMetodo ?? []).map((m): ItemBarra => ({
      etiqueta: ETIQUETA_METODO_CAJA[m.metodo] ?? m.metodo,
      valor: m.total,
      texto: soles(m.total),
    })),
  );

  protected readonly porOrigen = computed<ItemBarra[]>(() =>
    (this.reporte.value()?.porOrigen ?? []).map((o): ItemBarra => ({
      etiqueta: `${ETIQUETA_ORIGEN_INGRESO[o.origen] ?? o.origen}${o.devengado ? ' (devengado)' : ''}`,
      valor: o.total,
      texto: soles(o.total),
      tono: o.devengado ? 'info' : 'neutro',
    })),
  );

  protected readonly cajeros = computed<ItemTarea[]>(() =>
    (this.reporte.value()?.cajas.porCajero ?? []).map((c): ItemTarea => ({
      id: c.nombre,
      titulo: c.nombre,
      detalle: `${c.sesiones} cierres · faltante ${soles(c.faltante)} · sobrante ${soles(c.sobrante)}`,
      tono: c.neto === 0 ? 'exito' : 'aviso',
      icono: c.neto === 0 ? 'pi-check' : 'pi-exclamation-triangle',
    })),
  );

  ngOnInit(): void {
    this.seguros.cargarResumen();
    this.cargar();
  }

  protected cambiarPeriodo(dias: number): void {
    this.dias.set(dias);
    guardarPeriodo(dias);
    this.cargar();
  }

  private cargar(): void {
    this.reporte.load(
      this.api.financiero({
        desde: inicioIso(this.dias() - 1),
        hasta: hoyIso(),
        agrupacion: 'DIA',
      }),
    );
  }
}
