import { CurrencyPipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  effect,
  inject,
  input,
  signal,
  untracked,
} from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Button } from 'primeng/button';
import { Message } from 'primeng/message';
import { MultiSelect } from 'primeng/multiselect';
import { Select } from 'primeng/select';
import { SelectButton } from 'primeng/selectbutton';
import { Textarea } from 'primeng/textarea';

import { ETIQUETA_CATEGORIA, PrioridadOrden } from '@tusalud/app/core/models';
import { NotificationService } from '@tusalud/app/core/services/notification.service';
import { ClinicaStore } from '@tusalud/app/core/state/clinica.store';
import { LaboratorioStore } from '@tusalud/app/core/state/laboratorio.store';
import { optionalNumberAttribute } from '@tusalud/app/core/utils/route-input';
import { PageHeader } from '@tusalud/app/shared/components/page-header/page-header';

@Component({
  selector: 'app-solicitar',
  imports: [
    CurrencyPipe,
    FormsModule,
    ReactiveFormsModule,
    Button,
    Message,
    MultiSelect,
    Select,
    SelectButton,
    Textarea,
    PageHeader,
  ],
  templateUrl: './solicitar.html',
  styleUrl: './solicitar.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Solicitar implements OnInit {
  readonly pacienteId = input(undefined, { transform: optionalNumberAttribute });
  readonly atencionId = input(undefined, { transform: optionalNumberAttribute });

  protected readonly store = inject(LaboratorioStore);
  protected readonly clinica = inject(ClinicaStore);
  private readonly notifications = inject(NotificationService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder).nonNullable;

  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly prioridades = [
    { label: 'Normal', value: 'NORMAL' },
    { label: 'Urgente', value: 'URGENTE' },
  ];

  protected readonly form = this.fb.group({
    pacienteId: [null as number | null, [Validators.required]],
    prioridad: ['NORMAL' as PrioridadOrden, [Validators.required]],
    indicaciones: ['', [Validators.maxLength(500)]],
    examenIds: [[] as number[], [Validators.required, Validators.minLength(1)]],
  });

  private readonly seleccion = signal<number[]>([]);

  protected readonly pacientes = computed(() =>
    (this.clinica.pacientes.value() ?? []).map((paciente) => ({
      label: `${paciente.nombreCompleto} · DNI ${paciente.dni}`,
      value: paciente.pacienteId,
    })),
  );

  protected readonly examenes = computed(() =>
    (this.store.examenes.value() ?? [])
      .filter((examen) => examen.activo)
      .map((examen) => ({
        label: `${examen.nombre} · ${ETIQUETA_CATEGORIA[examen.categoria]}`,
        value: examen.id,
        precio: examen.precio,
        indicaciones: examen.indicaciones,
      })),
  );

  protected readonly total = computed(() => {
    const elegidos = new Set(this.seleccion());
    return this.examenes()
      .filter((examen) => elegidos.has(examen.value))
      .reduce((suma, examen) => suma + examen.precio, 0);
  });

  protected readonly indicacionesExamenes = computed(() => {
    const elegidos = new Set(this.seleccion());
    return this.examenes()
      .filter((examen) => elegidos.has(examen.value) && examen.indicaciones)
      .map((examen) => `${examen.label.split(' · ')[0]}: ${examen.indicaciones}`);
  });

  constructor() {
    effect(() => {
      const id = this.pacienteId();
      if (id !== undefined) {
        untracked(() => this.form.controls.pacienteId.setValue(id));
      }
    });
    this.form.controls.examenIds.valueChanges.subscribe((valor) => this.seleccion.set(valor));
  }

  ngOnInit(): void {
    this.clinica.cargarMisPacientes();
    this.store.cargarExamenes({ activo: true });
  }

  protected enviar(): void {
    this.error.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const valor = this.form.getRawValue();
    this.guardando.set(true);
    this.store
      .crearOrden({
        pacienteId: valor.pacienteId!,
        atencionId: this.atencionId() ?? null,
        prioridad: valor.prioridad,
        indicaciones: valor.indicaciones.trim() || null,
        examenIds: valor.examenIds,
      })
      .subscribe({
        next: (orden) => {
          this.guardando.set(false);
          this.notifications.success(`La orden ${orden.codigo} fue enviada al laboratorio.`);
          void this.router.navigate(['/laboratorio/ordenes', orden.id]);
        },
        error: (error: { message: string }) => {
          this.guardando.set(false);
          this.error.set(error.message);
        },
      });
  }
}
