import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';

import {
  Cie10Response,
  NotificationService,
  PacienteClinicoResumen,
  QuirofanoApiService,
  TipoAnestesia,
  TipoCirugia,
} from '@tusalud/core';
import { PageHeader } from '@tusalud/ui/page-header';
import { SelectorCie10 } from '@tusalud/ui/selector-cie10';
import { SelectorPacienteClinico } from '@tusalud/ui/selector-paciente-clinico';
import { ETIQUETA_ANESTESIA, ETIQUETA_TIPO_CIRUGIA } from '../quirofano.utils';

@Component({
  selector: 'app-solicitud-cirugia',
  imports: [
    FormsModule,
    Button,
    InputText,
    Message,
    Select,
    PageHeader,
    SelectorCie10,
    SelectorPacienteClinico,
  ],
  template: `
    <app-page-header
      title="Solicitar cirugía"
      subtitle="Indique el paciente, el diagnóstico y el procedimiento. La programación la coordina el bloque quirúrgico."
      icon="pi-plus"
      backLink="/quirofano"
    />
    @if (error()) {
      <p-message severity="error">{{ error() }}</p-message>
    }
    <app-selector-paciente-clinico [(seleccionado)]="paciente" />
    @if (paciente(); as p) {
      <form class="panel formulario" (submit)="$event.preventDefault(); enviar(p)">
        <h2>{{ p.nombre }}</h2>
        <div class="campo">
          <app-selector-cie10
            inputId="cirugia-cie10"
            etiqueta="Diagnóstico CIE-10"
            (elegido)="diagnostico.set($event)"
          />
          @if (diagnostico(); as d) {
            <p class="elegido">
              <strong>{{ d.codigo }}</strong> {{ d.descripcion }}
            </p>
          }
        </div>
        <div class="campos">
          <label>
            Procedimiento
            <input
              pInputText
              name="procedimiento"
              maxlength="200"
              [(ngModel)]="procedimiento"
              required
            />
          </label>
          <label for="cirugia-tipo">
            Tipo
            <p-select
              inputId="cirugia-tipo"
              name="tipo"
              [options]="tipos"
              optionLabel="etiqueta"
              optionValue="valor"
              [(ngModel)]="tipo"
            />
          </label>
          <label for="cirugia-anestesia">
            Anestesia prevista
            <p-select
              inputId="cirugia-anestesia"
              name="anestesia"
              [options]="anestesias"
              optionLabel="etiqueta"
              optionValue="valor"
              [(ngModel)]="anestesia"
            />
          </label>
        </div>
        <label>
          Indicaciones preoperatorias
          <input
            pInputText
            name="indicaciones"
            maxlength="500"
            [(ngModel)]="indicaciones"
            placeholder="Ayuno, suspensión de medicamentos, exámenes previos"
          />
        </label>
        <div class="barra">
          <p-button
            type="submit"
            label="Solicitar cirugía"
            icon="pi pi-send"
            [loading]="enviando()"
            [disabled]="!puedeEnviar()"
          />
        </div>
      </form>
    }
  `,
  styles: `
    .panel {
      margin-top: 1rem;
    }

    .formulario {
      display: grid;
      gap: 0.75rem;
    }

    h2 {
      margin: 0;
      font-size: 1.1rem;
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

    .elegido {
      margin: 0.4rem 0 0;
    }

    .barra {
      display: flex;
      gap: 0.75rem;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SolicitudCirugia {
  private readonly api = inject(QuirofanoApiService);
  private readonly avisos = inject(NotificationService);
  private readonly router = inject(Router);

  protected readonly paciente = signal<PacienteClinicoResumen | null>(null);
  protected readonly diagnostico = signal<Cie10Response | null>(null);
  protected readonly enviando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly tipos: { valor: TipoCirugia; etiqueta: string }[] = (
    Object.keys(ETIQUETA_TIPO_CIRUGIA) as TipoCirugia[]
  ).map((valor) => ({ valor, etiqueta: ETIQUETA_TIPO_CIRUGIA[valor] }));
  protected readonly anestesias: { valor: TipoAnestesia; etiqueta: string }[] = (
    Object.keys(ETIQUETA_ANESTESIA) as TipoAnestesia[]
  ).map((valor) => ({ valor, etiqueta: ETIQUETA_ANESTESIA[valor] }));

  protected procedimiento = '';
  protected tipo: TipoCirugia = 'ELECTIVA';
  protected anestesia: TipoAnestesia = 'GENERAL';
  protected indicaciones = '';

  protected puedeEnviar(): boolean {
    return !!this.diagnostico() && this.procedimiento.trim().length >= 3;
  }

  protected enviar(paciente: PacienteClinicoResumen): void {
    const d = this.diagnostico();
    if (!d || !this.puedeEnviar()) {
      return;
    }
    this.enviando.set(true);
    this.error.set(null);
    this.api
      .solicitar({
        pacienteId: paciente.id,
        diagnosticoCodigo: d.codigo,
        procedimiento: this.procedimiento.trim(),
        tipo: this.tipo,
        tipoAnestesia: this.anestesia,
        indicacionesPreoperatorias: this.indicaciones.trim() || null,
      })
      .subscribe({
        next: (detalle) => {
          this.avisos.success('La solicitud quirúrgica quedó registrada');
          void this.router.navigate(['/quirofano', detalle.resumen.id]);
        },
        error: (e: { error?: { message?: string } }) => {
          this.enviando.set(false);
          this.error.set(e.error?.message ?? 'No pudimos registrar la solicitud');
        },
      });
  }
}
