import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  model,
  output,
  signal,
  untracked,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ConfirmationService } from 'primeng/api';
import { Button } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { Textarea } from 'primeng/textarea';
import { ToggleSwitch } from 'primeng/toggleswitch';
import { startWith } from 'rxjs';

import {
  COLORES_EVENTO,
  ColorEventoApi,
  EventoCalendarioRequest,
  EventoCalendarioResponse,
  TIPOS_EVENTO,
  TipoEvento,
} from '@tusalud/app/core/models';
import { NotificationService } from '@tusalud/app/core/services/notification.service';
import { CalendarioStore } from '@tusalud/app/core/state/calendario.store';
import { parseIsoDateTime, toIsoDateTime } from '@tusalud/app/core/utils/date';
import { CrearEvento } from '@tusalud/app/shared/components/calendario/calendario.utils';
import { ETIQUETA_TIPO } from '@tusalud/app/shared/utils/evento-calendario';

const RECORDATORIOS = [
  { label: 'Sin recordatorio', value: null },
  { label: '5 minutos antes', value: 5 },
  { label: '10 minutos antes', value: 10 },
  { label: '30 minutos antes', value: 30 },
  { label: '1 hora antes', value: 60 },
  { label: '1 día antes', value: 1440 },
];

const COLOR_HEX: Readonly<Record<ColorEventoApi, string>> = {
  AZUL: '#3b82f6',
  VERDE: '#22c55e',
  ROJO: '#ef4444',
  NARANJA: '#f97316',
  MORADO: '#a855f7',
  GRIS: '#64748b',
};

