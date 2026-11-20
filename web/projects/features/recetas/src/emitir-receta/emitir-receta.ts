import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  input,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Textarea } from 'primeng/textarea';
import { Subject, catchError, debounceTime, of, switchMap, tap } from 'rxjs';

import {
  AlertaReceta,
  AlertasRecetaRequest,
  ApiError,
  AtencionResponse,
  ClinicaApiService,
  EmitirRecetaRequest,
  Medicamento,
  NotificationService,
  RecetasApiService,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { SelectorMedicamento } from '@tusalud/ui/selector-medicamento';
import { PanelAlertas } from '../panel-alertas/panel-alertas';
import { dosisDiariaMg, justificacionesFaltantes } from '../recetas.utils';

interface FilaEmision {
  posicion: number;
  texto: string;
  medicamento: Medicamento | null;
  cantidad: number | null;
  via: string;
  dosisUnitariaMg: number | null;
  vecesPorDia: number | null;
}

@Component({
  selector: 'app-emitir-receta',
  imports: [
    FormsModule,
    RouterLink,
    Button,
    InputNumber,
    InputText,
    Message,
    Textarea,
    EmptyState,
    PageHeader,
    SelectorMedicamento,
    PanelAlertas,
  ],
  template: `
    <app-page-header
      title="Emitir receta electrónica"
      subtitle="Asocia cada línea de la receta con un medicamento del catálogo, revisa las alertas y firma"
      icon="pi-file-edit"
    />
    @if (cargando()) {
      <div class="panel"><p class="text-muted" role="status">Cargando la atención…</p></div>
    } @else if (error()) {
      <p-message severity="error">{{ error() }}</p-message>
    } @else if (atencion(); as a) {
      @if (filas().length === 0) {
        <div class="panel">
          <app-empty-state
            icon="pi-file-edit"
            title="La atención no tiene medicamentos"
            message="Agrega los medicamentos en la receta de la atención y vuelve a esta pantalla."
          />
          <p-button
            label="Volver a la atención"
            severity="secondary"
            [outlined]="true"
            [routerLink]="['/citas', a.citaId, 'atencion']"
          />
        </div>
      } @else {
        <div class="panel contexto">
          <div>
            <strong>{{ a.pacienteNombre }}</strong>
            <span class="text-muted"> · {{ a.especialidad }}</span>
          </div>
          @if (reemplaza()) {
            <p-message severity="info"
              >Esta receta reemplazará a una receta anulada de la misma atención</p-message
            >
          }
        </div>

        <form class="panel formulario" (submit)="$event.preventDefault(); emitir()">
          <h3>Medicamentos</h3>
          @for (fila of filas(); track fila.posicion; let i = $index) {
            <fieldset class="fila">
              <legend>Línea {{ i + 1 }}: {{ fila.texto }}</legend>
              @if (fila.medicamento; as m) {
                <div class="elegido">
                  <div>
                    <strong>{{ m.nombre }}</strong>
                    <small class="text-muted block"
                      >{{ m.viaAdministracion }}
                      @if (m.categoriaControl !== 'NINGUNA') {
                        · control especial: {{ m.categoriaControl }}
                      }
                    </small>
                  </div>
                  <p-button
                    label="Cambiar"
                    size="small"
                    [text]="true"
                    [ariaLabel]="'Cambiar el medicamento de la línea ' + (i + 1)"
                    (onClick)="cambiarMedicamento(i)"
                  />
                </div>
                <div class="campos">
                  <label [for]="'cantidad-' + i">
                    Cantidad a dispensar
                    <p-inputnumber
                      [inputId]="'cantidad-' + i"
                      [ngModel]="fila.cantidad"
                      (ngModelChange)="cambiar(i, { cantidad: $event })"
                      [name]="'cantidad-' + i"
                      [min]="1"
                      [max]="1000"
                      [useGrouping]="false"
                    />
                  </label>
                  <label [for]="'dosis-' + i">
                    Dosis por toma en mg
                    <p-inputnumber
                      [inputId]="'dosis-' + i"
                      [ngModel]="fila.dosisUnitariaMg"
                      (ngModelChange)="cambiar(i, { dosisUnitariaMg: $event })"
                      [name]="'dosis-' + i"
                      [min]="0"
                      [maxFractionDigits]="2"
                      [useGrouping]="false"
                    />
                  </label>
                  <label [for]="'veces-' + i">
                    Veces por día
                    <p-inputnumber
                      [inputId]="'veces-' + i"
                      [ngModel]="fila.vecesPorDia"
                      (ngModelChange)="cambiar(i, { vecesPorDia: $event })"
                      [name]="'veces-' + i"
                      [min]="1"
                      [max]="24"
                      [useGrouping]="false"
                    />
                  </label>
                  <label [for]="'via-' + i">
                    Vía
                    <input
                      pInputText
                      [id]="'via-' + i"
                      [name]="'via-' + i"
                      maxlength="30"
                      [ngModel]="fila.via"
                      (ngModelChange)="cambiar(i, { via: $event })"
                    />
                  </label>
                </div>
                @if (diaria(fila); as d) {
                  <small class="text-muted">Dosis diaria total: {{ d }} mg</small>
                }
              } @else {
                <app-selector-medicamento
                  [inputId]="'medicamento-' + i"
                  [etiqueta]="'Buscar el medicamento de la línea ' + (i + 1)"
                  (elegido)="elegir(i, $event)"
                />
              }
            </fieldset>
          }

          <div class="opciones">
            <label class="check"
              ><input
                type="checkbox"
                name="embarazada"
                [ngModel]="embarazada()"
                (ngModelChange)="cambiarEmbarazo($event)"
              />
              La paciente está embarazada</label
            >
            <label class="check"
              ><input
                type="checkbox"
                name="lactando"
                [ngModel]="lactando()"
                (ngModelChange)="cambiarLactancia($event)"
              />
              La paciente está lactando</label
            >
          </div>
          <div class="campos">
            <label for="vigencia">
              Vigencia en días
              <p-inputnumber
                inputId="vigencia"
                name="vigencia"
                [(ngModel)]="vigenciaDias"
                [min]="1"
                [max]="90"
                [useGrouping]="false"
                placeholder="Según el tipo de medicamento"
              />
            </label>
          </div>
          <label for="indicaciones">
            Indicaciones para el paciente
            <textarea
              pTextarea
              id="indicaciones"
              name="indicaciones"
              rows="3"
              maxlength="1000"
              [(ngModel)]="indicaciones"
            ></textarea>
          </label>

          <app-panel-alertas
            [alertas]="alertas()"
            [justificaciones]="justificaciones()"
            [cargando]="evaluando()"
            (cambio)="justificar($event.clave, $event.texto)"
          />

          @if (mensajeBloqueo(); as motivo) {
            <p class="text-muted" role="status">{{ motivo }}</p>
          }
          @if (errorEmision()) {
            <p-message severity="error">{{ errorEmision() }}</p-message>
          }
          <div class="acciones">
            <p-button
              label="Cancelar"
              severity="secondary"
              [outlined]="true"
              [routerLink]="['/citas', a.citaId, 'atencion']"
            />
            <p-button
              type="submit"
              label="Firmar y emitir"
              icon="pi pi-check"
              [loading]="emitiendo()"
              [disabled]="!puedeEmitir()"
            />
          </div>
        </form>
      }
    }
  `,
  styles: `
    .contexto {
      display: flex;
      align-items: center;
      justify-content: space-between;
      flex-wrap: wrap;
      gap: 0.75rem;
      margin-bottom: 1rem;
    }

    .formulario {
      display: grid;
      gap: 1rem;
    }

    h3 {
      margin: 0;
    }

    .fila {
      border: 1px solid var(--p-surface-200, #e2e8f0);
      border-radius: 0.5rem;
      padding: 0.75rem;
      display: grid;
      gap: 0.6rem;
    }

    legend {
      font-weight: 600;
      padding: 0 0.4rem;
    }

    .elegido {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 0.5rem;
    }

    .block {
      display: block;
    }

    .campos {
      display: grid;
      gap: 0.75rem;
      grid-template-columns: repeat(auto-fit, minmax(12rem, 1fr));
    }

    label {
      display: grid;
      gap: 0.25rem;
      font-weight: 600;
    }

    .opciones {
      display: flex;
      flex-wrap: wrap;
      gap: 1.5rem;
    }

    .check {
      display: flex;
      align-items: center;
      gap: 0.4rem;
      font-weight: 500;
    }

    textarea {
      width: 100%;
    }

    .acciones {
      display: flex;
      justify-content: flex-end;
      gap: 0.5rem;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EmitirReceta implements OnInit {
  readonly citaId = input.required<string>();
  readonly reemplaza = input<string | undefined>(undefined);

  private readonly clinica = inject(ClinicaApiService);
  private readonly api = inject(RecetasApiService);
  private readonly router = inject(Router);
  private readonly notifications = inject(NotificationService);
  private readonly solicitudes = new Subject<AlertasRecetaRequest | null>();

  protected readonly atencion = signal<AtencionResponse | null>(null);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly filas = signal<FilaEmision[]>([]);
  protected readonly alertas = signal<AlertaReceta[]>([]);
  protected readonly justificaciones = signal<Record<string, string>>({});
  protected readonly evaluando = signal(false);
  protected readonly emitiendo = signal(false);
  protected readonly errorEmision = signal<string | null>(null);
  protected readonly embarazada = signal(false);
  protected readonly lactando = signal(false);
  protected vigenciaDias: number | null = null;
  protected indicaciones = '';

  protected readonly completas = computed(() =>
    this.filas().every((f) => f.medicamento !== null && (f.cantidad ?? 0) >= 1),
  );
  protected readonly faltantes = computed(() =>
    justificacionesFaltantes(this.alertas(), this.justificaciones()),
  );
  protected readonly mensajeBloqueo = computed(() => {
    if (!this.completas()) {
      return 'Asocia cada línea con un medicamento del catálogo e indica la cantidad.';
    }
    if (this.evaluando()) {
      return 'Revisando las alertas…';
    }
    if (this.faltantes().length > 0) {
      return 'Faltan justificaciones para las alertas graves o contraindicadas.';
    }
    return null;
  });
  protected readonly puedeEmitir = computed(
    () => this.mensajeBloqueo() === null && !this.emitiendo(),
  );

  constructor() {
    this.solicitudes
      .pipe(
        tap(() => this.evaluando.set(true)),
        debounceTime(300),
        switchMap((solicitud) =>
          solicitud
            ? this.api.alertas(solicitud).pipe(catchError(() => of(null)))
            : of({
                alertas: [],
                pendientesDeJustificar: 0,
                hayContraindicadas: false,
                porValidar: true,
              }),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((respuesta) => {
        this.alertas.set(respuesta?.alertas ?? []);
        this.evaluando.set(false);
      });
  }

  ngOnInit(): void {
    this.clinica.obtenerAtencion(this.citaId()).subscribe({
      next: (a) => {
        this.atencion.set(a);
        this.filas.set(
          a.receta.map((linea, posicion) => ({
            posicion,
            texto: [linea.medicamento, linea.dosis, linea.frecuencia, linea.duracion]
              .filter((x) => !!x)
              .join(' · '),
            medicamento: null,
            cantidad: 1,
            via: '',
            dosisUnitariaMg: null,
            vecesPorDia: null,
          })),
        );
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar la atención de la cita');
        this.cargando.set(false);
      },
    });
  }

  protected diaria(fila: FilaEmision): number | null {
    return dosisDiariaMg(fila.dosisUnitariaMg, fila.vecesPorDia);
  }

  protected elegir(indice: number, medicamento: Medicamento): void {
    this.cambiar(indice, { medicamento, via: medicamento.viaAdministracion });
  }

  protected cambiarMedicamento(indice: number): void {
    this.cambiar(indice, { medicamento: null });
  }

  protected cambiar(indice: number, cambios: Partial<FilaEmision>): void {
    this.filas.update((filas) => filas.map((f, i) => (i === indice ? { ...f, ...cambios } : f)));
    this.evaluar();
  }

  protected cambiarEmbarazo(valor: boolean): void {
    this.embarazada.set(valor);
    this.evaluar();
  }

  protected cambiarLactancia(valor: boolean): void {
    this.lactando.set(valor);
    this.evaluar();
  }

  protected justificar(clave: string, texto: string): void {
    this.justificaciones.update((actual) => ({ ...actual, [clave]: texto }));
  }

  private evaluar(): void {
    const a = this.atencion();
    const elegidas = this.filas().filter((f) => f.medicamento !== null);
    if (!a || elegidas.length === 0) {
      this.solicitudes.next(null);
      return;
    }
    this.solicitudes.next({
      pacienteId: a.pacienteId,
      items: elegidas.map((f) => ({
        medicamentoId: (f.medicamento as Medicamento).id,
        dosisUnitariaMg: f.dosisUnitariaMg,
        vecesPorDia: f.vecesPorDia,
      })),
      embarazada: this.embarazada(),
      lactando: this.lactando(),
    });
  }

  protected emitir(): void {
    const a = this.atencion();
    if (!a || !this.puedeEmitir()) {
      return;
    }
    const claves = new Set(this.alertas().map((x) => x.clave));
    const solicitud: EmitirRecetaRequest = {
      atencionId: a.id,
      vigenciaDias: this.vigenciaDias,
      indicaciones: this.indicaciones.trim() || null,
      embarazada: this.embarazada(),
      lactando: this.lactando(),
      reemplazaId: this.reemplaza() ?? null,
      items: this.filas().map((f) => ({
        atencionPosicion: f.posicion,
        medicamentoId: (f.medicamento as Medicamento).id,
        cantidad: f.cantidad as number,
        via: f.via.trim() || null,
        dosisUnitariaMg: f.dosisUnitariaMg,
        vecesPorDia: f.vecesPorDia,
      })),
      justificaciones: Object.entries(this.justificaciones())
        .filter(([clave, texto]) => claves.has(clave) && texto.trim().length > 0)
        .map(([clave, texto]) => ({ clave, texto: texto.trim() })),
    };
    this.errorEmision.set(null);
    this.emitiendo.set(true);
    this.api.emitir(solicitud).subscribe({
      next: (receta) => {
        this.emitiendo.set(false);
        this.notifications.success(`Receta ${receta.numero} emitida y firmada.`);
        void this.router.navigate(['/recetas', receta.id]);
      },
      error: (error: ApiError) => {
        this.emitiendo.set(false);
        this.errorEmision.set(error.message ?? 'No se pudo emitir la receta');
      },
    });
  }
}
