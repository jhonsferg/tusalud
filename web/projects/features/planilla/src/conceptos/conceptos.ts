import { DecimalPipe } from '@angular/common';
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
import { Checkbox } from 'primeng/checkbox';
import { Dialog } from 'primeng/dialog';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';

import {
  AuthService,
  ConceptoPlanillaRequest,
  ConceptoPlanillaResponse,
  ETIQUETA_TIPO_CONCEPTO,
  ModoConcepto,
  NotificationService,
  PlanillaApiService,
  TipoConceptoPlanilla,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { ESTILOS_PLANILLA } from '../planilla.estilos';

function vacio(): ConceptoPlanillaRequest {
  return {
    codigo: '',
    nombre: '',
    tipo: 'INGRESO',
    modo: 'MONTO_FIJO',
    valorDefecto: 0,
    afectoRenta: true,
    afectoPension: true,
    afectoEssalud: true,
    activo: true,
  };
}

@Component({
  selector: 'app-conceptos-planilla',
  imports: [
    DecimalPipe,
    FormsModule,
    Button,
    Checkbox,
    Dialog,
    InputNumber,
    InputText,
    Select,
    TableModule,
    Tag,
    EmptyState,
    PageHeader,
  ],
  template: `
    <app-page-header
      title="Conceptos de planilla"
      subtitle="Ingresos y descuentos configurables. Los conceptos del sistema se calculan solos."
      icon="pi-list"
    >
      @if (esRrhh()) {
        <p-button label="Nuevo concepto" icon="pi pi-plus" (onClick)="abrirNuevo()" />
      }
    </app-page-header>

    <section class="panel">
      <p-table [value]="lista()" [loading]="cargando()" [rowHover]="true" responsiveLayout="scroll">
        <ng-template #header>
          <tr>
            <th>Código</th>
            <th>Concepto</th>
            <th>Tipo</th>
            <th>Cálculo</th>
            <th>Afecto a</th>
            <th>Estado</th>
            <th style="width: 6rem"></th>
          </tr>
        </ng-template>
        <ng-template #body let-c>
          <tr>
            <td>{{ c.codigo }}</td>
            <td class="font-semibold">
              {{ c.nombre }}
              @if (c.sistema) {
                <p-tag value="Sistema" severity="secondary" />
              }
            </td>
            <td>{{ tipo(c.tipo) }}</td>
            <td>
              @if (c.sistema) {
                Automático
              } @else {
                {{ c.valorDefecto | number: '1.2-2'
                }}{{ c.modo === 'PORCENTAJE_BASICO' ? ' % del básico' : ' fijos' }}
              }
            </td>
            <td>
              {{ c.afectoRenta ? 'Renta ' : '' }}{{ c.afectoPension ? 'Pensión ' : ''
              }}{{ c.afectoEssalud ? 'EsSalud' : '' }}
            </td>
            <td>
              <p-tag
                [value]="c.activo ? 'Activo' : 'Inactivo'"
                [severity]="c.activo ? 'success' : 'secondary'"
              />
            </td>
            <td class="derecha">
              @if (esRrhh() && !c.sistema) {
                <p-button
                  icon="pi pi-pencil"
                  [rounded]="true"
                  [text]="true"
                  [ariaLabel]="'Editar el concepto ' + c.nombre"
                  (onClick)="abrirEdicion(c)"
                />
              }
            </td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr>
            <td colspan="7">
              <app-empty-state
                icon="pi-list"
                title="Sin conceptos"
                message="Crea los bonos y descuentos que usa la clínica."
              />
            </td>
          </tr>
        </ng-template>
      </p-table>
    </section>

    <p-dialog
      [header]="editando() ? 'Editar concepto' : 'Nuevo concepto'"
      [visible]="dialogo()"
      (visibleChange)="dialogo.set($event)"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '34rem', maxWidth: '96vw' }"
    >
      <div class="formulario">
        <div class="dos">
          <label for="cp-cod"
            >Código
            <input
              pInputText
              id="cp-cod"
              maxlength="30"
              [disabled]="!!editando()"
              [ngModel]="f().codigo"
              (ngModelChange)="cambiar('codigo', $event.toUpperCase())"
            />
          </label>
          <label for="cp-nom"
            >Nombre
            <input
              pInputText
              id="cp-nom"
              maxlength="100"
              [ngModel]="f().nombre"
              (ngModelChange)="cambiar('nombre', $event)"
            />
          </label>
        </div>
        <div class="dos">
          <label for="cp-tipo"
            >Tipo
            <p-select
              inputId="cp-tipo"
              [options]="tipos"
              optionLabel="label"
              optionValue="value"
              [ngModel]="f().tipo"
              (ngModelChange)="cambiar('tipo', $event)"
              appendTo="body"
            />
          </label>
          <label for="cp-modo"
            >Cálculo
            <p-select
              inputId="cp-modo"
              [options]="modos"
              optionLabel="label"
              optionValue="value"
              [ngModel]="f().modo"
              (ngModelChange)="cambiar('modo', $event)"
              appendTo="body"
            />
          </label>
        </div>
        <label for="cp-val"
          >Valor por defecto
          <p-inputnumber
            inputId="cp-val"
            [ngModel]="f().valorDefecto"
            (ngModelChange)="cambiar('valorDefecto', $event ?? 0)"
            [min]="0"
            [minFractionDigits]="2"
            [maxFractionDigits]="2"
          />
        </label>
        <div class="acciones">
          <label class="casilla" for="cp-ren">
            <p-checkbox
              inputId="cp-ren"
              [binary]="true"
              [ngModel]="f().afectoRenta"
              (ngModelChange)="cambiar('afectoRenta', $event)"
            />
            Afecto a renta
          </label>
          <label class="casilla" for="cp-pen">
            <p-checkbox
              inputId="cp-pen"
              [binary]="true"
              [ngModel]="f().afectoPension"
              (ngModelChange)="cambiar('afectoPension', $event)"
            />
            Afecto a pensión
          </label>
          <label class="casilla" for="cp-ess">
            <p-checkbox
              inputId="cp-ess"
              [binary]="true"
              [ngModel]="f().afectoEssalud"
              (ngModelChange)="cambiar('afectoEssalud', $event)"
            />
            Afecto a EsSalud
          </label>
          <label class="casilla" for="cp-act">
            <p-checkbox
              inputId="cp-act"
              [binary]="true"
              [ngModel]="f().activo"
              (ngModelChange)="cambiar('activo', $event)"
            />
            Activo
          </label>
        </div>
      </div>
      <ng-template #footer>
        <p-button
          label="Cancelar"
          severity="secondary"
          [outlined]="true"
          (onClick)="dialogo.set(false)"
        />
        <p-button
          label="Guardar"
          icon="pi pi-check"
          [loading]="ocupado()"
          [disabled]="!valido()"
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
export class ConceptosPlanilla implements OnInit {
  private readonly api = inject(PlanillaApiService);
  private readonly auth = inject(AuthService);
  private readonly notificaciones = inject(NotificationService);

  protected readonly tipos: { label: string; value: TipoConceptoPlanilla }[] = [
    { label: 'Ingreso', value: 'INGRESO' },
    { label: 'Descuento', value: 'DESCUENTO' },
  ];
  protected readonly modos: { label: string; value: ModoConcepto }[] = [
    { label: 'Monto fijo', value: 'MONTO_FIJO' },
    { label: 'Porcentaje del básico', value: 'PORCENTAJE_BASICO' },
  ];
  protected readonly lista = signal<ConceptoPlanillaResponse[]>([]);
  protected readonly cargando = signal(false);
  protected readonly ocupado = signal(false);
  protected readonly dialogo = signal(false);
  protected readonly editando = signal<ConceptoPlanillaResponse | null>(null);
  protected readonly f = signal<ConceptoPlanillaRequest>(vacio());
  protected readonly esRrhh = computed(() => this.auth.isRrhh());
  protected readonly valido = computed(
    () => /^[A-Z0-9_]{2,30}$/.test(this.f().codigo) && !!this.f().nombre.trim(),
  );

  ngOnInit(): void {
    this.cargar();
  }

  protected tipo(t: TipoConceptoPlanilla): string {
    return ETIQUETA_TIPO_CONCEPTO[t];
  }

  private cargar(): void {
    this.cargando.set(true);
    this.api.conceptos().subscribe({
      next: (l) => {
        this.lista.set(l);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
  }

  protected cambiar<K extends keyof ConceptoPlanillaRequest>(
    clave: K,
    valor: ConceptoPlanillaRequest[K],
  ): void {
    this.f.update((f) => ({ ...f, [clave]: valor }));
  }

  protected abrirNuevo(): void {
    this.editando.set(null);
    this.f.set(vacio());
    this.dialogo.set(true);
  }

  protected abrirEdicion(c: ConceptoPlanillaResponse): void {
    this.editando.set(c);
    this.f.set({
      codigo: c.codigo,
      nombre: c.nombre,
      tipo: c.tipo,
      modo: c.modo,
      valorDefecto: c.valorDefecto,
      afectoRenta: c.afectoRenta,
      afectoPension: c.afectoPension,
      afectoEssalud: c.afectoEssalud,
      activo: c.activo,
    });
    this.dialogo.set(true);
  }

  protected guardar(): void {
    const actual = this.editando();
    this.ocupado.set(true);
    const peticion = actual
      ? this.api.actualizarConcepto(actual.id, this.f())
      : this.api.crearConcepto(this.f());
    peticion.subscribe({
      next: () => {
        this.ocupado.set(false);
        this.dialogo.set(false);
        this.notificaciones.success('El concepto quedó guardado.');
        this.cargar();
      },
      error: () => this.ocupado.set(false),
    });
  }
}
