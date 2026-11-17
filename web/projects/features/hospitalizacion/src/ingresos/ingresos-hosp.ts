import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { Subject, debounceTime } from 'rxjs';

import { HospitalizacionStore } from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';

@Component({
  selector: 'app-ingresos-hosp',
  imports: [
    CurrencyPipe,
    DatePipe,
    FormsModule,
    RouterLink,
    Button,
    IconField,
    InputIcon,
    InputText,
    Select,
    TableModule,
    Tag,
    EmptyState,
    PageHeader,
  ],
  templateUrl: './ingresos-hosp.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class IngresosHosp implements OnInit {
  protected readonly store = inject(HospitalizacionStore);
  protected readonly estado = signal<string | null>('ACTIVO');
  protected readonly busqueda = signal('');
  private readonly cambio = new Subject<void>();
  protected readonly estados = [
    { label: 'Activos', value: 'ACTIVO' },
    { label: 'Con alta', value: 'ALTA' },
    { label: 'Cancelados', value: 'CANCELADO' },
  ];

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
    this.store.cargarIngresos(this.estado(), this.busqueda().trim() || null);
  }
}
