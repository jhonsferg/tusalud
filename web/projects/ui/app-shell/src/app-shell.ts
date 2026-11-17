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
  APP_ACTUAL,
  AuthService,
  ETIQUETA_ROL,
  modulosDe,
  NotificacionesStore,
  NotificacionResponse,
  PerfilStore,
  StorageService,
  ThemeMode,
  ThemeService,
  TourService,
} from '@tusalud/core';
import { APP_SHELL_CONFIG, NavGroup, ShellAccess } from '@tusalud/ui/shell-config';
import { COLOR_NOTIFICACION, ICONO_NOTIFICACION } from '@tusalud/ui/utils';
import { Tour } from '@tusalud/ui/tour';
import { UserAvatar } from '@tusalud/ui/user-avatar';

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
  templateUrl: './app-shell.html',
  styleUrl: './app-shell.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppShell implements OnInit {
  protected readonly auth = inject(AuthService);
  protected readonly theme = inject(ThemeService);
  protected readonly notificaciones = inject(NotificacionesStore);
  private readonly perfil = inject(PerfilStore);
  private readonly router = inject(Router);
  private readonly config = inject(APP_SHELL_CONFIG);
  private readonly app = inject(APP_ACTUAL, { optional: true });
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
  protected readonly marca = this.config.brand;
  protected readonly grupos = computed(() =>
    this.config.grupos
      .map((grupo: NavGroup) => ({
        title: grupo.title,
        items: grupo.items.filter((item) => !item.roles || this.auth.hasAnyRole(item.roles)),
      }))
      .filter((grupo) => grupo.items.length > 0),
  );

  protected readonly accesos: readonly ShellAccess[] = this.config.accesos ?? [
    { label: 'Mi perfil', icon: 'pi-user', link: '/perfil', tab: null },
    { label: 'Seguridad y verificación', icon: 'pi-shield', link: '/perfil', tab: 'seguridad' },
    { label: 'Apariencia', icon: 'pi-palette', link: '/perfil', tab: 'apariencia' },
    { label: 'Notificaciones', icon: 'pi-bell', link: '/notificaciones', tab: null },
  ];

  protected readonly otrosModulos = computed(() =>
    modulosDe(this.auth.roles()).filter((modulo) => modulo.app !== this.app),
  );

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
