import { CurrencyPipe, DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';

import {
  AsientoPendienteResponse,
  AuthService,
  ContabilidadApiService,
  NotificationService,
  ResumenContabilidad,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { ESTILOS_CONTABILIDAD } from '../contabilidad.estilos';

@Component({
  selector: 'app-contabilidad-resumen',
  imports: [CurrencyPipe, DatePipe, RouterLink, Button, TableModule, Tag, EmptyState, PageHeader],
  template: `
    <app-page-header
      title="Contabilidad"
      subtitle="Periodo, asientos, tesorería y pendientes de contabilizar"
      icon="pi-book"
    />

    @if (resumen(); as r) {
      <div class="resumen" aria-label="Resumen contable">
        <div>
          <strong>{{ r.periodoActual ?? 'Sin abrir' }}</strong>
          <span>Periodo actual{{ r.estadoPeriodoActual === 'CERRADO' ? ', cerrado' : '' }}</span>
        </div>
        <div>
          <strong>{{ r.asientosDelPeriodo }}</strong>
          <span>Asientos contabilizados</span>
        </div>
        <div [class.alerta]="r.borradores > 0">
          <strong>{{ r.borradores }}</strong>
          <span>Borradores por contabilizar</span>
        </div>
        <div [class.alerta]="r.pendientesDeContabilizar > 0">
          <strong>{{ r.pendientesDeContabilizar }}</strong>
          <span>Hechos sin asiento</span>
        </div>
        <div>
          <strong>{{ r.saldoTesoreria | currency: 'PEN' : 'symbol-narrow' }}</strong>
          <span>Saldo de tesorería</span>
        </div>
        <div>
          <strong>{{ r.porCobrar | currency: 'PEN' : 'symbol-narrow' }}</strong>
          <span>Por cobrar a aseguradoras</span>
        </div>
        <div>
          <strong>{{ r.porPagar | currency: 'PEN' : 'symbol-narrow' }}</strong>
          <span>Por pagar a proveedores</span>
        </div>
        <div [class.alerta]="r.extractosSinConciliar > 0">
          <strong>{{ r.extractosSinConciliar }}</strong>
          <span>Líneas de extracto sin conciliar</span>
        </div>
      </div>
    }

    <section class="panel">
      <h3 class="subseccion">Accesos</h3>
      <div class="filtros">
        <a routerLink="/contabilidad/asientos"
          ><p-button label="Asientos" icon="pi pi-book" severity="secondary" [outlined]="true"
        /></a>
        <a routerLink="/contabilidad/libros"
          ><p-button
            label="Libros y estados"
            icon="pi pi-chart-bar"
            severity="secondary"
            [outlined]="true"
        /></a>
        <a routerLink="/contabilidad/tesoreria"
          ><p-button label="Tesorería" icon="pi pi-wallet" severity="secondary" [outlined]="true"
        /></a>
        <a routerLink="/contabilidad/conciliacion"
          ><p-button
            label="Conciliación"
            icon="pi pi-check-square"
            severity="secondary"
            [outlined]="true"
        /></a>
        <a routerLink="/contabilidad/flujo-caja"
          ><p-button
            label="Flujo de caja"
            icon="pi pi-chart-line"
            severity="secondary"
            [outlined]="true"
        /></a>
      </div>
    </section>

    <section class="panel">
      <h3 class="subseccion">Hechos que no pudieron contabilizarse</h3>
      <p class="text-muted">
        Cobros, comprobantes y pagos que no generaron su asiento, por ejemplo por un periodo
        cerrado. Corrige la causa y reprocesa.
      </p>
      <p-table [value]="pendientes()" [rowHover]="true" responsiveLayout="scroll">
        <ng-template #header>
          <tr>
            <th>Hecho</th>
            <th>Motivo</th>
            <th>Desde</th>
            <th>Intentos</th>
            <th style="width: 9rem"></th>
          </tr>
        </ng-template>
        <ng-template #body let-p>
          <tr>
            <td>
              <p-tag [value]="etiqueta(p.evento)" severity="warn" />
            </td>
            <td>{{ p.error }}</td>
            <td>{{ p.creadoEn | date: 'd MMM y HH:mm' }}</td>
            <td>{{ p.intentos }}</td>
            <td class="derecha">
              @if (esFinanzas()) {
                <p-button
                  label="Reprocesar"
                  icon="pi pi-refresh"
                  size="small"
                  [loading]="reprocesando() === p.id"
                  [ariaLabel]="'Reprocesar el hecho ' + etiqueta(p.evento)"
                  (onClick)="reprocesar(p)"
                />
              }
            </td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr>
            <td colspan="5">
              <app-empty-state
                icon="pi-check-circle"
                title="Todo contabilizado"
                message="No hay hechos pendientes de contabilizar."
              />
            </td>
          </tr>
        </ng-template>
      </p-table>
    </section>
  `,
  styles: ESTILOS_CONTABILIDAD,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ContabilidadResumen implements OnInit {
  private readonly api = inject(ContabilidadApiService);
  private readonly auth = inject(AuthService);
  private readonly notificaciones = inject(NotificationService);

  protected readonly resumen = signal<ResumenContabilidad | null>(null);
  protected readonly pendientes = signal<AsientoPendienteResponse[]>([]);
  protected readonly reprocesando = signal<string | null>(null);
  protected readonly esFinanzas = computed(() => this.auth.isFinanzas());

  ngOnInit(): void {
    this.cargar();
  }

  protected etiqueta(evento: string): string {
    return (
      (
        {
          CobroRegistrado: 'Cobro en caja',
          CobroAnulado: 'Anulación de cobro',
          CompraRegistrada: 'Comprobante de proveedor',
          CompraAnulada: 'Anulación de comprobante',
          PagoProveedorRegistrado: 'Pago a proveedor',
        } as Record<string, string>
      )[evento] ?? evento
    );
  }

  private cargar(): void {
    this.api.resumen().subscribe((r) => this.resumen.set(r));
    this.api.pendientes(true).subscribe((p) => this.pendientes.set(p));
  }

  protected reprocesar(p: AsientoPendienteResponse): void {
    this.reprocesando.set(p.id);
    this.api.reprocesar(p.id).subscribe({
      next: () => {
        this.reprocesando.set(null);
        this.notificaciones.success('El hecho fue contabilizado.');
        this.cargar();
      },
      error: () => {
        this.reprocesando.set(null);
        this.cargar();
      },
    });
  }
}
