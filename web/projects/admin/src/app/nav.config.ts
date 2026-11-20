import { NavGroup } from '@tusalud/ui/shell-config';

export const NAV_GROUPS: readonly NavGroup[] = [
  {
    title: 'Principal',
    items: [
      {
        label: 'Inicio',
        icon: 'pi-home',
        link: '/inicio',
        roles: ['ROLE_ADMIN'],
        exact: true,
      },
      {
        label: 'Panel',
        icon: 'pi-th-large',
        link: '/admin/panel',
        roles: ['ROLE_ADMIN'],
      },
      {
        label: 'Auditoría',
        icon: 'pi-chart-bar',
        link: '/admin/auditoria',
        roles: ['ROLE_ADMIN'],
      },
      {
        label: 'Seguridad',
        icon: 'pi-shield',
        link: '/admin/seguridad',
        roles: ['ROLE_ADMIN'],
      },
      {
        label: 'Calendario',
        icon: 'pi-calendar-plus',
        link: '/calendario',
        roles: ['ROLE_ADMIN'],
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
        roles: ['ROLE_ADMIN'],
        exact: true,
      },
      {
        label: 'Pacientes',
        icon: 'pi-id-card',
        link: '/recepcion/pacientes',
        roles: ['ROLE_ADMIN'],
      },
      {
        label: 'Reservar en mostrador',
        icon: 'pi-calendar-plus',
        link: '/recepcion/citas/nueva',
        roles: ['ROLE_ADMIN'],
      },
      {
        label: 'Sala de espera',
        icon: 'pi-desktop',
        link: '/recepcion/pantalla',
        roles: ['ROLE_ADMIN'],
      },
    ],
  },
  {
    title: 'Caja',
    items: [
      {
        label: 'Comprobantes',
        icon: 'pi-receipt',
        link: '/caja/comprobantes',
        roles: ['ROLE_ADMIN'],
      },
      {
        label: 'Historial de caja',
        icon: 'pi-history',
        link: '/caja/historial',
        roles: ['ROLE_ADMIN'],
      },
      {
        label: 'Reporte de ingresos',
        icon: 'pi-chart-bar',
        link: '/caja/reportes',
        roles: ['ROLE_ADMIN'],
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
        roles: ['ROLE_ADMIN'],
      },
      {
        label: 'Catálogo de estudios',
        icon: 'pi-book',
        link: '/imagenes/catalogo',
        roles: ['ROLE_ADMIN'],
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
        roles: ['ROLE_ADMIN'],
        exact: true,
      },
      {
        label: 'Emergencias',
        icon: 'pi-exclamation-circle',
        link: '/hospitalizacion/emergencias',
        roles: ['ROLE_ADMIN'],
      },
      {
        label: 'Ingresos',
        icon: 'pi-list',
        link: '/hospitalizacion/ingresos',
        roles: ['ROLE_ADMIN'],
      },
      {
        label: 'Camas',
        icon: 'pi-th-large',
        link: '/hospitalizacion/camas',
        roles: ['ROLE_ADMIN'],
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
        roles: ['ROLE_ADMIN'],
        exact: true,
      },
      {
        label: 'Catálogo de exámenes',
        icon: 'pi-book',
        link: '/laboratorio/examenes',
        roles: ['ROLE_ADMIN'],
      },
    ],
  },
  {
    title: 'Farmacia',
    items: [
      {
        label: 'Historial',
        icon: 'pi-history',
        link: '/farmacia/historial',
        roles: ['ROLE_ADMIN'],
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
        roles: ['ROLE_ADMIN'],
        exact: true,
      },
      {
        label: 'Productos',
        icon: 'pi-tags',
        link: '/almacen/productos',
        roles: ['ROLE_ADMIN'],
      },
      {
        label: 'Ingresos',
        icon: 'pi-download',
        link: '/almacen/ingresos',
        roles: ['ROLE_ADMIN'],
      },
      {
        label: 'Proveedores',
        icon: 'pi-truck',
        link: '/almacen/proveedores',
        roles: ['ROLE_ADMIN'],
      },
      {
        label: 'Kardex',
        icon: 'pi-arrows-v',
        link: '/almacen/movimientos',
        roles: ['ROLE_ADMIN'],
      },
    ],
  },
  {
    title: 'Gestión',
    items: [
      {
        label: 'Usuarios',
        icon: 'pi-id-card',
        link: '/admin/usuarios',
        roles: ['ROLE_ADMIN'],
      },
      {
        label: 'Pagos',
        icon: 'pi-wallet',
        link: '/admin/pagos',
        roles: ['ROLE_ADMIN'],
      },
      {
        label: 'Catálogo CIE-10',
        icon: 'pi-book',
        link: '/cie10',
        roles: ['ROLE_ADMIN'],
      },
      {
        label: 'Esquemas de vacunación',
        icon: 'pi-shield',
        link: '/esquemas-vacunacion',
        roles: ['ROLE_ADMIN'],
      },
      {
        label: 'Procedimientos odontológicos',
        icon: 'pi-list',
        link: '/procedimientos-odontologicos',
        roles: ['ROLE_ADMIN'],
      },
      {
        label: 'Cupones',
        icon: 'pi-ticket',
        link: '/admin/cupones',
        roles: ['ROLE_ADMIN'],
      },
    ],
  },
  {
    title: 'Documentos',
    items: [
      {
        label: 'Plantillas de documentos',
        icon: 'pi-file-edit',
        link: '/plantillas',
        roles: ['ROLE_ADMIN'],
      },
      {
        label: 'Establecimientos de salud',
        icon: 'pi-building',
        link: '/establecimientos',
        roles: ['ROLE_ADMIN'],
      },
      {
        label: 'Plantillas de consentimiento',
        icon: 'pi-pencil',
        link: '/consentimientos',
        roles: ['ROLE_ADMIN'],
      },
    ],
  },
  {
    title: 'Comunicaciones y respaldos',
    items: [
      {
        label: 'Plantillas de mensajes',
        icon: 'pi-envelope',
        link: '/canales/plantillas',
        roles: ['ROLE_ADMIN'],
      },
      {
        label: 'Entregas de avisos',
        icon: 'pi-send',
        link: '/canales/entregas',
        roles: ['ROLE_ADMIN'],
      },
      {
        label: 'Respaldos',
        icon: 'pi-database',
        link: '/respaldos',
        roles: ['ROLE_ADMIN'],
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
        roles: ['ROLE_ADMIN'],
      },
      {
        label: 'Especialidades',
        icon: 'pi-heart',
        link: '/especialidades',
        roles: ['ROLE_ADMIN'],
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
        roles: ['ROLE_ADMIN'],
      },
      {
        label: 'Financiero',
        icon: 'pi-money-bill',
        link: '/reportes/financiero',
        roles: ['ROLE_ADMIN'],
      },
      {
        label: 'Operativo',
        icon: 'pi-sitemap',
        link: '/reportes/operativo',
        roles: ['ROLE_ADMIN'],
      },
      {
        label: 'Calidad',
        icon: 'pi-verified',
        link: '/reportes/calidad',
        roles: ['ROLE_ADMIN'],
      },
      {
        label: 'Morbilidad',
        icon: 'pi-heart',
        link: '/reportes/morbilidad',
        roles: ['ROLE_ADMIN'],
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
        roles: ['ROLE_ADMIN'],
        exact: true,
      },
      {
        label: 'Empleados',
        icon: 'pi-users',
        link: '/rrhh/empleados',
        roles: ['ROLE_ADMIN'],
      },
      {
        label: 'Turnos',
        icon: 'pi-clock',
        link: '/rrhh/turnos',
        roles: ['ROLE_ADMIN'],
      },
      {
        label: 'Ausencias',
        icon: 'pi-calendar-minus',
        link: '/rrhh/ausencias',
        roles: ['ROLE_ADMIN'],
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
        roles: ['ROLE_ADMIN'],
      },
      {
        label: 'Cartera',
        icon: 'pi-briefcase',
        link: '/seguros/cartera',
        roles: ['ROLE_ADMIN'],
      },
      {
        label: 'Liquidaciones',
        icon: 'pi-file-edit',
        link: '/seguros/liquidaciones',
        roles: ['ROLE_ADMIN'],
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
        roles: ['ROLE_ADMIN'],
      },
      {
        label: 'Resúmenes y bajas',
        icon: 'pi-list-check',
        link: '/resumenes-sunat',
        roles: ['ROLE_ADMIN'],
      },
      {
        label: 'Registro de ventas',
        icon: 'pi-book',
        link: '/registro-ventas',
        roles: ['ROLE_ADMIN'],
      },
      {
        label: 'Configuración fiscal',
        icon: 'pi-cog',
        link: '/configuracion-fiscal',
        roles: ['ROLE_ADMIN'],
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
        roles: ['ROLE_ADMIN'],
      },
      {
        label: 'Registro de tratamientos',
        icon: 'pi-database',
        link: '/privacidad/registro',
        roles: ['ROLE_ADMIN'],
      },
      {
        label: 'Retención de datos',
        icon: 'pi-clock',
        link: '/privacidad/retencion',
        roles: ['ROLE_ADMIN'],
      },
      {
        label: 'Incidentes de datos',
        icon: 'pi-exclamation-triangle',
        link: '/privacidad/incidentes',
        roles: ['ROLE_ADMIN'],
      },
      {
        label: 'Política de privacidad',
        icon: 'pi-book',
        link: '/privacidad/politica',
        roles: ['ROLE_ADMIN'],
      },
    ],
  },
  {
    title: 'Compras y pagos',
    items: [
      {
        label: 'Requisiciones',
        icon: 'pi-clipboard',
        link: '/compras/requisiciones',
        roles: ['ROLE_ADMIN'],
      },
      {
        label: 'Órdenes de compra',
        icon: 'pi-shopping-cart',
        link: '/compras/ordenes',
        roles: ['ROLE_ADMIN'],
      },
      {
        label: 'Recepciones',
        icon: 'pi-download',
        link: '/compras/recepciones',
        roles: ['ROLE_ADMIN'],
      },
      {
        label: 'Cuentas por pagar',
        icon: 'pi-wallet',
        link: '/compras/cuentas-por-pagar',
        roles: ['ROLE_ADMIN'],
      },
      {
        label: 'Compras por proveedor',
        icon: 'pi-chart-bar',
        link: '/compras/reporte',
        roles: ['ROLE_ADMIN'],
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
        roles: ['ROLE_ADMIN'],
      },
      {
        label: 'Notificaciones',
        icon: 'pi-bell',
        link: '/notificaciones',
        roles: ['ROLE_ADMIN'],
        badge: 'notificaciones',
      },
      {
        label: 'Mis exportaciones',
        icon: 'pi-file-export',
        link: '/mis-exportaciones',
        roles: ['ROLE_ADMIN'],
      },
      {
        label: 'Mi perfil',
        icon: 'pi-user',
        link: '/perfil',
        roles: ['ROLE_ADMIN'],
      },
    ],
  },
];
