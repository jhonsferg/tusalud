import { CurrencyPipe, DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
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
import { SelectButton } from 'primeng/selectbutton';
import QRCode from 'qrcode';

import {
  ApiError,
  Billetera,
  CitaResponse,
  CuponValidacionResponse,
  PagoQrResponse,
  PagoResponse,
} from '@tusalud/app/core/models';
import { PagosApiService } from '@tusalud/app/core/services/api/pagos-api.service';
import { NotificationService } from '@tusalud/app/core/services/notification.service';
import { PagosStore } from '@tusalud/app/core/state/pagos.store';

type Canal = 'TARJETA' | Billetera;

const CANALES: { label: string; value: Canal; icon: string }[] = [
  { label: 'Tarjeta', value: 'TARJETA', icon: 'pi pi-credit-card' },
  { label: 'Yape', value: 'YAPE', icon: 'pi pi-mobile' },
  { label: 'Plin', value: 'PLIN', icon: 'pi pi-mobile' },
];

const SONDEO_MS = 3000;

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
    SelectButton,
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
  private temporizador: ReturnType<typeof setInterval> | null = null;
  private ultimoSondeo = 0;

  protected readonly canales = CANALES;
  protected readonly canal = signal<Canal>('TARJETA');
  protected readonly qr = signal<PagoQrResponse | null>(null);
  protected readonly qrImagen = signal<string | null>(null);
  protected readonly restante = signal(0);
  protected readonly generando = signal(false);
  protected readonly simulando = signal(false);
  protected readonly simulacionDisponible = signal(true);
  protected readonly billetera = computed(() => {
    const canal = this.canal();
    return canal === 'TARJETA' ? null : canal;
  });
  protected readonly vencido = computed(() => this.qr()?.estado === 'EXPIRADO');
  protected readonly tiempo = computed(() => {
    const total = Math.max(0, this.restante());
    return `${Math.floor(total / 60)}:${String(total % 60).padStart(2, '0')}`;
  });

  protected readonly metodoId = signal<string | null>(null);
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
    inject(DestroyRef).onDestroy(() => this.detener());
    effect(() => {
      if (!this.visible()) {
        untracked(() => this.descartarQr());
      }
    });
    effect(() => {
      if (this.visible()) {
        untracked(() => {
          this.store.cargarMetodos();
          this.canal.set('TARJETA');
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

  protected cambiarCanal(canal: Canal | null): void {
    if (!canal || canal === this.canal()) {
      return;
    }
    this.cancelarQr();
    this.error.set(null);
    this.canal.set(canal);
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

  protected generarQr(): void {
    const cita = this.cita();
    const billetera = this.billetera();
    if (!cita || !billetera) {
      return;
    }
    this.error.set(null);
    this.generando.set(true);
    this.api
      .crearPagoQr({
        citaId: cita.id,
        billetera,
        codigoCupon: this.cupon()?.valido ? this.cupon()!.codigo : null,
      })
      .subscribe({
        next: async (qr) => {
          this.qrImagen.set(await QRCode.toDataURL(qr.qrPayload, { width: 240, margin: 1 }));
          this.qr.set(qr);
          this.generando.set(false);
          this.iniciarSondeo(qr);
        },
        error: (error: ApiError) => {
          this.generando.set(false);
          this.error.set(error.message);
        },
      });
  }

  protected simularPagoEnApp(): void {
    const qr = this.qr();
    if (!qr) {
      return;
    }
    this.simulando.set(true);
    this.api.simularConfirmacionQr(qr.codigo).subscribe({
      next: (respuesta) => {
        this.simulando.set(false);
        this.actualizarQr(respuesta);
      },
      error: (error: ApiError) => {
        this.simulando.set(false);
        if (error.status === 403 || error.status === 404) {
          this.simulacionDisponible.set(false);
        }
      },
    });
  }

  protected cancelarQr(): void {
    const qr = this.qr();
    this.descartarQr();
    if (qr && qr.estado === 'PENDIENTE') {
      this.api.cancelarPagoQr(qr.codigo).subscribe({ error: () => undefined });
    }
  }

  private iniciarSondeo(qr: PagoQrResponse): void {
    this.detener();
    const expira = new Date(qr.expiraEn).getTime();
    this.restante.set(Math.round((expira - Date.now()) / 1000));
    this.ultimoSondeo = Date.now();
    this.temporizador = setInterval(() => {
      const segundos = Math.round((expira - Date.now()) / 1000);
      this.restante.set(segundos);
      const actual = this.qr();
      if (!actual || actual.estado !== 'PENDIENTE') {
        return;
      }
      if (segundos <= 0) {
        this.detener();
        this.qr.set({ ...actual, estado: 'EXPIRADO' });
        return;
      }
      if (Date.now() - this.ultimoSondeo >= SONDEO_MS) {
        this.ultimoSondeo = Date.now();
        this.api.consultarPagoQr(actual.codigo).subscribe({
          next: (respuesta) => this.actualizarQr(respuesta),
          error: () => undefined,
        });
      }
    }, 1000);
  }

  private actualizarQr(respuesta: PagoQrResponse): void {
    this.qr.set(respuesta);
    if (respuesta.estado === 'PENDIENTE') {
      return;
    }
    this.detener();
    const cita = this.cita();
    if (respuesta.estado === 'CONFIRMADO' && respuesta.pagoId && cita) {
      this.store.registrarPagoConfirmado(respuesta.pagoId, cita.id).subscribe({
        next: (pago) => {
          this.visible.set(false);
          this.notifications.success(`Pago registrado. Comprobante ${pago.comprobante}.`);
          this.pagado.emit(pago);
        },
        error: () => undefined,
      });
    }
  }

  private descartarQr(): void {
    this.detener();
    this.qr.set(null);
    this.qrImagen.set(null);
    this.restante.set(0);
  }

  private detener(): void {
    if (this.temporizador !== null) {
      clearInterval(this.temporizador);
      this.temporizador = null;
    }
  }
}
