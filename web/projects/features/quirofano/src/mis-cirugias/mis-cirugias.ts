import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Message } from 'primeng/message';
import { Tag } from 'primeng/tag';

import { MiCirugia, QuirofanoApiService } from '@tusalud/core';
import { PageHeader } from '@tusalud/ui/page-header';
import {
  ETIQUETA_CONSENTIMIENTO,
  ETIQUETA_ESTADO_CIRUGIA,
  SEVERIDAD_ESTADO_CIRUGIA,
  fechaHora,
} from '../quirofano.utils';

@Component({
  selector: 'app-mis-cirugias',
  imports: [RouterLink, Message, Tag, PageHeader],
  template: `
    <app-page-header
      title="Mis cirugías"
      subtitle="Procedimientos programados y los consentimientos que debe firmar"
      icon="pi-calendar-plus"
    />
    @if (error()) {
      <p-message severity="error">{{ error() }}</p-message>
    }
    @if (cirugias().length === 0 && !cargando()) {
      <div class="panel">
        <p class="text-muted">No tiene cirugías registradas.</p>
      </div>
    }
    @for (c of cirugias(); track c.id) {
      <section class="panel" [attr.aria-labelledby]="'cir-' + c.id">
        <h2 [id]="'cir-' + c.id">{{ c.procedimiento }}</h2>
        <p-tag [value]="estados[c.estado]" [severity]="severidades[c.estado]" />
        <dl>
          <div>
            <dt>Código</dt>
            <dd>{{ c.codigo }}</dd>
          </div>
          <div>
            <dt>Fecha y hora</dt>
            <dd>{{ c.inicio ? fechaHora(c.inicio) : 'Por definir' }}</dd>
          </div>
          <div>
            <dt>Lugar</dt>
            <dd>{{ c.quirofano ?? 'Por definir' }}</dd>
          </div>
          <div>
            <dt>Cirujano</dt>
            <dd>{{ c.cirujano }}</dd>
          </div>
          <div>
            <dt>Consentimiento quirúrgico</dt>
            <dd>
              {{
                c.consentimientoQuirurgico
                  ? consentimientos[c.consentimientoQuirurgico.estado]
                  : 'Sin solicitar'
              }}
            </dd>
          </div>
          <div>
            <dt>Consentimiento anestésico</dt>
            <dd>
              {{
                c.consentimientoAnestesico
                  ? consentimientos[c.consentimientoAnestesico.estado]
                  : 'No aplica o sin solicitar'
              }}
            </dd>
          </div>
        </dl>
        @if (pendiente(c)) {
          <p>
            Tiene un consentimiento por firmar.
            <a routerLink="/mis-consentimientos">Ir a Mis consentimientos</a>
          </p>
        }
      </section>
    }
  `,
  styles: `
    .panel {
      margin-bottom: 1rem;
    }

    h2 {
      margin: 0 0 0.5rem;
      font-size: 1.05rem;
    }

    dl {
      display: grid;
      gap: 0.75rem 1.25rem;
      grid-template-columns: repeat(auto-fit, minmax(13rem, 1fr));
      margin: 0.75rem 0 0;
    }

    dt {
      font-size: 0.78rem;
      font-weight: 600;
      color: var(--p-text-muted-color, #64748b);
    }

    dd {
      margin: 0;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MisCirugias implements OnInit {
  private readonly api = inject(QuirofanoApiService);

  protected readonly cirugias = signal<MiCirugia[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly estados = ETIQUETA_ESTADO_CIRUGIA;
  protected readonly severidades = SEVERIDAD_ESTADO_CIRUGIA;
  protected readonly consentimientos = ETIQUETA_CONSENTIMIENTO;
  protected readonly fechaHora = fechaHora;

  ngOnInit(): void {
    this.api.mias().subscribe({
      next: (lista) => {
        this.cirugias.set(lista);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No pudimos cargar sus cirugías');
        this.cargando.set(false);
      },
    });
  }

  protected pendiente(c: MiCirugia): boolean {
    return (
      c.consentimientoQuirurgico?.estado === 'PENDIENTE_FIRMA' ||
      c.consentimientoAnestesico?.estado === 'PENDIENTE_FIRMA'
    );
  }
}
