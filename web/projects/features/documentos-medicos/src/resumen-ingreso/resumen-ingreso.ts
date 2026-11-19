import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  effect,
  inject,
  input,
  signal,
  untracked,
} from '@angular/core';
import { Tag } from 'primeng/tag';

import {
  DocumentosMedicosApiService,
  ETIQUETA_ESTADO_CONSENTIMIENTO,
  ResumenIngresoDocumentos,
} from '@tusalud/core';
import { SEVERIDAD_ESTADO_CONSENTIMIENTO, SeveridadEtiqueta } from '../documentos-medicos.utils';

@Component({
  selector: 'app-resumen-ingreso',
  imports: [DatePipe, Tag],
  template: `
    @if (resumen(); as r) {
      <section class="bloque" aria-label="Documentos del ingreso">
        <h3>Documentos del ingreso</h3>
        @if (r.documentos.length === 0 && r.consentimientos.length === 0) {
          <p class="text-muted">Este ingreso aún no tiene epicrisis ni consentimientos.</p>
        }
        <ul>
          @for (d of r.documentos; track $index) {
            <li>
              <span>{{ d.tipoEtiqueta }} {{ d.numero }}</span>
              <small class="text-muted">{{ d.fecha | date: 'd MMM y' }} · {{ d.estado }}</small>
            </li>
          }
          @for (c of r.consentimientos; track c.id) {
            <li>
              <span>Consentimiento: {{ c.procedimiento }}</span>
              <p-tag [value]="etiqueta[c.estado]" [severity]="severidad[c.estado]" />
            </li>
          }
        </ul>
      </section>
    }
  `,
  styles: `
    .bloque h3 {
      margin: 0 0 0.5rem;
      font-size: 1rem;
    }

    ul {
      display: grid;
      gap: 0.4rem;
      margin: 0;
      padding: 0;
      list-style: none;
    }

    li {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 0.5rem;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ResumenIngreso {
  readonly ingresoId = input.required<string>();

  private readonly api = inject(DocumentosMedicosApiService);
  protected readonly resumen = signal<ResumenIngresoDocumentos | null>(null);
  protected readonly etiqueta: Record<string, string> = ETIQUETA_ESTADO_CONSENTIMIENTO;
  protected readonly severidad: Record<string, SeveridadEtiqueta> = SEVERIDAD_ESTADO_CONSENTIMIENTO;

  constructor() {
    effect(() => {
      const id = this.ingresoId();
      untracked(() =>
        this.api.resumenIngreso(id).subscribe({
          next: (r) => this.resumen.set(r),
          error: () => this.resumen.set(null),
        }),
      );
    });
  }
}
