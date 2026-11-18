import { CurrencyPipe, DatePipe } from '@angular/common';
import { MotivoService } from '@tusalud/ui/dialogo-motivo';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';
import { Dialog } from 'primeng/dialog';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputText } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { Subject, debounceTime } from 'rxjs';

import {
  DispensacionResponse,
  ETIQUETA_METODO_FARMACIA,
  FarmaciaStore,
  NotificationService,
  toIsoDate,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';

@Component({
  selector: 'app-historial-farmacia',
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
    TableModule,
    Tag,
    EmptyState,
    PageHeader,
  ],
  templateUrl: './historial.html',
  styleUrl: './historial.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HistorialFarmacia implements OnInit {
  protected readonly store = inject(FarmaciaStore);
  private readonly motivos = inject(MotivoService);
  private readonly notifications = inject(NotificationService);

  protected readonly busqueda = signal('');
  protected readonly rango = signal<Date[] | null>(null);
  protected readonly detalle = signal<DispensacionResponse | null>(null);
  protected readonly etiquetaMetodo: Readonly<Record<string, string>> = ETIQUETA_METODO_FARMACIA;
  private readonly cambio = new Subject<void>();

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
    const [inicio] = valor ?? [];
    if (!valor || inicio) {
      this.cargar();
    }
  }

  protected cargar(): void {
    const [inicio, fin] = this.rango() ?? [];
    this.store.cargarDispensaciones({
      q: this.busqueda().trim() || null,
      desde: inicio ? toIsoDate(inicio) : null,
      hasta: fin ? toIsoDate(fin) : inicio ? toIsoDate(inicio) : null,
    });
  }

  protected anular(dispensacion: DispensacionResponse): void {
    this.motivos
      .solicitar({
        titulo: 'Anular dispensación',
        descripcion: `Se anulará ${dispensacion.comprobante} y los productos volverán al stock.`,
        etiqueta: 'Motivo de la anulación',
        confirmar: 'Anular dispensación',
        severidad: 'danger',
        icono: 'pi pi-undo',
      })
      .subscribe((motivo) =>
        this.store.anular(dispensacion.id, motivo).subscribe({
          next: () =>
            this.notifications.success('La dispensación fue anulada y el stock devuelto.'),
          error: () => undefined,
        }),
      );
  }

  protected imprimir(): void {
    window.print();
  }
}
