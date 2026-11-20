import { NavGroup } from '@tusalud/ui/shell-config';

export const NAV_GROUPS: readonly NavGroup[] = [
  {
    title: 'Principal',
    items: [
      {
        label: 'Inicio',
        icon: 'pi-home',
        link: '/inicio',
        roles: ['ROLE_RECEPCION'],
        exact: true,
      },
      {
        label: 'Calendario',
        icon: 'pi-calendar-plus',
        link: '/calendario',
        roles: ['ROLE_RECEPCION'],
      },
    ],
  },
  {
    title: 'Recepción',
    items: [
      {
        label: 'Recepción',
        icon: 'pi-users',
        link: '/recepcion',
        roles: ['ROLE_RECEPCION'],
        exact: true,
      },
      {
        label: 'Pacientes',
        icon: 'pi-id-card',
        link: '/recepcion/pacientes',
        roles: ['ROLE_RECEPCION'],
      },
      {
        label: 'Reservar en mostrador',
        icon: 'pi-calendar-plus',
        link: '/recepcion/citas/nueva',
        roles: ['ROLE_RECEPCION'],
      },
      {
        label: 'Sala de espera',
        icon: 'pi-desktop',
        link: '/recepcion/pantalla',
        roles: ['ROLE_RECEPCION'],
      },
    ],
  },
  {
    title: 'Imágenes diagnósticas',
    items: [
      {
        label: 'Agenda de salas',
        icon: 'pi-calendar',
        link: '/imagenes/agenda',
        roles: ['ROLE_RECEPCION'],
      },
    ],
  },
  {
    title: 'Hospitalización',
    items: [
      {
        label: 'Mapa de camas',
        icon: 'pi-building',
        link: '/hospitalizacion',
        roles: ['ROLE_RECEPCION'],
        exact: true,
      },
      {
        label: 'Emergencias',
        icon: 'pi-exclamation-circle',
        link: '/hospitalizacion/emergencias',
        roles: ['ROLE_RECEPCION'],
      },
      {
        label: 'Ingresos',
        icon: 'pi-list',
        link: '/hospitalizacion/ingresos',
        roles: ['ROLE_RECEPCION'],
      },
    ],
  },
  {
    title: 'Seguros',
    items: [
      {
        label: 'Pólizas',
        icon: 'pi-id-card',
        link: '/seguros/polizas',
        roles: ['ROLE_RECEPCION', 'ROLE_ADMIN'],
      },
      {
        label: 'Preautorizaciones',
        icon: 'pi-verified',
        link: '/seguros/preautorizaciones',
        roles: ['ROLE_RECEPCION', 'ROLE_ADMIN'],
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
        roles: ['ROLE_RECEPCION'],
      },
      {
        label: 'Mis boletas',
        icon: 'pi-money-bill',
        link: '/mis-boletas',
        roles: ['ROLE_RECEPCION'],
      },
      {
        label: 'Mi asistencia',
        icon: 'pi-stopwatch',
        link: '/mi-asistencia',
        roles: ['ROLE_RECEPCION'],
      },
      {
        label: 'Notificaciones',
        icon: 'pi-bell',
        link: '/notificaciones',
        roles: ['ROLE_RECEPCION'],
        badge: 'notificaciones',
      },
      {
        label: 'Mi perfil',
        icon: 'pi-user',
        link: '/perfil',
        roles: ['ROLE_RECEPCION'],
      },
    ],
  },
];
