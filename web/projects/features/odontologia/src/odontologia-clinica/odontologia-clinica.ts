import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';

import {
  CambioOdontograma,
  EstadoPieza,
  NotificationService,
  Odontograma as OdontogramaDatos,
  OdontologiaApiService,
  PacienteClinicoResumen,
  SuperficieDental,
} from '@tusalud/core';
import { PageHeader } from '@tusalud/ui/page-header';
import { SelectorPacienteClinico } from '@tusalud/ui/selector-paciente-clinico';
import { Odontograma } from '../odontograma/odontograma';
import {
  ESTADOS_PIEZA_COMPLETA,
  ESTADOS_POR_SUPERFICIE,
  ETIQUETA_ESTADO_PIEZA,
  ETIQUETA_SUPERFICIE,
  SUPERFICIES,
  fechaLegible,
} from '../odontologia.utils';

@Component({
  selector: 'app-odontologia-clinica',
  imports: [
    FormsModule,
    Button,
    InputText,
    Message,
    Select,
    PageHeader,
    SelectorPacienteClinico,
    Odontograma,
  ],
  template: `
    <app-page-header
      title="Odontograma"
      subtitle="Estado de cada pieza dental con historial de cambios que no se puede modificar"
      icon="pi-heart"
    />
    <div class="disposicion">
      <app-selector-paciente-clinico
        [(seleccionado)]="paciente"
        (seleccionadoChange)="cargar($event)"
      />
      <div class="detalle">
        @if (cargando()) {
          <p class="text-muted" role="status">Cargando el odontograma…</p>
        } @else if (error()) {
          <p-message severity="error">{{ error() }}</p-message>
        } @else if (datos(); as d) {
          <div class="panel">
            <app-odontograma
              [datos]="d"
              [(seleccionada)]="pieza"
              (seleccionadaChange)="cargarHistorial()"
            />
          </div>
          @if (pieza(); as n) {
            <form class="panel formulario" (submit)="$event.preventDefault(); guardar(d, n)">
              <h3>Pieza {{ n }}</h3>
              <div class="campos">
                <label for="odo-superficie">
                  Alcance
                  <p-select
                    inputId="odo-superficie"
                    name="superficie"
                    [options]="alcances"
                    optionLabel="etiqueta"
                    optionValue="valor"
                    [(ngModel)]="superficie"
                    (ngModelChange)="ajustarEstado()"
                  />
                </label>
                <label for="odo-estado">
                  Estado
                  <p-select
                    inputId="odo-estado"
                    name="estado"
                    [options]="estadosDisponibles()"
                    optionLabel="etiqueta"
                    optionValue="valor"
                    [(ngModel)]="estado"
                  />
                </label>
                <label class="ancho">
                  Observación
                  <input pInputText name="observacion" maxlength="300" [(ngModel)]="observacion" />
                </label>
              </div>
              <div class="barra">
                <p-button
                  type="submit"
                  label="Registrar cambio"
                  icon="pi pi-check"
                  [loading]="guardando()"
                />
              </div>
            </form>
          }
          <div class="panel">
            <h3>
              Historial {{ pieza() ? 'de la pieza ' + pieza() : 'completo' }}
              <small class="text-muted">solo lectura</small>
            </h3>
            @if (historial().length === 0) {
              <p class="text-muted">Sin cambios registrados.</p>
            } @else {
              <ol class="historial">
                @for (c of historial(); track c.id) {
                  <li>
                    <strong>{{ fecha(c.registradoEn) }}</strong> · pieza {{ c.pieza }}
                    @if (c.superficie) {
                      · {{ superficies[c.superficie] }}
                    }
                    · {{ estados[c.estado] }}
                    @if (c.observacion) {
                      <small class="text-muted block">{{ c.observacion }}</small>
                    }
                    <small class="text-muted block">{{ c.registradoPor }}</small>
                  </li>
                }
              </ol>
            }
          </div>
        } @else {
          <div class="panel vacio">
            <p class="text-muted">Seleccione un paciente para ver su odontograma.</p>
          </div>
        }
      </div>
    </div>
  `,
  styles: `
    .disposicion {
      display: grid;
      gap: 1rem;
      grid-template-columns: minmax(16rem, 22rem) 1fr;
      align-items: start;
    }

    .detalle {
      display: grid;
      gap: 1rem;
      min-width: 0;
    }

    .formulario {
      display: grid;
      gap: 0.75rem;
    }

    h3 {
      margin: 0 0 0.5rem;
    }

    .campos {
      display: grid;
      gap: 0.75rem;
      grid-template-columns: repeat(auto-fit, minmax(14rem, 1fr));
    }

    label {
      display: grid;
      gap: 0.25rem;
      font-weight: 600;
      font-size: 0.85rem;
    }

    .ancho {
      grid-column: 1 / -1;
    }

    .historial {
      margin: 0;
      padding-left: 1.2rem;
      display: grid;
      gap: 0.4rem;
      max-height: 20rem;
      overflow: auto;
    }

    .block {
      display: block;
    }

    @media (max-width: 60rem) {
      .disposicion {
        grid-template-columns: 1fr;
      }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OdontologiaClinica {
  private readonly api = inject(OdontologiaApiService);
  private readonly avisos = inject(NotificationService);

  protected readonly paciente = signal<PacienteClinicoResumen | null>(null);
  protected readonly datos = signal<OdontogramaDatos | null>(null);
  protected readonly historial = signal<CambioOdontograma[]>([]);
  protected readonly pieza = signal<number | null>(null);
  protected readonly cargando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly guardando = signal(false);
  protected readonly fecha = fechaLegible;
  protected readonly estados = ETIQUETA_ESTADO_PIEZA;
  protected readonly superficies = ETIQUETA_SUPERFICIE;

  protected readonly alcances = [
    { valor: null, etiqueta: 'Pieza completa' },
    ...SUPERFICIES.map((s) => ({
      valor: s as SuperficieDental | null,
      etiqueta: ETIQUETA_SUPERFICIE[s],
    })),
  ];

  protected superficie: SuperficieDental | null = 'O';
  protected estado: EstadoPieza = 'CARIES';
  protected observacion = '';

  private readonly superficieActual = signal<SuperficieDental | null>('O');
  protected readonly estadosDisponibles = computed(() =>
    (this.superficieActual() ? ESTADOS_POR_SUPERFICIE : ESTADOS_PIEZA_COMPLETA).map((valor) => ({
      valor,
      etiqueta: ETIQUETA_ESTADO_PIEZA[valor],
    })),
  );

  protected cargar(p: PacienteClinicoResumen | null): void {
    this.datos.set(null);
    this.historial.set([]);
    this.pieza.set(null);
    this.error.set(null);
    if (!p) {
      return;
    }
    this.cargando.set(true);
    this.api.odontograma(p.id).subscribe({
      next: (d) => {
        this.datos.set(d);
        this.cargando.set(false);
        this.cargarHistorial();
      },
      error: () => {
        this.error.set('No se pudo cargar el odontograma de este paciente');
        this.cargando.set(false);
      },
    });
  }

  protected cargarHistorial(): void {
    const p = this.paciente();
    if (!p) {
      return;
    }
    const pieza = this.pieza();
    this.api.historial(p.id, pieza ?? undefined).subscribe({
      next: (lista) => this.historial.set([...lista].reverse()),
    });
  }

  protected ajustarEstado(): void {
    this.superficieActual.set(this.superficie);
    const validos = this.superficie ? ESTADOS_POR_SUPERFICIE : ESTADOS_PIEZA_COMPLETA;
    if (!validos.includes(this.estado)) {
      this.estado = validos[0];
    }
  }

  protected guardar(d: OdontogramaDatos, pieza: number): void {
    this.guardando.set(true);
    this.api
      .registrarCambio(d.pacienteId, {
        pieza,
        superficie: this.superficie,
        estado: this.estado,
        observacion: this.observacion.trim() || null,
      })
      .subscribe({
        next: () => {
          this.guardando.set(false);
          this.observacion = '';
          this.avisos.success('Cambio registrado en el historial');
          this.cargar(this.paciente());
          this.pieza.set(pieza);
        },
        error: () => this.guardando.set(false),
      });
  }
}
