import { NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Avatar } from 'primeng/avatar';
import { Button } from 'primeng/button';
import { Drawer } from 'primeng/drawer';
import { Tooltip } from 'primeng/tooltip';

import { Rol } from '@tusalud/app/core/models';
import { AuthService } from '@tusalud/app/core/services/auth.service';

interface NavItem {
  readonly label: string;
  readonly icon: string;
  readonly link: string;
  readonly roles: readonly Rol[];
  readonly exact?: boolean;
}

const NAV_ITEMS: readonly NavItem[] = [
  { label: 'Auditoría', icon: 'pi-chart-bar', link: '/admin/auditoria', roles: ['ROLE_ADMIN'] },
  { label: 'Mi agenda', icon: 'pi-calendar', link: '/agenda', roles: ['ROLE_MEDICO'] },
  {
    label: 'Mis citas',
    icon: 'pi-calendar-clock',
    link: '/citas',
    roles: ['ROLE_PACIENTE'],
    exact: true,
  },
  {
    label: 'Reservar cita',
    icon: 'pi-calendar-plus',
    link: '/citas/nueva',
    roles: ['ROLE_PACIENTE'],
  },
  {
    label: 'Médicos',
    icon: 'pi-users',
    link: '/medicos',
    roles: ['ROLE_ADMIN', 'ROLE_MEDICO', 'ROLE_PACIENTE'],
  },
  {
    label: 'Especialidades',
    icon: 'pi-heart',
    link: '/especialidades',
    roles: ['ROLE_ADMIN', 'ROLE_MEDICO', 'ROLE_PACIENTE'],
  },
];

const ETIQUETA_ROL: Readonly<Record<Rol, string>> = {
  ROLE_ADMIN: 'Administrador',
  ROLE_MEDICO: 'Médico',
  ROLE_PACIENTE: 'Paciente',
};

@Component({
  selector: 'app-shell',
  imports: [
    NgTemplateOutlet,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    Avatar,
    Button,
    Drawer,
    Tooltip,
  ],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Shell {
  protected readonly auth = inject(AuthService);
  protected readonly menuOpen = signal(false);

  protected readonly items = computed(() =>
    NAV_ITEMS.filter((item) => this.auth.hasAnyRole(item.roles)),
  );
  protected readonly user = this.auth.user;
  protected readonly rol = computed(() => ETIQUETA_ROL[this.auth.roles()[0]] ?? '');
  protected readonly iniciales = computed(() =>
    (this.user()?.nombreCompleto ?? '')
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map((parte) => parte[0].toUpperCase())
      .join(''),
  );

  protected salir(): void {
    this.menuOpen.set(false);
    this.auth.logout();
  }
}
