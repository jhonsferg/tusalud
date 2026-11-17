import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';
import { Dialog } from 'primeng/dialog';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { Textarea } from 'primeng/textarea';
import { Subject, debounceTime } from 'rxjs';

import {
  ComprobanteResponse,
  ETIQUETA_METODO_CAJA,
  ETIQUETA_ORIGEN,
} from '@tusalud/app/core/models';
import { NotificationService } from '@tusalud/app/core/services/notification.service';
import { CajaStore } from '@tusalud/app/core/state/caja.store';
import { toIsoDate } from '@tusalud/app/core/utils/date';
import { EmptyState } from '@tusalud/app/shared/components/empty-state/empty-state';
import { PageHeader } from '@tusalud/app/shared/components/page-header/page-header';

@Component({
  selector: 'app-comprobantes',
  imports: [
    CurrencyPipe,
    DatePipe,
    FormsModule,
    Button,
    DatePicker,
    Dialog,
    IconField,
    InputIcon,
    InputText,
    Message,
    Select,
    TableModule,
    Tag,
    Textarea,
    EmptyState,
    PageHeader,
  ],
  templateUrl: './comprobantes.html',
  styleUrl: './comprobantes.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Comprobantes implements OnInit {
  protected readonly store = inject(CajaStore);
  private readonly notifications = inject(NotificationService);

  protected readonly busqueda = signal('');
  protected readonly estado = signal<string | null>(null);
  protected readonly rango = signal<Date[] | null>(null);
  private readonly cambio = new Subject<void>();
  protected readonly estados = [
    { label: 'Emitidos', value: 'EMITIDO' },
    { label: 'Anulados', value: 'ANULADO' },
  ];
  protected readonly etiquetaMetodo = ETIQUETA_METODO_CAJA;
  protected readonly etiquetaOrigen = ETIQUETA_ORIGEN;

  protected readonly detalle = signal<ComprobanteResponse | null>(null);
  protected readonly anulando = signal<ComprobanteResponse | null>(null);
  protected readonly motivo = signal('');
  protected readonly procesando = signal(false);
  protected readonly error = signal<string | null>(null);

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

  protected cambiarRango(valor: Date[] | null): void {
    this.rango.set(valor);
    const [inicio, fin] = valor ?? [];
    if (!valor || (inicio && fin)) {
      this.cargar();
    }
  }

  protected cargar(): void {
    const [inicio, fin] = this.rango() ?? [];
    this.store.cargarComprobantes({
      q: this.busqueda().trim() || null,
      estado: this.estado(),
      desde: inicio ? toIsoDate(inicio) : null,
      hasta: fin ? toIsoDate(fin) : null,
    });
  }

  protected abrirAnulacion(comprobante: ComprobanteResponse): void {
    this.anulando.set(comprobante);
    this.motivo.set('');
    this.error.set(null);
  }

  protected anular(): void {
    const comprobante = this.anulando();
    if (!comprobante) {
      return;
    }
    if (!this.motivo().trim()) {
      this.error.set('Indica el motivo de la anulación.');
      return;
    }
    this.procesando.set(true);
    this.store.anular(comprobante.id, this.motivo().trim()).subscribe({
      next: () => {
        this.procesando.set(false);
        this.anulando.set(null);
        this.notifications.success(`El comprobante ${comprobante.codigo} fue anulado.`);
      },
      error: (error: { message: string }) => {
        this.procesando.set(false);
        this.error.set(error.message);
      },
    });
  }

  protected imprimir(): void {
    window.print();
  }
}
