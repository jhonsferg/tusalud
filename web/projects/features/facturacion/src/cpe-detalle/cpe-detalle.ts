import { CurrencyPipe, DatePipe } from '@angular/common';
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
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';

import {
  AuthService,
  DescargaService,
  DocumentoElectronicoDetalle,
  ETIQUETA_AFECTACION,
  ETIQUETA_OPERACION_SUNAT,
  ETIQUETA_TIPO_CPE,
  FacturacionApiService,
  MotivoNota,
  NotaItemRequest,
  NotificationService,
} from '@tusalud/core';
import { MotivoService } from '@tusalud/ui/dialogo-motivo';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { EstadoCpeTag } from '../estado-cpe-tag';

interface FilaNota {
  descripcion: string;
  cantidad: number;
  precioUnitario: number;
}

@Component({
  selector: 'app-cpe-detalle',
  imports: [
    CurrencyPipe,
    DatePipe,
    FormsModule,
    RouterLink,
    Button,
    Dialog,
    InputNumber,
    InputText,
    Message,
    Select,
    TableModule,
    Tag,
    EmptyState,
    EstadoCpeTag,
    PageHeader,
  ],
  template: `
    <app-page-header
      [title]="detalle()?.numeroCompleto ?? 'Comprobante electrónico'"
      [subtitle]="detalle() ? etiquetasTipo[detalle()!.tipo] : ''"
      icon="pi-verified"
      backLink="/comprobantes-electronicos"
      backLabel="Volver a comprobantes"
    />

    @if (error(); as mensaje) {
      <div class="panel">
        <app-empty-state
          tone="error"
          icon="pi-exclamation-triangle"
          title="No pudimos abrir el comprobante"
          [message]="mensaje"
        />
      </div>
    } @else if (detalle(); as d) {
      <section class="panel cabecera">
        <header>
          <app-estado-cpe-tag [estado]="d.estado" />
          @if (d.motivoBaja) {
            <p-tag severity="warn" value="Baja en trámite" />
          }
        </header>
        @if (d.estado === 'RECHAZADO') {
          <p-message severity="error" styleClass="w-full">
            SUNAT rechazó el comprobante: {{ d.cdrCodigo }} · {{ d.cdrDescripcion }}. Corrija los
            datos del cliente y vuelva a enviarlo.
          </p-message>
        }
        @if (d.estado === 'ERROR') {
          <p-message severity="error" styleClass="w-full">
            No se pudo enviar tras {{ d.intentos }} intentos: {{ d.ultimoError }}
          </p-message>
        }
        @if (d.estado === 'PENDIENTE' && d.ultimoError) {
          <p-message severity="warn" styleClass="w-full">
            Envío reprogramado. Último error: {{ d.ultimoError }}
          </p-message>
        }
        @if (d.estado === 'ACEPTADO_OBSERVACIONES') {
          <p-message severity="warn" styleClass="w-full">{{ d.cdrDescripcion }}</p-message>
        }
        <div class="datos">
          <div>
            <span class="rotulo">Emisión</span>
            <span>{{ d.emitidoEn | date: 'd MMM y, HH:mm' }}</span>
          </div>
          <div>
            <span class="rotulo">Cliente</span>
            <span>{{ d.clienteNombre }}</span>
          </div>
          <div>
            <span class="rotulo">{{
              d.clienteTipoDoc === 'SIN_DOC' ? 'Documento' : d.clienteTipoDoc
            }}</span>
            <span>{{ d.clienteDoc || 'Sin documento' }}</span>
          </div>
          <div>
            <span class="rotulo">Dirección</span>
            <span>{{ d.clienteDireccion || '—' }}</span>
          </div>
          @if (d.documentoReferenciaId) {
            <div>
              <span class="rotulo">Modifica a</span>
              <a [routerLink]="['/comprobantes-electronicos', d.documentoReferenciaId]">{{
                d.documentoReferenciaNumero
              }}</a>
            </div>
            <div>
              <span class="rotulo">Motivo</span>
              <span>{{ d.motivoCodigo }} · {{ d.motivoDescripcion }}</span>
            </div>
          }
          @if (d.comprobanteId) {
            <div>
              <span class="rotulo">Cobro de caja</span>
              <span>Comprobante interno {{ d.serie }}-{{ d.numero }}</span>
            </div>
          }
          @if (d.acreditado > 0) {
            <div>
              <span class="rotulo">Acreditado con notas</span>
              <span>{{ d.acreditado | currency: 'PEN' : 'symbol-narrow' }}</span>
            </div>
          }
        </div>
      </section>

      <section class="panel">
        <h3>Detalle</h3>
        <p-table [value]="d.items" responsiveLayout="scroll">
          <ng-template #header>
            <tr>
              <th>Descripción</th>
              <th class="text-right">Cant.</th>
              <th class="text-right">Precio</th>
              <th>Afectación</th>
              <th class="text-right">IGV</th>
              <th class="text-right">Importe</th>
            </tr>
          </ng-template>
          <ng-template #body let-i>
            <tr>
              <td>{{ i.descripcion }}</td>
              <td class="text-right">{{ i.cantidad }}</td>
              <td class="text-right">{{ i.precioUnitario | currency: 'PEN' : 'symbol-narrow' }}</td>
              <td>{{ afectaciones[i.afectacion] }}</td>
              <td class="text-right">{{ i.igv | currency: 'PEN' : 'symbol-narrow' }}</td>
              <td class="text-right">{{ i.total | currency: 'PEN' : 'symbol-narrow' }}</td>
            </tr>
          </ng-template>
        </p-table>
        <dl class="totales">
          @if (d.opGravada > 0) {
            <dt>Operación gravada</dt>
            <dd>{{ d.opGravada | currency: 'PEN' : 'symbol-narrow' }}</dd>
          }
          @if (d.opExonerada > 0) {
            <dt>Operación exonerada</dt>
            <dd>{{ d.opExonerada | currency: 'PEN' : 'symbol-narrow' }}</dd>
          }
          @if (d.opInafecta > 0) {
            <dt>Operación inafecta</dt>
            <dd>{{ d.opInafecta | currency: 'PEN' : 'symbol-narrow' }}</dd>
          }
          <dt>IGV</dt>
          <dd>{{ d.igv | currency: 'PEN' : 'symbol-narrow' }}</dd>
          <dt class="total">Total</dt>
          <dd class="total">{{ d.total | currency: 'PEN' : 'symbol-narrow' }}</dd>
        </dl>
      </section>

      <section class="panel">
        <h3>Firma y constancia de recepción</h3>
        <div class="datos">
          <div>
            <span class="rotulo">Valor resumen de la firma</span>
            <code class="hash">{{ d.digestValue || 'Sin firmar' }}</code>
          </div>
          <div>
            <span class="rotulo">Constancia</span>
            <span>{{
              d.cdrCodigo ? d.cdrCodigo + ' · ' + d.cdrDescripcion : 'Aún sin constancia'
            }}</span>
          </div>
          <div>
            <span class="rotulo">Recibida</span>
            <span>{{ d.cdrRecibidoEn ? (d.cdrRecibidoEn | date: 'd MMM y, HH:mm') : '—' }}</span>
          </div>
          <div>
            <span class="rotulo">Intentos de envío</span>
            <span>{{ d.intentos }}</span>
          </div>
        </div>
        <h4>Bitácora de envíos</h4>
        <p-table [value]="d.intercambios" responsiveLayout="scroll">
          <ng-template #header>
            <tr>
              <th>Fecha</th>
              <th>Operación</th>
              <th>Resultado</th>
              <th>Respuesta</th>
              <th class="text-right">Duración</th>
            </tr>
          </ng-template>
          <ng-template #body let-x>
            <tr>
              <td>{{ x.creadoEn | date: 'd MMM y, HH:mm:ss' }}</td>
              <td>{{ operaciones[x.operacion] }}</td>
              <td>
                <p-tag
                  [value]="x.exitoso ? 'Correcto' : 'Con error'"
                  [severity]="x.exitoso ? 'success' : 'danger'"
                />
              </td>
              <td>{{ x.codigoRespuesta }} {{ x.mensaje }}</td>
              <td class="text-right">{{ x.duracionMs }} ms</td>
            </tr>
          </ng-template>
          <ng-template #emptymessage>
            <tr>
              <td colspan="5">Todavía no se envió a SUNAT.</td>
            </tr>
          </ng-template>
        </p-table>
      </section>

      <div class="acciones">
        <p-button label="Ver PDF" icon="pi pi-eye" [outlined]="true" (onClick)="pdf('abrir')" />
        <p-button
          label="Descargar PDF"
          icon="pi pi-download"
          [outlined]="true"
          severity="secondary"
          (onClick)="pdf('guardar')"
        />
        @if (puedeGestionar()) {
          <p-button
            label="XML firmado"
            icon="pi pi-code"
            [outlined]="true"
            severity="secondary"
            (onClick)="archivo('xml')"
          />
          @if (d.cdrCodigo && d.estado !== 'RECHAZADO') {
            <p-button
              label="Constancia de SUNAT"
              icon="pi pi-file"
              [outlined]="true"
              severity="secondary"
              (onClick)="archivo('cdr')"
            />
          }
        }
        @if (d.estado === 'RECHAZADO' || d.estado === 'ERROR' || d.estado === 'PENDIENTE') {
          <p-button
            label="Corregir datos del cliente"
            icon="pi pi-user-edit"
            [outlined]="true"
            (onClick)="abrirCorreccion(d)"
          />
        }
        @if (d.estado === 'RECHAZADO' || d.estado === 'ERROR') {
          <p-button
            label="Reenviar"
            icon="pi pi-send"
            [loading]="trabajando()"
            (onClick)="reenviar()"
          />
        }
        @if (puedeGestionar() && esAceptado(d) && !d.motivoBaja) {
          @if (d.tipo === 'FACTURA' || d.tipo === 'BOLETA') {
            <p-button label="Emitir nota" icon="pi pi-file-edit" (onClick)="abrirNota()" />
          }
          <p-button
            label="Dar de baja"
            icon="pi pi-ban"
            severity="danger"
            [outlined]="true"
            (onClick)="baja()"
          />
        }
      </div>
    }

    <p-dialog
      header="Corregir datos del cliente"
      [(visible)]="corrigiendo"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '30rem', maxWidth: '96vw' }"
    >
      <form class="form" (submit)="$event.preventDefault(); corregir()" novalidate>
        @if (errorDialogo(); as mensaje) {
          <p-message severity="error" styleClass="w-full">{{ mensaje }}</p-message>
        }
        <label for="cor-tipo">Tipo de documento</label>
        <p-select
          inputId="cor-tipo"
          [options]="tiposDocumento"
          optionLabel="label"
          optionValue="value"
          [ngModel]="cliente().tipoDoc"
          (ngModelChange)="campoCliente('tipoDoc', $event)"
          [ngModelOptions]="{ standalone: true }"
          appendTo="body"
          styleClass="w-full"
        />
        <label for="cor-doc">Número de documento</label>
        <input
          pInputText
          id="cor-doc"
          class="w-full"
          maxlength="11"
          autocomplete="off"
          [ngModel]="cliente().documento"
          (ngModelChange)="campoCliente('documento', $event)"
          [ngModelOptions]="{ standalone: true }"
        />
        <label for="cor-nombre">Nombre o razón social</label>
        <input
          pInputText
          id="cor-nombre"
          class="w-full"
          maxlength="150"
          autocomplete="off"
          [ngModel]="cliente().nombre"
          (ngModelChange)="campoCliente('nombre', $event)"
          [ngModelOptions]="{ standalone: true }"
        />
        <label for="cor-dir">Dirección</label>
        <input
          pInputText
          id="cor-dir"
          class="w-full"
          maxlength="200"
          autocomplete="off"
          [ngModel]="cliente().direccion"
          (ngModelChange)="campoCliente('direccion', $event)"
          [ngModelOptions]="{ standalone: true }"
        />
        <small class="text-muted"
          >Se regenera el XML, se vuelve a firmar y se reenvía. El número no cambia.</small
        >
      </form>
      <ng-template #footer>
        <p-button
          label="Cancelar"
          severity="secondary"
          [outlined]="true"
          (onClick)="corrigiendo.set(false)"
        />
        <p-button
          label="Corregir y reenviar"
          icon="pi pi-check"
          [loading]="trabajando()"
          (onClick)="corregir()"
        />
      </ng-template>
    </p-dialog>

    <p-dialog
      header="Emitir una nota"
      [(visible)]="emitiendoNota"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '44rem', maxWidth: '96vw' }"
    >
      <form class="form" (submit)="$event.preventDefault(); emitirNota()" novalidate>
        @if (errorDialogo(); as mensaje) {
          <p-message severity="error" styleClass="w-full">{{ mensaje }}</p-message>
        }
        <label for="nota-tipo">Tipo de nota</label>
        <p-select
          inputId="nota-tipo"
          [options]="tiposNota"
          optionLabel="label"
          optionValue="value"
          [ngModel]="tipoNota()"
          (ngModelChange)="cambiarTipoNota($event)"
          [ngModelOptions]="{ standalone: true }"
          appendTo="body"
          styleClass="w-full"
        />
        <label for="nota-motivo">Motivo</label>
        <p-select
          inputId="nota-motivo"
          [options]="motivosDelTipo()"
          optionLabel="descripcion"
          optionValue="codigo"
          [ngModel]="motivoNota()"
          (ngModelChange)="motivoNota.set($event)"
          [ngModelOptions]="{ standalone: true }"
          appendTo="body"
          styleClass="w-full"
        />
        <label for="nota-desc">Sustento</label>
        <input
          pInputText
          id="nota-desc"
          class="w-full"
          maxlength="250"
          autocomplete="off"
          [ngModel]="descripcionNota()"
          (ngModelChange)="descripcionNota.set($event)"
          [ngModelOptions]="{ standalone: true }"
        />
        @if (motivoSeleccionado() && !motivoSeleccionado()!.total) {
          <h4>Ítems de la nota</h4>
          @for (fila of filasNota(); track $index) {
            <div class="fila-nota">
              <input
                pInputText
                class="w-full"
                maxlength="200"
                placeholder="Descripción"
                [attr.aria-label]="'Descripción del ítem ' + ($index + 1)"
                [ngModel]="fila.descripcion"
                (ngModelChange)="editarFila($index, 'descripcion', $event)"
                [ngModelOptions]="{ standalone: true }"
              />
              <p-inputnumber
                [ngModel]="fila.cantidad"
                (ngModelChange)="editarFila($index, 'cantidad', $event)"
                [ngModelOptions]="{ standalone: true }"
                [min]="1"
                [max]="9999"
                [ariaLabel]="'Cantidad del ítem ' + ($index + 1)"
                styleClass="w-full"
              />
              <p-inputnumber
                [ngModel]="fila.precioUnitario"
                (ngModelChange)="editarFila($index, 'precioUnitario', $event)"
                [ngModelOptions]="{ standalone: true }"
                mode="currency"
                currency="PEN"
                locale="es-PE"
                [min]="0.01"
                [ariaLabel]="'Precio con IGV del ítem ' + ($index + 1)"
                styleClass="w-full"
              />
              <p-button
                icon="pi pi-trash"
                [text]="true"
                [rounded]="true"
                severity="danger"
                [ariaLabel]="'Quitar el ítem ' + ($index + 1)"
                (onClick)="quitarFila($index)"
              />
            </div>
          }
          <p-button
            label="Agregar ítem"
            icon="pi pi-plus"
            size="small"
            [outlined]="true"
            (onClick)="agregarFila()"
          />
          <small class="text-muted">Los precios incluyen IGV cuando la operación es gravada.</small>
        } @else if (motivoSeleccionado()) {
          <small class="text-muted"
            >La nota repite todos los ítems del comprobante original por su importe completo.</small
          >
        }
      </form>
      <ng-template #footer>
        <p-button
          label="Cancelar"
          severity="secondary"
          [outlined]="true"
          (onClick)="emitiendoNota.set(false)"
        />
        <p-button
          label="Emitir nota"
          icon="pi pi-check"
          [loading]="trabajando()"
          (onClick)="emitirNota()"
        />
      </ng-template>
    </p-dialog>
  `,
  styles: `
    .cabecera,
    .panel {
      margin-bottom: 1rem;
    }

    .cabecera header {
      display: flex;
      gap: 0.5rem;
      margin-bottom: 0.75rem;
    }

    .datos {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(13rem, 1fr));
      gap: 1rem;
      margin-top: 0.75rem;
    }

    .datos > div {
      display: flex;
      flex-direction: column;
      gap: 0.15rem;
    }

    .rotulo {
      color: var(--p-text-muted-color);
      font-size: 0.8rem;
      text-transform: uppercase;
    }

    .hash {
      overflow-wrap: anywhere;
      font-size: 0.8rem;
    }

    .totales {
      display: grid;
      grid-template-columns: 1fr auto;
      gap: 0.25rem 1.5rem;
      margin: 1rem 0 0 auto;
      max-width: 22rem;
    }

    .totales dd {
      margin: 0;
      text-align: right;
    }

    .totales .total {
      font-weight: 700;
      font-size: 1.1rem;
    }

    .acciones {
      display: flex;
      flex-wrap: wrap;
      gap: 0.6rem;
    }

    .form {
      display: flex;
      flex-direction: column;
      gap: 0.4rem;
    }

    .form label {
      font-weight: 600;
      font-size: 0.9rem;
    }

    .fila-nota {
      display: grid;
      grid-template-columns: 1fr 6rem 9rem auto;
      gap: 0.5rem;
      align-items: center;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CpeDetalle {
  readonly id = input.required<string>();

  private readonly api = inject(FacturacionApiService);
  private readonly auth = inject(AuthService);
  private readonly descargas = inject(DescargaService);
  private readonly motivos = inject(MotivoService);
  private readonly notificaciones = inject(NotificationService);

  protected readonly etiquetasTipo: Record<string, string> = ETIQUETA_TIPO_CPE;
  protected readonly afectaciones: Record<string, string> = ETIQUETA_AFECTACION;
  protected readonly operaciones: Record<string, string> = ETIQUETA_OPERACION_SUNAT;
  protected readonly tiposDocumento = [
    { label: 'RUC', value: 'RUC' },
    { label: 'DNI', value: 'DNI' },
    { label: 'Sin documento', value: 'SIN_DOC' },
  ];
  protected readonly tiposNota = [
    { label: 'Nota de crédito', value: 'NOTA_CREDITO' },
    { label: 'Nota de débito', value: 'NOTA_DEBITO' },
  ];

  protected readonly detalle = signal<DocumentoElectronicoDetalle | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly trabajando = signal(false);
  protected readonly errorDialogo = signal<string | null>(null);
  protected readonly corrigiendo = signal(false);
  protected readonly emitiendoNota = signal(false);
  protected readonly cliente = signal({
    tipoDoc: 'RUC' as 'DNI' | 'RUC' | 'SIN_DOC',
    documento: '',
    nombre: '',
    direccion: '',
  });
  protected readonly catalogoMotivos = signal<MotivoNota[]>([]);
  protected readonly tipoNota = signal<'NOTA_CREDITO' | 'NOTA_DEBITO'>('NOTA_CREDITO');
  protected readonly motivoNota = signal<string>('');
  protected readonly descripcionNota = signal('');
  protected readonly filasNota = signal<FilaNota[]>([]);
  protected readonly puedeGestionar = computed(
    () => this.auth.isAdmin() || this.auth.roles().includes('ROLE_FINANZAS'),
  );
  protected readonly motivosDelTipo = computed(() =>
    this.catalogoMotivos().filter((m) => m.tipo === this.tipoNota()),
  );
  protected readonly motivoSeleccionado = computed(
    () => this.motivosDelTipo().find((m) => m.codigo === this.motivoNota()) ?? null,
  );

  constructor() {
    effect(() => {
      const id = this.id();
      untracked(() => this.cargar(id));
    });
  }

  protected esAceptado(d: DocumentoElectronicoDetalle): boolean {
    return d.estado === 'ACEPTADO' || d.estado === 'ACEPTADO_OBSERVACIONES';
  }

  protected pdf(accion: 'abrir' | 'guardar'): void {
    this.api.pdf(this.id()).subscribe((archivo) => {
      if (accion === 'abrir') {
        this.descargas.abrir(archivo);
      } else {
        this.descargas.guardar(archivo);
      }
    });
  }

  protected archivo(tipo: 'xml' | 'cdr'): void {
    const origen = tipo === 'xml' ? this.api.xml(this.id()) : this.api.cdr(this.id());
    origen.subscribe((archivo) => this.descargas.guardar(archivo));
  }

  protected reenviar(): void {
    this.trabajando.set(true);
    this.api.reenviar(this.id()).subscribe({
      next: (d) => this.tras(d, 'El comprobante se reenvió.'),
      error: () => this.trabajando.set(false),
    });
  }

  protected abrirCorreccion(d: DocumentoElectronicoDetalle): void {
    this.errorDialogo.set(null);
    this.cliente.set({
      tipoDoc: d.clienteTipoDoc,
      documento: d.clienteDoc ?? '',
      nombre: d.clienteNombre,
      direccion: d.clienteDireccion ?? '',
    });
    this.corrigiendo.set(true);
  }

  protected campoCliente(
    campo: 'tipoDoc' | 'documento' | 'nombre' | 'direccion',
    valor: string,
  ): void {
    this.cliente.update((actual) => ({ ...actual, [campo]: valor }));
  }

  protected corregir(): void {
    const c = this.cliente();
    if (!c.nombre.trim()) {
      this.errorDialogo.set('Indique el nombre o la razón social.');
      return;
    }
    this.trabajando.set(true);
    this.api
      .corregirCliente(this.id(), {
        tipoDoc: c.tipoDoc,
        documento: c.documento.trim() || null,
        nombre: c.nombre.trim(),
        direccion: c.direccion.trim() || null,
      })
      .subscribe({
        next: (d) => {
          this.corrigiendo.set(false);
          this.tras(d, 'Se corrigieron los datos y se reenvió el comprobante.');
        },
        error: (e: { message: string }) => {
          this.trabajando.set(false);
          this.errorDialogo.set(e.message);
        },
      });
  }

  protected baja(): void {
    const d = this.detalle();
    if (!d) {
      return;
    }
    this.motivos
      .solicitar({
        titulo: 'Dar de baja el comprobante',
        descripcion: `Se comunicará a SUNAT la baja de ${d.numeroCompleto}. Solo procede dentro del plazo de baja configurado y si no tiene notas de crédito.`,
        etiqueta: 'Motivo de la baja',
        confirmar: 'Comunicar baja',
        severidad: 'danger',
        icono: 'pi pi-ban',
      })
      .subscribe((motivo) =>
        this.api.baja(this.id(), motivo).subscribe((nuevo) => {
          this.detalle.set(nuevo);
          this.notificaciones.success('La baja quedó en trámite.');
        }),
      );
  }

  protected abrirNota(): void {
    this.errorDialogo.set(null);
    this.tipoNota.set('NOTA_CREDITO');
    this.descripcionNota.set('');
    this.filasNota.set([{ descripcion: '', cantidad: 1, precioUnitario: 0 }]);
    this.api.motivos().subscribe((lista) => {
      this.catalogoMotivos.set(lista);
      this.motivoNota.set(this.motivosDelTipo()[0]?.codigo ?? '');
      this.emitiendoNota.set(true);
    });
  }

  protected cambiarTipoNota(tipo: 'NOTA_CREDITO' | 'NOTA_DEBITO'): void {
    this.tipoNota.set(tipo);
    this.motivoNota.set(this.motivosDelTipo()[0]?.codigo ?? '');
  }

  protected agregarFila(): void {
    this.filasNota.update((filas) => [
      ...filas,
      { descripcion: '', cantidad: 1, precioUnitario: 0 },
    ]);
  }

  protected quitarFila(indice: number): void {
    this.filasNota.update((filas) => filas.filter((_, i) => i !== indice));
  }

  protected editarFila(indice: number, campo: keyof FilaNota, valor: string | number): void {
    this.filasNota.update((filas) =>
      filas.map((fila, i) => (i === indice ? ({ ...fila, [campo]: valor } as FilaNota) : fila)),
    );
  }

  protected emitirNota(): void {
    const motivo = this.motivoSeleccionado();
    if (!motivo) {
      this.errorDialogo.set('Elija el motivo de la nota.');
      return;
    }
    if (this.descripcionNota().trim().length < 5) {
      this.errorDialogo.set('Escriba el sustento de la nota, con al menos 5 caracteres.');
      return;
    }
    let items: NotaItemRequest[] | null = null;
    if (!motivo.total) {
      items = this.filasNota()
        .filter((f) => f.descripcion.trim() && f.precioUnitario > 0)
        .map((f) => ({
          descripcion: f.descripcion.trim(),
          cantidad: f.cantidad,
          precioUnitario: f.precioUnitario,
        }));
      if (items.length === 0) {
        this.errorDialogo.set('Agregue al menos un ítem con descripción e importe.');
        return;
      }
    }
    this.trabajando.set(true);
    this.api
      .nota({
        documentoId: this.id(),
        tipo: this.tipoNota(),
        motivoCodigo: motivo.codigo,
        descripcion: this.descripcionNota().trim(),
        items,
      })
      .subscribe({
        next: (nota) => {
          this.trabajando.set(false);
          this.emitiendoNota.set(false);
          this.notificaciones.success(`Se emitió la nota ${nota.numeroCompleto}.`);
          this.cargar(this.id());
        },
        error: (e: { message: string }) => {
          this.trabajando.set(false);
          this.errorDialogo.set(e.message);
        },
      });
  }

  private tras(d: DocumentoElectronicoDetalle, mensaje: string): void {
    this.trabajando.set(false);
    this.detalle.set(d);
    this.notificaciones.success(mensaje);
  }

  private cargar(id: string): void {
    this.error.set(null);
    this.api.detalle(id).subscribe({
      next: (d) => this.detalle.set(d),
      error: (e: { message: string }) => this.error.set(e.message),
    });
  }
}
