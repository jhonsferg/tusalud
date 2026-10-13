import { ChangeDetectionStrategy, Component, input, numberAttribute } from '@angular/core';

@Component({
  selector: 'app-medico-horarios',
  imports: [],
  templateUrl: './medico-horarios.html',
  styleUrl: './medico-horarios.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MedicoHorarios {
  readonly id = input.required({ transform: numberAttribute });
}
