import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { optionalNumberAttribute } from '../../../core/utils/route-input';

@Component({
  selector: 'app-cita-form',
  imports: [],
  templateUrl: './cita-form.html',
  styleUrl: './cita-form.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CitaForm {
  readonly medicoId = input<number | undefined, unknown>(undefined, {
    transform: optionalNumberAttribute,
  });
}
