import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { InputNumber } from 'primeng/inputnumber';
import { TableModule } from 'primeng/table';

import { ContabilidadApiService, FlujoCajaResponse } from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { ESTILOS_CONTABILIDAD } from '../contabilidad.estilos';

@Component({
  selector: 'app-flujo-caja',
  imports: [CurrencyPipe, DatePipe, FormsModule, InputNumber, TableModule, EmptyState, PageHeader],
  template: `
    <app-page-header
      title="Flujo de caja proyectado"
      subtitle="Saldo de tesorería, cobros esperados de aseguradoras y pagos a proveedores por vencimiento"
      icon="pi-chart-line"
    />

    <section class="panel">
      <div class="filtros">
        <label for="fc-semanas" class="font-semibold">Semanas a proyectar</label>
        <p-inputnumber
          inputId="fc-semanas"
          [ngModel]="semanas()"
          (ngModelChange)="cambiar($event)"
          [min]="1"
          [max]="26"
          [showButtons]="true"
          [useGrouping]="false"
        />
      </div>
      @if (flujo(); as f) {
        <div class="resumen" aria-label="Resumen del flujo de caja">
          <div>
            <strong>{{ f.saldoInicial | currency: 'PEN' : 'symbol-narrow' }}</strong>
            <span>Saldo actual de tesorería</span>
          </div>
          <div [class.alerta]="f.saldoFinal < 0">
            <strong>{{ f.saldoFinal | currency: 'PEN' : 'symbol-narrow' }}</strong>
            <span>Saldo proyectado a {{ f.semanas }} semanas</span>
          </div>
        </div>
        <p-table [value]="f.detalle" [rowHover]="true" responsiveLayout="scroll">
          <ng-template #header>
            <tr>
              <th>Semana</th>
              <th class="text-right">Cobros esperados</th>
              <th class="text-right">Pagos por vencer</th>
              <th class="text-right">Neto</th>
              <th class="text-right">Saldo proyectado</th>
            </tr>
          </ng-template>
          <ng-template #body let-s let-i="rowIndex">
            <tr>
              <td>
                {{ s.desde | date: 'd MMM' }} al {{ s.hasta | date: 'd MMM' }}
                @if (i === 0) {
                  <small class="block text-muted">Incluye lo vencido</small>
                }
              </td>
              <td class="text-right">{{ s.cobros | currency: 'PEN' : 'symbol-narrow' }}</td>
              <td class="text-right">{{ s.pagos | currency: 'PEN' : 'symbol-narrow' }}</td>
              <td class="text-right">{{ s.neto | currency: 'PEN' : 'symbol-narrow' }}</td>
              <td
                class="text-right"
                [style.color]="s.saldoProyectado < 0 ? 'var(--p-red-600)' : null"
              >
                {{ s.saldoProyectado | currency: 'PEN' : 'symbol-narrow' }}
              </td>
            </tr>
          </ng-template>
          <ng-template #emptymessage>
            <tr>
              <td colspan="5">
                <app-empty-state
                  icon="pi-chart-line"
                  title="Sin proyección"
                  message="No hay datos para proyectar."
                />
              </td>
            </tr>
          </ng-template>
        </p-table>
        <p class="aviso" role="note">
          Los cobros de aseguradoras se esperan a los 30 días de cada consumo. Los cobros de
          pacientes se reciben en el momento y no se proyectan.
        </p>
      }
    </section>
  `,
  styles: ESTILOS_CONTABILIDAD,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FlujoCaja implements OnInit {
  private readonly api = inject(ContabilidadApiService);

  protected readonly semanas = signal(8);
  protected readonly flujo = signal<FlujoCajaResponse | null>(null);

  ngOnInit(): void {
    this.cargar();
  }

  protected cambiar(valor: number | null): void {
    if (valor && valor >= 1 && valor <= 26) {
      this.semanas.set(valor);
      this.cargar();
    }
  }

  private cargar(): void {
    this.api.flujoCaja(this.semanas()).subscribe((f) => this.flujo.set(f));
  }
}
