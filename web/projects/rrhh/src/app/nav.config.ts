import { NavGroup } from '@tusalud/ui/shell-config';

export const NAV_GROUPS: readonly NavGroup[] = [
  {
    title: 'Principal',
    items: [
      {
        label: 'Inicio',
        icon: 'pi-home',
        link: '/inicio',
        roles: ['ROLE_RRHH'],
        exact: true,
      },
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
      {
        label: 'Planilla mensual',
        icon: 'pi-wallet',
        link: '/planilla',
        roles: ['ROLE_RRHH'],
      },
      {
        label: 'Contratos laborales',
        icon: 'pi-id-card',
        link: '/contratos-laborales',
        roles: ['ROLE_RRHH'],
      },
      {
        label: 'Asistencia',
        icon: 'pi-stopwatch',
        link: '/asistencia',
        roles: ['ROLE_RRHH'],
      },
      {
        label: 'Terminales de asistencia',
        icon: 'pi-tablet',
        link: '/terminales-asistencia',
        roles: ['ROLE_RRHH'],
      },
      {
        label: 'Conceptos de planilla',
        icon: 'pi-list',
        link: '/conceptos-planilla',
        roles: ['ROLE_RRHH'],
      },
      {
        label: 'Parámetros laborales',
        icon: 'pi-sliders-h',
        link: '/parametros-planilla',
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
        label: 'Mis boletas',
        icon: 'pi-money-bill',
        link: '/mis-boletas',
        roles: ['ROLE_RRHH'],
      },
      {
        label: 'Mi asistencia',
        icon: 'pi-stopwatch',
        link: '/mi-asistencia',
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
