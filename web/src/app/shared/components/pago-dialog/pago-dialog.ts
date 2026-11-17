import { CurrencyPipe, DatePipe } from '@angular/common';
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
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';

import {
  ApiError,
  CitaResponse,
  CuponValidacionResponse,
  PagoResponse,
} from '@tusalud/app/core/models';
import { PagosApiService } from '@tusalud/app/core/services/api/pagos-api.service';
import { NotificationService } from '@tusalud/app/core/services/notification.service';
import { PagosStore } from '@tusalud/app/core/state/pagos.store';

@Component({
  selector: 'app-pago-dialog',
  imports: [
    CurrencyPipe,
    DatePipe,
    FormsModule,
    RouterLink,
    Button,
    Dialog,
    InputText,
    Message,
    Select,
  ],
  templateUrl: './pago-dialog.html',
  styleUrl: './pago-dialog.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PagoDialog {
  readonly cita = input.required<CitaResponse | null>();
  readonly visible = model(false);
  readonly pagado = output<PagoResponse>();

  protected readonly store = inject(PagosStore);
  private readonly api = inject(PagosApiService);
  private readonly notifications = inject(NotificationService);

  protected readonly metodoId = signal<number | null>(null);
  protected readonly codigo = signal('');
  protected readonly cupon = signal<CuponValidacionResponse | null>(null);
  protected readonly validando = signal(false);
  protected readonly pagando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly opciones = computed(() =>
    this.store.metodosVigentes().map((metodo) => ({
      value: metodo.id,
      label: `${metodo.marca} terminada en ${metodo.ultimos4}${metodo.predeterminado ? ' (predeterminada)' : ''}`,
    })),
  );
  protected readonly descuento = computed(() =>
    this.cupon()?.valido ? this.cupon()!.descuento : 0,
  );
  protected readonly total = computed(() =>
    Math.max(0, (this.cita()?.monto ?? 0) - this.descuento()),
  );

  constructor() {
    effect(() => {
      if (this.visible()) {
        untracked(() => {
          this.store.cargarMetodos();
          this.codigo.set('');
          this.cupon.set(null);
          this.error.set(null);
        });
      }
    });
    effect(() => {
      const predeterminado = this.store.predeterminado();
      if (this.visible() && predeterminado && this.metodoId() === null) {
        untracked(() => this.metodoId.set(predeterminado.id));
      }
    });
  }

  protected aplicarCupon(): void {
    const cita = this.cita();
    const codigo = this.codigo().trim().toUpperCase();
    if (!cita || !codigo) {
      return;
    }
    this.validando.set(true);
    this.api.validarCupon(codigo, cita.monto).subscribe({
      next: (respuesta) => {
        this.validando.set(false);
        this.cupon.set(respuesta);
      },
      error: () => this.validando.set(false),
    });
  }

  protected quitarCupon(): void {
    this.cupon.set(null);
    this.codigo.set('');
  }

  protected pagar(): void {
    const cita = this.cita();
    const metodoId = this.metodoId();
    if (!cita || metodoId === null) {
      return;
    }
    this.error.set(null);
    this.pagando.set(true);
    this.store
      .pagarCita(cita.id, {
        metodoPagoId: metodoId,
        codigoCupon: this.cupon()?.valido ? this.cupon()!.codigo : null,
      })
      .subscribe({
        next: (pago) => {
          this.pagando.set(false);
          this.visible.set(false);
          this.notifications.success(`Pago registrado. Comprobante ${pago.comprobante}.`);
          this.pagado.emit(pago);
        },
        error: (error: ApiError) => {
          this.pagando.set(false);
          this.error.set(error.message);
        },
      });
  }
}
