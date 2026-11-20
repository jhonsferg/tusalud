import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';
import { Tag } from 'primeng/tag';

import { NotificationService, OdontologiaApiService, PlanOdontologicoResumen } from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import {
  ETIQUETA_ESTADO_PLAN,
  METODOS_PAGO,
  SEVERIDAD_ESTADO_PLAN,
  fechaLegible,
  soles,
} from '../odontologia.utils';

@Component({
  selector: 'app-cobros-odontologicos',
  imports: [
    FormsModule,
    Button,
    InputNumber,
    InputText,
    Message,
    Select,
    Tag,
    EmptyState,
    PageHeader,
  ],
  template: `
    <app-page-header
      title="Cobros odontológicos"
      subtitle="Planes de tratamiento aceptados con saldo pendiente de cobro"
      icon="pi-wallet"
    />
    @if (error()) {
      <p-message severity="error">{{ error() }}</p-message>
    }
    @if (cargando()) {
      <p class="text-muted" role="status">Cargando…</p>
    } @else if (planes().length === 0) {
      <div class="panel">
        <app-empty-state
          icon="pi-wallet"
          title="No hay saldos pendientes"
          message="Cuando un paciente acepte un plan de tratamiento aparecerá aquí para cobrar."
        />
      </div>
    } @else {
      <div class="panel tabla">
        <table>
          <caption class="sr-only">
            Planes odontológicos por cobrar
          </caption>
          <thead>
            <tr>
              <th scope="col">Paciente</th>
              <th scope="col">Plan</th>
              <th scope="col">Estado</th>
              <th scope="col">Total</th>
              <th scope="col">Pagado</th>
              <th scope="col">Saldo</th>
              <th scope="col"><span class="sr-only">Acciones</span></th>
            </tr>
          </thead>
          <tbody>
            @for (p of planes(); track p.id) {
              <tr>
                <th scope="row">{{ p.pacienteNombre }}</th>
                <td>{{ fecha(p.creadoEn) }}</td>
                <td><p-tag [value]="etiqueta[p.estado]" [severity]="severidad[p.estado]" /></td>
                <td>{{ soles(p.total) }}</td>
                <td>{{ soles(p.pagado) }}</td>
                <td>
                  <strong>{{ soles(p.saldo) }}</strong>
                </td>
                <td>
                  <p-button
                    label="Cobrar"
                    size="small"
                    [outlined]="true"
                    [ariaLabel]="'Cobrar el plan de ' + p.pacienteNombre"
                    (onClick)="elegir(p)"
                  />
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    }
    @if (seleccionado(); as p) {
      <form class="panel formulario" (submit)="$event.preventDefault(); cobrar(p)">
        <h3>Cobro para {{ p.pacienteNombre }}</h3>
        <p class="text-muted">Saldo pendiente: {{ soles(p.saldo) }}. La caja debe estar abierta.</p>
        <div class="campos">
          <label for="cobro-monto">
            Monto
            <p-inputnumber
              inputId="cobro-monto"
              name="monto"
              [(ngModel)]="monto"
              mode="decimal"
              [minFractionDigits]="2"
              [maxFractionDigits]="2"
              [min]="0.01"
              [max]="p.saldo"
            />
          </label>
          <label for="cobro-metodo">
            Método de pago
            <p-select
              inputId="cobro-metodo"
              name="metodo"
              [options]="metodos"
              [(ngModel)]="metodo"
            />
          </label>
          <label for="cobro-ref">
            Referencia
            <input
              pInputText
              id="cobro-ref"
              name="referencia"
              maxlength="60"
              [(ngModel)]="referencia"
            />
          </label>
        </div>
        <div class="barra">
          <p-button
            type="submit"
            label="Registrar cobro"
            icon="pi pi-check"
            [loading]="guardando()"
            [disabled]="!monto"
          />
          <p-button
            label="Cancelar"
            severity="secondary"
            [text]="true"
            (onClick)="seleccionado.set(null)"
          />
        </div>
      </form>
    }
  `,
  styles: `
    .panel {
      margin-bottom: 1rem;
    }

    .tabla {
      overflow-x: auto;
    }

    table {
      width: 100%;
      border-collapse: collapse;
      font-size: 0.92rem;
    }

    th,
    td {
      text-align: left;
      padding: 0.45rem 0.6rem;
      border-bottom: 1px solid var(--p-surface-200, #e2e8f0);
    }

    .formulario {
      display: grid;
      gap: 0.75rem;
    }

    h3,
    p {
      margin: 0;
    }

    .campos {
      display: grid;
      gap: 0.75rem;
      grid-template-columns: repeat(auto-fit, minmax(12rem, 1fr));
    }

    label {
      display: grid;
      gap: 0.25rem;
      font-weight: 600;
      font-size: 0.85rem;
    }

    .barra {
      display: flex;
      gap: 0.5rem;
    }

    .sr-only {
      position: absolute;
      width: 1px;
      height: 1px;
      overflow: hidden;
      clip: rect(0 0 0 0);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CobrosOdontologicos implements OnInit {
  private readonly api = inject(OdontologiaApiService);
  private readonly avisos = inject(NotificationService);

  protected readonly planes = signal<PlanOdontologicoResumen[]>([]);
  protected readonly seleccionado = signal<PlanOdontologicoResumen | null>(null);
  protected readonly cargando = signal(true);
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly fecha = fechaLegible;
  protected readonly soles = soles;
  protected readonly etiqueta = ETIQUETA_ESTADO_PLAN;
  protected readonly severidad = SEVERIDAD_ESTADO_PLAN;
  protected readonly metodos = [...METODOS_PAGO];

  protected monto: number | null = null;
  protected metodo = 'EFECTIVO';
  protected referencia = '';

  ngOnInit(): void {
    this.recargar();
  }

  protected elegir(p: PlanOdontologicoResumen): void {
    this.monto = p.saldo;
    this.metodo = 'EFECTIVO';
    this.referencia = '';
    this.seleccionado.set(p);
  }

  protected cobrar(p: PlanOdontologicoResumen): void {
    if (!this.monto) {
      return;
    }
    this.guardando.set(true);
    this.api
      .registrarPago(p.id, {
        monto: this.monto,
        metodo: this.metodo,
        referencia: this.referencia.trim() || null,
      })
      .subscribe({
        next: () => {
          this.guardando.set(false);
          this.seleccionado.set(null);
          this.avisos.success('Cobro registrado en la caja');
          this.recargar();
        },
        error: () => this.guardando.set(false),
      });
  }

  private recargar(): void {
    this.api.porCobrar().subscribe({
      next: (lista) => {
        this.planes.set(lista);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar los planes por cobrar');
        this.cargando.set(false);
      },
    });
  }
}
