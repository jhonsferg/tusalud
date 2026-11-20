import { NavGroup } from '@tusalud/ui/shell-config';

export const NAV_GROUPS: readonly NavGroup[] = [
  {
    title: 'Principal',
    items: [
      {
        label: 'Inicio',
        icon: 'pi-home',
        link: '/inicio',
        roles: ['ROLE_IMAGENES'],
        exact: true,
      },
      {
        label: 'Calendario',
        icon: 'pi-calendar-plus',
        link: '/calendario',
        roles: ['ROLE_IMAGENES'],
      },
    ],
  },
  {
    title: 'Imágenes diagnósticas',
    items: [
      {
        label: 'Órdenes',
        icon: 'pi-image',
        link: '/imagenes',
        roles: ['ROLE_IMAGENES'],
        exact: true,
      },
      {
        label: 'Agenda de salas',
        icon: 'pi-calendar',
        link: '/imagenes/agenda',
        roles: ['ROLE_IMAGENES'],
      },
      {
        label: 'Catálogo de estudios',
        icon: 'pi-book',
        link: '/imagenes/catalogo',
        roles: ['ROLE_IMAGENES'],
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
        roles: ['ROLE_IMAGENES'],
      },
      {
        label: 'Mis boletas',
        icon: 'pi-money-bill',
        link: '/mis-boletas',
        roles: ['ROLE_IMAGENES'],
      },
      {
        label: 'Mi asistencia',
        icon: 'pi-stopwatch',
        link: '/mi-asistencia',
        roles: ['ROLE_IMAGENES'],
      },
      {
        label: 'Notificaciones',
        icon: 'pi-bell',
        link: '/notificaciones',
        roles: ['ROLE_IMAGENES'],
        badge: 'notificaciones',
      },
      {
        label: 'Mi perfil',
        icon: 'pi-user',
        link: '/perfil',
        roles: ['ROLE_IMAGENES'],
      },
    ],
  },
];
