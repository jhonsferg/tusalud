import { CurrencyPipe, DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Button } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';
import { Dialog } from 'primeng/dialog';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { Subject, debounceTime } from 'rxjs';

import {
  AREAS_LABORALES,
  AuthService,
  ESTADOS_EMPLEADO,
  ETIQUETA_AREA_LABORAL,
  ETIQUETA_CONTRATO,
  ETIQUETA_ESTADO_EMPLEADO,
  EmpleadoRequest,
  EmpleadoResponse,
  NotificationService,
  RrhhApiService,
  RrhhStore,
  SaldoVacacionesResponse,
  TIPOS_CONTRATO,
  parseIsoDate,
  toIsoDate,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { severidadEmpleado } from '../shared/ausencia';

@Component({
  selector: 'app-empleados',
  imports: [
    CurrencyPipe,
    DatePipe,
    FormsModule,
    ReactiveFormsModule,
    Button,
    DatePicker,
    Dialog,
    IconField,
    InputIcon,
    InputNumber,
    InputText,
    Message,
    Select,
    TableModule,
    Tag,
    EmptyState,
    PageHeader,
  ],
  templateUrl: './empleados.html',
  styleUrl: './empleados.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Empleados implements OnInit {
  protected readonly store = inject(RrhhStore);
  private readonly api = inject(RrhhApiService);
  private readonly auth = inject(AuthService);
  private readonly notifications = inject(NotificationService);
  private readonly fb = inject(FormBuilder).nonNullable;

  protected readonly gestiona = computed(() => this.auth.isRrhh());
  protected readonly etiquetaArea = ETIQUETA_AREA_LABORAL;
  protected readonly etiquetaContrato = ETIQUETA_CONTRATO;
  protected readonly etiquetaEstado = ETIQUETA_ESTADO_EMPLEADO;
  protected readonly severidad = severidadEmpleado;
  protected readonly areas = AREAS_LABORALES.map((a) => ({
    label: ETIQUETA_AREA_LABORAL[a],
    value: a,
  }));
  protected readonly contratos = TIPOS_CONTRATO.map((c) => ({
    label: ETIQUETA_CONTRATO[c],
    value: c,
  }));
  protected readonly estados = ESTADOS_EMPLEADO.map((e) => ({
    label: ETIQUETA_ESTADO_EMPLEADO[e],
    value: e,
  }));
  protected readonly hoy = new Date();

  protected readonly busqueda = signal('');
  protected readonly filtroArea = signal<string | null>(null);
  protected readonly filtroEstado = signal<string | null>(null);
  private readonly cambio = new Subject<void>();

  protected readonly muestraSueldo = computed(() =>
    (this.store.empleados.value() ?? []).some((e) => e.sueldo != null),
  );

  protected readonly ficha = signal<EmpleadoResponse | null>(null);
  protected readonly saldo = signal<SaldoVacacionesResponse | null>(null);

  protected readonly formVisible = signal(false);
  protected readonly editando = signal<EmpleadoResponse | null>(null);
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly usuarios = computed(() =>
    (this.store.usuariosDisponibles.value() ?? []).map((u) => ({
      label: u.nombreCompleto + ' · ' + u.email,
      value: u.usuarioId,
    })),
  );

  protected readonly form = this.fb.group({
    usuarioId: [''],
    cargo: ['', [Validators.required, Validators.maxLength(100)]],
    area: ['MEDICA', [Validators.required]],
    tipoContrato: ['INDEFINIDO', [Validators.required]],
    fechaIngreso: [null as Date | null, [Validators.required]],
    fechaFinContrato: [null as Date | null],
    estado: ['ACTIVO'],
    sueldo: [0, [Validators.required, Validators.min(0)]],
    contactoEmergenciaNombre: [''],
    contactoEmergenciaTelefono: ['', [Validators.pattern(/^$|^\d{9}$/)]],
    diasVacacionesAnuales: [30],
  });

  constructor() {
    this.cambio.pipe(debounceTime(300), takeUntilDestroyed()).subscribe(() => this.cargar());
  }

  ngOnInit(): void {
    this.cargar();
  }

  protected buscar(valor: string): void {
    this.busqueda.set(valor);
    this.cambio.next();
  }

  protected cargar(): void {
    this.store.cargarEmpleados({
      q: this.busqueda().trim() || null,
      area: this.filtroArea(),
      estado: this.filtroEstado(),
    });
  }

  protected verFicha(empleado: EmpleadoResponse): void {
    this.ficha.set(empleado);
    this.saldo.set(null);
    if (this.gestiona()) {
      this.api.saldoVacaciones(empleado.id).subscribe({ next: (s) => this.saldo.set(s) });
    }
  }

  protected abrir(empleado: EmpleadoResponse | null): void {
    this.editando.set(empleado);
    this.error.set(null);
    if (!empleado) {
      this.store.cargarUsuariosDisponibles();
    }
    this.form.reset({
      usuarioId: '',
      cargo: empleado?.cargo ?? '',
      area: empleado?.area ?? 'MEDICA',
      tipoContrato: empleado?.tipoContrato ?? 'INDEFINIDO',
      fechaIngreso: empleado ? parseIsoDate(empleado.fechaIngreso) : null,
      fechaFinContrato: empleado?.fechaFinContrato ? parseIsoDate(empleado.fechaFinContrato) : null,
      estado: empleado?.estado ?? 'ACTIVO',
      sueldo: empleado?.sueldo ?? 0,
      contactoEmergenciaNombre: empleado?.contactoEmergenciaNombre ?? '',
      contactoEmergenciaTelefono: empleado?.contactoEmergenciaTelefono ?? '',
      diasVacacionesAnuales: empleado?.diasVacacionesAnuales ?? 30,
    });
    this.formVisible.set(true);
  }

  protected guardar(): void {
    this.error.set(null);
    const valor = this.form.getRawValue();
    const actual = this.editando();
    if (this.form.invalid || (!actual && !valor.usuarioId)) {
      this.form.markAllAsTouched();
      this.error.set('Completa el usuario, el cargo, el sueldo y la fecha de ingreso.');
      return;
    }
    const request: EmpleadoRequest = {
      usuarioId: actual ? null : valor.usuarioId,
      cargo: valor.cargo.trim(),
      area: valor.area as EmpleadoRequest['area'],
      tipoContrato: valor.tipoContrato as EmpleadoRequest['tipoContrato'],
      fechaIngreso: toIsoDate(valor.fechaIngreso as Date),
      fechaFinContrato: valor.fechaFinContrato ? toIsoDate(valor.fechaFinContrato) : null,
      estado: valor.estado as EmpleadoRequest['estado'],
      sueldo: valor.sueldo,
      contactoEmergenciaNombre: valor.contactoEmergenciaNombre.trim() || null,
      contactoEmergenciaTelefono: valor.contactoEmergenciaTelefono.trim() || null,
      diasVacacionesAnuales: valor.diasVacacionesAnuales,
    };
    this.guardando.set(true);
    const operacion = actual
      ? this.store.actualizarEmpleado(actual.id, request)
      : this.store.crearEmpleado(request);
    operacion.subscribe({
      next: () => {
        this.guardando.set(false);
        this.formVisible.set(false);
        this.notifications.success('La ficha laboral fue guardada.');
        this.cargar();
      },
      error: (error: { message: string }) => {
        this.guardando.set(false);
        this.error.set(error.message);
      },
    });
  }
}
