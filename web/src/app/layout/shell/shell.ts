import { DatePipe, NgTemplateOutlet } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  effect,
  computed,
  inject,
  signal,
} from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Badge } from 'primeng/badge';
import { Button } from 'primeng/button';
import { Drawer } from 'primeng/drawer';
import { Popover } from 'primeng/popover';
import { Tooltip } from 'primeng/tooltip';

import {
  AuthService,
  ETIQUETA_ROL,
  NotificacionesStore,
  NotificacionResponse,
  PerfilStore,
  Rol,
  StorageService,
  ThemeMode,
  ThemeService,
  TourService,
} from '@tusalud/core';
import { COLOR_NOTIFICACION, ICONO_NOTIFICACION } from '@tusalud/ui/utils';
import { Tour } from '@tusalud/ui/tour';
import { UserAvatar } from '@tusalud/ui/user-avatar';

interface NavItem {
  readonly label: string;
  readonly icon: string;
  readonly link: string;
  readonly roles: readonly Rol[];
  readonly exact?: boolean;
  readonly badge?: 'notificaciones';
}

interface NavGroup {
  readonly title: string;
  readonly items: readonly NavItem[];
}

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

const COLLAPSE_KEY = 'tusalud.sidebar.collapsed';

@Component({
  selector: 'app-shell',
  imports: [
    DatePipe,
    NgTemplateOutlet,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    Badge,
    Button,
    Drawer,
    Popover,
    Tooltip,
    UserAvatar,
    Tour,
  ],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Shell implements OnInit {
  protected readonly auth = inject(AuthService);
  protected readonly theme = inject(ThemeService);
  protected readonly notificaciones = inject(NotificacionesStore);
  private readonly perfil = inject(PerfilStore);
  private readonly router = inject(Router);
  private readonly storage = inject(StorageService);
  protected readonly tour = inject(TourService);
  private tourOfrecido = false;

  protected readonly menuOpen = signal(false);
  protected readonly collapsed = signal(
    this.storage.read(COLLAPSE_KEY, (value): value is boolean => typeof value === 'boolean') ??
      false,
  );

  protected readonly user = this.auth.user;
  protected readonly avatar = computed(() => this.perfil.perfil.value()?.avatar ?? null);
  protected readonly rol = computed(() => ETIQUETA_ROL[this.auth.roles()[0]] ?? '');
  protected readonly correo = computed(() => this.auth.user()?.email ?? '');
  protected readonly grupos = computed(() =>
    NAV_GROUPS.map((grupo) => ({
      title: grupo.title,
      items: grupo.items.filter((item) => this.auth.hasAnyRole(item.roles)),
    })).filter((grupo) => grupo.items.length > 0),
  );

  protected readonly accesos = [
    { label: 'Mi perfil', icon: 'pi-user', link: '/perfil', tab: null },
    { label: 'Seguridad y verificación', icon: 'pi-shield', link: '/perfil', tab: 'seguridad' },
    { label: 'Apariencia', icon: 'pi-palette', link: '/perfil', tab: 'apariencia' },
    { label: 'Notificaciones', icon: 'pi-bell', link: '/notificaciones', tab: null },
  ] as const;

  protected readonly modos: readonly { valor: ThemeMode; label: string; icon: string }[] = [
    { valor: 'light', label: 'Claro', icon: 'pi-sun' },
    { valor: 'dark', label: 'Oscuro', icon: 'pi-moon' },
    { valor: 'auto', label: 'Sistema', icon: 'pi-desktop' },
  ];

  protected readonly icono = ICONO_NOTIFICACION;
  protected readonly color = COLOR_NOTIFICACION;

  constructor() {
    effect(() => {
      const preferencias = this.perfil.preferencias.value();
      if (preferencias && !preferencias.tourCompletado && !this.tourOfrecido) {
        this.tourOfrecido = true;
        setTimeout(() => this.iniciarTour(), 600);
      }
    });
  }

  ngOnInit(): void {
    this.perfil.load();
    this.perfil.cargarPreferencias();
  }

  protected iniciarTour(panel?: Popover): void {
    panel?.hide();
    this.menuOpen.set(false);
    this.tour.start(() => this.perfil.marcarTour(true).subscribe({ error: () => undefined }));
  }

  protected salirDelPanel(panel: Popover): void {
    panel.hide();
    this.salir();
  }

  protected alternarColapso(): void {
    this.collapsed.update((valor) => !valor);
    this.storage.write(COLLAPSE_KEY, this.collapsed());
  }

  protected abrirNotificaciones(evento: Event, popover: Popover): void {
    this.notificaciones.cargar();
    popover.toggle(evento);
  }

  protected abrir(notificacion: NotificacionResponse, popover: Popover): void {
    popover.hide();
    if (!notificacion.leida) {
      this.notificaciones.marcarLeida(notificacion.id).subscribe({ error: () => undefined });
    }
    if (notificacion.enlace) {
      void this.router.navigateByUrl(notificacion.enlace);
    }
  }

  protected marcarTodas(): void {
    this.notificaciones.marcarTodasLeidas().subscribe({ error: () => undefined });
  }

  protected alternarTema(modo: ThemeMode): void {
    this.theme.setMode(modo);
  }

  protected salir(): void {
    this.menuOpen.set(false);
    this.auth.logout();
  }
}
