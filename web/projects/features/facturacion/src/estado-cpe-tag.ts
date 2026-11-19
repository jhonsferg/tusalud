import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { Tag } from 'primeng/tag';

import { ETIQUETA_ESTADO_CPE, EstadoCpe, SEVERIDAD_ESTADO_CPE } from '@tusalud/core';

@Component({
  selector: 'app-estado-cpe-tag',
  imports: [Tag],
  template: `<p-tag [value]="etiqueta()" [severity]="severidad()" />`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EstadoCpeTag {
  readonly estado = input.required<EstadoCpe>();

  protected readonly etiqueta = computed(() => ETIQUETA_ESTADO_CPE[this.estado()]);
  protected readonly severidad = computed(() => SEVERIDAD_ESTADO_CPE[this.estado()]);
}
