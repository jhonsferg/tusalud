import { NavGroup } from '@tusalud/ui/shell-config';

export const NAV_GROUPS: readonly NavGroup[] = [
  {
    title: 'Principal',
    items: [
      {
        label: 'Inicio',
        icon: 'pi-home',
        link: '/inicio',
        roles: ['ROLE_GERENCIA'],
        exact: true,
      },
      {
        label: 'Calendario',
        icon: 'pi-calendar-plus',
        link: '/calendario',
        roles: ['ROLE_GERENCIA'],
      },
    ],
  },
  {
    title: 'Reportes',
    items: [
      {
        label: 'Ejecutivo',
        icon: 'pi-chart-line',
        link: '/reportes/ejecutivo',
        roles: ['ROLE_GERENCIA'],
      },
      {
        label: 'Financiero',
        icon: 'pi-money-bill',
        link: '/reportes/financiero',
        roles: ['ROLE_GERENCIA'],
      },
      {
        label: 'Operativo',
        icon: 'pi-sitemap',
        link: '/reportes/operativo',
        roles: ['ROLE_GERENCIA'],
      },
      {
        label: 'Calidad',
        icon: 'pi-verified',
        link: '/reportes/calidad',
        roles: ['ROLE_GERENCIA'],
      },
      {
        label: 'Morbilidad',
        icon: 'pi-heart',
        link: '/reportes/morbilidad',
        roles: ['ROLE_GERENCIA'],
      },
    ],
  },
  {
    title: 'Recursos humanos',
    items: [
      {
        label: 'Resumen de plantilla',
        icon: 'pi-id-card',
        link: '/rrhh',
        roles: ['ROLE_GERENCIA'],
        exact: true,
      },
      {
        label: 'Empleados',
        icon: 'pi-users',
        link: '/rrhh/empleados',
        roles: ['ROLE_GERENCIA'],
      },
    ],
  },
  {
    title: 'Seguros',
    items: [
      {
        label: 'Cartera',
        icon: 'pi-briefcase',
        link: '/seguros/cartera',
        roles: ['ROLE_GERENCIA'],
      },
      {
        label: 'Liquidaciones',
        icon: 'pi-file-edit',
        link: '/seguros/liquidaciones',
        roles: ['ROLE_GERENCIA'],
      },
    ],
  },
  {
    title: 'Ventas',
    items: [
      {
        label: 'Registro de ventas',
        icon: 'pi-book',
        link: '/registro-ventas',
        roles: ['ROLE_GERENCIA'],
      },
    ],
  },
  {
    title: 'Compras',
    items: [
      {
        label: 'Órdenes de compra',
        icon: 'pi-shopping-cart',
        link: '/compras/ordenes',
        roles: ['ROLE_GERENCIA'],
      },
      {
        label: 'Cuentas por pagar',
        icon: 'pi-wallet',
        link: '/compras/cuentas-por-pagar',
        roles: ['ROLE_GERENCIA'],
      },
      {
        label: 'Compras por proveedor',
        icon: 'pi-chart-bar',
        link: '/compras/reporte',
        roles: ['ROLE_GERENCIA'],
      },
    ],
  },
  {
    title: 'Contabilidad',
    items: [
      {
        label: 'Resumen contable',
        icon: 'pi-book',
        link: '/contabilidad',
        roles: ['ROLE_GERENCIA'],
        exact: true,
      },
      {
        label: 'Libros y estados',
        icon: 'pi-chart-bar',
        link: '/contabilidad/libros',
        roles: ['ROLE_GERENCIA'],
      },
      {
        label: 'Periodos y cierre',
        icon: 'pi-calendar',
        link: '/contabilidad/periodos',
        roles: ['ROLE_GERENCIA'],
      },
      {
        label: 'Tesorería',
        icon: 'pi-wallet',
        link: '/contabilidad/tesoreria',
        roles: ['ROLE_GERENCIA'],
      },
      {
        label: 'Flujo de caja',
        icon: 'pi-chart-line',
        link: '/contabilidad/flujo-caja',
        roles: ['ROLE_GERENCIA'],
      },
    ],
  },
  {
    title: 'Planilla',
    items: [
      {
        label: 'Planilla mensual',
        icon: 'pi-wallet',
        link: '/planilla',
        roles: ['ROLE_GERENCIA'],
      },
      {
        label: 'Parámetros laborales',
        icon: 'pi-sliders-h',
        link: '/parametros-planilla',
        roles: ['ROLE_GERENCIA'],
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
        roles: ['ROLE_GERENCIA'],
      },
      {
        label: 'Mis boletas',
        icon: 'pi-money-bill',
        link: '/mis-boletas',
        roles: ['ROLE_GERENCIA'],
      },
      {
        label: 'Mi asistencia',
        icon: 'pi-stopwatch',
        link: '/mi-asistencia',
        roles: ['ROLE_GERENCIA'],
      },
      {
        label: 'Notificaciones',
        icon: 'pi-bell',
        link: '/notificaciones',
        roles: ['ROLE_GERENCIA'],
        badge: 'notificaciones',
      },
      {
        label: 'Mis exportaciones',
        icon: 'pi-file-export',
        link: '/mis-exportaciones',
        roles: ['ROLE_GERENCIA'],
      },
      {
        label: 'Mi perfil',
        icon: 'pi-user',
        link: '/perfil',
        roles: ['ROLE_GERENCIA'],
      },
    ],
  },
];
