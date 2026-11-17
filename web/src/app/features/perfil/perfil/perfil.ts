import { CurrencyPipe, DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  effect,
  inject,
  signal,
} from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { Avatar } from 'primeng/avatar';
import { Button } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Password } from 'primeng/password';
import { Skeleton } from 'primeng/skeleton';
import { Tab, TabList, TabPanel, TabPanels, Tabs } from 'primeng/tabs';
import { Tag } from 'primeng/tag';

import { ApiError } from '@tusalud/app/core/models';
import { NotificationService } from '@tusalud/app/core/services/notification.service';
import { PerfilStore } from '@tusalud/app/core/state/perfil.store';
import { parseIsoDate, toIsoDate } from '@tusalud/app/core/utils/date';
import { AppValidators, PASSWORD_MAX } from '@tusalud/app/core/validators/app.validators';
import { applyServerErrors } from '@tusalud/app/core/validators/server-errors';
import { EmptyState } from '@tusalud/app/shared/components/empty-state/empty-state';
import { FormField } from '@tusalud/app/shared/components/form-field/form-field';
import { PageHeader } from '@tusalud/app/shared/components/page-header/page-header';

const ETIQUETA_ROL: Readonly<Record<string, string>> = {
  ROLE_ADMIN: 'Administrador',
  ROLE_MEDICO: 'Médico',
  ROLE_PACIENTE: 'Paciente',
};

function passwordsIguales(group: AbstractControl): ValidationErrors | null {
  const nueva = group.get('passwordNueva');
  const confirmar = group.get('confirmar');
  if (!nueva || !confirmar || !confirmar.value) {
    return null;
  }
  const errores = { ...(confirmar.errors ?? {}) };
  if (nueva.value === confirmar.value) {
    delete errores['passwordMismatch'];
  } else {
    errores['passwordMismatch'] = true;
  }
  confirmar.setErrors(Object.keys(errores).length > 0 ? errores : null, { emitEvent: false });
  return null;
}

@Component({
  selector: 'app-perfil',
  imports: [
    CurrencyPipe,
    DatePipe,
    ReactiveFormsModule,
    Avatar,
    Button,
    DatePicker,
    InputText,
    Message,
    Password,
    Skeleton,
    Tab,
    TabList,
    TabPanel,
    TabPanels,
    Tabs,
    Tag,
    EmptyState,
    FormField,
    PageHeader,
  ],
  templateUrl: './perfil.html',
  styleUrl: './perfil.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Perfil implements OnInit {
  protected readonly store = inject(PerfilStore);
  private readonly notifications = inject(NotificationService);
  private readonly fb = inject(FormBuilder);

  protected readonly guardando = signal(false);
  protected readonly cambiando = signal(false);
  protected readonly errorPassword = signal<string | null>(null);
  protected readonly ayer = (() => {
    const fecha = new Date();
    fecha.setDate(fecha.getDate() - 1);
    return fecha;
  })();

  protected readonly esPaciente = computed(() => !!this.store.perfil.value()?.dni);
  protected readonly esMedico = computed(() => !!this.store.perfil.value()?.medicoId);
  protected readonly roles = computed(() =>
    (this.store.perfil.value()?.roles ?? []).map((rol) => ETIQUETA_ROL[rol] ?? rol),
  );
  protected readonly iniciales = computed(() => {
    const perfil = this.store.perfil.value();
    return perfil ? `${perfil.nombre[0] ?? ''}${perfil.apellido[0] ?? ''}`.toUpperCase() : '';
  });

  protected readonly datos = this.fb.group({
    nombre: ['', [Validators.required, AppValidators.notBlank, Validators.maxLength(100)]],
    apellido: ['', [Validators.required, AppValidators.notBlank, Validators.maxLength(100)]],
    telefono: ['', [AppValidators.telefono]],
    fechaNacimiento: [null as Date | null],
    consultorio: ['', [Validators.maxLength(50)]],
  });

  protected readonly seguridad = this.fb.nonNullable.group(
    {
      passwordActual: ['', [Validators.required]],
      passwordNueva: [
        '',
        [Validators.required, AppValidators.password, Validators.maxLength(PASSWORD_MAX)],
      ],
      confirmar: ['', [Validators.required]],
    },
    { validators: [passwordsIguales] },
  );

  constructor() {
    effect(() => {
      const perfil = this.store.perfil.value();
      if (perfil) {
        this.datos.reset({
          nombre: perfil.nombre,
          apellido: perfil.apellido,
          telefono: perfil.telefono ?? '',
          fechaNacimiento: perfil.fechaNacimiento ? parseIsoDate(perfil.fechaNacimiento) : null,
          consultorio: perfil.consultorio ?? '',
        });
      }
    });
  }

  ngOnInit(): void {
    this.store.load();
  }

  protected recargar(): void {
    this.store.load();
  }

  protected descartar(): void {
    const perfil = this.store.perfil.value();
    if (perfil) {
      this.datos.reset({
        nombre: perfil.nombre,
        apellido: perfil.apellido,
        telefono: perfil.telefono ?? '',
        fechaNacimiento: perfil.fechaNacimiento ? parseIsoDate(perfil.fechaNacimiento) : null,
        consultorio: perfil.consultorio ?? '',
      });
    }
  }

  protected guardar(): void {
    if (this.datos.invalid) {
      this.datos.markAllAsTouched();
      return;
    }
    const valor = this.datos.getRawValue();
    this.guardando.set(true);
    this.store
      .actualizar({
        nombre: valor.nombre!.trim(),
        apellido: valor.apellido!.trim(),
        telefono: valor.telefono || null,
        fechaNacimiento:
          this.esPaciente() && valor.fechaNacimiento ? toIsoDate(valor.fechaNacimiento) : null,
        consultorio: this.esMedico() ? valor.consultorio?.trim() || null : null,
      })
      .subscribe({
        next: () => {
          this.guardando.set(false);
          this.notifications.success('Tus datos fueron actualizados.');
        },
        error: (error: ApiError) => {
          this.guardando.set(false);
          applyServerErrors(this.datos, error);
        },
      });
  }

  protected cambiarPassword(): void {
    this.errorPassword.set(null);
    if (this.seguridad.invalid) {
      this.seguridad.markAllAsTouched();
      return;
    }
    const { passwordActual, passwordNueva } = this.seguridad.getRawValue();
    this.cambiando.set(true);
    this.store.cambiarPassword({ passwordActual, passwordNueva }).subscribe({
      next: () => {
        this.cambiando.set(false);
        this.seguridad.reset();
        this.notifications.success('Tu contraseña fue actualizada.');
      },
      error: (error: ApiError) => {
        this.cambiando.set(false);
        const sinMapear = applyServerErrors(this.seguridad, error);
        this.errorPassword.set(sinMapear[0] ?? error.message);
      },
    });
  }
}
