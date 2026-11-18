import { NavGroup } from '@tusalud/ui/shell-config';

export const NAV_GROUPS: readonly NavGroup[] = [
  {
    title: 'Principal',
    items: [
      {
        label: 'Inicio',
        icon: 'pi-home',
        link: '/inicio',
        roles: ['ROLE_ALMACEN'],
        exact: true,
      },
      {
        label: 'Calendario',
        icon: 'pi-calendar-plus',
        link: '/calendario',
        roles: ['ROLE_ALMACEN'],
      },
    ],
  },
  {
    title: 'Almacén',
    items: [
      {
        label: 'Resumen',
        icon: 'pi-box',
        link: '/almacen',
        roles: ['ROLE_ALMACEN'],
        exact: true,
      },
      {
        label: 'Productos',
        icon: 'pi-tags',
        link: '/almacen/productos',
        roles: ['ROLE_ALMACEN'],
      },
      {
        label: 'Ingresos',
        icon: 'pi-download',
        link: '/almacen/ingresos',
        roles: ['ROLE_ALMACEN'],
      },
      {
        label: 'Proveedores',
        icon: 'pi-truck',
        link: '/almacen/proveedores',
        roles: ['ROLE_ALMACEN'],
      },
      {
        label: 'Kardex',
        icon: 'pi-arrows-v',
        link: '/almacen/movimientos',
        roles: ['ROLE_ALMACEN'],
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
        roles: ['ROLE_ALMACEN'],
      },
      {
        label: 'Notificaciones',
        icon: 'pi-bell',
        link: '/notificaciones',
        roles: ['ROLE_ALMACEN'],
        badge: 'notificaciones',
      },
      {
        label: 'Mis exportaciones',
        icon: 'pi-file-export',
        link: '/mis-exportaciones',
        roles: ['ROLE_ALMACEN'],
      },
      {
        label: 'Mi perfil',
        icon: 'pi-user',
        link: '/perfil',
        roles: ['ROLE_ALMACEN'],
      },
    ],
  },
];
