import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { optionalNumberAttribute } from '../../../core/utils/route-input';

@Component({
  selector: 'app-especialidad-form',
  imports: [],
  templateUrl: './especialidad-form.html',
  styleUrl: './especialidad-form.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EspecialidadForm {
  readonly id = input<number | undefined, unknown>(undefined, {
    transform: optionalNumberAttribute,
  });
}
