import { ValidationErrors } from '@angular/forms';

type MessageFactory = (detail: unknown) => string;

interface LengthDetail {
  requiredLength: number;
}

interface RangeDetail {
  min: number;
  max: number;
}

const MESSAGES: Readonly<Record<string, MessageFactory>> = {
  required: () => 'Este campo es obligatorio.',
  notBlank: () => 'Este campo no puede contener solo espacios.',
  email: () => 'Ingresa un email válido.',
  minlength: (detail) =>
    `Debe tener al menos ${(detail as LengthDetail).requiredLength} caracteres.`,
  maxlength: (detail) => `No debe superar ${(detail as LengthDetail).requiredLength} caracteres.`,
  min: (detail) => `El valor mínimo es ${(detail as { min: number }).min}.`,
  max: (detail) => `El valor máximo es ${(detail as { max: number }).max}.`,
  dni: () => 'El DNI debe tener 8 dígitos.',
  telefono: () => 'El teléfono debe tener 9 dígitos.',
  password: (detail) =>
    `La contraseña debe tener entre ${(detail as RangeDetail).min} y ${(detail as RangeDetail).max} caracteres.`,
  hora: () => 'Ingresa una hora válida en formato HH:mm.',
  precio: () => 'Ingresa un monto mayor a 0 con hasta 2 decimales.',
  duracionCita: (detail) =>
    `La duración debe estar entre ${(detail as RangeDetail).min} y ${(detail as RangeDetail).max} minutos.`,
  fecha: () => 'Ingresa una fecha válida.',
  fechaHora: () => 'Ingresa una fecha y hora válidas.',
  pastDate: () => 'La fecha debe ser anterior a hoy.',
  futureDateTime: () => 'La fecha y hora deben ser futuras.',
  dateRange: () => 'La fecha de inicio no puede ser posterior a la fecha de fin.',
  timeRange: () => 'La hora de fin debe ser posterior a la hora de inicio.',
  bloqueCorto: () => 'El bloque de horario debe admitir al menos una cita completa.',
  server: (detail) => String(detail),
};

export function validationMessage(key: string, detail: unknown): string {
  return MESSAGES[key]?.(detail) ?? 'El valor ingresado no es válido.';
}

export function firstErrorMessage(errors: ValidationErrors | null | undefined): string | null {
  if (!errors) {
    return null;
  }
  const [key] = Object.keys(errors);
  return key ? validationMessage(key, errors[key]) : null;
}
