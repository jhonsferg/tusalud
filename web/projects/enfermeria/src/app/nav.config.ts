import { NavGroup } from '@tusalud/ui/shell-config';

export const NAV_GROUPS: readonly NavGroup[] = [
  {
    title: 'Principal',
    items: [
      {
        label: 'Inicio',
        icon: 'pi-home',
        link: '/inicio',
        roles: ['ROLE_ENFERMERIA'],
        exact: true,
      },
      {
        label: 'Calendario',
        icon: 'pi-calendar-plus',
        link: '/calendario',
        roles: ['ROLE_ENFERMERIA'],
      },
    ],
  },
  {
    title: 'Calidad y seguridad',
    items: [
      {
        label: 'Guías clínicas',
        icon: 'pi-book',
        link: '/guias',
        roles: ['ROLE_ENFERMERIA'],
      },
      {
        label: 'Notificar un evento',
        icon: 'pi-exclamation-triangle',
        link: '/eventos-seguridad',
        roles: ['ROLE_ENFERMERIA'],
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
        roles: ['ROLE_ENFERMERIA'],
        exact: true,
      },
      {
        label: 'Emergencias',
        icon: 'pi-exclamation-circle',
        link: '/hospitalizacion/emergencias',
        roles: ['ROLE_ENFERMERIA'],
      },
      {
        label: 'Ingresos',
        icon: 'pi-list',
        link: '/hospitalizacion/ingresos',
        roles: ['ROLE_ENFERMERIA'],
      },
      {
        label: 'Camas',
        icon: 'pi-th-large',
        link: '/hospitalizacion/camas',
        roles: ['ROLE_ENFERMERIA'],
      },
    ],
  },
  {
    title: 'Vacunación y crecimiento',
    items: [
      {
        label: 'Vacunación',
        icon: 'pi-shield',
        link: '/vacunacion',
        roles: ['ROLE_ENFERMERIA'],
      },
      {
        label: 'Crecimiento',
        icon: 'pi-chart-line',
        link: '/crecimiento',
        roles: ['ROLE_ENFERMERIA'],
      },
    ],
  },
  {
    title: 'Quirófano y esterilización',
    items: [
      {
        label: 'Agenda quirúrgica',
        icon: 'pi-calendar-plus',
        link: '/quirofano',
        roles: ['ROLE_ENFERMERIA'],
      },
      {
        label: 'Esterilización',
        icon: 'pi-shield',
        link: '/esterilizacion',
        roles: ['ROLE_ENFERMERIA'],
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
        roles: ['ROLE_ENFERMERIA'],
      },
      {
        label: 'Mis boletas',
        icon: 'pi-money-bill',
        link: '/mis-boletas',
        roles: ['ROLE_ENFERMERIA'],
      },
      {
        label: 'Mi asistencia',
        icon: 'pi-stopwatch',
        link: '/mi-asistencia',
        roles: ['ROLE_ENFERMERIA'],
      },
      {
        label: 'Notificaciones',
        icon: 'pi-bell',
        link: '/notificaciones',
        roles: ['ROLE_ENFERMERIA'],
        badge: 'notificaciones',
      },
      {
        label: 'Mi perfil',
        icon: 'pi-user',
        link: '/perfil',
        roles: ['ROLE_ENFERMERIA'],
      },
    ],
  },
];
