import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';

import {
  AuthService,
  CarneVacunacion,
  DescargaService,
  DosisCarne,
  NotificationService,
  PacienteClinicoResumen,
  VacunacionApiService,
} from '@tusalud/core';
import { PageHeader } from '@tusalud/ui/page-header';
import { SelectorPacienteClinico } from '@tusalud/ui/selector-paciente-clinico';
import { CarneVista } from '../carne-vista/carne-vista';

function hoyIso(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

@Component({
  selector: 'app-vacunacion-clinica',
  imports: [
    FormsModule,
    Button,
    InputText,
    Message,
    PageHeader,
    SelectorPacienteClinico,
    CarneVista,
  ],
  template: `
    <app-page-header
      title="Vacunación"
      subtitle="Carné de vacunación, registro de dosis y próximas vacunas por paciente"
      icon="pi-shield"
    />
    <div class="disposicion">
      <app-selector-paciente-clinico
        [(seleccionado)]="paciente"
        (seleccionadoChange)="cargar($event)"
      />
      <div class="detalle">
        @if (cargando()) {
          <p class="text-muted" role="status">Cargando el carné…</p>
        } @else if (error()) {
          <p-message severity="error">{{ error() }}</p-message>
        } @else if (carne(); as c) {
          <div class="panel">
            <div class="barra">
              <p-button
                label="Descargar carné en PDF"
                icon="pi pi-download"
                [outlined]="true"
                [loading]="descargando()"
                (onClick)="descargar(c)"
              />
            </div>
            <app-carne-vista
              [carne]="c"
              [permiteRegistrar]="true"
              [permiteAnular]="esMedico()"
              (registrar)="preparar($event)"
              (anular)="preguntarAnular($event)"
            />
          </div>
          @if (formulario(); as f) {
            <form class="panel formulario" (submit)="$event.preventDefault(); guardar(c, f)">
              <h3>Registrar {{ f.vacunaNombre }}, {{ f.etiqueta }}</h3>
              <div class="campos">
                <label>
                  Fecha de aplicación
                  <input
                    pInputText
                    type="date"
                    name="fecha"
                    [max]="hoy"
                    [(ngModel)]="fecha"
                    required
                  />
                </label>
                <label>
                  Lote
                  <input pInputText name="lote" maxlength="40" [(ngModel)]="lote" required />
                </label>
                <label>
                  Establecimiento
                  <input
                    pInputText
                    name="establecimiento"
                    maxlength="150"
                    [(ngModel)]="establecimiento"
                    placeholder="TuSalud"
                  />
                </label>
                <label class="ancho">
                  Observaciones
                  <input
                    pInputText
                    name="observaciones"
                    maxlength="300"
                    [(ngModel)]="observaciones"
                  />
                </label>
              </div>
              <div class="barra">
                <p-button
                  type="submit"
                  label="Guardar dosis"
                  icon="pi pi-check"
                  [loading]="guardando()"
                />
                <p-button
                  label="Cancelar"
                  severity="secondary"
                  [text]="true"
                  (onClick)="formulario.set(null)"
                />
              </div>
            </form>
          }
          @if (anulando(); as a) {
            <form
              class="panel formulario"
              (submit)="$event.preventDefault(); confirmarAnulacion(c, a)"
            >
              <h3>Anular {{ a.vacunaNombre }}, {{ a.etiqueta }}</h3>
              <label>
                Motivo de la anulación
                <input
                  pInputText
                  name="motivo"
                  minlength="5"
                  maxlength="300"
                  [(ngModel)]="motivo"
                  required
                />
              </label>
              <div class="barra">
                <p-button
                  type="submit"
                  label="Anular dosis"
                  severity="danger"
                  [loading]="guardando()"
                />
                <p-button
                  label="Cancelar"
                  severity="secondary"
                  [text]="true"
                  (onClick)="anulando.set(null)"
                />
              </div>
            </form>
          }
        } @else {
          <div class="panel vacio">
            <p class="text-muted">Seleccione un paciente para ver su carné de vacunación.</p>
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

    .barra {
      display: flex;
      gap: 0.5rem;
      margin-bottom: 0.75rem;
      flex-wrap: wrap;
    }

    .formulario {
      display: grid;
      gap: 0.75rem;
    }

    .formulario h3 {
      margin: 0;
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
      font-size: 0.85rem;
    }

    .ancho {
      grid-column: 1 / -1;
    }

    @media (max-width: 60rem) {
      .disposicion {
        grid-template-columns: 1fr;
      }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class VacunacionClinica {
  private readonly api = inject(VacunacionApiService);
  private readonly auth = inject(AuthService);
  private readonly avisos = inject(NotificationService);
  private readonly descargas = inject(DescargaService);

  protected readonly paciente = signal<PacienteClinicoResumen | null>(null);
  protected readonly carne = signal<CarneVacunacion | null>(null);
  protected readonly cargando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly descargando = signal(false);
  protected readonly guardando = signal(false);
  protected readonly formulario = signal<DosisCarne | null>(null);
  protected readonly anulando = signal<DosisCarne | null>(null);
  protected readonly esMedico = computed(() => this.auth.roles().includes('ROLE_MEDICO'));
  protected readonly hoy = hoyIso();

  protected fecha = hoyIso();
  protected lote = '';
  protected establecimiento = '';
  protected observaciones = '';
  protected motivo = '';

  protected cargar(p: PacienteClinicoResumen | null): void {
    this.formulario.set(null);
    this.anulando.set(null);
    this.carne.set(null);
    this.error.set(null);
    if (!p) {
      return;
    }
    this.cargando.set(true);
    this.api.carne(p.id).subscribe({
      next: (c) => {
        this.carne.set(c);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar el carné de este paciente');
        this.cargando.set(false);
      },
    });
  }

  protected preparar(d: DosisCarne): void {
    this.anulando.set(null);
    this.fecha = hoyIso();
    this.lote = '';
    this.establecimiento = '';
    this.observaciones = '';
    this.formulario.set(d);
  }

  protected preguntarAnular(d: DosisCarne): void {
    this.formulario.set(null);
    this.motivo = '';
    this.anulando.set(d);
  }

  protected guardar(c: CarneVacunacion, d: DosisCarne): void {
    if (!this.fecha || !this.lote.trim()) {
      return;
    }
    this.guardando.set(true);
    this.api
      .registrar({
        pacienteId: c.pacienteId,
        vacunaId: d.vacunaId,
        numeroDosis: d.numeroDosis,
        fechaAplicacion: this.fecha,
        lote: this.lote.trim(),
        establecimiento: this.establecimiento.trim() || null,
        observaciones: this.observaciones.trim() || null,
      })
      .subscribe({
        next: () => {
          this.guardando.set(false);
          this.formulario.set(null);
          this.avisos.success('Dosis registrada en el carné');
          this.cargar(this.paciente());
        },
        error: () => this.guardando.set(false),
      });
  }

  protected confirmarAnulacion(c: CarneVacunacion, d: DosisCarne): void {
    if (!d.registroId || this.motivo.trim().length < 5) {
      return;
    }
    this.guardando.set(true);
    this.api.anular(d.registroId, this.motivo.trim()).subscribe({
      next: () => {
        this.guardando.set(false);
        this.anulando.set(null);
        this.avisos.success('Dosis anulada');
        this.cargar(this.paciente());
      },
      error: () => this.guardando.set(false),
    });
    void c;
  }

  protected descargar(c: CarneVacunacion): void {
    this.descargando.set(true);
    this.api.pdf(c.pacienteId).subscribe({
      next: (archivo) => {
        this.descargando.set(false);
        this.descargas.guardar(archivo);
      },
      error: () => this.descargando.set(false),
    });
  }
}
