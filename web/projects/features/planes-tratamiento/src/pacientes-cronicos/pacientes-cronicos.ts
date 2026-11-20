import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { Tag } from 'primeng/tag';
import { Textarea } from 'primeng/textarea';

import {
  MetaPlan,
  NotificationService,
  PlanDetalle,
  PlanResumen,
  PlanesApiService,
  ProgramaCronico,
  PacienteClinicoResumen,
} from '@tusalud/core';
import { PageHeader } from '@tusalud/ui/page-header';
import { SelectorPacienteClinico } from '@tusalud/ui/selector-paciente-clinico';
import { PlanDetalleVista } from '../plan-detalle/plan-detalle';

const METAS_POR_PROGRAMA: Record<ProgramaCronico, MetaPlan[]> = {
  HIPERTENSION: [
    { parametro: 'PRESION_SISTOLICA', valorMin: null, valorMax: 130, unidad: 'mmHg' },
    { parametro: 'PRESION_DIASTOLICA', valorMin: null, valorMax: 80, unidad: 'mmHg' },
  ],
  DIABETES: [
    { parametro: 'GLUCOSA_AYUNO', valorMin: 70, valorMax: 130, unidad: 'mg/dl' },
    { parametro: 'HEMOGLOBINA_GLICOSILADA', valorMin: null, valorMax: 7, unidad: '%' },
  ],
  ASMA: [{ parametro: 'FLUJO_ESPIRATORIO', valorMin: 300, valorMax: null, unidad: 'l/min' }],
  OTRO: [{ parametro: 'PESO_KG', valorMin: null, valorMax: 90, unidad: 'kg' }],
};

