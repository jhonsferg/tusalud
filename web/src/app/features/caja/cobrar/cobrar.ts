import { CurrencyPipe, DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';
import { SelectButton } from 'primeng/selectbutton';
import { TableModule } from 'primeng/table';
import { Subject, debounceTime } from 'rxjs';

import {
  ComprobanteResponse,
  ETIQUETA_METODO_CAJA,
  ETIQUETA_ORIGEN,
  METODOS_CAJA,
  MetodoPagoCaja,
  PendienteCobroResponse,
  TipoComprobante,
  TipoDocumento,
} from '@tusalud/app/core/models';
import { NotificationService } from '@tusalud/app/core/services/notification.service';
import { CajaStore } from '@tusalud/app/core/state/caja.store';
import { EmptyState } from '@tusalud/app/shared/components/empty-state/empty-state';
import { PageHeader } from '@tusalud/app/shared/components/page-header/page-header';

@Component({
  selector: 'app-cobrar',
  imports: [
    CurrencyPipe,
    DatePipe,
    FormsModule,
    ReactiveFormsModule,
    Button,
    Dialog,
    InputNumber,
    InputText,
    Message,
    Select,
    SelectButton,
    TableModule,
    EmptyState,
    PageHeader,
  ],
  templateUrl: './cobrar.html',
  styleUrl: './cobrar.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Cobrar implements OnInit {
  protected readonly store = inject(CajaStore);
  private readonly notifications = inject(NotificationService);
  private readonly fb = inject(FormBuilder).nonNullable;

  protected readonly busqueda = signal('');
  private readonly cambio = new Subject<void>();
  protected readonly etiquetaOrigen = ETIQUETA_ORIGEN;
  protected readonly metodos = METODOS_CAJA.map((valor) => ({
    label: ETIQUETA_METODO_CAJA[valor],
    value: valor,
  }));
  protected readonly tiposComprobante = [
    { label: 'Boleta', value: 'BOLETA' },
    { label: 'Factura', value: 'FACTURA' },
  ];
  protected readonly tiposDocumento = [
    { label: 'DNI', value: 'DNI' },
    { label: 'RUC', value: 'RUC' },
    { label: 'Sin documento', value: 'SIN_DOC' },
  ];

  protected readonly visible = signal(false);
  protected readonly pendiente = signal<PendienteCobroResponse | null>(null);
  protected readonly libre = signal(false);
  protected readonly cobrando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly emitido = signal<ComprobanteResponse | null>(null);

  protected readonly form = this.fb.group({
    tipoComprobante: ['BOLETA' as TipoComprobante],
    clienteTipoDoc: ['DNI' as TipoDocumento],
    clienteDoc: [''],
    clienteNombre: ['', [Validators.required, Validators.maxLength(150)]],
    metodoPago: ['EFECTIVO' as MetodoPagoCaja],
    descripcion: [''],
    cantidad: [1],
    precioUnitario: [0],
  });

  protected readonly total = computed(() => this.pendiente()?.monto ?? 0);

  constructor() {
    this.cambio.pipe(debounceTime(300), takeUntilDestroyed()).subscribe(() => this.cargar());
  }

  ngOnInit(): void {
    this.cargar();
  }

  protected buscar(valor: string): void {
    this.busqueda.set(valor);
    this.cambio.next();
  }

  protected cargar(): void {
    this.store.cargarPendientes(this.busqueda().trim() || null);
  }

  protected abrir(pendiente: PendienteCobroResponse | null): void {
    this.pendiente.set(pendiente);
    this.libre.set(pendiente === null);
    this.error.set(null);
    this.form.reset({
      tipoComprobante: 'BOLETA',
      clienteTipoDoc: 'DNI',
      clienteDoc: pendiente?.dni ?? '',
      clienteNombre: pendiente?.paciente ?? '',
      metodoPago: 'EFECTIVO',
      descripcion: '',
      cantidad: 1,
      precioUnitario: 0,
    });
    this.visible.set(true);
  }

  protected cambiarTipo(tipo: TipoComprobante): void {
    this.form.controls.tipoComprobante.setValue(tipo);
    if (tipo === 'FACTURA') {
      this.form.controls.clienteTipoDoc.setValue('RUC');
    } else if (this.form.controls.clienteTipoDoc.value === 'RUC') {
      this.form.controls.clienteTipoDoc.setValue('DNI');
    }
  }

  protected cobrar(): void {
    this.error.set(null);
    const valor = this.form.getRawValue();
    if (this.form.invalid || (valor.clienteTipoDoc !== 'SIN_DOC' && !valor.clienteDoc.trim())) {
      this.form.markAllAsTouched();
      this.error.set('Indica el nombre del cliente y su documento.');
      return;
    }
    const pendiente = this.pendiente();
    if (!pendiente && (!valor.descripcion.trim() || valor.precioUnitario <= 0)) {
      this.error.set('Describe el servicio e indica un precio mayor a cero.');
      return;
    }
    this.cobrando.set(true);
    this.store
      .cobrar({
        origen: pendiente?.origen ?? 'SERVICIO',
        referenciaId: pendiente?.referenciaId ?? null,
        tipoComprobante: valor.tipoComprobante,
        clienteTipoDoc: valor.clienteTipoDoc,
        clienteDoc: valor.clienteDoc.trim() || null,
        clienteNombre: valor.clienteNombre.trim(),
        metodoPago: valor.metodoPago,
        items: pendiente
          ? null
          : [
              {
                descripcion: valor.descripcion.trim(),
                cantidad: valor.cantidad,
                precioUnitario: valor.precioUnitario,
              },
            ],
      })
      .subscribe({
        next: (comprobante) => {
          this.cobrando.set(false);
          this.visible.set(false);
          this.emitido.set(comprobante);
          this.notifications.success(`Comprobante ${comprobante.codigo} emitido.`);
          this.cargar();
        },
        error: (error: { message: string }) => {
          this.cobrando.set(false);
          this.error.set(error.message);
        },
      });
  }

  protected imprimir(): void {
    window.print();
  }
}
