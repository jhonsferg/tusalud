import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { tap } from 'rxjs';
import { Button } from 'primeng/button';

import {
  AsyncState,
  AuthService,
  CajaApiService,
  ETIQUETA_METODO_CAJA,
  ETIQUETA_ORIGEN,
  PendienteCobroResponse,
  ResumenCajaResponse,
  SesionCajaResponse,
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
  fechaLarga,
  saludoDelDia,
} from '@tusalud/ui/tablero';

@Component({
  selector: 'app-inicio-caja',
  imports: [
    CurrencyPipe,
    DatePipe,
    RouterLink,
    Button,
    HeroPortada,
    SectionCard,
    StatCard,
    StatusBadge,
    TaskList,
    BarList,
    QuickActions,
  ],
  template: `
    <app-hero-portada app="caja" [title]="saludo()" [subtitle]="fecha()" />

    <div class="estado-caja" [class.abierta]="abierta()">
      <div class="info">
        <app-status-badge
          [etiqueta]="abierta() ? 'Caja abierta' : 'Caja cerrada'"
          [tono]="abierta() ? 'exito' : 'neutro'"
          [icono]="abierta() ? 'pi-lock-open' : 'pi-lock'"
        />
        @if (sesion.value(); as s) {
          <span class="detalle">
            Abierta el {{ s.apertura | date: 'd MMM, HH:mm' }} con fondo de
            {{ s.montoApertura | currency: 'PEN' : 'symbol-narrow' }}
          </span>
        } @else {
          <span class="detalle">Abre tu caja para empezar a cobrar.</span>
        }
      </div>
      <div class="acciones">
        @if (abierta()) {
          <p-button label="Cobrar" icon="pi pi-dollar" routerLink="/caja/cobrar" />
          <p-button
            label="Cerrar caja"
            icon="pi pi-lock"
            [outlined]="true"
            severity="secondary"
            routerLink="/caja"
          />
        } @else {
          <p-button label="Abrir caja" icon="pi pi-lock-open" routerLink="/caja" />
        }
      </div>
    </div>

    <div class="fila-stats stats">
      <app-stat-card
        etiqueta="Cobrado en el turno"
        [valor]="(resumen.value()?.total ?? 0 | currency: 'PEN' : 'symbol-narrow') ?? ''"
        icono="pi-dollar"
        tono="exito"
        enlace="/caja/comprobantes"
      />
      <app-stat-card
        etiqueta="Comprobantes"
        [valor]="resumen.value()?.comprobantes ?? 0"
        icono="pi-receipt"
        enlace="/caja/comprobantes"
      />
      <app-stat-card
        etiqueta="Por cobrar"
        [valor]="pendientes.value()?.length ?? 0"
        icono="pi-clock"
        [tono]="(pendientes.value()?.length ?? 0) > 0 ? 'aviso' : 'neutro'"
        [detalle]="(totalPendiente() | currency: 'PEN' : 'symbol-narrow') ?? ''"
        enlace="/caja/cobrar"
      />
      <app-stat-card
        etiqueta="Última diferencia"
        [valor]="(ultimaDiferencia() | currency: 'PEN' : 'symbol-narrow') ?? '—'"
        icono="pi-exclamation-triangle"
        [tono]="ultimaDiferencia() === 0 || ultimaDiferencia() === null ? 'neutro' : 'aviso'"
        detalle="Cierre anterior"
        enlace="/caja/historial"
      />
    </div>

    <div class="tablero">
      <div class="c7">
        <app-section-card
          titulo="Cuentas por cobrar"
          icono="pi-clock"
          subtitulo="Pacientes con consultas o estudios pendientes de pago"
          [cargando]="pendientes.loading() && !pendientes.hasValue()"
          [error]="pendientes.error()?.message ?? null"
          [vacio]="listaPendientes().length === 0 && pendientes.hasValue()"
          textoVacio="No hay cuentas por cobrar."
        >
          <app-task-list [items]="listaPendientes()" />
          <a pie routerLink="/caja/cobrar">Ir a cobrar</a>
        </app-section-card>
      </div>

      <div class="c5">
        <app-section-card
          titulo="Cobrado por método"
          icono="pi-credit-card"
          [vacio]="barras().length === 0"
          textoVacio="Aún no hay cobros en este turno."
        >
          <app-bar-list [items]="barras()" etiqueta="Total cobrado por método de pago" />
        </app-section-card>
      </div>

      <div class="c12">
        <app-quick-actions [acciones]="acciones" />
      </div>
    </div>
  `,
  styles: `
    .estado-caja {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: var(--sp-3);
      margin-bottom: var(--sp-4);
      padding: var(--sp-4);
      border: 1px solid var(--p-content-border-color);
      border-left: 4px solid var(--estado-neutro);
      border-radius: var(--radio-md);
      background: var(--p-content-background);
      box-shadow: var(--sombra-1);

      &.abierta {
        border-left-color: var(--estado-exito);
      }
    }

    .info {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: var(--sp-3);
    }

    .detalle {
      color: var(--p-text-muted-color);
    }

    .acciones {
      display: flex;
      flex-wrap: wrap;
      gap: var(--sp-2);
    }

    .stats {
      margin-bottom: var(--sp-4);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InicioCaja implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly api = inject(CajaApiService);

  protected readonly sesion = new AsyncState<SesionCajaResponse>();
  protected readonly resumen = new AsyncState<ResumenCajaResponse>();
  protected readonly pendientes = new AsyncState<PendienteCobroResponse[]>();
  protected readonly historial = new AsyncState<SesionCajaResponse[]>();

  protected readonly saludo = computed(() => saludoDelDia(this.auth.user()?.nombreCompleto ?? ''));
  protected readonly fecha = computed(() => {
    const texto = fechaLarga();
    return texto.charAt(0).toUpperCase() + texto.slice(1);
  });

  protected readonly abierta = computed(() => this.sesion.value()?.estado === 'ABIERTA');

  protected readonly acciones: readonly AccionRapida[] = [
    {
      label: 'Cobrar',
      icono: 'pi-dollar',
      enlace: '/caja/cobrar',
      detalle: 'Cuentas por cobrar y cobro libre',
    },
    {
      label: 'Comprobantes',
      icono: 'pi-receipt',
      enlace: '/caja/comprobantes',
      detalle: 'Boletas y facturas',
    },
    {
      label: 'Historial de caja',
      icono: 'pi-history',
      enlace: '/caja/historial',
      detalle: 'Aperturas y cierres',
    },
    {
      label: 'Reporte de ingresos',
      icono: 'pi-chart-bar',
      enlace: '/caja/reportes',
      detalle: 'Por día y método',
    },
  ];

  protected readonly totalPendiente = computed(() =>
    (this.pendientes.value() ?? []).reduce((suma, p) => suma + p.monto, 0),
  );

  protected readonly ultimaDiferencia = computed<number | null>(() => {
    const cerrada = (this.historial.value() ?? []).find((s) => s.estado === 'CERRADA');
    return cerrada?.diferencia ?? null;
  });

  protected readonly barras = computed<ItemBarra[]>(() =>
    (this.resumen.value()?.porMetodo ?? []).map((m): ItemBarra => ({
      etiqueta: ETIQUETA_METODO_CAJA[m.metodo as keyof typeof ETIQUETA_METODO_CAJA] ?? m.metodo,
      valor: m.total,
      texto: `S/ ${m.total.toFixed(2)}`,
    })),
  );

  protected readonly listaPendientes = computed<ItemTarea[]>(() =>
    (this.pendientes.value() ?? []).slice(0, 8).map((p): ItemTarea => ({
      id: `${p.origen}-${p.referenciaId}`,
      titulo: p.paciente,
      detalle: `${ETIQUETA_ORIGEN[p.origen] ?? p.origen} · ${p.descripcion}`,
      cantidad: null,
      tono: 'aviso',
      icono: 'pi-dollar',
      enlace: '/caja/cobrar',
      sensible: true,
    })),
  );

  ngOnInit(): void {
    this.sesion.load(
      this.api.actual().pipe(
        tap((sesion) => {
          if (sesion.estado === 'ABIERTA') {
            this.resumen.load(this.api.resumen());
          }
        }),
      ),
    );
    this.pendientes.load(this.api.pendientes());
    this.historial.load(this.api.historial());
  }
}
