import { NavGroup } from '@tusalud/ui/shell-config';

export const NAV_GROUPS: readonly NavGroup[] = [
  {
    title: 'Principal',
    items: [
      {
        label: 'Calendario',
        icon: 'pi-calendar-plus',
        link: '/calendario',
        roles: ['ROLE_LABORATORIO'],
      },
    ],
  },
  {
    title: 'Laboratorio',
    items: [
      {
        label: 'Órdenes',
        icon: 'pi-filter',
        link: '/laboratorio',
        roles: ['ROLE_LABORATORIO'],
        exact: true,
      },
      {
        label: 'Catálogo de exámenes',
        icon: 'pi-book',
        link: '/laboratorio/examenes',
        roles: ['ROLE_LABORATORIO'],
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
        roles: ['ROLE_LABORATORIO'],
      },
      {
        label: 'Notificaciones',
        icon: 'pi-bell',
        link: '/notificaciones',
        roles: ['ROLE_LABORATORIO'],
        badge: 'notificaciones',
      },
      {
        label: 'Mi perfil',
        icon: 'pi-user',
        link: '/perfil',
        roles: ['ROLE_LABORATORIO'],
      },
    ],
  },
];
