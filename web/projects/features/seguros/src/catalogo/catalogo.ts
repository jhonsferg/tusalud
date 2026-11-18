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
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { ToggleSwitch } from 'primeng/toggleswitch';

import {
  AseguradoraResponse,
  AuthService,
  CATEGORIAS_SERVICIO,
  CategoriaServicio,
  ConvenioResponse,
  ETIQUETA_CATEGORIA_SERVICIO,
  ETIQUETA_TIPO_ASEGURADORA,
  NotificationService,
  SegurosStore,
  TIPOS_ASEGURADORA,
  TipoAseguradora,
  TipoCobertura,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { hoyIso, textoCobertura } from '../seguros.utils';

interface ReglaEditable {
  categoria: CategoriaServicio;
  activa: boolean;
  tipo: TipoCobertura;
  valor: number;
  tope: number | null;
  preautorizacion: boolean;
}

@Component({
  selector: 'app-seguros-catalogo',
  imports: [
    CurrencyPipe,
    DatePipe,
    FormsModule,
    Button,
    Dialog,
    InputNumber,
    InputText,
    Message,
    Select,
    TableModule,
    Tag,
    ToggleSwitch,
    EmptyState,
    PageHeader,
  ],
  template: `
    <app-page-header
      title="Aseguradoras y convenios"
      subtitle="Cobertura por categoría, topes y preautorización"
      icon="pi-shield"
    >
      @if (puedeEditar()) {
        <p-button
          label="Nueva aseguradora"
          icon="pi pi-plus"
          [outlined]="true"
          (onClick)="abrirAseguradora(null)"
        />
        <p-button label="Nuevo convenio" icon="pi pi-plus" (onClick)="abrirConvenio(null)" />
      }
    </app-page-header>

    <section class="panel">
      <h3>Aseguradoras</h3>
      <p-table [value]="store.aseguradoras.value() ?? []" [loading]="store.aseguradoras.loading()">
        <ng-template #header>
          <tr>
            <th>Código</th>
            <th>Nombre</th>
            <th>Tipo</th>
            <th>RUC</th>
            <th>Estado</th>
            <th style="width: 5rem"></th>
          </tr>
        </ng-template>
        <ng-template #body let-a>
          <tr>
            <td class="font-semibold">{{ a.codigo }}</td>
            <td>
              {{ a.nombre }}
              @if (a.contacto) {
                <small class="text-muted block">{{ a.contacto }}</small>
              }
            </td>
            <td>{{ etiquetaTipo[a.tipo] }}</td>
            <td>{{ a.ruc || '—' }}</td>
            <td>
              <p-tag
                [value]="a.activa ? 'Activa' : 'Inactiva'"
                [severity]="a.activa ? 'success' : 'secondary'"
              />
            </td>
            <td class="text-right">
              @if (puedeEditar()) {
                <p-button
                  icon="pi pi-pencil"
                  [text]="true"
                  [rounded]="true"
                  severity="secondary"
                  [ariaLabel]="'Editar ' + a.nombre"
                  (onClick)="abrirAseguradora(a)"
                />
              }
            </td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr>
            <td colspan="6">
              <app-empty-state
                icon="pi-shield"
                title="Sin aseguradoras"
                message="Registra la primera aseguradora."
              />
            </td>
          </tr>
        </ng-template>
      </p-table>
    </section>

    <section class="panel">
      <h3>Convenios</h3>
      @for (c of store.convenios.value() ?? []; track c.id) {
        <article class="convenio" [attr.aria-label]="c.nombre">
          <header>
            <div>
              <strong>{{ c.nombre }}</strong>
              <small class="text-muted block">
                {{ c.aseguradoraNombre }} · {{ c.vigenciaDesde | date: 'd MMM y' }} al
                {{ c.vigenciaHasta | date: 'd MMM y' }}
                @if (c.topeAnual) {
                  · Tope anual {{ c.topeAnual | currency: 'PEN' : 'symbol-narrow' }}
                }
              </small>
            </div>
            <span class="acciones">
              <p-tag
                [value]="c.vigente ? 'Vigente' : c.activo ? 'Fuera de vigencia' : 'Inactivo'"
                [severity]="c.vigente ? 'success' : 'secondary'"
              />
              @if (puedeEditar()) {
                <p-button
                  icon="pi pi-pencil"
                  [text]="true"
                  [rounded]="true"
                  severity="secondary"
                  [ariaLabel]="'Editar ' + c.nombre"
                  (onClick)="abrirConvenio(c)"
                />
              }
            </span>
          </header>
          <ul class="reglas">
            @for (r of c.reglas; track r.categoria) {
              <li>
                <strong>{{ etiquetaCategoria[r.categoria] }}</strong>
                <span>{{ texto(r.tipo, r.valor) }}</span>
                @if (r.topePorServicio) {
                  <small
                    >Tope por servicio
                    {{ r.topePorServicio | currency: 'PEN' : 'symbol-narrow' }}</small
                  >
                }
                @if (r.requierePreautorizacion) {
                  <p-tag value="Preautorización" severity="warn" />
                }
              </li>
            }
          </ul>
          @if (c.exclusiones.length > 0) {
            <small class="text-muted">Exclusiones: {{ c.exclusiones.join(', ') }}</small>
          }
        </article>
      } @empty {
        <app-empty-state
          icon="pi-file"
          title="Sin convenios"
          message="Define un convenio con sus reglas de cobertura."
        />
      }
    </section>

    <p-dialog
      [header]="aseguradoraEditada() ? 'Editar aseguradora' : 'Nueva aseguradora'"
      [visible]="dialogoAseguradora()"
      (visibleChange)="dialogoAseguradora.set($event)"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '32rem', maxWidth: '96vw' }"
    >
      <div class="form">
        @if (error(); as mensaje) {
          <p-message severity="error" styleClass="w-full">{{ mensaje }}</p-message>
        }
        <label
          >Código
          <input
            pInputText
            id="seg-codigo"
            class="w-full"
            [ngModel]="codigo()"
            (ngModelChange)="codigo.set($event)"
            maxlength="20"
        /></label>
        <label
          >Nombre
          <input
            pInputText
            id="seg-nombre"
            class="w-full"
            [ngModel]="nombre()"
            (ngModelChange)="nombre.set($event)"
            maxlength="120"
        /></label>
        <label>
          Tipo
          <p-select
            inputId="seg-tipo"
            [options]="tipos"
            optionLabel="label"
            optionValue="value"
            [ngModel]="tipo()"
            (ngModelChange)="tipo.set($event)"
            appendTo="body"
            styleClass="w-full"
          />
        </label>
        <label
          >RUC
          <input
            pInputText
            id="seg-ruc"
            class="w-full"
            [ngModel]="ruc()"
            (ngModelChange)="ruc.set($event)"
            maxlength="11"
            inputmode="numeric"
        /></label>
        <label
          >Contacto
          <input
            pInputText
            id="seg-contacto"
            class="w-full"
            [ngModel]="contacto()"
            (ngModelChange)="contacto.set($event)"
            maxlength="150"
        /></label>
        <label class="check"
          ><p-toggleswitch
            [ngModel]="activa()"
            (ngModelChange)="activa.set($event)"
            ariaLabel="Aseguradora activa"
          />
          Aseguradora activa</label
        >
      </div>
      <ng-template #footer>
        <p-button
          label="Cancelar"
          severity="secondary"
          [outlined]="true"
          (onClick)="dialogoAseguradora.set(false)"
        />
        <p-button
          label="Guardar"
          icon="pi pi-check"
          [loading]="guardando()"
          (onClick)="guardarAseguradora()"
        />
      </ng-template>
    </p-dialog>

    <p-dialog
      [header]="convenioEditado() ? 'Editar convenio' : 'Nuevo convenio'"
      [visible]="dialogoConvenio()"
      (visibleChange)="dialogoConvenio.set($event)"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '56rem', maxWidth: '96vw' }"
    >
      <div class="form">
        @if (error(); as mensaje) {
          <p-message severity="error" styleClass="w-full">{{ mensaje }}</p-message>
        }
        <div class="dos">
          <label>
            Aseguradora
            <p-select
              inputId="con-aseguradora"
              [options]="opcionesAseguradora()"
              optionLabel="label"
              optionValue="value"
              [ngModel]="convenioAseguradora()"
              (ngModelChange)="convenioAseguradora.set($event)"
              [disabled]="convenioEditado() !== null"
              placeholder="Selecciona"
              appendTo="body"
              styleClass="w-full"
            />
          </label>
          <label
            >Nombre del convenio
            <input
              pInputText
              id="con-nombre"
              class="w-full"
              [ngModel]="convenioNombre()"
              (ngModelChange)="convenioNombre.set($event)"
              maxlength="120"
          /></label>
        </div>
        <div class="tres">
          <label
            >Vigente desde
            <input
              pInputText
              id="con-desde"
              type="date"
              class="w-full"
              [ngModel]="desde()"
              (ngModelChange)="desde.set($event)"
          /></label>
          <label
            >Vigente hasta
            <input
              pInputText
              id="con-hasta"
              type="date"
              class="w-full"
              [ngModel]="hasta()"
              (ngModelChange)="hasta.set($event)"
          /></label>
          <label>
            Tope anual por asegurado
            <p-inputnumber
              inputId="con-tope"
              mode="currency"
              currency="PEN"
              locale="es-PE"
              [min]="0"
              [ngModel]="topeAnual()"
              (ngModelChange)="topeAnual.set($event)"
              [fluid]="true"
              placeholder="Sin tope"
            />
          </label>
        </div>
        <table class="editor" aria-label="Reglas de cobertura por categoría">
          <thead>
            <tr>
              <th>Incluir</th>
              <th>Categoría</th>
              <th>Cobertura</th>
              <th>Valor</th>
              <th>Tope por servicio</th>
              <th>Preautorización</th>
            </tr>
          </thead>
          <tbody>
            @for (r of reglas(); track r.categoria; let i = $index) {
              <tr>
                <td>
                  <p-toggleswitch
                    [ngModel]="r.activa"
                    (ngModelChange)="cambiarRegla(i, { activa: $event })"
                    [ariaLabel]="'Incluir ' + etiquetaCategoria[r.categoria]"
                  />
                </td>
                <td>{{ etiquetaCategoria[r.categoria] }}</td>
                <td>
                  <p-select
                    [options]="coberturas"
                    optionLabel="label"
                    optionValue="value"
                    [ngModel]="r.tipo"
                    (ngModelChange)="cambiarRegla(i, { tipo: $event })"
                    [disabled]="!r.activa"
                    appendTo="body"
                    [ariaLabel]="'Tipo de cobertura de ' + etiquetaCategoria[r.categoria]"
                  />
                </td>
                <td>
                  <p-inputnumber
                    [ngModel]="r.valor"
                    (ngModelChange)="cambiarRegla(i, { valor: $event ?? 0 })"
                    [min]="0"
                    [max]="r.tipo === 'PORCENTAJE' ? 100 : 99999"
                    [suffix]="r.tipo === 'PORCENTAJE' ? ' %' : ''"
                    [prefix]="r.tipo === 'COPAGO_FIJO' ? 'S/ ' : ''"
                    [disabled]="!r.activa"
                    [inputId]="'con-valor-' + r.categoria"
                  />
                </td>
                <td>
                  <p-inputnumber
                    [ngModel]="r.tope"
                    (ngModelChange)="cambiarRegla(i, { tope: $event })"
                    [min]="0"
                    prefix="S/ "
                    [disabled]="!r.activa"
                    placeholder="Sin tope"
                  />
                </td>
                <td>
                  <p-toggleswitch
                    [ngModel]="r.preautorizacion"
                    (ngModelChange)="cambiarRegla(i, { preautorizacion: $event })"
                    [disabled]="!r.activa"
                    [ariaLabel]="'Exige preautorización en ' + etiquetaCategoria[r.categoria]"
                  />
                </td>
              </tr>
            }
          </tbody>
        </table>
        <label>
          Exclusiones por código de servicio
          <input
            pInputText
            id="con-exclusiones"
            class="w-full"
            [ngModel]="exclusiones()"
            (ngModelChange)="exclusiones.set($event)"
            placeholder="ECO-MAMA, RM-ENCEFALO"
          />
          <small class="text-muted"
            >Separa los códigos con comas. Esos servicios no se cubren.</small
          >
        </label>
        <label class="check"
          ><p-toggleswitch
            [ngModel]="convenioActivo()"
            (ngModelChange)="convenioActivo.set($event)"
            ariaLabel="Convenio activo"
          />
          Convenio activo</label
        >
      </div>
      <ng-template #footer>
        <p-button
          label="Cancelar"
          severity="secondary"
          [outlined]="true"
          (onClick)="dialogoConvenio.set(false)"
        />
        <p-button
          label="Guardar convenio"
          icon="pi pi-check"
          [loading]="guardando()"
          (onClick)="guardarConvenio()"
        />
      </ng-template>
    </p-dialog>
  `,
  styles: `
    h3 {
      margin: 0 0 var(--sp-3);
      font-size: 1rem;
    }
    .panel + .panel {
      margin-top: var(--sp-4);
    }
    .convenio {
      padding: var(--sp-3) 0;
      border-top: 1px solid var(--p-content-border-color);
    }
    .convenio:first-of-type {
      border-top: 0;
    }
    .convenio header {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: var(--sp-3);
    }
    .acciones {
      display: inline-flex;
      align-items: center;
      gap: var(--sp-2);
    }
    .reglas {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(15rem, 1fr));
      gap: var(--sp-2);
      margin: var(--sp-3) 0;
      padding: 0;
      list-style: none;
    }
    .reglas li {
      display: flex;
      flex-direction: column;
      gap: 0.15rem;
      padding: var(--sp-2) var(--sp-3);
      border-radius: var(--radio-sm);
      background: var(--p-content-hover-background);
    }
    .form {
      display: flex;
      flex-direction: column;
      gap: var(--sp-3);
    }
    .form label {
      display: flex;
      flex-direction: column;
      gap: 0.35rem;
      font-size: 0.9rem;
      font-weight: 600;
    }
    .form label.check {
      flex-direction: row;
      align-items: center;
      font-weight: 500;
    }
    .dos {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: var(--sp-3);
    }
    .tres {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: var(--sp-3);
    }
    .editor {
      width: 100%;
      border-collapse: collapse;
    }
    .editor th {
      padding: 0.4rem;
      text-align: left;
      font-size: 0.78rem;
      color: var(--p-text-muted-color);
    }
    .editor td {
      padding: 0.35rem 0.4rem;
      border-top: 1px solid var(--p-content-border-color);
    }
    @media (max-width: 720px) {
      .dos,
      .tres {
        grid-template-columns: 1fr;
      }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Catalogo implements OnInit {
  protected readonly store = inject(SegurosStore);
  private readonly auth = inject(AuthService);
  private readonly notifications = inject(NotificationService);

  protected readonly puedeEditar = computed(() =>
    this.auth.hasAnyRole(['ROLE_FINANZAS', 'ROLE_ADMIN']),
  );
  protected readonly etiquetaTipo: Readonly<Record<string, string>> = ETIQUETA_TIPO_ASEGURADORA;
  protected readonly etiquetaCategoria: Readonly<Record<string, string>> =
    ETIQUETA_CATEGORIA_SERVICIO;
  protected readonly tipos = TIPOS_ASEGURADORA.map((valor) => ({
    label: ETIQUETA_TIPO_ASEGURADORA[valor],
    value: valor,
  }));
  protected readonly coberturas = [
    { label: 'Porcentaje', value: 'PORCENTAJE' },
    { label: 'Copago fijo', value: 'COPAGO_FIJO' },
  ];
  protected readonly texto = textoCobertura;

  protected readonly dialogoAseguradora = signal(false);
  protected readonly aseguradoraEditada = signal<AseguradoraResponse | null>(null);
  protected readonly codigo = signal('');
  protected readonly nombre = signal('');
  protected readonly tipo = signal<TipoAseguradora>('PRIVADO');
  protected readonly ruc = signal('');
  protected readonly contacto = signal('');
  protected readonly activa = signal(true);

  protected readonly dialogoConvenio = signal(false);
  protected readonly convenioEditado = signal<ConvenioResponse | null>(null);
  protected readonly convenioAseguradora = signal<string | null>(null);
  protected readonly convenioNombre = signal('');
  protected readonly desde = signal(hoyIso());
  protected readonly hasta = signal(hoyIso(365));
  protected readonly topeAnual = signal<number | null>(null);
  protected readonly exclusiones = signal('');
  protected readonly convenioActivo = signal(true);
  protected readonly reglas = signal<ReglaEditable[]>([]);

  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly opcionesAseguradora = computed(() =>
    (this.store.aseguradoras.value() ?? [])
      .filter((a) => a.activa)
      .map((a) => ({ label: a.nombre, value: a.id })),
  );

  ngOnInit(): void {
    this.store.cargarCatalogo();
  }

  protected abrirAseguradora(aseguradora: AseguradoraResponse | null): void {
    this.aseguradoraEditada.set(aseguradora);
    this.codigo.set(aseguradora?.codigo ?? '');
    this.nombre.set(aseguradora?.nombre ?? '');
    this.tipo.set(aseguradora?.tipo ?? 'PRIVADO');
    this.ruc.set(aseguradora?.ruc ?? '');
    this.contacto.set(aseguradora?.contacto ?? '');
    this.activa.set(aseguradora?.activa ?? true);
    this.error.set(null);
    this.dialogoAseguradora.set(true);
  }

  protected guardarAseguradora(): void {
    this.error.set(null);
    if (!this.codigo().trim() || !this.nombre().trim()) {
      this.error.set('Indica el código y el nombre de la aseguradora.');
      return;
    }
    this.guardando.set(true);
    this.store
      .guardarAseguradora(this.aseguradoraEditada()?.id ?? null, {
        codigo: this.codigo().trim(),
        nombre: this.nombre().trim(),
        tipo: this.tipo(),
        ruc: this.ruc().trim() || null,
        contacto: this.contacto().trim() || null,
        activa: this.activa(),
      })
      .subscribe({
        next: (guardada) => {
          this.guardando.set(false);
          this.dialogoAseguradora.set(false);
          this.notifications.success(`La aseguradora ${guardada.nombre} fue guardada.`);
        },
        error: (error: { message: string }) => {
          this.guardando.set(false);
          this.error.set(error.message);
        },
      });
  }

  protected abrirConvenio(convenio: ConvenioResponse | null): void {
    this.convenioEditado.set(convenio);
    this.convenioAseguradora.set(convenio?.aseguradoraId ?? null);
    this.convenioNombre.set(convenio?.nombre ?? '');
    this.desde.set(convenio?.vigenciaDesde ?? hoyIso());
    this.hasta.set(convenio?.vigenciaHasta ?? hoyIso(365));
    this.topeAnual.set(convenio?.topeAnual ?? null);
    this.exclusiones.set((convenio?.exclusiones ?? []).join(', '));
    this.convenioActivo.set(convenio?.activo ?? true);
    this.reglas.set(
      CATEGORIAS_SERVICIO.map((categoria): ReglaEditable => {
        const existente = convenio?.reglas.find((r) => r.categoria === categoria);
        return {
          categoria,
          activa: existente !== undefined || (!convenio && categoria === 'CONSULTA'),
          tipo: existente?.tipo ?? 'PORCENTAJE',
          valor: existente?.valor ?? 80,
          tope: existente?.topePorServicio ?? null,
          preautorizacion: existente?.requierePreautorizacion ?? false,
        };
      }),
    );
    this.error.set(null);
    this.dialogoConvenio.set(true);
  }

  protected cambiarRegla(indice: number, cambio: Partial<ReglaEditable>): void {
    this.reglas.update((reglas) => reglas.map((r, i) => (i === indice ? { ...r, ...cambio } : r)));
  }

  protected guardarConvenio(): void {
    this.error.set(null);
    const aseguradoraId = this.convenioAseguradora();
    const activas = this.reglas().filter((r) => r.activa);
    if (!aseguradoraId || !this.convenioNombre().trim()) {
      this.error.set('Elige la aseguradora e indica el nombre del convenio.');
      return;
    }
    if (activas.length === 0) {
      this.error.set('Incluye al menos una categoría de cobertura.');
      return;
    }
    this.guardando.set(true);
    this.store
      .guardarConvenio(this.convenioEditado()?.id ?? null, {
        aseguradoraId,
        nombre: this.convenioNombre().trim(),
        vigenciaDesde: this.desde(),
        vigenciaHasta: this.hasta(),
        topeAnual: this.topeAnual(),
        activo: this.convenioActivo(),
        reglas: activas.map((r) => ({
          categoria: r.categoria,
          tipo: r.tipo,
          valor: r.valor,
          topePorServicio: r.tope,
          requierePreautorizacion: r.preautorizacion,
        })),
        exclusiones: this.exclusiones()
          .split(',')
          .map((codigo) => codigo.trim())
          .filter((codigo) => codigo.length > 0),
      })
      .subscribe({
        next: (guardado) => {
          this.guardando.set(false);
          this.dialogoConvenio.set(false);
          this.notifications.success(`El convenio ${guardado.nombre} fue guardado.`);
        },
        error: (error: { message: string }) => {
          this.guardando.set(false);
          this.error.set(error.message);
        },
      });
  }
}
