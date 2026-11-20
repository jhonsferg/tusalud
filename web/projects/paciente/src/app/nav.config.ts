import { NavGroup } from '@tusalud/ui/shell-config';

export const NAV_GROUPS: readonly NavGroup[] = [
  {
    title: 'Principal',
    items: [
      {
        label: 'Inicio',
        icon: 'pi-home',
        link: '/inicio',
        roles: ['ROLE_PACIENTE'],
        exact: true,
        inferior: true,
      },
      {
        label: 'Mis citas',
        icon: 'pi-calendar-clock',
        link: '/citas',
        roles: ['ROLE_PACIENTE'],
        exact: true,
        inferior: true,
        corta: 'Citas',
      },
      {
        label: 'Reservar cita',
        icon: 'pi-calendar-plus',
        link: '/citas/nueva',
        roles: ['ROLE_PACIENTE'],
        inferior: true,
        corta: 'Reservar',
      },
      {
        label: 'Teleconsultas',
        icon: 'pi-video',
        link: '/teleconsultas',
        roles: ['ROLE_PACIENTE'],
      },
      {
        label: 'Calendario',
        icon: 'pi-calendar',
        link: '/calendario',
        roles: ['ROLE_PACIENTE'],
      },
    ],
  },
  {
    title: 'Mi historia impresa',
    items: [
      {
        label: 'Mi plan de tratamiento',
        icon: 'pi-heart',
        link: '/mi-plan',
        roles: ['ROLE_PACIENTE'],
      },
      {
        label: 'Mi historia en PDF',
        icon: 'pi-file-pdf',
        link: '/mi-historia-impresa',
        roles: ['ROLE_PACIENTE'],
      },
    ],
  },
  {
    title: 'Mi salud',
    items: [
      {
        label: 'Historia clínica',
        icon: 'pi-folder-open',
        link: '/historia-clinica',
        roles: ['ROLE_PACIENTE'],
      },
      {
        label: 'Mis resultados',
        icon: 'pi-filter',
        link: '/mis-resultados',
        roles: ['ROLE_PACIENTE'],
        inferior: true,
        corta: 'Resultados',
      },
      {
        label: 'Mis estudios',
        icon: 'pi-image',
        link: '/mis-estudios',
        roles: ['ROLE_PACIENTE'],
      },
      {
        label: 'Mis hospitalizaciones',
        icon: 'pi-building',
        link: '/mis-ingresos',
        roles: ['ROLE_PACIENTE'],
      },
      {
        label: 'Mis medicamentos',
        icon: 'pi-heart',
        link: '/mis-medicamentos',
        roles: ['ROLE_PACIENTE'],
      },
      {
        label: 'Mis pagos',
        icon: 'pi-wallet',
        link: '/pagos',
        roles: ['ROLE_PACIENTE'],
        exact: true,
      },
      {
        label: 'Métodos de pago',
        icon: 'pi-credit-card',
        link: '/pagos/metodos',
        roles: ['ROLE_PACIENTE'],
      },
    ],
  },
  {
    title: 'Vacunas y salud dental',
    items: [
      {
        label: 'Mi carné de vacunación',
        icon: 'pi-shield',
        link: '/mi-vacunacion',
        roles: ['ROLE_PACIENTE'],
      },
      {
        label: 'Mi crecimiento',
        icon: 'pi-chart-line',
        link: '/mi-crecimiento',
        roles: ['ROLE_PACIENTE'],
      },
      {
        label: 'Mi salud dental',
        icon: 'pi-heart',
        link: '/mi-salud-dental',
        roles: ['ROLE_PACIENTE'],
      },
    ],
  },
  {
    title: 'Directorio',
    items: [
      {
        label: 'Médicos',
        icon: 'pi-user-plus',
        link: '/medicos',
        roles: ['ROLE_PACIENTE'],
      },
      {
        label: 'Especialidades',
        icon: 'pi-heart',
        link: '/especialidades',
        roles: ['ROLE_PACIENTE'],
      },
    ],
  },
  {
    title: 'Mi seguro',
    items: [
      {
        label: 'Mis seguros',
        icon: 'pi-shield',
        link: '/mis-seguros',
        roles: ['ROLE_PACIENTE'],
      },
      {
        label: 'Mis recetas',
        icon: 'pi-file-edit',
        link: '/mis-recetas',
        roles: ['ROLE_PACIENTE'],
      },
      {
        label: 'Mis documentos',
        icon: 'pi-file-check',
        link: '/mis-documentos',
        roles: ['ROLE_PACIENTE'],
      },
      {
        label: 'Mis consentimientos',
        icon: 'pi-pencil',
        link: '/mis-consentimientos',
        roles: ['ROLE_PACIENTE'],
      },
    ],
  },
  {
    title: 'Mi privacidad y pagos',
    items: [
      {
        label: 'Mis comprobantes',
        icon: 'pi-receipt',
        link: '/mis-comprobantes',
        roles: ['ROLE_PACIENTE'],
      },
      {
        label: 'Mi privacidad',
        icon: 'pi-shield',
        link: '/privacidad',
        roles: ['ROLE_PACIENTE'],
      },
    ],
  },
  {
    title: 'Mis procedimientos',
    items: [
      {
        label: 'Mis cirugías',
        icon: 'pi-calendar-plus',
        link: '/mis-cirugias',
        roles: ['ROLE_PACIENTE'],
      },
    ],
  },
  {
    title: 'Cuenta',
    items: [
      {
        label: 'Notificaciones',
        icon: 'pi-bell',
        link: '/notificaciones',
        roles: ['ROLE_PACIENTE'],
        badge: 'notificaciones',
      },
      {
        label: 'Mi perfil',
        icon: 'pi-user',
        link: '/perfil',
        roles: ['ROLE_PACIENTE'],
      },
    ],
  },
];
