import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';

import {
  ETIQUETA_ESTADO_RESUMEN,
  EstadoResumen,
  FacturacionApiService,
  NotificationService,
  ResumenSunat,
  toIsoDate,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';

const SEVERIDAD: Readonly<Record<EstadoResumen, 'success' | 'info' | 'warn' | 'danger'>> = {
  PENDIENTE: 'info',
  ENVIADO: 'warn',
  ACEPTADO: 'success',
  RECHAZADO: 'danger',
  ERROR: 'danger',
};

@Component({
  selector: 'app-resumenes-sunat',
  imports: [DatePipe, FormsModule, Button, DatePicker, TableModule, Tag, EmptyState, PageHeader],
  template: `
    <app-page-header
      title="Resúmenes diarios y bajas"
      subtitle="Boletas informadas por resumen y comunicaciones de baja enviadas a SUNAT"
      icon="pi-list-check"
    />

    <section class="panel generar">
      <div>
        <h3>Generar el resumen diario de boletas</h3>
        <p class="text-muted">
          Reúne las boletas pendientes de una fecha. Solo aplica cuando la configuración informa las
          boletas por resumen.
        </p>
      </div>
      <div class="controles">
        <p-datepicker
          [ngModel]="fecha()"
          (ngModelChange)="fecha.set($event)"
          [showIcon]="true"
          dateFormat="dd/mm/yy"
          [maxDate]="hoy"
          [readonlyInput]="true"
          appendTo="body"
          ariaLabel="Fecha de las boletas"
        />
        <p-button
          label="Generar resumen"
          icon="pi pi-plus"
          [loading]="trabajando()"
          (onClick)="generar()"
        />
        <p-button
          label="Procesar pendientes"
          icon="pi pi-send"
          [outlined]="true"
          [loading]="trabajando()"
          (onClick)="procesar()"
        />
      </div>
    </section>

    <section class="panel">
      <p-table [value]="resumenes()" [loading]="cargando()" responsiveLayout="scroll">
        <ng-template #header>
          <tr>
            <th>Identificador</th>
            <th>Tipo</th>
            <th>Fecha de referencia</th>
            <th class="text-right">Documentos</th>
            <th>Estado</th>
            <th>Ticket y constancia</th>
          </tr>
        </ng-template>
        <ng-template #body let-r>
          <tr>
            <td class="font-semibold">{{ r.identificador }}</td>
            <td>{{ r.tipo === 'RESUMEN_DIARIO' ? 'Resumen diario' : 'Comunicación de baja' }}</td>
            <td>{{ r.fechaReferencia | date: 'd MMM y' }}</td>
            <td class="text-right">{{ r.documentos }}</td>
            <td>
              <p-tag [value]="etiquetas[r.estado]" [severity]="severidades[r.estado]" />
            </td>
            <td>
              @if (r.ticket) {
                <small class="block text-muted">{{ r.ticket }}</small>
              }
              {{ r.cdrDescripcion }}
              @if (r.ultimoError) {
                <small class="block text-muted">{{ r.ultimoError }}</small>
              }
              @if (r.motivo) {
                <small class="block text-muted">Motivo: {{ r.motivo }}</small>
              }
            </td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr>
            <td colspan="6">
              <app-empty-state icon="pi-list-check" title="Sin resúmenes ni bajas" />
            </td>
          </tr>
        </ng-template>
      </p-table>
    </section>
  `,
  styles: `
    .generar {
      display: flex;
      flex-wrap: wrap;
      justify-content: space-between;
      align-items: center;
      gap: 1rem;
      margin-bottom: 1rem;
    }

    .generar h3,
    .generar p {
      margin: 0;
    }

    .controles {
      display: flex;
      flex-wrap: wrap;
      gap: 0.6rem;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ResumenesSunat implements OnInit {
  private readonly api = inject(FacturacionApiService);
  private readonly notificaciones = inject(NotificationService);

  protected readonly etiquetas: Record<string, string> = ETIQUETA_ESTADO_RESUMEN;
  protected readonly severidades: Record<
    string,
    'success' | 'info' | 'warn' | 'danger' | 'secondary'
  > = SEVERIDAD;
  protected readonly hoy = new Date();
  protected readonly resumenes = signal<ResumenSunat[]>([]);
  protected readonly cargando = signal(false);
  protected readonly trabajando = signal(false);
  protected readonly fecha = signal<Date>(new Date());

  ngOnInit(): void {
    this.cargar();
  }

  protected generar(): void {
    this.trabajando.set(true);
    this.api.generarResumen(toIsoDate(this.fecha())).subscribe({
      next: (r) => {
        this.trabajando.set(false);
        this.notificaciones.success(`Se generó el resumen ${r.identificador}.`);
        this.cargar();
      },
      error: () => this.trabajando.set(false),
    });
  }

  protected procesar(): void {
    this.trabajando.set(true);
    this.api.procesarCola().subscribe({
      next: (r) => {
        this.trabajando.set(false);
        this.notificaciones.success(`Se procesaron ${r.procesados} envíos.`);
        this.cargar();
      },
      error: () => this.trabajando.set(false),
    });
  }

  private cargar(): void {
    this.cargando.set(true);
    this.api.resumenes().subscribe({
      next: (lista) => {
        this.resumenes.set(lista);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
  }
}