@Component({
  selector: 'app-evento-dialog',
  imports: [
    ReactiveFormsModule,
    Button,
    DatePicker,
    Dialog,
    InputText,
    Select,
    Textarea,
    ToggleSwitch,
  ],
  templateUrl: './evento-dialog.html',
  styleUrl: './evento-dialog.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EventoDialog {
  readonly visible = model(false);
  readonly evento = input<EventoCalendarioResponse | null>(null);
  readonly borrador = input<CrearEvento | null>(null);
  readonly tiposPermitidos = input<readonly TipoEvento[]>(['PERSONAL', 'RECORDATORIO']);

  readonly guardado = output<EventoCalendarioResponse>();
  readonly eliminado = output<number>();

  private readonly store = inject(CalendarioStore);
  private readonly notifications = inject(NotificationService);
  private readonly confirmation = inject(ConfirmationService);
  private readonly fb = inject(FormBuilder).nonNullable;

  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly colores = COLORES_EVENTO;
  protected readonly colorHex = COLOR_HEX;
  protected readonly recordatorios = RECORDATORIOS;

  protected readonly form = this.fb.group({
    titulo: ['', [Validators.required, Validators.maxLength(120)]],
    tipo: ['REUNION' as TipoEvento, [Validators.required]],
    todoElDia: [false],
    inicio: [new Date() as Date | null, [Validators.required]],
    fin: [new Date() as Date | null, [Validators.required]],
    ubicacion: ['', [Validators.maxLength(120)]],
    descripcion: ['', [Validators.maxLength(500)]],
    color: [null as ColorEventoApi | null],
    recordatorioMinutos: [null as number | null],
  });

  private readonly valores = toSignal(this.form.valueChanges.pipe(startWith(this.form.value)), {
    initialValue: this.form.value,
  });

  protected readonly editando = computed(() => this.evento() !== null);
  protected readonly todoElDia = computed(() => this.valores().todoElDia ?? false);
  protected readonly esTarea = computed(() => this.valores().tipo === 'TAREA');
  protected readonly tipos = computed(() =>
    TIPOS_EVENTO.filter((tipo) => this.tiposPermitidos().includes(tipo)).map((tipo) => ({
      label: ETIQUETA_TIPO[tipo],
      value: tipo,
    })),
  );
  protected readonly rangoInvalido = computed(() => {
    const { inicio, fin } = this.valores();
    if (!inicio || !fin) {
      return false;
    }
    return this.todoElDia() ? fin.getTime() < inicio.getTime() : fin.getTime() <= inicio.getTime();
  });

  constructor() {
    effect(() => {
      if (!this.visible()) {
        return;
      }
      const evento = this.evento();
      const borrador = this.borrador();
      untracked(() => {
        this.error.set(null);
        if (evento) {
          this.form.reset({
            titulo: evento.titulo,
            tipo: evento.tipo,
            todoElDia: evento.todoElDia,
            inicio: parseIsoDateTime(evento.inicio),
            fin: parseIsoDateTime(evento.fin),
            ubicacion: evento.ubicacion ?? '',
            descripcion: evento.descripcion ?? '',
            color: evento.color ?? null,
            recordatorioMinutos: evento.recordatorioMinutos ?? null,
          });
        } else {
          const inicio = borrador?.inicio ?? new Date();
          this.form.reset({
            titulo: '',
            tipo: this.tiposPermitidos()[0] ?? 'PERSONAL',
            todoElDia: borrador?.todoElDia ?? false,
            inicio,
            fin: borrador?.fin ?? new Date(inicio.getTime() + 60 * 60_000),
            ubicacion: '',
            descripcion: '',
            color: null,
            recordatorioMinutos: null,
          });
        }
      });
    });
  }

  protected elegirColor(color: ColorEventoApi | null): void {
    this.form.controls.color.setValue(color);
  }

  protected guardar(): void {
    this.error.set(null);
    if (this.form.invalid || this.rangoInvalido()) {
      this.form.markAllAsTouched();
      return;
    }
    const valor = this.form.getRawValue();
    const todoElDia = valor.todoElDia;
    const inicio = new Date(valor.inicio!);
    const fin = new Date(valor.fin!);
    if (todoElDia) {
      inicio.setHours(0, 0, 0, 0);
      fin.setHours(0, 0, 0, 0);
    }
    const request: EventoCalendarioRequest = {
      titulo: valor.titulo.trim(),
      descripcion: valor.descripcion.trim() || null,
      ubicacion: valor.ubicacion.trim() || null,
      tipo: valor.tipo,
      inicio: toIsoDateTime(inicio),
      fin: toIsoDateTime(fin),
      todoElDia,
      color: valor.color,
      recordatorioMinutos: valor.recordatorioMinutos,
    };
    const actual = this.evento();
    this.guardando.set(true);
    const operacion = actual
      ? this.store.actualizar(actual.id, request)
      : this.store.crear(request);
    operacion.subscribe({
      next: (guardado) => {
        this.guardando.set(false);
        this.visible.set(false);
        this.notifications.success(actual ? 'El evento fue actualizado.' : 'El evento fue creado.');
        this.guardado.emit(guardado);
      },
      error: (error: { message: string }) => {
        this.guardando.set(false);
        this.error.set(error.message);
      },
    });
  }

  protected alternarCompletado(): void {
    const actual = this.evento();
    if (!actual) {
      return;
    }
    this.store.completar(actual.id, !actual.completado).subscribe({
      next: (guardado) => {
        this.visible.set(false);
        this.guardado.emit(guardado);
      },
      error: () => undefined,
    });
  }

  protected eliminar(): void {
    const actual = this.evento();
    if (!actual) {
      return;
    }
    this.confirmation.confirm({
      header: 'Eliminar evento',
      message: `Se eliminará "${actual.titulo}".`,
      icon: 'pi pi-trash',
      acceptLabel: 'Eliminar',
      rejectLabel: 'Cancelar',
      acceptButtonProps: { severity: 'danger' },
      rejectButtonProps: { severity: 'secondary', outlined: true },
      accept: () =>
        this.store.eliminar(actual.id).subscribe({
          next: () => {
            this.visible.set(false);
            this.notifications.success('El evento fue eliminado.');
            this.eliminado.emit(actual.id);
          },
          error: () => undefined,
        }),
    });
  }
}
