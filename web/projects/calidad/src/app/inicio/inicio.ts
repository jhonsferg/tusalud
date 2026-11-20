import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';

import { AsyncState, AuthService, ReporteCalidadResponse, ReportesApiService } from '@tusalud/core';
import { HeroPortada } from '@tusalud/ui/hero-portada';
import {
  AccionRapida,
  BarList,
  ItemBarra,
  ItemTarea,
  QuickActions,
  SectionCard,
  SelectorPeriodo,
  StatCard,
  TaskList,
  dec,
  fechaLarga,
  guardarPeriodo,
  hoyIso,
  inicioIso,
  leerPeriodo,
  saludoDelDia,
} from '@tusalud/ui/tablero';

@Component({
  selector: 'app-inicio-calidad',
  imports: [HeroPortada, SectionCard, StatCard, BarList, TaskList, QuickActions, SelectorPeriodo],
  template: `
    <app-hero-portada app="calidad" [title]="saludo()" [subtitle]="fecha()">
      <app-selector-periodo [valor]="dias()" (cambio)="cambiarPeriodo($event)" />
    </app-hero-portada>

    @if (reporte.error(); as error) {
      <p class="error" role="alert">{{ error.message }}</p>
    }

    @if (reporte.value(); as r) {
      <div class="fila-stats stats">
        <app-stat-card
          etiqueta="Satisfacción del paciente"
          [valor]="dec(r.valoraciones.promedio) + ' / 5'"
          icono="pi-star"
          tono="info"
          [detalle]="r.valoraciones.total + ' valoraciones'"
          enlace="/reportes/calidad"
        />
        <app-stat-card
          etiqueta="Entrega de resultados"
          [valor]="dec(r.laboratorio.entregaPromedioHoras) + ' h'"
          icono="pi-filter"
          [detalle]="'Percentil 90: ' + dec(r.laboratorio.entregaP90Horas) + ' h'"
        />
        <app-stat-card
          etiqueta="Comprobantes anulados"
          [valor]="r.anulaciones.comprobantes"
          icono="pi-ban"
          [tono]="r.anulaciones.comprobantes > 0 ? 'aviso' : 'exito'"
        />
        <app-stat-card
          etiqueta="Dispensaciones anuladas"
          [valor]="r.anulaciones.dispensaciones"
          icono="pi-undo"
          [tono]="r.anulaciones.dispensaciones > 0 ? 'aviso' : 'exito'"
        />
      </div>
    }

    <div class="tablero">
      <div class="c6">
        <app-section-card
          titulo="Satisfacción por especialidad"
          icono="pi-star"
          [cargando]="reporte.loading() && !reporte.hasValue()"
          [vacio]="satisfaccion().length === 0 && reporte.hasValue()"
          textoVacio="Aún no hay valoraciones en el periodo."
        >
          <app-bar-list
            [items]="satisfaccion()"
            etiqueta="Promedio de valoración por especialidad"
            [maximo]="5"
          />
        </app-section-card>
      </div>

      <div class="c6">
        <app-section-card
          titulo="Cancelación e inasistencia"
          icono="pi-calendar-minus"
          subtitulo="Especialidades con mayor tasa"
          [vacio]="tasas().length === 0 && reporte.hasValue()"
          textoVacio="Sin datos en el periodo."
        >
          <app-task-list [items]="tasas()" />
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
export class InicioCalidad implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly api = inject(ReportesApiService);

  protected readonly dec = dec;
  protected readonly reporte = new AsyncState<ReporteCalidadResponse>();
  protected readonly dias = signal(leerPeriodo());

  protected readonly saludo = computed(() => saludoDelDia(this.auth.user()?.nombreCompleto ?? ''));
  protected readonly fecha = computed(() => {
    const texto = fechaLarga();
    return texto.charAt(0).toUpperCase() + texto.slice(1);
  });

  protected readonly acciones: readonly AccionRapida[] = [
    {
      label: 'Reporte de calidad',
      icono: 'pi-verified',
      enlace: '/reportes/calidad',
      detalle: 'Satisfacción, tiempos y anulaciones',
    },
    {
      label: 'Morbilidad',
      icono: 'pi-heart',
      enlace: '/reportes/morbilidad',
      detalle: 'Diagnósticos más frecuentes',
    },
  ];

  protected readonly satisfaccion = computed<ItemBarra[]>(() =>
    (this.reporte.value()?.porEspecialidad ?? [])
      .filter((e) => e.total > 0)
      .sort((a, b) => b.promedio - a.promedio)
      .map((e): ItemBarra => ({
        etiqueta: e.nombre,
        valor: e.promedio,
        texto: `${e.promedio.toFixed(1)} · ${e.total} valoraciones`,
        tono: e.promedio < 3.5 ? 'aviso' : 'neutro',
      })),
  );

  protected readonly tasas = computed<ItemTarea[]>(() =>
    [...(this.reporte.value()?.citasPorEspecialidad ?? [])]
      .sort(
        (a, b) =>
          b.tasaCancelacionPct + b.tasaAusenciaPct - (a.tasaCancelacionPct + a.tasaAusenciaPct),
      )
      .slice(0, 6)
      .map((e): ItemTarea => ({
        id: e.especialidad,
        titulo: e.especialidad,
        detalle: `Cancelación ${e.tasaCancelacionPct}% · inasistencia ${e.tasaAusenciaPct}%`,
        cantidad: e.total,
        tono: e.tasaCancelacionPct + e.tasaAusenciaPct > 30 ? 'aviso' : 'neutro',
        icono: 'pi-calendar',
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
      this.api.calidad({
        desde: inicioIso(this.dias() - 1),
        hasta: hoyIso(),
        agrupacion: 'DIA',
      }),
    );
  }
}
