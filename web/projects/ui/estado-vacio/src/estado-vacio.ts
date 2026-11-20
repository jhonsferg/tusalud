import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-estado-vacio',
  templateUrl: './estado-vacio.html',
  styleUrl: './estado-vacio.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EstadoVacio {
  readonly imagen = input.required<string>();
  readonly title = input.required<string>();
  readonly message = input<string>();
}
