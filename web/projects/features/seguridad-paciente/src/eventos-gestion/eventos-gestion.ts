import { DatePipe, KeyValuePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';
import { Tag } from 'primeng/tag';
import { Textarea } from 'primeng/textarea';

import {
  AuthService,
  DescargaService,
  EstadoEvento,
  EventoDetalle,
  EventoResumen,
  MetodoAnalisis,
  NotificationService,
  SeguridadPacienteApiService,
  TableroSeguridad,
} from '@tusalud/core';
import { PageHeader } from '@tusalud/ui/page-header';
import {
  ETIQUETA_ESTADO_EVENTO,
  ETIQUETA_SEVERIDAD,
  ETIQUETA_TIPO_EVENTO,
  barraProporcional,
} from '../seguridad.utils';

@Component({
  selector: 'app-eventos-gestion',
  imports: [
    DatePipe,
    KeyValuePipe,
    FormsModule,
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
      title="Seguridad del paciente"
      subtitle="Reportes de eventos adversos y casi incidentes, con análisis de causa y plan de acción"
      icon="pi-shield"
    />
    @if (tablero(); as t) {
      <section class="kpis" aria-label="Indicadores">
        <article class="panel kpi">
          <strong>{{ t.total }}</strong
          ><span>Reportes</span>
        </article>
        <article class="panel kpi">
          <strong>{{ t.casiIncidentes }}</strong
          ><span>Casi incidentes</span>
        </article>
        <article class="panel kpi">
          <strong>{{ t.eventosAdversos }}</strong
          ><span>Eventos adversos</span>
        </article>
        <article class="panel kpi">
          <strong>{{ t.accionesVencidas }}</strong
          ><span>Acciones vencidas</span>
        </article>
        <article class="panel kpi">
          <strong>{{ t.diasMedioCierre }}</strong
          ><span>Días medios al cierre</span>
        </article>
      </section>
      <section class="panel">
        <h3>Tendencia mensual</h3>
        @for (m of t.porMes | keyvalue; track m.key) {
          <div class="barra">
            <span>{{ m.key }}</span>
            <div
              class="relleno"
              role="img"
              [attr.aria-label]="m.value + ' reportes en ' + m.key"
              [style.width.%]="porcentaje(m.value, t.total)"
            ></div>
            <span>{{ m.value }}</span>
          </div>
        }
      </section>
    }

    <section class="panel">
      <div class="barra-acciones">
        <p-select
          [options]="estados"
          optionLabel="label"
          optionValue="value"
          placeholder="Todos los estados"
          [showClear]="true"
          ariaLabel="Filtrar por estado"
          [ngModel]="filtro()"
          (ngModelChange)="cambiarFiltro($event)"
        />
        <p-button
          label="Exportar a Excel"
          icon="pi pi-download"
          severity="secondary"
          [outlined]="true"
          (onClick)="exportar()"
        />
      </div>
      @if (lista().length === 0) {
        <p class="text-muted">No hay reportes con ese filtro.</p>
      }
      @for (e of lista(); track e.id) {
        <p class="fila">
          <strong>{{ e.codigo }}</strong>
          <p-tag [value]="tipoEtiqueta[e.tipo]" severity="info" />
          <p-tag [value]="severidadEtiqueta[e.severidad]" severity="warn" />
          <p-tag [value]="estadoEtiqueta[e.estado]" severity="secondary" />
          <span class="text-muted"
            >{{ e.area }} · {{ e.notificadoEn | date: 'd MMM y' }} ·
            {{ e.anonimo ? 'Anónimo' : e.notificante }}</span
          >
          <p-button
            label="Gestionar"
            size="small"
            [outlined]="true"
            [ariaLabel]="'Gestionar el reporte ' + e.codigo"
            (onClick)="abrir(e)"
          />
        </p>
      }
    </section>

    @if (detalle(); as d) {
      <section class="panel detalle" aria-live="polite">
        <h3>{{ d.codigo }} · {{ estadoEtiqueta[d.estado] }}</h3>
        <p>{{ d.descripcion }}</p>
        @if (d.paciente) {
          <p class="text-muted">Paciente vinculado: {{ d.paciente }}</p>
        }
        @if (d.analisisMetodo) {
          <h4>Análisis de causa</h4>
          @for (c of d.causas; track c.posicion) {
            <p>{{ c.posicion }}. {{ c.categoria }}: {{ c.texto }}</p>
          }
          <p><strong>Conclusión:</strong> {{ d.analisisConclusion }}</p>
        }
        <h4>Acciones de mejora</h4>
        @for (a of d.acciones; track a.id) {
          <p class="fila">
            <span>{{ a.descripcion }} · {{ a.responsable }} · límite {{ a.fechaLimite }}</span>
            <p-tag
              [value]="a.estado === 'HECHA' ? 'Hecha' : a.vencida ? 'Vencida' : 'Pendiente'"
              [severity]="a.estado === 'HECHA' ? 'success' : a.vencida ? 'danger' : 'warn'"
            />
            @if (a.estado === 'PENDIENTE' && puedeGestionar) {
              <p-button
                label="Marcar hecha"
                size="small"
                [text]="true"
                (onClick)="completar(a.id)"
              />
            }
          </p>
        }
        @if (d.estado === 'CERRADO') {
          <p-message severity="success" [closable]="false">
            Cerrado el {{ d.cerradoEn | date: 'd MMM y' }} por {{ d.cerradoPor }}. Verificación:
            {{ d.verificacion }}.
          </p-message>
        } @else if (puedeGestionar) {
          <h4>Registrar análisis</h4>
          <p-select
            [options]="metodos"
            optionLabel="label"
            optionValue="value"
            ariaLabel="Método de análisis"
            [ngModel]="metodo()"
            (ngModelChange)="metodo.set($event)"
          />
          @for (n of causas(); track $index) {
            <input
              pInputText
              [attr.aria-label]="'Causa ' + ($index + 1)"
              [placeholder]="
                metodo() === 'CINCO_PORQUES' ? 'Porqué ' + ($index + 1) : 'Causa ' + ($index + 1)
              "
              [ngModel]="n"
              (ngModelChange)="cambiarCausa($index, $event)"
            />
          }
          <textarea
            pTextarea
            rows="2"
            aria-label="Conclusión del análisis"
            placeholder="Conclusión"
            [ngModel]="conclusion()"
            (ngModelChange)="conclusion.set($event)"
          ></textarea>
          <p-button
            label="Guardar análisis"
            [disabled]="conclusion().trim().length === 0 || causasLlenas().length === 0"
            (onClick)="analizar(d)"
          />
          <h4>Nueva acción de mejora asignada a mí</h4>
          <input
            pInputText
            aria-label="Descripción de la acción"
            placeholder="Descripción"
            [ngModel]="accion()"
            (ngModelChange)="accion.set($event)"
          />
          <input
            pInputText
            type="date"
            aria-label="Fecha límite"
            [ngModel]="limite()"
            (ngModelChange)="limite.set($event)"
          />
          <p-button
            label="Agregar acción"
            [disabled]="accion().trim().length === 0 || limite().length === 0"
            (onClick)="agregarAccion(d)"
          />
          <h4>Cierre</h4>
          <textarea
            pTextarea
            rows="2"
            aria-label="Verificación de eficacia"
            placeholder="Verificación de eficacia"
            [ngModel]="verificacion()"
            (ngModelChange)="verificacion.set($event)"
          ></textarea>
          <p-button
            label="Cerrar reporte"
            severity="success"
            [disabled]="verificacion().trim().length === 0"
            (onClick)="cerrar(d)"
          />
        }
      </section>
    }
  `,
  styles: `
    .kpis {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(9rem, 1fr));
      gap: 0.75rem;
      margin-bottom: 1rem;
    }

    .kpi {
      display: flex;
      flex-direction: column;
      align-items: center;
    }

    .kpi strong {
      font-size: 1.6rem;
    }

    .barra {
      display: grid;
      grid-template-columns: 5rem 1fr 2rem;
      align-items: center;
      gap: 0.5rem;
    }

    .relleno {
      height: 0.8rem;
      background: var(--p-primary-color, #0f766e);
      border-radius: 0.4rem;
      min-width: 2px;
    }

    .barra-acciones,
    .fila {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 0.5rem;
    }

    .detalle {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
      margin-top: 1rem;
    }

    section.panel {
      margin-bottom: 1rem;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EventosGestion implements OnInit {
  private readonly api = inject(SeguridadPacienteApiService);
  private readonly auth = inject(AuthService);
  private readonly avisos = inject(NotificationService);
  private readonly descargas = inject(DescargaService);

  protected readonly tipoEtiqueta = ETIQUETA_TIPO_EVENTO;
  protected readonly severidadEtiqueta = ETIQUETA_SEVERIDAD;
  protected readonly estadoEtiqueta = ETIQUETA_ESTADO_EVENTO;
  protected readonly estados = Object.entries(ETIQUETA_ESTADO_EVENTO).map(([value, label]) => ({
    value,
    label,
  }));
  protected readonly metodos = [
    { value: 'CINCO_PORQUES', label: 'Cinco porqués' },
    { value: 'ESPINA_PESCADO', label: 'Espina de pescado' },
  ];

  protected readonly puedeGestionar = this.auth.roles().includes('ROLE_CALIDAD');
  protected readonly tablero = signal<TableroSeguridad | null>(null);
  protected readonly lista = signal<EventoResumen[]>([]);
  protected readonly detalle = signal<EventoDetalle | null>(null);
  protected readonly filtro = signal<EstadoEvento | ''>('');
  protected readonly metodo = signal<MetodoAnalisis>('CINCO_PORQUES');
  protected readonly causas = signal<string[]>(['', '', '', '', '']);
  protected readonly conclusion = signal('');
  protected readonly accion = signal('');
  protected readonly limite = signal('');
  protected readonly verificacion = signal('');

  ngOnInit(): void {
    this.cargar();
  }

  protected porcentaje(valor: number, total: number): number {
    return barraProporcional(valor, total);
  }

  protected cambiarFiltro(estado: EstadoEvento | null): void {
    this.filtro.set(estado ?? '');
    this.cargar();
  }

  protected cambiarCausa(indice: number, texto: string): void {
    this.causas.update((actual) => actual.map((c, i) => (i === indice ? texto : c)));
  }

  protected causasLlenas(): string[] {
    return this.causas().filter((c) => c.trim().length > 0);
  }

  protected abrir(e: EventoResumen): void {
    this.api.detalle(e.id).subscribe((d) => this.detalle.set(d));
  }

  protected completar(accionId: string): void {
    this.aplicar(this.api.completarAccion(accionId), 'Acción completada');
  }

  protected analizar(d: EventoDetalle): void {
    this.aplicar(
      this.api.analizar(d.id, {
        metodo: this.metodo(),
        causas: this.causasLlenas().map((texto) => ({
          categoria: this.metodo() === 'CINCO_PORQUES' ? 'PORQUE' : 'CAUSA',
          texto: texto.trim(),
        })),
        conclusion: this.conclusion().trim(),
        clasificacion: null,
      }),
      'Análisis guardado',
    );
  }

  protected agregarAccion(d: EventoDetalle): void {
    this.aplicar(
      this.api.agregarAccion(d.id, {
        descripcion: this.accion().trim(),
        responsableId: this.auth.user()?.id ?? '',
        fechaLimite: this.limite(),
      }),
      'Acción agregada',
    );
  }

  protected cerrar(d: EventoDetalle): void {
    this.aplicar(
      this.api.cerrar(d.id, { verificacion: this.verificacion().trim(), accionEficaz: true }),
      'Reporte cerrado',
    );
  }

  protected exportar(): void {
    this.api.exportar().subscribe((archivo) => this.descargas.guardar(archivo));
  }

  private aplicar(
    peticion: {
      subscribe: (o: {
        next: (d: EventoDetalle) => void;
        error: (e: { error?: { message?: string } }) => void;
      }) => unknown;
    },
    mensaje: string,
  ): void {
    peticion.subscribe({
      next: (d) => {
        this.avisos.success(mensaje);
        this.detalle.set(d);
        this.cargar();
      },
      error: (e) => this.avisos.error(e.error?.message ?? 'No se pudo completar la operación'),
    });
  }

  private cargar(): void {
    this.api.tablero().subscribe((t) => this.tablero.set(t));
    this.api.listar(this.filtro()).subscribe((l) => this.lista.set(l));
  }
}
