import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { Button } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Subject, debounceTime, switchMap, of } from 'rxjs';

import {
  NotificationService,
  PacienteMostradorResponse,
  PacienteRegistradoResponse,
  RecepcionApiService,
  toIsoDate,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';

@Component({
  selector: 'app-pacientes-mostrador',
  imports: [
    Select,
    FormsModule,
    ReactiveFormsModule,
    Button,
    DatePicker,
    Dialog,
    InputText,
    Message,
    TableModule,
    EmptyState,
    PageHeader,
  ],
  templateUrl: './pacientes-mostrador.html',
  styleUrl: './pacientes-mostrador.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PacientesMostrador {
  private readonly api = inject(RecepcionApiService);
  private readonly notifications = inject(NotificationService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder).nonNullable;

  protected readonly sexos = [
    { value: 'F', label: 'Femenino' },
    { value: 'M', label: 'Masculino' },
  ];
  protected readonly termino = signal('');
  protected readonly resultados = signal<PacienteMostradorResponse[]>([]);
  protected readonly buscando = signal(false);
  protected readonly buscado = signal(false);
  private readonly consulta = new Subject<string>();

  protected readonly formVisible = signal(false);
  protected readonly registrado = signal<PacienteRegistradoResponse | null>(null);
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly ayer = new Date(Date.now() - 86_400_000);

  protected readonly form = this.fb.group({
    nombre: ['', [Validators.required, Validators.maxLength(80)]],
    apellido: ['', [Validators.required, Validators.maxLength(80)]],
    dni: ['', [Validators.required, Validators.pattern(/^\d{8}$/)]],
    fechaNacimiento: [null as Date | null, [Validators.required]],
    sexo: [null as string | null],
    telefono: ['', [Validators.pattern(/^$|^\d{9}$/)]],
    email: ['', [Validators.email]],
  });

  constructor() {
    this.consulta
      .pipe(
        debounceTime(300),
        switchMap((texto) => {
          if (texto.trim().length < 2) {
            this.buscado.set(false);
            return of([] as PacienteMostradorResponse[]);
          }
          this.buscando.set(true);
          return this.api.buscarPacientes(texto.trim());
        }),
        takeUntilDestroyed(),
      )
      .subscribe({
        next: (lista) => {
          this.buscando.set(false);
          this.buscado.set(this.termino().trim().length >= 2);
          this.resultados.set(lista);
        },
        error: () => this.buscando.set(false),
      });
  }

  protected buscar(valor: string): void {
    this.termino.set(valor);
    this.consulta.next(valor);
  }

  protected abrir(): void {
    this.error.set(null);
    this.form.reset({
      nombre: '',
      apellido: '',
      dni: '',
      fechaNacimiento: null,
      sexo: null,
      telefono: '',
      email: '',
    });
    this.formVisible.set(true);
  }

  protected guardar(): void {
    this.error.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.error.set('Completa nombre, apellido, DNI de 8 dígitos y fecha de nacimiento.');
      return;
    }
    const valor = this.form.getRawValue();
    this.guardando.set(true);
    this.api
      .registrarPaciente({
        nombre: valor.nombre.trim(),
        apellido: valor.apellido.trim(),
        dni: valor.dni,
        fechaNacimiento: toIsoDate(valor.fechaNacimiento!),
        sexo: valor.sexo || null,
        telefono: valor.telefono || null,
        email: valor.email.trim() || null,
      })
      .subscribe({
        next: (respuesta) => {
          this.guardando.set(false);
          this.formVisible.set(false);
          this.registrado.set(respuesta);
          this.notifications.success('El paciente fue registrado.');
        },
        error: (error: { message: string }) => {
          this.guardando.set(false);
          this.error.set(error.message);
        },
      });
  }

  protected irACita(paciente: PacienteMostradorResponse): void {
    void this.router.navigate(['/recepcion/citas/nueva'], {
      queryParams: { dni: paciente.dni },
    });
  }
}
