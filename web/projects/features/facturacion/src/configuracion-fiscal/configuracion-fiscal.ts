import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { Checkbox } from 'primeng/checkbox';
import { Dialog } from 'primeng/dialog';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';

import {
  ConfiguracionFiscal,
  ConfiguracionFiscalRequest,
  ETIQUETA_AFECTACION,
  ETIQUETA_PROVEEDOR_FISCAL,
  ETIQUETA_TIPO_CPE,
  FacturacionApiService,
  NotificationService,
  SerieComprobante,
  TipoCpe,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';

const VACIA: ConfiguracionFiscalRequest = {
  habilitada: false,
  ruc: '',
  razonSocial: '',
  nombreComercial: null,
  direccion: '',
  ubigeo: '',
  departamento: '',
  provincia: '',
  distrito: '',
  codigoLocal: '0000',
  proveedor: 'SIMULACION',
  modo: 'BETA',
  urlServicio: null,
  afectacionServicios: 'GRAVADO',
  boletasPorResumen: false,
  diasLimiteBaja: 7,
};

@Component({
  selector: 'app-configuracion-fiscal',
  imports: [
    FormsModule,
    Button,
    Checkbox,
    Dialog,
    InputNumber,
    InputText,
    Message,
    Select,
    TableModule,
    Tag,
    EmptyState,
    PageHeader,
  ],
  template: `
    <app-page-header
      title="Configuración fiscal"
      subtitle="Datos del emisor, proveedor de envío y series de comprobantes"
      icon="pi-cog"
    />

    <p class="panel aviso" role="note">
      El usuario y la clave SOL, y el certificado digital, se configuran como secretos del servidor.
      No se guardan en la base de datos ni se muestran aquí.
    </p>

    @if (config(); as c) {
      <section class="panel estado">
        <p-tag
          [value]="c.habilitada ? 'Facturación habilitada' : 'Facturación deshabilitada'"
          [severity]="c.habilitada ? 'success' : 'secondary'"
        />
        <p-tag
          [value]="c.certificadoDePrueba ? 'Certificado de prueba' : 'Certificado configurado'"
          [severity]="c.certificadoDePrueba ? 'warn' : 'success'"
        />
        <p-tag
          [value]="
            c.credencialesSolConfiguradas ? 'Credenciales SOL configuradas' : 'Sin credenciales SOL'
          "
          [severity]="c.credencialesSolConfiguradas ? 'success' : 'secondary'"
        />
      </section>
    }

    <section class="panel">
      <h3>Emisor</h3>
      <form class="grid" (submit)="$event.preventDefault(); guardar()" novalidate>
        @if (error(); as mensaje) {
          <p-message severity="error" styleClass="col-span">{{ mensaje }}</p-message>
        }
        <div class="campo">
          <label for="cf-ruc">RUC</label>
          <input
            pInputText
            id="cf-ruc"
            maxlength="11"
            autocomplete="off"
            [ngModel]="form().ruc"
            (ngModelChange)="campo('ruc', $event)"
            [ngModelOptions]="{ standalone: true }"
          />
        </div>
        <div class="campo">
          <label for="cf-razon">Razón social</label>
          <input
            pInputText
            id="cf-razon"
            maxlength="150"
            autocomplete="off"
            [ngModel]="form().razonSocial"
            (ngModelChange)="campo('razonSocial', $event)"
            [ngModelOptions]="{ standalone: true }"
          />
        </div>
        <div class="campo">
          <label for="cf-comercial">Nombre comercial</label>
          <input
            pInputText
            id="cf-comercial"
            maxlength="150"
            autocomplete="off"
            [ngModel]="form().nombreComercial"
            (ngModelChange)="campo('nombreComercial', $event)"
            [ngModelOptions]="{ standalone: true }"
          />
        </div>
        <div class="campo ancho">
          <label for="cf-dir">Dirección fiscal</label>
          <input
            pInputText
            id="cf-dir"
            maxlength="200"
            autocomplete="off"
            [ngModel]="form().direccion"
            (ngModelChange)="campo('direccion', $event)"
            [ngModelOptions]="{ standalone: true }"
          />
        </div>
        <div class="campo">
          <label for="cf-ubigeo">Ubigeo</label>
          <input
            pInputText
            id="cf-ubigeo"
            maxlength="6"
            autocomplete="off"
            [ngModel]="form().ubigeo"
            (ngModelChange)="campo('ubigeo', $event)"
            [ngModelOptions]="{ standalone: true }"
          />
        </div>
        <div class="campo">
          <label for="cf-dep">Departamento</label>
          <input
            pInputText
            id="cf-dep"
            maxlength="60"
            autocomplete="off"
            [ngModel]="form().departamento"
            (ngModelChange)="campo('departamento', $event)"
            [ngModelOptions]="{ standalone: true }"
          />
        </div>
        <div class="campo">
          <label for="cf-prov">Provincia</label>
          <input
            pInputText
            id="cf-prov"
            maxlength="60"
            autocomplete="off"
            [ngModel]="form().provincia"
            (ngModelChange)="campo('provincia', $event)"
            [ngModelOptions]="{ standalone: true }"
          />
        </div>
        <div class="campo">
          <label for="cf-dist">Distrito</label>
          <input
            pInputText
            id="cf-dist"
            maxlength="60"
            autocomplete="off"
            [ngModel]="form().distrito"
            (ngModelChange)="campo('distrito', $event)"
            [ngModelOptions]="{ standalone: true }"
          />
        </div>
        <div class="campo">
          <label for="cf-local">Código de local anexo</label>
          <input
            pInputText
            id="cf-local"
            maxlength="4"
            autocomplete="off"
            [ngModel]="form().codigoLocal"
            (ngModelChange)="campo('codigoLocal', $event)"
            [ngModelOptions]="{ standalone: true }"
          />
        </div>

        <h3 class="col-span">Envío</h3>
        <div class="campo">
          <label for="cf-proveedor">Proveedor de envío</label>
          <p-select
            inputId="cf-proveedor"
            [options]="proveedores"
            optionLabel="label"
            optionValue="value"
            [ngModel]="form().proveedor"
            (ngModelChange)="campo('proveedor', $event)"
            [ngModelOptions]="{ standalone: true }"
            appendTo="body"
            styleClass="w-full"
          />
        </div>
        <div class="campo">
          <label for="cf-modo">Ambiente</label>
          <p-select
            inputId="cf-modo"
            [options]="modos"
            optionLabel="label"
            optionValue="value"
            [ngModel]="form().modo"
            (ngModelChange)="campo('modo', $event)"
            [ngModelOptions]="{ standalone: true }"
            appendTo="body"
            styleClass="w-full"
          />
        </div>
        <div class="campo ancho">
          <label for="cf-url">Dirección del servicio, solo para OSE o PSE</label>
          <input
            pInputText
            id="cf-url"
            maxlength="300"
            autocomplete="off"
            [ngModel]="form().urlServicio"
            (ngModelChange)="campo('urlServicio', $event)"
            [ngModelOptions]="{ standalone: true }"
          />
        </div>
        <div class="campo">
          <label for="cf-afectacion">Afectación de los servicios</label>
          <p-select
            inputId="cf-afectacion"
            [options]="afectaciones"
            optionLabel="label"
            optionValue="value"
            [ngModel]="form().afectacionServicios"
            (ngModelChange)="campo('afectacionServicios', $event)"
            [ngModelOptions]="{ standalone: true }"
            appendTo="body"
            styleClass="w-full"
          />
        </div>
        <div class="campo">
          <label for="cf-baja">Días límite para la baja</label>
          <p-inputnumber
            inputId="cf-baja"
            [ngModel]="form().diasLimiteBaja"
            (ngModelChange)="campo('diasLimiteBaja', $event)"
            [ngModelOptions]="{ standalone: true }"
            [min]="1"
            [max]="7"
          />
        </div>
        <div class="check col-span">
          <p-checkbox
            inputId="cf-resumen"
            [binary]="true"
            [ngModel]="form().boletasPorResumen"
            (ngModelChange)="campo('boletasPorResumen', $event)"
            [ngModelOptions]="{ standalone: true }"
          />
          <label for="cf-resumen"
            >Informar las boletas por resumen diario en lugar de enviarlas una a una</label
          >
        </div>
        <div class="check col-span">
          <p-checkbox
            inputId="cf-habilitada"
            [binary]="true"
            [ngModel]="form().habilitada"
            (ngModelChange)="campo('habilitada', $event)"
            [ngModelOptions]="{ standalone: true }"
          />
          <label for="cf-habilitada">Emitir comprobantes electrónicos en cada cobro de caja</label>
        </div>
        <div class="col-span">
          <p-button
            label="Guardar configuración"
            icon="pi pi-check"
            [loading]="guardando()"
            (onClick)="guardar()"
          />
        </div>
      </form>
    </section>

    <section class="panel">
      <header class="cabecera-series">
        <h3>Series y correlativos</h3>
        <p-button label="Nueva serie" icon="pi pi-plus" size="small" (onClick)="abrirSerie()" />
      </header>
      <p-table [value]="series()" responsiveLayout="scroll">
        <ng-template #header>
          <tr>
            <th>Serie</th>
            <th>Tipo</th>
            <th class="text-right">Último número</th>
            <th>Estado</th>
            <th></th>
          </tr>
        </ng-template>
        <ng-template #body let-s>
          <tr>
            <td class="font-semibold">{{ s.serie }}</td>
            <td>{{ tipos[s.tipo] }}</td>
            <td class="text-right">{{ s.correlativoActual }}</td>
            <td>
              <p-tag
                [value]="s.activa ? 'Activa' : 'Inactiva'"
                [severity]="s.activa ? 'success' : 'secondary'"
              />
            </td>
            <td class="derecha">
              <p-button
                [label]="s.activa ? 'Desactivar' : 'Activar'"
                size="small"
                [outlined]="true"
                [ariaLabel]="(s.activa ? 'Desactivar' : 'Activar') + ' la serie ' + s.serie"
                (onClick)="cambiarSerie(s)"
              />
            </td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr>
            <td colspan="5"><app-empty-state icon="pi-hashtag" title="Sin series" /></td>
          </tr>
        </ng-template>
      </p-table>
    </section>

    <p-dialog
      header="Nueva serie"
      [(visible)]="creandoSerie"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '26rem', maxWidth: '96vw' }"
    >
      <form class="form" (submit)="$event.preventDefault(); crearSerie()" novalidate>
        @if (errorSerie(); as mensaje) {
          <p-message severity="error" styleClass="w-full">{{ mensaje }}</p-message>
        }
        <label for="ns-tipo">Tipo de comprobante</label>
        <p-select
          inputId="ns-tipo"
          [options]="opcionesTipo"
          optionLabel="label"
          optionValue="value"
          [ngModel]="serieTipo()"
          (ngModelChange)="serieTipo.set($event)"
          [ngModelOptions]="{ standalone: true }"
          appendTo="body"
          styleClass="w-full"
        />
        <label for="ns-serie">Serie</label>
        <input
          pInputText
          id="ns-serie"
          maxlength="4"
          class="w-full"
          autocomplete="off"
          placeholder="F001"
          [ngModel]="serieCodigo()"
          (ngModelChange)="serieCodigo.set(($event ?? '').toUpperCase())"
          [ngModelOptions]="{ standalone: true }"
        />
        <small class="text-muted"
          >Empieza con F para facturas y sus notas, o con B para boletas y sus notas.</small
        >
      </form>
      <ng-template #footer>
        <p-button
          label="Cancelar"
          severity="secondary"
          [outlined]="true"
          (onClick)="creandoSerie.set(false)"
        />
        <p-button label="Crear serie" icon="pi pi-check" (onClick)="crearSerie()" />
      </ng-template>
    </p-dialog>
  `,
  styles: `
    .aviso {
      margin-bottom: 1rem;
      border-left: 4px solid var(--p-primary-color);
    }

    .estado {
      display: flex;
      flex-wrap: wrap;
      gap: 0.5rem;
      margin-bottom: 1rem;
    }

    .panel {
      margin-bottom: 1rem;
    }

    .grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(14rem, 1fr));
      gap: 0.9rem 1rem;
      align-items: end;
    }

    .col-span {
      grid-column: 1 / -1;
    }

    .ancho {
      grid-column: span 2;
    }

    .campo {
      display: flex;
      flex-direction: column;
      gap: 0.3rem;
    }

    .campo label,
    .form label {
      font-weight: 600;
      font-size: 0.9rem;
    }

    .check {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .cabecera-series {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .derecha {
      text-align: right;
    }

    .form {
      display: flex;
      flex-direction: column;
      gap: 0.4rem;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConfiguracionFiscalPagina implements OnInit {
  private readonly api = inject(FacturacionApiService);
  private readonly notificaciones = inject(NotificationService);

  protected readonly tipos: Record<string, string> = ETIQUETA_TIPO_CPE;
  protected readonly proveedores = Object.entries(ETIQUETA_PROVEEDOR_FISCAL).map(
    ([value, label]) => ({
      label,
      value,
    }),
  );
  protected readonly modos = [
    { label: 'Beta, pruebas', value: 'BETA' },
    { label: 'Producción', value: 'PRODUCCION' },
  ];
  protected readonly afectaciones = Object.entries(ETIQUETA_AFECTACION).map(([value, label]) => ({
    label,
    value,
  }));
  protected readonly opcionesTipo = Object.entries(ETIQUETA_TIPO_CPE).map(([value, label]) => ({
    label,
    value,
  }));

  protected readonly config = signal<ConfiguracionFiscal | null>(null);
  protected readonly form = signal<ConfiguracionFiscalRequest>({ ...VACIA });
  protected readonly series = signal<SerieComprobante[]>([]);
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly creandoSerie = signal(false);
  protected readonly serieTipo = signal<TipoCpe>('NOTA_CREDITO');
  protected readonly serieCodigo = signal('');
  protected readonly errorSerie = signal<string | null>(null);

  ngOnInit(): void {
    this.api.configuracion().subscribe({
      next: (c) => {
        this.config.set(c);
        this.form.set({
          habilitada: c.habilitada,
          ruc: c.ruc,
          razonSocial: c.razonSocial,
          nombreComercial: c.nombreComercial,
          direccion: c.direccion,
          ubigeo: c.ubigeo,
          departamento: c.departamento,
          provincia: c.provincia,
          distrito: c.distrito,
          codigoLocal: c.codigoLocal,
          proveedor: c.proveedor,
          modo: c.modo,
          urlServicio: c.urlServicio,
          afectacionServicios: c.afectacionServicios,
          boletasPorResumen: c.boletasPorResumen,
          diasLimiteBaja: c.diasLimiteBaja,
        });
      },
      error: () => undefined,
    });
    this.cargarSeries();
  }

  protected campo<K extends keyof ConfiguracionFiscalRequest>(
    clave: K,
    valor: ConfiguracionFiscalRequest[K],
  ): void {
    this.form.update((actual) => ({ ...actual, [clave]: valor }));
  }

  protected guardar(): void {
    this.error.set(null);
    this.guardando.set(true);
    const f = this.form();
    this.api
      .guardarConfiguracion({
        ...f,
        nombreComercial: f.nombreComercial?.trim() || null,
        urlServicio: f.urlServicio?.trim() || null,
      })
      .subscribe({
        next: (c) => {
          this.guardando.set(false);
          this.config.set(c);
          this.notificaciones.success('La configuración fiscal se guardó.');
        },
        error: (e: { message: string }) => {
          this.guardando.set(false);
          this.error.set(e.message);
        },
      });
  }

  protected abrirSerie(): void {
    this.errorSerie.set(null);
    this.serieCodigo.set('');
    this.creandoSerie.set(true);
  }

  protected crearSerie(): void {
    this.api
      .crearSerie({
        tipo: this.serieTipo(),
        serie: this.serieCodigo().trim(),
        correlativoInicial: 0,
      })
      .subscribe({
        next: () => {
          this.creandoSerie.set(false);
          this.notificaciones.success('La serie se creó.');
          this.cargarSeries();
        },
        error: (e: { message: string }) => this.errorSerie.set(e.message),
      });
  }

  protected cambiarSerie(s: SerieComprobante): void {
    this.api.estadoSerie(s.id, !s.activa).subscribe(() => this.cargarSeries());
  }

  private cargarSeries(): void {
    this.api.series().subscribe((lista) => this.series.set(lista));
  }
}
