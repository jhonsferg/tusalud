import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { Message } from 'primeng/message';
import { Tag } from 'primeng/tag';

import { DescargaService, RecetaResumen, RecetasApiService } from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { ETIQUETA_ESTADO_RECETA, SEVERIDAD_ESTADO_RECETA, textoVigencia } from '../recetas.utils';

@Component({
  selector: 'app-mis-recetas',
  imports: [DatePipe, RouterLink, Button, Message, Tag, EmptyState, PageHeader],
  template: `
    <app-page-header
      title="Mis recetas"
      subtitle="Las recetas electrónicas que tus médicos emitieron para ti"
      icon="pi-file-edit"
    />
    @if (error()) {
      <p-message severity="error">{{ error() }}</p-message>
    } @else if (cargando()) {
      <p class="text-muted" role="status">Cargando tus recetas…</p>
    } @else if (recetas().length === 0) {
      <div class="panel">
        <app-empty-state
          icon="pi-file-edit"
          title="Aún no tienes recetas"
          message="Cuando tu médico emita una receta electrónica aparecerá aquí, lista para descargar y llevar a la farmacia."
        />
      </div>
    } @else {
      <div class="grid">
        @for (r of recetas(); track r.id) {
          <article class="panel receta">
            <header>
              <div>
                <strong>{{ r.numero }}</strong>
                <small class="block text-muted">{{ r.medicoNombre }}</small>
              </div>
              <p-tag [value]="etiqueta[r.estado]" [severity]="severidad[r.estado]" />
            </header>
            <p class="text-muted">
              Emitida el {{ r.emitidaEn | date: 'd MMM y' }} · {{ r.medicamentos }}
              {{ r.medicamentos === 1 ? 'medicamento' : 'medicamentos' }}
            </p>
            <p class="vigencia">{{ r.estado === 'ANULADA' ? 'Anulada' : vigencia(r) }}</p>
            <footer>
              <p-button
                label="Ver detalle"
                size="small"
                [outlined]="true"
                [routerLink]="['/mis-recetas', r.id]"
                [ariaLabel]="'Ver el detalle de la receta ' + r.numero"
              />
              <p-button
                label="Descargar PDF"
                icon="pi pi-download"
                size="small"
                severity="secondary"
                [outlined]="true"
                [loading]="descargando() === r.id"
                [ariaLabel]="'Descargar el PDF de la receta ' + r.numero"
                (onClick)="descargar(r)"
              />
            </footer>
          </article>
        }
      </div>
    }
  `,
  styles: `
    .grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(20rem, 1fr));
      gap: 1rem;
    }

    .receta {
      display: flex;
      flex-direction: column;
      gap: 0.4rem;
    }

    .receta header,
    .receta footer {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 0.5rem;
    }

    .receta p {
      margin: 0;
    }

    .vigencia {
      font-weight: 600;
    }

    .block {
      display: block;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MisRecetas implements OnInit {
  private readonly api = inject(RecetasApiService);
  private readonly descargas = inject(DescargaService);

  protected readonly etiqueta = ETIQUETA_ESTADO_RECETA;
  protected readonly severidad = SEVERIDAD_ESTADO_RECETA;
  protected readonly recetas = signal<RecetaResumen[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly descargando = signal<string | null>(null);

  ngOnInit(): void {
    this.api.mias().subscribe({
      next: (lista) => {
        this.recetas.set(lista);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar tus recetas');
        this.cargando.set(false);
      },
    });
  }

  protected vigencia(r: RecetaResumen): string {
    return textoVigencia(r.vigente, r.vigenteHasta);
  }

  protected descargar(r: RecetaResumen): void {
    this.descargando.set(r.id);
    this.api.pdf(r.id).subscribe({
      next: (archivo) => {
        this.descargando.set(null);
        this.descargas.guardar(archivo);
      },
      error: () => this.descargando.set(null),
    });
  }
}
