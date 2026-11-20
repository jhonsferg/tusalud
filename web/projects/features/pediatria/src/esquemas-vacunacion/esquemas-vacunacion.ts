import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { Message } from 'primeng/message';
import { Tag } from 'primeng/tag';

import { EsquemaVacunacion, VacunacionApiService } from '@tusalud/core';
import { PageHeader } from '@tusalud/ui/page-header';
import { edadLegible, fechaLegible } from '../pediatria.utils';

@Component({
  selector: 'app-esquemas-vacunacion',
  imports: [Message, Tag, PageHeader],
  template: `
    <app-page-header
      title="Esquemas de vacunación"
      subtitle="Calendario de dosis por vigencia que usa el carné de cada paciente"
      icon="pi-shield"
    />
    @if (error()) {
      <p-message severity="error">{{ error() }}</p-message>
    }
    @for (e of esquemas(); track e.id) {
      <section class="panel esquema">
        <header>
          <div>
            <h2>{{ e.nombre }}</h2>
            <p class="text-muted">
              Vigente desde {{ fecha(e.vigenteDesde) }}
              @if (e.vigenteHasta) {
                hasta {{ fecha(e.vigenteHasta) }}
              }
            </p>
          </div>
          @if (e.porValidar) {
            <p-tag severity="warn" value="Por validar" />
          } @else {
            <p-tag severity="success" value="Validado" />
          }
        </header>
        @if (e.fuente) {
          <p class="text-muted">{{ e.fuente }}</p>
        }
        <div class="tabla">
          <table>
            <caption class="sr-only">
              Dosis del esquema
              {{
                e.nombre
              }}
            </caption>
            <thead>
              <tr>
                <th scope="col">Vacuna</th>
                <th scope="col">Dosis</th>
                <th scope="col">Edad mínima</th>
                <th scope="col">Edad recomendada</th>
                <th scope="col">Edad máxima</th>
                <th scope="col">Intervalo mínimo</th>
              </tr>
            </thead>
            <tbody>
              @for (d of e.dosis; track d.vacunaId + d.numeroDosis) {
                <tr>
                  <th scope="row">{{ d.vacunaNombre }}</th>
                  <td>{{ d.etiqueta }}</td>
                  <td>{{ dias(d.edadMinimaDias) }}</td>
                  <td>{{ dias(d.edadRecomendadaDias) }}</td>
                  <td>{{ d.edadMaximaDias === null ? 'Sin límite' : dias(d.edadMaximaDias) }}</td>
                  <td>
                    {{ d.intervaloMinimoDias === null ? '—' : d.intervaloMinimoDias + ' días' }}
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </section>
    }
  `,
  styles: `
    .esquema {
      display: grid;
      gap: 0.5rem;
      margin-bottom: 1rem;
    }

    header {
      display: flex;
      justify-content: space-between;
      gap: 1rem;
    }

    h2,
    p {
      margin: 0;
    }

    .tabla {
      overflow-x: auto;
    }

    table {
      width: 100%;
      border-collapse: collapse;
      font-size: 0.9rem;
    }

    th,
    td {
      text-align: left;
      padding: 0.35rem 0.5rem;
      border-bottom: 1px solid var(--p-surface-200, #e2e8f0);
    }

    .sr-only {
      position: absolute;
      width: 1px;
      height: 1px;
      overflow: hidden;
      clip: rect(0 0 0 0);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EsquemasVacunacion implements OnInit {
  private readonly api = inject(VacunacionApiService);

  protected readonly esquemas = signal<EsquemaVacunacion[]>([]);
  protected readonly error = signal<string | null>(null);
  protected readonly fecha = fechaLegible;

  ngOnInit(): void {
    this.api.esquemas().subscribe({
      next: (lista) => this.esquemas.set(lista),
      error: () => this.error.set('No se pudieron cargar los esquemas de vacunación'),
    });
  }

  protected dias(dias: number): string {
    return dias < 60 ? `${dias} días` : edadLegible(Math.round(dias / 30.4375));
  }
}
