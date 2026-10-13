import { ChangeDetectionStrategy, Component, input, numberAttribute } from '@angular/core';

@Component({
  selector: 'app-cita-detail',
  imports: [],
  templateUrl: './cita-detail.html',
  styleUrl: './cita-detail.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CitaDetail {
  readonly id = input.required({ transform: numberAttribute });
}
