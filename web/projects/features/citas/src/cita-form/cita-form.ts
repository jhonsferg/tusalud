import { CurrencyPipe, DatePipe } from '@angular/common';
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
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { startWith } from 'rxjs';
import { Button } from 'primeng/button';
import { Select } from 'primeng/select';
import { SelectButton } from 'primeng/selectbutton';
import { Textarea } from 'primeng/textarea';

import {
  ApiError,
  applyServerErrors,
  AppValidators,
  CitasStore,
  combineDateAndTime,
  EspecialidadesStore,
  MedicosStore,
  ModalidadCita,
  NotificationService,
  optionalIdAttribute,
  parseIsoDate,
  toIsoDate,
} from '@tusalud/core';
import {
  Calendario,
  CalEvento,
  CalHueco,
  RangoVisible,
  VistaCalendario,
} from '@tusalud/ui/calendario';
import { citaAEvento, construirHuecos, horarioLaboral } from '@tusalud/ui/utils';
import { FormField } from '@tusalud/ui/form-field';
import { PageHeader } from '@tusalud/ui/page-header';

const MOTIVO_MAX = 500;

@Component({
  selector: 'app-cita-form',
  imports: [
    CurrencyPipe,
    DatePipe,
    ReactiveFormsModule,
    RouterLink,
    Button,
    Select,
    SelectButton,
    Calendario,
    Textarea,
    FormField,
    PageHeader,
  ],
  templateUrl: './cita-form.html',
  styleUrl: './cita-form.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CitaForm {
  readonly medicoId = input(undefined, { transform: optionalIdAttribute });
  readonly especialidadId = input(undefined, { transform: optionalIdAttribute });
  readonly fecha = input<string>();
  readonly hora = input<string>();

  protected readonly medicos = inject(MedicosStore);
  protected readonly especialidades = inject(EspecialidadesStore);
  private readonly citas = inject(CitasStore);
  private readonly notifications = inject(NotificationService);
  private readonly router = inject(Router);

  protected readonly submitting = signal(false);
  protected readonly minimo = new Date();
  protected readonly motivoMax = MOTIVO_MAX;

  protected readonly form = inject(FormBuilder).group({
    especialidadId: [null as string | null, [Validators.required]],
    medicoId: [null as string | null, [Validators.required]],
    fecha: [null as Date | null, [Validators.required]],
    hora: [null as string | null, [Validators.required]],
    motivo: ['', [Validators.required, AppValidators.notBlank, Validators.maxLength(MOTIVO_MAX)]],
    modalidad: ['PRESENCIAL' as ModalidadCita],
  });

  protected readonly modalidades = [
    { label: 'Presencial', value: 'PRESENCIAL' },
    { label: 'Teleconsulta', value: 'VIRTUAL' },
  ];

  private readonly valores = toSignal(this.form.valueChanges.pipe(startWith(this.form.value)), {
    initialValue: this.form.value,
  });

  protected readonly precioReserva = computed(() => {
    const especialidad = this.especialidadElegida();
    if (!especialidad) {
      return 0;
    }
    return this.valores().modalidad === 'VIRTUAL' && especialidad.precioVirtual
      ? especialidad.precioVirtual
      : especialidad.precioConsulta;
  });

  protected readonly opcionesEspecialidad = computed(() =>
    this.especialidades.especialidades().map((especialidad) => ({
      value: especialidad.id,
      label: especialidad.nombre,
      precio: especialidad.precioConsulta,
    })),
  );

  protected readonly opcionesMedico = computed(() => {
    const especialidadId = this.valores().especialidadId;
    return this.medicos
      .medicosActivos()
      .filter(
        (medico) =>
          !especialidadId ||
          medico.especialidades.some((especialidad) => especialidad.id === especialidadId),
      )
      .map((medico) => ({
        value: medico.id,
        label: `${medico.nombreCompleto} · ${medico.especialidades.map((e) => e.nombre).join(', ')}`,
      }));
  });

  protected readonly medicoElegido = computed(() =>
    this.medicos.medicosActivos().find((medico) => medico.id === this.valores().medicoId),
  );

  protected readonly especialidadElegida = computed(() =>
    this.especialidades.especialidades().find((item) => item.id === this.valores().especialidadId),
  );

  protected readonly vistaCalendario = signal<VistaCalendario>('semana');
  protected readonly fechaCalendario = signal(new Date());
  private readonly rangoCalendario = signal<RangoVisible | null>(null);

  protected readonly eventosPropios = computed<CalEvento[]>(() =>
    this.citas.proximas().map((cita) => ({
      ...citaAEvento(cita, 'medico'),
      titulo: 'Tu cita',
      detalle: cita.medicoNombre,
      color: 'secondary' as const,
    })),
  );

  protected readonly huecos = computed<CalHueco[] | null>(() =>
    this.valores().medicoId
      ? construirHuecos(this.medicos.disponibilidadRango.value() ?? [])
      : null,
  );

  protected readonly laboral = computed(() => horarioLaboral(this.medicos.horarios.value() ?? []));

  protected readonly seleccionActual = computed(() => {
    const { fecha, hora } = this.valores();
    if (!fecha || !hora) {
      return null;
    }
    const [h, m] = hora.split(':').map(Number);
    return new Date(fecha.getFullYear(), fecha.getMonth(), fecha.getDate(), h, m);
  });

  protected readonly fechaHora = computed(() => {
    const { fecha, hora } = this.valores();
    return fecha && hora ? combineDateAndTime(toIsoDate(fecha), hora) : null;
  });

  constructor() {
    this.medicos.load(null);
    this.especialidades.load();

    effect(() => {
      const especialidadParam = this.especialidadId();
      if (especialidadParam !== undefined) {
        untracked(() => this.form.controls.especialidadId.setValue(especialidadParam));
      }
    });

    effect(() => {
      const medico = this.medicoElegido();
      if (!medico) {
        return;
      }
      untracked(() => {
        const actual = this.form.controls.especialidadId.value;
        if (!actual || !medico.especialidades.some((especialidad) => especialidad.id === actual)) {
          this.form.controls.especialidadId.setValue(medico.especialidades[0]?.id ?? null);
        }
      });
    });

    effect(() => {
      const medicoId = this.medicoId();
      const fechaParam = this.fecha();
      const horaParam = this.hora();
      untracked(() => {
        if (medicoId !== undefined) {
          this.form.controls.medicoId.setValue(medicoId);
        }
        const fecha = fechaParam ? parseIsoDate(fechaParam) : null;
        if (fecha) {
          this.form.controls.fecha.setValue(fecha);
        }
        if (horaParam) {
          this.form.controls.hora.setValue(horaParam);
        }
      });
    });

    this.citas.loadMisCitas();

    effect(() => {
      const id = this.valores().medicoId;
      if (id) {
        untracked(() => this.medicos.loadHorarios(id));
      }
    });

    effect(() => {
      const medicoId = this.valores().medicoId;
      const rango = this.rangoCalendario();
      if (medicoId && rango) {
        untracked(() =>
          this.medicos.loadDisponibilidadRango(
            medicoId,
            toIsoDate(rango.inicio),
            toIsoDate(rango.fin),
          ),
        );
      }
    });

    effect(() => {
      const fecha = this.form.controls.fecha.value;
      const seleccion = this.seleccionActual();
      if (seleccion || fecha) {
        untracked(() => this.fechaCalendario.set(seleccion ?? fecha ?? new Date()));
      }
    });
  }

  protected cargarRango(rango: RangoVisible): void {
    this.rangoCalendario.set(rango);
  }

  protected elegirHueco(inicio: Date): void {
    const hora = `${String(inicio.getHours()).padStart(2, '0')}:${String(inicio.getMinutes()).padStart(2, '0')}`;
    this.form.controls.fecha.setValue(
      new Date(inicio.getFullYear(), inicio.getMonth(), inicio.getDate()),
    );
    this.form.controls.hora.setValue(hora);
    this.form.controls.fecha.markAsTouched();
    this.form.controls.hora.markAsTouched();
  }

  protected cambioEspecialidad(): void {
    const medico = this.medicoElegido();
    const especialidadId = this.form.controls.especialidadId.value;
    if (
      medico &&
      !medico.especialidades.some((especialidad) => especialidad.id === especialidadId)
    ) {
      this.form.controls.medicoId.setValue(null);
    }
    this.form.controls.hora.setValue(null);
  }

  protected cambioMedico(): void {
    this.form.controls.hora.setValue(null);
    this.form.controls.fecha.setValue(null);
  }

  protected submit(): void {
    this.form.markAllAsTouched();
    const fechaHora = this.fechaHora();
    if (this.form.invalid || !fechaHora) {
      return;
    }
    const valor = this.form.getRawValue();
    this.submitting.set(true);
    this.citas
      .crear({
        medicoId: valor.medicoId!,
        especialidadId: valor.especialidadId!,
        fechaHora,
        motivo: valor.motivo!.trim(),
        modalidad: valor.modalidad ?? 'PRESENCIAL',
      })
      .subscribe({
        next: (cita) => {
          this.notifications.success('Tu cita fue reservada. El médico la confirmará pronto.');
          void this.router.navigate(['/citas', cita.id]);
        },
        error: (error: ApiError) => {
          this.submitting.set(false);
          applyServerErrors(this.form, error);
          if (error.status === 409) {
            this.form.controls.hora.setValue(null);
            const rango = this.rangoCalendario();
            if (valor.medicoId && rango) {
              this.medicos.loadDisponibilidadRango(
                valor.medicoId,
                toIsoDate(rango.inicio),
                toIsoDate(rango.fin),
              );
            }
            this.citas.loadMisCitas();
          }
        },
      });
  }
}
