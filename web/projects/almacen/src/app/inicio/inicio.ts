import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import {
  AlertasAlmacenResponse,
  AlmacenApiService,
  AsyncState,
  AuthService,
  MovimientoResponse,
  ResumenAlmacenResponse,
} from '@tusalud/core';
import { PageHeader } from '@tusalud/ui/page-header';
import {
  AccionRapida,
  ItemTarea,
  QuickActions,
  SectionCard,
  StatCard,
  TaskList,
  fechaLarga,
  hoyIso,
  saludoDelDia,
} from '@tusalud/ui/tablero';

@Component({
  selector: 'app-inicio-almacen',
  imports: [CurrencyPipe, RouterLink, PageHeader, SectionCard, StatCard, TaskList, QuickActions],
  template: `
    <app-page-header [title]="saludo()" [subtitle]="fecha()" icon="pi-box" />

    <div class="fila-stats stats">
      <app-stat-card
        etiqueta="Productos"
        [valor]="resumen.value()?.totalProductos ?? 0"
        icono="pi-box"
        enlace="/almacen/productos"
      />
      <app-stat-card
        etiqueta="Bajo stock mínimo"
        [valor]="resumen.value()?.productosBajoStock ?? 0"
        icono="pi-exclamation-triangle"
        [tono]="(resumen.value()?.productosBajoStock ?? 0) > 0 ? 'aviso' : 'exito'"
        enlace="/almacen/productos"
      />
      <app-stat-card
        etiqueta="Lotes por vencer"
        [valor]="resumen.value()?.lotesPorVencer ?? 0"
        icono="pi-clock"
        [tono]="(resumen.value()?.lotesPorVencer ?? 0) > 0 ? 'aviso' : 'exito'"
        detalle="En los próximos 60 días"
      />
      <app-stat-card
        etiqueta="Lotes vencidos"
        [valor]="resumen.value()?.lotesVencidos ?? 0"
        icono="pi-ban"
        [tono]="(resumen.value()?.lotesVencidos ?? 0) > 0 ? 'peligro' : 'exito'"
      />
      <app-stat-card
        etiqueta="Valor del inventario"
        [valor]="(resumen.value()?.valorInventario ?? 0 | currency: 'PEN' : 'symbol-narrow') ?? ''"
        icono="pi-dollar"
      />
    </div>

    <div class="tablero">
      <div class="c7">
        <app-section-card
          titulo="Alertas de inventario"
          icono="pi-bell"
          subtitulo="Reposición y vencimientos que requieren acción"
          [cargando]="alertas.loading() && !alertas.hasValue()"
          [error]="alertas.error()?.message ?? null"
          [vacio]="listaAlertas().length === 0 && alertas.hasValue()"
          textoVacio="Todo el inventario está en orden."
        >
          <app-task-list [items]="listaAlertas()" />
          <a pie routerLink="/almacen/productos">Revisar productos</a>
        </app-section-card>
      </div>

      <div class="c5">
        <app-section-card
          titulo="Movimientos de hoy"
          icono="pi-arrow-right-arrow-left"
          [cargando]="movimientos.loading() && !movimientos.hasValue()"
          [vacio]="listaMovimientos().length === 0 && movimientos.hasValue()"
          textoVacio="Sin movimientos registrados hoy."
        >
          <app-task-list [items]="listaMovimientos()" />
          <a pie routerLink="/almacen/movimientos">Ver el kardex</a>
        </app-section-card>
      </div>

      <div class="c12">
        <app-quick-actions [acciones]="acciones" />
      </div>
    </div>
  `,
  styles: `
    .stats {
      margin-bottom: var(--sp-4);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InicioAlmacen implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly api = inject(AlmacenApiService);

  protected readonly resumen = new AsyncState<ResumenAlmacenResponse>();
  protected readonly alertas = new AsyncState<AlertasAlmacenResponse>();
  protected readonly movimientos = new AsyncState<MovimientoResponse[]>();

  protected readonly saludo = computed(() => saludoDelDia(this.auth.user()?.nombreCompleto ?? ''));
  protected readonly fecha = computed(() => {
    const texto = fechaLarga();
    return texto.charAt(0).toUpperCase() + texto.slice(1);
  });

  protected readonly acciones: readonly AccionRapida[] = [
    {
      label: 'Registrar ingreso',
      icono: 'pi-download',
      enlace: '/almacen/ingresos',
      detalle: 'Compras con lote',
    },
    {
      label: 'Productos y lotes',
      icono: 'pi-box',
      enlace: '/almacen/productos',
      detalle: 'Stock por producto',
    },
    {
      label: 'Proveedores',
      icono: 'pi-truck',
      enlace: '/almacen/proveedores',
      detalle: 'Directorio',
    },
    {
      label: 'Movimientos',
      icono: 'pi-arrow-right-arrow-left',
      enlace: '/almacen/movimientos',
      detalle: 'Kardex',
    },
  ];

  protected readonly listaAlertas = computed<ItemTarea[]>(() => {
    const datos = this.alertas.value();
    if (!datos) {
      return [];
    }
    return [
      ...datos.vencidos.slice(0, 3).map((lote): ItemTarea => ({
        id: `v-${lote.id}`,
        titulo: `${lote.productoNombre} · lote ${lote.numeroLote}`,
        detalle: `Vencido el ${lote.fechaVencimiento} · ${lote.cantidadActual} unidades`,
        tono: 'peligro',
        icono: 'pi-ban',
        enlace: '/almacen/productos',
      })),
      ...datos.porVencer.slice(0, 3).map((lote): ItemTarea => ({
        id: `x-${lote.id}`,
        titulo: `${lote.productoNombre} · lote ${lote.numeroLote}`,
        detalle: `Vence el ${lote.fechaVencimiento} · ${lote.cantidadActual} unidades`,
        tono: 'aviso',
        icono: 'pi-clock',
        enlace: '/almacen/productos',
      })),
      ...datos.bajoStock.slice(0, 4).map((producto): ItemTarea => ({
        id: `b-${producto.id}`,
        titulo: producto.nombre,
        detalle: `Stock ${producto.stockActual} de ${producto.stockMinimo} mínimo`,
        tono: 'aviso',
        icono: 'pi-exclamation-triangle',
        enlace: '/almacen/productos',
      })),
    ];
  });

  protected readonly listaMovimientos = computed<ItemTarea[]>(() =>
    (this.movimientos.value() ?? []).slice(0, 8).map((m): ItemTarea => ({
      id: m.id,
      titulo: m.productoNombre,
      detalle: `${m.tipo.replace('_', ' ').toLowerCase()} · lote ${m.numeroLote ?? '—'}`,
      cantidad: m.cantidad,
      tono: 'neutro',
      icono: 'pi-arrow-right-arrow-left',
    })),
  );

  ngOnInit(): void {
    const hoy = hoyIso();
    this.resumen.load(this.api.resumen());
    this.alertas.load(this.api.alertas());
    this.movimientos.load(this.api.listarMovimientos({ desde: hoy, hasta: hoy }));
  }
}
