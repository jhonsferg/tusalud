import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { Indicador } from '@tusalud/core';

@Component({
  selector: 'app-kpi-variacion',
  templateUrl: './kpi-variacion.html',
  styleUrl: './kpi-variacion.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class KpiVariacion {
  readonly etiqueta = input.required<string>();
  readonly valor = input.required<string>();
  readonly icono = input('pi-chart-bar');
  readonly indicador = input<Indicador | null>(null);
  readonly menorEsMejor = input(false);

  protected readonly variacion = computed(() => this.indicador()?.variacionPct ?? null);
  protected readonly tono = computed(() => {
    const variacion = this.variacion();
    if (variacion === null || variacion === 0) {
      return 'neutro';
    }
    const sube = variacion > 0;
    return sube !== this.menorEsMejor() ? 'bien' : 'mal';
  });
  protected readonly flecha = computed(() => {
    const variacion = this.variacion();
    if (variacion === null || variacion === 0) {
      return 'pi-minus';
    }
    return variacion > 0 ? 'pi-arrow-up' : 'pi-arrow-down';
  });
  protected readonly texto = computed(() => {
    const variacion = this.variacion();
    if (variacion === null) {
      return 'Sin comparación';
    }
    return Math.abs(variacion).toFixed(1) + ' % vs periodo anterior';
  });
}
