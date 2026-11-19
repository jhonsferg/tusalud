import { CurrencyPipe, DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Subject, debounceTime } from 'rxjs';

import {
  AuthService,
  DescargaService,
  DocumentoElectronicoResumen,
  ETIQUETA_ESTADO_CPE,
  ETIQUETA_TIPO_CPE,
  EstadoCpe,
  EstadoFacturacion,
  FacturacionApiService,
  NotificationService,
  TipoCpe,
  toIsoDate,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { EstadoCpeTag } from '../estado-cpe-tag';

@Component({
  selector: 'app-bandeja-cpe',
  imports: [
    CurrencyPipe,
    DatePipe,
    FormsModule,
    RouterLink,
    Button,
    DatePicker,
    IconField,
    InputIcon,
    InputText,
    Select,
    TableModule,
    EmptyState,
    EstadoCpeTag,
    PageHeader,
  ],
  template: `
    <app-page-header
      title="Comprobantes electrónicos"
      subtitle="Boletas, facturas y notas enviadas a SUNAT, con su constancia de recepción"
      icon="pi-verified"
    >
      @if (puedeGestionar()) {
        <p-button
          label="Procesar envíos ahora"
          icon="pi pi-send"
          [outlined]="true"
          [loading]="procesando()"
          (onClick)="procesar()"
        />
      }
    </app-page-header>

    @if (estado(); as e) {
      @if (!e.habilitada) {
        <p class="panel aviso" role="status">
          La facturación electrónica está deshabilitada. Los cobros se emiten solo como comprobantes
          internos.
        </p>
      } @else if (e.proveedor === 'SIMULACION') {
        <p class="panel aviso" role="status">
          Modo de simulación: los comprobantes se firman y se envían a un SUNAT simulado, sin valor
          tributario.
        </p>
      }
      <div class="resumen" aria-label="Resumen de los últimos 30 días">
        <div>
          <strong>{{ e.pendientesDeEnvio }}</strong>
          <span>Por enviar</span>
        </div>
        <div>
          <strong>{{ e.conteo['ACEPTADO'] + e.conteo['ACEPTADO_OBSERVACIONES'] }}</strong>
          <span>Aceptados</span>
        </div>
        <div [class.alerta]="e.conteo['RECHAZADO'] > 0">
          <strong>{{ e.conteo['RECHAZADO'] }}</strong>
          <span>Rechazados</span>
        </div>
        <div [class.alerta]="e.conteo['ERROR'] > 0">
          <strong>{{ e.conteo['ERROR'] }}</strong>
          <span>Con error de envío</span>
        </div>
      </div>
    }

    <section class="panel">
      <div class="filtros">
        <p-iconfield class="w-full md:w-20rem">
          <p-inputicon class="pi pi-search" />
          <input
            pInputText
            type="search"
            class="w-full"
            placeholder="Buscar por cliente, documento o serie"
            aria-label="Buscar comprobante"
            [ngModel]="busqueda()"
            (ngModelChange)="buscar($event)"
          />
        </p-iconfield>
        <p-select
          [options]="opcionesTipo"
          optionLabel="label"
          optionValue="value"
          [ngModel]="tipo()"
          (ngModelChange)="tipo.set($event); cargar()"
          placeholder="Todos los tipos"
          [showClear]="true"
          ariaLabel="Filtrar por tipo"
          appendTo="body"
          styleClass="w-full md:w-12rem"
        />
        <p-select
          [options]="opcionesEstado"
          optionLabel="label"
          optionValue="value"
          [ngModel]="estadoFiltro()"
          (ngModelChange)="estadoFiltro.set($event); cargar()"
          placeholder="Todos los estados"
          [showClear]="true"
          ariaLabel="Filtrar por estado"
          appendTo="body"
          styleClass="w-full md:w-12rem"
        />
        <p-datepicker
          selectionMode="range"
          [ngModel]="rango()"
          (ngModelChange)="cambiarRango($event)"
          [showIcon]="true"
          [showButtonBar]="true"
          dateFormat="dd/mm/yy"
          placeholder="Filtrar por fechas"
          [readonlyInput]="true"
          appendTo="body"
        />
      </div>

      @if (error(); as mensaje) {
        <app-empty-state
          tone="error"
          icon="pi-exclamation-triangle"
          title="No pudimos cargar los comprobantes"
          [message]="mensaje"
        >
          <p-button label="Reintentar" icon="pi pi-refresh" (onClick)="cargar()" />
        </app-empty-state>
      } @else {
        <p-table
          [value]="documentos()"
          [loading]="cargando()"
          [paginator]="documentos().length > 12"
          [rows]="12"
          [rowHover]="true"
          responsiveLayout="scroll"
        >
          <ng-template #header>
            <tr>
              <th>Comprobante</th>
              <th>Emisión</th>
              <th>Cliente</th>
              <th class="text-right">Total</th>
              <th>Estado</th>
              <th style="width: 9rem"></th>
            </tr>
          </ng-template>
          <ng-template #body let-d>
            <tr>
              <td>
                <span class="font-semibold">{{ d.numeroCompleto }}</span>
                <small class="block text-muted">{{ etiquetasTipo[d.tipo] }}</small>
              </td>
              <td>{{ d.emitidoEn | date: 'd MMM y, HH:mm' }}</td>
              <td>
                {{ d.clienteNombre }}
                <small class="block text-muted">{{ d.clienteDoc || 'Sin documento' }}</small>
              </td>
              <td class="text-right">{{ d.total | currency: 'PEN' : 'symbol-narrow' }}</td>
              <td>
                <app-estado-cpe-tag [estado]="d.estado" />
                @if (d.estado === 'RECHAZADO' || d.estado === 'ERROR') {
                  <small class="block text-muted">{{ d.cdrDescripcion }}</small>
                }
              </td>
              <td class="derecha">
                <p-button
                  icon="pi pi-file-pdf"
                  size="small"
                  [text]="true"
                  [rounded]="true"
                  [ariaLabel]="'Descargar el PDF de ' + d.numeroCompleto"
                  (onClick)="pdf(d)"
                />
                <p-button
                  label="Ver"
                  size="small"
                  [outlined]="true"
                  [routerLink]="[d.id]"
                  [ariaLabel]="'Ver el detalle de ' + d.numeroCompleto"
                />
              </td>
            </tr>
          </ng-template>
          <ng-template #emptymessage>
            <tr>
              <td colspan="6">
                <app-empty-state
                  icon="pi-verified"
                  title="Sin comprobantes"
                  message="No hay comprobantes electrónicos con estos filtros."
                />
              </td>
            </tr>
          </ng-template>
        </p-table>
      }
    </section>
  `,
  styles: `
    .filtros {
      display: flex;
      flex-wrap: wrap;
      gap: 0.75rem;
      margin-bottom: 1rem;
    }

    .derecha {
      text-align: right;
      white-space: nowrap;
    }

    .aviso {
      margin-bottom: 1rem;
      border-left: 4px solid var(--p-primary-color);
    }

    .resumen {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(10rem, 1fr));
      gap: 0.75rem;
      margin-bottom: 1rem;
    }

    .resumen div {
      display: flex;
      flex-direction: column;
      padding: 0.8rem 1rem;
      border: 1px solid var(--p-content-border-color);
      border-radius: 8px;
      background: var(--p-content-background);
    }

    .resumen strong {
      font-size: 1.5rem;
    }

    .resumen span {
      color: var(--p-text-muted-color);
      font-size: 0.85rem;
    }

    .resumen .alerta strong {
      color: var(--p-red-600);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BandejaCpe implements OnInit {
  private readonly api = inject(FacturacionApiService);
  private readonly auth = inject(AuthService);
  private readonly descargas = inject(DescargaService);
  private readonly notificaciones = inject(NotificationService);

  protected readonly etiquetasTipo: Record<string, string> = ETIQUETA_TIPO_CPE;
  protected readonly opcionesTipo = Object.entries(ETIQUETA_TIPO_CPE).map(([value, label]) => ({
    label,
    value,
  }));
  protected readonly opcionesEstado = Object.entries(ETIQUETA_ESTADO_CPE).map(([value, label]) => ({
    label,
    value,
  }));

  protected readonly documentos = signal<DocumentoElectronicoResumen[]>([]);
  protected readonly estado = signal<EstadoFacturacion | null>(null);
  protected readonly cargando = signal(false);
  protected readonly procesando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly busqueda = signal('');
  protected readonly tipo = signal<TipoCpe | null>(null);
  protected readonly estadoFiltro = signal<EstadoCpe | null>(null);
  protected readonly rango = signal<Date[] | null>(null);
  protected readonly puedeGestionar = computed(
    () => this.auth.isAdmin() || this.auth.roles().includes('ROLE_FINANZAS'),
  );

  private readonly busquedas = new Subject<string>();

  constructor() {
    this.busquedas.pipe(debounceTime(300)).subscribe(() => this.cargar());
  }

  ngOnInit(): void {
    this.cargar();
    this.api.estado().subscribe({ next: (e) => this.estado.set(e), error: () => undefined });
  }

  protected buscar(texto: string): void {
    this.busqueda.set(texto);
    this.busquedas.next(texto);
  }

  protected cambiarRango(rango: Date[] | null): void {
    this.rango.set(rango);
    if (!rango || rango[1]) {
      this.cargar();
    }
  }

  protected cargar(): void {
    const rango = this.rango();
    const desde = rango?.[0];
    this.cargando.set(true);
    this.error.set(null);
    this.api
      .documentos({
        desde: desde ? toIsoDate(desde) : undefined,
        hasta: desde ? toIsoDate(rango?.[1] ?? desde) : undefined,
        tipo: this.tipo(),
        estado: this.estadoFiltro(),
        q: this.busqueda().trim() || undefined,
      })
      .subscribe({
        next: (lista) => {
          this.documentos.set(lista);
          this.cargando.set(false);
        },
        error: (e: { message: string }) => {
          this.error.set(e.message);
          this.cargando.set(false);
        },
      });
  }

  protected procesar(): void {
    this.procesando.set(true);
    this.api.procesarCola().subscribe({
      next: (r) => {
        this.procesando.set(false);
        this.notificaciones.success(`Se procesaron ${r.procesados} envíos.`);
        this.cargar();
        this.api.estado().subscribe((e) => this.estado.set(e));
      },
      error: () => this.procesando.set(false),
    });
  }

  protected pdf(d: DocumentoElectronicoResumen): void {
    this.api.pdf(d.id).subscribe((archivo) => this.descargas.guardar(archivo));
  }
}
