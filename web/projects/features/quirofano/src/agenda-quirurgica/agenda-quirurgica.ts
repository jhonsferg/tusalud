import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';
import { Tag } from 'primeng/tag';

import { AuthService, CirugiaResumen, Quirofano, QuirofanoApiService } from '@tusalud/core';
import { CalEvento, Calendario, RangoVisible } from '@tusalud/ui/calendario';
import { PageHeader } from '@tusalud/ui/page-header';
import {
  ETIQUETA_ESTADO_CIRUGIA,
  SEVERIDAD_ESTADO_CIRUGIA,
  aIsoLocal,
  cirugiaAEvento,
  fechaHora,
} from '../quirofano.utils';

@Component({
  selector: 'app-agenda-quirurgica',
  imports: [FormsModule, RouterLink, Button, Message, Select, Tag, Calendario, PageHeader],
  template: `
    <app-page-header
      title="Agenda quirúrgica"
      subtitle="Quirófanos, horarios y equipos de cada cirugía"
      icon="pi-calendar-plus"
    >
      @if (auth.isMedico()) {
        <p-button label="Solicitar cirugía" icon="pi pi-plus" routerLink="nueva" />
      }
    </app-page-header>
    @if (error()) {
      <p-message severity="error">{{ error() }}</p-message>
    }
    <div class="panel barra">
      <label for="agenda-quirofano">
        Quirófano
        <p-select
          inputId="agenda-quirofano"
          [options]="opcionesQuirofano()"
          optionLabel="nombre"
          optionValue="id"
          [ngModel]="quirofanoId()"
          (ngModelChange)="quirofanoId.set($event)"
          [showClear]="true"
          placeholder="Todos"
        />
      </label>
    </div>
    <div class="panel">
      <app-calendario
        [eventos]="eventos()"
        [cargando]="cargando()"
        [bloquearPasado]="false"
        [vistas]="['semana', 'dia', 'mes']"
        textoVacio="No hay cirugías en este periodo."
        (rangoCambiado)="cargarRango($event)"
        (eventoElegido)="abrir($event)"
      />
    </div>
    <section class="panel" aria-labelledby="pendientes-titulo">
      <h2 id="pendientes-titulo">Pendientes de programar</h2>
      @if (pendientes().length === 0) {
        <p class="text-muted">No hay solicitudes pendientes.</p>
      } @else {
        <div class="tabla">
          <table>
            <caption class="sr-only">
              Cirugías solicitadas sin programar
            </caption>
            <thead>
              <tr>
                <th scope="col">Código</th>
                <th scope="col">Paciente</th>
                <th scope="col">Procedimiento</th>
                <th scope="col">Solicitante</th>
                <th scope="col">Tipo</th>
                <th scope="col"><span class="sr-only">Acciones</span></th>
              </tr>
            </thead>
            <tbody>
              @for (c of pendientes(); track c.id) {
                <tr>
                  <th scope="row">{{ c.codigo }}</th>
                  <td>{{ c.pacienteNombre ?? 'Reservado' }}</td>
                  <td>{{ c.procedimiento ?? 'Reservado' }}</td>
                  <td>{{ c.solicitanteNombre ?? '—' }}</td>
                  <td>
                    <p-tag
                      [value]="c.tipo === 'URGENCIA' ? 'Urgencia' : 'Electiva'"
                      [severity]="c.tipo === 'URGENCIA' ? 'danger' : 'secondary'"
                    />
                  </td>
                  <td>
                    <p-button
                      label="Abrir"
                      size="small"
                      [text]="true"
                      [ariaLabel]="'Abrir la cirugía ' + c.codigo"
                      (onClick)="ir(c.id)"
                    />
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </section>
    <section class="panel" aria-labelledby="lista-titulo">
      <h2 id="lista-titulo">Cirugías del periodo</h2>
      @if (programadas().length === 0) {
        <p class="text-muted">No hay cirugías programadas en el periodo visible.</p>
      } @else {
        <div class="tabla">
          <table>
            <caption class="sr-only">
              Cirugías programadas en el periodo
            </caption>
            <thead>
              <tr>
                <th scope="col">Inicio</th>
                <th scope="col">Quirófano</th>
                <th scope="col">Código</th>
                <th scope="col">Procedimiento</th>
                <th scope="col">Estado</th>
                <th scope="col"><span class="sr-only">Acciones</span></th>
              </tr>
            </thead>
            <tbody>
              @for (c of programadas(); track c.id) {
                <tr>
                  <td>{{ fechaHora(c.inicio) }}</td>
                  <td>{{ c.quirofanoNombre ?? '—' }}</td>
                  <th scope="row">{{ c.codigo }}</th>
                  <td>
                    {{ c.procedimiento ?? 'Reservado' }}
                    @if (c.enMiEquipo) {
                      <small class="text-muted block">Está en su equipo</small>
                    }
                  </td>
                  <td>
                    <p-tag [value]="estados[c.estado]" [severity]="severidades[c.estado]" />
                  </td>
                  <td>
                    <p-button
                      label="Abrir"
                      size="small"
                      [text]="true"
                      [ariaLabel]="'Abrir la cirugía ' + c.codigo"
                      (onClick)="ir(c.id)"
                    />
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </section>
  `,
  styles: `
    .panel {
      margin-bottom: 1rem;
    }

    .barra {
      display: flex;
      gap: 1rem;
      flex-wrap: wrap;
      align-items: end;
    }

    label {
      display: grid;
      gap: 0.25rem;
      font-weight: 600;
      font-size: 0.85rem;
    }

    h2 {
      margin: 0 0 0.75rem;
      font-size: 1.05rem;
    }

    .tabla {
      overflow-x: auto;
    }

    table {
      width: 100%;
      border-collapse: collapse;
      font-size: 0.92rem;
    }

    th,
    td {
      text-align: left;
      padding: 0.45rem 0.6rem;
      border-bottom: 1px solid var(--p-surface-200, #e2e8f0);
    }

    .block {
      display: block;
    }

    .sr-only {
      position: absolute;
      width: 1px;
      height: 1px;
      overflow: hidden;
      clip: rect(0 0 0 0);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AgendaQuirurgica implements OnInit {
  private readonly api = inject(QuirofanoApiService);
  private readonly router = inject(Router);
  protected readonly auth = inject(AuthService);

  protected readonly cirugias = signal<CirugiaResumen[]>([]);
  protected readonly quirofanos = signal<Quirofano[]>([]);
  protected readonly quirofanoId = signal<string | null>(null);
  protected readonly cargando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly estados = ETIQUETA_ESTADO_CIRUGIA;
  protected readonly severidades = SEVERIDAD_ESTADO_CIRUGIA;
  protected readonly fechaHora = fechaHora;

  private rango: RangoVisible | null = null;

  protected readonly opcionesQuirofano = computed(() => this.quirofanos());

  protected readonly visibles = computed(() => {
    const id = this.quirofanoId();
    return id
      ? this.cirugias().filter((c) => c.quirofanoId === id || c.estado === 'SOLICITADA')
      : this.cirugias();
  });

  protected readonly eventos = computed<CalEvento[]>(() =>
    this.visibles()
      .map(cirugiaAEvento)
      .filter((e): e is CalEvento => e !== null),
  );

  protected readonly pendientes = computed(() =>
    this.cirugias().filter((c) => c.estado === 'SOLICITADA'),
  );

  protected readonly programadas = computed(() =>
    this.visibles()
      .filter((c) => c.estado !== 'SOLICITADA' && c.inicio)
      .sort((a, b) => (a.inicio ?? '').localeCompare(b.inicio ?? '')),
  );

  ngOnInit(): void {
    this.api.quirofanos().subscribe({
      next: (lista) => this.quirofanos.set(lista),
      error: () => this.error.set('No pudimos cargar los quirófanos'),
    });
  }

  protected cargarRango(rango: RangoVisible): void {
    this.rango = rango;
    this.recargar();
  }

  protected abrir(evento: CalEvento): void {
    this.ir(evento.id);
  }

  protected ir(id: string): void {
    void this.router.navigate(['/quirofano', id]);
  }

  private recargar(): void {
    if (!this.rango) {
      return;
    }
    this.cargando.set(true);
    this.error.set(null);
    this.api.agenda(aIsoLocal(this.rango.inicio), aIsoLocal(this.rango.fin)).subscribe({
      next: (lista) => {
        this.cirugias.set(lista);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No pudimos cargar la agenda quirúrgica');
        this.cargando.set(false);
      },
    });
  }
}
