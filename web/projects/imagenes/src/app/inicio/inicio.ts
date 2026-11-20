import { ChangeDetectionStrategy, Component, OnInit, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import {
  AgendaImagenResponse,
  AsyncState,
  AuthService,
  ETIQUETA_ESTADO_IMAGEN,
  ImagenesApiService,
  OrdenImagenResponse,
  ResumenImagenesResponse,
} from '@tusalud/core';
import { HeroPortada } from '@tusalud/ui/hero-portada';
import {
  AccionRapida,
  ItemLineaTiempo,
  ItemTarea,
  QuickActions,
  SectionCard,
  StatCard,
  TaskList,
  Timeline,
  fechaLarga,
  horaCorta,
  hoyIso,
  saludoDelDia,
} from '@tusalud/ui/tablero';

@Component({
  selector: 'app-inicio-imagenes',
  imports: [RouterLink, HeroPortada, SectionCard, StatCard, TaskList, Timeline, QuickActions],
  template: `
    <app-hero-portada app="imagenes" [title]="saludo()" [subtitle]="fecha()" />

    <div class="fila-stats stats">
      <app-stat-card
        etiqueta="Urgentes"
        [valor]="resumen.value()?.urgentes ?? 0"
        icono="pi-exclamation-triangle"
        [tono]="(resumen.value()?.urgentes ?? 0) > 0 ? 'peligro' : 'exito'"
        enlace="/imagenes"
      />
      <app-stat-card
        etiqueta="Por programar"
        [valor]="resumen.value()?.solicitadas ?? 0"
        icono="pi-inbox"
        tono="aviso"
        enlace="/imagenes"
      />
      <app-stat-card
        etiqueta="Programadas"
        [valor]="resumen.value()?.programadas ?? 0"
        icono="pi-calendar"
        tono="info"
        enlace="/imagenes/agenda"
      />
      <app-stat-card
        etiqueta="Por informar"
        [valor]="resumen.value()?.porInformar ?? 0"
        icono="pi-file-edit"
        [tono]="(resumen.value()?.porInformar ?? 0) > 0 ? 'aviso' : 'neutro'"
        enlace="/imagenes"
      />
      <app-stat-card
        etiqueta="Informadas hoy"
        [valor]="resumen.value()?.informadasHoy ?? 0"
        icono="pi-check-circle"
        tono="exito"
      />
    </div>

    <div class="tablero">
      <div class="c7">
        <app-section-card
          titulo="Cola de trabajo"
          icono="pi-list"
          subtitulo="Urgentes primero, luego por antigüedad"
          [cargando]="ordenes.loading() && !ordenes.hasValue()"
          [error]="ordenes.error()?.message ?? null"
          [vacio]="cola().length === 0 && ordenes.hasValue()"
          textoVacio="No hay estudios pendientes."
        >
          <app-task-list [items]="cola()" />
          <a pie routerLink="/imagenes">Ver todas las órdenes</a>
        </app-section-card>
      </div>

      <div class="c5">
        <app-section-card
          titulo="Estudios de hoy"
          icono="pi-calendar-clock"
          [cargando]="agenda.loading() && !agenda.hasValue()"
          [vacio]="lineaAgenda().length === 0 && agenda.hasValue()"
          textoVacio="No hay estudios programados para hoy."
        >
          <app-timeline [items]="lineaAgenda()" />
          <a pie routerLink="/imagenes/agenda">Abrir agenda de salas</a>
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
export class InicioImagenes implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly api = inject(ImagenesApiService);

  protected readonly resumen = new AsyncState<ResumenImagenesResponse>();
  protected readonly ordenes = new AsyncState<OrdenImagenResponse[]>();
  protected readonly agenda = new AsyncState<AgendaImagenResponse[]>();

  protected readonly saludo = computed(() => saludoDelDia(this.auth.user()?.nombreCompleto ?? ''));
  protected readonly fecha = computed(() => {
    const texto = fechaLarga();
    return texto.charAt(0).toUpperCase() + texto.slice(1);
  });

  protected readonly acciones: readonly AccionRapida[] = [
    {
      label: 'Órdenes',
      icono: 'pi-list',
      enlace: '/imagenes',
      detalle: 'Programar, realizar e informar',
    },
    {
      label: 'Agenda de salas',
      icono: 'pi-calendar',
      enlace: '/imagenes/agenda',
      detalle: 'Estudios programados',
    },
    {
      label: 'Catálogo de estudios',
      icono: 'pi-book',
      enlace: '/imagenes/catalogo',
      detalle: 'Precios y preparación',
    },
  ];

  protected readonly cola = computed<ItemTarea[]>(() =>
    (this.ordenes.value() ?? [])
      .filter(
        (o) => o.estado === 'SOLICITADA' || o.estado === 'PROGRAMADA' || o.estado === 'REALIZADA',
      )
      .sort(
        (a, b) =>
          Number(b.prioridad === 'URGENTE') - Number(a.prioridad === 'URGENTE') ||
          a.fecha.localeCompare(b.fecha),
      )
      .slice(0, 10)
      .map((orden): ItemTarea => ({
        id: orden.id,
        titulo: `${orden.codigo} · ${orden.pacienteNombre ?? ''}`,
        detalle: ETIQUETA_ESTADO_IMAGEN[orden.estado],
        cantidad: orden.estudios.length,
        tono:
          orden.prioridad === 'URGENTE'
            ? 'peligro'
            : orden.estado === 'REALIZADA'
              ? 'aviso'
              : 'neutro',
        icono: orden.prioridad === 'URGENTE' ? 'pi-exclamation-triangle' : 'pi-image',
        enlace: ['/imagenes/ordenes', orden.id],
        sensible: true,
      })),
  );

  protected readonly lineaAgenda = computed<ItemLineaTiempo[]>(() =>
    (this.agenda.value() ?? [])
      .sort((a, b) => a.programadaPara.localeCompare(b.programadaPara))
      .map((item): ItemLineaTiempo => ({
        id: item.id,
        hora: horaCorta(item.programadaPara),
        titulo: item.pacienteNombre,
        detalle: `${item.modalidades.join(', ')} · ${item.sala}`,
        tono: item.prioridad === 'URGENTE' ? 'peligro' : 'info',
        sensible: true,
      })),
  );

  ngOnInit(): void {
    const hoy = hoyIso();
    this.resumen.load(this.api.resumen());
    this.ordenes.load(this.api.listarOrdenes({}));
    this.agenda.load(this.api.agenda(hoy, hoy));
  }
}
