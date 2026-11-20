import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';
import { Tag } from 'primeng/tag';
import { Textarea } from 'primeng/textarea';

import {
  CampoCorregible,
  HceApiService,
  HistorialAtencion,
  NotificationService,
} from '@tusalud/core';
import { PageHeader } from '@tusalud/ui/page-header';

const CAMPOS: { label: string; value: CampoCorregible }[] = [
  { label: 'Diagnóstico', value: 'DIAGNOSTICO' },
  { label: 'Tratamiento', value: 'TRATAMIENTO' },
  { label: 'Indicaciones', value: 'INDICACIONES' },
  { label: 'Observaciones', value: 'OBSERVACIONES' },
];

@Component({
  selector: 'app-historial-atencion',
  imports: [
    DatePipe,
    FormsModule,
    RouterLink,
    Button,
    InputText,
    Message,
    Select,
    Tag,
    Textarea,
    PageHeader,
  ],
  template: `
    <app-page-header
      title="Historial y rectificación de la atención"
      subtitle="Una atención cerrada no se sobrescribe: se rectifica con una adenda o una corrección que deja rastro"
      icon="pi-history"
    />
    @if (error()) {
      <p-message severity="error">{{ error() }}</p-message>
    } @else if (historial(); as h) {
      <section class="panel estado">
        @if (h.cerrada) {
          <p-tag value="Cerrada y firmada" severity="success" />
          <span>
            {{ h.cierre.cerradoEn | date: 'd MMM y, HH:mm' }} por {{ h.cierre.cerradoPor }}. Huella
            {{ h.cierre.hashCorto }}.
            {{ h.cierre.firmaValida ? 'La firma se verifica.' : 'La firma no se pudo verificar.' }}
          </span>
          @if (h.cierre.advertencia) {
            <p-message severity="warn" [closable]="false">{{ h.cierre.advertencia }}</p-message>
          }
        } @else {
          <p-tag value="Sin cerrar" severity="warn" />
          <span>Mientras no se cierre, la atención se puede editar con normalidad.</span>
          @if (edicion) {
            <p-button
              label="Cerrar y firmar"
              icon="pi pi-lock"
              [loading]="trabajando()"
              (onClick)="cerrar()"
            />
          }
        }
      </section>

      <section class="comparacion">
        <article class="panel">
          <h3>Registro original</h3>
          @for (c of campos; track c.value) {
            <p>
              <strong>{{ c.label }}</strong
              ><br />{{ valor(h.original, c.value) }}
            </p>
          }
        </article>
        <article class="panel">
          <h3>Contenido vigente</h3>
          @for (c of campos; track c.value) {
            <p [class.cambiado]="valor(h.original, c.value) !== valor(h.vigente, c.value)">
              <strong>{{ c.label }}</strong
              ><br />{{ valor(h.vigente, c.value) }}
            </p>
          }
        </article>
      </section>

      <section class="panel">
        <h3>Línea de tiempo</h3>
        @if (h.versiones.length === 0) {
          <p class="text-muted">Aún no hay rectificaciones.</p>
        }
        <ol class="linea">
          @for (v of h.versiones; track v.id) {
            <li>
              <p-tag [value]="v.tipo === 'ADENDA' ? 'Adenda' : 'Corrección'" severity="info" />
              <strong> Versión {{ v.numero }}</strong>
              <span class="text-muted">
                · {{ v.autor }} · {{ v.creadoEn | date: 'd MMM y, HH:mm' }}
              </span>
              @if (v.tipo === 'CORRECCION') {
                <p>
                  <em>{{ v.campo }}</em
                  >: antes «{{ v.valorAnterior }}», ahora «{{ v.valorNuevo }}»
                </p>
              } @else {
                <p>{{ v.valorNuevo }}</p>
              }
              <small class="text-muted">Motivo: {{ v.motivo }}</small>
            </li>
          }
        </ol>
      </section>

      @if (edicion && h.cerrada) {
        <section class="rectificar">
          <article class="panel">
            <h3>Agregar una adenda</h3>
            <label for="adenda-texto">Texto de la adenda</label>
            <textarea
              pTextarea
              id="adenda-texto"
              rows="3"
              [ngModel]="adendaTexto()"
              (ngModelChange)="adendaTexto.set($event)"
            ></textarea>
            <label for="adenda-motivo">Motivo</label>
            <input
              pInputText
              id="adenda-motivo"
              [ngModel]="adendaMotivo()"
              (ngModelChange)="adendaMotivo.set($event)"
            />
            <p-button
              label="Agregar adenda"
              icon="pi pi-plus"
              [disabled]="adendaTexto().trim().length === 0 || adendaMotivo().trim().length < 5"
              [loading]="trabajando()"
              (onClick)="agregarAdenda()"
            />
          </article>
          <article class="panel">
            <h3>Corregir un dato</h3>
            <label for="corr-campo">Campo</label>
            <p-select
              inputId="corr-campo"
              [options]="campos"
              optionLabel="label"
              optionValue="value"
              [ngModel]="campo()"
              (ngModelChange)="campo.set($event)"
            />
            <label for="corr-valor">Valor corregido</label>
            <textarea
              pTextarea
              id="corr-valor"
              rows="3"
              [ngModel]="valorNuevo()"
              (ngModelChange)="valorNuevo.set($event)"
            ></textarea>
            <label for="corr-motivo">Motivo de la corrección</label>
            <input
              pInputText
              id="corr-motivo"
              [ngModel]="correccionMotivo()"
              (ngModelChange)="correccionMotivo.set($event)"
            />
            <p-button
              label="Registrar corrección"
              icon="pi pi-pencil"
              [disabled]="valorNuevo().trim().length === 0 || correccionMotivo().trim().length < 5"
              [loading]="trabajando()"
              (onClick)="corregir()"
            />
          </article>
        </section>
      }
    } @else {
      <p class="text-muted" role="status">Cargando el historial…</p>
    }
    <p-button
      label="Volver"
      icon="pi pi-arrow-left"
      severity="secondary"
      [text]="true"
      routerLink="/citas"
    />
  `,
  styles: `
    .estado {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 0.75rem;
      margin-bottom: 1rem;
    }

    .comparacion,
    .rectificar {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(20rem, 1fr));
      gap: 1rem;
      margin-bottom: 1rem;
    }

    .rectificar .panel {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
    }

    .cambiado {
      background: var(--p-yellow-50, #fefce8);
      border-left: 3px solid var(--p-yellow-500, #eab308);
      padding-left: 0.5rem;
    }

    .linea {
      list-style: none;
      padding: 0;
      margin: 0;
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
    }

    .linea p {
      margin: 0.25rem 0;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HistorialAtencionPagina implements OnInit {
  private readonly api = inject(HceApiService);
  private readonly ruta = inject(ActivatedRoute);
  private readonly avisos = inject(NotificationService);

  protected readonly campos = CAMPOS;
  protected readonly edicion = this.ruta.snapshot.data['edicion'] === true;
  protected readonly historial = signal<HistorialAtencion | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly trabajando = signal(false);
  protected readonly adendaTexto = signal('');
  protected readonly adendaMotivo = signal('');
  protected readonly campo = signal<CampoCorregible>('DIAGNOSTICO');
  protected readonly valorNuevo = signal('');
  protected readonly correccionMotivo = signal('');

  private id = '';

  ngOnInit(): void {
    this.id = this.ruta.snapshot.paramMap.get('atencionId') ?? '';
    this.cargar();
  }

  protected valor(contenido: HistorialAtencion['original'], campo: CampoCorregible): string {
    const mapa: Record<CampoCorregible, string | null> = {
      DIAGNOSTICO: contenido.diagnostico,
      TRATAMIENTO: contenido.tratamiento,
      INDICACIONES: contenido.indicaciones,
      OBSERVACIONES: contenido.observaciones,
    };
    return mapa[campo] ?? 'Sin dato';
  }

  protected cerrar(): void {
    this.ejecutar(this.api.cerrar(this.id), 'La atención quedó cerrada y firmada');
  }

  protected agregarAdenda(): void {
    this.ejecutar(
      this.api.adenda(this.id, {
        texto: this.adendaTexto().trim(),
        motivo: this.adendaMotivo().trim(),
      }),
      'Adenda registrada',
      () => {
        this.adendaTexto.set('');
        this.adendaMotivo.set('');
      },
    );
  }

  protected corregir(): void {
    this.ejecutar(
      this.api.corregir(this.id, {
        campo: this.campo(),
        valorNuevo: this.valorNuevo().trim(),
        motivo: this.correccionMotivo().trim(),
      }),
      'Corrección registrada',
      () => {
        this.valorNuevo.set('');
        this.correccionMotivo.set('');
      },
    );
  }

  private ejecutar(
    peticion: {
      subscribe: (observador: {
        next: () => void;
        error: (e: { error?: { message?: string } }) => void;
      }) => unknown;
    },
    mensaje: string,
    alExito?: () => void,
  ): void {
    this.trabajando.set(true);
    peticion.subscribe({
      next: () => {
        this.trabajando.set(false);
        this.avisos.success(mensaje);
        alExito?.();
        this.cargar();
      },
      error: (e) => {
        this.trabajando.set(false);
        this.avisos.error(e.error?.message ?? 'No se pudo completar la operación');
      },
    });
  }

  private cargar(): void {
    this.api.historial(this.id).subscribe({
      next: (h) => this.historial.set(h),
      error: () => this.error.set('No se pudo cargar el historial de la atención'),
    });
  }
}
