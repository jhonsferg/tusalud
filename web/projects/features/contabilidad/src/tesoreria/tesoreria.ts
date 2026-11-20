import { CurrencyPipe, DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
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
  AuthService,
  ContabilidadApiService,
  CuentaContableResponse,
  CuentaTesoreriaResponse,
  ETIQUETA_ORIGEN_MOVIMIENTO,
  ETIQUETA_TIPO_MOVIMIENTO,
  MovimientoTesoreriaResponse,
  NotificationService,
  TipoCuentaTesoreria,
  TipoMovimientoTesoreria,
  toIsoDate,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { ESTILOS_CONTABILIDAD } from '../contabilidad.estilos';

@Component({
  selector: 'app-tesoreria',
  imports: [
    CurrencyPipe,
    DatePipe,
    FormsModule,
    ReactiveFormsModule,
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
      title="Tesorería"
      subtitle="Cuentas de caja y bancos, y sus movimientos. Cada movimiento manual genera su asiento."
      icon="pi-wallet"
    >
      @if (esFinanzas()) {
        <p-button
          label="Nuevo movimiento"
          icon="pi pi-plus"
          [disabled]="cuentas().length === 0"
          (onClick)="abrirMovimiento()"
        />
        <p-button
          label="Nueva cuenta"
          icon="pi pi-building-columns"
          severity="secondary"
          [outlined]="true"
          (onClick)="abrirCuenta(null)"
        />
      }
    </app-page-header>

    <section class="panel">
      <h3 class="subseccion">Cuentas</h3>
      <p-table [value]="cuentas()" [rowHover]="true" responsiveLayout="scroll">
        <ng-template #header>
          <tr>
            <th>Cuenta</th>
            <th>Tipo</th>
            <th>Cuenta contable</th>
            <th class="text-right">Saldo</th>
            <th>Estado</th>
            <th style="width: 6rem"></th>
          </tr>
        </ng-template>
        <ng-template #body let-c>
          <tr>
            <td>
              <span class="font-semibold">{{ c.nombre }}</span>
              @if (c.banco) {
                <small class="block text-muted"
                  >{{ c.banco }} {{ c.ultimos4 ? '· ****' + c.ultimos4 : '' }}</small
                >
              }
            </td>
            <td>{{ c.tipo === 'CAJA' ? 'Caja' : 'Banco' }}</td>
            <td>{{ c.cuentaContableCodigo }}</td>
            <td class="text-right">{{ c.saldo | currency: 'PEN' : 'symbol-narrow' }}</td>
            <td>
              <p-tag
                [value]="c.activa ? 'Activa' : 'Inactiva'"
                [severity]="c.activa ? 'success' : 'secondary'"
              />
              @if (c.predeterminada) {
                <small class="block text-muted">Recibe cobros y pagos electrónicos</small>
              }
            </td>
            <td class="derecha">
              @if (esFinanzas()) {
                <p-button
                  icon="pi pi-pencil"
                  size="small"
                  [text]="true"
                  [rounded]="true"
                  [ariaLabel]="'Editar la cuenta ' + c.nombre"
                  (onClick)="abrirCuenta(c)"
                />
              }
            </td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr>
            <td colspan="6">
              <app-empty-state
                icon="pi-wallet"
                title="Sin cuentas"
                message="Crea la primera cuenta de caja o banco."
              />
            </td>
          </tr>
        </ng-template>
      </p-table>
    </section>

    <section class="panel">
      <h3 class="subseccion">Movimientos</h3>
      <div class="filtros">
        <p-select
          [options]="opcionesCuenta()"
          optionLabel="label"
          optionValue="value"
          [ngModel]="filtroCuenta()"
          (ngModelChange)="filtroCuenta.set($event); cargarMovimientos()"
          placeholder="Todas las cuentas"
          [showClear]="true"
          ariaLabel="Filtrar por cuenta"
          appendTo="body"
        />
      </div>
      <p-table
        [value]="movimientos()"
        [paginator]="movimientos().length > 12"
        [rows]="12"
        [rowHover]="true"
        responsiveLayout="scroll"
      >
        <ng-template #header>
          <tr>
            <th>Fecha</th>
            <th>Cuenta</th>
            <th>Descripción</th>
            <th>Origen</th>
            <th class="text-right">Monto</th>
            <th>Conciliado</th>
          </tr>
        </ng-template>
        <ng-template #body let-m>
          <tr>
            <td>{{ m.fecha | date: 'd MMM y' }}</td>
            <td>{{ m.cuentaNombre }}</td>
            <td>
              {{ m.descripcion }}
              @if (m.referencia) {
                <small class="block text-muted">{{ m.referencia }}</small>
              }
            </td>
            <td>{{ origenes[m.origen] }}</td>
            <td class="text-right" [style.color]="m.tipo === 'EGRESO' ? 'var(--p-red-600)' : null">
              {{ m.tipo === 'EGRESO' ? '-' : '' }}{{ m.monto | currency: 'PEN' : 'symbol-narrow' }}
            </td>
            <td>
              <p-tag
                [value]="m.conciliado ? 'Sí' : 'No'"
                [severity]="m.conciliado ? 'success' : 'secondary'"
              />
            </td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr>
            <td colspan="6">
              <app-empty-state
                icon="pi-list"
                title="Sin movimientos"
                message="No hay movimientos con este filtro."
              />
            </td>
          </tr>
        </ng-template>
      </p-table>
    </section>

    <p-dialog
      [header]="cuentaEditada() ? 'Editar cuenta' : 'Nueva cuenta de tesorería'"
      [visible]="dialogoCuenta()"
      (visibleChange)="dialogoCuenta.set($event)"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '34rem', maxWidth: '96vw' }"
    >
      <form
        class="formulario"
        [formGroup]="formCuenta"
        (submit)="$event.preventDefault(); guardarCuenta()"
        novalidate
      >
        <label for="ct-nombre"
          >Nombre
          <input
            pInputText
            id="ct-nombre"
            formControlName="nombre"
            maxlength="100"
            class="w-full"
          />
        </label>
        <div class="dos">
          <label for="ct-tipo"
            >Tipo
            <p-select
              inputId="ct-tipo"
              formControlName="tipo"
              [options]="opcionesTipo"
              optionLabel="label"
              optionValue="value"
              appendTo="body"
            />
          </label>
          <label for="ct-contable"
            >Cuenta contable
            <p-select
              inputId="ct-contable"
              formControlName="cuentaContableId"
              [options]="activos()"
              optionLabel="etiqueta"
              optionValue="id"
              [filter]="true"
              filterBy="etiqueta"
              placeholder="Elige una cuenta"
              appendTo="body"
            />
          </label>
        </div>
        <div class="dos">
          <label for="ct-banco"
            >Banco
            <input pInputText id="ct-banco" formControlName="banco" maxlength="60" class="w-full" />
          </label>
          <label for="ct-ult"
            >Últimos 4 dígitos
            <input
              pInputText
              id="ct-ult"
              formControlName="ultimos4"
              maxlength="4"
              inputmode="numeric"
              class="w-full"
            />
          </label>
        </div>
        @if (!cuentaEditada()) {
          <label for="ct-saldo"
            >Saldo inicial
            <p-inputnumber
              inputId="ct-saldo"
              formControlName="saldoInicial"
              mode="currency"
              currency="PEN"
              locale="es-PE"
              [min]="0"
            />
          </label>
        }
        <label class="flex align-items-center gap-2" for="ct-pred">
          <p-checkbox inputId="ct-pred" formControlName="predeterminada" [binary]="true" />
          Cuenta bancaria predeterminada para cobros y pagos electrónicos
        </label>
        @if (cuentaEditada()) {
          <label class="flex align-items-center gap-2" for="ct-activa">
            <p-checkbox inputId="ct-activa" formControlName="activa" [binary]="true" />
            Cuenta activa
          </label>
        }
        @if (error(); as e) {
          <p class="error" role="alert">{{ e }}</p>
        }
      </form>
      <ng-template #footer>
        <p-button
          label="Cancelar"
          severity="secondary"
          [outlined]="true"
          (onClick)="dialogoCuenta.set(false)"
        />
        <p-button
          label="Guardar"
          icon="pi pi-check"
          [loading]="ocupado()"
          (onClick)="guardarCuenta()"
        />
      </ng-template>
    </p-dialog>

    <p-dialog
      header="Nuevo movimiento"
      [visible]="dialogoMovimiento()"
      (visibleChange)="dialogoMovimiento.set($event)"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '36rem', maxWidth: '96vw' }"
    >
      <form
        class="formulario"
        [formGroup]="formMovimiento"
        (submit)="$event.preventDefault(); guardarMovimiento()"
        novalidate
      >
        <div class="dos">
          <label for="mv-cuenta"
            >Cuenta
            <p-select
              inputId="mv-cuenta"
              formControlName="cuentaId"
              [options]="opcionesCuentaActiva()"
              optionLabel="label"
              optionValue="value"
              placeholder="Elige una cuenta"
              appendTo="body"
            />
          </label>
          <label for="mv-tipo"
            >Tipo
            <p-select
              inputId="mv-tipo"
              formControlName="tipo"
              [options]="opcionesMovimiento"
              optionLabel="label"
              optionValue="value"
              appendTo="body"
            />
          </label>
        </div>
        <div class="dos">
          <label for="mv-fecha"
            >Fecha
            <p-datepicker
              inputId="mv-fecha"
              [showOnFocus]="false"
              formControlName="fecha"
              dateFormat="dd/mm/yy"
              [maxDate]="hoy"
              appendTo="body"
            />
          </label>
          <label for="mv-monto"
            >Monto
            <p-inputnumber
              inputId="mv-monto"
              formControlName="monto"
              mode="currency"
              currency="PEN"
              locale="es-PE"
              [min]="0.01"
            />
          </label>
        </div>
        <label for="mv-desc"
          >Descripción
          <input
            pInputText
            id="mv-desc"
            formControlName="descripcion"
            maxlength="200"
            class="w-full"
          />
        </label>
        <label for="mv-ref"
          >Referencia de la operación
          <input
            pInputText
            id="mv-ref"
            formControlName="referencia"
            maxlength="60"
            class="w-full"
          />
        </label>
        <label for="mv-contra"
          >Cuenta contable de contrapartida
          <p-select
            inputId="mv-contra"
            formControlName="contrapartidaId"
            [options]="contrapartidas()"
            optionLabel="etiqueta"
            optionValue="id"
            [filter]="true"
            filterBy="etiqueta"
            placeholder="Elige la cuenta contable"
            appendTo="body"
          />
        </label>
        @if (error(); as e) {
          <p class="error" role="alert">{{ e }}</p>
        }
      </form>
      <ng-template #footer>
        <p-button
          label="Cancelar"
          severity="secondary"
          [outlined]="true"
          (onClick)="dialogoMovimiento.set(false)"
        />
        <p-button
          label="Registrar"
          icon="pi pi-check"
          [loading]="ocupado()"
          (onClick)="guardarMovimiento()"
        />
      </ng-template>
    </p-dialog>
  `,
  styles: ESTILOS_CONTABILIDAD,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Tesoreria implements OnInit {
  private readonly api = inject(ContabilidadApiService);
  private readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder).nonNullable;
  private readonly notificaciones = inject(NotificationService);

  protected readonly origenes: Record<string, string> = ETIQUETA_ORIGEN_MOVIMIENTO;
  protected readonly hoy = new Date();
  protected readonly opcionesTipo = [
    { label: 'Caja', value: 'CAJA' },
    { label: 'Banco', value: 'BANCO' },
  ];
  protected readonly opcionesMovimiento = Object.entries(ETIQUETA_TIPO_MOVIMIENTO).map(
    ([value, label]) => ({ label, value }),
  );

  protected readonly cuentas = signal<CuentaTesoreriaResponse[]>([]);
  protected readonly movimientos = signal<MovimientoTesoreriaResponse[]>([]);
  protected readonly plan = signal<CuentaContableResponse[]>([]);
  protected readonly filtroCuenta = signal<string | null>(null);
  protected readonly dialogoCuenta = signal(false);
  protected readonly dialogoMovimiento = signal(false);
  protected readonly cuentaEditada = signal<CuentaTesoreriaResponse | null>(null);
  protected readonly ocupado = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly esFinanzas = computed(() => this.auth.isFinanzas());
  protected readonly opcionesCuenta = computed(() =>
    this.cuentas().map((c) => ({ label: c.nombre, value: c.id })),
  );
  protected readonly opcionesCuentaActiva = computed(() =>
    this.cuentas()
      .filter((c) => c.activa)
      .map((c) => ({ label: c.nombre, value: c.id })),
  );
  protected readonly activos = computed(() =>
    this.plan()
      .filter((c) => c.activa && c.aceptaMovimientos && c.tipo === 'ACTIVO')
      .map((c) => ({ id: c.id, etiqueta: `${c.codigo} ${c.nombre}` })),
  );
  protected readonly contrapartidas = computed(() =>
    this.plan()
      .filter((c) => c.activa && c.aceptaMovimientos)
      .map((c) => ({ id: c.id, etiqueta: `${c.codigo} ${c.nombre}` })),
  );

  protected readonly formCuenta = this.fb.group({
    nombre: ['', [Validators.required, Validators.maxLength(100)]],
    tipo: ['BANCO' as TipoCuentaTesoreria, [Validators.required]],
    banco: [''],
    ultimos4: ['', [Validators.pattern(/^(\d{4})?$/)]],
    cuentaContableId: ['', [Validators.required]],
    saldoInicial: [0, [Validators.required, Validators.min(0)]],
    predeterminada: [false],
    activa: [true],
  });

  protected readonly formMovimiento = this.fb.group({
    cuentaId: ['', [Validators.required]],
    tipo: ['INGRESO' as TipoMovimientoTesoreria, [Validators.required]],
    fecha: [new Date() as Date | null, [Validators.required]],
    monto: [null as number | null, [Validators.required, Validators.min(0.01)]],
    descripcion: ['', [Validators.required, Validators.maxLength(200)]],
    referencia: [''],
    contrapartidaId: ['', [Validators.required]],
  });

  ngOnInit(): void {
    this.cargarCuentas();
    this.cargarMovimientos();
    this.api.cuentas(true).subscribe((c) => this.plan.set(c));
  }

  private cargarCuentas(): void {
    this.api.cuentasTesoreria().subscribe((c) => this.cuentas.set(c));
  }

  protected cargarMovimientos(): void {
    this.api
      .movimientos({ cuentaId: this.filtroCuenta() })
      .subscribe((m) => this.movimientos.set(m));
  }

  protected abrirCuenta(cuenta: CuentaTesoreriaResponse | null): void {
    this.error.set(null);
    this.cuentaEditada.set(cuenta);
    this.formCuenta.reset({
      nombre: cuenta?.nombre ?? '',
      tipo: cuenta?.tipo ?? 'BANCO',
      banco: cuenta?.banco ?? '',
      ultimos4: cuenta?.ultimos4 ?? '',
      cuentaContableId: cuenta?.cuentaContableId ?? '',
      saldoInicial: cuenta?.saldoInicial ?? 0,
      predeterminada: cuenta?.predeterminada ?? false,
      activa: cuenta?.activa ?? true,
    });
    this.dialogoCuenta.set(true);
  }

  protected guardarCuenta(): void {
    this.error.set(null);
    if (this.formCuenta.invalid) {
      this.formCuenta.markAllAsTouched();
      this.error.set('Revisa el nombre, la cuenta contable y los 4 últimos dígitos.');
      return;
    }
    const v = this.formCuenta.getRawValue();
    const request = {
      nombre: v.nombre.trim(),
      tipo: v.tipo,
      banco: v.banco.trim() || null,
      ultimos4: v.ultimos4.trim() || null,
      cuentaContableId: v.cuentaContableId,
      saldoInicial: v.saldoInicial,
      predeterminada: v.predeterminada,
      activa: this.cuentaEditada() ? v.activa : null,
    };
    const actual = this.cuentaEditada();
    this.ocupado.set(true);
    (actual
      ? this.api.actualizarCuentaTesoreria(actual.id, request)
      : this.api.crearCuentaTesoreria(request)
    ).subscribe({
      next: () => {
        this.ocupado.set(false);
        this.dialogoCuenta.set(false);
        this.notificaciones.success('La cuenta fue guardada.');
        this.cargarCuentas();
      },
      error: () => this.ocupado.set(false),
    });
  }

  protected abrirMovimiento(): void {
    this.error.set(null);
    this.formMovimiento.reset({
      cuentaId: this.cuentas().find((c) => c.activa)?.id ?? '',
      tipo: 'INGRESO',
      fecha: new Date(),
      monto: null,
      descripcion: '',
      referencia: '',
      contrapartidaId: '',
    });
    this.dialogoMovimiento.set(true);
  }

  protected guardarMovimiento(): void {
    this.error.set(null);
    if (this.formMovimiento.invalid) {
      this.formMovimiento.markAllAsTouched();
      this.error.set('Completa la cuenta, el monto, la descripción y la contrapartida.');
      return;
    }
    const v = this.formMovimiento.getRawValue();
    this.ocupado.set(true);
    this.api
      .registrarMovimiento({
        cuentaId: v.cuentaId,
        fecha: toIsoDate(v.fecha as Date),
        tipo: v.tipo,
        monto: v.monto ?? 0,
        descripcion: v.descripcion.trim(),
        referencia: v.referencia.trim() || null,
        contrapartidaId: v.contrapartidaId,
      })
      .subscribe({
        next: () => {
          this.ocupado.set(false);
          this.dialogoMovimiento.set(false);
          this.notificaciones.success('El movimiento fue registrado y contabilizado.');
          this.cargarCuentas();
          this.cargarMovimientos();
        },
        error: () => this.ocupado.set(false),
      });
  }
}
