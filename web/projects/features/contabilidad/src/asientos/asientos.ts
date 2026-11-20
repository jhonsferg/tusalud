import { CurrencyPipe, DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import {
  FormArray,
  FormBuilder,
  FormsModule,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ConfirmationService } from 'primeng/api';
import { Button } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';
import { Dialog } from 'primeng/dialog';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';

import {
  AsientoResponse,
  AsientoResumen,
  AuthService,
  ContabilidadApiService,
  CuentaContableResponse,
  EstadoAsiento,
  ETIQUETA_ESTADO_ASIENTO,
  ETIQUETA_ORIGEN_ASIENTO,
  NotificationService,
  OrigenAsiento,
  SEVERIDAD_ESTADO_ASIENTO,
  toIsoDate,
} from '@tusalud/core';
import { MotivoService } from '@tusalud/ui/dialogo-motivo';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { ESTILOS_CONTABILIDAD } from '../contabilidad.estilos';
import { redondear } from '../contabilidad.utils';

@Component({
  selector: 'app-asientos',
  imports: [
    CurrencyPipe,
    DatePipe,
    FormsModule,
    ReactiveFormsModule,
    Button,
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
      title="Asientos contables"
      subtitle="Libro diario. Un asiento contabilizado no se edita, se corrige con una reversa."
      icon="pi-book"
    >
      @if (esFinanzas()) {
        <p-button label="Nuevo asiento" icon="pi pi-plus" (onClick)="abrirFormulario(null)" />
      }
    </app-page-header>

    <section class="panel">
      <div class="filtros">
        <input
          pInputText
          type="search"
          placeholder="Buscar por número o glosa"
          aria-label="Buscar asiento"
          [ngModel]="q()"
          (ngModelChange)="q.set($event)"
          (keyup.enter)="cargar()"
        />
        <p-select
          [options]="opcionesEstado"
          optionLabel="label"
          optionValue="value"
          [ngModel]="estado()"
          (ngModelChange)="estado.set($event); cargar()"
          placeholder="Todos los estados"
          [showClear]="true"
          ariaLabel="Filtrar por estado"
          appendTo="body"
        />
        <p-select
          [options]="opcionesOrigen"
          optionLabel="label"
          optionValue="value"
          [ngModel]="origen()"
          (ngModelChange)="origen.set($event); cargar()"
          placeholder="Todos los orígenes"
          [showClear]="true"
          ariaLabel="Filtrar por origen"
          appendTo="body"
        />
        <p-datepicker
          [ngModel]="desde()"
          (ngModelChange)="desde.set($event); cargar()"
          dateFormat="dd/mm/yy"
          placeholder="Desde"
          [showClear]="true"
          ariaLabel="Fecha inicial"
          appendTo="body"
        />
        <p-datepicker
          [ngModel]="hasta()"
          (ngModelChange)="hasta.set($event); cargar()"
          dateFormat="dd/mm/yy"
          placeholder="Hasta"
          [showClear]="true"
          ariaLabel="Fecha final"
          appendTo="body"
        />
      </div>
      <p-table
        [value]="lista()"
        [loading]="cargando()"
        [paginator]="lista().length > 15"
        [rows]="15"
        [rowHover]="true"
        responsiveLayout="scroll"
      >
        <ng-template #header>
          <tr>
            <th>Número</th>
            <th>Fecha</th>
            <th>Glosa</th>
            <th>Origen</th>
            <th class="text-right">Debe</th>
            <th class="text-right">Haber</th>
            <th>Estado</th>
            <th style="width: 5rem"></th>
          </tr>
        </ng-template>
        <ng-template #body let-a>
          <tr>
            <td class="font-semibold">{{ a.numero ?? 'Borrador' }}</td>
            <td>{{ a.fecha | date: 'd MMM y' }}</td>
            <td>{{ a.glosa }}</td>
            <td>{{ origenes[a.origen] }}</td>
            <td class="text-right">{{ a.totalDebe | currency: 'PEN' : 'symbol-narrow' }}</td>
            <td class="text-right">{{ a.totalHaber | currency: 'PEN' : 'symbol-narrow' }}</td>
            <td><p-tag [value]="etiquetas[a.estado]" [severity]="severidades[a.estado]" /></td>
            <td class="derecha">
              <p-button
                icon="pi pi-eye"
                size="small"
                [text]="true"
                [rounded]="true"
                [ariaLabel]="'Ver el asiento ' + (a.numero ?? a.glosa)"
                (onClick)="ver(a)"
              />
            </td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr>
            <td colspan="8">
              <app-empty-state
                icon="pi-book"
                title="Sin asientos"
                message="No hay asientos con estos filtros."
              />
            </td>
          </tr>
        </ng-template>
      </p-table>
    </section>

    <p-dialog
      [header]="'Asiento ' + (detalle()?.numero ?? 'en borrador')"
      [visible]="dialogoDetalle()"
      (visibleChange)="dialogoDetalle.set($event)"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '50rem', maxWidth: '96vw' }"
    >
      @if (detalle(); as a) {
        <dl class="ficha">
          <div>
            <dt>Fecha</dt>
            <dd>{{ a.fecha | date: 'd MMM y' }}</dd>
          </div>
          <div>
            <dt>Periodo</dt>
            <dd>{{ a.periodo }}</dd>
          </div>
          <div>
            <dt>Origen</dt>
            <dd>{{ origenes[a.origen] }}</dd>
          </div>
          <div>
            <dt>Estado</dt>
            <dd>{{ etiquetas[a.estado] }}</dd>
          </div>
          <div>
            <dt>Registrado por</dt>
            <dd>{{ a.creadoPor }}</dd>
          </div>
          @if (a.contabilizadoEn) {
            <div>
              <dt>Contabilizado por</dt>
              <dd>{{ a.contabilizadoPor }}, {{ a.contabilizadoEn | date: 'd MMM y HH:mm' }}</dd>
            </div>
          }
          @if (a.revierteANumero) {
            <div>
              <dt>Revierte al asiento</dt>
              <dd>{{ a.revierteANumero }}, {{ a.motivoReversa }}</dd>
            </div>
          }
        </dl>
        <p class="negrita">{{ a.glosa }}</p>
        <p-table [value]="a.lineas" responsiveLayout="scroll">
          <ng-template #header>
            <tr>
              <th>Cuenta</th>
              <th>Glosa</th>
              <th class="text-right">Debe</th>
              <th class="text-right">Haber</th>
            </tr>
          </ng-template>
          <ng-template #body let-l>
            <tr>
              <td>{{ l.cuentaCodigo }} {{ l.cuentaNombre }}</td>
              <td>{{ l.glosa }}</td>
              <td class="text-right">
                {{ l.debe > 0 ? (l.debe | currency: 'PEN' : 'symbol-narrow') : '' }}
              </td>
              <td class="text-right">
                {{ l.haber > 0 ? (l.haber | currency: 'PEN' : 'symbol-narrow') : '' }}
              </td>
            </tr>
          </ng-template>
          <ng-template #footer>
            <tr>
              <td colspan="2" class="negrita">Totales</td>
              <td class="text-right negrita">
                {{ a.totalDebe | currency: 'PEN' : 'symbol-narrow' }}
              </td>
              <td class="text-right negrita">
                {{ a.totalHaber | currency: 'PEN' : 'symbol-narrow' }}
              </td>
            </tr>
          </ng-template>
        </p-table>
      }
      <ng-template #footer>
        @if (esFinanzas() && detalle(); as a) {
          @if (a.estado === 'BORRADOR') {
            <p-button
              label="Eliminar"
              icon="pi pi-trash"
              severity="danger"
              [outlined]="true"
              (onClick)="eliminar(a)"
            />
            <p-button
              label="Editar"
              icon="pi pi-pencil"
              severity="secondary"
              [outlined]="true"
              (onClick)="editar(a)"
            />
            <p-button
              label="Contabilizar"
              icon="pi pi-check"
              [loading]="ocupado()"
              (onClick)="contabilizar(a)"
            />
          }
          @if (a.estado === 'CONTABILIZADO' && a.origen !== 'REVERSA') {
            <p-button
              label="Reversar"
              icon="pi pi-undo"
              severity="danger"
              [outlined]="true"
              (onClick)="reversar(a)"
            />
          }
        }
      </ng-template>
    </p-dialog>

    <p-dialog
      [header]="editandoId() ? 'Editar borrador' : 'Nuevo asiento'"
      [visible]="dialogoForm()"
      (visibleChange)="dialogoForm.set($event)"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '62rem', maxWidth: '98vw' }"
    >
      <form
        class="formulario"
        [formGroup]="form"
        (submit)="$event.preventDefault(); guardar()"
        novalidate
      >
        <div class="dos">
          <label for="as-fecha"
            >Fecha
            <p-datepicker
              inputId="as-fecha"
              [showOnFocus]="false"
              formControlName="fecha"
              dateFormat="dd/mm/yy"
              [maxDate]="hoy"
              appendTo="body"
            />
          </label>
          <label for="as-glosa"
            >Glosa
            <input
              pInputText
              id="as-glosa"
              formControlName="glosa"
              maxlength="200"
              class="w-full"
            />
          </label>
        </div>
        <div formArrayName="lineas">
          @for (linea of lineas.controls; track linea; let i = $index) {
            <div class="linea" [formGroupName]="i">
              <label
                >Cuenta
                <p-select
                  formControlName="cuentaId"
                  [options]="cuentasMovimiento()"
                  optionLabel="etiqueta"
                  optionValue="id"
                  [filter]="true"
                  filterBy="etiqueta"
                  placeholder="Elige una cuenta"
                  [ariaLabel]="'Cuenta de la línea ' + (i + 1)"
                  appendTo="body"
                />
              </label>
              <label
                >Debe
                <p-inputnumber
                  formControlName="debe"
                  mode="decimal"
                  [minFractionDigits]="2"
                  [maxFractionDigits]="2"
                  [min]="0"
                  [ariaLabel]="'Debe de la línea ' + (i + 1)"
                />
              </label>
              <label
                >Haber
                <p-inputnumber
                  formControlName="haber"
                  mode="decimal"
                  [minFractionDigits]="2"
                  [maxFractionDigits]="2"
                  [min]="0"
                  [ariaLabel]="'Haber de la línea ' + (i + 1)"
                />
              </label>
              <label
                >Glosa de la línea
                <input pInputText formControlName="glosa" maxlength="200" class="w-full" />
              </label>
              <p-button
                icon="pi pi-times"
                severity="danger"
                [text]="true"
                [rounded]="true"
                [disabled]="lineas.length <= 2"
                [ariaLabel]="'Quitar la línea ' + (i + 1)"
                (onClick)="quitarLinea(i)"
              />
            </div>
          }
        </div>
        <div>
          <p-button
            label="Agregar línea"
            icon="pi pi-plus"
            size="small"
            severity="secondary"
            [outlined]="true"
            (onClick)="agregarLinea()"
          />
        </div>
        <div class="cuadre" aria-live="polite">
          <span>Debe {{ totales().debe | currency: 'PEN' : 'symbol-narrow' }}</span>
          <span>Haber {{ totales().haber | currency: 'PEN' : 'symbol-narrow' }}</span>
          <span [class.ok]="totales().cuadra" [class.mal]="!totales().cuadra">
            {{ totales().cuadra ? 'Cuadra' : 'Diferencia ' + totales().diferencia.toFixed(2) }}
          </span>
        </div>
        @if (error(); as e) {
          <p class="error" role="alert">{{ e }}</p>
        }
      </form>
      <ng-template #footer>
        <p-button
          label="Cancelar"
          severity="secondary"
          [outlined]="true"
          (onClick)="dialogoForm.set(false)"
        />
        <p-button
          label="Guardar borrador"
          icon="pi pi-save"
          [loading]="ocupado()"
          (onClick)="guardar()"
        />
      </ng-template>
    </p-dialog>
  `,
  styles: ESTILOS_CONTABILIDAD,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Asientos implements OnInit {
  private readonly api = inject(ContabilidadApiService);
  private readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder).nonNullable;
  private readonly notificaciones = inject(NotificationService);
  private readonly motivos = inject(MotivoService);
  private readonly confirmacion = inject(ConfirmationService);

  protected readonly etiquetas: Record<string, string> = ETIQUETA_ESTADO_ASIENTO;
  protected readonly severidades: Record<string, 'secondary' | 'success' | 'warn'> =
    SEVERIDAD_ESTADO_ASIENTO;
  protected readonly origenes: Record<string, string> = ETIQUETA_ORIGEN_ASIENTO;
  protected readonly opcionesEstado = Object.entries(ETIQUETA_ESTADO_ASIENTO).map(
    ([value, label]) => ({ label, value }),
  );
  protected readonly opcionesOrigen = Object.entries(ETIQUETA_ORIGEN_ASIENTO).map(
    ([value, label]) => ({ label, value }),
  );
  protected readonly hoy = new Date();

  protected readonly lista = signal<AsientoResumen[]>([]);
  protected readonly cuentas = signal<CuentaContableResponse[]>([]);
  protected readonly cuentasMovimiento = computed(() =>
    this.cuentas()
      .filter((c) => c.activa && c.aceptaMovimientos)
      .map((c) => ({ id: c.id, etiqueta: `${c.codigo} ${c.nombre}` })),
  );
  protected readonly detalle = signal<AsientoResponse | null>(null);
  protected readonly cargando = signal(false);
  protected readonly ocupado = signal(false);
  protected readonly q = signal('');
  protected readonly estado = signal<EstadoAsiento | null>(null);
  protected readonly origen = signal<OrigenAsiento | null>(null);
  protected readonly desde = signal<Date | null>(null);
  protected readonly hasta = signal<Date | null>(null);
  protected readonly dialogoDetalle = signal(false);
  protected readonly dialogoForm = signal(false);
  protected readonly editandoId = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly esFinanzas = computed(() => this.auth.isFinanzas());

  protected readonly form = this.fb.group({
    fecha: [new Date() as Date | null, [Validators.required]],
    glosa: ['', [Validators.required, Validators.maxLength(200)]],
    lineas: this.fb.array([this.nuevaLinea(), this.nuevaLinea()]),
  });

  protected get lineas(): FormArray {
    return this.form.controls.lineas;
  }

  private readonly cambios = toSignal(this.form.valueChanges, { initialValue: this.form.value });
  protected readonly totales = computed(() => {
    const lineas = (this.cambios().lineas ?? []) as { debe: number | null; haber: number | null }[];
    const debe = redondear(lineas.reduce((s, l) => s + (l.debe ?? 0), 0));
    const haber = redondear(lineas.reduce((s, l) => s + (l.haber ?? 0), 0));
    return {
      debe,
      haber,
      diferencia: redondear(Math.abs(debe - haber)),
      cuadra: debe === haber && debe > 0,
    };
  });

  private nuevaLinea() {
    return this.fb.group({
      cuentaId: ['', [Validators.required]],
      debe: [null as number | null],
      haber: [null as number | null],
      glosa: [''],
    });
  }

  ngOnInit(): void {
    this.cargar();
    this.api.cuentas(true).subscribe((c) => this.cuentas.set(c));
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.api
      .asientos({
        q: this.q().trim() || null,
        estado: this.estado(),
        origen: this.origen(),
        desde: this.desde() ? toIsoDate(this.desde()!) : null,
        hasta: this.hasta() ? toIsoDate(this.hasta()!) : null,
      })
      .subscribe({
        next: (lista) => {
          this.lista.set(lista);
          this.cargando.set(false);
        },
        error: () => this.cargando.set(false),
      });
  }

  protected ver(a: AsientoResumen): void {
    this.api.asiento(a.id).subscribe((detalle) => {
      this.detalle.set(detalle);
      this.dialogoDetalle.set(true);
    });
  }

  protected abrirFormulario(a: AsientoResponse | null): void {
    this.error.set(null);
    this.editandoId.set(a?.id ?? null);
    this.lineas.clear();
    const origen = a?.lineas ?? [null, null];
    for (const l of origen) {
      const grupo = this.nuevaLinea();
      if (l) {
        grupo.patchValue({
          cuentaId: l.cuentaId,
          debe: l.debe > 0 ? l.debe : null,
          haber: l.haber > 0 ? l.haber : null,
          glosa: l.glosa ?? '',
        });
      }
      this.lineas.push(grupo);
    }
    this.form.patchValue({
      fecha: a ? new Date(`${a.fecha}T00:00:00`) : new Date(),
      glosa: a?.glosa ?? '',
    });
    this.dialogoDetalle.set(false);
    this.dialogoForm.set(true);
  }

  protected editar(a: AsientoResponse): void {
    this.abrirFormulario(a);
  }

  protected agregarLinea(): void {
    this.lineas.push(this.nuevaLinea());
  }

  protected quitarLinea(i: number): void {
    if (this.lineas.length > 2) {
      this.lineas.removeAt(i);
    }
  }

  protected guardar(): void {
    this.error.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.error.set('Indica la fecha, la glosa y una cuenta en cada línea.');
      return;
    }
    const v = this.form.getRawValue();
    const lineas = (
      v.lineas as { cuentaId: string; debe: number | null; haber: number | null; glosa: string }[]
    ).map((l) => ({
      cuentaId: l.cuentaId,
      debe: l.debe && l.debe > 0 ? l.debe : null,
      haber: l.haber && l.haber > 0 ? l.haber : null,
      glosa: l.glosa.trim() || null,
    }));
    if (lineas.some((l) => (l.debe === null) === (l.haber === null))) {
      this.error.set('Cada línea debe tener un importe en debe o en haber, no ambos.');
      return;
    }
    const request = { fecha: toIsoDate(v.fecha as Date), glosa: v.glosa.trim(), lineas };
    const id = this.editandoId();
    this.ocupado.set(true);
    (id ? this.api.actualizarAsiento(id, request) : this.api.crearAsiento(request)).subscribe({
      next: (a) => {
        this.ocupado.set(false);
        this.dialogoForm.set(false);
        this.notificaciones.success('El borrador fue guardado. Contabilízalo para registrarlo.');
        this.cargar();
        this.detalle.set(a);
        this.dialogoDetalle.set(true);
      },
      error: () => this.ocupado.set(false),
    });
  }

  protected contabilizar(a: AsientoResponse): void {
    this.ocupado.set(true);
    this.api.contabilizar(a.id).subscribe({
      next: (r) => {
        this.ocupado.set(false);
        this.detalle.set(r);
        this.notificaciones.success(`El asiento ${r.numero} fue contabilizado.`);
        this.cargar();
      },
      error: () => this.ocupado.set(false),
    });
  }

  protected eliminar(a: AsientoResponse): void {
    this.confirmacion.confirm({
      header: 'Eliminar borrador',
      message: '¿Quieres eliminar este borrador? No se puede recuperar.',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Eliminar',
      rejectLabel: 'Cancelar',
      acceptButtonProps: { severity: 'danger' },
      rejectButtonProps: { severity: 'secondary', outlined: true },
      accept: () =>
        this.api.eliminarAsiento(a.id).subscribe(() => {
          this.dialogoDetalle.set(false);
          this.notificaciones.success('El borrador fue eliminado.');
          this.cargar();
        }),
    });
  }

  protected reversar(a: AsientoResponse): void {
    this.motivos
      .solicitar({
        titulo: 'Reversar asiento',
        descripcion: `Se generará un contraasiento que anula los efectos de ${a.numero}.`,
        etiqueta: 'Motivo de la reversa',
        confirmar: 'Reversar asiento',
        severidad: 'danger',
        minimo: 5,
      })
      .subscribe((motivo) =>
        this.api.reversar(a.id, motivo).subscribe((r) => {
          this.detalle.set(r);
          this.notificaciones.success(`Se generó la reversa ${r.numero}.`);
          this.cargar();
        }),
      );
  }
}
