import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { Checkbox } from 'primeng/checkbox';
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Tab, TabList, TabPanel, TabPanels, Tabs } from 'primeng/tabs';
import { Tag } from 'primeng/tag';
import { Observable } from 'rxjs';

import {
  BancoDatos,
  BancoDatosRequest,
  EncargadoRequest,
  EncargadoTratamiento,
  FinalidadRequest,
  FinalidadTratamiento,
  NotificationService,
  ProteccionDatosApiService,
  RegistroTratamientos,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';

type Entidad = 'banco' | 'finalidad' | 'encargado';

const BANCO_VACIO: BancoDatosRequest = {
  codigo: '',
  nombre: '',
  descripcion: '',
  finalidadPrincipal: '',
  categoriasDatos: '',
  contieneSensibles: false,
  responsable: '',
  ubicacion: '',
  medidasSeguridad: '',
  transferenciaTransfronteriza: false,
  numeroInscripcion: null,
  fechaInscripcion: null,
  activo: true,
};

const FINALIDAD_VACIA: FinalidadRequest = {
  codigo: '',
  nombre: '',
  descripcion: '',
  bancoId: null,
  baseLegal: '',
  obligatoria: false,
  categoriaDatos: '',
  plazoRetencionDias: null,
  activa: true,
};

const ENCARGADO_VACIO: EncargadoRequest = {
  razonSocial: '',
  ruc: null,
  servicio: '',
  categoriasDatos: '',
  pais: 'Perú',
  transfronterizo: false,
  contratoVigenteHasta: null,
  contacto: null,
  activo: true,
};

@Component({
  selector: 'app-registro-tratamientos',
  imports: [
    DatePipe,
    FormsModule,
    Button,
    Checkbox,
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
    EmptyState,
    PageHeader,
  ],
  template: `
    <app-page-header
      title="Registro de tratamientos"
      subtitle="Bancos de datos, finalidades y encargados de tratamiento"
      icon="pi-database"
    />

    <section class="panel">
      <p-tabs value="bancos" [scrollable]="true">
        <p-tablist>
          <p-tab value="bancos"><i class="pi pi-database"></i> Bancos de datos</p-tab>
          <p-tab value="finalidades"><i class="pi pi-list"></i> Finalidades</p-tab>
          <p-tab value="encargados"><i class="pi pi-building"></i> Encargados</p-tab>
        </p-tablist>
        <p-tabpanels>
          <p-tabpanel value="bancos">
            <div class="barra">
              <p-button
                label="Nuevo banco de datos"
                icon="pi pi-plus"
                size="small"
                (onClick)="abrirBanco(null)"
              />
            </div>
            <p-table [value]="registro()?.bancos ?? []" responsiveLayout="scroll">
              <ng-template #header>
                <tr>
                  <th>Banco</th>
                  <th>Datos</th>
                  <th>Inscripción</th>
                  <th>Estado</th>
                  <th></th>
                </tr>
              </ng-template>
              <ng-template #body let-b>
                <tr>
                  <td>
                    <strong>{{ b.nombre }}</strong>
                    <small class="block text-muted">{{ b.finalidadPrincipal }}</small>
                  </td>
                  <td>
                    {{ b.categoriasDatos }}
                    @if (b.contieneSensibles) {
                      <p-tag value="Datos sensibles" severity="warn" />
                    }
                  </td>
                  <td>{{ b.numeroInscripcion || 'Sin inscribir' }}</td>
                  <td>
                    <p-tag
                      [value]="b.activo ? 'Activo' : 'Inactivo'"
                      [severity]="b.activo ? 'success' : 'secondary'"
                    />
                  </td>
                  <td class="derecha">
                    <p-button
                      label="Editar"
                      size="small"
                      [outlined]="true"
                      [ariaLabel]="'Editar ' + b.nombre"
                      (onClick)="abrirBanco(b)"
                    />
                  </td>
                </tr>
              </ng-template>
              <ng-template #emptymessage>
                <tr>
                  <td colspan="5">
                    <app-empty-state icon="pi-database" title="Sin bancos de datos" />
                  </td>
                </tr>
              </ng-template>
            </p-table>
          </p-tabpanel>

          <p-tabpanel value="finalidades">
            <div class="barra">
              <p-button
                label="Nueva finalidad"
                icon="pi pi-plus"
                size="small"
                (onClick)="abrirFinalidad(null)"
              />
            </div>
            <p-table [value]="registro()?.finalidades ?? []" responsiveLayout="scroll">
              <ng-template #header>
                <tr>
                  <th>Finalidad</th>
                  <th>Base legal</th>
                  <th>Consentimiento</th>
                  <th>Banco</th>
                  <th></th>
                </tr>
              </ng-template>
              <ng-template #body let-f>
                <tr>
                  <td>
                    <strong>{{ f.nombre }}</strong>
                    <small class="block text-muted">{{ f.categoriaDatos }}</small>
                  </td>
                  <td>{{ f.baseLegal }}</td>
                  <td>
                    <p-tag
                      [value]="f.obligatoria ? 'No requiere' : 'Requiere'"
                      [severity]="f.obligatoria ? 'secondary' : 'info'"
                    />
                    @if (!f.activa) {
                      <p-tag value="Inactiva" severity="secondary" />
                    }
                  </td>
                  <td>{{ f.bancoNombre || '—' }}</td>
                  <td class="derecha">
                    <p-button
                      label="Editar"
                      size="small"
                      [outlined]="true"
                      [ariaLabel]="'Editar ' + f.nombre"
                      (onClick)="abrirFinalidad(f)"
                    />
                  </td>
                </tr>
              </ng-template>
              <ng-template #emptymessage>
                <tr>
                  <td colspan="5"><app-empty-state icon="pi-list" title="Sin finalidades" /></td>
                </tr>
              </ng-template>
            </p-table>
          </p-tabpanel>

          <p-tabpanel value="encargados">
            <div class="barra">
              <p-button
                label="Nuevo encargado"
                icon="pi pi-plus"
                size="small"
                (onClick)="abrirEncargado(null)"
              />
            </div>
            <p-table [value]="registro()?.encargados ?? []" responsiveLayout="scroll">
              <ng-template #header>
                <tr>
                  <th>Encargado</th>
                  <th>Servicio y datos</th>
                  <th>País</th>
                  <th>Contrato hasta</th>
                  <th></th>
                </tr>
              </ng-template>
              <ng-template #body let-e>
                <tr>
                  <td>
                    <strong>{{ e.razonSocial }}</strong>
                    <small class="block text-muted">{{ e.ruc || 'Sin RUC' }}</small>
                  </td>
                  <td>
                    {{ e.servicio }}
                    <small class="block text-muted">{{ e.categoriasDatos }}</small>
                  </td>
                  <td>
                    {{ e.pais }}
                    @if (e.transfronterizo) {
                      <p-tag value="Transferencia al exterior" severity="warn" />
                    }
                  </td>
                  <td [class.vencido]="e.contratoVencido">
                    {{
                      e.contratoVigenteHasta
                        ? (e.contratoVigenteHasta | date: 'd MMM y')
                        : 'Sin fecha'
                    }}
                    @if (e.contratoVencido) {
                      <small class="block">Contrato vencido</small>
                    }
                  </td>
                  <td class="derecha">
                    <p-button
                      label="Editar"
                      size="small"
                      [outlined]="true"
                      [ariaLabel]="'Editar ' + e.razonSocial"
                      (onClick)="abrirEncargado(e)"
                    />
                  </td>
                </tr>
              </ng-template>
              <ng-template #emptymessage>
                <tr>
                  <td colspan="5"><app-empty-state icon="pi-building" title="Sin encargados" /></td>
                </tr>
              </ng-template>
            </p-table>
          </p-tabpanel>
        </p-tabpanels>
      </p-tabs>
    </section>

    <p-dialog
      [header]="titulo()"
      [visible]="entidad() !== null"
      (visibleChange)="$event ? null : entidad.set(null)"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '40rem', maxWidth: '96vw' }"
    >
      <form class="form" (submit)="$event.preventDefault(); guardar()" novalidate>
        @if (error(); as mensaje) {
          <p-message severity="error" styleClass="w-full">{{ mensaje }}</p-message>
        }
        @switch (entidad()) {
          @case ('banco') {
            @for (c of camposBanco; track c.clave) {
              <label [for]="'rt-' + c.clave">{{ c.etiqueta }}</label>
              <input
                pInputText
                [id]="'rt-' + c.clave"
                class="w-full"
                autocomplete="off"
                [ngModel]="valorBanco(c.clave)"
                (ngModelChange)="campoBanco(c.clave, $event)"
                [ngModelOptions]="{ standalone: true }"
              />
            }
            <div class="check">
              <p-checkbox
                inputId="rt-sens"
                [binary]="true"
                [ngModel]="banco().contieneSensibles"
                (ngModelChange)="campoBanco('contieneSensibles', $event)"
                [ngModelOptions]="{ standalone: true }"
              />
              <label for="rt-sens">Contiene datos sensibles, como los de salud</label>
            </div>
            <div class="check">
              <p-checkbox
                inputId="rt-trans"
                [binary]="true"
                [ngModel]="banco().transferenciaTransfronteriza"
                (ngModelChange)="campoBanco('transferenciaTransfronteriza', $event)"
                [ngModelOptions]="{ standalone: true }"
              />
              <label for="rt-trans">Hay transferencia transfronteriza</label>
            </div>
            <div class="check">
              <p-checkbox
                inputId="rt-act"
                [binary]="true"
                [ngModel]="banco().activo"
                (ngModelChange)="campoBanco('activo', $event)"
                [ngModelOptions]="{ standalone: true }"
              />
              <label for="rt-act">Banco activo</label>
            </div>
          }
          @case ('finalidad') {
            @for (c of camposFinalidad; track c.clave) {
              <label [for]="'rf-' + c.clave">{{ c.etiqueta }}</label>
              <input
                pInputText
                [id]="'rf-' + c.clave"
                class="w-full"
                autocomplete="off"
                [ngModel]="valorFinalidad(c.clave)"
                (ngModelChange)="campoFinalidad(c.clave, $event)"
                [ngModelOptions]="{ standalone: true }"
              />
            }
            <label for="rf-banco">Banco de datos</label>
            <p-select
              inputId="rf-banco"
              [options]="registro()?.bancos ?? []"
              optionLabel="nombre"
              optionValue="id"
              [ngModel]="finalidad().bancoId"
              (ngModelChange)="campoFinalidad('bancoId', $event)"
              [ngModelOptions]="{ standalone: true }"
              [showClear]="true"
              appendTo="body"
              styleClass="w-full"
            />
            <div class="check">
              <p-checkbox
                inputId="rf-obl"
                [binary]="true"
                [ngModel]="finalidad().obligatoria"
                (ngModelChange)="campoFinalidad('obligatoria', $event)"
                [ngModelOptions]="{ standalone: true }"
              />
              <label for="rf-obl">Es necesaria para el servicio y no requiere consentimiento</label>
            </div>
            <div class="check">
              <p-checkbox
                inputId="rf-act"
                [binary]="true"
                [ngModel]="finalidad().activa"
                (ngModelChange)="campoFinalidad('activa', $event)"
                [ngModelOptions]="{ standalone: true }"
              />
              <label for="rf-act">Finalidad activa</label>
            </div>
          }
          @case ('encargado') {
            @for (c of camposEncargado; track c.clave) {
              <label [for]="'re-' + c.clave">{{ c.etiqueta }}</label>
              <input
                pInputText
                [id]="'re-' + c.clave"
                class="w-full"
                autocomplete="off"
                [ngModel]="valorEncargado(c.clave)"
                (ngModelChange)="campoEncargado(c.clave, $event)"
                [ngModelOptions]="{ standalone: true }"
              />
            }
            <div class="check">
              <p-checkbox
                inputId="re-trans"
                [binary]="true"
                [ngModel]="encargado().transfronterizo"
                (ngModelChange)="campoEncargado('transfronterizo', $event)"
                [ngModelOptions]="{ standalone: true }"
              />
              <label for="re-trans">Transfiere datos fuera del país</label>
            </div>
            <div class="check">
              <p-checkbox
                inputId="re-act"
                [binary]="true"
                [ngModel]="encargado().activo"
                (ngModelChange)="campoEncargado('activo', $event)"
                [ngModelOptions]="{ standalone: true }"
              />
              <label for="re-act">Encargado activo</label>
            </div>
          }
        }
      </form>
      <ng-template #footer>
        <p-button
          label="Cancelar"
          severity="secondary"
          [outlined]="true"
          (onClick)="entidad.set(null)"
        />
        <p-button
          label="Guardar"
          icon="pi pi-check"
          [loading]="guardando()"
          (onClick)="guardar()"
        />
      </ng-template>
    </p-dialog>
  `,
  styles: `
    .barra {
      display: flex;
      justify-content: flex-end;
      margin: 0.5rem 0 0.75rem;
    }

    .derecha {
      text-align: right;
    }

    .vencido {
      color: var(--p-red-600);
      font-weight: 600;
    }

    .form {
      display: flex;
      flex-direction: column;
      gap: 0.3rem;
      max-height: 65vh;
      overflow: auto;
      padding-right: 0.4rem;
    }

    .form label {
      font-weight: 600;
      font-size: 0.9rem;
      margin-top: 0.3rem;
    }

    .check {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      margin-top: 0.4rem;
    }

    .check label {
      margin: 0;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RegistroTratamientosPagina implements OnInit {
  private readonly api = inject(ProteccionDatosApiService);
  private readonly notificaciones = inject(NotificationService);

  protected readonly camposBanco: { clave: keyof BancoDatosRequest; etiqueta: string }[] = [
    { clave: 'codigo', etiqueta: 'Código' },
    { clave: 'nombre', etiqueta: 'Nombre' },
    { clave: 'descripcion', etiqueta: 'Descripción' },
    { clave: 'finalidadPrincipal', etiqueta: 'Finalidad principal' },
    { clave: 'categoriasDatos', etiqueta: 'Categorías de datos' },
    { clave: 'responsable', etiqueta: 'Responsable del banco' },
    { clave: 'ubicacion', etiqueta: 'Ubicación' },
    { clave: 'medidasSeguridad', etiqueta: 'Medidas de seguridad' },
    { clave: 'numeroInscripcion', etiqueta: 'Número de inscripción en el registro' },
    { clave: 'fechaInscripcion', etiqueta: 'Fecha de inscripción, AAAA-MM-DD' },
  ];
  protected readonly camposFinalidad: { clave: keyof FinalidadRequest; etiqueta: string }[] = [
    { clave: 'codigo', etiqueta: 'Código' },
    { clave: 'nombre', etiqueta: 'Nombre' },
    { clave: 'descripcion', etiqueta: 'Descripción' },
    { clave: 'baseLegal', etiqueta: 'Base legal' },
    { clave: 'categoriaDatos', etiqueta: 'Categoría de datos' },
  ];
  protected readonly camposEncargado: { clave: keyof EncargadoRequest; etiqueta: string }[] = [
    { clave: 'razonSocial', etiqueta: 'Razón social' },
    { clave: 'ruc', etiqueta: 'RUC' },
    { clave: 'servicio', etiqueta: 'Servicio que presta' },
    { clave: 'categoriasDatos', etiqueta: 'Categorías de datos que trata' },
    { clave: 'pais', etiqueta: 'País' },
    { clave: 'contratoVigenteHasta', etiqueta: 'Contrato vigente hasta, AAAA-MM-DD' },
    { clave: 'contacto', etiqueta: 'Contacto' },
  ];

  protected readonly registro = signal<RegistroTratamientos | null>(null);
  protected readonly entidad = signal<Entidad | null>(null);
  protected readonly editandoId = signal<string | null>(null);
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly banco = signal<BancoDatosRequest>({ ...BANCO_VACIO });
  protected readonly finalidad = signal<FinalidadRequest>({ ...FINALIDAD_VACIA });
  protected readonly encargado = signal<EncargadoRequest>({ ...ENCARGADO_VACIO });

  protected titulo(): string {
    const nuevo = this.editandoId() === null;
    switch (this.entidad()) {
      case 'banco':
        return nuevo ? 'Nuevo banco de datos' : 'Editar banco de datos';
      case 'finalidad':
        return nuevo ? 'Nueva finalidad' : 'Editar finalidad';
      case 'encargado':
        return nuevo ? 'Nuevo encargado' : 'Editar encargado';
      default:
        return '';
    }
  }

  ngOnInit(): void {
    this.cargar();
  }

  protected valorBanco(clave: keyof BancoDatosRequest): string {
    return String(this.banco()[clave] ?? '');
  }

  protected valorFinalidad(clave: keyof FinalidadRequest): string {
    return String(this.finalidad()[clave] ?? '');
  }

  protected valorEncargado(clave: keyof EncargadoRequest): string {
    return String(this.encargado()[clave] ?? '');
  }

  protected campoBanco(clave: keyof BancoDatosRequest, valor: unknown): void {
    this.banco.update((b) => ({ ...b, [clave]: valor }));
  }

  protected campoFinalidad(clave: keyof FinalidadRequest, valor: unknown): void {
    this.finalidad.update((f) => ({ ...f, [clave]: valor }));
  }

  protected campoEncargado(clave: keyof EncargadoRequest, valor: unknown): void {
    this.encargado.update((e) => ({ ...e, [clave]: valor }));
  }

  protected abrirBanco(b: BancoDatos | null): void {
    this.error.set(null);
    this.editandoId.set(b?.id ?? null);
    this.banco.set(b ? { ...b } : { ...BANCO_VACIO });
    this.entidad.set('banco');
  }

  protected abrirFinalidad(f: FinalidadTratamiento | null): void {
    this.error.set(null);
    this.editandoId.set(f?.id ?? null);
    this.finalidad.set(f ? { ...f } : { ...FINALIDAD_VACIA });
    this.entidad.set('finalidad');
  }

  protected abrirEncargado(e: EncargadoTratamiento | null): void {
    this.error.set(null);
    this.editandoId.set(e?.id ?? null);
    this.encargado.set(e ? { ...e } : { ...ENCARGADO_VACIO });
    this.entidad.set('encargado');
  }

  protected guardar(): void {
    this.guardando.set(true);
    this.error.set(null);
    const id = this.editandoId();
    const peticion: Observable<unknown> =
      this.entidad() === 'banco'
        ? this.api.guardarBanco(id, this.limpiarBanco())
        : this.entidad() === 'finalidad'
          ? this.api.guardarFinalidad(id, this.limpiarFinalidad())
          : this.api.guardarEncargado(id, this.limpiarEncargado());
    peticion.subscribe({
      next: () => {
        this.guardando.set(false);
        this.entidad.set(null);
        this.notificaciones.success('El registro se guardó.');
        this.cargar();
      },
      error: (e: { message: string }) => {
        this.guardando.set(false);
        this.error.set(e.message);
      },
    });
  }

  private limpiarBanco(): BancoDatosRequest {
    const b = this.banco();
    return {
      codigo: b.codigo.trim(),
      nombre: b.nombre.trim(),
      descripcion: b.descripcion.trim(),
      finalidadPrincipal: b.finalidadPrincipal.trim(),
      categoriasDatos: b.categoriasDatos.trim(),
      contieneSensibles: b.contieneSensibles,
      responsable: b.responsable.trim(),
      ubicacion: b.ubicacion.trim(),
      medidasSeguridad: b.medidasSeguridad.trim(),
      transferenciaTransfronteriza: b.transferenciaTransfronteriza,
      numeroInscripcion: b.numeroInscripcion?.toString().trim() || null,
      fechaInscripcion: b.fechaInscripcion || null,
      activo: b.activo,
    };
  }

  private limpiarFinalidad(): FinalidadRequest {
    const f = this.finalidad();
    const plazo = Number(f.plazoRetencionDias);
    return {
      codigo: f.codigo.trim(),
      nombre: f.nombre.trim(),
      descripcion: f.descripcion.trim(),
      bancoId: f.bancoId || null,
      baseLegal: f.baseLegal.trim(),
      obligatoria: f.obligatoria,
      categoriaDatos: f.categoriaDatos.trim(),
      plazoRetencionDias: plazo > 0 ? plazo : null,
      activa: f.activa,
    };
  }

  private limpiarEncargado(): EncargadoRequest {
    const e = this.encargado();
    return {
      razonSocial: e.razonSocial.trim(),
      ruc: e.ruc?.toString().trim() || null,
      servicio: e.servicio.trim(),
      categoriasDatos: e.categoriasDatos.trim(),
      pais: e.pais.trim(),
      transfronterizo: e.transfronterizo,
      contratoVigenteHasta: e.contratoVigenteHasta || null,
      contacto: e.contacto?.toString().trim() || null,
      activo: e.activo,
    };
  }

  private cargar(): void {
    this.api.registro().subscribe((r) => this.registro.set(r));
  }
}
