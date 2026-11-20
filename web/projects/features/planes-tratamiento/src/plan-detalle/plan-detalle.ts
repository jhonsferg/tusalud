import { DatePipe, DecimalPipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  input,
  linkedSignal,
  output,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { Tag } from 'primeng/tag';

import { ParametroSeguimiento, PlanDetalle } from '@tusalud/core';

export const ETIQUETA_PARAMETRO: Record<ParametroSeguimiento, string> = {
  PRESION_SISTOLICA: 'Presión sistólica',
  PRESION_DIASTOLICA: 'Presión diastólica',
  GLUCOSA_AYUNO: 'Glucosa en ayunas',
  HEMOGLOBINA_GLICOSILADA: 'Hemoglobina glicosilada',
  PESO_KG: 'Peso',
  FLUJO_ESPIRATORIO: 'Flujo espiratorio',
};

@Component({
  selector: 'app-plan-detalle',
  imports: [DatePipe, DecimalPipe, FormsModule, Button, InputText, Select, Tag],
  template: `
    @if (plan(); as p) {
      <article class="panel">
        <header class="cabecera">
          <h3>{{ etiquetaPrograma(p.programa) }} · {{ p.paciente }}</h3>
          <p-tag
            [value]="'Riesgo ' + p.riesgo.toLowerCase()"
            [severity]="p.riesgo === 'ALTO' ? 'danger' : p.riesgo === 'MEDIO' ? 'warn' : 'success'"
          />
          <p-tag [value]="'Adherencia ' + p.adherencia + ' %'" severity="info" />
        </header>
        <p><strong>Objetivos:</strong> {{ p.objetivos }}</p>
        @if (p.medicacion) {
          <p><strong>Medicación:</strong> {{ p.medicacion }}</p>
        }
        <h4>Metas</h4>
        <ul>
          @for (m of p.metas; track m.parametro) {
            <li>
              {{ etiqueta[m.parametro] }}:
              {{ m.valorMin !== null ? 'mínimo ' + m.valorMin : '' }}
              {{ m.valorMax !== null ? 'máximo ' + m.valorMax : '' }} {{ m.unidad }}
            </li>
          }
        </ul>
        <h4>Controles programados</h4>
        <ul class="controles">
          @for (c of p.controles; track c.id) {
            <li>
              Control {{ c.numero }} · {{ c.fechaProgramada | date: 'd MMM y' }}
              <p-tag
                [value]="c.estado === 'CUMPLIDO' ? 'Cumplido' : c.vencido ? 'Vencido' : 'Pendiente'"
                [severity]="
                  c.estado === 'CUMPLIDO' ? 'success' : c.vencido ? 'danger' : 'secondary'
                "
              />
              @if (puedeCumplir() && c.estado === 'PENDIENTE') {
                <p-button
                  label="Marcar cumplido"
                  size="small"
                  [text]="true"
                  [ariaLabel]="'Marcar cumplido el control ' + c.numero"
                  (onClick)="cumplir.emit(c.id)"
                />
              }
            </li>
          }
        </ul>
        <h4>Mediciones</h4>
        @if (p.mediciones.length === 0) {
          <p class="text-muted">Todavía no hay mediciones.</p>
        }
        <ul>
          @for (m of p.mediciones; track m.id) {
            <li [class.fuera]="m.fueraDeRango">
              {{ m.medidoEn | date: 'd MMM y, HH:mm' }} · {{ etiqueta[m.parametro] }}:
              <strong>{{ m.valor | number: '1.0-2' }}</strong>
              @if (m.fueraDeRango) {
                <p-tag value="Fuera de la meta" severity="warn" />
              }
            </li>
          }
        </ul>
        @if (p.estado === 'ACTIVO') {
          <div class="medir">
            <p-select
              [options]="parametros()"
              optionLabel="label"
              optionValue="value"
              ariaLabel="Parámetro a medir"
              [ngModel]="parametro()"
              (ngModelChange)="parametro.set($event)"
            />
            <input
              pInputText
              type="number"
              step="0.1"
              aria-label="Valor medido"
              placeholder="Valor"
              [ngModel]="valor()"
              (ngModelChange)="valor.set($event)"
            />
            <p-button
              label="Registrar medición"
              icon="pi pi-plus"
              [disabled]="valor() === null || valor() === undefined || parametro() === null"
              (onClick)="registrar()"
            />
          </div>
        }
      </article>
    }
  `,
  styles: `
    .cabecera,
    .medir {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 0.5rem;
    }

    .controles li {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 0.5rem;
    }

    .fuera {
      color: var(--p-orange-700, #c2410c);
    }

    ul {
      padding-left: 1.2rem;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PlanDetalleVista {
  readonly plan = input<PlanDetalle | null>(null);
  readonly puedeCumplir = input(false);
  readonly cumplir = output<string>();
  readonly medir = output<{ parametro: ParametroSeguimiento; valor: number }>();

  protected readonly etiqueta = ETIQUETA_PARAMETRO;
  protected readonly parametro = linkedSignal<ParametroSeguimiento | null>(
    () => this.plan()?.metas[0]?.parametro ?? null,
  );
  protected readonly valor = signal<number | null>(null);

  protected parametros(): { label: string; value: ParametroSeguimiento }[] {
    const metas = this.plan()?.metas ?? [];
    return metas.map((m) => ({ label: ETIQUETA_PARAMETRO[m.parametro], value: m.parametro }));
  }

  protected etiquetaPrograma(programa: string): string {
    const etiquetas: Record<string, string> = {
      HIPERTENSION: 'Hipertensión',
      DIABETES: 'Diabetes',
      ASMA: 'Asma',
      OTRO: 'Otro programa',
    };
    return etiquetas[programa] ?? programa;
  }

  protected registrar(): void {
    const parametro = this.parametro();
    const valor = this.valor();
    if (parametro !== null && valor !== null && valor !== undefined) {
      this.medir.emit({ parametro, valor: Number(valor) });
      this.valor.set(null);
    }
  }
}
