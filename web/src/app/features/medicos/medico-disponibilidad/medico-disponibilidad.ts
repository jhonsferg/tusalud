import { ChangeDetectionStrategy, Component, input, numberAttribute } from '@angular/core';

@Component({
  selector: 'app-medico-disponibilidad',
  imports: [],
  templateUrl: './medico-disponibilidad.html',
  styleUrl: './medico-disponibilidad.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MedicoDisponibilidad {
  readonly id = input.required({ transform: numberAttribute });
}
