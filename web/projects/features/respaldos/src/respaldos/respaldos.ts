import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { Button } from 'primeng/button';
import { SelectButton } from 'primeng/selectbutton';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { FormsModule } from '@angular/forms';

import {
  CanalesStore,
  ETIQUETA_TIPO_RESPALDO,
  SemaforoRespaldos,
  TipoEjecucionRespaldo,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';

type FiltroTipo = TipoEjecucionRespaldo | 'TODOS';

export function formatearBytes(bytes: number | null): string {
  if (bytes === null || bytes === undefined) {
    return '—';
  }
  if (bytes < 1024) {
    return `${bytes} B`;
  }
  const unidades = ['KB', 'MB', 'GB', 'TB'];
  let valor = bytes / 1024;
  let indice = 0;
  while (valor >= 1024 && indice < unidades.length - 1) {
    valor /= 1024;
    indice++;
  }
  return `${valor.toFixed(1)} ${unidades[indice]}`;
}

@Component({
  selector: 'app-respaldos',
  imports: [
    DatePipe,
    DecimalPipe,
    FormsModule,
    Button,
    SelectButton,
    TableModule,
    Tag,
    EmptyState,
    PageHeader,
  ],
  template: `
    <app-page-header
      title="Respaldos y recuperación"
      subtitle="Estado de los respaldos de la base y del almacén, y de la última prueba de restauración"
      icon="pi-database"
    >
      <p-button label="Actualizar" icon="pi pi-refresh" [outlined]="true" (onClick)="recargar()" />
    </app-page-header>

    @if (store.estadoRespaldos.error(); as problema) {
      <app-empty-state
        tone="error"
        icon="pi-exclamation-triangle"
        title="No pudimos cargar el estado de los respaldos"
        [message]="problema.message"
      >
        <p-button label="Reintentar" icon="pi pi-refresh" (onClick)="recargar()" />
      </app-empty-state>
    } @else if (store.estadoRespaldos.value(); as estado) {
      <section class="estado" aria-label="Estado de los respaldos">
        <article class="semaforo" [attr.data-semaforo]="estado.semaforo" role="status">
          <i class="pi {{ iconoSemaforo(estado.semaforo) }}" aria-hidden="true"></i>
          <div>
            <strong data-testid="semaforo">{{ textoSemaforo(estado.semaforo) }}</strong>
            <p>
              @if (estado.respaldoVencido) {
                Hace más de {{ estado.maxHorasSinRespaldo }} horas que no hay un respaldo exitoso.
              } @else if (estado.ultimoRespaldoFallido && semaforoRojo(estado.semaforo)) {
                El último respaldo falló. Revisa el detalle abajo.
              } @else if (estado.pruebaVencida) {
                Falta una prueba de restauración exitosa en los últimos
                {{ estado.maxDiasSinPrueba }} días.
              } @else if (!estado.ultimoRespaldoExitoso || !estado.ultimaPruebaExitosa) {
                Aún no hay un respaldo y una prueba de restauración exitosos registrados.
              } @else {
                Respaldo reciente y restauración probada.
              }
            </p>
          </div>
        </article>

        <article class="tarjeta">
          <h2>Último respaldo exitoso</h2>
          @if (estado.ultimoRespaldoExitoso; as r) {
            <p class="valor">{{ r.finalizadoEn | date: 'd MMM y, HH:mm' }}</p>
            <small class="text-muted">
              Hace {{ estado.horasDesdeUltimoRespaldo }} h · {{ formatear(r.bytesTotal) }} ·
              {{ r.duracionSeg }} s · {{ r.objetosAlmacen ?? 0 }} objetos del almacén
            </small>
          } @else {
            <p class="valor">Sin registros</p>
          }
        </article>

        <article class="tarjeta">
          <h2>Última prueba de restauración exitosa</h2>
          @if (estado.ultimaPruebaExitosa; as r) {
            <p class="valor">{{ r.finalizadoEn | date: 'd MMM y, HH:mm' }}</p>
            <small class="text-muted">
              {{ r.migracionesValidadas }} migraciones · {{ r.tablasValidadas }} tablas ·
              {{ r.objetosAlmacen ?? 0 }} objetos verificados · {{ r.duracionSeg }} s
            </small>
          } @else {
            <p class="valor">Sin registros</p>
          }
        </article>

        <article class="tarjeta">
          <h2>Objetivos de recuperación</h2>
          <p class="valor">
            RPO {{ estado.rpoObjetivoHoras }} h · RTO {{ estado.rtoObjetivoMinutos }} min
          </p>
          <small class="text-muted">
            Máximo de datos que se aceptan perder y tiempo objetivo para volver a operar.
          </small>
        </article>
      </section>
    }

    <section class="panel">
      <div class="barra">
        <h2>Historial</h2>
        <p-selectbutton
          [options]="opciones"
          optionLabel="label"
          optionValue="value"
          [ngModel]="filtro()"
          (ngModelChange)="cambiarFiltro($event ?? 'TODOS')"
          [allowEmpty]="false"
          ariaLabel="Filtrar por tipo de ejecución"
        />
      </div>
      <p-table
        [value]="store.respaldos.value() ?? []"
        [loading]="store.respaldos.loading() && !store.respaldos.hasValue()"
        [rowHover]="true"
        responsiveLayout="scroll"
      >
        <ng-template #header>
          <tr>
            <th>Inicio</th>
            <th>Tipo</th>
            <th>Resultado</th>
            <th>Categoría</th>
            <th>Duración</th>
            <th>Tamaño</th>
            <th>Detalle</th>
          </tr>
        </ng-template>
        <ng-template #body let-r>
          <tr>
            <td>{{ r.iniciadoEn | date: 'd MMM y, HH:mm' }}</td>
            <td>{{ tipos[r.tipo] }}</td>
            <td>
              <p-tag
                [value]="r.estado === 'EXITOSO' ? 'Exitoso' : 'Fallido'"
                [severity]="r.estado === 'EXITOSO' ? 'success' : 'danger'"
              />
            </td>
            <td>{{ r.categoria ?? '—' }}</td>
            <td>{{ r.duracionSeg | number }} s</td>
            <td>{{ formatear(r.bytesTotal) }}</td>
            <td class="detalle">
              {{ r.detalle || '—' }}
              @if (r.copiaRemota) {
                <p-tag value="Copia remota" severity="info" />
              }
            </td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr>
            <td colspan="7">
              <app-empty-state
                icon="pi-database"
                title="Sin ejecuciones registradas"
                message="El contenedor de respaldos reporta aquí cada respaldo y cada prueba de restauración."
              />
            </td>
          </tr>
        </ng-template>
      </p-table>
    </section>

    <section class="panel plan">
      <h2>Plan de recuperación</h2>
      <ol>
        <li>Detener la API para que no entren datos nuevos durante la restauración.</li>
        <li>
          Elegir el respaldo más reciente de la lista y restaurarlo con el script de restauración
          del contenedor de respaldos.
        </li>
        <li>Reponer los objetos del almacén con la misma operación.</li>
        <li>
          Levantar la API, que validará las migraciones al arrancar, y revisar el inicio de sesión.
        </li>
      </ol>
    </section>
  `,
  styles: `
    .estado {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(15rem, 1fr));
      gap: 0.75rem;
      margin-bottom: 1rem;
    }

    .semaforo {
      display: flex;
      align-items: flex-start;
      gap: 0.8rem;
      padding: 1rem;
      border: 1px solid var(--p-content-border-color);
      border-radius: 10px;
      background: var(--p-content-background);
      border-left-width: 6px;
    }

    .semaforo i {
      font-size: 1.6rem;
    }

    .semaforo p {
      margin: 0.2rem 0 0;
      font-size: 0.9rem;
    }

    .semaforo[data-semaforo='VERDE'] {
      border-left-color: #15803d;
    }

    .semaforo[data-semaforo='VERDE'] i {
      color: #15803d;
    }

    .semaforo[data-semaforo='AMARILLO'] {
      border-left-color: #d97706;
    }

    .semaforo[data-semaforo='AMARILLO'] i {
      color: #d97706;
    }

    .semaforo[data-semaforo='ROJO'] {
      border-left-color: #dc2626;
    }

    .semaforo[data-semaforo='ROJO'] i {
      color: #dc2626;
    }

    .tarjeta {
      padding: 1rem;
      border: 1px solid var(--p-content-border-color);
      border-radius: 10px;
      background: var(--p-content-background);
    }

    .tarjeta h2 {
      margin: 0 0 0.3rem;
      font-size: 0.85rem;
      font-weight: 600;
      color: var(--p-text-muted-color);
    }

    .tarjeta .valor {
      margin: 0 0 0.2rem;
      font-size: 1.15rem;
      font-weight: 600;
    }

    .barra {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: 0.75rem;
      margin-bottom: 1rem;
    }

    .barra h2,
    .plan h2 {
      margin: 0;
      font-size: 1.05rem;
    }

    .detalle {
      max-width: 28rem;
    }

    .plan ol {
      margin: 0.6rem 0 0;
      padding-left: 1.3rem;
      line-height: 1.7;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Respaldos implements OnInit {
  protected readonly store = inject(CanalesStore);

  protected readonly tipos: Record<string, string> = ETIQUETA_TIPO_RESPALDO;
  protected readonly opciones: { label: string; value: FiltroTipo }[] = [
    { label: 'Todos', value: 'TODOS' },
    { label: 'Respaldos', value: 'RESPALDO' },
    { label: 'Pruebas de restauración', value: 'PRUEBA_RESTAURACION' },
  ];
  protected readonly filtro = signal<FiltroTipo>('TODOS');

  ngOnInit(): void {
    this.recargar();
  }

  protected recargar(): void {
    this.store.cargarEstadoRespaldos();
    this.cargarHistorial();
  }

  protected cambiarFiltro(filtro: FiltroTipo): void {
    this.filtro.set(filtro);
    this.cargarHistorial();
  }

  protected formatear(bytes: number | null): string {
    return formatearBytes(bytes);
  }

  protected iconoSemaforo(semaforo: SemaforoRespaldos): string {
    if (semaforo === 'VERDE') {
      return 'pi-check-circle';
    }
    return semaforo === 'AMARILLO' ? 'pi-exclamation-circle' : 'pi-times-circle';
  }

  protected textoSemaforo(semaforo: SemaforoRespaldos): string {
    if (semaforo === 'VERDE') {
      return 'Respaldos al día';
    }
    return semaforo === 'AMARILLO' ? 'Requiere atención' : 'Respaldos en riesgo';
  }

  protected semaforoRojo(semaforo: SemaforoRespaldos): boolean {
    return semaforo === 'ROJO';
  }

  private cargarHistorial(): void {
    const filtro = this.filtro();
    this.store.cargarRespaldos(filtro === 'TODOS' ? null : filtro);
  }
}
