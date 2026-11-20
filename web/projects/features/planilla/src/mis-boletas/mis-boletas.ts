import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { TableModule } from 'primeng/table';

import {
  BoletaPagoResponse,
  ETIQUETA_TIPO_CONCEPTO,
  MiBoletaResumenResponse,
  PlanillaApiService,
} from '@tusalud/core';
import { BotonDocumento } from '@tusalud/ui/boton-documento';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { SecurityNotice } from '@tusalud/ui/tablero';
import { ESTILOS_PLANILLA } from '../planilla.estilos';
import { etiquetaPeriodo } from '../planilla.utils';

@Component({
  selector: 'app-mis-boletas',
  imports: [
    CurrencyPipe,
    Button,
    Dialog,
    TableModule,
    BotonDocumento,
    EmptyState,
    PageHeader,
    SecurityNotice,
  ],
  template: `
    <app-page-header
      title="Mis boletas de pago"
      subtitle="Solo tú ves tus boletas, y aparecen cuando la planilla del mes ya fue pagada."
      icon="pi-money-bill"
    />
    <app-security-notice
      variante="confidencial"
      texto="Tu boleta es personal. No la compartas ni la dejes abierta en una pantalla compartida."
    />

    <section class="panel">
      <p-table [value]="lista()" [loading]="cargando()" [rowHover]="true" responsiveLayout="scroll">
        <ng-template #header>
          <tr>
            <th>Periodo</th>
            <th>Boleta</th>
            <th class="text-right">Ingresos</th>
            <th class="text-right">Descuentos</th>
            <th class="text-right">Neto</th>
            <th style="width: 9rem"></th>
          </tr>
        </ng-template>
        <ng-template #body let-b>
          <tr>
            <td class="font-semibold">{{ periodo(b) }}</td>
            <td>{{ b.numero }}</td>
            <td class="text-right">{{ b.totalIngresos | currency: 'PEN' : 'S/ ' }}</td>
            <td class="text-right">{{ b.totalDescuentos | currency: 'PEN' : 'S/ ' }}</td>
            <td class="text-right negrita">{{ b.neto | currency: 'PEN' : 'S/ ' }}</td>
            <td class="derecha">
              <p-button
                icon="pi pi-eye"
                [rounded]="true"
                [text]="true"
                [ariaLabel]="'Ver la boleta de ' + periodo(b)"
                (onClick)="ver(b)"
              />
              <app-boton-documento
                tipo="boleta-pago"
                [registro]="b.id"
                [soloIcono]="true"
                [ariaEtiqueta]="'Descargar la boleta de ' + periodo(b)"
              />
            </td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr>
            <td colspan="6">
              <app-empty-state
                icon="pi-money-bill"
                title="Sin boletas todavía"
                message="Cuando se pague la planilla del mes verás aquí tu boleta."
              />
            </td>
          </tr>
        </ng-template>
      </p-table>
    </section>

    <p-dialog
      [header]="detalle() ? 'Boleta ' + detalle()?.numero : 'Boleta'"
      [visible]="!!detalle()"
      (visibleChange)="$event ? null : detalle.set(null)"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '40rem', maxWidth: '96vw' }"
    >
      @if (detalle(); as d) {
        <dl class="ficha">
          <div>
            <dt>Periodo</dt>
            <dd>{{ etiquetaPeriodo(d.anio, d.mes) }}</dd>
          </div>
          <div>
            <dt>Cargo</dt>
            <dd>{{ d.cargo }}</dd>
          </div>
          <div>
            <dt>Días laborados</dt>
            <dd>{{ d.diasLaborados }}</dd>
          </div>
          <div>
            <dt>Faltas</dt>
            <dd>{{ d.diasFalta }}</dd>
          </div>
        </dl>
        <p-table [value]="d.lineas" responsiveLayout="scroll">
          <ng-template #header>
            <tr>
              <th>Concepto</th>
              <th>Tipo</th>
              <th class="text-right">Monto</th>
            </tr>
          </ng-template>
          <ng-template #body let-l>
            <tr>
              <td>{{ l.nombre }}</td>
              <td>{{ tipoConcepto(l.tipo) }}</td>
              <td class="text-right">{{ l.monto | currency: 'PEN' : 'S/ ' }}</td>
            </tr>
          </ng-template>
        </p-table>
        <div class="cuadre">
          <span>Ingresos {{ d.totalIngresos | currency: 'PEN' : 'S/ ' }}</span>
          <span>Descuentos {{ d.totalDescuentos | currency: 'PEN' : 'S/ ' }}</span>
          <span class="ok">Neto {{ d.neto | currency: 'PEN' : 'S/ ' }}</span>
        </div>
      }
    </p-dialog>
  `,
  styles: ESTILOS_PLANILLA,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MisBoletas implements OnInit {
  private readonly api = inject(PlanillaApiService);

  protected readonly lista = signal<MiBoletaResumenResponse[]>([]);
  protected readonly detalle = signal<BoletaPagoResponse | null>(null);
  protected readonly cargando = signal(false);
  protected readonly etiquetaPeriodo = etiquetaPeriodo;

  ngOnInit(): void {
    this.cargando.set(true);
    this.api.misBoletas().subscribe({
      next: (l) => {
        this.lista.set(l);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
  }

  protected periodo(b: MiBoletaResumenResponse): string {
    return etiquetaPeriodo(b.anio, b.mes);
  }

  protected tipoConcepto(tipo: keyof typeof ETIQUETA_TIPO_CONCEPTO): string {
    return ETIQUETA_TIPO_CONCEPTO[tipo];
  }

  protected ver(b: MiBoletaResumenResponse): void {
    this.api.boleta(b.id).subscribe((x) => this.detalle.set(x));
  }
}
