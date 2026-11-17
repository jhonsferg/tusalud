import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { AbstractControl } from '@angular/forms';
import { map, merge, of, startWith, switchMap } from 'rxjs';

import { firstErrorMessage } from '@tusalud/core';

@Component({
  selector: 'app-form-field',
  templateUrl: './form-field.html',
  styleUrl: './form-field.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FormField {
  readonly label = input.required<string>();
  readonly control = input.required<AbstractControl>();
  readonly inputId = input.required<string>();
  readonly hint = input<string>();
  readonly required = input(false);

  private readonly tick = toSignal(
    toObservable(this.control).pipe(
      switchMap((control) =>
        merge(control.events, of(null)).pipe(
          startWith(null),
          map(() => ({})),
        ),
      ),
    ),
  );

  protected readonly error = computed(() => {
    this.tick();
    const control = this.control();
    return control.invalid && (control.touched || control.dirty)
      ? firstErrorMessage(control.errors)
      : null;
  });
}
