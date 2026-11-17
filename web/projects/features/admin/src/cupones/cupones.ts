import { CurrencyPipe, DatePipe } from '@angular/common';
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
import { DatePicker } from 'primeng/datepicker';
import { Dialog } from 'primeng/dialog';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { ToggleSwitch } from 'primeng/toggleswitch';

import {
  AdminStore,
  ApiError,
  applyServerErrors,
  AppValidators,
  CuponResponse,
  NotificationService,
  parseIsoDate,
  TipoCupon,
  toIsoDate,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { FormField } from '@tusalud/ui/form-field';
import { PageHeader } from '@tusalud/ui/page-header';

@Component({
  selector: 'app-cupones',
  imports: [
    CurrencyPipe,
    DatePipe,
    ReactiveFormsModule,
    Button,
    DatePicker,
    Dialog,
    InputNumber,
    InputText,
    Select,
    TableModule,
    Tag,
    ToggleSwitch,
    EmptyState,
    FormField,
    PageHeader,
  ],
  templateUrl: './cupones.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Cupones implements OnInit {
  protected readonly store = inject(AdminStore);
  private readonly notifications = inject(NotificationService);
  private readonly confirmation = inject(ConfirmationService);

  protected readonly dialogo = signal(false);
  protected readonly guardando = signal(false);
  protected readonly editandoId = signal<string | undefined>(undefined);
  protected readonly tipos = [
    { label: 'Porcentaje', value: 'PORCENTAJE' },
    { label: 'Monto fijo', value: 'MONTO_FIJO' },
  ];

  protected readonly form = inject(FormBuilder).group({
    codigo: ['', [Validators.required, AppValidators.notBlank, Validators.maxLength(30)]],
    descripcion: ['', [Validators.maxLength(200)]],
    tipo: ['PORCENTAJE' as TipoCupon, [Validators.required]],
    valor: [null as number | null, [Validators.required, Validators.min(0.01)]],
    fechaInicio: [null as Date | null, [Validators.required]],
    fechaFin: [null as Date | null, [Validators.required]],
    usosMaximos: [null as number | null, [Validators.min(1)]],
    activo: [true],
  });

  protected readonly esPorcentaje = computed(() => this.form.controls.tipo.value === 'PORCENTAJE');

  ngOnInit(): void {
    this.store.cargarCupones();
  }

  protected nuevo(): void {
    this.editandoId.set(undefined);
    const hoy = new Date();
    const fin = new Date();
    fin.setFullYear(fin.getFullYear() + 1);
    this.form.reset({ tipo: 'PORCENTAJE', activo: true, fechaInicio: hoy, fechaFin: fin });
    this.dialogo.set(true);
  }

  protected editar(cupon: CuponResponse): void {
    this.editandoId.set(cupon.id);
    this.form.reset({
      codigo: cupon.codigo,
      descripcion: cupon.descripcion ?? '',
      tipo: cupon.tipo,
      valor: cupon.valor,
      fechaInicio: parseIsoDate(cupon.fechaInicio),
      fechaFin: parseIsoDate(cupon.fechaFin),
      usosMaximos: cupon.usosMaximos ?? null,
      activo: cupon.activo,
    });
    this.dialogo.set(true);
  }

  protected guardar(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      return;
    }
    const valor = this.form.getRawValue();
    if (valor.fechaInicio! > valor.fechaFin!) {
      this.form.controls.fechaFin.setErrors({ dateRange: true });
      return;
    }
    this.guardando.set(true);
    this.store
      .guardarCupon(
        {
          codigo: valor.codigo!.trim().toUpperCase(),
          descripcion: valor.descripcion?.trim() || null,
          tipo: valor.tipo!,
          valor: valor.valor!,
          fechaInicio: toIsoDate(valor.fechaInicio!),
          fechaFin: toIsoDate(valor.fechaFin!),
          usosMaximos: valor.usosMaximos,
          activo: !!valor.activo,
        },
        this.editandoId(),
      )
      .subscribe({
        next: () => {
          this.guardando.set(false);
          this.dialogo.set(false);
          this.notifications.success('El cupón fue guardado.');
        },
        error: (error: ApiError) => {
          this.guardando.set(false);
          applyServerErrors(this.form, error);
        },
      });
  }

  protected eliminar(cupon: CuponResponse): void {
    this.confirmation.confirm({
      header: 'Eliminar cupón',
      message: `¿Quieres eliminar el cupón ${cupon.codigo}? Solo se puede eliminar si nadie lo usó.`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Eliminar',
      rejectLabel: 'Cancelar',
      acceptButtonProps: { severity: 'danger' },
      rejectButtonProps: { severity: 'secondary', outlined: true },
      accept: () =>
        this.store.eliminarCupon(cupon.id).subscribe({
          next: () => this.notifications.success('El cupón fue eliminado.'),
          error: () => undefined,
        }),
    });
  }
}
