import { NavGroup } from '@tusalud/ui/shell-config';

export const NAV_GROUPS: readonly NavGroup[] = [
  {
    title: 'Principal',
    items: [
      {
        label: 'Mi agenda',
        icon: 'pi-calendar',
        link: '/agenda',
        roles: ['ROLE_MEDICO'],
      },
      {
        label: 'Cola de pacientes',
        icon: 'pi-users',
        link: '/cola',
        roles: ['ROLE_MEDICO'],
      },
      {
        label: 'Calendario',
        icon: 'pi-calendar-plus',
        link: '/calendario',
        roles: ['ROLE_MEDICO'],
      },
      {
        label: 'Mis pacientes',
        icon: 'pi-users',
        link: '/pacientes',
        roles: ['ROLE_MEDICO'],
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
        roles: ['ROLE_MEDICO'],
        exact: true,
      },
      {
        label: 'Emergencias',
        icon: 'pi-exclamation-circle',
        link: '/hospitalizacion/emergencias',
        roles: ['ROLE_MEDICO'],
      },
      {
        label: 'Ingresos',
        icon: 'pi-list',
        link: '/hospitalizacion/ingresos',
        roles: ['ROLE_MEDICO'],
      },
      {
        label: 'Camas',
        icon: 'pi-th-large',
        link: '/hospitalizacion/camas',
        roles: ['ROLE_MEDICO'],
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
        roles: ['ROLE_MEDICO'],
        exact: true,
      },
      {
        label: 'Solicitar exámenes',
        icon: 'pi-send',
        link: '/laboratorio/solicitar',
        roles: ['ROLE_MEDICO'],
      },
      {
        label: 'Catálogo de exámenes',
        icon: 'pi-book',
        link: '/laboratorio/examenes',
        roles: ['ROLE_MEDICO'],
      },
    ],
  },
  {
    title: 'Imágenes diagnósticas',
    items: [
      {
        label: 'Mis estudios de imagen',
        icon: 'pi-image',
        link: '/imagenes',
        roles: ['ROLE_MEDICO'],
        exact: true,
      },
      {
        label: 'Solicitar estudio',
        icon: 'pi-send',
        link: '/imagenes/solicitar',
        roles: ['ROLE_MEDICO'],
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
        roles: ['ROLE_MEDICO'],
      },
      {
        label: 'Notificaciones',
        icon: 'pi-bell',
        link: '/notificaciones',
        roles: ['ROLE_MEDICO'],
        badge: 'notificaciones',
      },
      {
        label: 'Mis exportaciones',
        icon: 'pi-file-export',
        link: '/mis-exportaciones',
        roles: ['ROLE_MEDICO'],
      },
      {
        label: 'Mi perfil',
        icon: 'pi-user',
        link: '/perfil',
        roles: ['ROLE_MEDICO'],
      },
    ],
  },
];
