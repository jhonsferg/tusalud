import { NavGroup } from '@tusalud/ui/shell-config';

export const NAV_GROUPS: readonly NavGroup[] = [
  {
    title: 'Principal',
    items: [
      {
        label: 'Calendario',
        icon: 'pi-calendar-plus',
        link: '/calendario',
        roles: ['ROLE_RRHH'],
      },
    ],
  },
  {
    title: 'Recursos humanos',
    items: [
      {
        label: 'Resumen de plantilla',
        icon: 'pi-id-card',
        link: '/rrhh',
        roles: ['ROLE_RRHH'],
        exact: true,
      },
      {
        label: 'Empleados',
        icon: 'pi-users',
        link: '/rrhh/empleados',
        roles: ['ROLE_RRHH'],
      },
      {
        label: 'Turnos',
        icon: 'pi-clock',
        link: '/rrhh/turnos',
        roles: ['ROLE_RRHH'],
      },
      {
        label: 'Ausencias',
        icon: 'pi-calendar-minus',
        link: '/rrhh/ausencias',
        roles: ['ROLE_RRHH'],
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
        roles: ['ROLE_RRHH'],
      },
      {
        label: 'Notificaciones',
        icon: 'pi-bell',
        link: '/notificaciones',
        roles: ['ROLE_RRHH'],
        badge: 'notificaciones',
      },
      {
        label: 'Mis exportaciones',
        icon: 'pi-file-export',
        link: '/mis-exportaciones',
        roles: ['ROLE_RRHH'],
      },
      {
        label: 'Mi perfil',
        icon: 'pi-user',
        link: '/perfil',
        roles: ['ROLE_RRHH'],
      },
    ],
  },
];
