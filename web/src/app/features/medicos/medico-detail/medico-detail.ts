import { ChangeDetectionStrategy, Component, input, numberAttribute } from '@angular/core';

@Component({
  selector: 'app-medico-detail',
  imports: [],
  templateUrl: './medico-detail.html',
  styleUrl: './medico-detail.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MedicoDetail {
  readonly id = input.required({ transform: numberAttribute });
}
