import { NavGroup } from '@tusalud/ui/shell-config';
import { Rol } from '@tusalud/core';

const TODOS: readonly Rol[] = [
  'ROLE_ADMIN',
  'ROLE_MEDICO',
  'ROLE_PACIENTE',
  'ROLE_LABORATORIO',
  'ROLE_FARMACIA',
  'ROLE_ALMACEN',
  'ROLE_RECEPCION',
  'ROLE_CAJA',
  'ROLE_ENFERMERIA',
];
const GESTION_CLINICA: readonly Rol[] = ['ROLE_ADMIN', 'ROLE_LABORATORIO', 'ROLE_MEDICO'];

const NAV_GROUPS: readonly NavGroup[] = [
  {
    title: 'Principal',
    items: [
      { label: 'Panel', icon: 'pi-th-large', link: '/admin/panel', roles: ['ROLE_ADMIN'] },
      { label: 'Auditoría', icon: 'pi-chart-bar', link: '/admin/auditoria', roles: ['ROLE_ADMIN'] },
      { label: 'Mi agenda', icon: 'pi-calendar', link: '/agenda', roles: ['ROLE_MEDICO'] },
      { label: 'Cola de pacientes', icon: 'pi-users', link: '/cola', roles: ['ROLE_MEDICO'] },
      { label: 'Calendario', icon: 'pi-calendar-plus', link: '/calendario', roles: TODOS },
      { label: 'Mis pacientes', icon: 'pi-users', link: '/pacientes', roles: ['ROLE_MEDICO'] },
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
    title: 'Recepción',
    items: [
      {
        label: 'Recepción',
        icon: 'pi-users',
        link: '/recepcion',
        roles: ['ROLE_ADMIN', 'ROLE_RECEPCION'],
        exact: true,
      },
      {
        label: 'Pacientes',
        icon: 'pi-id-card',
        link: '/recepcion/pacientes',
        roles: ['ROLE_ADMIN', 'ROLE_RECEPCION'],
      },
      {
        label: 'Reservar en mostrador',
        icon: 'pi-calendar-plus',
        link: '/recepcion/citas/nueva',
        roles: ['ROLE_ADMIN', 'ROLE_RECEPCION'],
      },
      {
        label: 'Sala de espera',
        icon: 'pi-desktop',
        link: '/recepcion/pantalla',
        roles: ['ROLE_ADMIN', 'ROLE_RECEPCION'],
      },
    ],
  },
  {
    title: 'Caja',
    items: [
      { label: 'Caja', icon: 'pi-wallet', link: '/caja', roles: ['ROLE_CAJA'], exact: true },
      { label: 'Cobrar', icon: 'pi-dollar', link: '/caja/cobrar', roles: ['ROLE_CAJA'] },
      {
        label: 'Comprobantes',
        icon: 'pi-receipt',
        link: '/caja/comprobantes',
        roles: ['ROLE_CAJA', 'ROLE_ADMIN'],
      },
      {
        label: 'Historial de caja',
        icon: 'pi-history',
        link: '/caja/historial',
        roles: ['ROLE_CAJA', 'ROLE_ADMIN'],
      },
      {
        label: 'Reporte de ingresos',
        icon: 'pi-chart-bar',
        link: '/caja/reportes',
        roles: ['ROLE_CAJA', 'ROLE_ADMIN'],
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
        roles: ['ROLE_ENFERMERIA', 'ROLE_MEDICO', 'ROLE_ADMIN', 'ROLE_RECEPCION'],
        exact: true,
      },
      {
        label: 'Emergencias',
        icon: 'pi-exclamation-circle',
        link: '/hospitalizacion/emergencias',
        roles: ['ROLE_ENFERMERIA', 'ROLE_MEDICO', 'ROLE_ADMIN', 'ROLE_RECEPCION'],
      },
      {
        label: 'Ingresos',
        icon: 'pi-list',
        link: '/hospitalizacion/ingresos',
        roles: ['ROLE_ENFERMERIA', 'ROLE_MEDICO', 'ROLE_ADMIN', 'ROLE_RECEPCION'],
      },
      {
        label: 'Camas',
        icon: 'pi-th-large',
        link: '/hospitalizacion/camas',
        roles: ['ROLE_ENFERMERIA', 'ROLE_MEDICO', 'ROLE_ADMIN'],
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
        roles: GESTION_CLINICA,
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
        roles: GESTION_CLINICA,
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
        roles: ['ROLE_ADMIN', 'ROLE_FARMACIA'],
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
        roles: ['ROLE_ADMIN', 'ROLE_ALMACEN'],
        exact: true,
      },
      {
        label: 'Productos',
        icon: 'pi-tags',
        link: '/almacen/productos',
        roles: ['ROLE_ADMIN', 'ROLE_ALMACEN', 'ROLE_FARMACIA'],
      },
      {
        label: 'Ingresos',
        icon: 'pi-download',
        link: '/almacen/ingresos',
        roles: ['ROLE_ADMIN', 'ROLE_ALMACEN'],
      },
      {
        label: 'Proveedores',
        icon: 'pi-truck',
        link: '/almacen/proveedores',
        roles: ['ROLE_ADMIN', 'ROLE_ALMACEN'],
      },
      {
        label: 'Kardex',
        icon: 'pi-arrows-v',
        link: '/almacen/movimientos',
        roles: ['ROLE_ADMIN', 'ROLE_ALMACEN', 'ROLE_FARMACIA'],
      },
    ],
  },
  {
    title: 'Gestión',
    items: [
      { label: 'Usuarios', icon: 'pi-id-card', link: '/admin/usuarios', roles: ['ROLE_ADMIN'] },
      { label: 'Pagos', icon: 'pi-wallet', link: '/admin/pagos', roles: ['ROLE_ADMIN'] },
      { label: 'Cupones', icon: 'pi-ticket', link: '/admin/cupones', roles: ['ROLE_ADMIN'] },
    ],
  },
  {
    title: 'Directorio',
    items: [
      {
        label: 'Médicos',
        icon: 'pi-user-plus',
        link: '/medicos',
        roles: ['ROLE_ADMIN', 'ROLE_PACIENTE'],
      },
      {
        label: 'Especialidades',
        icon: 'pi-heart',
        link: '/especialidades',
        roles: ['ROLE_ADMIN', 'ROLE_PACIENTE'],
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
        roles: TODOS,
        badge: 'notificaciones',
      },
      { label: 'Mi perfil', icon: 'pi-user', link: '/perfil', roles: TODOS },
    ],
  },
];

export { NAV_GROUPS };
