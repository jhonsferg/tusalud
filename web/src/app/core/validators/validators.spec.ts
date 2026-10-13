import { FormControl, FormGroup, Validators } from '@angular/forms';

import { toIsoDate, toIsoDateTime } from '../utils/date';
import { AppValidators } from './app.validators';
import { applyServerErrors } from './server-errors';
import { firstErrorMessage, validationMessage } from './validation-messages';

function check(validator: (control: FormControl) => unknown, value: unknown): unknown {
  return validator(new FormControl(value));
}

describe('AppValidators', () => {
  it('valida DNI, teléfono y espacios en blanco', () => {
    expect(check(AppValidators.dni, '12345678')).toBeNull();
    expect(check(AppValidators.dni, '1234567')).toEqual({ dni: true });
    expect(check(AppValidators.dni, '')).toBeNull();
    expect(check(AppValidators.telefono, '987654321')).toBeNull();
    expect(check(AppValidators.telefono, '98765432a')).toEqual({ telefono: true });
    expect(check(AppValidators.notBlank, '   ')).toEqual({ notBlank: true });
    expect(check(AppValidators.notBlank, ' a ')).toBeNull();
  });

  it('valida contraseña, hora, precio y duración', () => {
    expect(check(AppValidators.password, 'corta')).toEqual({
      password: { min: 8, max: 72, actual: 5 },
    });
    expect(check(AppValidators.password, 'Paciente123!')).toBeNull();
    expect(check(AppValidators.hora, '08:30')).toBeNull();
    expect(check(AppValidators.hora, '24:00')).toEqual({ hora: true });
    expect(check(AppValidators.precio, '150.50')).toBeNull();
    expect(check(AppValidators.precio, '150.555')).toEqual({ precio: true });
    expect(check(AppValidators.precio, 0)).toEqual({ precio: true });
    expect(check(AppValidators.duracionCita, 30)).toBeNull();
    expect(check(AppValidators.duracionCita, 5)).toEqual({ duracionCita: { min: 10, max: 240 } });
  });

  it('valida fechas pasadas y fechas futuras', () => {
    const ayer = new Date();
    ayer.setDate(ayer.getDate() - 1);
    const manana = new Date();
    manana.setDate(manana.getDate() + 1);

    expect(check(AppValidators.pastDate, toIsoDate(ayer))).toBeNull();
    expect(check(AppValidators.pastDate, toIsoDate(new Date()))).toEqual({ pastDate: true });
    expect(check(AppValidators.pastDate, '2026-02-30')).toEqual({ fecha: true });
    expect(check(AppValidators.futureDateTime, toIsoDateTime(manana))).toBeNull();
    expect(check(AppValidators.futureDateTime, toIsoDateTime(ayer))).toEqual({
      futureDateTime: true,
    });
    expect(check(AppValidators.futureDateTime, 'mañana')).toEqual({ fechaHora: true });
  });

  it('valida rangos de fechas y bloques de horario', () => {
    const fechas = new FormGroup(
      { inicio: new FormControl('2026-10-10'), fin: new FormControl('2026-10-01') },
      { validators: AppValidators.dateRange('inicio', 'fin') },
    );
    expect(fechas.errors).toEqual({ dateRange: true });

    const bloque = new FormGroup(
      {
        horaInicio: new FormControl('08:00'),
        horaFin: new FormControl('08:20'),
        duracion: new FormControl(30),
      },
      { validators: AppValidators.timeRange('horaInicio', 'horaFin', 'duracion') },
    );
    expect(bloque.errors).toEqual({ bloqueCorto: { span: 20, duration: 30 } });
    bloque.patchValue({ horaFin: '07:00' });
    expect(bloque.errors).toEqual({ timeRange: true });
    bloque.patchValue({ horaFin: '13:00' });
    expect(bloque.errors).toBeNull();
  });
});

describe('mensajes de validación', () => {
  it('traduce las claves de error a mensajes en español', () => {
    expect(firstErrorMessage({ required: true })).toBe('Este campo es obligatorio.');
    expect(firstErrorMessage({ minlength: { requiredLength: 3, actualLength: 1 } })).toBe(
      'Debe tener al menos 3 caracteres.',
    );
    expect(firstErrorMessage({ server: 'El DNI ya existe' })).toBe('El DNI ya existe');
    expect(firstErrorMessage(null)).toBeNull();
    expect(validationMessage('desconocido', true)).toBe('El valor ingresado no es válido.');
  });
});

describe('applyServerErrors', () => {
  it('asigna los errores del servidor a los controles y devuelve los no asignados', () => {
    const form = new FormGroup({
      dni: new FormControl('12345678', Validators.required),
      email: new FormControl('a@b.com'),
    });
    const unmatched = applyServerErrors(form, {
      status: 400,
      message: 'Error de validación',
      path: null,
      timestamp: null,
      validationErrors: { dni: 'El DNI debe tener 8 dígitos', otro: 'Campo desconocido' },
    });

    expect(form.get('dni')?.errors).toEqual({ server: 'El DNI debe tener 8 dígitos' });
    expect(form.get('dni')?.touched).toBe(true);
    expect(form.get('email')?.errors).toBeNull();
    expect(unmatched).toEqual(['Campo desconocido']);
  });
});
