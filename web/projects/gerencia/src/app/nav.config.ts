import { NavGroup } from '@tusalud/ui/shell-config';

export const NAV_GROUPS: readonly NavGroup[] = [
  {
    title: 'Principal',
    items: [
      {
        label: 'Calendario',
        icon: 'pi-calendar-plus',
        link: '/calendario',
        roles: ['ROLE_GERENCIA'],
      },
    ],
  },
  {
    title: 'Reportes',
    items: [
      {
        label: 'Ejecutivo',
        icon: 'pi-chart-line',
        link: '/reportes/ejecutivo',
        roles: ['ROLE_GERENCIA'],
      },
      {
        label: 'Financiero',
        icon: 'pi-money-bill',
        link: '/reportes/financiero',
        roles: ['ROLE_GERENCIA'],
      },
      {
        label: 'Operativo',
        icon: 'pi-sitemap',
        link: '/reportes/operativo',
        roles: ['ROLE_GERENCIA'],
      },
      {
        label: 'Calidad',
        icon: 'pi-verified',
        link: '/reportes/calidad',
        roles: ['ROLE_GERENCIA'],
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
        roles: ['ROLE_GERENCIA'],
        exact: true,
      },
      {
        label: 'Empleados',
        icon: 'pi-users',
        link: '/rrhh/empleados',
        roles: ['ROLE_GERENCIA'],
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
        roles: ['ROLE_GERENCIA'],
      },
      {
        label: 'Notificaciones',
        icon: 'pi-bell',
        link: '/notificaciones',
        roles: ['ROLE_GERENCIA'],
        badge: 'notificaciones',
      },
      {
        label: 'Mis exportaciones',
        icon: 'pi-file-export',
        link: '/mis-exportaciones',
        roles: ['ROLE_GERENCIA'],
      },
      {
        label: 'Mi perfil',
        icon: 'pi-user',
        link: '/perfil',
        roles: ['ROLE_GERENCIA'],
      },
    ],
  },
];
