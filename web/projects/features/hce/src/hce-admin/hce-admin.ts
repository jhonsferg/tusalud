import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Tag } from 'primeng/tag';

import {
  FusionPacientes,
  HceApiService,
  NotificationService,
  PacienteDuplicado,
  PoliticaConservacion,
} from '@tusalud/core';
import { PageHeader } from '@tusalud/ui/page-header';

@Component({
  selector: 'app-hce-admin',
  imports: [DatePipe, FormsModule, Button, InputText, Message, Tag, PageHeader],
  template: `
    <app-page-header
      title="Historia clínica electrónica"
      subtitle="Conservación de registros y control de historias duplicadas"
      icon="pi-folder-open"
    />
    <p-message severity="warn" [closable]="false">
      Los plazos de conservación son referenciales y deben confirmarse con el asesor legal. Ningún
      registro clínico se depura desde el sistema.
    </p-message>

    <section class="panel">
      <h3>Políticas de conservación</h3>
      <div class="tabla" role="region" aria-label="Políticas de conservación" tabindex="0">
        <table>
          <thead>
            <tr>
              <th>Tipo de registro</th>
              <th>Años mínimos</th>
              <th>Referencia</th>
              <th>Estado</th>
              <th>Depuración</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            @for (p of politicas(); track p.id) {
              <tr>
                <td>{{ p.descripcion }}</td>
                <td>
                  <input
                    pInputText
                    type="number"
                    min="1"
                    max="100"
                    [attr.aria-label]="'Años de conservación de ' + p.descripcion"
                    [ngModel]="anios()[p.tipoRegistro] ?? p.aniosMinimos"
                    (ngModelChange)="cambiarAnios(p.tipoRegistro, $event)"
                  />
                </td>
                <td>{{ p.referencia }}</td>
                <td>
                  <p-tag
                    [value]="p.porConfirmar ? 'Por confirmar' : 'Confirmado'"
                    [severity]="p.porConfirmar ? 'warn' : 'success'"
                  />
                </td>
                <td><p-tag value="Bloqueada" severity="secondary" /></td>
                <td>
                  <p-button
                    label="Marcar confirmado"
                    size="small"
                    [outlined]="true"
                    [ariaLabel]="'Guardar y marcar como confirmada la política de ' + p.descripcion"
                    (onClick)="guardar(p)"
                  />
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    </section>

    <section class="panel">
      <h3>Posibles historias duplicadas</h3>
      @if (duplicados().length === 0) {
        <p class="text-muted">No se detectaron coincidencias de nombre y fecha de nacimiento.</p>
      }
      @for (d of duplicados(); track d.pacienteAId + d.pacienteBId) {
        <article class="duplicado">
          <div>
            <strong>{{ d.nombreA }}</strong> · DNI {{ d.dniA }} · {{ d.historialA }}
            <br />
            <strong>{{ d.nombreB }}</strong> · DNI {{ d.dniB }} · {{ d.historialB }}
            <br />
            <small class="text-muted">{{ d.criterio }}</small>
          </div>
          <div class="acciones">
            <input
              pInputText
              placeholder="Motivo de la fusión"
              [attr.aria-label]="'Motivo de la fusión de ' + d.nombreA"
              [ngModel]="motivos()[d.pacienteAId + d.pacienteBId] ?? ''"
              (ngModelChange)="cambiarMotivo(d, $event)"
            />
            <p-button
              label="Conservar la primera historia"
              size="small"
              [disabled]="(motivos()[d.pacienteAId + d.pacienteBId] ?? '').trim().length < 5"
              [loading]="trabajando()"
              (onClick)="fusionar(d)"
            />
          </div>
        </article>
      }
    </section>

    <section class="panel">
      <h3>Fusiones realizadas</h3>
      @if (fusiones().length === 0) {
        <p class="text-muted">Todavía no se fusionó ninguna historia.</p>
      }
      @for (f of fusiones(); track f.id) {
        <p>
          {{ f.realizadoEn | date: 'd MMM y, HH:mm' }}: la historia {{ f.origenHistorial }} de
          {{ f.origenNombre }} se fusionó en la de {{ f.destinoNombre }} por {{ f.realizadoPor }}.
          Motivo: {{ f.motivo }}.
        </p>
      }
    </section>
  `,
  styles: `
    section {
      margin-top: 1rem;
    }

    .tabla {
      overflow-x: auto;
    }

    table {
      width: 100%;
      border-collapse: collapse;
    }

    th,
    td {
      text-align: left;
      padding: 0.5rem;
      border-bottom: 1px solid var(--p-content-border-color, #e5e7eb);
    }

    td input[type='number'] {
      width: 5rem;
    }

    .duplicado {
      display: flex;
      flex-wrap: wrap;
      justify-content: space-between;
      gap: 0.75rem;
      padding: 0.75rem 0;
      border-bottom: 1px solid var(--p-content-border-color, #e5e7eb);
    }

    .acciones {
      display: flex;
      flex-wrap: wrap;
      gap: 0.5rem;
      align-items: center;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HceAdmin implements OnInit {
  private readonly api = inject(HceApiService);
  private readonly avisos = inject(NotificationService);

  protected readonly politicas = signal<PoliticaConservacion[]>([]);
  protected readonly duplicados = signal<PacienteDuplicado[]>([]);
  protected readonly fusiones = signal<FusionPacientes[]>([]);
  protected readonly anios = signal<Record<string, number>>({});
  protected readonly motivos = signal<Record<string, string>>({});
  protected readonly trabajando = signal(false);

  ngOnInit(): void {
    this.cargar();
  }

  protected cambiarAnios(tipo: string, valor: number): void {
    this.anios.update((actual) => ({ ...actual, [tipo]: Number(valor) }));
  }

  protected cambiarMotivo(d: PacienteDuplicado, valor: string): void {
    this.motivos.update((actual) => ({ ...actual, [d.pacienteAId + d.pacienteBId]: valor }));
  }

  protected guardar(p: PoliticaConservacion): void {
    this.api
      .actualizarPolitica(p.tipoRegistro, {
        aniosMinimos: this.anios()[p.tipoRegistro] ?? p.aniosMinimos,
        referencia: p.referencia,
        porConfirmar: false,
      })
      .subscribe({
        next: () => {
          this.avisos.success('Política actualizada');
          this.cargar();
        },
        error: () => this.avisos.error('No se pudo actualizar la política'),
      });
  }

  protected fusionar(d: PacienteDuplicado): void {
    this.trabajando.set(true);
    this.api
      .fusionar({
        origenId: d.pacienteBId,
        destinoId: d.pacienteAId,
        motivo: (this.motivos()[d.pacienteAId + d.pacienteBId] ?? '').trim(),
      })
      .subscribe({
        next: () => {
          this.trabajando.set(false);
          this.avisos.success('Las historias se fusionaron');
          this.cargar();
        },
        error: (e) => {
          this.trabajando.set(false);
          this.avisos.error(e.error?.message ?? 'No se pudo fusionar');
        },
      });
  }

  private cargar(): void {
    this.api.politicas().subscribe((p) => this.politicas.set(p));
    this.api.duplicados().subscribe((d) => this.duplicados.set(d));
    this.api.fusiones().subscribe((f) => this.fusiones.set(f));
  }
}
