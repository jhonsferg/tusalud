import { ChangeDetectionStrategy, Component, OnInit, inject, input, signal } from '@angular/core';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Button } from 'primeng/button';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';

import {
  AlmacenApiService,
  ComprasApiService,
  NotificationService,
  ProductoResponse,
  RequisicionDetalle,
  RequisicionRequest,
} from '@tusalud/core';
import { PageHeader } from '@tusalud/ui/page-header';
import { ESTILOS_COMPRAS } from '../compras.estilos';

@Component({
  selector: 'app-requisicion-form',
  imports: [ReactiveFormsModule, Button, InputNumber, InputText, Select, PageHeader],
  template: `
    <app-page-header
      [title]="id() ? 'Editar requisición' : 'Nueva requisición'"
      subtitle="Indica qué productos necesitas y cuántos"
      icon="pi-clipboard"
      backLink="/compras/requisiciones"
      backLabel="Requisiciones"
    />

    <form class="panel formulario" [formGroup]="form" (submit)="$event.preventDefault()" novalidate>
      <div class="dos">
        <label for="req-prioridad"
          >Prioridad
          <p-select
            inputId="req-prioridad"
            formControlName="prioridad"
            [options]="prioridades"
            optionLabel="label"
            optionValue="value"
            appendTo="body"
          />
        </label>
        <label for="req-motivo"
          >Motivo
          <input
            pInputText
            id="req-motivo"
            formControlName="motivo"
            maxlength="300"
            class="w-full"
          />
        </label>
      </div>

      <div class="filtros">
        <h3 class="subseccion">Productos</h3>
        <p-button
          label="Cargar sugeridos por stock bajo"
          icon="pi pi-bolt"
          severity="secondary"
          [outlined]="true"
          size="small"
          [loading]="cargandoSugeridas()"
          (onClick)="cargarSugeridas()"
        />
      </div>

      <div formArrayName="items">
        @for (fila of items.controls; track fila; let i = $index) {
          <div class="linea simple" [formGroupName]="i">
            <label
              >Producto
              <p-select
                formControlName="productoId"
                [options]="productos()"
                optionLabel="nombre"
                optionValue="id"
                [filter]="true"
                filterBy="nombre,codigo"
                placeholder="Elige un producto"
                appendTo="body"
                [ariaLabel]="'Producto ' + (i + 1)"
              />
            </label>
            <label
              >Cantidad
              <p-inputnumber
                formControlName="cantidad"
                [min]="1"
                [max]="100000"
                [useGrouping]="false"
                [inputId]="'req-cantidad-' + i"
                [ariaLabel]="'Cantidad del producto ' + (i + 1)"
              />
            </label>
            <p-button
              icon="pi pi-trash"
              severity="danger"
              [text]="true"
              [rounded]="true"
              [ariaLabel]="'Quitar el producto ' + (i + 1)"
              (onClick)="quitar(i)"
            />
          </div>
        }
      </div>
      <div>
        <p-button
          label="Agregar producto"
          icon="pi pi-plus"
          severity="secondary"
          [outlined]="true"
          size="small"
          (onClick)="agregar()"
        />
      </div>
      @if (error(); as mensaje) {
        <p class="text-red-600" role="alert">{{ mensaje }}</p>
      }
      <div class="acciones">
        <p-button
          label="Guardar borrador"
          icon="pi pi-save"
          [loading]="guardando()"
          (onClick)="guardar(false)"
        />
        <p-button
          label="Guardar y enviar"
          icon="pi pi-send"
          severity="secondary"
          [loading]="guardando()"
          (onClick)="guardar(true)"
        />
      </div>
    </form>
  `,
  styles: [ESTILOS_COMPRAS],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RequisicionForm implements OnInit {
  readonly id = input<string>();

  private readonly api = inject(ComprasApiService);
  private readonly almacen = inject(AlmacenApiService);
  private readonly notificaciones = inject(NotificationService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder).nonNullable;

  protected readonly prioridades = [
    { label: 'Normal', value: 'NORMAL' },
    { label: 'Urgente', value: 'URGENTE' },
  ];
  protected readonly productos = signal<ProductoResponse[]>([]);
  protected readonly cargandoSugeridas = signal(false);
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);
  private sugerida = false;

  protected readonly form = this.fb.group({
    prioridad: ['NORMAL'],
    motivo: ['', [Validators.maxLength(300)]],
    items: this.fb.array([this.nuevaLinea()]),
  });

  protected get items() {
    return this.form.controls.items;
  }

  ngOnInit(): void {
    this.almacen.listarProductos({ activo: true }).subscribe((lista) => this.productos.set(lista));
    const id = this.id();
    if (id) {
      this.api.requisicion(id).subscribe((detalle) => this.cargar(detalle));
    }
  }

  protected agregar(): void {
    this.items.push(this.nuevaLinea());
  }

  protected quitar(indice: number): void {
    this.items.removeAt(indice);
    if (this.items.length === 0) {
      this.agregar();
    }
  }

  protected cargarSugeridas(): void {
    this.cargandoSugeridas.set(true);
    this.api.sugeridas().subscribe({
      next: (sugeridas) => {
        this.cargandoSugeridas.set(false);
        if (sugeridas.length === 0) {
          this.notificaciones.info('No hay productos por debajo de su stock mínimo.');
          return;
        }
        this.items.clear();
        for (const s of sugeridas) {
          this.items.push(this.nuevaLinea(s.productoId, s.cantidadSugerida));
        }
        this.sugerida = true;
        this.form.controls.motivo.setValue('Reposición de productos con stock bajo');
      },
      error: () => this.cargandoSugeridas.set(false),
    });
  }

  protected guardar(enviar: boolean): void {
    this.error.set(null);
    const valor = this.form.getRawValue();
    const items = (valor.items as { productoId: string | null; cantidad: number | null }[])
      .filter((i) => i.productoId)
      .map((i) => ({ productoId: i.productoId as string, cantidad: i.cantidad ?? 1 }));
    if (items.length === 0) {
      this.error.set('Agrega al menos un producto.');
      return;
    }
    const request: RequisicionRequest = {
      prioridad: valor.prioridad as 'NORMAL' | 'URGENTE',
      motivo: valor.motivo.trim() || null,
      sugerida: this.sugerida,
      items,
    };
    this.guardando.set(true);
    const id = this.id();
    const operacion = id
      ? this.api.actualizarRequisicion(id, request)
      : this.api.crearRequisicion(request);
    operacion.subscribe({
      next: (detalle) => {
        if (enviar) {
          this.api.enviarRequisicion(detalle.resumen.id).subscribe({
            next: (enviada) => this.terminar(enviada, 'La requisición fue enviada.'),
            error: () => this.terminar(detalle, 'La requisición quedó en borrador.'),
          });
        } else {
          this.terminar(detalle, 'La requisición quedó en borrador.');
        }
      },
      error: () => this.guardando.set(false),
    });
  }

  private terminar(detalle: RequisicionDetalle, mensaje: string): void {
    this.guardando.set(false);
    this.notificaciones.success(mensaje);
    void this.router.navigate(['/compras/requisiciones', detalle.resumen.id]);
  }

  private cargar(detalle: RequisicionDetalle): void {
    this.form.controls.prioridad.setValue(detalle.resumen.prioridad);
    this.form.controls.motivo.setValue(detalle.resumen.motivo ?? '');
    this.items.clear();
    for (const item of detalle.items) {
      this.items.push(this.nuevaLinea(item.productoId, item.cantidad));
    }
  }

  private nuevaLinea(productoId: string | null = null, cantidad = 1) {
    return this.fb.group({
      productoId: [productoId as string | null, [Validators.required]],
      cantidad: [cantidad, [Validators.required, Validators.min(1)]],
    });
  }
}
