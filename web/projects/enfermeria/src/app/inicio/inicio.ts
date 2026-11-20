import { ChangeDetectionStrategy, Component, OnInit, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import {
  AsyncState,
  AuthService,
  EvolucionPendienteResponse,
  HospitalizacionApiService,
  IngresoHospResponse,
  MapaCamasResponse,
  ResumenHospResponse,
} from '@tusalud/core';
import { HeroPortada } from '@tusalud/ui/hero-portada';
import {
  AccionRapida,
  BarList,
  ItemBarra,
  ItemTarea,
  QuickActions,
  SectionCard,
  StatCard,
  TaskList,
  TonoTablero,
  fechaLarga,
  saludoDelDia,
} from '@tusalud/ui/tablero';

const AREAS: Readonly<Record<string, string>> = {
  HOSPITALIZACION: 'Hospitalización',
  EMERGENCIA: 'Emergencia',
  UCI: 'Cuidados intensivos',
  OBSERVACION: 'Observación',
};

const NIVEL: Readonly<Record<number, { texto: string; tono: TonoTablero }>> = {
  1: { texto: 'Nivel 1 · Resucitación', tono: 'peligro' },
  2: { texto: 'Nivel 2 · Emergencia', tono: 'peligro' },
  3: { texto: 'Nivel 3 · Urgencia', tono: 'aviso' },
  4: { texto: 'Nivel 4 · Urgencia menor', tono: 'exito' },
  5: { texto: 'Nivel 5 · No urgente', tono: 'info' },
};

@Component({
  selector: 'app-inicio-enfermeria',
  imports: [RouterLink, HeroPortada, SectionCard, StatCard, TaskList, BarList, QuickActions],
  template: `
    <app-hero-portada app="enfermeria" [title]="saludo()" [subtitle]="fecha()" />

    <div class="fila-stats stats">
      <app-stat-card
        etiqueta="Emergencias en espera"
        [valor]="resumen.value()?.emergenciasEnEspera ?? 0"
        icono="pi-exclamation-circle"
        [tono]="(resumen.value()?.emergenciasEnEspera ?? 0) > 0 ? 'peligro' : 'exito'"
        enlace="/hospitalizacion/emergencias"
      />
      <app-stat-card
        etiqueta="Ingresos activos"
        [valor]="resumen.value()?.ingresosActivos ?? 0"
        icono="pi-user"
        enlace="/hospitalizacion/ingresos"
      />
      <app-stat-card
        etiqueta="Camas libres"
        [valor]="resumen.value()?.camasLibres ?? 0"
        icono="pi-check-circle"
        tono="exito"
        [detalle]="(resumen.value()?.camasOcupadas ?? 0) + ' ocupadas'"
        enlace="/hospitalizacion"
      />
      <app-stat-card
        etiqueta="Camas por limpiar"
        [valor]="resumen.value()?.camasLimpieza ?? 0"
        icono="pi-sparkles"
        [tono]="(resumen.value()?.camasLimpieza ?? 0) > 0 ? 'aviso' : 'neutro'"
        enlace="/hospitalizacion/camas"
      />
      <app-stat-card
        etiqueta="Ocupación"
        [valor]="(resumen.value()?.ocupacionPorcentaje ?? 0).toFixed(0) + '%'"
        icono="pi-chart-pie"
        [tono]="(resumen.value()?.ocupacionPorcentaje ?? 0) > 85 ? 'aviso' : 'neutro'"
      />
    </div>

    <div class="tablero">
      <div class="c7">
        <app-section-card
          titulo="Emergencias por prioridad"
          icono="pi-exclamation-triangle"
          subtitulo="Ordenadas por nivel de triaje y tiempo de espera"
          [cargando]="emergencias.loading() && !emergencias.hasValue()"
          [error]="emergencias.error()?.message ?? null"
          [vacio]="lineaEmergencias().length === 0 && emergencias.hasValue()"
          textoVacio="No hay emergencias en espera."
        >
          <app-task-list [items]="lineaEmergencias()" />
          <a pie routerLink="/hospitalizacion/emergencias">Abrir cola de emergencias</a>
        </app-section-card>
      </div>

      <div class="c5">
        <app-section-card
          titulo="Ocupación por área"
          icono="pi-building"
          [cargando]="mapa.loading() && !mapa.hasValue()"
          [vacio]="barras().length === 0 && mapa.hasValue()"
          textoVacio="Aún no hay camas configuradas."
        >
          <app-bar-list [items]="barras()" etiqueta="Camas ocupadas por área" />
          <a pie routerLink="/hospitalizacion">Ver mapa de camas</a>
        </app-section-card>
      </div>

      <div class="c12">
        <app-section-card
          titulo="Evoluciones pendientes"
          icono="pi-clock"
          subtitulo="Ingresos con cama sin evolución de enfermería en las últimas 8 horas"
          [cargando]="pendientes.loading() && !pendientes.hasValue()"
          [error]="pendientes.error()?.message ?? null"
          [vacio]="lineaPendientes().length === 0 && pendientes.hasValue()"
          textoVacio="Las evoluciones de enfermería están al día."
        >
          <app-task-list [items]="lineaPendientes()" />
          <a pie routerLink="/hospitalizacion/ingresos">Abrir ingresos para registrarlas</a>
        </app-section-card>
      </div>

      <div class="c12">
        <app-section-card
          titulo="Pacientes hospitalizados"
          icono="pi-list"
          subtitulo="Ingresos activos, abre la ficha para registrar la evolución del turno"
          [cargando]="ingresos.loading() && !ingresos.hasValue()"
          [vacio]="lineaIngresos().length === 0 && ingresos.hasValue()"
          textoVacio="No hay pacientes hospitalizados."
        >
          <app-task-list [items]="lineaIngresos()" />
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
export class InicioEnfermeria implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly api = inject(HospitalizacionApiService);

  protected readonly resumen = new AsyncState<ResumenHospResponse>();
  protected readonly mapa = new AsyncState<MapaCamasResponse[]>();
  protected readonly emergencias = new AsyncState<IngresoHospResponse[]>();
  protected readonly ingresos = new AsyncState<IngresoHospResponse[]>();
  protected readonly pendientes = new AsyncState<EvolucionPendienteResponse[]>();

  protected readonly saludo = computed(() => saludoDelDia(this.auth.user()?.nombreCompleto ?? ''));
  protected readonly fecha = computed(() => {
    const texto = fechaLarga();
    return texto.charAt(0).toUpperCase() + texto.slice(1);
  });

  protected readonly acciones: readonly AccionRapida[] = [
    {
      label: 'Mapa de camas',
      icono: 'pi-building',
      enlace: '/hospitalizacion',
      detalle: 'Estado de cada cama',
    },
    {
      label: 'Registrar triaje',
      icono: 'pi-exclamation-circle',
      enlace: '/hospitalizacion/emergencias',
      detalle: 'Cola de emergencia',
    },
    {
      label: 'Ingresos',
      icono: 'pi-list',
      enlace: '/hospitalizacion/ingresos',
      detalle: 'Evoluciones y traslados',
    },
    {
      label: 'Camas',
      icono: 'pi-th-large',
      enlace: '/hospitalizacion/camas',
      detalle: 'Limpieza y mantenimiento',
    },
  ];

  protected readonly barras = computed<ItemBarra[]>(() =>
    (this.mapa.value() ?? []).map((grupo): ItemBarra => {
      const total = grupo.total ?? grupo.camas.length;
      const ocupadas = grupo.ocupadas ?? grupo.camas.filter((c) => c.estado === 'OCUPADA').length;
      const porcentaje = total === 0 ? 0 : Math.round((ocupadas / total) * 100);
      return {
        etiqueta: AREAS[grupo.area] ?? grupo.area,
        valor: porcentaje,
        texto: `${ocupadas} de ${total} · ${porcentaje}%`,
        tono: porcentaje > 85 ? 'peligro' : porcentaje > 65 ? 'aviso' : 'neutro',
      };
    }),
  );

  protected readonly lineaEmergencias = computed<ItemTarea[]>(() =>
    [...(this.emergencias.value() ?? [])]
      .sort(
        (a, b) =>
          (a.triaje?.nivel ?? 9) - (b.triaje?.nivel ?? 9) ||
          (b.esperaMinutos ?? 0) - (a.esperaMinutos ?? 0),
      )
      .slice(0, 8)
      .map((ingreso): ItemTarea => {
        const nivel = NIVEL[ingreso.triaje?.nivel ?? 5];
        return {
          id: ingreso.id,
          titulo: ingreso.pacienteNombre,
          detalle: `${nivel.texto}${ingreso.esperaMinutos ? ' · espera de ' + ingreso.esperaMinutos + ' min' : ''}`,
          cantidad: ingreso.triaje?.nivel ?? null,
          tono: nivel.tono,
          icono: 'pi-exclamation-circle',
          enlace: ['/hospitalizacion/ingresos', ingreso.id],
          sensible: true,
        };
      }),
  );

  protected readonly lineaPendientes = computed<ItemTarea[]>(() =>
    (this.pendientes.value() ?? []).slice(0, 10).map((pendiente): ItemTarea => ({
      id: pendiente.ingresoId,
      titulo: pendiente.paciente,
      detalle: `Cama ${pendiente.cama} · ${AREAS[pendiente.area] ?? pendiente.area} · ${pendiente.ultimaEvolucion ? 'sin registro hace ' : 'sin ninguna evolución hace '}${pendiente.horasSinRegistro} h`,
      cantidad: pendiente.horasSinRegistro,
      tono: pendiente.horasSinRegistro >= 12 ? 'peligro' : 'aviso',
      icono: 'pi-clock',
      enlace: ['/hospitalizacion/ingresos', pendiente.ingresoId],
      sensible: true,
    })),
  );

  protected readonly lineaIngresos = computed<ItemTarea[]>(() =>
    (this.ingresos.value() ?? [])
      .filter((ingreso) => ingreso.estado === 'ACTIVO')
      .slice(0, 10)
      .map((ingreso): ItemTarea => ({
        id: ingreso.id,
        titulo: ingreso.pacienteNombre,
        detalle: `${ingreso.camaCodigo ? 'Cama ' + ingreso.camaCodigo : 'Sin cama'} · ${ingreso.diasEstancia} ${ingreso.diasEstancia === 1 ? 'día' : 'días'} de estancia`,
        tono: 'neutro',
        icono: 'pi-user',
        enlace: ['/hospitalizacion/ingresos', ingreso.id],
        sensible: true,
      })),
  );

  ngOnInit(): void {
    this.resumen.load(this.api.resumen());
    this.mapa.load(this.api.mapa());
    this.emergencias.load(this.api.emergencias());
    this.ingresos.load(this.api.ingresos('ACTIVO'));
    this.pendientes.load(this.api.evolucionesPendientes());
  }
}
