import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

import { parseIsoDate, parseIsoDateTime } from '../utils/date';

const DNI = /^\d{8}$/;
const TELEFONO = /^\d{9}$/;
const HORA = /^([01]\d|2[0-3]):[0-5]\d$/;
const DECIMAL_DOS = /^\d{1,8}(\.\d{1,2})?$/;

export const PASSWORD_MIN = 8;
export const PASSWORD_MAX = 72;
export const DURACION_MIN = 10;
export const DURACION_MAX = 240;

function isEmpty(value: unknown): boolean {
  return (
    value === null || value === undefined || (typeof value === 'string' && value.trim() === '')
  );
}

function toMinutes(time: string): number {
  const [hours, minutes] = time.split(':').map(Number);
  return hours * 60 + minutes;
}

export class AppValidators {
  static notBlank(control: AbstractControl): ValidationErrors | null {
    return typeof control.value === 'string' &&
      control.value.length > 0 &&
      control.value.trim() === ''
      ? { notBlank: true }
      : null;
  }

  static dni(control: AbstractControl): ValidationErrors | null {
    return isEmpty(control.value) || DNI.test(String(control.value)) ? null : { dni: true };
  }

  static telefono(control: AbstractControl): ValidationErrors | null {
    return isEmpty(control.value) || TELEFONO.test(String(control.value))
      ? null
      : { telefono: true };
  }

  static password(control: AbstractControl): ValidationErrors | null {
    if (isEmpty(control.value)) {
      return null;
    }
    const length = String(control.value).length;
    return length < PASSWORD_MIN || length > PASSWORD_MAX
      ? { password: { min: PASSWORD_MIN, max: PASSWORD_MAX, actual: length } }
      : null;
  }

  static hora(control: AbstractControl): ValidationErrors | null {
    return isEmpty(control.value) || HORA.test(String(control.value)) ? null : { hora: true };
  }

  static precio(control: AbstractControl): ValidationErrors | null {
    if (isEmpty(control.value)) {
      return null;
    }
    const value = Number(control.value);
    return Number.isFinite(value) && value > 0 && DECIMAL_DOS.test(String(control.value))
      ? null
      : { precio: true };
  }

  static duracionCita(control: AbstractControl): ValidationErrors | null {
    if (isEmpty(control.value)) {
      return null;
    }
    const value = Number(control.value);
    return Number.isInteger(value) && value >= DURACION_MIN && value <= DURACION_MAX
      ? null
      : { duracionCita: { min: DURACION_MIN, max: DURACION_MAX } };
  }

  static pastDate(control: AbstractControl): ValidationErrors | null {
    if (isEmpty(control.value)) {
      return null;
    }
    const date = parseIsoDate(String(control.value));
    if (!date) {
      return { fecha: true };
    }
    const startOfToday = new Date();
    startOfToday.setHours(0, 0, 0, 0);
    return date < startOfToday ? null : { pastDate: true };
  }

  static futureDateTime(control: AbstractControl): ValidationErrors | null {
    if (isEmpty(control.value)) {
      return null;
    }
    const date = parseIsoDateTime(String(control.value));
    if (!date) {
      return { fechaHora: true };
    }
    return date.getTime() > Date.now() ? null : { futureDateTime: true };
  }

  static dateRange(startKey: string, endKey: string): ValidatorFn {
    return (group: AbstractControl): ValidationErrors | null => {
      const start = group.get(startKey)?.value;
      const end = group.get(endKey)?.value;
      if (isEmpty(start) || isEmpty(end)) {
        return null;
      }
      return String(start) <= String(end) ? null : { dateRange: true };
    };
  }

  static timeRange(startKey: string, endKey: string, durationKey?: string): ValidatorFn {
    return (group: AbstractControl): ValidationErrors | null => {
      const start = group.get(startKey)?.value;
      const end = group.get(endKey)?.value;
      if (!HORA.test(String(start ?? '')) || !HORA.test(String(end ?? ''))) {
        return null;
      }
      const span = toMinutes(String(end)) - toMinutes(String(start));
      if (span <= 0) {
        return { timeRange: true };
      }
      const duration = durationKey ? Number(group.get(durationKey)?.value) : NaN;
      return Number.isFinite(duration) && duration > span
        ? { bloqueCorto: { span, duration } }
        : null;
    };
  }
}
