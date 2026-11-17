import { NavGroup } from '@tusalud/ui/shell-config';

export const NAV_GROUPS: readonly NavGroup[] = [
  {
    title: 'Principal',
    items: [
      {
        label: 'Calendario',
        icon: 'pi-calendar-plus',
        link: '/calendario',
        roles: ['ROLE_OPERACIONES'],
      },
    ],
  },
  {
    title: 'Operaciones',
    items: [
      {
        label: 'Reporte operativo',
        icon: 'pi-sitemap',
        link: '/reportes/operativo',
        roles: ['ROLE_OPERACIONES'],
      },
    ],
  },
  {
    title: 'Cuenta',
    items: [
      {
        label: 'Mi espacio',
        icon: 'pi-clock',
        link: '/mi-espacio/turnos',
        roles: ['ROLE_OPERACIONES'],
      },
      {
        label: 'Notificaciones',
        icon: 'pi-bell',
        link: '/notificaciones',
        roles: ['ROLE_OPERACIONES'],
        badge: 'notificaciones',
      },
      {
        label: 'Mi perfil',
        icon: 'pi-user',
        link: '/perfil',
        roles: ['ROLE_OPERACIONES'],
      },
    ],
  },
];
