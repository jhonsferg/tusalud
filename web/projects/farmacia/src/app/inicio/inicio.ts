import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import {
  AlertasAlmacenResponse,
  AlmacenApiService,
  AsyncState,
  AuthService,
  FarmaciaApiService,
  RecetaFarmaciaResponse,
  ResumenFarmaciaResponse,
} from '@tusalud/core';
import { HeroPortada } from '@tusalud/ui/hero-portada';
import {
  AccionRapida,
  ItemTarea,
  QuickActions,
  SectionCard,
  StatCard,
  TaskList,
  fechaLarga,
  saludoDelDia,
} from '@tusalud/ui/tablero';

@Component({
  selector: 'app-inicio-farmacia',
  imports: [CurrencyPipe, RouterLink, HeroPortada, SectionCard, StatCard, TaskList, QuickActions],
  template: `
    <app-hero-portada app="farmacia" [title]="saludo()" [subtitle]="fecha()" />

    <div class="fila-stats stats">
      <app-stat-card
        etiqueta="Recetas pendientes"
        [valor]="resumen.value()?.recetasPendientes ?? 0"
        icono="pi-file"
        [tono]="(resumen.value()?.recetasPendientes ?? 0) > 0 ? 'aviso' : 'exito'"
        enlace="/farmacia"
      />
      <app-stat-card
        etiqueta="Dispensaciones de hoy"
        [valor]="resumen.value()?.dispensacionesHoy ?? 0"
        icono="pi-check-circle"
        tono="exito"
        enlace="/farmacia/historial"
      />
      <app-stat-card
        etiqueta="Ventas de hoy"
        [valor]="(resumen.value()?.ventasHoy ?? 0 | currency: 'PEN' : 'symbol-narrow') ?? ''"
        icono="pi-dollar"
        enlace="/farmacia/historial"
      />
      <app-stat-card
        etiqueta="Productos con stock bajo"
        [valor]="alertas.value()?.bajoStock?.length ?? 0"
        icono="pi-exclamation-triangle"
        [tono]="(alertas.value()?.bajoStock?.length ?? 0) > 0 ? 'aviso' : 'neutro'"
        enlace="/almacen/productos"
      />
    </div>

    <div class="tablero">
      <div class="c7">
        <app-section-card
          titulo="Recetas por dispensar"
          icono="pi-file"
          [cargando]="recetas.loading() && !recetas.hasValue()"
          [error]="recetas.error()?.message ?? null"
          [vacio]="listaRecetas().length === 0 && recetas.hasValue()"
          textoVacio="No hay recetas pendientes."
        >
          <app-task-list [items]="listaRecetas()" />
          <a pie routerLink="/farmacia/dispensar">Ir a dispensar</a>
        </app-section-card>
      </div>

      <div class="c5">
        <app-section-card
          titulo="Alertas de inventario"
          icono="pi-bell"
          [vacio]="listaAlertas().length === 0 && alertas.hasValue()"
          textoVacio="Sin alertas de inventario."
        >
          <app-task-list [items]="listaAlertas()" />
          <a pie routerLink="/almacen/productos">Consultar productos</a>
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
export class InicioFarmacia implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly api = inject(FarmaciaApiService);
  private readonly almacen = inject(AlmacenApiService);

  protected readonly resumen = new AsyncState<ResumenFarmaciaResponse>();
  protected readonly recetas = new AsyncState<RecetaFarmaciaResponse[]>();
  protected readonly alertas = new AsyncState<AlertasAlmacenResponse>();

  protected readonly saludo = computed(() => saludoDelDia(this.auth.user()?.nombreCompleto ?? ''));
  protected readonly fecha = computed(() => {
    const texto = fechaLarga();
    return texto.charAt(0).toUpperCase() + texto.slice(1);
  });

  protected readonly acciones: readonly AccionRapida[] = [
    {
      label: 'Dispensar',
      icono: 'pi-shopping-cart',
      enlace: '/farmacia/dispensar',
      detalle: 'Receta o venta libre',
    },
    {
      label: 'Historial',
      icono: 'pi-history',
      enlace: '/farmacia/historial',
      detalle: 'Dispensaciones y comprobantes',
    },
    { label: 'Productos', icono: 'pi-box', enlace: '/almacen/productos', detalle: 'Stock y lotes' },
    {
      label: 'Movimientos',
      icono: 'pi-arrow-right-arrow-left',
      enlace: '/almacen/movimientos',
      detalle: 'Kardex',
    },
  ];

  protected readonly listaRecetas = computed<ItemTarea[]>(() =>
    (this.recetas.value() ?? [])
      .filter((r) => r.estado !== 'DISPENSADA')
      .slice(0, 8)
      .map((receta): ItemTarea => ({
        id: receta.atencionId,
        titulo: receta.pacienteNombre,
        detalle: `${receta.items.length} medicamentos · Dr. ${receta.medicoNombre}`,
        cantidad: receta.items.length,
        tono: receta.estado === 'PARCIAL' ? 'aviso' : 'info',
        icono: 'pi-file',
        enlace: '/farmacia/dispensar',
        sensible: true,
      })),
  );

  protected readonly listaAlertas = computed<ItemTarea[]>(() => {
    const datos = this.alertas.value();
    if (!datos) {
      return [];
    }
    const items: ItemTarea[] = [];
    if (datos.bajoStock.length > 0) {
      items.push({
        id: 'bajo',
        titulo: 'Productos bajo el stock mínimo',
        detalle: datos.bajoStock
          .slice(0, 2)
          .map((p) => p.nombre)
          .join(', '),
        cantidad: datos.bajoStock.length,
        tono: 'aviso',
        icono: 'pi-exclamation-triangle',
      });
    }
    if (datos.porVencer.length > 0) {
      items.push({
        id: 'vencer',
        titulo: 'Lotes por vencer',
        cantidad: datos.porVencer.length,
        tono: 'aviso',
        icono: 'pi-clock',
      });
    }
    if (datos.vencidos.length > 0) {
      items.push({
        id: 'vencidos',
        titulo: 'Lotes vencidos',
        cantidad: datos.vencidos.length,
        tono: 'peligro',
        icono: 'pi-ban',
      });
    }
    return items;
  });

  ngOnInit(): void {
    this.resumen.load(this.api.resumen());
    this.recetas.load(this.api.recetas());
    this.alertas.load(this.almacen.alertas());
  }
}
