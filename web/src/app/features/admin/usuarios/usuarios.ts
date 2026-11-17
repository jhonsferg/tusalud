import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MultiSelect } from 'primeng/multiselect';
import { Password } from 'primeng/password';
import { ConfirmationService } from 'primeng/api';
import { Button } from 'primeng/button';
import { Checkbox } from 'primeng/checkbox';
import { Dialog } from 'primeng/dialog';
import { Message } from 'primeng/message';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { Tooltip } from 'primeng/tooltip';
import { Subject, debounceTime } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import {
  AdminStore,
  AuthService,
  DESCRIPCION_ROL,
  ETIQUETA_ROL,
  NotificationService,
  ROLES,
  ROLES_PERSONAL,
  UsuarioAdminResponse,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { UserAvatar } from '@tusalud/ui/user-avatar';

@Component({
  selector: 'app-usuarios',
  imports: [
    DatePipe,
    FormsModule,
    ReactiveFormsModule,
    MultiSelect,
    Password,
    Button,
    Checkbox,
    Dialog,
    Message,
    IconField,
    InputIcon,
    InputText,
    Select,
    TableModule,
    Tag,
    Tooltip,
    EmptyState,
    PageHeader,
    UserAvatar,
  ],
  templateUrl: './usuarios.html',
  styleUrl: './usuarios.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Usuarios implements OnInit {
  protected readonly store = inject(AdminStore);
  protected readonly auth = inject(AuthService);
  private readonly confirmation = inject(ConfirmationService);
  private readonly notifications = inject(NotificationService);

  protected readonly rol = signal<string | null>(null);
  protected readonly busqueda = signal('');
  private readonly busquedaCambia = new Subject<void>();
  protected readonly etiquetaRol = ETIQUETA_ROL;
  protected readonly roles = ROLES.map((rol) => ({ label: ETIQUETA_ROL[rol], value: rol }));
  protected readonly rolesPersonal = ROLES_PERSONAL.map((rol) => ({
    label: ETIQUETA_ROL[rol],
    value: rol,
  }));

  constructor() {
    this.busquedaCambia
      .pipe(debounceTime(300), takeUntilDestroyed())
      .subscribe(() => this.cargar());
  }

  protected readonly editando = signal<UsuarioAdminResponse | null>(null);
  protected readonly seleccion = signal<string[]>([]);
  protected readonly guardando = signal(false);
  protected readonly errorRoles = signal<string | null>(null);
  protected readonly descripcionRol = DESCRIPCION_ROL;

  ngOnInit(): void {
    this.cargar();
    this.store.cargarRoles();
  }

  protected readonly personalVisible = signal(false);
  protected readonly creandoPersonal = signal(false);
  protected readonly errorPersonal = signal<string | null>(null);
  protected readonly personal = inject(FormBuilder).nonNullable.group({
    nombre: ['', [Validators.required, Validators.maxLength(80)]],
    apellido: ['', [Validators.required, Validators.maxLength(80)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
    telefono: ['', [Validators.pattern(/^$|^\d{9}$/)]],
    roles: [[] as string[], [Validators.required, Validators.minLength(1)]],
  });

  protected abrirPersonal(): void {
    this.errorPersonal.set(null);
    this.personal.reset({
      nombre: '',
      apellido: '',
      email: '',
      password: '',
      telefono: '',
      roles: [],
    });
    this.personalVisible.set(true);
  }

  protected crearPersonal(): void {
    this.errorPersonal.set(null);
    if (this.personal.invalid) {
      this.personal.markAllAsTouched();
      return;
    }
    const valor = this.personal.getRawValue();
    this.creandoPersonal.set(true);
    this.store
      .crearPersonal({
        nombre: valor.nombre.trim(),
        apellido: valor.apellido.trim(),
        email: valor.email.trim().toLowerCase(),
        password: valor.password,
        telefono: valor.telefono || null,
        roles: valor.roles,
      })
      .subscribe({
        next: (usuario) => {
          this.creandoPersonal.set(false);
          this.personalVisible.set(false);
          this.notifications.success(`${usuario.nombreCompleto} fue registrado como personal.`);
        },
        error: (error: { message: string }) => {
          this.creandoPersonal.set(false);
          this.errorPersonal.set(error.message);
        },
      });
  }

  protected abrirRoles(usuario: UsuarioAdminResponse): void {
    this.errorRoles.set(null);
    this.seleccion.set([...usuario.roles]);
    this.editando.set(usuario);
  }

  protected alternarRolSeleccionado(rol: string, marcado: boolean): void {
    this.seleccion.update((actual) =>
      marcado ? [...new Set([...actual, rol])] : actual.filter((item) => item !== rol),
    );
  }

  protected guardarRoles(): void {
    const usuario = this.editando();
    if (!usuario || this.seleccion().length === 0) {
      return;
    }
    this.errorRoles.set(null);
    this.guardando.set(true);
    this.store.actualizarRoles(usuario.id, this.seleccion()).subscribe({
      next: () => {
        this.guardando.set(false);
        this.editando.set(null);
        this.notifications.success('Los roles del usuario fueron actualizados.');
      },
      error: (error: { message: string }) => {
        this.guardando.set(false);
        this.errorRoles.set(error.message);
      },
    });
  }

  protected cargar(): void {
    this.store.cargarUsuarios(this.rol(), this.busqueda().trim() || null);
  }

  protected cambiarRol(valor: string | null): void {
    this.rol.set(valor);
    this.cargar();
  }

  protected escribir(valor: string): void {
    this.busqueda.set(valor);
    this.busquedaCambia.next();
  }

  protected esYo(usuario: UsuarioAdminResponse): boolean {
    return usuario.id === this.auth.user()?.id;
  }

  protected alternarEstado(usuario: UsuarioAdminResponse): void {
    const desactivar = usuario.estado === 'ACTIVO';
    this.confirmation.confirm({
      header: desactivar ? 'Desactivar usuario' : 'Activar usuario',
      message: desactivar
        ? `${usuario.nombreCompleto} no podrá iniciar sesión y sus sesiones activas se cerrarán.`
        : `${usuario.nombreCompleto} podrá volver a iniciar sesión.`,
      icon: desactivar ? 'pi pi-ban' : 'pi pi-check-circle',
      acceptLabel: desactivar ? 'Desactivar' : 'Activar',
      rejectLabel: 'Cancelar',
      acceptButtonProps: { severity: desactivar ? 'danger' : 'success' },
      rejectButtonProps: { severity: 'secondary', outlined: true },
      accept: () =>
        this.store.cambiarEstadoUsuario(usuario.id, desactivar ? 'INACTIVO' : 'ACTIVO').subscribe({
          next: () =>
            this.notifications.success(
              desactivar ? 'El usuario fue desactivado.' : 'El usuario fue activado.',
            ),
          error: () => undefined,
        }),
    });
  }
}
