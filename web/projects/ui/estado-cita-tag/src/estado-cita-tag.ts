import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { Tag } from 'primeng/tag';

import { EstadoCita } from '@tusalud/core';
import { ETIQUETA_ESTADO, ICONO_ESTADO, SEVERIDAD_ESTADO } from '@tusalud/ui/utils';

@Component({
  selector: 'app-estado-cita-tag',
  imports: [Tag],
  template: `<p-tag
    [value]="etiqueta[estado()]"
    [severity]="severidad[estado()]"
    [icon]="'pi ' + iconos[estado()]"
  />`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EstadoCitaTag {
  readonly estado = input.required<EstadoCita>();
  protected readonly etiqueta = ETIQUETA_ESTADO;
  protected readonly severidad = SEVERIDAD_ESTADO;
  protected readonly iconos = ICONO_ESTADO;
}
