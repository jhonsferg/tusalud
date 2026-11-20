import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { Message } from 'primeng/message';

import { Crecimiento, CrecimientoApiService } from '@tusalud/core';
import { PageHeader } from '@tusalud/ui/page-header';
import { CurvaCrecimiento } from '../curva-crecimiento/curva-crecimiento';

@Component({
  selector: 'app-mi-crecimiento',
  imports: [Message, PageHeader, CurvaCrecimiento],
  template: `
    <app-page-header
      title="Mi crecimiento"
      subtitle="Peso, talla y perímetro cefálico comparados con la referencia para tu edad"
      icon="pi-chart-line"
    />
    @if (cargando()) {
      <p class="text-muted" role="status">Cargando tus curvas…</p>
    } @else if (error()) {
      <p-message severity="error">{{ error() }}</p-message>
    } @else if (datos(); as d) {
      @if (d.referenciaPorValidar) {
        <p-message severity="warn" icon="pi pi-info-circle" size="small">
          Las curvas usan una referencia resumida de la OMS que está por validar.
        </p-message>
      }
      @if (!d.aplica) {
        <div class="panel">
          <p class="text-muted">Las curvas de crecimiento aplican a menores de 19 años.</p>
        </div>
      } @else {
        <div class="curvas">
          @for (curva of d.curvas; track curva.indicador) {
            <div class="panel">
              <app-curva-crecimiento [curva]="curva" />
            </div>
          }
        </div>
      }
    }
  `,
  styles: `
    .curvas {
      display: grid;
      gap: 1rem;
      grid-template-columns: repeat(auto-fit, minmax(22rem, 1fr));
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MiCrecimiento implements OnInit {
  private readonly api = inject(CrecimientoApiService);

  protected readonly datos = signal<Crecimiento | null>(null);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.api.mio().subscribe({
      next: (d) => {
        this.datos.set(d);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar tus curvas de crecimiento');
        this.cargando.set(false);
      },
    });
  }
}
