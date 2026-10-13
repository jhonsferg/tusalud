import { AbstractControl, FormGroup } from '@angular/forms';

import { ApiError } from '@tusalud/app/core/models';

export function applyServerErrors(form: FormGroup, error: ApiError): string[] {
  const unmatched: string[] = [];
  Object.entries(error.validationErrors).forEach(([field, message]) => {
    const control: AbstractControl | null = form.get(field);
    if (control) {
      control.setErrors({ ...(control.errors ?? {}), server: message });
      control.markAsTouched();
    } else {
      unmatched.push(message);
    }
  });
  return unmatched;
}
