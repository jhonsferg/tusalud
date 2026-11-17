import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
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

import { UsuarioAdminResponse } from '@tusalud/app/core/models';
import { AuthService } from '@tusalud/app/core/services/auth.service';
import { NotificationService } from '@tusalud/app/core/services/notification.service';
import { AdminStore } from '@tusalud/app/core/state/admin.store';
import { EmptyState } from '@tusalud/app/shared/components/empty-state/empty-state';
import { PageHeader } from '@tusalud/app/shared/components/page-header/page-header';
import { UserAvatar } from '@tusalud/app/shared/components/user-avatar/user-avatar';

const ETIQUETA_ROL: Readonly<Record<string, string>> = {
  ROLE_ADMIN: 'Administrador',
  ROLE_MEDICO: 'Médico',
  ROLE_PACIENTE: 'Paciente',
};

@Component({
  selector: 'app-usuarios',
  imports: [
    DatePipe,
    FormsModule,
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
  protected readonly roles = [
    { label: 'Administradores', value: 'ROLE_ADMIN' },
    { label: 'Médicos', value: 'ROLE_MEDICO' },
    { label: 'Pacientes', value: 'ROLE_PACIENTE' },
  ];

  constructor() {
    this.busquedaCambia
      .pipe(debounceTime(300), takeUntilDestroyed())
      .subscribe(() => this.cargar());
  }

  protected readonly editando = signal<UsuarioAdminResponse | null>(null);
  protected readonly seleccion = signal<string[]>([]);
  protected readonly guardando = signal(false);
  protected readonly errorRoles = signal<string | null>(null);
  protected readonly descripcionRol: Readonly<Record<string, string>> = {
    ROLE_ADMIN: 'Gestiona usuarios, roles, tarifas, cupones y pagos.',
    ROLE_MEDICO: 'Atiende pacientes y administra su agenda. Requiere perfil médico.',
    ROLE_PACIENTE: 'Reserva citas y paga consultas. Requiere perfil de paciente.',
  };

  ngOnInit(): void {
    this.cargar();
    this.store.cargarRoles();
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
