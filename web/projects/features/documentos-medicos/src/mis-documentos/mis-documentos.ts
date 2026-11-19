import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { Tag } from 'primeng/tag';

import {
  DescargaService,
  DocumentoMedicoResumen,
  DocumentosMedicosApiService,
  DocumentosMedicosStore,
  ETIQUETA_ESTADO_DOCUMENTO_MEDICO,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { SEVERIDAD_ESTADO_DOCUMENTO, SeveridadEtiqueta } from '../documentos-medicos.utils';

@Component({
  selector: 'app-mis-documentos',
  imports: [DatePipe, RouterLink, Button, Tag, EmptyState, PageHeader],
  template: `
    <app-page-header
      title="Mis documentos"
      subtitle="Certificados, descansos, informes y otros documentos que tu médico emitió para ti"
      icon="pi-file-check"
    />

    @if (store.documentos.value(); as lista) {
      @if (lista.length === 0) {
        <div class="panel">
          <app-empty-state
            icon="pi-file-check"
            title="Aún no tienes documentos"
            message="Cuando tu médico emita un certificado, un descanso o un informe aparecerá aquí."
          />
        </div>
      } @else {
        <div class="grid">
          @for (d of lista; track d.id) {
            <article class="panel documento">
              <header>
                <div>
                  <strong>{{ d.tipoEtiqueta }}</strong>
                  <small class="block text-muted">{{ d.numero }}</small>
                </div>
                <p-tag [value]="etiquetaEstado[d.estado]" [severity]="severidad[d.estado]" />
              </header>
              <p class="text-muted">
                Emitido el {{ d.emitidoEn | date: 'd MMM y' }} por {{ d.medicoNombre }}
              </p>
              @if (d.estado === 'ANULADO') {
                <p class="anulado">Anulado: {{ d.motivoAnulacion }}</p>
              }
              <footer>
                <p-button
                  label="Ver detalle"
                  size="small"
                  [outlined]="true"
                  [routerLink]="['/mis-documentos', d.id]"
                  [ariaLabel]="'Ver detalle de ' + d.tipoEtiqueta + ' ' + d.numero"
                />
                <p-button
                  label="Descargar PDF"
                  icon="pi pi-download"
                  size="small"
                  severity="secondary"
                  [outlined]="true"
                  [loading]="descargando() === d.id"
                  [ariaLabel]="'Descargar PDF de ' + d.tipoEtiqueta + ' ' + d.numero"
                  (onClick)="descargar(d)"
                />
              </footer>
            </article>
          }
        </div>
      }
    }
  `,
  styles: `
    .grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(20rem, 1fr));
      gap: 1rem;
    }

    .documento {
      display: flex;
      flex-direction: column;
      gap: 0.4rem;
    }

    .documento header,
    .documento footer {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 0.5rem;
    }

    .documento p {
      margin: 0;
    }

    .anulado {
      color: var(--p-red-700);
      font-weight: 600;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MisDocumentos implements OnInit {
  protected readonly store = inject(DocumentosMedicosStore);
  private readonly api = inject(DocumentosMedicosApiService);
  private readonly descargas = inject(DescargaService);

  protected readonly etiquetaEstado: Record<string, string> = ETIQUETA_ESTADO_DOCUMENTO_MEDICO;
  protected readonly severidad: Record<string, SeveridadEtiqueta> = SEVERIDAD_ESTADO_DOCUMENTO;
  protected readonly descargando = signal<string | null>(null);

  ngOnInit(): void {
    this.store.cargarMios();
  }

  protected descargar(documento: DocumentoMedicoResumen): void {
    this.descargando.set(documento.id);
    this.api.pdf(documento.id).subscribe({
      next: (archivo) => {
        this.descargando.set(null);
        this.descargas.guardar(archivo);
      },
      error: () => this.descargando.set(null),
    });
  }
}
