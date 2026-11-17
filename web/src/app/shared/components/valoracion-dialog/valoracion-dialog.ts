import {
  ChangeDetectionStrategy,
  Component,
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
import { Dialog } from 'primeng/dialog';
import { Message } from 'primeng/message';
import { Rating } from 'primeng/rating';
import { Textarea } from 'primeng/textarea';

import { ApiError, CitaResponse, ValoracionResponse } from '@tusalud/app/core/models';
import { NotificationService } from '@tusalud/app/core/services/notification.service';
import { ClinicaStore } from '@tusalud/app/core/state/clinica.store';

@Component({
  selector: 'app-valoracion-dialog',
  imports: [FormsModule, Button, Dialog, Message, Rating, Textarea],
  templateUrl: './valoracion-dialog.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ValoracionDialog {
  readonly cita = input.required<CitaResponse | null>();
  readonly visible = model(false);
  readonly valorada = output<ValoracionResponse>();

  private readonly store = inject(ClinicaStore);
  private readonly notifications = inject(NotificationService);

  protected readonly puntuacion = signal(0);
  protected readonly comentario = signal('');
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);

  constructor() {
    effect(() => {
      if (this.visible()) {
        untracked(() => {
          this.puntuacion.set(0);
          this.comentario.set('');
          this.error.set(null);
        });
      }
    });
  }

  protected enviar(): void {
    const cita = this.cita();
    if (!cita || this.puntuacion() < 1) {
      return;
    }
    this.guardando.set(true);
    this.store
      .valorar(cita.id, {
        puntuacion: this.puntuacion(),
        comentario: this.comentario().trim() || null,
      })
      .subscribe({
        next: (valoracion) => {
          this.guardando.set(false);
          this.visible.set(false);
          this.notifications.success('Gracias por valorar tu atención.');
          this.valorada.emit(valoracion);
        },
        error: (error: ApiError) => {
          this.guardando.set(false);
          this.error.set(error.message);
        },
      });
  }
}
