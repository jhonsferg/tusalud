import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Tag } from 'primeng/tag';

import {
  NotificationService,
  Odontograma as OdontogramaDatos,
  OdontologiaApiService,
  PlanOdontologico,
  PlanOdontologicoResumen,
} from '@tusalud/core';
import { PageHeader } from '@tusalud/ui/page-header';
import { Odontograma } from '../odontograma/odontograma';
import { PlanDetalle } from '../plan-detalle/plan-detalle';
import {
  ETIQUETA_ESTADO_PIEZA,
  ETIQUETA_ESTADO_PLAN,
  SEVERIDAD_ESTADO_PLAN,
  fechaLegible,
  soles,
} from '../odontologia.utils';

@Component({
  selector: 'app-mis-planes-odontologicos',
  imports: [FormsModule, Button, InputText, Message, Tag, PageHeader, Odontograma, PlanDetalle],
  template: `
    <app-page-header
      title="Mi salud dental"
      subtitle="Tu odontograma y los presupuestos de tratamiento que te presentó tu odontólogo"
      icon="pi-heart"
    />
    @if (error()) {
      <p-message severity="error">{{ error() }}</p-message>
    }
    @if (odontograma(); as o) {
      <section class="panel">
        <h2>Mi odontograma</h2>
        <app-odontograma [datos]="o" [(seleccionada)]="pieza" />
        @if (pieza(); as n) {
          <p class="text-muted" role="status">Pieza {{ n }} seleccionada. {{ descripcion(n) }}</p>
        }
      </section>
    }
    <section class="panel">
      <h2>Mis planes de tratamiento</h2>
      @if (cargando()) {
        <p class="text-muted" role="status">Cargando…</p>
      } @else if (planes().length === 0) {
        <p class="text-muted">Aún no tienes planes de tratamiento presentados.</p>
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
                <span
                  >{{ fecha(r.creadoEn) }} · {{ soles(r.total) }} · saldo {{ soles(r.saldo) }}</span
                >
                <p-tag [value]="etiquetaPlan[r.estado]" [severity]="severidadPlan[r.estado]" />
              </button>
            </li>
          }
        </ul>
      }
    </section>
    @if (plan(); as pl) {
      <section class="panel">
        <app-plan-detalle [plan]="pl">
          @if (pl.estado === 'PRESENTADO') {
            <div class="barra">
              <p-button
                label="Aceptar el plan"
                icon="pi pi-check"
                [loading]="guardando()"
                (onClick)="aceptar(pl)"
              />
              <p-button
                label="Rechazar"
                severity="danger"
                [outlined]="true"
                (onClick)="rechazando.set(true)"
              />
            </div>
            @if (rechazando()) {
              <form class="inline" (submit)="$event.preventDefault(); rechazar(pl)">
                <label>
                  Cuéntanos el motivo
                  <input
                    pInputText
                    name="motivo"
                    minlength="5"
                    maxlength="300"
                    [(ngModel)]="motivo"
                    required
                  />
                </label>
                <p-button type="submit" label="Enviar" severity="danger" [loading]="guardando()" />
                <p-button
                  label="Volver"
                  severity="secondary"
                  [text]="true"
                  (onClick)="rechazando.set(false)"
                />
              </form>
            }
          }
        </app-plan-detalle>
      </section>
    }
  `,
  styles: `
    .panel {
      display: grid;
      gap: 0.75rem;
      margin-bottom: 1rem;
    }

    h2 {
      margin: 0;
      font-size: 1.1rem;
    }

    .planes {
      list-style: none;
      margin: 0;
      padding: 0;
      display: grid;
      gap: 0.35rem;
    }

    .plan-item {
      width: 100%;
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 0.75rem;
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
    }

    label {
      display: grid;
      gap: 0.25rem;
      font-weight: 600;
      font-size: 0.85rem;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MisPlanesOdontologicos implements OnInit {
  private readonly api = inject(OdontologiaApiService);
  private readonly avisos = inject(NotificationService);

  protected readonly odontograma = signal<OdontogramaDatos | null>(null);
  protected readonly planes = signal<PlanOdontologicoResumen[]>([]);
  protected readonly plan = signal<PlanOdontologico | null>(null);
  protected readonly pieza = signal<number | null>(null);
  protected readonly cargando = signal(true);
  protected readonly guardando = signal(false);
  protected readonly rechazando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly fecha = fechaLegible;
  protected readonly soles = soles;
  protected readonly etiquetaPlan = ETIQUETA_ESTADO_PLAN;
  protected readonly severidadPlan = SEVERIDAD_ESTADO_PLAN;
  protected motivo = '';

  ngOnInit(): void {
    this.api.odontogramaMio().subscribe({ next: (o) => this.odontograma.set(o) });
    this.recargar();
  }

  protected descripcion(n: number): string {
    const p = this.odontograma()?.piezas.find((x) => x.pieza === n);
    if (!p) {
      return 'Sin registros.';
    }
    const partes = p.estado ? [ETIQUETA_ESTADO_PIEZA[p.estado]] : [];
    for (const [superficie, estado] of Object.entries(p.superficies)) {
      partes.push(`${superficie}: ${ETIQUETA_ESTADO_PIEZA[estado]}`);
    }
    return partes.join(' · ') || 'Sin registros.';
  }

  protected abrir(id: string): void {
    this.rechazando.set(false);
    this.api.plan(id).subscribe({
      next: (pl) => this.plan.set(pl),
      error: () => this.error.set('No se pudo abrir el plan'),
    });
  }

  protected aceptar(pl: PlanOdontologico): void {
    this.guardando.set(true);
    this.api.aceptar(pl.id).subscribe({
      next: (actualizado) => {
        this.guardando.set(false);
        this.plan.set(actualizado);
        this.avisos.success('Aceptaste el plan de tratamiento');
        this.recargar();
      },
      error: () => this.guardando.set(false),
    });
  }

  protected rechazar(pl: PlanOdontologico): void {
    if (this.motivo.trim().length < 5) {
      return;
    }
    this.guardando.set(true);
    this.api.rechazar(pl.id, this.motivo.trim()).subscribe({
      next: (actualizado) => {
        this.guardando.set(false);
        this.rechazando.set(false);
        this.plan.set(actualizado);
        this.recargar();
      },
      error: () => this.guardando.set(false),
    });
  }

  private recargar(): void {
    this.api.planesMios().subscribe({
      next: (lista) => {
        this.planes.set(lista);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar tus planes');
        this.cargando.set(false);
      },
    });
  }
}
