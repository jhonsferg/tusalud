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

import {
  ClinicaStore,
  ETIQUETA_MODALIDAD,
  ImagenesStore,
  NotificationService,
  PrioridadOrden,
  optionalIdAttribute,
} from '@tusalud/core';
import { PageHeader } from '@tusalud/ui/page-header';

@Component({
  selector: 'app-solicitar-imagen',
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
export class SolicitarImagen implements OnInit {
  readonly pacienteId = input(undefined, { transform: optionalIdAttribute });
  readonly atencionId = input(undefined, { transform: optionalIdAttribute });

  protected readonly store = inject(ImagenesStore);
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
    pacienteId: [null as string | null, [Validators.required]],
    prioridad: ['NORMAL' as PrioridadOrden, [Validators.required]],
    indicacionClinica: ['', [Validators.required, Validators.maxLength(500)]],
    estudioIds: [[] as string[], [Validators.required, Validators.minLength(1)]],
  });

  private readonly seleccion = signal<string[]>([]);

  protected readonly pacientes = computed(() =>
    (this.clinica.pacientes.value() ?? []).map((paciente) => ({
      label: `${paciente.nombreCompleto} · DNI ${paciente.dni}`,
      value: paciente.pacienteId,
    })),
  );

  protected readonly estudios = computed(() =>
    (this.store.estudios.value() ?? [])
      .filter((estudio) => estudio.activo)
      .map((estudio) => ({
        label: `${estudio.nombre} · ${ETIQUETA_MODALIDAD[estudio.modalidad]}`,
        value: estudio.id,
        precio: estudio.precio,
        preparacion: estudio.preparacion,
      })),
  );

  protected readonly total = computed(() => {
    const elegidos = new Set(this.seleccion());
    return this.estudios()
      .filter((estudio) => elegidos.has(estudio.value))
      .reduce((suma, estudio) => suma + estudio.precio, 0);
  });

  protected readonly preparaciones = computed(() => {
    const elegidos = new Set(this.seleccion());
    return this.estudios()
      .filter((estudio) => elegidos.has(estudio.value) && estudio.preparacion)
      .map((estudio) => `${estudio.label.split(' · ')[0]}: ${estudio.preparacion}`);
  });

  constructor() {
    effect(() => {
      const id = this.pacienteId();
      if (id !== undefined) {
        untracked(() => this.form.controls.pacienteId.setValue(id));
      }
    });
    this.form.controls.estudioIds.valueChanges.subscribe((valor) => this.seleccion.set(valor));
  }

  ngOnInit(): void {
    this.clinica.cargarMisPacientes();
    this.store.cargarEstudios({ activo: true });
  }

  protected enviar(): void {
    this.error.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.error.set('Elige el paciente, al menos un estudio y escribe la indicación clínica.');
      return;
    }
    const valor = this.form.getRawValue();
    this.guardando.set(true);
    this.store
      .crearOrden({
        pacienteId: valor.pacienteId!,
        atencionId: this.atencionId() ?? null,
        prioridad: valor.prioridad,
        indicacionClinica: valor.indicacionClinica.trim(),
        estudioIds: valor.estudioIds,
      })
      .subscribe({
        next: (orden) => {
          this.guardando.set(false);
          this.notifications.success(
            `La orden ${orden.codigo} fue enviada a imágenes diagnósticas.`,
          );
          void this.router.navigate(['/imagenes/ordenes', orden.id]);
        },
        error: (error: { message: string }) => {
          this.guardando.set(false);
          this.error.set(error.message);
        },
      });
  }
}
