import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';

import {
  AuthService,
  ComprasApiService,
  ETIQUETA_ESTADO_REQUISICION,
  EstadoRequisicion,
  RequisicionResumen,
  SEVERIDAD_ESTADO_REQUISICION,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { ESTILOS_COMPRAS } from '../compras.estilos';

@Component({
  selector: 'app-requisiciones',
  imports: [
    DatePipe,
    FormsModule,
    RouterLink,
    Button,
    Select,
    TableModule,
    Tag,
    EmptyState,
    PageHeader,
  ],
  template: `
    <app-page-header
      title="Requisiciones"
      subtitle="Pedidos internos de productos que pasan a cotización y orden de compra"
      icon="pi-clipboard"
    >
      @if (puedeSolicitar()) {
        <p-button label="Nueva requisición" icon="pi pi-plus" routerLink="nueva" />
      }
    </app-page-header>

    <section class="panel">
      <div class="filtros">
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
          styleClass="w-full md:w-14rem"
        />
      </div>
      <p-table
        [value]="lista()"
        [loading]="cargando()"
        [paginator]="lista().length > 12"
        [rows]="12"
        [rowHover]="true"
        responsiveLayout="scroll"
      >
        <ng-template #header>
          <tr>
            <th>Requisición</th>
            <th>Solicitante</th>
            <th>Prioridad</th>
            <th class="text-right">Productos</th>
            <th>Estado</th>
            <th style="width: 6rem"></th>
          </tr>
        </ng-template>
        <ng-template #body let-r>
          <tr>
            <td>
              <span class="font-semibold">{{ r.codigo }}</span>
              <small class="block text-muted">{{ r.creadaEn | date: 'd MMM y, HH:mm' }}</small>
            </td>
            <td>
              {{ r.solicitanteNombre }}
              <small class="block text-muted">{{
                r.area === 'FARMACIA' ? 'Farmacia' : 'Almacén'
              }}</small>
            </td>
            <td>
              <p-tag
                [value]="r.prioridad === 'URGENTE' ? 'Urgente' : 'Normal'"
                [severity]="r.prioridad === 'URGENTE' ? 'danger' : 'secondary'"
              />
            </td>
            <td class="text-right">{{ r.cantidadItems }}</td>
            <td><p-tag [value]="etiquetas[r.estado]" [severity]="severidades[r.estado]" /></td>
            <td class="derecha">
              <p-button
                label="Ver"
                size="small"
                [outlined]="true"
                [routerLink]="[r.id]"
                [ariaLabel]="'Ver la requisición ' + r.codigo"
              />
            </td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr>
            <td colspan="6">
              <app-empty-state
                icon="pi-clipboard"
                title="Sin requisiciones"
                message="No hay requisiciones con este filtro."
              />
            </td>
          </tr>
        </ng-template>
      </p-table>
    </section>
  `,
  styles: [ESTILOS_COMPRAS],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Requisiciones implements OnInit {
  private readonly api = inject(ComprasApiService);
  private readonly auth = inject(AuthService);

  protected readonly etiquetas: Record<string, string> = ETIQUETA_ESTADO_REQUISICION;
  protected readonly severidades: Record<
    string,
    'success' | 'info' | 'warn' | 'danger' | 'secondary'
  > = SEVERIDAD_ESTADO_REQUISICION;
  protected readonly opcionesEstado = Object.entries(ETIQUETA_ESTADO_REQUISICION).map(
    ([value, label]) => ({ label, value }),
  );
  protected readonly lista = signal<RequisicionResumen[]>([]);
  protected readonly cargando = signal(false);
  protected readonly estado = signal<EstadoRequisicion | null>(null);
  protected readonly puedeSolicitar = computed(
    () => this.auth.isAlmacen() || this.auth.isFarmacia(),
  );

  ngOnInit(): void {
    this.cargar();
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.api.requisiciones({ estado: this.estado() }).subscribe({
      next: (lista) => {
        this.lista.set(lista);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
  }
}
