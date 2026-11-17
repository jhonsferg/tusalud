import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ConfirmationService } from 'primeng/api';
import { Button } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';
import { Dialog } from 'primeng/dialog';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputText } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { Subject, debounceTime } from 'rxjs';

import { DispensacionResponse, ETIQUETA_METODO_FARMACIA } from '@tusalud/app/core/models';
import { NotificationService } from '@tusalud/app/core/services/notification.service';
import { FarmaciaStore } from '@tusalud/app/core/state/farmacia.store';
import { toIsoDate } from '@tusalud/app/core/utils/date';
import { EmptyState } from '@tusalud/app/shared/components/empty-state/empty-state';
import { PageHeader } from '@tusalud/app/shared/components/page-header/page-header';

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
  private readonly confirmation = inject(ConfirmationService);
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
    const [inicio, fin] = valor ?? [];
    if (!valor || (inicio && fin)) {
      this.cargar();
    }
  }

  protected cargar(): void {
    const [inicio, fin] = this.rango() ?? [];
    this.store.cargarDispensaciones({
      q: this.busqueda().trim() || null,
      desde: inicio ? toIsoDate(inicio) : null,
      hasta: fin ? toIsoDate(fin) : null,
    });
  }

  protected anular(dispensacion: DispensacionResponse): void {
    this.confirmation.confirm({
      header: 'Anular dispensación',
      message: `Se anulará ${dispensacion.comprobante} y los productos volverán al stock.`,
      icon: 'pi pi-undo',
      acceptLabel: 'Anular',
      rejectLabel: 'Cancelar',
      acceptButtonProps: { severity: 'danger' },
      rejectButtonProps: { severity: 'secondary', outlined: true },
      accept: () =>
        this.store.anular(dispensacion.id).subscribe({
          next: () =>
            this.notifications.success('La dispensación fue anulada y el stock devuelto.'),
          error: () => undefined,
        }),
    });
  }

  protected imprimir(): void {
    window.print();
  }
}
