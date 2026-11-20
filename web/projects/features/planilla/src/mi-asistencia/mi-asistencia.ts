import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePicker } from 'primeng/datepicker';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';

import {
  ETIQUETA_ESTADO_ASISTENCIA,
  EstadoAsistencia,
  PlanillaApiService,
  ResumenAsistenciaResponse,
  toIsoDate,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { ESTILOS_PLANILLA } from '../planilla.estilos';
import { inicioDeMes, minutosAHoras } from '../planilla.utils';

@Component({
  selector: 'app-mi-asistencia',
  imports: [DatePipe, FormsModule, DatePicker, TableModule, Tag, EmptyState, PageHeader],
  template: `
    <app-page-header
      title="Mi asistencia"
      subtitle="Tus entradas, salidas, tardanzas y horas extra del periodo."
      icon="pi-clock"
    />

    <section class="panel">
      <div class="filtros">
        <label for="ma-desde"
          >Desde
          <p-datepicker
            inputId="ma-desde"
            [ngModel]="desde()"
            (ngModelChange)="cambiarDesde($event)"
            dateFormat="dd/mm/yy"
          />
        </label>
        <label for="ma-hasta"
          >Hasta
          <p-datepicker
            inputId="ma-hasta"
            [ngModel]="hasta()"
            (ngModelChange)="cambiarHasta($event)"
            dateFormat="dd/mm/yy"
          />
        </label>
      </div>

      @if (resumen(); as r) {
        @if (r.codigoMarcacion) {
          <p class="aviso">
            Tu código de marcación en las terminales es <strong>{{ r.codigoMarcacion }}</strong
            >. Es personal, no lo compartas.
          </p>
        }
        <div class="resumen">
          <div>
            <strong>{{ r.diasPresente }}</strong>
            <span>Días asistidos</span>
          </div>
          <div [class.alerta]="r.diasTardanza > 0">
            <strong>{{ r.diasTardanza }}</strong>
            <span>Tardanzas</span>
          </div>
          <div [class.alerta]="r.diasFalta > 0">
            <strong>{{ r.diasFalta }}</strong>
            <span>Faltas</span>
          </div>
          <div>
            <strong>{{ r.diasAusenciaAprobada }}</strong>
            <span>Ausencias aprobadas</span>
          </div>
          <div>
            <strong>{{ horas(r.minutosTrabajados) }}</strong>
            <span>Trabajadas</span>
          </div>
          <div>
            <strong>{{ horas(r.minutosExtra) }}</strong>
            <span>Horas extra</span>
          </div>
        </div>

        <p-table
          [value]="r.dias"
          [loading]="cargando()"
          [rowHover]="true"
          responsiveLayout="scroll"
        >
          <ng-template #header>
            <tr>
              <th>Fecha</th>
              <th>Estado</th>
              <th>Turno</th>
              <th>Entrada</th>
              <th>Salida</th>
              <th class="text-right">Tardanza</th>
              <th class="text-right">Extra</th>
            </tr>
          </ng-template>
          <ng-template #body let-d>
            <tr>
              <td>{{ d.fecha | date: 'EEE d MMM' }}</td>
              <td>
                <p-tag [value]="etiqueta(d.estado)" [severity]="severidad(d.estado)" />
                @if (d.faltaJustificada) {
                  <small class="block text-muted">Justificada</small>
                }
              </td>
              <td>
                @if (d.programadaInicio) {
                  {{ d.programadaInicio.substring(0, 5) }} a {{ d.programadaFin?.substring(0, 5) }}
                } @else {
                  Sin turno
                }
              </td>
              <td>{{ d.entrada ? (d.entrada | date: 'HH:mm') : '' }}</td>
              <td>{{ d.salida ? (d.salida | date: 'HH:mm') : '' }}</td>
              <td class="text-right">{{ d.minutosTardanza }} min</td>
              <td class="text-right">{{ d.minutosExtra }} min</td>
            </tr>
          </ng-template>
          <ng-template #emptymessage>
            <tr>
              <td colspan="7">
                <app-empty-state
                  icon="pi-clock"
                  title="Sin registros"
                  message="No hay asistencia en el periodo elegido."
                />
              </td>
            </tr>
          </ng-template>
        </p-table>
      }
    </section>
  `,
  styles: ESTILOS_PLANILLA,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MiAsistencia implements OnInit {
  private readonly api = inject(PlanillaApiService);

  protected readonly desde = signal<Date>(inicioDeMes());
  protected readonly hasta = signal<Date>(new Date());
  protected readonly resumen = signal<ResumenAsistenciaResponse | null>(null);
  protected readonly cargando = signal(false);

  ngOnInit(): void {
    this.cargar();
  }

  protected horas(minutos: number): string {
    return minutosAHoras(minutos);
  }

  protected etiqueta(estado: EstadoAsistencia): string {
    return ETIQUETA_ESTADO_ASISTENCIA[estado];
  }

  protected severidad(
    estado: EstadoAsistencia,
  ): 'success' | 'warn' | 'danger' | 'info' | 'secondary' {
    switch (estado) {
      case 'PRESENTE':
        return 'success';
      case 'TARDANZA':
      case 'SIN_SALIDA':
        return 'warn';
      case 'FALTA':
        return 'danger';
      case 'AUSENCIA_APROBADA':
        return 'info';
      default:
        return 'secondary';
    }
  }

  protected cambiarDesde(valor: Date): void {
    this.desde.set(valor);
    this.cargar();
  }

  protected cambiarHasta(valor: Date): void {
    this.hasta.set(valor);
    this.cargar();
  }

  private cargar(): void {
    this.cargando.set(true);
    this.api.miAsistencia(toIsoDate(this.desde()), toIsoDate(this.hasta())).subscribe({
      next: (r) => {
        this.resumen.set(r);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
  }
}
