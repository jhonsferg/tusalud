import { NavGroup } from '@tusalud/ui/shell-config';

export const NAV_GROUPS: readonly NavGroup[] = [
  {
    title: 'Principal',
    items: [
      {
        label: 'Calendario',
        icon: 'pi-calendar-plus',
        link: '/calendario',
        roles: ['ROLE_CALIDAD'],
      },
    ],
  },
  {
    title: 'Calidad',
    items: [
      {
        label: 'Reporte de calidad',
        icon: 'pi-verified',
        link: '/reportes/calidad',
        roles: ['ROLE_CALIDAD'],
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
        roles: ['ROLE_CALIDAD'],
      },
      {
        label: 'Notificaciones',
        icon: 'pi-bell',
        link: '/notificaciones',
        roles: ['ROLE_CALIDAD'],
        badge: 'notificaciones',
      },
      {
        label: 'Mi perfil',
        icon: 'pi-user',
        link: '/perfil',
        roles: ['ROLE_CALIDAD'],
      },
    ],
  },
];
