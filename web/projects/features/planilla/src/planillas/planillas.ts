import { CurrencyPipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { InputNumber } from 'primeng/inputnumber';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';

import {
  AuthService,
  ETIQUETA_ESTADO_PLANILLA,
  EstadoPlanilla,
  NotificationService,
  PlanillaApiService,
  PlanillaResponse,
  ResumenPlanillaResponse,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { MESES, etiquetaPeriodo } from '../planilla.utils';
import { ESTILOS_PLANILLA } from '../planilla.estilos';

export function severidadPlanilla(
  estado: EstadoPlanilla,
): 'secondary' | 'info' | 'warn' | 'success' {
  switch (estado) {
    case 'BORRADOR':
      return 'secondary';
    case 'CALCULADA':
      return 'warn';
    case 'APROBADA':
      return 'info';
    default:
      return 'success';
  }
}

@Component({
  selector: 'app-planillas',
  imports: [
    CurrencyPipe,
    FormsModule,
    Button,
    Dialog,
    InputNumber,
    Select,
    TableModule,
    Tag,
    EmptyState,
    PageHeader,
  ],
  template: `
    <app-page-header
      title="Planilla mensual"
      subtitle="Cálculo, aprobación y pago de las remuneraciones. Quien calcula no aprueba y quien aprueba no paga."
      icon="pi-users"
    >
      @if (esRrhh()) {
        <p-button label="Calcular planilla" icon="pi pi-calculator" (onClick)="abrirCalculo()" />
      }
    </app-page-header>

    @if (resumen(); as r) {
      <section class="resumen">
        <div>
          <strong>{{ r.empleadosConContrato }}</strong>
          <span>Empleados con contrato vigente</span>
        </div>
        <div>
          <strong>{{ r.masaSalarialMensual | currency: 'PEN' : 'S/ ' }}</strong>
          <span>Masa salarial mensual</span>
        </div>
        <div [class.alerta]="r.planillasPendientesAprobacion > 0">
          <strong>{{ r.planillasPendientesAprobacion }}</strong>
          <span>Planillas por aprobar</span>
        </div>
        <div>
          <strong>{{ r.marcacionesHoy }}</strong>
          <span>Marcaciones de hoy</span>
        </div>
      </section>
    }

    <section class="panel">
      <p-table [value]="lista()" [loading]="cargando()" [rowHover]="true" responsiveLayout="scroll">
        <ng-template #header>
          <tr>
            <th>Periodo</th>
            <th>Estado</th>
            <th class="text-right">Boletas</th>
            <th class="text-right">Ingresos</th>
            <th class="text-right">Descuentos</th>
            <th class="text-right">Neto</th>
            <th class="text-right">Aporte del empleador</th>
            <th style="width: 8rem"></th>
          </tr>
        </ng-template>
        <ng-template #body let-p>
          <tr>
            <td class="font-semibold">{{ periodo(p) }}</td>
            <td>
              <p-tag [value]="etiqueta(p.estado)" [severity]="severidad(p.estado)" />
            </td>
            <td class="text-right">{{ p.cantidadBoletas }}</td>
            <td class="text-right">{{ p.totalIngresos | currency: 'PEN' : 'S/ ' }}</td>
            <td class="text-right">{{ p.totalDescuentos | currency: 'PEN' : 'S/ ' }}</td>
            <td class="text-right negrita">{{ p.totalNeto | currency: 'PEN' : 'S/ ' }}</td>
            <td class="text-right">{{ p.totalAporteEmpleador | currency: 'PEN' : 'S/ ' }}</td>
            <td class="derecha">
              <p-button
                label="Abrir"
                icon="pi pi-arrow-right"
                size="small"
                [outlined]="true"
                [ariaLabel]="'Abrir la planilla de ' + periodo(p)"
                (onClick)="abrir(p)"
              />
            </td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr>
            <td colspan="8">
              <app-empty-state
                icon="pi-users"
                title="Sin planillas"
                message="Calcula la planilla del primer mes para generar las boletas."
              />
            </td>
          </tr>
        </ng-template>
      </p-table>
    </section>

    <p-dialog
      header="Calcular planilla"
      [visible]="dialogo()"
      (visibleChange)="dialogo.set($event)"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '26rem', maxWidth: '96vw' }"
    >
      <p class="aviso">
        Consolida la asistencia, aplica los contratos, conceptos y parámetros vigentes y genera las
        boletas. Se puede recalcular mientras no esté aprobada.
      </p>
      <div class="formulario">
        <label for="pl-mes"
          >Mes
          <p-select
            inputId="pl-mes"
            [options]="meses"
            optionLabel="label"
            optionValue="value"
            [ngModel]="mes()"
            (ngModelChange)="mes.set($event)"
            appendTo="body"
          />
        </label>
        <label for="pl-anio"
          >Año
          <p-inputnumber
            inputId="pl-anio"
            [ngModel]="anio()"
            (ngModelChange)="anio.set($event)"
            [useGrouping]="false"
            [min]="2000"
            [max]="2100"
          />
        </label>
      </div>
      <ng-template #footer>
        <p-button
          label="Cancelar"
          severity="secondary"
          [outlined]="true"
          (onClick)="dialogo.set(false)"
        />
        <p-button
          label="Calcular"
          icon="pi pi-check"
          [loading]="ocupado()"
          (onClick)="calcular()"
        />
      </ng-template>
    </p-dialog>
  `,
  styles: ESTILOS_PLANILLA,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Planillas implements OnInit {
  private readonly api = inject(PlanillaApiService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly notificaciones = inject(NotificationService);

  protected readonly meses = MESES;
  protected readonly lista = signal<PlanillaResponse[]>([]);
  protected readonly resumen = signal<ResumenPlanillaResponse | null>(null);
  protected readonly cargando = signal(false);
  protected readonly ocupado = signal(false);
  protected readonly dialogo = signal(false);
  protected readonly mes = signal(new Date().getMonth() === 0 ? 12 : new Date().getMonth());
  protected readonly anio = signal(
    new Date().getMonth() === 0 ? new Date().getFullYear() - 1 : new Date().getFullYear(),
  );
  protected readonly esRrhh = computed(() => this.auth.isRrhh());

  ngOnInit(): void {
    this.cargar();
  }

  protected periodo(p: PlanillaResponse): string {
    return etiquetaPeriodo(p.anio, p.mes);
  }

  protected etiqueta(estado: EstadoPlanilla): string {
    return ETIQUETA_ESTADO_PLANILLA[estado];
  }

  protected severidad(estado: EstadoPlanilla): 'secondary' | 'info' | 'warn' | 'success' {
    return severidadPlanilla(estado);
  }

  private cargar(): void {
    this.cargando.set(true);
    this.api.resumen().subscribe({ next: (r) => this.resumen.set(r) });
    this.api.planillas().subscribe({
      next: (l) => {
        this.lista.set(l);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
  }

  protected abrirCalculo(): void {
    this.dialogo.set(true);
  }

  protected abrir(p: PlanillaResponse): void {
    void this.router.navigate(['/planilla', p.id]);
  }

  protected calcular(): void {
    this.ocupado.set(true);
    this.api.calcular(this.anio(), this.mes()).subscribe({
      next: (p) => {
        this.ocupado.set(false);
        this.dialogo.set(false);
        this.notificaciones.success(
          `La planilla ${p.codigo} quedó calculada con ${p.cantidadBoletas} boletas.`,
        );
        void this.router.navigate(['/planilla', p.id]);
      },
      error: () => this.ocupado.set(false),
    });
  }
}
