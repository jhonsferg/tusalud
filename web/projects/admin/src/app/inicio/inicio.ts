import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import {
  AdminApiService,
  AsyncState,
  AuditoriaApiService,
  AuthService,
  CitasPorEstadoResponse,
  EstadisticasResponse,
  ETIQUETA_ROL,
  EventoSeguridadResponse,
  SeguridadOperativaStore,
  RolResumenResponse,
  UsuarioAdminResponse,
} from '@tusalud/core';
import { HeroPortada } from '@tusalud/ui/hero-portada';
import {
  AccionRapida,
  BarList,
  ItemBarra,
  ItemTarea,
  QuickActions,
  SectionCard,
  StatCard,
  StatusBadge,
  TaskList,
  TonoTablero,
  fechaLarga,
  saludoDelDia,
} from '@tusalud/ui/tablero';

const TONO_ESTADO: Readonly<Record<string, TonoTablero>> = {
  PENDIENTE: 'aviso',
  CONFIRMADA: 'info',
  ATENDIDA: 'exito',
  CANCELADA: 'neutro',
};

@Component({
  selector: 'app-inicio-admin',
  imports: [
    CurrencyPipe,
    RouterLink,
    HeroPortada,
    SectionCard,
    StatCard,
    BarList,
    TaskList,
    QuickActions,
    StatusBadge,
  ],
  template: `
    <app-hero-portada app="admin" [title]="saludo()" [subtitle]="fecha()" />

    <div class="fila-stats stats">
      <app-stat-card
        etiqueta="Usuarios activos"
        [valor]="activos().length"
        icono="pi-users"
        [detalle]="inactivos() + ' inactivos'"
        enlace="/admin/usuarios"
      />
      <app-stat-card
        etiqueta="Citas de hoy"
        [valor]="estadisticas.value()?.citasHoy ?? 0"
        icono="pi-calendar"
        [detalle]="(estadisticas.value()?.citasPendientes ?? 0) + ' pendientes'"
        enlace="/admin/auditoria"
      />
      <app-stat-card
        etiqueta="Pacientes"
        [valor]="estadisticas.value()?.totalPacientes ?? 0"
        icono="pi-id-card"
        [detalle]="(estadisticas.value()?.totalMedicos ?? 0) + ' médicos'"
      />
      <app-stat-card
        etiqueta="Ingresos del mes"
        [valor]="(estadisticas.value()?.ingresosMes ?? 0 | currency: 'PEN' : 'symbol-narrow') ?? ''"
        icono="pi-dollar"
        tono="exito"
        enlace="/admin/panel"
      />
    </div>

    <div class="tablero">
      <div class="c6">
        <app-section-card
          titulo="Accesos recientes"
          icono="pi-shield"
          subtitulo="Último acceso de las cuentas, para detectar inactividad inusual"
          [cargando]="usuarios.loading() && !usuarios.hasValue()"
          [error]="usuarios.error()?.message ?? null"
          [vacio]="recientes().length === 0 && usuarios.hasValue()"
          textoVacio="Aún no hay accesos registrados."
        >
          <app-task-list [items]="recientes()" />
          <a pie routerLink="/admin/usuarios">Gestionar usuarios</a>
        </app-section-card>
      </div>

      <div class="c6">
        <app-section-card
          titulo="Usuarios por rol"
          icono="pi-id-card"
          [vacio]="porRol().length === 0 && roles.hasValue()"
        >
          <app-bar-list [items]="porRol()" etiqueta="Usuarios por rol" />
        </app-section-card>
      </div>

      <div class="c6">
        <app-section-card
          titulo="Citas por estado"
          icono="pi-calendar"
          [vacio]="porEstado().length === 0 && citasEstado.hasValue()"
        >
          <app-bar-list [items]="porEstado()" etiqueta="Citas por estado" />
          <a pie routerLink="/admin/auditoria">Abrir la auditoría</a>
        </app-section-card>
      </div>

      <div class="c6">
        <app-section-card
          titulo="Seguridad"
          icono="pi-lock"
          subtitulo="Eventos de las últimas 24 horas y accesos inusuales de la semana"
          [cargando]="seguridad.resumen.loading() && !seguridad.resumen.hasValue()"
          [error]="seguridad.resumen.error()?.message ?? null"
        >
          @if (seguridad.resumen.value(); as resumen) {
            <div
              class="conteos"
              role="group"
              aria-label="Eventos de seguridad de las últimas 24 horas"
            >
              <div class="conteo" data-tono="peligro">
                <strong>{{ resumen.ultimas24Horas.altas }}</strong>
                <span>Alta severidad</span>
              </div>
              <div class="conteo" data-tono="aviso">
                <strong>{{ resumen.ultimas24Horas.avisos }}</strong>
                <span>Avisos</span>
              </div>
              <div class="conteo" data-tono="neutro">
                <strong>{{ resumen.ultimas24Horas.total }}</strong>
                <span>Eventos en total</span>
              </div>
            </div>
            @if (inusuales().length === 0) {
              <p class="sin-eventos">
                <i class="pi pi-check-circle" aria-hidden="true"></i> Sin inicios de sesión
                inusuales esta semana.
              </p>
            } @else {
              <app-task-list [items]="inusuales()" />
            }
          }
          <a pie routerLink="/admin/seguridad">Ver todos los eventos</a>
        </app-section-card>
      </div>

      <div class="c6">
        <app-section-card
          titulo="Estado del sistema"
          icono="pi-server"
          [subtitulo]="subtituloEstado()"
          [cargando]="seguridad.estado.loading() && !seguridad.estado.hasValue()"
          [error]="seguridad.estado.error()?.message ?? null"
        >
          @if (seguridad.estado.value(); as estado) {
            <ul class="servicios">
              @for (componente of estado.componentes; track componente.nombre) {
                <li>
                  <span>{{ componente.nombre }}</span>
                  <span class="latencia">
                    @if (componente.latenciaMs !== null && componente.latenciaMs !== undefined) {
                      {{ componente.latenciaMs }} ms
                    }
                  </span>
                  <app-status-badge
                    [etiqueta]="componente.estado === 'OK' ? 'Operativo' : 'Sin respuesta'"
                    [tono]="componente.estado === 'OK' ? 'exito' : 'peligro'"
                  />
                </li>
              }
              <li>
                <span>Migraciones de base de datos</span>
                <span class="latencia">
                  @if (estado.migraciones.disponible) {
                    {{ estado.migraciones.aplicadas }} aplicadas
                  }
                </span>
                <app-status-badge
                  [etiqueta]="
                    estado.migraciones.disponible
                      ? 'Versión ' + estado.migraciones.ultima
                      : 'Sin dato'
                  "
                  [tono]="estado.migraciones.disponible ? 'info' : 'neutro'"
                />
              </li>
            </ul>
          }
        </app-section-card>
      </div>

      <div class="c6">
        <app-section-card
          titulo="Almacenamiento"
          icono="pi-database"
          [subtitulo]="subtituloUso()"
          [cargando]="seguridad.uso.loading() && !seguridad.uso.hasValue()"
          [error]="seguridad.uso.error()?.message ?? null"
          [vacio]="usoBuckets().length === 0 && seguridad.uso.hasValue()"
          textoVacio="Aún no hay archivos guardados."
        >
          <app-bar-list [items]="usoBuckets()" etiqueta="Almacenamiento por bucket" />
        </app-section-card>
      </div>

      <div class="c12">
        <app-quick-actions [acciones]="acciones" />
      </div>
    </div>
  `,
  styles: `
    .stats {
      margin-bottom: var(--sp-4);
    }

    .conteos {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: var(--sp-2);
      margin-bottom: var(--sp-3);
    }

    .conteo {
      display: flex;
      flex-direction: column;
      padding: var(--sp-2) var(--sp-3);
      border-radius: 10px;
      background: var(--estado-neutro-fondo);

      strong {
        font-size: 1.5rem;
        font-variant-numeric: tabular-nums;
      }

      span {
        color: var(--p-text-muted-color);
        font-size: 0.8125rem;
      }

      &[data-tono='peligro'] {
        background: var(--estado-peligro-fondo);
      }

      &[data-tono='aviso'] {
        background: var(--estado-aviso-fondo);
      }
    }

    .sin-eventos {
      display: flex;
      align-items: center;
      gap: var(--sp-2);
      margin: 0;
      color: var(--p-text-muted-color);

      .pi-check-circle {
        color: var(--estado-exito);
      }
    }

    .servicios {
      display: grid;
      gap: var(--sp-2);
      margin: 0;
      padding: 0;
      list-style: none;

      li {
        display: grid;
        grid-template-columns: minmax(0, 1fr) auto auto;
        align-items: center;
        gap: var(--sp-3);
      }

      .latencia {
        color: var(--p-text-muted-color);
        font-size: 0.8125rem;
        font-variant-numeric: tabular-nums;
      }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InicioAdmin implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly adminApi = inject(AdminApiService);
  private readonly auditoriaApi = inject(AuditoriaApiService);
  protected readonly seguridad = inject(SeguridadOperativaStore);

  protected readonly estadisticas = new AsyncState<EstadisticasResponse>();
  protected readonly usuarios = new AsyncState<UsuarioAdminResponse[]>();
  protected readonly roles = new AsyncState<RolResumenResponse[]>();
  protected readonly citasEstado = new AsyncState<CitasPorEstadoResponse[]>();

  protected readonly saludo = computed(() => saludoDelDia(this.auth.user()?.nombreCompleto ?? ''));
  protected readonly fecha = computed(() => {
    const texto = fechaLarga();
    return texto.charAt(0).toUpperCase() + texto.slice(1);
  });

  protected readonly acciones: readonly AccionRapida[] = [
    {
      label: 'Usuarios y roles',
      icono: 'pi-users',
      enlace: '/admin/usuarios',
      detalle: 'Cuentas, roles y personal',
    },
    {
      label: 'Tarifas por especialidad',
      icono: 'pi-heart',
      enlace: '/especialidades',
      detalle: 'Precio de la consulta',
    },
    { label: 'Médicos', icono: 'pi-user-plus', enlace: '/medicos', detalle: 'Altas y horarios' },
    {
      label: 'Pagos y cupones',
      icono: 'pi-wallet',
      enlace: '/admin/pagos',
      detalle: 'Cobros y descuentos',
    },
    {
      label: 'Auditoría',
      icono: 'pi-shield',
      enlace: '/admin/auditoria',
      detalle: 'Citas por estado y periodo',
    },
    {
      label: 'Reporte ejecutivo',
      icono: 'pi-chart-line',
      enlace: '/reportes/ejecutivo',
      detalle: 'Indicadores del periodo',
    },
  ];

  protected readonly activos = computed(() =>
    (this.usuarios.value() ?? []).filter((u) => u.estado === 'ACTIVO'),
  );
  protected readonly inactivos = computed(
    () => (this.usuarios.value() ?? []).length - this.activos().length,
  );

  protected readonly recientes = computed<ItemTarea[]>(() =>
    [...this.activos()]
      .filter((u) => u.ultimoAcceso)
      .sort((a, b) => (b.ultimoAcceso ?? '').localeCompare(a.ultimoAcceso ?? ''))
      .slice(0, 6)
      .map((u): ItemTarea => ({
        id: u.id,
        titulo: u.nombreCompleto,
        detalle: `${u.roles.map((r) => ETIQUETA_ROL[r as keyof typeof ETIQUETA_ROL] ?? r).join(', ')} · ${new Date(u.ultimoAcceso!).toLocaleString('es-PE', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit', hour12: false })}`,
        tono: 'neutro',
        icono: 'pi-user',
        sensible: true,
      })),
  );

  protected readonly inusuales = computed<ItemTarea[]>(() =>
    (this.seguridad.resumen.value()?.iniciosInusuales ?? []).map(
      (e: EventoSeguridadResponse): ItemTarea => ({
        id: e.id,
        titulo: e.etiqueta,
        detalle: `${e.usuarioNombre ?? 'Cuenta desconocida'} · ${new Date(e.creadoEn).toLocaleString('es-PE', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit', hour12: false })}${e.ip ? ' · ' + e.ip : ''}`,
        tono: e.severidad === 'ALTA' ? 'peligro' : e.severidad === 'AVISO' ? 'aviso' : 'info',
        icono: 'pi-exclamation-triangle',
        sensible: true,
      }),
    ),
  );

  protected readonly subtituloEstado = computed(() => {
    const estado = this.seguridad.estado.value();
    if (!estado) {
      return 'Servicios de la plataforma';
    }
    const horas = Math.floor(estado.segundosEnMarcha / 3600);
    const minutos = Math.floor((estado.segundosEnMarcha % 3600) / 60);
    return `Versión ${estado.version} · en marcha ${horas > 0 ? horas + ' h ' : ''}${minutos} min`;
  });

  protected readonly subtituloUso = computed(() => {
    const uso = this.seguridad.uso.value();
    return uso
      ? `${uso.totalObjetos} archivos · ${this.formatoBytes(uso.totalBytes)}`
      : 'Uso por bucket';
  });

  protected readonly usoBuckets = computed<ItemBarra[]>(() =>
    (this.seguridad.uso.value()?.buckets ?? []).map((b): ItemBarra => ({
      etiqueta: b.bucket.charAt(0).toUpperCase() + b.bucket.slice(1),
      valor: b.bytes,
      texto: `${this.formatoBytes(b.bytes)} · ${b.objetos} archivos`,
    })),
  );

  protected readonly porRol = computed<ItemBarra[]>(() =>
    (this.roles.value() ?? [])
      .filter((r) => r.usuarios > 0)
      .sort((a, b) => b.usuarios - a.usuarios)
      .map((r): ItemBarra => ({
        etiqueta: ETIQUETA_ROL[r.nombre as keyof typeof ETIQUETA_ROL] ?? r.nombre,
        valor: r.usuarios,
      })),
  );

  protected readonly porEstado = computed<ItemBarra[]>(() =>
    (this.citasEstado.value() ?? []).map((c): ItemBarra => ({
      etiqueta: c.estado.charAt(0) + c.estado.slice(1).toLowerCase(),
      valor: c.cantidad,
      tono: TONO_ESTADO[c.estado] ?? 'neutro',
    })),
  );

  ngOnInit(): void {
    this.estadisticas.load(this.adminApi.estadisticas());
    this.usuarios.load(this.adminApi.listarUsuarios());
    this.roles.load(this.adminApi.listarRoles());
    this.citasEstado.load(this.auditoriaApi.citasPorEstado());
    this.seguridad.cargarResumen();
    this.seguridad.cargarEstado();
    this.seguridad.cargarUso();
  }

  private formatoBytes(bytes: number): string {
    if (bytes < 1024) {
      return `${bytes} B`;
    }
    if (bytes < 1024 * 1024) {
      return `${(bytes / 1024).toFixed(1)} KB`;
    }
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }
}
