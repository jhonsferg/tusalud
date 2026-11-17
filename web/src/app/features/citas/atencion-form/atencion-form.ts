import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  signal,
  untracked,
} from '@angular/core';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { Skeleton } from 'primeng/skeleton';
import { Textarea } from 'primeng/textarea';

import { ApiError, AtencionRequest } from '@tusalud/app/core/models';
import { NotificationService } from '@tusalud/app/core/services/notification.service';
import { CitasStore } from '@tusalud/app/core/state/citas.store';
import { ClinicaStore } from '@tusalud/app/core/state/clinica.store';
import { optionalIdAttribute } from '@tusalud/app/core/utils/route-input';
import { AppValidators } from '@tusalud/app/core/validators/app.validators';
import { applyServerErrors } from '@tusalud/app/core/validators/server-errors';
import { EmptyState } from '@tusalud/app/shared/components/empty-state/empty-state';
import { FormField } from '@tusalud/app/shared/components/form-field/form-field';
import { PageHeader } from '@tusalud/app/shared/components/page-header/page-header';

@Component({
  selector: 'app-atencion-form',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    RouterLink,
    Button,
    InputNumber,
    InputText,
    Skeleton,
    Textarea,
    EmptyState,
    FormField,
    PageHeader,
  ],
  templateUrl: './atencion-form.html',
  styleUrl: './atencion-form.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AtencionForm {
  readonly id = input(undefined, { transform: optionalIdAttribute });

  protected readonly citas = inject(CitasStore);
  protected readonly clinica = inject(ClinicaStore);
  private readonly notifications = inject(NotificationService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  protected readonly guardando = signal(false);
  protected readonly editando = computed(() => this.clinica.atencion.hasValue());

  protected readonly form = this.fb.group({
    diagnostico: ['', [Validators.required, AppValidators.notBlank, Validators.maxLength(1000)]],
    tratamiento: ['', [Validators.maxLength(2000)]],
    indicaciones: ['', [Validators.maxLength(2000)]],
    observaciones: ['', [Validators.maxLength(2000)]],
    pesoKg: [null as number | null, [Validators.min(0.5), Validators.max(500)]],
    tallaCm: [null as number | null, [Validators.min(20), Validators.max(260)]],
    presionArterial: ['', [Validators.pattern(/^\d{2,3}\/\d{2,3}$/)]],
    temperaturaC: [null as number | null, [Validators.min(30), Validators.max(45)]],
    frecuenciaCardiaca: [null as number | null, [Validators.min(20), Validators.max(250)]],
    receta: this.fb.array([]),
  });

  protected get receta(): FormArray {
    return this.form.controls.receta;
  }

  constructor() {
    effect(() => {
      const id = this.id();
      if (id !== undefined) {
        untracked(() => {
          this.clinica.atencion.reset();
          this.citas.loadOne(id);
          this.clinica.cargarAtencion(id);
        });
      }
    });
    effect(() => {
      const atencion = this.clinica.atencion.value();
      if (atencion) {
        this.receta.clear();
        atencion.receta.forEach((item) => this.agregarMedicamento(item));
        this.form.patchValue({
          diagnostico: atencion.diagnostico,
          tratamiento: atencion.tratamiento ?? '',
          indicaciones: atencion.indicaciones ?? '',
          observaciones: atencion.observaciones ?? '',
          pesoKg: atencion.pesoKg ?? null,
          tallaCm: atencion.tallaCm ?? null,
          presionArterial: atencion.presionArterial ?? '',
          temperaturaC: atencion.temperaturaC ?? null,
          frecuenciaCardiaca: atencion.frecuenciaCardiaca ?? null,
        });
        this.form.markAsPristine();
      }
    });
  }

  protected agregarMedicamento(
    valor: {
      medicamento?: string;
      dosis?: string | null;
      frecuencia?: string | null;
      duracion?: string | null;
    } = {},
  ): void {
    this.receta.push(
      this.fb.group({
        medicamento: [
          valor.medicamento ?? '',
          [Validators.required, AppValidators.notBlank, Validators.maxLength(120)],
        ],
        dosis: [valor.dosis ?? '', [Validators.maxLength(80)]],
        frecuencia: [valor.frecuencia ?? '', [Validators.maxLength(80)]],
        duracion: [valor.duracion ?? '', [Validators.maxLength(80)]],
      }),
    );
  }

  protected quitarMedicamento(indice: number): void {
    this.receta.removeAt(indice);
    this.form.markAsDirty();
  }

  protected guardar(): void {
    const id = this.id();
    this.form.markAllAsTouched();
    if (id === undefined || this.form.invalid) {
      return;
    }
    const valor = this.form.getRawValue();
    const solicitud: AtencionRequest = {
      diagnostico: valor.diagnostico!.trim(),
      tratamiento: valor.tratamiento?.trim() || null,
      indicaciones: valor.indicaciones?.trim() || null,
      observaciones: valor.observaciones?.trim() || null,
      pesoKg: valor.pesoKg,
      tallaCm: valor.tallaCm,
      presionArterial: valor.presionArterial?.trim() || null,
      temperaturaC: valor.temperaturaC,
      frecuenciaCardiaca: valor.frecuenciaCardiaca,
      receta: (
        valor.receta as {
          medicamento: string;
          dosis: string;
          frecuencia: string;
          duracion: string;
        }[]
      ).map((item) => ({
        medicamento: item.medicamento.trim(),
        dosis: item.dosis?.trim() || null,
        frecuencia: item.frecuencia?.trim() || null,
        duracion: item.duracion?.trim() || null,
      })),
    };
    this.guardando.set(true);
    const operacion = this.editando()
      ? this.clinica.actualizarAtencion(id, solicitud)
      : this.clinica.registrarAtencion(id, solicitud);
    operacion.subscribe({
      next: () => {
        this.guardando.set(false);
        this.notifications.success('La atención médica fue guardada.');
        this.citas.loadOne(id);
        void this.router.navigate(['/citas', id]);
      },
      error: (error: ApiError) => {
        this.guardando.set(false);
        applyServerErrors(this.form, error);
      },
    });
  }
}
