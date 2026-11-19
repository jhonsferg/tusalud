import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Tab, TabList, TabPanel, TabPanels, Tabs } from 'primeng/tabs';
import { Tag } from 'primeng/tag';
import { Textarea } from 'primeng/textarea';

import {
  AccesoDatos,
  CampoRectificable,
  ConsentimientoTratamientoEstado,
  DESCRIPCION_TIPO_ARCO,
  DescargaService,
  ETIQUETA_CAMPO_RECTIFICABLE,
  ETIQUETA_ESTADO_ARCO,
  ETIQUETA_TIPO_ARCO,
  EstadoAceptacion,
  NotificationService,
  PoliticaPublica,
  PreviaCancelacion,
  ProteccionDatosApiService,
  SEVERIDAD_ESTADO_ARCO,
  SolicitudArco,
  SolicitudArcoDetalle,
  TipoArco,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';

@Component({
  selector: 'app-mi-privacidad',
  imports: [
    DatePipe,
    FormsModule,
    Button,
    Dialog,
    InputText,
    Message,
    Select,
    Tab,
    TabList,
    TabPanel,
    TabPanels,
    Tabs,
    TableModule,
    Tag,
    Textarea,
    EmptyState,
    PageHeader,
  ],
  template: `
    <app-page-header
      title="Mi privacidad"
      subtitle="Cómo tratamos sus datos personales y cómo ejercer sus derechos"
      icon="pi-shield"
    />

    @if (aceptacion(); as a) {
      @if (a.versionVigente !== null && !a.aceptada) {
        <section class="panel pendiente" role="status">
          <div>
            <strong>Hay una política de privacidad nueva, versión {{ a.versionVigente }}.</strong>
            <p>Lea el texto y registre que lo conoce y lo acepta.</p>
          </div>
          <div class="acciones">
            <p-button
              label="Leer la política"
              icon="pi pi-book"
              [outlined]="true"
              (onClick)="leerPolitica()"
            />
            <p-button
              label="Aceptar"
              icon="pi pi-check"
              [loading]="trabajando()"
              (onClick)="aceptarPolitica()"
            />
          </div>
        </section>
      }
    }

    <section class="panel">
      <p-tabs value="consentimientos" [scrollable]="true">
        <p-tablist>
          <p-tab value="consentimientos"
            ><i class="pi pi-check-square"></i> Mis autorizaciones</p-tab
          >
          <p-tab value="solicitudes"><i class="pi pi-inbox"></i> Mis solicitudes</p-tab>
          <p-tab value="accesos"><i class="pi pi-eye"></i> Quién accedió a mis datos</p-tab>
        </p-tablist>
        <p-tabpanels>
          <p-tabpanel value="consentimientos">
            <p class="ayuda">
              Algunos tratamientos son necesarios para atenderlo y no requieren su autorización. Los
              demás los decide usted y puede cambiarlos cuando quiera.
              @if (aceptacion()?.aceptada) {
                Aceptó la política versión {{ aceptacion()!.versionAceptada }} el
                {{ aceptacion()!.aceptadaEn | date: 'd MMM y, HH:mm' }}.
              }
              <p-button
                label="Leer la política de privacidad"
                [link]="true"
                (onClick)="leerPolitica()"
              />
            </p>
            <ul class="finalidades">
              @for (c of consentimientos(); track c.finalidadId) {
                <li>
                  <div>
                    <strong>{{ c.nombre }}</strong>
                    <p>{{ c.descripcion }}</p>
                    <small class="text-muted">Base: {{ c.baseLegal }}</small>
                  </div>
                  <div class="decision">
                    @if (c.obligatoria) {
                      <p-tag value="Necesario para su atención" severity="secondary" />
                    } @else if (c.estado === 'OTORGADO') {
                      <p-tag value="Autorizado" severity="success" />
                      <p-button
                        label="Revocar"
                        size="small"
                        severity="danger"
                        [outlined]="true"
                        [ariaLabel]="'Revocar la autorización de ' + c.nombre"
                        (onClick)="revocar(c)"
                      />
                    } @else {
                      <p-tag
                        [value]="c.estado === 'REVOCADO' ? 'Revocado' : 'Sin autorizar'"
                        severity="warn"
                      />
                      <p-button
                        label="Autorizar"
                        size="small"
                        [ariaLabel]="'Autorizar ' + c.nombre"
                        (onClick)="otorgar(c)"
                      />
                    }
                  </div>
                </li>
              }
            </ul>
          </p-tabpanel>

          <p-tabpanel value="solicitudes">
            <div class="barra">
              <p class="ayuda">
                Puede pedir una copia de sus datos, corregirlos, cancelarlos o oponerse a un
                tratamiento. Respondemos dentro del plazo legal.
              </p>
              <p-button label="Nueva solicitud" icon="pi pi-plus" (onClick)="abrirNueva()" />
            </div>
            <p-table [value]="solicitudes()" responsiveLayout="scroll">
              <ng-template #header>
                <tr>
                  <th>Solicitud</th>
                  <th>Derecho</th>
                  <th>Estado</th>
                  <th>Respuesta antes del</th>
                  <th></th>
                </tr>
              </ng-template>
              <ng-template #body let-s>
                <tr>
                  <td class="font-semibold">{{ s.codigo }}</td>
                  <td>{{ tipos[s.tipo] }}</td>
                  <td><p-tag [value]="estados[s.estado]" [severity]="severidades[s.estado]" /></td>
                  <td>{{ s.venceEn | date: 'd MMM y' }}</td>
                  <td class="derecha">
                    <p-button
                      label="Ver"
                      size="small"
                      [outlined]="true"
                      [ariaLabel]="'Ver la solicitud ' + s.codigo"
                      (onClick)="ver(s)"
                    />
                  </td>
                </tr>
              </ng-template>
              <ng-template #emptymessage>
                <tr>
                  <td colspan="5">
                    <app-empty-state icon="pi-inbox" title="Aún no hizo solicitudes" />
                  </td>
                </tr>
              </ng-template>
            </p-table>
          </p-tabpanel>

          <p-tabpanel value="accesos">
            <p class="ayuda">
              Registro de las consultas hechas a su historia clínica y a sus documentos. Si ve un
              acceso que no reconoce, comuníquese con la clínica.
            </p>
            <p-table
              [value]="accesos()"
              [paginator]="accesos().length > 10"
              [rows]="10"
              responsiveLayout="scroll"
            >
              <ng-template #header>
                <tr>
                  <th>Fecha</th>
                  <th>Acción</th>
                  <th>Realizada por</th>
                </tr>
              </ng-template>
              <ng-template #body let-a>
                <tr>
                  <td>{{ a.fecha | date: 'd MMM y, HH:mm' }}</td>
                  <td>{{ a.accion }}</td>
                  <td>{{ a.realizadoPor }}</td>
                </tr>
              </ng-template>
              <ng-template #emptymessage>
                <tr>
                  <td colspan="3">
                    <app-empty-state icon="pi-eye" title="Sin accesos registrados" />
                  </td>
                </tr>
              </ng-template>
            </p-table>
          </p-tabpanel>
        </p-tabpanels>
      </p-tabs>
    </section>

    <p-dialog
      header="Política de privacidad"
      [(visible)]="verPolitica"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '42rem', maxWidth: '96vw' }"
    >
      @if (politica(); as p) {
        <h4>{{ p.titulo }} · versión {{ p.version }}</h4>
        <div class="texto-politica">{{ p.contenido }}</div>
      } @else {
        <p>La clínica aún no publicó su política de privacidad.</p>
      }
    </p-dialog>

    <p-dialog
      header="Nueva solicitud"
      [(visible)]="nueva"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '38rem', maxWidth: '96vw' }"
    >
      <form class="form" (submit)="$event.preventDefault(); enviar()" novalidate>
        @if (error(); as mensaje) {
          <p-message severity="error" styleClass="w-full">{{ mensaje }}</p-message>
        }
        <label for="sol-tipo">Derecho que desea ejercer</label>
        <p-select
          inputId="sol-tipo"
          [options]="opcionesTipo"
          optionLabel="label"
          optionValue="value"
          [ngModel]="tipo()"
          (ngModelChange)="cambiarTipo($event)"
          [ngModelOptions]="{ standalone: true }"
          appendTo="body"
          styleClass="w-full"
        />
        <small class="text-muted">{{ descripciones[tipo()] }}</small>

        @if (tipo() === 'RECTIFICACION') {
          <label for="sol-campo">Dato a corregir</label>
          <p-select
            inputId="sol-campo"
            [options]="opcionesCampo"
            optionLabel="label"
            optionValue="value"
            [ngModel]="campo()"
            (ngModelChange)="campo.set($event)"
            [ngModelOptions]="{ standalone: true }"
            [showClear]="true"
            placeholder="Otro dato, por ejemplo clínico"
            appendTo="body"
            styleClass="w-full"
          />
          @if (campo()) {
            <label for="sol-valor">Valor correcto</label>
            <input
              pInputText
              id="sol-valor"
              class="w-full"
              maxlength="200"
              autocomplete="off"
              [ngModel]="valor()"
              (ngModelChange)="valor.set($event)"
              [ngModelOptions]="{ standalone: true }"
            />
          } @else {
            <small class="text-muted"
              >Los datos clínicos no se modifican directamente. Su médico los corrige con un
              addendum que deja rastro del cambio.</small
            >
          }
        }

        @if (tipo() === 'OPOSICION') {
          <label for="sol-finalidad">Tratamiento al que se opone</label>
          <p-select
            inputId="sol-finalidad"
            [options]="opcionesFinalidad()"
            optionLabel="nombre"
            optionValue="finalidadId"
            [ngModel]="finalidadId()"
            (ngModelChange)="finalidadId.set($event)"
            [ngModelOptions]="{ standalone: true }"
            appendTo="body"
            styleClass="w-full"
          />
        }

        @if (tipo() === 'CANCELACION' && previa(); as pr) {
          <div class="previa" role="region" aria-label="Qué se cancela y qué se conserva">
            <strong>Qué se puede cancelar y qué se conserva</strong>
            @for (b of pr.categorias; track b.categoria) {
              <div class="bloqueo" [class.bloqueado]="b.estado === 'BLOQUEADO'">
                <p-tag
                  [value]="b.estado === 'BLOQUEADO' ? 'Se conserva' : 'Se cancela'"
                  [severity]="b.estado === 'BLOQUEADO' ? 'warn' : 'success'"
                />
                <div>
                  <strong>{{ b.categoria }}</strong>
                  <p>{{ b.motivo }}</p>
                  <small class="text-muted">{{ b.baseLegal }}</small>
                </div>
              </div>
            }
          </div>
        }

        <label for="sol-desc">Cuéntenos qué necesita</label>
        <textarea
          pTextarea
          id="sol-desc"
          rows="4"
          class="w-full"
          maxlength="1000"
          [ngModel]="descripcion()"
          (ngModelChange)="descripcion.set($event)"
          [ngModelOptions]="{ standalone: true }"
        ></textarea>
      </form>
      <ng-template #footer>
        <p-button
          label="Cancelar"
          severity="secondary"
          [outlined]="true"
          (onClick)="nueva.set(false)"
        />
        <p-button
          label="Enviar solicitud"
          icon="pi pi-send"
          [loading]="trabajando()"
          (onClick)="enviar()"
        />
      </ng-template>
    </p-dialog>

    <p-dialog
      [header]="detalle() ? 'Solicitud ' + detalle()!.solicitud.codigo : ''"
      [visible]="detalle() !== null"
      (visibleChange)="$event ? null : detalle.set(null)"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '40rem', maxWidth: '96vw' }"
    >
      @if (detalle(); as d) {
        <p>
          <strong>{{ tipos[d.solicitud.tipo] }}</strong> ·
          <p-tag
            [value]="estados[d.solicitud.estado]"
            [severity]="severidades[d.solicitud.estado]"
          />
        </p>
        <p>{{ d.solicitud.descripcion }}</p>
        @if (d.solicitud.resolucion) {
          <p-message severity="info" styleClass="w-full"
            >Respuesta: {{ d.solicitud.resolucion }}</p-message
          >
        }
        @if (d.resultado.length > 0) {
          <h4>Qué se hizo con sus datos</h4>
          @for (b of d.resultado; track b.categoria) {
            <div class="bloqueo" [class.bloqueado]="b.estado === 'BLOQUEADO'">
              <p-tag
                [value]="b.estado === 'BLOQUEADO' ? 'Se conservó' : 'Se canceló'"
                [severity]="b.estado === 'BLOQUEADO' ? 'warn' : 'success'"
              />
              <div>
                <strong>{{ b.categoria }}</strong>
                <p>{{ b.motivo }}</p>
              </div>
            </div>
          }
        }
        @if (d.solicitud.tieneExportacion) {
          <p-button
            label="Descargar mis datos"
            icon="pi pi-download"
            (onClick)="descargar(d.solicitud)"
          />
        }
        <h4>Historial</h4>
        <ul class="historial">
          @for (e of d.eventos; track e.id) {
            <li>
              <span class="text-muted">{{ e.registradoEn | date: 'd MMM y, HH:mm' }}</span>
              <strong>{{ e.accion }}</strong>
              <span>{{ e.detalle }}</span>
            </li>
          }
        </ul>
      }
    </p-dialog>
  `,
  styles: `
    .pendiente {
      display: flex;
      flex-wrap: wrap;
      justify-content: space-between;
      align-items: center;
      gap: 1rem;
      margin-bottom: 1rem;
      border-left: 4px solid var(--p-orange-500);
    }

    .pendiente p {
      margin: 0.2rem 0 0;
    }

    .acciones {
      display: flex;
      gap: 0.5rem;
    }

    .ayuda {
      color: var(--p-text-muted-color);
      margin: 0.5rem 0 1rem;
    }

    .finalidades {
      display: grid;
      gap: 0.75rem;
      margin: 0;
      padding: 0;
      list-style: none;
    }

    .finalidades li {
      display: flex;
      flex-wrap: wrap;
      justify-content: space-between;
      gap: 1rem;
      padding: 0.9rem 1rem;
      border: 1px solid var(--p-content-border-color);
      border-radius: 8px;
    }

    .finalidades p {
      margin: 0.2rem 0;
    }

    .decision {
      display: flex;
      align-items: center;
      gap: 0.6rem;
    }

    .barra {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: 1rem;
    }

    .derecha {
      text-align: right;
    }

    .form {
      display: flex;
      flex-direction: column;
      gap: 0.4rem;
    }

    .form label {
      font-weight: 600;
      font-size: 0.9rem;
      margin-top: 0.4rem;
    }

    .texto-politica {
      white-space: pre-wrap;
      line-height: 1.55;
      max-height: 60vh;
      overflow: auto;
    }

    .previa {
      display: flex;
      flex-direction: column;
      gap: 0.6rem;
      margin: 0.6rem 0;
      padding: 0.8rem 1rem;
      border: 1px dashed var(--p-content-border-color);
      border-radius: 8px;
    }

    .bloqueo {
      display: flex;
      gap: 0.75rem;
      align-items: flex-start;
      margin-bottom: 0.5rem;
    }

    .bloqueo p {
      margin: 0.1rem 0;
    }

    .historial {
      display: grid;
      gap: 0.5rem;
      margin: 0;
      padding: 0;
      list-style: none;
    }

    .historial li {
      display: flex;
      flex-direction: column;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MiPrivacidad implements OnInit {
  private readonly api = inject(ProteccionDatosApiService);
  private readonly descargas = inject(DescargaService);
  private readonly notificaciones = inject(NotificationService);

  protected readonly tipos: Record<string, string> = ETIQUETA_TIPO_ARCO;
  protected readonly descripciones: Record<string, string> = DESCRIPCION_TIPO_ARCO;
  protected readonly estados: Record<string, string> = ETIQUETA_ESTADO_ARCO;
  protected readonly severidades: Record<
    string,
    'success' | 'info' | 'warn' | 'danger' | 'secondary'
  > = SEVERIDAD_ESTADO_ARCO;
  protected readonly opcionesTipo = Object.entries(ETIQUETA_TIPO_ARCO).map(([value, label]) => ({
    label,
    value,
  }));
  protected readonly opcionesCampo = Object.entries(ETIQUETA_CAMPO_RECTIFICABLE).map(
    ([value, label]) => ({ label, value }),
  );

  protected readonly aceptacion = signal<EstadoAceptacion | null>(null);
  protected readonly consentimientos = signal<ConsentimientoTratamientoEstado[]>([]);
  protected readonly solicitudes = signal<SolicitudArco[]>([]);
  protected readonly accesos = signal<AccesoDatos[]>([]);
  protected readonly politica = signal<PoliticaPublica | null>(null);
  protected readonly detalle = signal<SolicitudArcoDetalle | null>(null);
  protected readonly previa = signal<PreviaCancelacion | null>(null);
  protected readonly verPolitica = signal(false);
  protected readonly nueva = signal(false);
  protected readonly trabajando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly tipo = signal<TipoArco>('ACCESO');
  protected readonly campo = signal<CampoRectificable | null>(null);
  protected readonly valor = signal('');
  protected readonly finalidadId = signal<string | null>(null);
  protected readonly descripcion = signal('');
  protected readonly opcionesFinalidad = computed(() =>
    this.consentimientos().filter((c) => !c.obligatoria),
  );

  ngOnInit(): void {
    this.api.estadoAceptacion().subscribe((a) => this.aceptacion.set(a));
    this.cargarConsentimientos();
    this.cargarSolicitudes();
    this.api.accesos().subscribe((a) => this.accesos.set(a));
  }

  protected leerPolitica(): void {
    this.verPolitica.set(true);
    if (!this.politica()) {
      this.api
        .politicaVigente()
        .subscribe({ next: (p) => this.politica.set(p), error: () => undefined });
    }
  }

  protected aceptarPolitica(): void {
    this.trabajando.set(true);
    this.api.aceptarPolitica().subscribe({
      next: (a) => {
        this.trabajando.set(false);
        this.aceptacion.set(a);
        this.notificaciones.success('Registramos que aceptó la política de privacidad.');
      },
      error: () => this.trabajando.set(false),
    });
  }

  protected otorgar(c: ConsentimientoTratamientoEstado): void {
    this.api.otorgar(c.finalidadId).subscribe(() => {
      this.notificaciones.success(`Autorizó: ${c.nombre}.`);
      this.cargarConsentimientos();
    });
  }

  protected revocar(c: ConsentimientoTratamientoEstado): void {
    this.api.revocar(c.finalidadId, null).subscribe(() => {
      this.notificaciones.success(`Revocó la autorización: ${c.nombre}.`);
      this.cargarConsentimientos();
    });
  }

  protected abrirNueva(): void {
    this.error.set(null);
    this.tipo.set('ACCESO');
    this.campo.set(null);
    this.valor.set('');
    this.finalidadId.set(this.opcionesFinalidad()[0]?.finalidadId ?? null);
    this.descripcion.set('');
    this.previa.set(null);
    this.nueva.set(true);
  }

  protected cambiarTipo(tipo: TipoArco): void {
    this.tipo.set(tipo);
    if (tipo === 'CANCELACION' && !this.previa()) {
      this.api.previaCancelacion().subscribe((p) => this.previa.set(p));
    }
  }

  protected enviar(): void {
    if (this.descripcion().trim().length < 10) {
      this.error.set('Cuéntenos qué necesita, con al menos 10 caracteres.');
      return;
    }
    if (this.tipo() === 'RECTIFICACION' && this.campo() && !this.valor().trim()) {
      this.error.set('Indique el valor correcto del dato.');
      return;
    }
    this.error.set(null);
    this.trabajando.set(true);
    this.api
      .solicitar({
        tipo: this.tipo(),
        descripcion: this.descripcion().trim(),
        campo: this.tipo() === 'RECTIFICACION' ? this.campo() : null,
        valorPropuesto:
          this.tipo() === 'RECTIFICACION' && this.campo() ? this.valor().trim() : null,
        finalidadId: this.tipo() === 'OPOSICION' ? this.finalidadId() : null,
      })
      .subscribe({
        next: (s) => {
          this.trabajando.set(false);
          this.nueva.set(false);
          this.notificaciones.success(`Registramos su solicitud ${s.codigo}.`);
          this.cargarSolicitudes();
        },
        error: (e: { message: string }) => {
          this.trabajando.set(false);
          this.error.set(e.message);
        },
      });
  }

  protected ver(s: SolicitudArco): void {
    this.api.detalle(s.id).subscribe((d) => this.detalle.set(d));
  }

  protected descargar(s: SolicitudArco): void {
    this.api.exportacion(s.id).subscribe((archivo) => this.descargas.guardar(archivo));
  }

  private cargarConsentimientos(): void {
    this.api.consentimientos().subscribe((lista) => this.consentimientos.set(lista));
  }

  private cargarSolicitudes(): void {
    this.api.mias().subscribe((lista) => this.solicitudes.set(lista));
  }
}
