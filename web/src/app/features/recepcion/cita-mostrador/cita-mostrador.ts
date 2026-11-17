import { CurrencyPipe, DatePipe } from '@angular/common';
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
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Button } from 'primeng/button';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';
import { Textarea } from 'primeng/textarea';

import {
  ApiError,
  EspecialidadesStore,
  MedicosStore,
  NotificationService,
  PacienteMostradorResponse,
  RecepcionApiService,
  toIsoDate,
  toIsoDateTime,
} from '@tusalud/core';
import { Calendario, RangoVisible, VistaCalendario } from '@tusalud/ui/calendario';
import { construirHuecos, horarioLaboral } from '@tusalud/ui/utils';
import { PacienteBuscador } from '@tusalud/ui/paciente-buscador';
import { PageHeader } from '@tusalud/ui/page-header';

@Component({
  selector: 'app-cita-mostrador',
  imports: [
    CurrencyPipe,
    DatePipe,
    FormsModule,
    Button,
    Message,
    Select,
    Textarea,
    Calendario,
    PacienteBuscador,
    PageHeader,
  ],
  templateUrl: './cita-mostrador.html',
  styleUrl: './cita-mostrador.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CitaMostrador implements OnInit {
  readonly dni = input<string>();

  protected readonly medicos = inject(MedicosStore);
  protected readonly especialidades = inject(EspecialidadesStore);
  private readonly api = inject(RecepcionApiService);
  private readonly notifications = inject(NotificationService);
  private readonly router = inject(Router);

  protected readonly paciente = signal<PacienteMostradorResponse | null>(null);
  protected readonly especialidadId = signal<string | null>(null);
  protected readonly medicoId = signal<string | null>(null);
  protected readonly motivo = signal('');
  protected readonly elegido = signal<Date | null>(null);
  protected readonly vista = signal<VistaCalendario>('semana');
  protected readonly fecha = signal(new Date());
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);
  private readonly rango = signal<RangoVisible | null>(null);

  protected readonly opcionesEspecialidad = computed(() =>
    this.especialidades
      .especialidades()
      .map((e) => ({ label: e.nombre, value: e.id, precio: e.precioConsulta })),
  );
  protected readonly opcionesMedico = computed(() => {
    const especialidad = this.especialidadId();
    return this.medicos
      .medicosActivos()
      .filter((m) => !especialidad || m.especialidades.some((e) => e.id === especialidad))
      .map((m) => ({ label: m.nombreCompleto, value: m.id }));
  });
  protected readonly precio = computed(
    () =>
      this.especialidades.especialidades().find((e) => e.id === this.especialidadId())
        ?.precioConsulta ?? 0,
  );
  protected readonly huecos = computed(() =>
    this.medicoId() ? construirHuecos(this.medicos.disponibilidadRango.value() ?? []) : null,
  );
  protected readonly laboral = computed(() => horarioLaboral(this.medicos.horarios.value() ?? []));

  constructor() {
    effect(() => {
      const medicoId = this.medicoId();
      const rango = this.rango();
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
      const id = this.medicoId();
      if (id) {
        untracked(() => this.medicos.loadHorarios(id));
      }
    });
  }

  ngOnInit(): void {
    this.medicos.load(null);
    this.especialidades.load();
    const dni = this.dni();
    if (dni) {
      this.api.buscarPacientes(dni).subscribe({
        next: (lista) =>
          this.paciente.set(lista.find((item) => item.dni === dni) ?? lista[0] ?? null),
        error: () => undefined,
      });
    }
  }

  protected cambioEspecialidad(valor: string | null): void {
    this.especialidadId.set(valor);
    const medico = this.medicos.medicosActivos().find((m) => m.id === this.medicoId());
    if (medico && valor && !medico.especialidades.some((e) => e.id === valor)) {
      this.medicoId.set(null);
    }
    this.elegido.set(null);
  }

  protected cambioMedico(valor: string | null): void {
    this.medicoId.set(valor);
    this.elegido.set(null);
    const medico = this.medicos.medicosActivos().find((m) => m.id === valor);
    if (medico && !medico.especialidades.some((e) => e.id === this.especialidadId())) {
      this.especialidadId.set(medico.especialidades[0]?.id ?? null);
    }
  }

  protected cargarRango(rango: RangoVisible): void {
    this.rango.set(rango);
  }

  protected reservar(): void {
    const paciente = this.paciente();
    const inicio = this.elegido();
    if (
      !paciente ||
      !inicio ||
      !this.medicoId() ||
      !this.especialidadId() ||
      !this.motivo().trim()
    ) {
      this.error.set(
        'Elige el paciente, la especialidad, el médico, un hueco del calendario y escribe el motivo.',
      );
      return;
    }
    this.error.set(null);
    this.guardando.set(true);
    this.api
      .reservarCita({
        pacienteId: paciente.pacienteId,
        medicoId: this.medicoId()!,
        especialidadId: this.especialidadId()!,
        fechaHora: toIsoDateTime(inicio),
        motivo: this.motivo().trim(),
      })
      .subscribe({
        next: () => {
          this.guardando.set(false);
          this.notifications.success(`La cita de ${paciente.nombreCompleto} fue reservada.`);
          void this.router.navigate(['/recepcion']);
        },
        error: (error: ApiError) => {
          this.guardando.set(false);
          this.error.set(error.message);
          this.elegido.set(null);
          const rango = this.rango();
          const medicoId = this.medicoId();
          if (rango && medicoId) {
            this.medicos.loadDisponibilidadRango(
              medicoId,
              toIsoDate(rango.inicio),
              toIsoDate(rango.fin),
            );
          }
        },
      });
  }
}
