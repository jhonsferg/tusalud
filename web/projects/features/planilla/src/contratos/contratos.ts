import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
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
import { DatePicker } from 'primeng/datepicker';
import { Dialog } from 'primeng/dialog';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';

import {
  ConceptoPlanillaResponse,
  ContratoLaboralRequest,
  ContratoLaboralResponse,
  EmpleadoConceptoResponse,
  EmpleadoResponse,
  ETIQUETA_REGIMEN,
  NotificationService,
  PlanillaApiService,
  RegimenLaboral,
  RrhhApiService,
  SistemaPensionario,
  toIsoDate,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { ESTILOS_PLANILLA } from '../planilla.estilos';

interface FormularioContrato {
  empleadoId: string | null;
  regimen: RegimenLaboral;
  remuneracion: number | null;
  asignacionFamiliar: boolean;
  sistemaPensionario: SistemaPensionario;
  afpNombre: string;
  horasSemanales: number;
  banco: string;
  cuenta: string;
  cargo: string;
  fechaInicio: Date | null;
  fechaFin: Date | null;
}

function vacio(): FormularioContrato {
  return {
    empleadoId: null,
    regimen: 'GENERAL',
    remuneracion: null,
    asignacionFamiliar: false,
    sistemaPensionario: 'ONP',
    afpNombre: '',
    horasSemanales: 48,
    banco: '',
    cuenta: '',
    cargo: '',
    fechaInicio: new Date(),
    fechaFin: null,
  };
}

@Component({
  selector: 'app-contratos-laborales',
  imports: [
    CurrencyPipe,
    DatePipe,
    DecimalPipe,
    FormsModule,
    Button,
    Checkbox,
    DatePicker,
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
      title="Contratos laborales"
      subtitle="Régimen, remuneración y condiciones de cada trabajador. La remuneración solo la ve recursos humanos."
      icon="pi-id-card"
    >
      <p-button label="Nuevo contrato" icon="pi pi-plus" (onClick)="abrirNuevo()" />
    </app-page-header>

    <section class="panel">
      <p-table
        [value]="lista()"
        [loading]="cargando()"
        [rowHover]="true"
        [paginator]="lista().length > 15"
        [rows]="15"
        responsiveLayout="scroll"
      >
        <ng-template #header>
          <tr>
            <th>Empleado</th>
            <th>Régimen</th>
            <th class="text-right">Remuneración</th>
            <th>Pensión</th>
            <th>Vigencia</th>
            <th>Estado</th>
            <th style="width: 16rem"></th>
          </tr>
        </ng-template>
        <ng-template #body let-c>
          <tr>
            <td>
              <span class="font-semibold">{{ c.empleadoNombre }}</span>
              <small class="block text-muted">{{ c.cargo }}</small>
              <small class="block text-muted">Código de marcación {{ c.codigoMarcacion }}</small>
            </td>
            <td>{{ regimen(c.regimen) }}</td>
            <td class="text-right">{{ c.remuneracion | currency: 'PEN' : 'S/ ' }}</td>
            <td>{{ c.sistemaPensionario }}{{ c.afpNombre ? ' ' + c.afpNombre : '' }}</td>
            <td>
              {{ c.fechaInicio | date: 'd MMM y' }}
              @if (c.fechaFin) {
                <small class="block text-muted">hasta {{ c.fechaFin | date: 'd MMM y' }}</small>
              }
            </td>
            <td>
              <p-tag
                [value]="c.estado === 'VIGENTE' ? 'Vigente' : 'Finalizado'"
                [severity]="c.estado === 'VIGENTE' ? 'success' : 'secondary'"
              />
            </td>
            <td class="derecha">
              <p-button
                icon="pi pi-list"
                [rounded]="true"
                [text]="true"
                [ariaLabel]="'Conceptos de ' + c.empleadoNombre"
                (onClick)="abrirConceptos(c)"
              />
              @if (c.estado === 'VIGENTE') {
                <p-button
                  icon="pi pi-pencil"
                  [rounded]="true"
                  [text]="true"
                  [ariaLabel]="'Editar el contrato de ' + c.empleadoNombre"
                  (onClick)="abrirEdicion(c)"
                />
                <p-button
                  icon="pi pi-flag"
                  [rounded]="true"
                  [text]="true"
                  severity="danger"
                  [ariaLabel]="'Finalizar el contrato de ' + c.empleadoNombre"
                  (onClick)="abrirFin(c)"
                />
              }
            </td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr>
            <td colspan="7">
              <app-empty-state
                icon="pi-id-card"
                title="Sin contratos"
                message="Registra el contrato de cada trabajador para incluirlo en la planilla."
              />
            </td>
          </tr>
        </ng-template>
      </p-table>
    </section>

    <p-dialog
      [header]="editando() ? 'Editar contrato' : 'Nuevo contrato'"
      [visible]="dialogo()"
      (visibleChange)="dialogo.set($event)"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '40rem', maxWidth: '96vw' }"
    >
      <div class="formulario">
        <label for="ct-emp"
          >Empleado
          <p-select
            inputId="ct-emp"
            [options]="empleados()"
            optionLabel="nombreCompleto"
            optionValue="id"
            [filter]="true"
            [disabled]="!!editando()"
            [ngModel]="f().empleadoId"
            (ngModelChange)="cambiar('empleadoId', $event)"
            appendTo="body"
          />
        </label>
        <div class="dos">
          <label for="ct-reg"
            >Régimen
            <p-select
              inputId="ct-reg"
              [options]="regimenes"
              optionLabel="label"
              optionValue="value"
              [ngModel]="f().regimen"
              (ngModelChange)="cambiar('regimen', $event)"
              appendTo="body"
            />
          </label>
          <label for="ct-rem"
            >Remuneración mensual
            <p-inputnumber
              inputId="ct-rem"
              mode="currency"
              currency="PEN"
              locale="es-PE"
              [ngModel]="f().remuneracion"
              (ngModelChange)="cambiar('remuneracion', $event)"
              [min]="0"
            />
          </label>
        </div>
        <div class="dos">
          <label for="ct-pen"
            >Sistema pensionario
            <p-select
              inputId="ct-pen"
              [options]="pensiones"
              [ngModel]="f().sistemaPensionario"
              (ngModelChange)="cambiar('sistemaPensionario', $event)"
              appendTo="body"
            />
          </label>
          @if (f().sistemaPensionario === 'AFP') {
            <label for="ct-afp"
              >AFP
              <input
                pInputText
                id="ct-afp"
                maxlength="30"
                [ngModel]="f().afpNombre"
                (ngModelChange)="cambiar('afpNombre', $event)"
              />
            </label>
          }
        </div>
        <div class="dos">
          <label for="ct-cargo"
            >Cargo
            <input
              pInputText
              id="ct-cargo"
              maxlength="100"
              [ngModel]="f().cargo"
              (ngModelChange)="cambiar('cargo', $event)"
            />
          </label>
          <label for="ct-horas"
            >Horas semanales
            <p-inputnumber
              inputId="ct-horas"
              [ngModel]="f().horasSemanales"
              (ngModelChange)="cambiar('horasSemanales', $event)"
              [min]="1"
              [max]="60"
            />
          </label>
        </div>
        <div class="dos">
          <label for="ct-ini"
            >Inicio
            <p-datepicker
              inputId="ct-ini"
              [ngModel]="f().fechaInicio"
              (ngModelChange)="cambiar('fechaInicio', $event)"
              dateFormat="dd/mm/yy"
              appendTo="body"
            />
          </label>
          <label for="ct-fin"
            >Fin, si es de plazo fijo
            <p-datepicker
              inputId="ct-fin"
              [ngModel]="f().fechaFin"
              (ngModelChange)="cambiar('fechaFin', $event)"
              dateFormat="dd/mm/yy"
              [showClear]="true"
              appendTo="body"
            />
          </label>
        </div>
        <div class="dos">
          <label for="ct-ban"
            >Banco
            <input
              pInputText
              id="ct-ban"
              maxlength="60"
              [ngModel]="f().banco"
              (ngModelChange)="cambiar('banco', $event)"
            />
          </label>
          <label for="ct-cue"
            >Cuenta para el abono
            <input
              pInputText
              id="ct-cue"
              maxlength="30"
              inputmode="numeric"
              [ngModel]="f().cuenta"
              (ngModelChange)="cambiar('cuenta', $event)"
            />
          </label>
        </div>
        <label class="casilla" for="ct-asig">
          <p-checkbox
            inputId="ct-asig"
            [binary]="true"
            [ngModel]="f().asignacionFamiliar"
            (ngModelChange)="cambiar('asignacionFamiliar', $event)"
          />
          Percibe asignación familiar
        </label>
        @if (editando()) {
          <small class="text-muted"
            >Si la cuenta se deja vacía no cambia. Las condiciones económicas no se pueden editar
            cuando el contrato ya tiene planillas aprobadas.</small
          >
        }
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

    <p-dialog
      header="Finalizar contrato"
      [visible]="!!finalizando()"
      (visibleChange)="$event ? null : finalizando.set(null)"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '24rem', maxWidth: '96vw' }"
    >
      <label class="formulario" for="ct-ffin"
        >Fecha de fin
        <p-datepicker
          inputId="ct-ffin"
          [ngModel]="fechaFin()"
          (ngModelChange)="fechaFin.set($event)"
          dateFormat="dd/mm/yy"
          appendTo="body"
        />
      </label>
      <ng-template #footer>
        <p-button
          label="Cancelar"
          severity="secondary"
          [outlined]="true"
          (onClick)="finalizando.set(null)"
        />
        <p-button
          label="Finalizar"
          icon="pi pi-flag"
          severity="danger"
          [loading]="ocupado()"
          [disabled]="!fechaFin()"
          (onClick)="finalizar()"
        />
      </ng-template>
    </p-dialog>

    <p-dialog
      [header]="'Conceptos de ' + (conceptosDe()?.empleadoNombre ?? '')"
      [visible]="!!conceptosDe()"
      (visibleChange)="$event ? null : conceptosDe.set(null)"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '44rem', maxWidth: '96vw' }"
    >
      <p-table [value]="asignados()" responsiveLayout="scroll">
        <ng-template #header>
          <tr>
            <th>Concepto</th>
            <th class="text-right">Valor</th>
            <th>Desde</th>
            <th>Hasta</th>
            <th style="width: 4rem"></th>
          </tr>
        </ng-template>
        <ng-template #body let-a>
          <tr>
            <td>
              {{ a.conceptoNombre }}
              <small class="block text-muted">{{
                a.tipo === 'INGRESO' ? 'Ingreso' : 'Descuento'
              }}</small>
            </td>
            <td class="text-right">
              {{ a.valor | number: '1.2-2' }}{{ a.modo === 'PORCENTAJE_BASICO' ? ' %' : '' }}
            </td>
            <td>{{ a.vigenteDesde | date: 'd MMM y' }}</td>
            <td>{{ a.vigenteHasta ? (a.vigenteHasta | date: 'd MMM y') : 'Sin fin' }}</td>
            <td class="derecha">
              @if (a.activo) {
                <p-button
                  icon="pi pi-times"
                  [rounded]="true"
                  [text]="true"
                  severity="danger"
                  [ariaLabel]="'Quitar ' + a.conceptoNombre"
                  (onClick)="quitar(a)"
                />
              }
            </td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr>
            <td colspan="5">
              <app-empty-state
                icon="pi-list"
                title="Sin conceptos recurrentes"
                message="Agrega bonos o descuentos que se aplican cada mes."
              />
            </td>
          </tr>
        </ng-template>
      </p-table>
      <h4 class="subseccion">Agregar concepto</h4>
      <div class="formulario">
        <div class="dos">
          <label for="ec-con"
            >Concepto
            <p-select
              inputId="ec-con"
              [options]="conceptos()"
              optionLabel="nombre"
              optionValue="id"
              [ngModel]="nuevoConcepto()"
              (ngModelChange)="nuevoConcepto.set($event)"
              appendTo="body"
            />
          </label>
          <label for="ec-val"
            >Valor
            <p-inputnumber
              inputId="ec-val"
              [ngModel]="nuevoValor()"
              (ngModelChange)="nuevoValor.set($event)"
              [min]="0"
              [minFractionDigits]="2"
              [maxFractionDigits]="2"
            />
          </label>
        </div>
        <label for="ec-desde"
          >Vigente desde
          <p-datepicker
            inputId="ec-desde"
            [ngModel]="nuevoDesde()"
            (ngModelChange)="nuevoDesde.set($event)"
            dateFormat="dd/mm/yy"
            appendTo="body"
          />
        </label>
      </div>
      <ng-template #footer>
        <p-button
          label="Cerrar"
          severity="secondary"
          [outlined]="true"
          (onClick)="conceptosDe.set(null)"
        />
        <p-button
          label="Agregar"
          icon="pi pi-plus"
          [loading]="ocupado()"
          [disabled]="!nuevoConcepto() || nuevoValor() === null || !nuevoDesde()"
          (onClick)="asignar()"
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
export class ContratosLaborales implements OnInit {
  private readonly api = inject(PlanillaApiService);
  private readonly rrhh = inject(RrhhApiService);
  private readonly notificaciones = inject(NotificationService);

  protected readonly regimenes = (Object.keys(ETIQUETA_REGIMEN) as RegimenLaboral[]).map((v) => ({
    value: v,
    label: ETIQUETA_REGIMEN[v],
  }));
  protected readonly pensiones: SistemaPensionario[] = ['ONP', 'AFP'];
  protected readonly lista = signal<ContratoLaboralResponse[]>([]);
  protected readonly empleados = signal<EmpleadoResponse[]>([]);
  protected readonly conceptos = signal<ConceptoPlanillaResponse[]>([]);
  protected readonly cargando = signal(false);
  protected readonly ocupado = signal(false);
  protected readonly dialogo = signal(false);
  protected readonly editando = signal<ContratoLaboralResponse | null>(null);
  protected readonly f = signal<FormularioContrato>(vacio());
  protected readonly finalizando = signal<ContratoLaboralResponse | null>(null);
  protected readonly fechaFin = signal<Date | null>(new Date());
  protected readonly conceptosDe = signal<ContratoLaboralResponse | null>(null);
  protected readonly asignados = signal<EmpleadoConceptoResponse[]>([]);
  protected readonly nuevoConcepto = signal<string | null>(null);
  protected readonly nuevoValor = signal<number | null>(null);
  protected readonly nuevoDesde = signal<Date | null>(new Date());
  protected readonly valido = computed(() => {
    const f = this.f();
    return (
      !!f.empleadoId &&
      !!f.cargo.trim() &&
      !!f.remuneracion &&
      f.remuneracion > 0 &&
      !!f.fechaInicio &&
      (f.sistemaPensionario === 'ONP' || !!f.afpNombre.trim())
    );
  });

  ngOnInit(): void {
    this.cargar();
    this.rrhh.empleados({}).subscribe((e) => this.empleados.set(e));
    this.api
      .conceptos()
      .subscribe((c) => this.conceptos.set(c.filter((x) => !x.sistema && x.activo)));
  }

  protected regimen(r: RegimenLaboral): string {
    return ETIQUETA_REGIMEN[r];
  }

  private cargar(): void {
    this.cargando.set(true);
    this.api.contratos().subscribe({
      next: (l) => {
        this.lista.set(l);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
  }

  protected cambiar<K extends keyof FormularioContrato>(
    clave: K,
    valor: FormularioContrato[K],
  ): void {
    this.f.update((f) => ({ ...f, [clave]: valor }));
    if (clave === 'empleadoId' && !this.editando()) {
      const e = this.empleados().find((x) => x.id === valor);
      if (e) {
        this.f.update((f) => ({ ...f, cargo: e.cargo, remuneracion: e.sueldo ?? f.remuneracion }));
      }
    }
  }

  protected abrirNuevo(): void {
    this.editando.set(null);
    this.f.set(vacio());
    this.dialogo.set(true);
  }

  protected abrirEdicion(c: ContratoLaboralResponse): void {
    this.editando.set(c);
    this.f.set({
      empleadoId: c.empleadoId,
      regimen: c.regimen,
      remuneracion: c.remuneracion,
      asignacionFamiliar: c.asignacionFamiliar,
      sistemaPensionario: c.sistemaPensionario,
      afpNombre: c.afpNombre ?? '',
      horasSemanales: c.horasSemanales,
      banco: c.banco ?? '',
      cuenta: '',
      cargo: c.cargo,
      fechaInicio: new Date(c.fechaInicio + 'T00:00:00'),
      fechaFin: c.fechaFin ? new Date(c.fechaFin + 'T00:00:00') : null,
    });
    this.dialogo.set(true);
  }

  protected guardar(): void {
    const f = this.f();
    const request: ContratoLaboralRequest = {
      empleadoId: f.empleadoId ?? '',
      regimen: f.regimen,
      remuneracion: f.remuneracion ?? 0,
      asignacionFamiliar: f.asignacionFamiliar,
      sistemaPensionario: f.sistemaPensionario,
      afpNombre: f.sistemaPensionario === 'AFP' ? f.afpNombre.trim() : null,
      horasSemanales: f.horasSemanales,
      banco: f.banco.trim() || null,
      cuenta: f.cuenta.trim() || null,
      cargo: f.cargo.trim(),
      fechaInicio: f.fechaInicio ? toIsoDate(f.fechaInicio) : '',
      fechaFin: f.fechaFin ? toIsoDate(f.fechaFin) : null,
    };
    const actual = this.editando();
    this.ocupado.set(true);
    const peticion = actual
      ? this.api.actualizarContrato(actual.id, request)
      : this.api.crearContrato(request);
    peticion.subscribe({
      next: () => {
        this.ocupado.set(false);
        this.dialogo.set(false);
        this.notificaciones.success('El contrato quedó guardado.');
        this.cargar();
      },
      error: () => this.ocupado.set(false),
    });
  }

  protected abrirFin(c: ContratoLaboralResponse): void {
    this.fechaFin.set(new Date());
    this.finalizando.set(c);
  }

  protected finalizar(): void {
    const c = this.finalizando();
    const fecha = this.fechaFin();
    if (!c || !fecha) {
      return;
    }
    this.ocupado.set(true);
    this.api.finalizarContrato(c.id, toIsoDate(fecha)).subscribe({
      next: () => {
        this.ocupado.set(false);
        this.finalizando.set(null);
        this.notificaciones.success('El contrato fue finalizado.');
        this.cargar();
      },
      error: () => this.ocupado.set(false),
    });
  }

  protected abrirConceptos(c: ContratoLaboralResponse): void {
    this.conceptosDe.set(c);
    this.nuevoConcepto.set(null);
    this.nuevoValor.set(null);
    this.recargarConceptos(c.empleadoId);
  }

  private recargarConceptos(empleadoId: string): void {
    this.api.conceptosDeEmpleado(empleadoId).subscribe((a) => this.asignados.set(a));
  }

  protected asignar(): void {
    const c = this.conceptosDe();
    const concepto = this.nuevoConcepto();
    const valor = this.nuevoValor();
    const desde = this.nuevoDesde();
    if (!c || !concepto || valor === null || !desde) {
      return;
    }
    this.ocupado.set(true);
    this.api
      .asignarConcepto(c.empleadoId, {
        conceptoId: concepto,
        valor,
        vigenteDesde: toIsoDate(desde),
      })
      .subscribe({
        next: () => {
          this.ocupado.set(false);
          this.nuevoConcepto.set(null);
          this.nuevoValor.set(null);
          this.notificaciones.success('El concepto quedó asignado.');
          this.recargarConceptos(c.empleadoId);
        },
        error: () => this.ocupado.set(false),
      });
  }

  protected quitar(a: EmpleadoConceptoResponse): void {
    const c = this.conceptosDe();
    if (!c) {
      return;
    }
    this.api.quitarConcepto(c.empleadoId, a.id).subscribe(() => {
      this.notificaciones.success('El concepto se quitó.');
      this.recargarConceptos(c.empleadoId);
    });
  }
}
