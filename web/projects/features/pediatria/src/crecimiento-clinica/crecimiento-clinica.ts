import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';

import {
  AuthService,
  Crecimiento,
  CrecimientoApiService,
  NotificationService,
  PacienteClinicoResumen,
} from '@tusalud/core';
import { PageHeader } from '@tusalud/ui/page-header';
import { SelectorPacienteClinico } from '@tusalud/ui/selector-paciente-clinico';
import { CurvaCrecimiento } from '../curva-crecimiento/curva-crecimiento';
import { ETIQUETA_NIVEL, fechaLegible } from '../pediatria.utils';

function hoyIso(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

@Component({
  selector: 'app-crecimiento-clinica',
  imports: [
    FormsModule,
    Button,
    InputNumber,
    InputText,
    Message,
    PageHeader,
    SelectorPacienteClinico,
    CurvaCrecimiento,
  ],
  template: `
    <app-page-header
      title="Crecimiento"
      subtitle="Mediciones de peso, talla y perímetro cefálico con percentiles y alertas para menores de 19 años"
      icon="pi-chart-line"
    />
    <div class="disposicion">
      <app-selector-paciente-clinico
        [(seleccionado)]="paciente"
        (seleccionadoChange)="cargar($event)"
      />
      <div class="detalle">
        @if (cargando()) {
          <p class="text-muted" role="status">Cargando las curvas…</p>
        } @else if (error()) {
          <p-message severity="error">{{ error() }}</p-message>
        } @else if (datos(); as d) {
          @if (d.referenciaPorValidar) {
            <p-message severity="warn" icon="pi pi-info-circle" size="small">
              Las tablas de referencia son un resumen de los patrones de la OMS y están por validar
              con las tablas oficiales.
            </p-message>
          }
          @for (a of d.alertas; track a.indicador) {
            <p-message [severity]="a.nivel === 'ALERTA' ? 'error' : 'warn'">
              <strong>{{ a.etiqueta }}:</strong> {{ a.mensaje }} (z {{ a.z }}, {{ nivel[a.nivel] }})
            </p-message>
          }
          @if (!d.aplica) {
            <div class="panel">
              <p class="text-muted">
                Las curvas aplican a pacientes menores de 19 años con fecha de nacimiento y sexo
                registrados.
              </p>
            </div>
          } @else {
            <form class="panel formulario" (submit)="$event.preventDefault(); guardar(d)">
              <h3>Registrar medición</h3>
              <div class="campos">
                <label>
                  Fecha
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
                  Peso en kg
                  <p-inputnumber
                    name="peso"
                    [(ngModel)]="peso"
                    [minFractionDigits]="0"
                    [maxFractionDigits]="2"
                    [min]="0.3"
                    [max]="200"
                  />
                </label>
                <label>
                  Talla en cm
                  <p-inputnumber
                    name="talla"
                    [(ngModel)]="talla"
                    [minFractionDigits]="0"
                    [maxFractionDigits]="1"
                    [min]="15"
                    [max]="230"
                  />
                </label>
                <label>
                  Perímetro cefálico en cm
                  <p-inputnumber
                    name="pc"
                    [(ngModel)]="pc"
                    [minFractionDigits]="0"
                    [maxFractionDigits]="1"
                    [min]="15"
                    [max]="70"
                  />
                </label>
              </div>
              <div class="barra">
                <p-button
                  type="submit"
                  label="Guardar medición"
                  icon="pi pi-check"
                  [loading]="guardando()"
                />
              </div>
            </form>
            <div class="curvas">
              @for (curva of d.curvas; track curva.indicador) {
                <div class="panel">
                  <app-curva-crecimiento [curva]="curva" />
                </div>
              }
            </div>
            @if (d.mediciones.length) {
              <div class="panel">
                <h3>Mediciones registradas</h3>
                <ul class="mediciones">
                  @for (m of d.mediciones; track m.id) {
                    <li>
                      <span>
                        <strong>{{ fecha2(m.fecha) }}</strong> ·
                        @if (m.pesoKg !== null) {
                          {{ m.pesoKg }} kg ·
                        }
                        @if (m.tallaCm !== null) {
                          {{ m.tallaCm }} cm ·
                        }
                        @if (m.perimetroCefalicoCm !== null) {
                          PC {{ m.perimetroCefalicoCm }} cm ·
                        }
                        @if (m.imc !== null) {
                          IMC {{ m.imc }}
                        }
                        <small class="text-muted">por {{ m.registradoPor }}</small>
                      </span>
                      @if (esMedico()) {
                        <p-button
                          label="Anular"
                          size="small"
                          severity="danger"
                          [text]="true"
                          [ariaLabel]="'Anular la medición del ' + fecha2(m.fecha)"
                          (onClick)="anular(m.id)"
                        />
                      }
                    </li>
                  }
                </ul>
                @if (anulandoId(); as id) {
                  <form
                    class="anulacion"
                    (submit)="$event.preventDefault(); confirmarAnulacion(id)"
                  >
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
                    <p-button
                      type="submit"
                      label="Confirmar anulación"
                      severity="danger"
                      [loading]="guardando()"
                    />
                    <p-button
                      label="Cancelar"
                      severity="secondary"
                      [text]="true"
                      (onClick)="anulandoId.set(null)"
                    />
                  </form>
                }
              </div>
            }
          }
        } @else {
          <div class="panel vacio">
            <p class="text-muted">Seleccione un paciente para ver sus curvas de crecimiento.</p>
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

    .detalle,
    .curvas {
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
      grid-template-columns: repeat(auto-fit, minmax(11rem, 1fr));
    }

    label {
      display: grid;
      gap: 0.25rem;
      font-weight: 600;
      font-size: 0.85rem;
    }

    .mediciones {
      list-style: none;
      margin: 0;
      padding: 0;
      display: grid;
      gap: 0.25rem;
    }

    .mediciones li {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 0.5rem;
      border-bottom: 1px solid var(--p-surface-200, #e2e8f0);
      padding: 0.25rem 0;
    }

    .anulacion {
      display: flex;
      gap: 0.5rem;
      align-items: end;
      margin-top: 0.75rem;
      flex-wrap: wrap;
    }

    @media (max-width: 60rem) {
      .disposicion {
        grid-template-columns: 1fr;
      }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CrecimientoClinica {
  private readonly api = inject(CrecimientoApiService);
  private readonly auth = inject(AuthService);
  private readonly avisos = inject(NotificationService);

  protected readonly paciente = signal<PacienteClinicoResumen | null>(null);
  protected readonly datos = signal<Crecimiento | null>(null);
  protected readonly cargando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly guardando = signal(false);
  protected readonly anulandoId = signal<string | null>(null);
  protected readonly esMedico = computed(() => this.auth.roles().includes('ROLE_MEDICO'));
  protected readonly nivel = ETIQUETA_NIVEL;
  protected readonly fecha2 = fechaLegible;
  protected readonly hoy = hoyIso();

  protected fecha = hoyIso();
  protected peso: number | null = null;
  protected talla: number | null = null;
  protected pc: number | null = null;
  protected motivo = '';

  protected cargar(p: PacienteClinicoResumen | null): void {
    this.datos.set(null);
    this.error.set(null);
    this.anulandoId.set(null);
    if (!p) {
      return;
    }
    this.cargando.set(true);
    this.api.paciente(p.id).subscribe({
      next: (d) => {
        this.datos.set(d);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar las curvas de este paciente');
        this.cargando.set(false);
      },
    });
  }

  protected guardar(d: Crecimiento): void {
    if (this.peso === null && this.talla === null && this.pc === null) {
      this.avisos.warning('Indique al menos el peso, la talla o el perímetro cefálico');
      return;
    }
    this.guardando.set(true);
    this.api
      .registrar({
        pacienteId: d.pacienteId,
        fecha: this.fecha,
        pesoKg: this.peso,
        tallaCm: this.talla,
        perimetroCefalicoCm: this.pc,
      })
      .subscribe({
        next: () => {
          this.guardando.set(false);
          this.peso = null;
          this.talla = null;
          this.pc = null;
          this.avisos.success('Medición registrada');
          this.cargar(this.paciente());
        },
        error: () => this.guardando.set(false),
      });
  }

  protected anular(id: string): void {
    this.motivo = '';
    this.anulandoId.set(id);
  }

  protected confirmarAnulacion(id: string): void {
    if (this.motivo.trim().length < 5) {
      return;
    }
    this.guardando.set(true);
    this.api.anular(id, this.motivo.trim()).subscribe({
      next: () => {
        this.guardando.set(false);
        this.anulandoId.set(null);
        this.avisos.success('Medición anulada');
        this.cargar(this.paciente());
      },
      error: () => this.guardando.set(false),
    });
  }
}
