import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { DiagnosticoResponse, TIPOS_DIAGNOSTICO } from '@tusalud/core';

@Component({
  selector: 'app-lista-diagnosticos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (lista().length > 0) {
      <ul class="dx" aria-label="Diagnósticos CIE-10">
        @for (d of lista(); track d.codigo) {
          <li [class.principal]="d.principal">
            <span class="cod">{{ d.codigo }}</span>
            <span class="desc">{{ d.descripcion }}</span>
            @if (d.principal) {
              <span class="etq destacada">Principal</span>
            }
            <span class="etq">{{ etiquetaTipo(d.tipo) }}</span>
            @if (d.observacion) {
              <small class="obs">{{ d.observacion }}</small>
            }
          </li>
        }
      </ul>
      @if (notaAparte(); as nota) {
        <p class="nota">{{ nota }}</p>
      }
    } @else if (descripcion()) {
      <p class="nota">{{ descripcion() }}</p>
    }
  `,
  styles: `
    .dx {
      display: flex;
      flex-direction: column;
      gap: 0.35rem;
      margin: 0 0 0.5rem;
      padding: 0;
      list-style: none;
    }

    li {
      display: flex;
      flex-wrap: wrap;
      align-items: baseline;
      gap: 0.2rem 0.6rem;
    }

    .cod {
      min-width: 3.4rem;
      color: var(--app-primary-text, var(--p-primary-color));
      font-family: ui-monospace, 'Cascadia Mono', Consolas, monospace;
      font-weight: 700;
    }

    .principal .desc {
      font-weight: 600;
    }

    .etq {
      padding: 0.05rem 0.5rem;
      border-radius: 999px;
      background: var(--p-content-hover-background);
      color: var(--p-text-muted-color);
      font-size: 0.72rem;
      font-weight: 600;
    }

    .etq.destacada {
      background: var(--app-primary-soft, var(--p-content-hover-background));
      color: var(--app-primary-text, var(--p-primary-color));
    }

    .obs,
    .nota {
      color: var(--p-text-muted-color);
    }

    .nota {
      margin: 0;
    }
  `,
})
export class ListaDiagnosticos {
  readonly diagnosticos = input<readonly DiagnosticoResponse[] | null | undefined>([]);
  readonly descripcion = input<string | null | undefined>(null);

  protected readonly lista = computed(() => this.diagnosticos() ?? []);

  protected readonly notaAparte = computed(() => {
    const texto = this.descripcion()?.trim();
    const principal = this.lista().find((d) => d.principal);
    if (!texto || (principal && principal.descripcion.trim() === texto)) {
      return null;
    }
    return texto;
  });

  protected etiquetaTipo(tipo: string): string {
    return TIPOS_DIAGNOSTICO.find((t) => t.value === tipo)?.label ?? tipo;
  }
}
