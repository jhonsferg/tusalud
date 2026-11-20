import { DatePipe, DecimalPipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';
import { Dialog } from 'primeng/dialog';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { ToggleSwitch } from 'primeng/toggleswitch';

import {
  AuthService,
  NotificationService,
  ParametroPlanillaResponse,
  PlanillaApiService,
  toIsoDate,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { ESTILOS_PLANILLA } from '../planilla.estilos';

@Component({
  selector: 'app-parametros-planilla',
  imports: [
    DatePipe,
    DecimalPipe,
    FormsModule,
    Button,
    DatePicker,
    Dialog,
    InputNumber,
    InputText,
    TableModule,
    Tag,
    ToggleSwitch,
    EmptyState,
    PageHeader,
  ],
  template: `
    <app-page-header
      title="Parámetros del régimen laboral"
      subtitle="Valores de ley por vigencia. Están por validar con el contador antes de usarse en una planilla real."
      icon="pi-sliders-h"
    >
      @if (esRrhh()) {
        <p-button label="Nueva versión" icon="pi pi-plus" (onClick)="abrirNueva(null)" />
      }
    </app-page-header>

    <p class="aviso">
      Cada cambio crea una versión nueva desde el primer día de un mes y conserva la anterior. Las
      planillas ya aprobadas no se alteran.
    </p>

    <section class="panel">
      <div class="filtros">
        <label class="casilla" for="pa-hist">
          <p-toggleswitch
            inputId="pa-hist"
            [ngModel]="historial()"
            (ngModelChange)="cambiarHistorial($event)"
          />
          Ver el historial completo
        </label>
      </div>
      <p-table
        [value]="lista()"
        [loading]="cargando()"
        [rowHover]="true"
        [paginator]="lista().length > 20"
        [rows]="20"
        responsiveLayout="scroll"
      >
        <ng-template #header>
          <tr>
            <th>Parámetro</th>
            <th>Descripción</th>
            <th class="text-right">Valor</th>
            <th>Vigencia</th>
            <th>Validación</th>
            <th style="width: 9rem"></th>
          </tr>
        </ng-template>
        <ng-template #body let-p>
          <tr>
            <td class="font-semibold">{{ p.codigo }}</td>
            <td>{{ p.descripcion }}</td>
            <td class="text-right negrita">{{ p.valor | number: '1.2-4' }}</td>
            <td>
              {{ p.vigenteDesde | date: 'd MMM y' }}
              @if (p.vigenteHasta) {
                <small class="block text-muted">hasta {{ p.vigenteHasta | date: 'd MMM y' }}</small>
              }
              @if (p.vigente) {
                <p-tag value="Vigente" severity="success" />
              }
            </td>
            <td>
              <p-tag
                [value]="p.porValidar ? 'Por validar' : 'Validado'"
                [severity]="p.porValidar ? 'warn' : 'success'"
              />
            </td>
            <td class="derecha">
              @if (esFinanzas() && p.porValidar) {
                <p-button
                  label="Validar"
                  icon="pi pi-check"
                  size="small"
                  [outlined]="true"
                  [ariaLabel]="'Validar ' + p.codigo"
                  (onClick)="validar(p)"
                />
              }
              @if (esRrhh() && p.vigente) {
                <p-button
                  icon="pi pi-pencil"
                  [rounded]="true"
                  [text]="true"
                  [ariaLabel]="'Cambiar ' + p.codigo"
                  (onClick)="abrirNueva(p)"
                />
              }
            </td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr>
            <td colspan="6">
              <app-empty-state
                icon="pi-sliders-h"
                title="Sin parámetros"
                message="No hay parámetros vigentes."
              />
            </td>
          </tr>
        </ng-template>
      </p-table>
    </section>

    <p-dialog
      header="Nueva versión de un parámetro"
      [visible]="dialogo()"
      (visibleChange)="dialogo.set($event)"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '28rem', maxWidth: '96vw' }"
    >
      <div class="formulario">
        <label for="pn-cod"
          >Código
          <input
            pInputText
            id="pn-cod"
            maxlength="40"
            [ngModel]="codigo()"
            (ngModelChange)="codigo.set($event.toUpperCase())"
          />
        </label>
        <label for="pn-val"
          >Nuevo valor
          <p-inputnumber
            inputId="pn-val"
            [ngModel]="valor()"
            (ngModelChange)="valor.set($event)"
            [min]="0"
            [minFractionDigits]="2"
            [maxFractionDigits]="4"
          />
        </label>
        <label for="pn-desde"
          >Vigente desde, un primer día de mes
          <p-datepicker
            inputId="pn-desde"
            [ngModel]="desde()"
            (ngModelChange)="desde.set($event)"
            dateFormat="dd/mm/yy"
            appendTo="body"
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
          label="Guardar versión"
          icon="pi pi-check"
          [loading]="ocupado()"
          [disabled]="!codigo().trim() || valor() === null || !desde()"
          (onClick)="guardar()"
        />
      </ng-template>
    </p-dialog>
  `,
  styles: [
    ESTILOS_PLANILLA,
    `
      .casilla {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        font-weight: 500;
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ParametrosPlanilla implements OnInit {
  private readonly api = inject(PlanillaApiService);
  private readonly auth = inject(AuthService);
  private readonly notificaciones = inject(NotificationService);

  protected readonly lista = signal<ParametroPlanillaResponse[]>([]);
  protected readonly historial = signal(false);
  protected readonly cargando = signal(false);
  protected readonly ocupado = signal(false);
  protected readonly dialogo = signal(false);
  protected readonly codigo = signal('');
  protected readonly valor = signal<number | null>(null);
  protected readonly desde = signal<Date | null>(null);
  protected readonly esRrhh = computed(() => this.auth.isRrhh());
  protected readonly esFinanzas = computed(() => this.auth.isFinanzas());

  ngOnInit(): void {
    this.cargar();
  }

  protected cambiarHistorial(valor: boolean): void {
    this.historial.set(valor);
    this.cargar();
  }

  private cargar(): void {
    this.cargando.set(true);
    this.api.parametros(!this.historial()).subscribe({
      next: (l) => {
        this.lista.set(l);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
  }

  protected abrirNueva(p: ParametroPlanillaResponse | null): void {
    this.codigo.set(p?.codigo ?? '');
    this.valor.set(p?.valor ?? null);
    const hoy = new Date();
    this.desde.set(new Date(hoy.getFullYear(), hoy.getMonth() + 1, 1));
    this.dialogo.set(true);
  }

  protected guardar(): void {
    const desde = this.desde();
    const valor = this.valor();
    if (!desde || valor === null) {
      return;
    }
    this.ocupado.set(true);
    this.api
      .nuevoParametro({ codigo: this.codigo().trim(), valor, vigenteDesde: toIsoDate(desde) })
      .subscribe({
        next: () => {
          this.ocupado.set(false);
          this.dialogo.set(false);
          this.notificaciones.success('La nueva versión del parámetro quedó registrada.');
          this.cargar();
        },
        error: () => this.ocupado.set(false),
      });
  }

  protected validar(p: ParametroPlanillaResponse): void {
    this.api.validarParametro(p.id).subscribe(() => {
      this.notificaciones.success(`${p.codigo} quedó validado.`);
      this.cargar();
    });
  }
}
