import { NavGroup } from '@tusalud/ui/shell-config';

export const NAV_GROUPS: readonly NavGroup[] = [
  {
    title: 'Principal',
    items: [
      {
        label: 'Inicio',
        icon: 'pi-home',
        link: '/inicio',
        roles: ['ROLE_MEDICO'],
        exact: true,
      },
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
        label: 'Teleconsultas',
        icon: 'pi-video',
        link: '/teleconsultas',
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
    title: 'Calidad y seguridad',
    items: [
      {
        label: 'Guías clínicas',
        icon: 'pi-book',
        link: '/guias',
        roles: ['ROLE_MEDICO'],
      },
      {
        label: 'Pacientes crónicos',
        icon: 'pi-heart',
        link: '/pacientes-cronicos',
        roles: ['ROLE_MEDICO'],
      },
      {
        label: 'Notificar un evento',
        icon: 'pi-exclamation-triangle',
        link: '/eventos-seguridad',
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
    title: 'Seguros',
    items: [
      {
        label: 'Preautorizaciones',
        icon: 'pi-verified',
        link: '/preautorizaciones',
        roles: ['ROLE_MEDICO'],
      },
      {
        label: 'Recetas electrónicas',
        icon: 'pi-file-edit',
        link: '/recetas',
        roles: ['ROLE_MEDICO'],
      },
      {
        label: 'Documentos médicos',
        icon: 'pi-file-check',
        link: '/documentos',
        roles: ['ROLE_MEDICO'],
      },
      {
        label: 'Consentimientos',
        icon: 'pi-pencil',
        link: '/consentimientos',
        roles: ['ROLE_MEDICO'],
      },
    ],
  },
  {
    title: 'Especialidades',
    items: [
      {
        label: 'Vacunación',
        icon: 'pi-shield',
        link: '/vacunacion',
        roles: ['ROLE_MEDICO'],
      },
      {
        label: 'Crecimiento',
        icon: 'pi-chart-line',
        link: '/crecimiento',
        roles: ['ROLE_MEDICO'],
      },
      {
        label: 'Odontograma',
        icon: 'pi-heart',
        link: '/odontograma',
        roles: ['ROLE_MEDICO'],
      },
      {
        label: 'Planes odontológicos',
        icon: 'pi-list-check',
        link: '/planes-odontologicos',
        roles: ['ROLE_MEDICO'],
      },
    ],
  },
  {
    title: 'Quirófano',
    items: [
      {
        label: 'Agenda quirúrgica',
        icon: 'pi-calendar-plus',
        link: '/quirofano',
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
        label: 'Mis boletas',
        icon: 'pi-money-bill',
        link: '/mis-boletas',
        roles: ['ROLE_MEDICO'],
      },
      {
        label: 'Mi asistencia',
        icon: 'pi-stopwatch',
        link: '/mi-asistencia',
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
