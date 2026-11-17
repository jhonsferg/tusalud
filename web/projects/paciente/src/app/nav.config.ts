import { NavGroup } from '@tusalud/ui/shell-config';

export const NAV_GROUPS: readonly NavGroup[] = [
  {
    title: 'Principal',
    items: [
      {
        label: 'Calendario',
        icon: 'pi-calendar-plus',
        link: '/calendario',
        roles: ['ROLE_PACIENTE'],
      },
      {
        label: 'Mis citas',
        icon: 'pi-calendar-clock',
        link: '/citas',
        roles: ['ROLE_PACIENTE'],
        exact: true,
      },
      {
        label: 'Reservar cita',
        icon: 'pi-calendar-plus',
        link: '/citas/nueva',
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
