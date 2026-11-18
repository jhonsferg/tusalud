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
  RolResumenResponse,
  UsuarioAdminResponse,
} from '@tusalud/core';
import { PageHeader } from '@tusalud/ui/page-header';
import {
  AccionRapida,
  BarList,
  ItemBarra,
  ItemTarea,
  QuickActions,
  SectionCard,
  StatCard,
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
    PageHeader,
    SectionCard,
    StatCard,
    BarList,
    TaskList,
    QuickActions,
  ],
  template: `
    <app-page-header [title]="saludo()" [subtitle]="fecha()" icon="pi-cog" />

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
          subtitulo="Estado de las protecciones activas"
        >
          <ul class="seguridad">
            <li>
              <i class="pi pi-check-circle" aria-hidden="true"></i> Bloqueo por inactividad y modo
              privacidad en todas las aplicaciones
            </li>
            <li>
              <i class="pi pi-check-circle" aria-hidden="true"></i> Acceso a datos clínicos solo con
              relación asistencial
            </li>
            <li>
              <i class="pi pi-check-circle" aria-hidden="true"></i> Consultas de historias ajenas
              auditadas
            </li>
            <li>
              <i class="pi pi-info-circle" aria-hidden="true"></i> Eventos de inicio de sesión
              inusuales y estado de servicios: requieren un endpoint del backend
            </li>
          </ul>
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

    .seguridad {
      display: grid;
      gap: var(--sp-2);
      margin: 0;
      padding: 0;
      list-style: none;

      li {
        display: flex;
        align-items: flex-start;
        gap: var(--sp-2);
      }

      .pi-check-circle {
        margin-top: 0.2rem;
        color: var(--estado-exito);
      }

      .pi-info-circle {
        margin-top: 0.2rem;
        color: var(--estado-info);
      }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InicioAdmin implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly adminApi = inject(AdminApiService);
  private readonly auditoriaApi = inject(AuditoriaApiService);

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
  }
}
