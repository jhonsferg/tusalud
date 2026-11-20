import { NavGroup } from '@tusalud/ui/shell-config';

export const NAV_GROUPS: readonly NavGroup[] = [
  {
    title: 'Principal',
    items: [
      {
        label: 'Inicio',
        icon: 'pi-home',
        link: '/inicio',
        roles: ['ROLE_FINANZAS'],
        exact: true,
      },
      {
        label: 'Calendario',
        icon: 'pi-calendar-plus',
        link: '/calendario',
        roles: ['ROLE_FINANZAS'],
      },
    ],
  },
  {
    title: 'Finanzas',
    items: [
      {
        label: 'Reporte financiero',
        icon: 'pi-money-bill',
        link: '/reportes/financiero',
        roles: ['ROLE_FINANZAS'],
      },
    ],
  },
  {
    title: 'Seguros y convenios',
    items: [
      {
        label: 'Aseguradoras y convenios',
        icon: 'pi-shield',
        link: '/seguros/aseguradoras',
        roles: ['ROLE_FINANZAS'],
      },
      {
        label: 'Cartera',
        icon: 'pi-briefcase',
        link: '/seguros/cartera',
        roles: ['ROLE_FINANZAS'],
      },
      {
        label: 'Liquidaciones',
        icon: 'pi-file-edit',
        link: '/seguros/liquidaciones',
        roles: ['ROLE_FINANZAS'],
      },
      {
        label: 'Preautorizaciones',
        icon: 'pi-verified',
        link: '/seguros/preautorizaciones',
        roles: ['ROLE_FINANZAS'],
      },
    ],
  },
  {
    title: 'Facturación electrónica',
    items: [
      {
        label: 'Comprobantes electrónicos',
        icon: 'pi-verified',
        link: '/comprobantes-electronicos',
        roles: ['ROLE_FINANZAS'],
      },
      {
        label: 'Resúmenes y bajas',
        icon: 'pi-list-check',
        link: '/resumenes-sunat',
        roles: ['ROLE_FINANZAS'],
      },
      {
        label: 'Registro de ventas',
        icon: 'pi-book',
        link: '/registro-ventas',
        roles: ['ROLE_FINANZAS'],
      },
      {
        label: 'Configuración fiscal',
        icon: 'pi-cog',
        link: '/configuracion-fiscal',
        roles: ['ROLE_FINANZAS'],
      },
    ],
  },
  {
    title: 'Compras y pagos',
    items: [
      {
        label: 'Órdenes de compra',
        icon: 'pi-shopping-cart',
        link: '/compras/ordenes',
        roles: ['ROLE_FINANZAS'],
      },
      {
        label: 'Cuentas por pagar',
        icon: 'pi-wallet',
        link: '/compras/cuentas-por-pagar',
        roles: ['ROLE_FINANZAS'],
      },
      {
        label: 'Compras por proveedor',
        icon: 'pi-chart-bar',
        link: '/compras/reporte',
        roles: ['ROLE_FINANZAS'],
      },
      {
        label: 'Requisiciones',
        icon: 'pi-clipboard',
        link: '/compras/requisiciones',
        roles: ['ROLE_FINANZAS'],
      },
      {
        label: 'Recepciones',
        icon: 'pi-download',
        link: '/compras/recepciones',
        roles: ['ROLE_FINANZAS'],
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
        roles: ['ROLE_FINANZAS'],
        exact: true,
      },
      {
        label: 'Asientos',
        icon: 'pi-pencil',
        link: '/contabilidad/asientos',
        roles: ['ROLE_FINANZAS'],
      },
      {
        label: 'Libros y estados',
        icon: 'pi-chart-bar',
        link: '/contabilidad/libros',
        roles: ['ROLE_FINANZAS'],
      },
      {
        label: 'Periodos',
        icon: 'pi-calendar',
        link: '/contabilidad/periodos',
        roles: ['ROLE_FINANZAS'],
      },
      {
        label: 'Plan contable',
        icon: 'pi-list',
        link: '/contabilidad/plan-cuentas',
        roles: ['ROLE_FINANZAS'],
      },
      {
        label: 'Plantillas de asiento',
        icon: 'pi-sliders-h',
        link: '/contabilidad/plantillas',
        roles: ['ROLE_FINANZAS'],
      },
      {
        label: 'Libros electrónicos',
        icon: 'pi-file-export',
        link: '/contabilidad/libros-electronicos',
        roles: ['ROLE_FINANZAS'],
      },
    ],
  },
  {
    title: 'Tesorería',
    items: [
      {
        label: 'Cuentas y movimientos',
        icon: 'pi-wallet',
        link: '/contabilidad/tesoreria',
        roles: ['ROLE_FINANZAS'],
      },
      {
        label: 'Conciliación',
        icon: 'pi-check-square',
        link: '/contabilidad/conciliacion',
        roles: ['ROLE_FINANZAS'],
      },
      {
        label: 'Flujo de caja',
        icon: 'pi-chart-line',
        link: '/contabilidad/flujo-caja',
        roles: ['ROLE_FINANZAS'],
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
        roles: ['ROLE_FINANZAS'],
      },
      {
        label: 'Conceptos de planilla',
        icon: 'pi-list',
        link: '/conceptos-planilla',
        roles: ['ROLE_FINANZAS'],
      },
      {
        label: 'Parámetros laborales',
        icon: 'pi-sliders-h',
        link: '/parametros-planilla',
        roles: ['ROLE_FINANZAS'],
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
        roles: ['ROLE_FINANZAS'],
      },
      {
        label: 'Mis boletas',
        icon: 'pi-money-bill',
        link: '/mis-boletas',
        roles: ['ROLE_FINANZAS'],
      },
      {
        label: 'Mi asistencia',
        icon: 'pi-stopwatch',
        link: '/mi-asistencia',
        roles: ['ROLE_FINANZAS'],
      },
      {
        label: 'Notificaciones',
        icon: 'pi-bell',
        link: '/notificaciones',
        roles: ['ROLE_FINANZAS'],
        badge: 'notificaciones',
      },
      {
        label: 'Mis exportaciones',
        icon: 'pi-file-export',
        link: '/mis-exportaciones',
        roles: ['ROLE_FINANZAS'],
      },
      {
        label: 'Mi perfil',
        icon: 'pi-user',
        link: '/perfil',
        roles: ['ROLE_FINANZAS'],
      },
    ],
  },
];