@Component({
  selector: 'app-pacientes-cronicos',
  imports: [
    FormsModule,
    Button,
    InputText,
    Select,
    Tag,
    Textarea,
    PageHeader,
    SelectorPacienteClinico,
    PlanDetalleVista,
  ],
  template: `
    <app-page-header
      title="Pacientes crónicos"
      subtitle="Planes de tratamiento, controles programados y mediciones, ordenados por riesgo"
      icon="pi-heart"
    />
    <section class="panel">
      <h3>Mis pacientes en seguimiento</h3>
      @if (lista().length === 0) {
        <p class="text-muted">Todavía no tienes planes activos.</p>
      }
      @for (p of lista(); track p.id) {
        <p class="fila">
          <strong>{{ p.paciente }}</strong>
          <p-tag [value]="programas[p.programa]" severity="info" />
          <p-tag
            [value]="'Riesgo ' + p.riesgo.toLowerCase()"
            [severity]="p.riesgo === 'ALTO' ? 'danger' : p.riesgo === 'MEDIO' ? 'warn' : 'success'"
          />
          <span class="text-muted"
            >Adherencia {{ p.adherencia }} % · {{ p.controlesVencidos }} controles vencidos ·
            {{ p.medicionesFueraDeRango }} mediciones fuera de la meta</span
          >
          <p-button
            label="Ver plan"
            size="small"
            [outlined]="true"
            [ariaLabel]="'Ver el plan de ' + p.paciente"
            (onClick)="abrir(p)"
          />
        </p>
      }
    </section>

    <app-plan-detalle
      [plan]="detalle()"
      [puedeCumplir]="true"
      (cumplir)="cumplir($event)"
      (medir)="medir($event)"
    />

    <section class="panel nuevo">
      <h3>Crear un plan de tratamiento</h3>
      <app-selector-paciente-clinico [(seleccionado)]="paciente" />
      <label for="plan-programa">Programa</label>
      <p-select
        inputId="plan-programa"
        [options]="opcionesPrograma"
        optionLabel="label"
        optionValue="value"
        [ngModel]="programa()"
        (ngModelChange)="programa.set($event)"
      />
      <label for="plan-objetivos">Objetivos</label>
      <textarea
        pTextarea
        id="plan-objetivos"
        rows="2"
        [ngModel]="objetivos()"
        (ngModelChange)="objetivos.set($event)"
      ></textarea>
      <label for="plan-medicacion">Medicación</label>
      <input
        pInputText
        id="plan-medicacion"
        [ngModel]="medicacion()"
        (ngModelChange)="medicacion.set($event)"
      />
      <label for="plan-frecuencia">Días entre controles</label>
      <input
        pInputText
        id="plan-frecuencia"
        type="number"
        [ngModel]="frecuencia()"
        (ngModelChange)="frecuencia.set($event)"
      />
      <label for="plan-cantidad">Cantidad de controles</label>
      <input
        pInputText
        id="plan-cantidad"
        type="number"
        [ngModel]="cantidad()"
        (ngModelChange)="cantidad.set($event)"
      />
      <p class="text-muted">
        Las metas iniciales dependen del programa y se pueden ajustar desde la API.
      </p>
      <p-button
        label="Crear plan"
        icon="pi pi-plus"
        [disabled]="paciente() === null || objetivos().trim().length === 0"
        [loading]="creando()"
        (onClick)="crear()"
      />
    </section>
  `,
  styles: `
    section {
      margin-bottom: 1rem;
    }

    .fila {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 0.5rem;
    }

    .nuevo {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
      max-width: 44rem;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PacientesCronicos implements OnInit {
  private readonly api = inject(PlanesApiService);
  private readonly avisos = inject(NotificationService);

  protected readonly programas: Record<ProgramaCronico, string> = {
    HIPERTENSION: 'Hipertensión',
    DIABETES: 'Diabetes',
    ASMA: 'Asma',
    OTRO: 'Otro',
  };
  protected readonly opcionesPrograma = Object.entries(this.programas).map(([value, label]) => ({
    value,
    label,
  }));

  protected readonly lista = signal<PlanResumen[]>([]);
  protected readonly detalle = signal<PlanDetalle | null>(null);
  protected readonly paciente = signal<PacienteClinicoResumen | null>(null);
  protected readonly programa = signal<ProgramaCronico>('HIPERTENSION');
  protected readonly objetivos = signal('');
  protected readonly medicacion = signal('');
  protected readonly frecuencia = signal(30);
  protected readonly cantidad = signal(6);
  protected readonly creando = signal(false);

  ngOnInit(): void {
    this.cargar();
  }

  protected abrir(p: PlanResumen): void {
    this.api.obtener(p.id).subscribe((d) => this.detalle.set(d));
  }

  protected cumplir(controlId: string): void {
    const plan = this.detalle();
    if (plan) {
      this.api.cumplirControl(plan.id, controlId).subscribe((d) => {
        this.detalle.set(d);
        this.cargar();
      });
    }
  }

  protected medir(evento: {
    parametro: PlanDetalle['metas'][number]['parametro'];
    valor: number;
  }): void {
    const plan = this.detalle();
    if (plan) {
      this.api.medir(plan.id, evento).subscribe({
        next: (d) => {
          this.detalle.set(d);
          this.cargar();
        },
        error: (e) => this.avisos.error(e.error?.message ?? 'No se pudo registrar la medición'),
      });
    }
  }

  protected crear(): void {
    const paciente = this.paciente();
    if (!paciente) {
      return;
    }
    this.creando.set(true);
    this.api
      .crear({
        pacienteId: paciente.id,
        programa: this.programa(),
        objetivos: this.objetivos().trim(),
        medicacion: this.medicacion().trim() || null,
        frecuenciaControlDias: Number(this.frecuencia()),
        cantidadControles: Number(this.cantidad()),
        metas: METAS_POR_PROGRAMA[this.programa()],
      })
      .subscribe({
        next: (d) => {
          this.creando.set(false);
          this.avisos.success('Plan creado');
          this.objetivos.set('');
          this.medicacion.set('');
          this.detalle.set(d);
          this.cargar();
        },
        error: (e) => {
          this.creando.set(false);
          this.avisos.error(e.error?.message ?? 'No se pudo crear el plan');
        },
      });
  }

  private cargar(): void {
    this.api.tablero().subscribe((l) => this.lista.set(l));
  }
}
