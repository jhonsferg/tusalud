import { NavGroup } from '@tusalud/ui/shell-config';

export const NAV_GROUPS: readonly NavGroup[] = [
  {
    title: 'Principal',
    items: [
      {
        label: 'Calendario',
        icon: 'pi-calendar-plus',
        link: '/calendario',
        roles: ['ROLE_FARMACIA'],
      },
    ],
  },
  {
    title: 'Farmacia',
    items: [
      {
        label: 'Farmacia',
        icon: 'pi-plus-circle',
        link: '/farmacia',
        roles: ['ROLE_FARMACIA'],
        exact: true,
      },
      {
        label: 'Dispensar',
        icon: 'pi-shopping-cart',
        link: '/farmacia/dispensar',
        roles: ['ROLE_FARMACIA'],
      },
      {
        label: 'Historial',
        icon: 'pi-history',
        link: '/farmacia/historial',
        roles: ['ROLE_FARMACIA'],
      },
    ],
  },
  {
    title: 'Almacén',
    items: [
      {
        label: 'Productos',
        icon: 'pi-tags',
        link: '/almacen/productos',
        roles: ['ROLE_FARMACIA'],
      },
      {
        label: 'Kardex',
        icon: 'pi-arrows-v',
        link: '/almacen/movimientos',
        roles: ['ROLE_FARMACIA'],
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
        roles: ['ROLE_FARMACIA'],
      },
      {
        label: 'Notificaciones',
        icon: 'pi-bell',
        link: '/notificaciones',
        roles: ['ROLE_FARMACIA'],
        badge: 'notificaciones',
      },
      {
        label: 'Mi perfil',
        icon: 'pi-user',
        link: '/perfil',
        roles: ['ROLE_FARMACIA'],
      },
    ],
  },
];
