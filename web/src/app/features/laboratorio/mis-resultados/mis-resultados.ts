import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { Tag } from 'primeng/tag';

import { ETIQUETA_ESTADO_ORDEN, OrdenResponse } from '@tusalud/app/core/models';
import { LaboratorioStore } from '@tusalud/app/core/state/laboratorio.store';
import { EmptyState } from '@tusalud/app/shared/components/empty-state/empty-state';
import { PageHeader } from '@tusalud/app/shared/components/page-header/page-header';
import {
  ICONO_ORDEN,
  SEVERIDAD_ORDEN,
  rangoReferencia,
} from '@tusalud/app/shared/utils/laboratorio';

@Component({
  selector: 'app-mis-resultados',
  imports: [DatePipe, Button, Dialog, Tag, EmptyState, PageHeader],
  templateUrl: './mis-resultados.html',
  styleUrl: './mis-resultados.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MisResultados implements OnInit {
  protected readonly store = inject(LaboratorioStore);
  protected readonly seleccionada = signal<OrdenResponse | null>(null);

  protected readonly etiqueta: Readonly<Record<string, string>> = ETIQUETA_ESTADO_ORDEN;
  protected readonly severidad: Readonly<
    Record<string, 'success' | 'info' | 'warn' | 'danger' | 'secondary'>
  > = SEVERIDAD_ORDEN;
  protected readonly icono: Readonly<Record<string, string>> = ICONO_ORDEN;
  protected readonly rango = rangoReferencia;

  ngOnInit(): void {
    this.store.cargarMisOrdenes();
  }

  protected fuera(orden: OrdenResponse): number {
    return orden.items.reduce(
      (suma, item) => suma + item.resultados.filter((resultado) => resultado.fueraDeRango).length,
      0,
    );
  }

  protected imprimir(): void {
    window.print();
  }
}
