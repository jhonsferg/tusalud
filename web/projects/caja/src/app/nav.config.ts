import { NavGroup } from '@tusalud/ui/shell-config';

export const NAV_GROUPS: readonly NavGroup[] = [
  {
    title: 'Principal',
    items: [
      {
        label: 'Inicio',
        icon: 'pi-home',
        link: '/inicio',
        roles: ['ROLE_CAJA'],
        exact: true,
      },
      {
        label: 'Calendario',
        icon: 'pi-calendar-plus',
        link: '/calendario',
        roles: ['ROLE_CAJA'],
      },
    ],
  },
  {
    title: 'Caja',
    items: [
      {
        label: 'Caja',
        icon: 'pi-wallet',
        link: '/caja',
        roles: ['ROLE_CAJA'],
        exact: true,
      },
      {
        label: 'Cobrar',
        icon: 'pi-dollar',
        link: '/caja/cobrar',
        roles: ['ROLE_CAJA'],
      },
      {
        label: 'Comprobantes',
        icon: 'pi-receipt',
        link: '/caja/comprobantes',
        roles: ['ROLE_CAJA'],
      },
      {
        label: 'Historial de caja',
        icon: 'pi-history',
        link: '/caja/historial',
        roles: ['ROLE_CAJA'],
      },
      {
        label: 'Reporte de ingresos',
        icon: 'pi-chart-bar',
        link: '/caja/reportes',
        roles: ['ROLE_CAJA'],
      },
    ],
  },
  {
    title: 'Facturación electrónica',
    items: [
      {
        label: 'Comprobantes electrónicos',
        icon: 'pi-verified',
        link: '/comprobantes-electronicos',
        roles: ['ROLE_CAJA'],
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
        roles: ['ROLE_CAJA'],
      },
      {
        label: 'Notificaciones',
        icon: 'pi-bell',
        link: '/notificaciones',
        roles: ['ROLE_CAJA'],
        badge: 'notificaciones',
      },
      {
        label: 'Mis exportaciones',
        icon: 'pi-file-export',
        link: '/mis-exportaciones',
        roles: ['ROLE_CAJA'],
      },
      {
        label: 'Mi perfil',
        icon: 'pi-user',
        link: '/perfil',
        roles: ['ROLE_CAJA'],
      },
    ],
  },
];
