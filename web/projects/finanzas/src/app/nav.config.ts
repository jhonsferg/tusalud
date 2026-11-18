import { NavGroup } from '@tusalud/ui/shell-config';

export const NAV_GROUPS: readonly NavGroup[] = [
  {
    title: 'Principal',
    items: [
      {
        label: 'Inicio',
        icon: 'pi-home',
        link: '/inicio',
        roles: ['ROLE_FINANZAS'],
        exact: true,
      },
      {
        label: 'Calendario',
        icon: 'pi-calendar-plus',
        link: '/calendario',
        roles: ['ROLE_FINANZAS'],
      },
    ],
  },
  {
    title: 'Finanzas',
    items: [
      {
        label: 'Reporte financiero',
        icon: 'pi-money-bill',
        link: '/reportes/financiero',
        roles: ['ROLE_FINANZAS'],
      },
    ],
  },
  {
    title: 'Seguros y convenios',
    items: [
      {
        label: 'Aseguradoras y convenios',
        icon: 'pi-shield',
        link: '/seguros/aseguradoras',
        roles: ['ROLE_FINANZAS'],
      },
      {
        label: 'Cartera',
        icon: 'pi-briefcase',
        link: '/seguros/cartera',
        roles: ['ROLE_FINANZAS'],
      },
      {
        label: 'Liquidaciones',
        icon: 'pi-file-edit',
        link: '/seguros/liquidaciones',
        roles: ['ROLE_FINANZAS'],
      },
      {
        label: 'Preautorizaciones',
        icon: 'pi-verified',
        link: '/seguros/preautorizaciones',
        roles: ['ROLE_FINANZAS'],
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
        roles: ['ROLE_FINANZAS'],
      },
      {
        label: 'Notificaciones',
        icon: 'pi-bell',
        link: '/notificaciones',
        roles: ['ROLE_FINANZAS'],
        badge: 'notificaciones',
      },
      {
        label: 'Mis exportaciones',
        icon: 'pi-file-export',
        link: '/mis-exportaciones',
        roles: ['ROLE_FINANZAS'],
      },
      {
        label: 'Mi perfil',
        icon: 'pi-user',
        link: '/perfil',
        roles: ['ROLE_FINANZAS'],
      },
    ],
  },
];
