import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ConfirmationService } from 'primeng/api';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { Skeleton } from 'primeng/skeleton';
import { Tag } from 'primeng/tag';
import { ToggleSwitch } from 'primeng/toggleswitch';

import { ApiError, MetodoPagoResponse } from '@tusalud/app/core/models';
import { NotificationService } from '@tusalud/app/core/services/notification.service';
import { PagosStore } from '@tusalud/app/core/state/pagos.store';
import { AppValidators } from '@tusalud/app/core/validators/app.validators';
import { applyServerErrors } from '@tusalud/app/core/validators/server-errors';
import { EmptyState } from '@tusalud/app/shared/components/empty-state/empty-state';
import { FormField } from '@tusalud/app/shared/components/form-field/form-field';
import { PageHeader } from '@tusalud/app/shared/components/page-header/page-header';

@Component({
  selector: 'app-metodos-pago',
  imports: [
    ReactiveFormsModule,
    Button,
    Dialog,
    InputText,
    Select,
    Skeleton,
    Tag,
    ToggleSwitch,
    EmptyState,
    FormField,
    PageHeader,
  ],
  templateUrl: './metodos-pago.html',
  styleUrl: './metodos-pago.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MetodosPago implements OnInit {
  protected readonly store = inject(PagosStore);
  private readonly notifications = inject(NotificationService);
  private readonly confirmation = inject(ConfirmationService);

  protected readonly dialogo = signal(false);
  protected readonly guardando = signal(false);

  protected readonly tipos = [
    { label: 'Tarjeta de crédito', value: 'TARJETA_CREDITO' },
    { label: 'Tarjeta de débito', value: 'TARJETA_DEBITO' },
  ];
  protected readonly meses = Array.from({ length: 12 }, (_, i) => ({
    label: String(i + 1).padStart(2, '0'),
    value: i + 1,
  }));
  protected readonly anios = Array.from({ length: 12 }, (_, i) => {
    const anio = new Date().getFullYear() + i;
    return { label: String(anio), value: anio };
  });

  protected readonly form = inject(FormBuilder).group({
    tipo: ['TARJETA_CREDITO' as string, [Validators.required]],
    titular: ['', [Validators.required, AppValidators.notBlank, Validators.maxLength(80)]],
    numeroTarjeta: ['', [Validators.required, AppValidators.tarjeta]],
    vencimientoMes: [null as number | null, [Validators.required]],
    vencimientoAnio: [null as number | null, [Validators.required]],
    cvv: ['', [Validators.required, AppValidators.cvv]],
    predeterminado: [false],
  });

  constructor() {
    this.form.addValidators(AppValidators.vencimientoTarjeta('vencimientoMes', 'vencimientoAnio'));
  }

  protected readonly metodos = computed(() => this.store.metodos.value() ?? []);

  ngOnInit(): void {
    this.store.cargarMetodos();
  }

  protected abrir(): void {
    this.form.reset({ tipo: 'TARJETA_CREDITO', predeterminado: this.metodos().length === 0 });
    this.dialogo.set(true);
  }

  protected errorVencimiento(): string | null {
    return this.form.errors?.['tarjetaVencida'] && (this.form.touched || this.form.dirty)
      ? 'La tarjeta está vencida.'
      : null;
  }

  protected formatearNumero(): void {
    const control = this.form.controls.numeroTarjeta;
    const digitos = String(control.value ?? '')
      .replace(/\D/g, '')
      .slice(0, 19);
    control.setValue(digitos.replace(/(\d{4})(?=\d)/g, '$1 '), { emitEvent: false });
  }

  protected guardar(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      return;
    }
    const valor = this.form.getRawValue();
    this.guardando.set(true);
    this.store
      .crearMetodo({
        tipo: valor.tipo as 'TARJETA_CREDITO' | 'TARJETA_DEBITO',
        titular: valor.titular!.trim().toUpperCase(),
        numeroTarjeta: valor.numeroTarjeta!.replace(/\s+/g, ''),
        vencimientoMes: valor.vencimientoMes!,
        vencimientoAnio: valor.vencimientoAnio!,
        cvv: valor.cvv!,
        predeterminado: !!valor.predeterminado,
      })
      .subscribe({
        next: () => {
          this.guardando.set(false);
          this.dialogo.set(false);
          this.notifications.success('Tu método de pago fue agregado.');
        },
        error: (error: ApiError) => {
          this.guardando.set(false);
          applyServerErrors(this.form, error);
        },
      });
  }

  protected predeterminar(metodo: MetodoPagoResponse): void {
    this.store.marcarPredeterminado(metodo.id).subscribe({
      next: () => this.notifications.success('Se actualizó tu método predeterminado.'),
      error: () => undefined,
    });
  }

  protected eliminar(metodo: MetodoPagoResponse): void {
    this.confirmation.confirm({
      header: 'Eliminar método de pago',
      message: `¿Quieres eliminar la tarjeta ${metodo.marca} terminada en ${metodo.ultimos4}?`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Eliminar',
      rejectLabel: 'Cancelar',
      acceptButtonProps: { severity: 'danger' },
      rejectButtonProps: { severity: 'secondary', outlined: true },
      accept: () =>
        this.store.eliminarMetodo(metodo.id).subscribe({
          next: () => this.notifications.success('El método de pago fue eliminado.'),
          error: () => undefined,
        }),
    });
  }
}
