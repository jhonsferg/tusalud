import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, input, signal } from '@angular/core';
import { Button } from 'primeng/button';
import { Tag } from 'primeng/tag';

import {
  AuthService,
  ConsentimientoResumen,
  DescargaService,
  DocumentosMedicosApiService,
  DocumentosMedicosStore,
  ETIQUETA_ESTADO_CONSENTIMIENTO,
  NotificationService,
} from '@tusalud/core';
import { MotivoService } from '@tusalud/ui/dialogo-motivo';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { ConsentimientoFirma } from '../consentimiento-firma/consentimiento-firma';
import { SEVERIDAD_ESTADO_CONSENTIMIENTO, SeveridadEtiqueta } from '../documentos-medicos.utils';

@Component({
  selector: 'app-mis-consentimientos',
  imports: [DatePipe, Button, Tag, EmptyState, PageHeader, ConsentimientoFirma],
  template: `
    <app-page-header
      title="Mis consentimientos"
      subtitle="Autorizaciones informadas para procedimientos y tratamientos"
      icon="pi-pencil"
    />

    @if (store.consentimientos.value(); as lista) {
      @if (lista.length === 0) {
        <div class="panel">
          <app-empty-state
            icon="pi-pencil"
            title="Sin consentimientos"
            message="Cuando tu médico te solicite un consentimiento informado lo verás aquí para leerlo y firmarlo."
          />
        </div>
      } @else {
        <div class="grid">
          @for (c of lista; track c.id) {
            <article class="panel consentimiento">
              <header>
                <div>
                  <strong>{{ c.procedimiento }}</strong>
                  <small class="block text-muted"
                    >{{ c.plantillaNombre }} · {{ c.medicoNombre }}</small
                  >
                </div>
                <p-tag [value]="etiqueta[c.estado]" [severity]="severidad[c.estado]" />
              </header>
              @if (c.estado === 'FIRMADO') {
                <p class="text-muted">
                  Firmado el {{ c.firmadoEn | date: 'd MMM y, HH:mm' }}
                  @if (c.venceEn) {
                    · vigente hasta el {{ c.venceEn | date: 'd MMM y' }}
                  }
                </p>
              }
              <footer>
                @if (c.estado === 'PENDIENTE_FIRMA') {
                  <p-button
                    label="Leer y firmar"
                    icon="pi pi-pencil"
                    size="small"
                    [ariaLabel]="'Leer y firmar el consentimiento de ' + c.procedimiento"
                    (onClick)="firmando.set(c.id)"
                  />
                }
                @if (c.estado === 'FIRMADO') {
                  <p-button
                    label="Revocar"
                    severity="danger"
                    size="small"
                    [outlined]="true"
                    [ariaLabel]="'Revocar el consentimiento de ' + c.procedimiento"
                    (onClick)="revocar(c)"
                  />
                }
                @if (c.estado !== 'PENDIENTE_FIRMA') {
                  <p-button
                    label="Descargar PDF"
                    icon="pi pi-download"
                    size="small"
                    severity="secondary"
                    [outlined]="true"
                    [loading]="descargando() === c.id"
                    [ariaLabel]="'Descargar el PDF del consentimiento de ' + c.procedimiento"
                    (onClick)="descargar(c)"
                  />
                }
              </footer>
            </article>
          }
        </div>
      }
    }

    <app-consentimiento-firma
      [id]="firmando()"
      via="APP_PACIENTE"
      [nombreInicial]="auth.user()?.nombreCompleto ?? ''"
      (firmado)="alFirmar()"
      (cerrar)="firmando.set(null)"
    />
  `,
  styles: `
    .grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(22rem, 1fr));
      gap: 1rem;
    }

    .consentimiento {
      display: flex;
      flex-direction: column;
      gap: 0.4rem;
    }

    .consentimiento header,
    .consentimiento footer {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 0.5rem;
    }

    .consentimiento p {
      margin: 0;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MisConsentimientos implements OnInit {
  readonly firmar = input<string>();

  protected readonly store = inject(DocumentosMedicosStore);
  protected readonly auth = inject(AuthService);
  private readonly api = inject(DocumentosMedicosApiService);
  private readonly descargas = inject(DescargaService);
  private readonly motivos = inject(MotivoService);
  private readonly notificaciones = inject(NotificationService);

  protected readonly etiqueta: Record<string, string> = ETIQUETA_ESTADO_CONSENTIMIENTO;
  protected readonly severidad: Record<string, SeveridadEtiqueta> = SEVERIDAD_ESTADO_CONSENTIMIENTO;
  protected readonly firmando = signal<string | null>(null);
  protected readonly descargando = signal<string | null>(null);

  ngOnInit(): void {
    this.store.cargarConsentimientosMios();
    const id = this.firmar();
    if (id) {
      this.firmando.set(id);
    }
  }

  protected alFirmar(): void {
    this.firmando.set(null);
    this.store.cargarConsentimientosMios();
  }

  protected revocar(consentimiento: ConsentimientoResumen): void {
    this.motivos
      .solicitar({
        titulo: 'Revocar consentimiento',
        descripcion: `Se revocará tu consentimiento para ${consentimiento.procedimiento}. Tu médico será notificado.`,
        etiqueta: 'Motivo de la revocación',
        confirmar: 'Revocar',
        severidad: 'danger',
      })
      .subscribe((motivo) =>
        this.store.revocarConsentimiento(consentimiento.id, motivo).subscribe(() => {
          this.notificaciones.success('El consentimiento fue revocado.');
          this.store.cargarConsentimientosMios();
        }),
      );
  }

  protected descargar(consentimiento: ConsentimientoResumen): void {
    this.descargando.set(consentimiento.id);
    this.api.consentimientoPdf(consentimiento.id).subscribe({
      next: (archivo) => {
        this.descargando.set(null);
        this.descargas.guardar(archivo);
      },
      error: () => this.descargando.set(null),
    });
  }
}
