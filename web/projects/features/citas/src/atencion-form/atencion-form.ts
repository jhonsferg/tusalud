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

import {
  ApiError,
  applyServerErrors,
  AppValidators,
  AtencionRequest,
  Cie10Response,
  CitasStore,
  ClinicaStore,
  DiagnosticoResponse,
  TIPOS_DIAGNOSTICO,
  TipoDiagnostico,
  NotificationService,
  optionalIdAttribute,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { FormField } from '@tusalud/ui/form-field';
import { PageHeader } from '@tusalud/ui/page-header';
import { SelectorCie10 } from '@tusalud/ui/selector-cie10';

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
    SelectorCie10,
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
  protected readonly tiposDiagnostico = TIPOS_DIAGNOSTICO;
  protected readonly errorDiagnosticos = signal<string | null>(null);
  protected readonly editando = computed(() => this.clinica.atencion.hasValue());

  protected readonly form = this.fb.group({
    diagnostico: ['', [Validators.maxLength(1000)]],
    diagnosticos: this.fb.array([]),
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

  protected get diagnosticos(): FormArray {
    return this.form.controls.diagnosticos;
  }

  protected get codigosElegidos(): string[] {
    return (this.diagnosticos.getRawValue() as { codigo: string }[]).map((d) => d.codigo);
  }

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
        this.diagnosticos.clear();
        (atencion.diagnosticos ?? []).forEach((d) => this.agregarDiagnosticoDe(d));
        atencion.receta.forEach((item) => this.agregarMedicamento(item));
        this.form.patchValue({
          diagnostico: atencion.diagnosticos?.length ? '' : atencion.diagnostico,
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

  protected agregarDiagnostico(item: Cie10Response): void {
    const primero = this.diagnosticos.length === 0;
    this.errorDiagnosticos.set(null);
    this.diagnosticos.push(
      this.fb.group({
        codigo: [item.codigo],
        descripcion: [item.descripcion],
        tipo: ['DEFINITIVO' as TipoDiagnostico],
        principal: [primero],
        observacion: ['', [Validators.maxLength(200)]],
      }),
    );
    this.form.markAsDirty();
  }

  private agregarDiagnosticoDe(d: DiagnosticoResponse): void {
    this.diagnosticos.push(
      this.fb.group({
        codigo: [d.codigo],
        descripcion: [d.descripcion],
        tipo: [d.tipo],
        principal: [d.principal],
        observacion: [d.observacion ?? '', [Validators.maxLength(200)]],
      }),
    );
  }

  protected quitarDiagnostico(indice: number): void {
    const eraPrincipal = this.diagnosticos.at(indice).value.principal === true;
    this.diagnosticos.removeAt(indice);
    if (eraPrincipal && this.diagnosticos.length > 0) {
      this.diagnosticos.at(0).patchValue({ principal: true });
    }
    this.form.markAsDirty();
  }

  protected hacerPrincipal(indice: number): void {
    this.diagnosticos.controls.forEach((control, i) =>
      control.patchValue({ principal: i === indice }),
    );
    this.form.markAsDirty();
  }

  protected moverDiagnostico(indice: number, delta: number): void {
    const destino = indice + delta;
    if (destino < 0 || destino >= this.diagnosticos.length) {
      return;
    }
    const control = this.diagnosticos.at(indice);
    this.diagnosticos.removeAt(indice);
    this.diagnosticos.insert(destino, control);
    this.form.markAsDirty();
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
    if (this.diagnosticos.length === 0) {
      this.errorDiagnosticos.set(
        'Agrega al menos un diagnóstico CIE-10 y marca uno como principal.',
      );
      return;
    }
    const valor = this.form.getRawValue();
    const solicitud: AtencionRequest = {
      diagnostico: valor.diagnostico?.trim() || null,
      diagnosticos: (
        valor.diagnosticos as {
          codigo: string;
          tipo: TipoDiagnostico;
          principal: boolean;
          observacion: string;
        }[]
      ).map((d) => ({
        codigo: d.codigo,
        tipo: d.tipo,
        principal: d.principal,
        observacion: d.observacion?.trim() || null,
      })),
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
      next: (guardada) => {
        this.guardando.set(false);
        this.notifications.success('La atención médica fue guardada.');
        (guardada.advertencias ?? []).forEach((aviso) => this.notifications.warning(aviso));
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
