import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';
import { Tag } from 'primeng/tag';

import {
  EstadoPieza,
  NotificationService,
  OdontologiaApiService,
  ProcedimientoOdontologico,
  ProcedimientoOdontologicoRequest,
} from '@tusalud/core';
import { PageHeader } from '@tusalud/ui/page-header';
import { ETIQUETA_ESTADO_PIEZA, soles } from '../odontologia.utils';

@Component({
  selector: 'app-procedimientos-odontologicos',
  imports: [FormsModule, Button, InputNumber, InputText, Message, Select, Tag, PageHeader],
  template: `
    <app-page-header
      title="Procedimientos odontológicos"
      subtitle="Catálogo con precios, estado que deja en el odontograma y exigencia de consentimiento"
      icon="pi-list"
    />
    @if (error()) {
      <p-message severity="error">{{ error() }}</p-message>
    }
    <div class="panel tabla">
      <table>
        <caption class="sr-only">
          Catálogo de procedimientos odontológicos
        </caption>
        <thead>
          <tr>
            <th scope="col">Código</th>
            <th scope="col">Nombre</th>
            <th scope="col">Categoría</th>
            <th scope="col">Precio</th>
            <th scope="col">Deja la pieza como</th>
            <th scope="col">Estado</th>
            <th scope="col"><span class="sr-only">Acciones</span></th>
          </tr>
        </thead>
        <tbody>
          @for (p of procedimientos(); track p.id) {
            <tr>
              <th scope="row">{{ p.codigo }}</th>
              <td>
                {{ p.nombre }}
                @if (p.requiereConsentimiento) {
                  <small class="text-muted block">Requiere consentimiento</small>
                }
              </td>
              <td>{{ p.categoria }}</td>
              <td>{{ soles(p.precio) }}</td>
              <td>{{ p.estadoResultante ? estados[p.estadoResultante] : '—' }}</td>
              <td>
                <p-tag
                  [value]="p.activo ? 'Activo' : 'Inactivo'"
                  [severity]="p.activo ? 'success' : 'secondary'"
                />
              </td>
              <td>
                <p-button
                  label="Editar"
                  size="small"
                  [text]="true"
                  [ariaLabel]="'Editar ' + p.nombre"
                  (onClick)="editar(p)"
                />
              </td>
            </tr>
          }
        </tbody>
      </table>
    </div>
    <form class="panel formulario" (submit)="$event.preventDefault(); guardar()">
      <h3>{{ editandoId() ? 'Editar procedimiento' : 'Nuevo procedimiento' }}</h3>
      <div class="campos">
        <label>
          Código
          <input
            pInputText
            name="codigo"
            maxlength="20"
            [(ngModel)]="codigo"
            [disabled]="!!editandoId()"
            required
          />
        </label>
        <label>
          Nombre
          <input pInputText name="nombre" maxlength="120" [(ngModel)]="nombre" required />
        </label>
        <label>
          Categoría
          <input pInputText name="categoria" maxlength="40" [(ngModel)]="categoria" required />
        </label>
        <label for="proc-precio">
          Precio en soles
          <p-inputnumber
            inputId="proc-precio"
            name="precio"
            [(ngModel)]="precio"
            mode="decimal"
            [minFractionDigits]="2"
            [maxFractionDigits]="2"
            [min]="0"
          />
        </label>
        <label for="proc-estado">
          Deja la pieza como
          <p-select
            inputId="proc-estado"
            name="estado"
            [options]="opcionesEstado"
            optionLabel="etiqueta"
            optionValue="valor"
            [(ngModel)]="estadoResultante"
          />
        </label>
      </div>
      <div class="marcas">
        <label class="check"
          ><input type="checkbox" name="pieza" [(ngModel)]="requierePieza" /> Requiere pieza</label
        >
        <label class="check"
          ><input type="checkbox" name="superficie" [(ngModel)]="requiereSuperficie" /> Requiere
          superficie</label
        >
        <label class="check"
          ><input type="checkbox" name="consentimiento" [(ngModel)]="requiereConsentimiento" />
          Requiere consentimiento</label
        >
        <label class="check"
          ><input type="checkbox" name="activo" [(ngModel)]="activo" /> Activo</label
        >
      </div>
      <div class="barra">
        <p-button type="submit" label="Guardar" icon="pi pi-check" [loading]="guardando()" />
        @if (editandoId()) {
          <p-button
            label="Cancelar edición"
            severity="secondary"
            [text]="true"
            (onClick)="nuevo()"
          />
        }
      </div>
    </form>
  `,
  styles: `
    .panel {
      margin-bottom: 1rem;
    }

    .tabla {
      overflow-x: auto;
    }

    table {
      width: 100%;
      border-collapse: collapse;
      font-size: 0.92rem;
    }

    th,
    td {
      text-align: left;
      padding: 0.45rem 0.6rem;
      border-bottom: 1px solid var(--p-surface-200, #e2e8f0);
    }

    .block {
      display: block;
    }

    .formulario {
      display: grid;
      gap: 0.75rem;
    }

    h3 {
      margin: 0;
    }

    .campos {
      display: grid;
      gap: 0.75rem;
      grid-template-columns: repeat(auto-fit, minmax(13rem, 1fr));
    }

    label {
      display: grid;
      gap: 0.25rem;
      font-weight: 600;
      font-size: 0.85rem;
    }

    .marcas,
    .barra {
      display: flex;
      gap: 1rem;
      flex-wrap: wrap;
    }

    .check {
      display: flex;
      gap: 0.4rem;
      align-items: center;
      font-weight: 400;
    }

    .sr-only {
      position: absolute;
      width: 1px;
      height: 1px;
      overflow: hidden;
      clip: rect(0 0 0 0);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProcedimientosOdontologicos implements OnInit {
  private readonly api = inject(OdontologiaApiService);
  private readonly avisos = inject(NotificationService);

  protected readonly procedimientos = signal<ProcedimientoOdontologico[]>([]);
  protected readonly editandoId = signal<string | null>(null);
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly estados = ETIQUETA_ESTADO_PIEZA;
  protected readonly soles = soles;
  protected readonly opcionesEstado: { valor: EstadoPieza | null; etiqueta: string }[] = [
    { valor: null, etiqueta: 'No cambia el odontograma' },
    ...(Object.keys(ETIQUETA_ESTADO_PIEZA) as EstadoPieza[]).map((valor) => ({
      valor,
      etiqueta: ETIQUETA_ESTADO_PIEZA[valor],
    })),
  ];

  protected codigo = '';
  protected nombre = '';
  protected categoria = '';
  protected precio: number | null = 0;
  protected estadoResultante: EstadoPieza | null = null;
  protected requierePieza = true;
  protected requiereSuperficie = false;
  protected requiereConsentimiento = false;
  protected activo = true;

  ngOnInit(): void {
    this.recargar();
  }

  protected editar(p: ProcedimientoOdontologico): void {
    this.editandoId.set(p.id);
    this.codigo = p.codigo;
    this.nombre = p.nombre;
    this.categoria = p.categoria;
    this.precio = p.precio;
    this.estadoResultante = p.estadoResultante;
    this.requierePieza = p.requierePieza;
    this.requiereSuperficie = p.requiereSuperficie;
    this.requiereConsentimiento = p.requiereConsentimiento;
    this.activo = p.activo;
  }

  protected nuevo(): void {
    this.editandoId.set(null);
    this.codigo = '';
    this.nombre = '';
    this.categoria = '';
    this.precio = 0;
    this.estadoResultante = null;
    this.requierePieza = true;
    this.requiereSuperficie = false;
    this.requiereConsentimiento = false;
    this.activo = true;
  }

  protected guardar(): void {
    if (
      !this.codigo.trim() ||
      !this.nombre.trim() ||
      !this.categoria.trim() ||
      this.precio === null
    ) {
      return;
    }
    const request: ProcedimientoOdontologicoRequest = {
      codigo: this.codigo.trim(),
      nombre: this.nombre.trim(),
      categoria: this.categoria.trim(),
      precio: this.precio,
      requierePieza: this.requierePieza,
      requiereSuperficie: this.requiereSuperficie,
      estadoResultante: this.estadoResultante,
      requiereConsentimiento: this.requiereConsentimiento,
      activo: this.activo,
    };
    const id = this.editandoId();
    this.guardando.set(true);
    (id
      ? this.api.actualizarProcedimiento(id, request)
      : this.api.crearProcedimiento(request)
    ).subscribe({
      next: () => {
        this.guardando.set(false);
        this.avisos.success('Procedimiento guardado');
        this.nuevo();
        this.recargar();
      },
      error: () => this.guardando.set(false),
    });
  }

  private recargar(): void {
    this.api.procedimientos(false).subscribe({
      next: (lista) => this.procedimientos.set(lista),
      error: () => this.error.set('No se pudo cargar el catálogo de procedimientos'),
    });
  }
}
