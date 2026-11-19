import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';

import { PoliticaPublica as PoliticaPublicaModelo, ProteccionDatosApiService } from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';

@Component({
  selector: 'app-politica-publica',
  imports: [DatePipe, RouterLink, Button, EmptyState],
  template: `
    <main class="pagina">
      <a class="volver" routerLink="/">Volver al inicio</a>
      @if (politica(); as p) {
        <article class="panel">
          <header>
            <h1>{{ p.titulo }}</h1>
            <p class="meta">
              Versión {{ p.version }} · vigente desde el {{ p.vigenteDesde | date: 'd MMMM y' }}
            </p>
          </header>
          <div class="texto">{{ p.contenido }}</div>
          <footer>
            <small class="text-muted">Huella de esta versión: {{ p.hash }}</small>
          </footer>
        </article>
      } @else if (error()) {
        <div class="panel">
          <app-empty-state
            icon="pi-shield"
            title="Aún no hay una política publicada"
            message="Cuando la clínica publique su política de privacidad, aparecerá aquí."
          >
            <p-button label="Reintentar" icon="pi pi-refresh" (onClick)="cargar()" />
          </app-empty-state>
        </div>
      }
    </main>
  `,
  styles: `
    .pagina {
      max-width: 52rem;
      margin: 0 auto;
      padding: 1.5rem 1rem 3rem;
    }

    .volver {
      display: inline-block;
      margin-bottom: 1rem;
    }

    h1 {
      margin: 0 0 0.25rem;
      font-size: 1.6rem;
    }

    .meta {
      margin: 0 0 1.25rem;
      color: var(--p-text-muted-color);
    }

    .texto {
      white-space: pre-wrap;
      line-height: 1.6;
    }

    footer {
      margin-top: 1.5rem;
      overflow-wrap: anywhere;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PoliticaPublica implements OnInit {
  private readonly api = inject(ProteccionDatosApiService);

  protected readonly politica = signal<PoliticaPublicaModelo | null>(null);
  protected readonly error = signal(false);

  ngOnInit(): void {
    this.cargar();
  }

  protected cargar(): void {
    this.error.set(false);
    this.api.politicaVigente().subscribe({
      next: (p) => this.politica.set(p),
      error: () => this.error.set(true),
    });
  }
}
