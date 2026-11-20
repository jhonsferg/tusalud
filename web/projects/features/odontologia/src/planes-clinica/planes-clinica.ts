import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';
import { Tag } from 'primeng/tag';

import {
  ConsentimientoResumen,
  DocumentosMedicosApiService,
  ItemPlanOdontologico,
  NotificationService,
  OdontologiaApiService,
  PacienteClinicoResumen,
  PlanOdontologico,
  PlanOdontologicoResumen,
  ProcedimientoOdontologico,
  SuperficieDental,
} from '@tusalud/core';
import { PageHeader } from '@tusalud/ui/page-header';
import { SelectorPacienteClinico } from '@tusalud/ui/selector-paciente-clinico';
import { PlanDetalle } from '../plan-detalle/plan-detalle';
import {
  ETIQUETA_ESTADO_PLAN,
  ETIQUETA_SUPERFICIE,
  SEVERIDAD_ESTADO_PLAN,
  SUPERFICIES,
  fechaLegible,
  soles,
} from '../odontologia.utils';

@Component({
  selector: 'app-planes-clinica',
  imports: [
    FormsModule,
    Button,
    InputNumber,
    InputText,
    Message,
    Select,
    Tag,
    PageHeader,
    SelectorPacienteClinico,
    PlanDetalle,
  ],
  template: `
    <app-page-header
      title="Planes de tratamiento"
      subtitle="Presupuesto odontológico por procedimiento, aceptación del paciente y avance por sesión"
      icon="pi-list-check"
    />
    <div class="disposicion">
      <app-selector-paciente-clinico
        [(seleccionado)]="paciente"
        (seleccionadoChange)="cargar($event)"
      />
      <div class="detalle">
        @if (error()) {
          <p-message severity="error">{{ error() }}</p-message>
        }
        @if (paciente(); as p) {
          <div class="panel">
            <div class="cabecera">
              <h3>Planes de {{ p.nombre }}</h3>
              <p-button
                label="Nuevo plan"
                icon="pi pi-plus"
                size="small"
                [loading]="guardando()"
                (onClick)="crear(p)"
              />
            </div>
            @if (planes().length === 0) {
              <p class="text-muted">Este paciente aún no tiene planes de tratamiento.</p>
            } @else {
              <ul class="planes">
                @for (r of planes(); track r.id) {
                  <li>
                    <button
                      type="button"
                      class="plan-item"
                      [class.activo]="plan()?.id === r.id"
                      [attr.aria-pressed]="plan()?.id === r.id"
                      (click)="abrir(r.id)"
                    >
                      <span>{{ fecha(r.creadoEn) }} · {{ soles(r.total) }}</span>
                      <p-tag
                        [value]="etiquetaPlan[r.estado]"
                        [severity]="severidadPlan[r.estado]"
                      />
                    </button>
                  </li>
                }
              </ul>
            }
          </div>
        }
        @if (plan(); as pl) {
          <div class="panel">
            <app-plan-detalle
              [plan]="pl"
              [editable]="true"
              (quitar)="quitar(pl, $event)"
              (vincular)="elegirConsentimiento($event)"
            >
              <div class="barra">
                @if (pl.estado === 'BORRADOR') {
                  <p-button
                    label="Presentar al paciente"
                    icon="pi pi-send"
                    [loading]="guardando()"
                    (onClick)="presentar(pl)"
                  />
                }
                @if (puedeCancelar(pl)) {
                  <p-button
                    label="Cancelar plan"
                    severity="danger"
                    [outlined]="true"
                    (onClick)="cancelando.set(true)"
                  />
                }
              </div>
            </app-plan-detalle>
            @if (cancelando()) {
              <form class="inline" (submit)="$event.preventDefault(); cancelar(pl)">
                <label>
                  Motivo de la cancelación
                  <input
                    pInputText
                    name="motivoCancelacion"
                    minlength="5"
                    maxlength="300"
                    [(ngModel)]="motivo"
                    required
                  />
                </label>
                <p-button
                  type="submit"
                  label="Confirmar cancelación"
                  severity="danger"
                  [loading]="guardando()"
                />
                <p-button
                  label="Volver"
                  severity="secondary"
                  [text]="true"
                  (onClick)="cancelando.set(false)"
                />
              </form>
            }
          </div>
          @if (pl.estado === 'BORRADOR') {
            <form class="panel formulario" (submit)="$event.preventDefault(); agregar(pl)">
              <h3>Agregar procedimiento</h3>
              <div class="campos">
                <label for="plan-proc">
                  Procedimiento
                  <p-select
                    inputId="plan-proc"
                    name="procedimiento"
                    [options]="procedimientos()"
                    optionLabel="nombre"
                    optionValue="id"
                    [filter]="true"
                    [ngModel]="procedimientoId()"
                    (ngModelChange)="procedimientoId.set($event)"
                    placeholder="Elija un procedimiento"
                  />
                </label>
                @if (seleccionado(); as s) {
                  @if (s.requierePieza) {
                    <label for="plan-pieza">
                      Pieza FDI
                      <p-inputnumber
                        inputId="plan-pieza"
                        name="pieza"
                        [(ngModel)]="pieza"
                        [min]="11"
                        [max]="85"
                        [useGrouping]="false"
                      />
                    </label>
                  }
                  @if (s.requiereSuperficie) {
                    <label for="plan-sup">
                      Superficie
                      <p-select
                        inputId="plan-sup"
                        name="superficie"
                        [options]="superficiesOpc"
                        optionLabel="etiqueta"
                        optionValue="valor"
                        [(ngModel)]="superficie"
                      />
                    </label>
                  }
                  <p class="text-muted ancho">
                    {{ s.categoria }} · {{ soles(s.precio) }}
                    @if (s.requiereConsentimiento) {
                      · requiere consentimiento firmado antes de ejecutarse
                    }
                  </p>
                }
              </div>
              <div class="barra">
                <p-button
                  type="submit"
                  label="Agregar"
                  icon="pi pi-plus"
                  [loading]="guardando()"
                  [disabled]="!procedimientoId()"
                />
              </div>
            </form>
          }
          @if (puedeRegistrarSesion(pl)) {
            <form class="panel formulario" (submit)="$event.preventDefault(); registrarSesion(pl)">
              <h3>Registrar una sesión</h3>
              <fieldset>
                <legend>Procedimientos realizados hoy</legend>
                @for (i of pendientes(pl); track i.id) {
                  <label class="check">
                    <input
                      type="checkbox"
                      [checked]="marcados().has(i.id)"
                      (change)="marcar(i.id, $any($event.target).checked)"
                    />
                    {{ i.nombre }}{{ i.pieza ? ' · pieza ' + i.pieza : '' }}
                  </label>
                }
              </fieldset>
              <label>
                Notas de la sesión
                <input pInputText name="notas" maxlength="500" [(ngModel)]="notasSesion" />
              </label>
              <div class="barra">
                <p-button
                  type="submit"
                  label="Guardar sesión"
                  icon="pi pi-check"
                  [loading]="guardando()"
                  [disabled]="marcados().size === 0"
                />
              </div>
            </form>
          }
          @if (vinculando(); as item) {
            <div class="panel formulario">
              <h3>Vincular consentimiento a {{ item.nombre }}</h3>
              @if (consentimientos().length === 0) {
                <p class="text-muted">
                  El paciente no tiene consentimientos firmados y vigentes. Solicítelo desde
                  Consentimientos.
                </p>
              } @else {
                <label for="plan-cons">
                  Consentimiento firmado
                  <p-select
                    inputId="plan-cons"
                    name="consentimiento"
                    [options]="consentimientos()"
                    optionLabel="procedimiento"
                    optionValue="id"
                    [(ngModel)]="consentimientoId"
                  />
                </label>
              }
              <div class="barra">
                <p-button
                  label="Vincular"
                  [disabled]="!consentimientoId"
                  [loading]="guardando()"
                  (onClick)="vincular(pl, item)"
                />
                <p-button
                  label="Cerrar"
                  severity="secondary"
                  [text]="true"
                  (onClick)="vinculando.set(null)"
                />
              </div>
            </div>
          }
        }
        @if (!paciente()) {
          <div class="panel vacio">
            <p class="text-muted">
              Seleccione un paciente para ver y crear sus planes de tratamiento.
            </p>
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

    .cabecera,
    .plan-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 0.75rem;
    }

    h3 {
      margin: 0;
    }

    .planes {
      list-style: none;
      margin: 0.75rem 0 0;
      padding: 0;
      display: grid;
      gap: 0.35rem;
    }

    .plan-item {
      width: 100%;
      padding: 0.5rem 0.75rem;
      border: 1px solid var(--p-surface-300, #cbd5e1);
      border-radius: 0.5rem;
      background: var(--p-surface-0, #fff);
      cursor: pointer;
      text-align: left;
    }

    .plan-item.activo {
      border-color: var(--p-primary-600, #2563eb);
      background: var(--p-primary-50, #eff6ff);
    }

    .barra,
    .inline {
      display: flex;
      gap: 0.5rem;
      flex-wrap: wrap;
      align-items: end;
      margin-top: 0.5rem;
    }

    .formulario {
      display: grid;
      gap: 0.75rem;
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

    .check {
      display: flex;
      gap: 0.5rem;
      align-items: center;
      font-weight: 400;
    }

    .ancho {
      grid-column: 1 / -1;
      margin: 0;
    }

    fieldset {
      border: 1px solid var(--p-surface-300, #cbd5e1);
      border-radius: 0.5rem;
      display: grid;
      gap: 0.35rem;
    }

    @media (max-width: 60rem) {
      .disposicion {
        grid-template-columns: 1fr;
      }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PlanesClinica {
  private readonly api = inject(OdontologiaApiService);
  private readonly documentos = inject(DocumentosMedicosApiService);
  private readonly avisos = inject(NotificationService);

  protected readonly paciente = signal<PacienteClinicoResumen | null>(null);
  protected readonly planes = signal<PlanOdontologicoResumen[]>([]);
  protected readonly plan = signal<PlanOdontologico | null>(null);
  protected readonly procedimientos = signal<ProcedimientoOdontologico[]>([]);
  protected readonly consentimientos = signal<ConsentimientoResumen[]>([]);
  protected readonly marcados = signal<ReadonlySet<string>>(new Set());
  protected readonly vinculando = signal<ItemPlanOdontologico | null>(null);
  protected readonly cancelando = signal(false);
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly fecha = fechaLegible;
  protected readonly soles = soles;
  protected readonly etiquetaPlan = ETIQUETA_ESTADO_PLAN;
  protected readonly severidadPlan = SEVERIDAD_ESTADO_PLAN;
  protected readonly superficiesOpc = SUPERFICIES.map((s) => ({
    valor: s,
    etiqueta: ETIQUETA_SUPERFICIE[s],
  }));

  protected readonly procedimientoId = signal<string | null>(null);
  protected pieza: number | null = null;
  protected superficie: SuperficieDental | null = null;
  protected consentimientoId: string | null = null;
  protected notasSesion = '';
  protected motivo = '';

  protected readonly seleccionado = computed(
    () => this.procedimientos().find((p) => p.id === this.procedimientoId()) ?? null,
  );

  constructor() {
    this.api.procedimientos().subscribe((lista) => this.procedimientos.set(lista));
  }

  protected cargar(p: PacienteClinicoResumen | null): void {
    this.planes.set([]);
    this.plan.set(null);
    this.error.set(null);
    if (!p) {
      return;
    }
    this.api.planesDelPaciente(p.id).subscribe({
      next: (lista) => {
        this.planes.set(lista);
        if (lista.length) {
          this.abrir(lista[0].id);
        }
      },
      error: () => this.error.set('No se pudieron cargar los planes de este paciente'),
    });
  }

  protected abrir(id: string): void {
    this.cancelando.set(false);
    this.vinculando.set(null);
    this.marcados.set(new Set());
    this.api.plan(id).subscribe({
      next: (pl) => this.plan.set(pl),
      error: () => this.error.set('No se pudo abrir el plan'),
    });
  }

  protected crear(p: PacienteClinicoResumen): void {
    this.guardando.set(true);
    this.api.crearPlan(p.id, null).subscribe({
      next: (pl) => {
        this.guardando.set(false);
        this.avisos.success('Plan creado en borrador');
        this.plan.set(pl);
        this.refrescarLista(p.id);
      },
      error: () => this.guardando.set(false),
    });
  }

  protected agregar(pl: PlanOdontologico): void {
    const procedimientoId = this.procedimientoId();
    if (!procedimientoId) {
      return;
    }
    this.guardando.set(true);
    this.api
      .agregarItem(pl.id, {
        procedimientoId,
        pieza: this.seleccionado()?.requierePieza ? this.pieza : null,
        superficie: this.seleccionado()?.requiereSuperficie ? this.superficie : null,
      })
      .subscribe({
        next: (actualizado) => {
          this.guardando.set(false);
          this.plan.set(actualizado);
          this.procedimientoId.set(null);
          this.pieza = null;
          this.superficie = null;
          this.refrescarLista(actualizado.pacienteId);
        },
        error: () => this.guardando.set(false),
      });
  }

  protected quitar(pl: PlanOdontologico, item: ItemPlanOdontologico): void {
    this.api.quitarItem(pl.id, item.id).subscribe((actualizado) => {
      this.plan.set(actualizado);
      this.refrescarLista(actualizado.pacienteId);
    });
  }

  protected presentar(pl: PlanOdontologico): void {
    this.guardando.set(true);
    this.api.presentar(pl.id).subscribe({
      next: (actualizado) => {
        this.guardando.set(false);
        this.plan.set(actualizado);
        this.avisos.success('El plan fue presentado al paciente');
        this.refrescarLista(actualizado.pacienteId);
      },
      error: () => this.guardando.set(false),
    });
  }

  protected cancelar(pl: PlanOdontologico): void {
    if (this.motivo.trim().length < 5) {
      return;
    }
    this.guardando.set(true);
    this.api.cancelar(pl.id, this.motivo.trim()).subscribe({
      next: (actualizado) => {
        this.guardando.set(false);
        this.cancelando.set(false);
        this.motivo = '';
        this.plan.set(actualizado);
        this.refrescarLista(actualizado.pacienteId);
      },
      error: () => this.guardando.set(false),
    });
  }

  protected puedeCancelar(pl: PlanOdontologico): boolean {
    return !['COMPLETADO', 'CANCELADO', 'RECHAZADO'].includes(pl.estado);
  }

  protected puedeRegistrarSesion(pl: PlanOdontologico): boolean {
    return ['ACEPTADO', 'EN_CURSO'].includes(pl.estado) && this.pendientes(pl).length > 0;
  }

  protected pendientes(pl: PlanOdontologico): ItemPlanOdontologico[] {
    return pl.items.filter((i) => i.estado === 'PENDIENTE');
  }

  protected marcar(id: string, marcado: boolean): void {
    const nuevo = new Set(this.marcados());
    if (marcado) {
      nuevo.add(id);
    } else {
      nuevo.delete(id);
    }
    this.marcados.set(nuevo);
  }

  protected registrarSesion(pl: PlanOdontologico): void {
    this.guardando.set(true);
    this.api
      .registrarSesion(pl.id, {
        itemIds: [...this.marcados()],
        notas: this.notasSesion.trim() || null,
      })
      .subscribe({
        next: (actualizado) => {
          this.guardando.set(false);
          this.plan.set(actualizado);
          this.marcados.set(new Set());
          this.notasSesion = '';
          this.avisos.success('Sesión registrada y odontograma actualizado');
          this.refrescarLista(actualizado.pacienteId);
        },
        error: () => this.guardando.set(false),
      });
  }

  protected elegirConsentimiento(item: ItemPlanOdontologico): void {
    this.vinculando.set(item);
    this.consentimientoId = null;
    const p = this.paciente();
    if (p) {
      this.documentos
        .consentimientosDelPaciente(p.id)
        .subscribe((lista) =>
          this.consentimientos.set(lista.filter((c) => c.estado === 'FIRMADO')),
        );
    }
  }

  protected vincular(pl: PlanOdontologico, item: ItemPlanOdontologico): void {
    if (!this.consentimientoId) {
      return;
    }
    this.guardando.set(true);
    this.api.vincularConsentimiento(pl.id, item.id, this.consentimientoId).subscribe({
      next: (actualizado) => {
        this.guardando.set(false);
        this.plan.set(actualizado);
        this.vinculando.set(null);
        this.avisos.success('Consentimiento vinculado');
      },
      error: () => this.guardando.set(false),
    });
  }

  private refrescarLista(pacienteId: string): void {
    this.api.planesDelPaciente(pacienteId).subscribe((lista) => this.planes.set(lista));
  }
}
