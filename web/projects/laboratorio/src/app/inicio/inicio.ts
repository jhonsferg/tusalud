import { ChangeDetectionStrategy, Component, OnInit, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import {
  AsyncState,
  AuthService,
  ETIQUETA_ESTADO_ORDEN,
  LaboratorioApiService,
  OrdenResponse,
  ResumenLaboratorioResponse,
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

function esperaTexto(fecha: string): string {
  const minutos = Math.max(0, Math.round((Date.now() - new Date(fecha).getTime()) / 60_000));
  if (minutos < 60) {
    return `${minutos} min`;
  }
  const horas = Math.floor(minutos / 60);
  return horas < 24 ? `${horas} h` : `${Math.floor(horas / 24)} d`;
}

@Component({
  selector: 'app-inicio-laboratorio',
  imports: [RouterLink, HeroPortada, SectionCard, StatCard, TaskList, QuickActions],
  template: `
    <app-hero-portada app="laboratorio" [title]="saludo()" [subtitle]="fecha()" />

    <div class="fila-stats stats">
      <app-stat-card
        etiqueta="Urgentes"
        [valor]="resumen.value()?.urgentes ?? 0"
        icono="pi-exclamation-triangle"
        [tono]="(resumen.value()?.urgentes ?? 0) > 0 ? 'peligro' : 'exito'"
        enlace="/laboratorio"
      />
      <app-stat-card
        etiqueta="Por tomar muestra"
        [valor]="resumen.value()?.pendientes ?? 0"
        icono="pi-inbox"
        tono="aviso"
        enlace="/laboratorio"
      />
      <app-stat-card
        etiqueta="En proceso"
        [valor]="resumen.value()?.enProceso ?? 0"
        icono="pi-spinner"
        tono="info"
        enlace="/laboratorio"
      />
      <app-stat-card
        etiqueta="Completadas hoy"
        [valor]="resumen.value()?.completadasHoy ?? 0"
        icono="pi-check-circle"
        tono="exito"
      />
    </div>

    <div class="tablero">
      <div class="c8">
        <app-section-card
          titulo="Cola de trabajo"
          icono="pi-list"
          subtitulo="Urgentes primero, luego por antigüedad"
          [cargando]="ordenes.loading() && !ordenes.hasValue()"
          [error]="ordenes.error()?.message ?? null"
          [vacio]="cola().length === 0 && ordenes.hasValue()"
          textoVacio="No hay órdenes pendientes. Buen trabajo."
        >
          <app-task-list [items]="cola()" />
          <a pie routerLink="/laboratorio">Abrir todas las órdenes</a>
        </app-section-card>
      </div>

      <div class="c4">
        <app-section-card titulo="Accesos" icono="pi-bolt">
          <app-quick-actions [acciones]="acciones" />
        </app-section-card>
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
export class InicioLaboratorio implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly api = inject(LaboratorioApiService);

  protected readonly resumen = new AsyncState<ResumenLaboratorioResponse>();
  protected readonly ordenes = new AsyncState<OrdenResponse[]>();

  protected readonly saludo = computed(() => saludoDelDia(this.auth.user()?.nombreCompleto ?? ''));
  protected readonly fecha = computed(() => {
    const texto = fechaLarga();
    return texto.charAt(0).toUpperCase() + texto.slice(1);
  });

  protected readonly acciones: readonly AccionRapida[] = [
    { label: 'Órdenes', icono: 'pi-list', enlace: '/laboratorio' },
    { label: 'Catálogo de exámenes', icono: 'pi-book', enlace: '/laboratorio/examenes' },
  ];

  protected readonly cola = computed<ItemTarea[]>(() =>
    (this.ordenes.value() ?? [])
      .filter((o) => o.estado !== 'COMPLETADA' && o.estado !== 'CANCELADA')
      .sort(
        (a, b) =>
          Number(b.prioridad === 'URGENTE') - Number(a.prioridad === 'URGENTE') ||
          a.fecha.localeCompare(b.fecha),
      )
      .slice(0, 10)
      .map((orden): ItemTarea => ({
        id: orden.id,
        titulo: `${orden.codigo} · ${orden.pacienteNombre}`,
        detalle: `${ETIQUETA_ESTADO_ORDEN[orden.estado]} · hace ${esperaTexto(orden.fecha)}`,
        cantidad: orden.items.length,
        tono: orden.prioridad === 'URGENTE' ? 'peligro' : 'neutro',
        icono: orden.prioridad === 'URGENTE' ? 'pi-exclamation-triangle' : 'pi-file',
        enlace: ['/laboratorio/ordenes', orden.id],
        sensible: true,
      })),
  );

  ngOnInit(): void {
    this.resumen.load(this.api.resumen());
    this.ordenes.load(this.api.listarOrdenes({}));
  }
}
