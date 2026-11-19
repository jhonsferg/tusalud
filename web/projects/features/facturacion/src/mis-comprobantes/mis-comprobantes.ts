import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { Button } from 'primeng/button';
import { TableModule } from 'primeng/table';

import {
  DescargaService,
  DocumentoElectronicoResumen,
  ETIQUETA_TIPO_CPE,
  FacturacionApiService,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { EstadoCpeTag } from '../estado-cpe-tag';

@Component({
  selector: 'app-mis-comprobantes',
  imports: [CurrencyPipe, DatePipe, Button, TableModule, EmptyState, EstadoCpeTag, PageHeader],
  template: `
    <app-page-header
      title="Mis comprobantes"
      subtitle="Boletas y facturas emitidas a su nombre"
      icon="pi-receipt"
    />

    <section class="panel">
      @if (error(); as mensaje) {
        <app-empty-state
          tone="error"
          icon="pi-exclamation-triangle"
          title="No pudimos cargar sus comprobantes"
          [message]="mensaje"
        >
          <p-button label="Reintentar" icon="pi pi-refresh" (onClick)="cargar()" />
        </app-empty-state>
      } @else {
        <p-table
          [value]="comprobantes()"
          [loading]="cargando()"
          [paginator]="comprobantes().length > 10"
          [rows]="10"
          responsiveLayout="scroll"
        >
          <ng-template #header>
            <tr>
              <th>Comprobante</th>
              <th>Emisión</th>
              <th class="text-right">Total</th>
              <th>Estado</th>
              <th></th>
            </tr>
          </ng-template>
          <ng-template #body let-c>
            <tr>
              <td>
                <span class="font-semibold">{{ c.numeroCompleto }}</span>
                <small class="block text-muted">{{ tipos[c.tipo] }}</small>
              </td>
              <td>{{ c.emitidoEn | date: 'd MMM y, HH:mm' }}</td>
              <td class="text-right">{{ c.total | currency: 'PEN' : 'symbol-narrow' }}</td>
              <td><app-estado-cpe-tag [estado]="c.estado" /></td>
              <td class="derecha">
                <p-button
                  label="Descargar PDF"
                  icon="pi pi-download"
                  size="small"
                  [outlined]="true"
                  [ariaLabel]="'Descargar el PDF de ' + c.numeroCompleto"
                  (onClick)="pdf(c)"
                />
              </td>
            </tr>
          </ng-template>
          <ng-template #emptymessage>
            <tr>
              <td colspan="5">
                <app-empty-state
                  icon="pi-receipt"
                  title="Aún no tiene comprobantes"
                  message="Cuando pague una consulta o un servicio, su comprobante aparecerá aquí."
                />
              </td>
            </tr>
          </ng-template>
        </p-table>
      }
    </section>
  `,
  styles: `
    .derecha {
      text-align: right;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MisComprobantes implements OnInit {
  private readonly api = inject(FacturacionApiService);
  private readonly descargas = inject(DescargaService);

  protected readonly tipos: Record<string, string> = ETIQUETA_TIPO_CPE;
  protected readonly comprobantes = signal<DocumentoElectronicoResumen[]>([]);
  protected readonly cargando = signal(false);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.cargar();
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.api.misComprobantes().subscribe({
      next: (lista) => {
        this.comprobantes.set(lista);
        this.cargando.set(false);
      },
      error: (e: { message: string }) => {
        this.error.set(e.message);
        this.cargando.set(false);
      },
    });
  }

  protected pdf(c: DocumentoElectronicoResumen): void {
    this.api.pdf(c.id).subscribe((archivo) => this.descargas.guardar(archivo));
  }
}
