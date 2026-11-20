import { NavGroup } from '@tusalud/ui/shell-config';

export const NAV_GROUPS: readonly NavGroup[] = [
  {
    title: 'Principal',
    items: [
      {
        label: 'Inicio',
        icon: 'pi-home',
        link: '/inicio',
        roles: ['ROLE_CALIDAD'],
        exact: true,
      },
      {
        label: 'Calendario',
        icon: 'pi-calendar-plus',
        link: '/calendario',
        roles: ['ROLE_CALIDAD'],
      },
    ],
  },
  {
    title: 'Calidad',
    items: [
      {
        label: 'Reporte de calidad',
        icon: 'pi-verified',
        link: '/reportes/calidad',
        roles: ['ROLE_CALIDAD'],
      },
      {
        label: 'Morbilidad',
        icon: 'pi-heart',
        link: '/reportes/morbilidad',
        roles: ['ROLE_CALIDAD'],
      },
    ],
  },
  {
    title: 'Protección de datos',
    items: [
      {
        label: 'Solicitudes ARCO',
        icon: 'pi-inbox',
        link: '/privacidad/solicitudes',
        roles: ['ROLE_CALIDAD'],
      },
      {
        label: 'Registro de tratamientos',
        icon: 'pi-database',
        link: '/privacidad/registro',
        roles: ['ROLE_CALIDAD'],
      },
      {
        label: 'Retención de datos',
        icon: 'pi-clock',
        link: '/privacidad/retencion',
        roles: ['ROLE_CALIDAD'],
      },
      {
        label: 'Incidentes de datos',
        icon: 'pi-exclamation-triangle',
        link: '/privacidad/incidentes',
        roles: ['ROLE_CALIDAD'],
      },
      {
        label: 'Política de privacidad',
        icon: 'pi-book',
        link: '/privacidad/politica',
        roles: ['ROLE_CALIDAD'],
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
        roles: ['ROLE_CALIDAD'],
      },
      {
        label: 'Mis boletas',
        icon: 'pi-money-bill',
        link: '/mis-boletas',
        roles: ['ROLE_CALIDAD'],
      },
      {
        label: 'Mi asistencia',
        icon: 'pi-stopwatch',
        link: '/mi-asistencia',
        roles: ['ROLE_CALIDAD'],
      },
      {
        label: 'Notificaciones',
        icon: 'pi-bell',
        link: '/notificaciones',
        roles: ['ROLE_CALIDAD'],
        badge: 'notificaciones',
      },
      {
        label: 'Mis exportaciones',
        icon: 'pi-file-export',
        link: '/mis-exportaciones',
        roles: ['ROLE_CALIDAD'],
      },
      {
        label: 'Mi perfil',
        icon: 'pi-user',
        link: '/perfil',
        roles: ['ROLE_CALIDAD'],
      },
    ],
  },
];
