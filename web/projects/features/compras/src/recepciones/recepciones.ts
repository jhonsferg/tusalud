import { CurrencyPipe, DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import {
  FormBuilder,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { ToggleSwitch } from 'primeng/toggleswitch';

import {
  AuthService,
  ComprasApiService,
  DevolucionResponse,
  NotificationService,
  RecepcionDetalle,
  RecepcionResumen,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { ESTILOS_COMPRAS } from '../compras.estilos';

function lineaDevolucion(fb: NonNullableFormBuilder, recepcionItemId: string, maximo: number) {
  return fb.group({
    recepcionItemId: [recepcionItemId],
    cantidad: [0, [Validators.min(0), Validators.max(maximo)]],
  });
}

@Component({
  selector: 'app-recepciones',
  imports: [
    CurrencyPipe,
    DatePipe,
    FormsModule,
    ReactiveFormsModule,
    RouterLink,
    Button,
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
      title="Recepciones y devoluciones"
      subtitle="Mercadería recibida contra órdenes de compra y devoluciones a proveedores"
      icon="pi-download"
    />

    <section class="panel">
      <div class="filtros">
        <label class="filtros" for="solo-disc">
          <p-toggleswitch
            inputId="solo-disc"
            [ngModel]="soloDiscrepancias()"
            (ngModelChange)="soloDiscrepancias.set($event); cargar()"
          />
          Solo con discrepancias
        </label>
      </div>
      <p-table
        [value]="recepciones()"
        [loading]="cargando()"
        [paginator]="recepciones().length > 12"
        [rows]="12"
        [rowHover]="true"
        responsiveLayout="scroll"
      >
        <ng-template #header>
          <tr>
            <th>Recepción</th>
            <th>Orden</th>
            <th>Proveedor</th>
            <th>Recibida por</th>
            <th>Estado</th>
            <th style="width: 12rem"></th>
          </tr>
        </ng-template>
        <ng-template #body let-r>
          <tr>
            <td>
              {{ r.codigo }}
              <small class="block text-muted">{{ r.recibidaEn | date: 'd MMM y, HH:mm' }}</small>
            </td>
            <td>
              <a [routerLink]="['/compras/ordenes', r.ordenId]">{{ r.ordenCodigo }}</a>
            </td>
            <td>{{ r.proveedorNombre }}</td>
            <td>{{ r.recibidaPorNombre }}</td>
            <td>
              <p-tag
                [value]="r.conDiscrepancia ? 'Con discrepancias' : 'Conforme'"
                [severity]="r.conDiscrepancia ? 'warn' : 'success'"
              />
            </td>
            <td class="derecha">
              @if (esAlmacen()) {
                <p-button
                  label="Devolver"
                  icon="pi pi-undo"
                  size="small"
                  [outlined]="true"
                  [ariaLabel]="'Registrar una devolución de ' + r.codigo"
                  (onClick)="abrirDevolucion(r)"
                />
              }
            </td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr>
            <td colspan="6">
              <app-empty-state
                icon="pi-download"
                title="Sin recepciones"
                message="Todavía no se recibió mercadería con estos filtros."
              />
            </td>
          </tr>
        </ng-template>
      </p-table>
    </section>

    <section class="panel">
      <h3 class="subseccion">Devoluciones a proveedores</h3>
      <p-table [value]="devoluciones()" responsiveLayout="scroll">
        <ng-template #header>
          <tr>
            <th>Devolución</th>
            <th>Proveedor</th>
            <th>Motivo</th>
            <th class="text-right">Monto</th>
            <th>Crédito</th>
          </tr>
        </ng-template>
        <ng-template #body let-d>
          <tr>
            <td>
              {{ d.codigo }}
              <small class="block text-muted"
                >{{ d.registradaEn | date: 'd MMM y' }} · {{ d.recepcionCodigo }}</small
              >
            </td>
            <td>{{ d.proveedorNombre }}</td>
            <td>{{ d.motivo }}</td>
            <td class="text-right">{{ d.monto | currency: 'PEN' : 'symbol-narrow' }}</td>
            <td>
              <p-tag
                [value]="d.comprobanteId ? 'Aplicado' : 'Pendiente de aplicar'"
                [severity]="d.comprobanteId ? 'success' : 'secondary'"
              />
            </td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr>
            <td colspan="5">
              <app-empty-state
                icon="pi-undo"
                title="Sin devoluciones"
                message="No se registraron devoluciones a proveedores."
              />
            </td>
          </tr>
        </ng-template>
      </p-table>
    </section>

    <p-dialog
      [header]="'Devolver mercadería de ' + (recepcionActiva()?.resumen?.codigo ?? '')"
      [visible]="dialogoDevolucion()"
      (visibleChange)="dialogoDevolucion.set($event)"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '36rem', maxWidth: '96vw' }"
    >
      <form
        class="formulario"
        [formGroup]="formDevolucion"
        (submit)="$event.preventDefault(); guardarDevolucion()"
        novalidate
      >
        <label for="dev-motivo"
          >Motivo de la devolución
          <input
            pInputText
            id="dev-motivo"
            formControlName="motivo"
            maxlength="300"
            class="w-full"
          />
        </label>
        <div formArrayName="items">
          @for (fila of lineas.controls; track fila; let i = $index) {
            <div [formGroupName]="i">
              <label>
                {{ disponibles()[i]?.productoNombre }}, lote {{ disponibles()[i]?.numeroLote }}, se
                pueden devolver
                {{ (disponibles()[i]?.recibida ?? 0) - (disponibles()[i]?.devuelta ?? 0) }}
                <p-inputnumber
                  formControlName="cantidad"
                  [min]="0"
                  [useGrouping]="false"
                  [inputId]="'dev-cantidad-' + i"
                  [ariaLabel]="'Cantidad a devolver de ' + disponibles()[i]?.productoNombre"
                />
              </label>
            </div>
          }
        </div>
      </form>
      <ng-template #footer>
        <p-button
          label="Cancelar"
          severity="secondary"
          [outlined]="true"
          (onClick)="dialogoDevolucion.set(false)"
        />
        <p-button
          label="Registrar devolución"
          icon="pi pi-check"
          [loading]="ocupado()"
          (onClick)="guardarDevolucion()"
        />
      </ng-template>
    </p-dialog>
  `,
  styles: [ESTILOS_COMPRAS],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Recepciones implements OnInit {
  private readonly api = inject(ComprasApiService);
  private readonly auth = inject(AuthService);
  private readonly notificaciones = inject(NotificationService);
  private readonly fb = inject(FormBuilder).nonNullable;

  protected readonly recepciones = signal<RecepcionResumen[]>([]);
  protected readonly devoluciones = signal<DevolucionResponse[]>([]);
  protected readonly cargando = signal(false);
  protected readonly ocupado = signal(false);
  protected readonly soloDiscrepancias = signal(false);
  protected readonly dialogoDevolucion = signal(false);
  protected readonly recepcionActiva = signal<RecepcionDetalle | null>(null);
  protected readonly disponibles = signal<RecepcionDetalle['items']>([]);
  protected readonly esAlmacen = computed(() => this.auth.isAlmacen());

  protected readonly formDevolucion = this.fb.group({
    motivo: ['', [Validators.required, Validators.maxLength(300)]],
    items: this.fb.array<ReturnType<typeof lineaDevolucion>>([]),
  });

  protected get lineas() {
    return this.formDevolucion.controls.items;
  }

  ngOnInit(): void {
    this.cargar();
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.api.recepciones({ soloDiscrepancias: this.soloDiscrepancias() }).subscribe({
      next: (lista) => {
        this.recepciones.set(lista);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
    this.api.devoluciones().subscribe((lista) => this.devoluciones.set(lista));
  }

  protected abrirDevolucion(r: RecepcionResumen): void {
    this.api.recepcion(r.id).subscribe((detalle) => {
      const items = detalle.items.filter((i) => i.loteId && i.recibida - i.devuelta > 0);
      if (items.length === 0) {
        this.notificaciones.info('Esta recepción no tiene unidades que devolver.');
        return;
      }
      this.recepcionActiva.set(detalle);
      this.disponibles.set(items);
      this.formDevolucion.reset({ motivo: '' });
      this.lineas.clear();
      for (const i of items) {
        this.lineas.push(lineaDevolucion(this.fb, i.id, i.recibida - i.devuelta));
      }
      this.dialogoDevolucion.set(true);
    });
  }

  protected guardarDevolucion(): void {
    const detalle = this.recepcionActiva();
    if (!detalle || this.formDevolucion.invalid) {
      this.formDevolucion.markAllAsTouched();
      this.notificaciones.warning('Indica el motivo y las cantidades dentro de lo disponible.');
      return;
    }
    const v = this.formDevolucion.getRawValue();
    const items = v.items.filter((i) => (i.cantidad ?? 0) > 0);
    if (items.length === 0) {
      this.notificaciones.warning('Indica al menos una cantidad a devolver.');
      return;
    }
    this.ocupado.set(true);
    this.api
      .devolver({
        recepcionId: detalle.resumen.id,
        motivo: v.motivo.trim(),
        items: items.map((i) => ({ recepcionItemId: i.recepcionItemId, cantidad: i.cantidad })),
      })
      .subscribe({
        next: (d) => {
          this.ocupado.set(false);
          this.dialogoDevolucion.set(false);
          this.notificaciones.success(`Se registró la devolución ${d.codigo}.`);
          this.cargar();
        },
        error: () => this.ocupado.set(false),
      });
  }
}
