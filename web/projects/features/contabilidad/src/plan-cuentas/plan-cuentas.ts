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
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';

import {
  AuthService,
  ContabilidadApiService,
  CuentaContableResponse,
  ETIQUETA_TIPO_CUENTA_CONTABLE,
  NotificationService,
  TipoCuentaContable,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { ESTILOS_CONTABILIDAD } from '../contabilidad.estilos';

@Component({
  selector: 'app-plan-cuentas',
  imports: [
    FormsModule,
    ReactiveFormsModule,
    Button,
    Checkbox,
    Dialog,
    InputText,
    Select,
    TableModule,
    Tag,
    EmptyState,
    PageHeader,
  ],
  template: `
    <app-page-header
      title="Plan contable"
      subtitle="Cuentas del plan contable simplificado. Las cuentas de agrupación no reciben asientos."
      icon="pi-list"
    >
      @if (esFinanzas()) {
        <p-button label="Nueva cuenta" icon="pi pi-plus" (onClick)="abrir(null)" />
      }
    </app-page-header>

    <section class="panel">
      <div class="filtros">
        <input
          pInputText
          type="search"
          placeholder="Buscar por código o nombre"
          aria-label="Buscar cuenta"
          [ngModel]="q()"
          (ngModelChange)="q.set($event)"
        />
        <p-select
          [options]="opcionesTipo"
          optionLabel="label"
          optionValue="value"
          [ngModel]="tipo()"
          (ngModelChange)="tipo.set($event)"
          placeholder="Todos los tipos"
          [showClear]="true"
          ariaLabel="Filtrar por tipo"
          appendTo="body"
        />
      </div>
      <p-table
        [value]="filtradas()"
        [paginator]="filtradas().length > 20"
        [rows]="20"
        [rowHover]="true"
        responsiveLayout="scroll"
      >
        <ng-template #header>
          <tr>
            <th>Código</th>
            <th>Cuenta</th>
            <th>Tipo</th>
            <th>Naturaleza</th>
            <th>Registro</th>
            <th>Estado</th>
            <th style="width: 6rem"></th>
          </tr>
        </ng-template>
        <ng-template #body let-c>
          <tr [class.agregada]="!c.aceptaMovimientos">
            <td>{{ c.codigo }}</td>
            <td>{{ c.nombre }}</td>
            <td>{{ tipos[c.tipo] }}</td>
            <td>{{ c.naturaleza === 'DEUDORA' ? 'Deudora' : 'Acreedora' }}</td>
            <td>{{ c.aceptaMovimientos ? 'Recibe asientos' : 'Agrupación' }}</td>
            <td>
              <p-tag
                [value]="c.activa ? 'Activa' : 'Inactiva'"
                [severity]="c.activa ? 'success' : 'secondary'"
              />
              @if (c.sistema) {
                <small class="block text-muted">Del sistema</small>
              }
            </td>
            <td class="derecha">
              @if (esFinanzas()) {
                <p-button
                  icon="pi pi-pencil"
                  size="small"
                  [text]="true"
                  [rounded]="true"
                  [ariaLabel]="'Editar la cuenta ' + c.codigo"
                  (onClick)="abrir(c)"
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
                title="Sin cuentas"
                message="No hay cuentas con estos filtros."
              />
            </td>
          </tr>
        </ng-template>
      </p-table>
    </section>

    <p-dialog
      [header]="editando() ? 'Editar cuenta ' + editando()!.codigo : 'Nueva cuenta'"
      [visible]="dialogo()"
      (visibleChange)="dialogo.set($event)"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '30rem', maxWidth: '96vw' }"
    >
      <form
        class="formulario"
        [formGroup]="form"
        (submit)="$event.preventDefault(); guardar()"
        novalidate
      >
        @if (!editando()) {
          <label for="cc-codigo"
            >Código
            <input
              pInputText
              id="cc-codigo"
              formControlName="codigo"
              maxlength="10"
              class="w-full"
            />
          </label>
          <label for="cc-tipo"
            >Tipo
            <p-select
              inputId="cc-tipo"
              formControlName="tipo"
              [options]="opcionesTipo"
              optionLabel="label"
              optionValue="value"
              appendTo="body"
            />
          </label>
        }
        <label for="cc-nombre"
          >Nombre
          <input
            pInputText
            id="cc-nombre"
            formControlName="nombre"
            maxlength="120"
            class="w-full"
          />
        </label>
        @if (!editando()?.sistema) {
          <label class="flex align-items-center gap-2" for="cc-mov">
            <p-checkbox inputId="cc-mov" formControlName="aceptaMovimientos" [binary]="true" />
            Recibe asientos
          </label>
        }
        @if (editando() && !editando()!.sistema) {
          <label class="flex align-items-center gap-2" for="cc-activa">
            <p-checkbox inputId="cc-activa" formControlName="activa" [binary]="true" />
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
          (onClick)="dialogo.set(false)"
        />
        <p-button label="Guardar" icon="pi pi-check" [loading]="ocupado()" (onClick)="guardar()" />
      </ng-template>
    </p-dialog>
  `,
  styles: ESTILOS_CONTABILIDAD,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PlanCuentas implements OnInit {
  private readonly api = inject(ContabilidadApiService);
  private readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder).nonNullable;
  private readonly notificaciones = inject(NotificationService);

  protected readonly tipos: Record<string, string> = ETIQUETA_TIPO_CUENTA_CONTABLE;
  protected readonly opcionesTipo = Object.entries(ETIQUETA_TIPO_CUENTA_CONTABLE).map(
    ([value, label]) => ({ label, value }),
  );
  protected readonly cuentas = signal<CuentaContableResponse[]>([]);
  protected readonly q = signal('');
  protected readonly tipo = signal<TipoCuentaContable | null>(null);
  protected readonly dialogo = signal(false);
  protected readonly editando = signal<CuentaContableResponse | null>(null);
  protected readonly ocupado = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly esFinanzas = computed(() => this.auth.isFinanzas());
  protected readonly filtradas = computed(() => {
    const termino = this.q().trim().toLowerCase();
    const tipo = this.tipo();
    return this.cuentas().filter(
      (c) =>
        (!tipo || c.tipo === tipo) &&
        (!termino || c.codigo.includes(termino) || c.nombre.toLowerCase().includes(termino)),
    );
  });

  protected readonly form = this.fb.group({
    codigo: ['', [Validators.required, Validators.pattern(/^\d{2,10}$/)]],
    nombre: ['', [Validators.required, Validators.maxLength(120)]],
    tipo: ['ACTIVO' as TipoCuentaContable, [Validators.required]],
    aceptaMovimientos: [true],
    activa: [true],
  });

  ngOnInit(): void {
    this.cargar();
  }

  private cargar(): void {
    this.api.cuentas().subscribe((c) => this.cuentas.set(c));
  }

  protected abrir(cuenta: CuentaContableResponse | null): void {
    this.error.set(null);
    this.editando.set(cuenta);
    this.form.reset({
      codigo: cuenta?.codigo ?? '',
      nombre: cuenta?.nombre ?? '',
      tipo: cuenta?.tipo ?? 'ACTIVO',
      aceptaMovimientos: cuenta?.aceptaMovimientos ?? true,
      activa: cuenta?.activa ?? true,
    });
    this.dialogo.set(true);
  }

  protected guardar(): void {
    this.error.set(null);
    const actual = this.editando();
    const v = this.form.getRawValue();
    if (this.form.controls.nombre.invalid || (!actual && this.form.invalid)) {
      this.form.markAllAsTouched();
      this.error.set('Revisa el código de 2 a 10 dígitos y el nombre.');
      return;
    }
    this.ocupado.set(true);
    const peticion = actual
      ? this.api.actualizarCuenta(actual.id, {
          nombre: v.nombre.trim(),
          aceptaMovimientos: v.aceptaMovimientos,
          activa: v.activa,
        })
      : this.api.crearCuenta({
          codigo: v.codigo.trim(),
          nombre: v.nombre.trim(),
          tipo: v.tipo,
          aceptaMovimientos: v.aceptaMovimientos,
        });
    peticion.subscribe({
      next: () => {
        this.ocupado.set(false);
        this.dialogo.set(false);
        this.notificaciones.success('La cuenta fue guardada.');
        this.cargar();
      },
      error: () => this.ocupado.set(false),
    });
  }
}
