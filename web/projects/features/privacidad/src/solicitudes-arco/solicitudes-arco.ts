import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { Textarea } from 'primeng/textarea';

import {
  DecisionArco,
  DescargaService,
  ETIQUETA_CAMPO_RECTIFICABLE,
  ETIQUETA_DECISION_ARCO,
  ETIQUETA_ESTADO_ARCO,
  ETIQUETA_TIPO_ARCO,
  EstadoArco,
  EstadoPrivacidad,
  NotificationService,
  ProteccionDatosApiService,
  SEVERIDAD_ESTADO_ARCO,
  SolicitudArco,
  SolicitudArcoDetalle,
  TipoArco,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';

@Component({
  selector: 'app-solicitudes-arco',
  imports: [
    DatePipe,
    FormsModule,
    Button,
    Dialog,
    Message,
    Select,
    TableModule,
    Tag,
    Textarea,
    EmptyState,
    PageHeader,
  ],
  template: `
    <app-page-header
      title="Solicitudes de privacidad"
      subtitle="Derechos de acceso, rectificación, cancelación y oposición de los titulares"
      icon="pi-inbox"
    />

    @if (resumen(); as r) {
      <div class="indicadores">
        <div>
          <strong>{{ r.solicitudesAbiertas }}</strong>
          <span>Abiertas</span>
        </div>
        <div [class.alerta]="r.solicitudesVencidas > 0">
          <strong>{{ r.solicitudesVencidas }}</strong>
          <span>Vencidas</span>
        </div>
        <div [class.alerta]="r.incidentesVencidos > 0">
          <strong>{{ r.incidentesAbiertos }}</strong>
          <span>Incidentes abiertos</span>
        </div>
      </div>
    }

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
        <p-select
          [options]="opcionesTipo"
          optionLabel="label"
          optionValue="value"
          [ngModel]="tipo()"
          (ngModelChange)="tipo.set($event); cargar()"
          placeholder="Todos los derechos"
          [showClear]="true"
          ariaLabel="Filtrar por derecho"
          appendTo="body"
          styleClass="w-full md:w-14rem"
        />
      </div>

      <p-table
        [value]="solicitudes()"
        [loading]="cargando()"
        [rowHover]="true"
        responsiveLayout="scroll"
      >
        <ng-template #header>
          <tr>
            <th>Solicitud</th>
            <th>Titular</th>
            <th>Derecho</th>
            <th>Estado</th>
            <th>Vence</th>
            <th>Responsable</th>
            <th></th>
          </tr>
        </ng-template>
        <ng-template #body let-s>
          <tr>
            <td class="font-semibold">{{ s.codigo }}</td>
            <td>{{ s.solicitante }}</td>
            <td>{{ tipos[s.tipo] }}</td>
            <td><p-tag [value]="estados[s.estado]" [severity]="severidades[s.estado]" /></td>
            <td [class.vencida]="s.vencida">
              {{ s.venceEn | date: 'd MMM y' }}
              @if (s.vencida) {
                <small class="block">Vencida</small>
              }
            </td>
            <td>{{ s.responsable || 'Sin asignar' }}</td>
            <td class="derecha">
              <p-button
                label="Gestionar"
                size="small"
                [outlined]="true"
                [ariaLabel]="'Gestionar la solicitud ' + s.codigo"
                (onClick)="abrir(s)"
              />
            </td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr>
            <td colspan="7">
              <app-empty-state
                icon="pi-inbox"
                title="Sin solicitudes"
                message="No hay solicitudes con estos filtros."
              />
            </td>
          </tr>
        </ng-template>
      </p-table>
    </section>

    <p-dialog
      [header]="detalle() ? 'Solicitud ' + detalle()!.solicitud.codigo : ''"
      [visible]="detalle() !== null"
      (visibleChange)="$event ? null : detalle.set(null)"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '44rem', maxWidth: '96vw' }"
    >
      @if (detalle(); as d) {
        <div class="cabecera">
          <strong>{{ tipos[d.solicitud.tipo] }}</strong>
          <p-tag
            [value]="estados[d.solicitud.estado]"
            [severity]="severidades[d.solicitud.estado]"
          />
          <span class="text-muted"
            >de {{ d.solicitud.solicitante }}, recibida el
            {{ d.solicitud.recibidaEn | date: 'd MMM y' }}, vence el
            {{ d.solicitud.venceEn | date: 'd MMM y' }}</span
          >
        </div>
        <p>{{ d.solicitud.descripcion }}</p>
        @if (d.solicitud.campo) {
          <p-message severity="info" styleClass="w-full"
            >Dato a corregir: {{ campos[d.solicitud.campo] }}. Valor propuesto:
            <strong>{{ d.solicitud.valorPropuesto }}</strong></p-message
          >
        }
        @if (d.finalidad) {
          <p-message severity="info" styleClass="w-full"
            >Finalidad a la que se opone: <strong>{{ d.finalidad }}</strong></p-message
          >
        }
        @if (
          d.solicitud.tipo === 'CANCELACION' &&
          d.solicitud.estado !== 'RECIBIDA' &&
          d.solicitud.estado !== 'EN_REVISION'
        ) {
          @for (b of d.resultado; track b.categoria) {
            <p>
              <p-tag
                [value]="b.estado === 'BLOQUEADO' ? 'Se conservó' : 'Se canceló'"
                [severity]="b.estado === 'BLOQUEADO' ? 'warn' : 'success'"
              />
              <strong> {{ b.categoria }}</strong> · {{ b.motivo }}
            </p>
          }
        }
        @if (d.solicitud.resolucion) {
          <p-message severity="success" styleClass="w-full"
            >Resolución: {{ d.solicitud.resolucion }}</p-message
          >
        }
        @if (d.solicitud.tieneExportacion) {
          <p-button
            label="Descargar la exportación"
            icon="pi pi-download"
            [outlined]="true"
            (onClick)="descargar(d.solicitud)"
          />
        }

        @if (d.solicitud.estado === 'RECIBIDA' || d.solicitud.estado === 'EN_REVISION') {
          <h4>Resolver</h4>
          @if (error(); as mensaje) {
            <p-message severity="error" styleClass="w-full">{{ mensaje }}</p-message>
          }
          <div class="form">
            @if (d.solicitud.estado === 'RECIBIDA') {
              <p-button
                label="Asumir la solicitud"
                icon="pi pi-user-plus"
                [outlined]="true"
                (onClick)="tomar(d.solicitud)"
              />
            }
            <label for="res-decision">Decisión</label>
            <p-select
              inputId="res-decision"
              [options]="opcionesDecision"
              optionLabel="label"
              optionValue="value"
              [ngModel]="decision()"
              (ngModelChange)="decision.set($event)"
              appendTo="body"
              styleClass="w-full"
            />
            <label for="res-texto">Respuesta al titular</label>
            <textarea
              pTextarea
              id="res-texto"
              rows="3"
              class="w-full"
              maxlength="1000"
              [ngModel]="resolucion()"
              (ngModelChange)="resolucion.set($event)"
            ></textarea>
            <small class="text-muted">{{ ayudaDecision(d.solicitud.tipo) }}</small>
            <div>
              <p-button
                label="Registrar resolución"
                icon="pi pi-check"
                [loading]="trabajando()"
                (onClick)="resolver(d.solicitud)"
              />
            </div>
          </div>
        }

        <h4>Historial</h4>
        <ul class="historial">
          @for (e of d.eventos; track e.id) {
            <li>
              <span class="text-muted"
                >{{ e.registradoEn | date: 'd MMM y, HH:mm' }} · {{ e.actor }}</span
              >
              <strong>{{ e.accion }}</strong>
              <span>{{ e.detalle }}</span>
            </li>
          }
        </ul>
      }
    </p-dialog>
  `,
  styles: `
    .indicadores {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(10rem, 1fr));
      gap: 0.75rem;
      margin-bottom: 1rem;
    }

    .indicadores div {
      display: flex;
      flex-direction: column;
      padding: 0.8rem 1rem;
      border: 1px solid var(--p-content-border-color);
      border-radius: 8px;
    }

    .indicadores strong {
      font-size: 1.5rem;
    }

    .indicadores .alerta strong {
      color: var(--p-red-600);
    }

    .filtros {
      display: flex;
      flex-wrap: wrap;
      gap: 0.75rem;
      margin-bottom: 1rem;
    }

    .vencida {
      color: var(--p-red-600);
      font-weight: 600;
    }

    .derecha {
      text-align: right;
    }

    .cabecera {
      display: flex;
      flex-wrap: wrap;
      gap: 0.6rem;
      align-items: center;
    }

    .form {
      display: flex;
      flex-direction: column;
      gap: 0.4rem;
    }

    .form label {
      font-weight: 600;
      font-size: 0.9rem;
    }

    .historial {
      display: grid;
      gap: 0.5rem;
      margin: 0;
      padding: 0;
      list-style: none;
    }

    .historial li {
      display: flex;
      flex-direction: column;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SolicitudesArco implements OnInit {
  private readonly api = inject(ProteccionDatosApiService);
  private readonly descargas = inject(DescargaService);
  private readonly notificaciones = inject(NotificationService);

  protected readonly tipos: Record<string, string> = ETIQUETA_TIPO_ARCO;
  protected readonly estados: Record<string, string> = ETIQUETA_ESTADO_ARCO;
  protected readonly severidades: Record<
    string,
    'success' | 'info' | 'warn' | 'danger' | 'secondary'
  > = SEVERIDAD_ESTADO_ARCO;
  protected readonly campos: Record<string, string> = ETIQUETA_CAMPO_RECTIFICABLE;
  protected readonly opcionesEstado = Object.entries(ETIQUETA_ESTADO_ARCO).map(
    ([value, label]) => ({
      label,
      value,
    }),
  );
  protected readonly opcionesTipo = Object.entries(ETIQUETA_TIPO_ARCO).map(([value, label]) => ({
    label,
    value,
  }));
  protected readonly opcionesDecision = Object.entries(ETIQUETA_DECISION_ARCO).map(
    ([value, label]) => ({
      label,
      value,
    }),
  );

  protected readonly solicitudes = signal<SolicitudArco[]>([]);
  protected readonly resumen = signal<EstadoPrivacidad | null>(null);
  protected readonly detalle = signal<SolicitudArcoDetalle | null>(null);
  protected readonly cargando = signal(false);
  protected readonly trabajando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly estado = signal<EstadoArco | null>(null);
  protected readonly tipo = signal<TipoArco | null>(null);
  protected readonly decision = signal<DecisionArco>('ATENDER');
  protected readonly resolucion = signal('');

  ngOnInit(): void {
    this.cargar();
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.api.bandeja(this.estado(), this.tipo()).subscribe({
      next: (lista) => {
        this.solicitudes.set(lista);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
    this.api.estado().subscribe({ next: (r) => this.resumen.set(r), error: () => undefined });
  }

  protected abrir(s: SolicitudArco): void {
    this.error.set(null);
    this.decision.set('ATENDER');
    this.resolucion.set('');
    this.api.detalle(s.id).subscribe((d) => this.detalle.set(d));
  }

  protected ayudaDecision(tipo: TipoArco): string {
    switch (tipo) {
      case 'ACCESO':
        return 'Al atender, se genera la exportación de los datos del titular y queda disponible para su descarga.';
      case 'RECTIFICACION':
        return 'Si el dato es de contacto o identidad, se aplica el valor propuesto. Los datos clínicos se derivan al médico con la resolución parcial.';
      case 'CANCELACION':
        return 'Se cancela lo que la ley permite y se conserva la historia clínica y los comprobantes. El resultado queda como atendida en parte.';
      case 'OPOSICION':
        return 'Se revoca el consentimiento de la finalidad indicada.';
    }
  }

  protected tomar(s: SolicitudArco): void {
    this.api.tomar(s.id).subscribe(() => {
      this.notificaciones.success('Asumió la solicitud.');
      this.abrir(s);
      this.cargar();
    });
  }

  protected resolver(s: SolicitudArco): void {
    if (this.resolucion().trim().length < 10) {
      this.error.set('Escriba la respuesta al titular, con al menos 10 caracteres.');
      return;
    }
    this.error.set(null);
    this.trabajando.set(true);
    this.api
      .resolver(s.id, { decision: this.decision(), resolucion: this.resolucion().trim() })
      .subscribe({
        next: () => {
          this.trabajando.set(false);
          this.detalle.set(null);
          this.notificaciones.success('La resolución quedó registrada y se avisó al titular.');
          this.cargar();
        },
        error: (e: { message: string }) => {
          this.trabajando.set(false);
          this.error.set(e.message);
        },
      });
  }

  protected descargar(s: SolicitudArco): void {
    this.api.exportacion(s.id).subscribe((archivo) => this.descargas.guardar(archivo));
  }
}
