import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  signal,
  untracked,
} from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { Tag } from 'primeng/tag';

import {
  AuthService,
  DescargaService,
  DocumentoMedicoDetalle,
  DocumentosMedicosApiService,
  DocumentosMedicosStore,
  ETIQUETA_ESTADO_DOCUMENTO_MEDICO,
  ETIQUETA_ESTADO_REFERENCIA,
  EstadoReferencia,
  NotificationService,
} from '@tusalud/core';
import { MotivoService } from '@tusalud/ui/dialogo-motivo';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { SecurityNotice } from '@tusalud/ui/tablero';
import {
  CAMPOS_DOCUMENTO,
  SEVERIDAD_ESTADO_DOCUMENTO,
  SEVERIDAD_ESTADO_REFERENCIA,
} from '../documentos-medicos.utils';

interface Fila {
  readonly etiqueta: string;
  readonly valor: string;
}

@Component({
  selector: 'app-documento-detalle',
  imports: [DatePipe, RouterLink, Button, Tag, EmptyState, PageHeader, SecurityNotice],
  template: `
    <app-page-header
      [title]="detalle()?.resumen?.tipoEtiqueta ?? 'Documento médico'"
      [subtitle]="detalle()?.resumen?.numero ?? 'Borrador sin numerar'"
      icon="pi-file-check"
      [backLink]="esMedico() ? '/documentos' : '/mis-documentos'"
      backLabel="Volver a documentos"
    />

    @if (store.detalle.error(); as problema) {
      <div class="panel">
        <app-empty-state
          tone="error"
          icon="pi-exclamation-triangle"
          title="No pudimos abrir el documento"
          [message]="problema.message"
        />
      </div>
    } @else if (detalle(); as d) {
      <app-security-notice variante="auditado" />

      <section class="panel cabecera">
        <div class="datos">
          <div>
            <span class="rotulo">Paciente</span>
            <strong class="sensible">{{ d.resumen.pacienteNombre }}</strong>
          </div>
          <div>
            <span class="rotulo">Médico</span>
            <strong>{{ d.resumen.medicoNombre }}</strong>
            <small class="text-muted">CMP {{ d.resumen.medicoColegiatura }}</small>
          </div>
          <div>
            <span class="rotulo">Emisión</span>
            <strong>{{
              d.resumen.emitidoEn ? (d.resumen.emitidoEn | date: 'd MMM y, HH:mm') : 'Sin emitir'
            }}</strong>
          </div>
          <div>
            <span class="rotulo">Estado</span>
            <p-tag
              [value]="etiquetaEstado[d.resumen.estado]"
              [severity]="severidadEstado[d.resumen.estado]"
            />
          </div>
        </div>
        @if (d.codigoVerificacion) {
          <p class="verificacion">
            Código de verificación <code>{{ d.codigoVerificacion }}</code>
            <a [href]="urlVerificacion(d.codigoVerificacion)" target="_blank" rel="noopener"
              >Verificar en el portal</a
            >
          </p>
        }
        @if (d.resumen.estado === 'ANULADO') {
          <p class="anulado" role="status">
            Anulado el {{ d.resumen.anuladoEn | date: 'd MMM y, HH:mm' }}. Motivo:
            {{ d.resumen.motivoAnulacion }}
          </p>
        }
      </section>

      <section class="panel contenido" aria-label="Contenido del documento">
        <dl>
          @for (fila of filas(); track fila.etiqueta) {
            <div>
              <dt>{{ fila.etiqueta }}</dt>
              <dd class="sensible">{{ fila.valor }}</dd>
            </div>
          }
        </dl>
        @if (diagnosticos().length > 0) {
          <h3>Diagnósticos CIE-10</h3>
          <ul class="diagnosticos">
            @for (dx of diagnosticos(); track dx) {
              <li class="sensible">{{ dx }}</li>
            }
          </ul>
        }
      </section>

      @if (d.resumen.referencia; as r) {
        <section class="panel referencia" aria-label="Estado de la referencia">
          <header>
            <h3>Referencia a {{ r.destinoNombre }}</h3>
            <p-tag
              [value]="etiquetaReferencia[r.estado]"
              [severity]="severidadReferencia[r.estado]"
            />
          </header>
          @if (r.motivoRechazo) {
            <p class="anulado">Rechazada: {{ r.motivoRechazo }}</p>
          }
          @if (esEmisor() && d.resumen.estado === 'EMITIDO') {
            <div class="acciones">
              @if (r.estado === 'ENVIADA') {
                <p-button
                  label="Marcar como recibida"
                  size="small"
                  [outlined]="true"
                  (onClick)="cambiarReferencia(r.estado, 'RECIBIDA')"
                />
              }
              @if (r.estado === 'ENVIADA' || r.estado === 'RECIBIDA') {
                <p-button
                  label="Marcar como rechazada"
                  severity="danger"
                  size="small"
                  [outlined]="true"
                  (onClick)="rechazarReferencia()"
                />
                <p-button
                  label="Registrar contrarreferencia"
                  icon="pi pi-reply"
                  size="small"
                  [routerLink]="['/documentos/nuevo']"
                  [queryParams]="{
                    tipo: 'CONTRARREFERENCIA',
                    pacienteId: d.resumen.pacienteId,
                    referenciaId: r.id,
                  }"
                />
              }
            </div>
          }
        </section>
      }

      <div class="acciones principal">
        @if (d.resumen.estado !== 'BORRADOR') {
          <p-button
            label="Ver PDF"
            icon="pi pi-eye"
            [outlined]="true"
            [loading]="cargandoPdf()"
            (onClick)="pdf('abrir')"
          />
          <p-button
            label="Descargar PDF"
            icon="pi pi-download"
            [outlined]="true"
            severity="secondary"
            [loading]="cargandoPdf()"
            (onClick)="pdf('guardar')"
          />
        } @else if (esEmisor()) {
          <p-button
            label="Vista previa"
            icon="pi pi-eye"
            [outlined]="true"
            severity="secondary"
            [loading]="cargandoPdf()"
            (onClick)="pdf('abrir')"
          />
        }
        @if (esEmisor()) {
          @if (d.resumen.estado === 'BORRADOR') {
            <p-button
              label="Editar borrador"
              icon="pi pi-pencil"
              [outlined]="true"
              [routerLink]="['/documentos', d.resumen.id, 'editar']"
            />
            <p-button
              label="Emitir documento"
              icon="pi pi-check"
              [loading]="trabajando()"
              (onClick)="emitir()"
            />
          }
          @if (d.resumen.estado === 'EMITIDO') {
            <p-button
              label="Anular"
              icon="pi pi-ban"
              severity="danger"
              [outlined]="true"
              (onClick)="anular()"
            />
          }
          @if (d.resumen.estado === 'ANULADO' && !d.reemplazado && d.resumen.tipo !== 'EPICRISIS') {
            <p-button
              label="Emitir un reemplazo"
              icon="pi pi-refresh"
              [routerLink]="['/documentos/nuevo']"
              [queryParams]="{
                tipo: d.resumen.tipo,
                pacienteId: d.resumen.pacienteId,
                origenId: d.resumen.id,
              }"
            />
          }
        }
      </div>
    }
  `,
  styles: `
    .cabecera,
    .contenido,
    .referencia {
      margin-bottom: 1rem;
    }

    .datos {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(11rem, 1fr));
      gap: 1rem;
    }

    .datos > div {
      display: flex;
      flex-direction: column;
      gap: 0.15rem;
    }

    .rotulo {
      color: var(--p-text-muted-color);
      font-size: 0.78rem;
    }

    .verificacion {
      margin: 1rem 0 0;
      font-size: 0.9rem;
    }

    .verificacion code {
      margin: 0 0.5rem;
      font-weight: 700;
      letter-spacing: 0.04em;
    }

    .anulado {
      margin: 0.75rem 0 0;
      color: var(--p-red-700);
      font-weight: 600;
    }

    dl {
      display: grid;
      gap: 0.75rem;
      margin: 0;
    }

    dt {
      color: var(--p-text-muted-color);
      font-size: 0.8rem;
      font-weight: 700;
    }

    dd {
      margin: 0.1rem 0 0;
      white-space: pre-line;
    }

    h3 {
      margin: 1rem 0 0.4rem;
      font-size: 1rem;
    }

    .diagnosticos {
      margin: 0;
      padding-left: 1.1rem;
    }

    .referencia header {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .referencia h3 {
      margin: 0;
    }

    .acciones {
      display: flex;
      flex-wrap: wrap;
      gap: 0.6rem;
      margin-top: 0.75rem;
    }

    .acciones.principal {
      margin: 0;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DocumentoDetalle {
  readonly id = input.required<string>();

  protected readonly store = inject(DocumentosMedicosStore);
  private readonly api = inject(DocumentosMedicosApiService);
  private readonly auth = inject(AuthService);
  private readonly descargas = inject(DescargaService);
  private readonly motivos = inject(MotivoService);
  private readonly notificaciones = inject(NotificationService);
  private readonly router = inject(Router);

  protected readonly detalle = this.store.detalle.value;
  protected readonly etiquetaEstado = ETIQUETA_ESTADO_DOCUMENTO_MEDICO;
  protected readonly severidadEstado = SEVERIDAD_ESTADO_DOCUMENTO;
  protected readonly etiquetaReferencia = ETIQUETA_ESTADO_REFERENCIA;
  protected readonly severidadReferencia = SEVERIDAD_ESTADO_REFERENCIA;
  protected readonly esMedico = computed(() => this.auth.isMedico());
  protected readonly trabajando = signal(false);
  protected readonly cargandoPdf = signal(false);

  protected readonly esEmisor = computed(() => {
    const d = this.detalle();
    return (
      !!d && this.auth.isMedico() && d.resumen.medicoNombre === this.auth.user()?.nombreCompleto
    );
  });

  protected readonly filas = computed<Fila[]>(() => {
    const d = this.detalle();
    if (!d) {
      return [];
    }
    return CAMPOS_DOCUMENTO[d.resumen.tipo]
      .map((campo) => ({ etiqueta: campo.etiqueta, valor: this.texto(d, campo.clave) }))
      .filter((fila) => fila.valor);
  });

  protected readonly diagnosticos = computed<string[]>(() => {
    const lista = this.detalle()?.contenido['diagnosticos'];
    if (!Array.isArray(lista)) {
      return [];
    }
    return lista.map((item) => {
      const dx = item as { codigo: string; descripcion: string; principal?: boolean };
      return `${dx.codigo} · ${dx.descripcion}${dx.principal ? ' (principal)' : ''}`;
    });
  });

  constructor() {
    effect(() => {
      const id = this.id();
      untracked(() => this.store.cargarDetalle(id));
    });
  }

  protected urlVerificacion(codigo: string): string {
    return `/verificar/${codigo}`;
  }

  protected emitir(): void {
    this.trabajando.set(true);
    this.store.emitir(this.id()).subscribe({
      next: (d) => {
        this.trabajando.set(false);
        this.notificaciones.success(`Documento ${d.resumen.numero} emitido.`);
      },
      error: () => this.trabajando.set(false),
    });
  }

  protected anular(): void {
    const d = this.detalle();
    if (!d) {
      return;
    }
    this.motivos
      .solicitar({
        titulo: 'Anular documento',
        descripcion: `Se anulará el documento ${d.resumen.numero}. Seguirá visible como anulado y podrás emitir un reemplazo.`,
        etiqueta: 'Motivo de la anulación',
        confirmar: 'Anular documento',
        severidad: 'danger',
        icono: 'pi pi-ban',
      })
      .subscribe((motivo) =>
        this.store
          .anular(this.id(), motivo)
          .subscribe(() => this.notificaciones.success('El documento fue anulado.')),
      );
  }

  protected cambiarReferencia(_actual: EstadoReferencia, nuevo: EstadoReferencia): void {
    this.store
      .cambiarEstadoReferencia(this.id(), { estado: nuevo })
      .subscribe(() => this.notificaciones.success('La referencia fue actualizada.'));
  }

  protected rechazarReferencia(): void {
    this.motivos
      .solicitar({
        titulo: 'Rechazar referencia',
        etiqueta: 'Motivo del rechazo',
        confirmar: 'Rechazar',
        severidad: 'danger',
      })
      .subscribe((motivo) =>
        this.store
          .cambiarEstadoReferencia(this.id(), { estado: 'RECHAZADA', motivoRechazo: motivo })
          .subscribe(() => this.notificaciones.success('La referencia fue rechazada.')),
      );
  }

  protected pdf(accion: 'abrir' | 'guardar'): void {
    this.cargandoPdf.set(true);
    this.api.pdf(this.id()).subscribe({
      next: (archivo) => {
        this.cargandoPdf.set(false);
        if (accion === 'abrir') {
          this.descargas.abrir(archivo);
        } else {
          this.descargas.guardar(archivo);
        }
      },
      error: () => this.cargandoPdf.set(false),
    });
  }

  private texto(d: DocumentoMedicoDetalle, clave: string): string {
    const valor = d.contenido[clave];
    if (valor === undefined || valor === null || valor === '') {
      return '';
    }
    if (clave === 'destinoId') {
      return String(d.contenido['destinoNombre'] ?? '');
    }
    if (clave === 'vigenciaDias') {
      return `${valor} días desde la emisión`;
    }
    return String(valor);
  }
}
