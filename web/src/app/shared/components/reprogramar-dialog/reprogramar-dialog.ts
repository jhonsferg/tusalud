import { DatePipe } from '@angular/common';
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
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';
import { Dialog } from 'primeng/dialog';
import { Message } from 'primeng/message';
import { Skeleton } from 'primeng/skeleton';

import { ApiError, CitaResponse } from '@tusalud/app/core/models';
import { NotificationService } from '@tusalud/app/core/services/notification.service';
import { CitasStore } from '@tusalud/app/core/state/citas.store';
import { MedicosStore } from '@tusalud/app/core/state/medicos.store';
import { combineDateAndTime, toIsoDate } from '@tusalud/app/core/utils/date';

@Component({
  selector: 'app-reprogramar-dialog',
  imports: [DatePipe, FormsModule, Button, DatePicker, Dialog, Message, Skeleton],
  templateUrl: './reprogramar-dialog.html',
  styleUrl: './reprogramar-dialog.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReprogramarDialog {
  readonly cita = input.required<CitaResponse | null>();
  readonly visible = model(false);
  readonly reprogramada = output<CitaResponse>();

  protected readonly medicos = inject(MedicosStore);
  private readonly citas = inject(CitasStore);
  private readonly notifications = inject(NotificationService);

  protected readonly minimo = new Date();
  protected readonly fecha = signal<Date | null>(null);
  protected readonly hora = signal<string | null>(null);
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly horas = computed(() =>
    this.medicos.horasDisponibles().map((hora) => hora.slice(0, 5)),
  );

  constructor() {
    effect(() => {
      if (this.visible()) {
        untracked(() => {
          this.fecha.set(null);
          this.hora.set(null);
          this.error.set(null);
        });
      }
    });
    effect(() => {
      const cita = this.cita();
      const fecha = this.fecha();
      if (this.visible() && cita && fecha) {
        untracked(() => {
          this.hora.set(null);
          this.medicos.loadDisponibilidad(cita.medicoId, toIsoDate(fecha));
        });
      }
    });
  }

  protected confirmar(): void {
    const cita = this.cita();
    const fecha = this.fecha();
    const hora = this.hora();
    if (!cita || !fecha || !hora) {
      return;
    }
    this.error.set(null);
    this.guardando.set(true);
    this.citas.reprogramar(cita.id, combineDateAndTime(toIsoDate(fecha), hora)).subscribe({
      next: (actualizada) => {
        this.guardando.set(false);
        this.visible.set(false);
        this.notifications.success('Tu cita fue reprogramada. Quedó pendiente de confirmación.');
        this.reprogramada.emit(actualizada);
      },
      error: (error: ApiError) => {
        this.guardando.set(false);
        this.error.set(error.message);
      },
    });
  }
}
