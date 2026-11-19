import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { Checkbox } from 'primeng/checkbox';
import { Dialog } from 'primeng/dialog';
import { InputNumber } from 'primeng/inputnumber';
import { Message } from 'primeng/message';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';

import {
  EjecucionRetencion,
  ETIQUETA_ACCION_RETENCION,
  NotificationService,
  PoliticaRetencion,
  ProteccionDatosApiService,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';

@Component({
  selector: 'app-retencion-datos',
  imports: [
    DatePipe,
    FormsModule,
    Button,
    Checkbox,
    Dialog,
    InputNumber,
    Message,
    TableModule,
    Tag,
    EmptyState,
    PageHeader,
  ],
  template: `
    <app-page-header
      title="Retención de datos"
      subtitle="Cuánto tiempo se conserva cada tipo de dato y qué pasa al vencer el plazo"
      icon="pi-clock"
    >
      <p-button
        label="Ejecutar ahora"
        icon="pi pi-play"
        [loading]="ejecutando()"
        (onClick)="ejecutar()"
      />
    </app-page-header>

    <p-message severity="info" styleClass="w-full mb-3">
      La historia clínica nunca se elimina de forma automática. La tarea solo marca las historias
      inactivas para que una persona las revise. Los plazos y bases legales están por confirmar con
      un asesor legal.
    </p-message>

    <section class="panel">
      <h3>Políticas</h3>
      <p-table [value]="politicas()" responsiveLayout="scroll">
        <ng-template #header>
          <tr>
            <th>Tipo de dato</th>
            <th>Al vencer</th>
            <th class="text-right">Plazo</th>
            <th>Última ejecución</th>
            <th></th>
          </tr>
        </ng-template>
        <ng-template #body let-p>
          <tr>
            <td>
              <strong>{{ p.nombre }}</strong>
              <small class="block text-muted">{{ p.descripcion }}</small>
              <small class="block text-muted">Base: {{ p.baseLegal }}</small>
            </td>
            <td>
              <p-tag
                [value]="acciones[p.accion]"
                [severity]="p.accion === 'MARCAR' ? 'info' : 'warn'"
              />
              @if (!p.activa) {
                <p-tag value="Desactivada" severity="secondary" />
              }
            </td>
            <td class="text-right">{{ p.plazoDias }} días</td>
            <td>
              {{ p.ultimaEjecucion ? (p.ultimaEjecucion | date: 'd MMM y, HH:mm') : 'Nunca' }}
            </td>
            <td class="derecha">
              <p-button
                label="Ajustar"
                size="small"
                [outlined]="true"
                [ariaLabel]="'Ajustar ' + p.nombre"
                (onClick)="editar(p)"
              />
            </td>
          </tr>
        </ng-template>
      </p-table>
    </section>

    <section class="panel">
      <h3>Historial de ejecuciones</h3>
      <p-table
        [value]="historial()"
        [paginator]="historial().length > 10"
        [rows]="10"
        responsiveLayout="scroll"
      >
        <ng-template #header>
          <tr>
            <th>Fecha</th>
            <th>Política</th>
            <th class="text-right">Registros</th>
            <th>Detalle</th>
          </tr>
        </ng-template>
        <ng-template #body let-e>
          <tr>
            <td>{{ e.ejecutadaEn | date: 'd MMM y, HH:mm' }}</td>
            <td>{{ e.politica }}</td>
            <td class="text-right">{{ e.afectados }}</td>
            <td>{{ e.detalle }}</td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr>
            <td colspan="4"><app-empty-state icon="pi-clock" title="Sin ejecuciones" /></td>
          </tr>
        </ng-template>
      </p-table>
    </section>

    <p-dialog
      [header]="editando() ? 'Ajustar: ' + editando()!.nombre : ''"
      [visible]="editando() !== null"
      (visibleChange)="$event ? null : editando.set(null)"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '26rem', maxWidth: '96vw' }"
    >
      <form class="form" (submit)="$event.preventDefault(); guardar()" novalidate>
        <label for="ret-plazo">Plazo de conservación en días</label>
        <p-inputnumber
          inputId="ret-plazo"
          [ngModel]="plazo()"
          (ngModelChange)="plazo.set($event)"
          [ngModelOptions]="{ standalone: true }"
          [min]="1"
          [max]="36500"
        />
        <div class="check">
          <p-checkbox
            inputId="ret-activa"
            [binary]="true"
            [ngModel]="activa()"
            (ngModelChange)="activa.set($event)"
            [ngModelOptions]="{ standalone: true }"
          />
          <label for="ret-activa">Aplicar esta política en la tarea programada</label>
        </div>
      </form>
      <ng-template #footer>
        <p-button
          label="Cancelar"
          severity="secondary"
          [outlined]="true"
          (onClick)="editando.set(null)"
        />
        <p-button label="Guardar" icon="pi pi-check" (onClick)="guardar()" />
      </ng-template>
    </p-dialog>
  `,
  styles: `
    .panel {
      margin-bottom: 1rem;
    }

    .derecha {
      text-align: right;
    }

    .form {
      display: flex;
      flex-direction: column;
      gap: 0.4rem;
    }

    .form label {
      font-weight: 600;
      font-size: 0.9rem;
    }

    .check {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      margin-top: 0.5rem;
    }

    .check label {
      margin: 0;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RetencionDatos implements OnInit {
  private readonly api = inject(ProteccionDatosApiService);
  private readonly notificaciones = inject(NotificationService);

  protected readonly acciones: Record<string, string> = ETIQUETA_ACCION_RETENCION;
  protected readonly politicas = signal<PoliticaRetencion[]>([]);
  protected readonly historial = signal<EjecucionRetencion[]>([]);
  protected readonly editando = signal<PoliticaRetencion | null>(null);
  protected readonly plazo = signal(30);
  protected readonly activa = signal(true);
  protected readonly ejecutando = signal(false);

  ngOnInit(): void {
    this.cargar();
  }

  protected editar(p: PoliticaRetencion): void {
    this.plazo.set(p.plazoDias);
    this.activa.set(p.activa);
    this.editando.set(p);
  }

  protected guardar(): void {
    const p = this.editando();
    if (!p) {
      return;
    }
    this.api.actualizarRetencion(p.id, this.plazo(), this.activa()).subscribe(() => {
      this.editando.set(null);
      this.notificaciones.success('La política de retención se actualizó.');
      this.cargar();
    });
  }

  protected ejecutar(): void {
    this.ejecutando.set(true);
    this.api.ejecutarRetencion().subscribe({
      next: (resultado) => {
        this.ejecutando.set(false);
        const total = resultado.reduce((suma, e) => suma + e.afectados, 0);
        this.notificaciones.success(`La retención procesó ${total} registros.`);
        this.cargar();
      },
      error: () => this.ejecutando.set(false),
    });
  }

  private cargar(): void {
    this.api.retenciones().subscribe((lista) => this.politicas.set(lista));
    this.api.historialRetencion().subscribe((lista) => this.historial.set(lista));
  }
}
