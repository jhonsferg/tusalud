import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';
import { Tag } from 'primeng/tag';

import {
  Esterilizador,
  EsterilizacionApiService,
  NotificationService,
  SetInstrumental,
  TipoEsterilizacion,
} from '@tusalud/core';
import { PageHeader } from '@tusalud/ui/page-header';

const ETIQUETA_TIPO: Readonly<Record<TipoEsterilizacion, string>> = {
  VAPOR: 'Vapor',
  PLASMA: 'Plasma',
  OXIDO_ETILENO: 'Óxido de etileno',
};

@Component({
  selector: 'app-catalogo-esterilizacion',
  imports: [FormsModule, Button, InputNumber, InputText, Message, Select, Tag, PageHeader],
  template: `
    <app-page-header
      title="Esterilizadores y sets"
      subtitle="Equipos de esterilización y sets de instrumental con su vigencia de esterilidad"
      icon="pi-box"
    />
    @if (error()) {
      <p-message severity="error">{{ error() }}</p-message>
    }
    <section class="panel" aria-labelledby="cat-esterilizadores">
      <h2 id="cat-esterilizadores">Esterilizadores</h2>
      <div class="tabla">
        <table>
          <caption class="sr-only">
            Esterilizadores registrados
          </caption>
          <thead>
            <tr>
              <th scope="col">Código</th>
              <th scope="col">Nombre</th>
              <th scope="col">Tipo</th>
              <th scope="col">Estado</th>
              <th scope="col"><span class="sr-only">Acciones</span></th>
            </tr>
          </thead>
          <tbody>
            @for (e of esterilizadores(); track e.id) {
              <tr>
                <th scope="row">{{ e.codigo }}</th>
                <td>{{ e.nombre }}</td>
                <td>{{ tipos[e.tipo] }}</td>
                <td>
                  <p-tag
                    [value]="e.activo ? 'Activo' : 'Inactivo'"
                    [severity]="e.activo ? 'success' : 'secondary'"
                  />
                </td>
                <td>
                  <p-button
                    label="Editar"
                    size="small"
                    [text]="true"
                    [ariaLabel]="'Editar ' + e.nombre"
                    (onClick)="editarEsterilizador(e)"
                  />
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
      <form class="formulario" (submit)="$event.preventDefault(); guardarEsterilizador()">
        <div class="campos">
          <label>
            Código
            <input
              pInputText
              name="eCodigo"
              maxlength="20"
              [(ngModel)]="eCodigo"
              [disabled]="!!eEditando()"
              required
            />
          </label>
          <label>
            Nombre
            <input pInputText name="eNombre" maxlength="100" [(ngModel)]="eNombre" required />
          </label>
          <label for="e-tipo">
            Tipo
            <p-select
              inputId="e-tipo"
              name="eTipo"
              [options]="opcionesTipo"
              optionLabel="etiqueta"
              optionValue="valor"
              [(ngModel)]="eTipo"
            />
          </label>
        </div>
        <label class="check"
          ><input type="checkbox" name="eActivo" [(ngModel)]="eActivo" /> Activo</label
        >
        <div class="barra">
          <p-button
            type="submit"
            [label]="eEditando() ? 'Guardar cambios' : 'Crear esterilizador'"
            icon="pi pi-save"
            [disabled]="!eCodigo.trim() || !eNombre.trim()"
          />
          @if (eEditando()) {
            <p-button
              label="Cancelar"
              severity="secondary"
              [outlined]="true"
              (onClick)="limpiarEsterilizador()"
            />
          }
        </div>
      </form>
    </section>

    <section class="panel" aria-labelledby="cat-sets">
      <h2 id="cat-sets">Sets de instrumental</h2>
      <div class="tabla">
        <table>
          <caption class="sr-only">
            Sets de instrumental registrados
          </caption>
          <thead>
            <tr>
              <th scope="col">Código</th>
              <th scope="col">Nombre</th>
              <th scope="col">Vigencia de esterilidad</th>
              <th scope="col">Estado</th>
              <th scope="col"><span class="sr-only">Acciones</span></th>
            </tr>
          </thead>
          <tbody>
            @for (s of sets(); track s.id) {
              <tr>
                <th scope="row">{{ s.codigo }}</th>
                <td>{{ s.nombre }}</td>
                <td>{{ s.vigenciaDias }} días</td>
                <td>
                  <p-tag
                    [value]="s.activo ? 'Activo' : 'Inactivo'"
                    [severity]="s.activo ? 'success' : 'secondary'"
                  />
                </td>
                <td>
                  <p-button
                    label="Editar"
                    size="small"
                    [text]="true"
                    [ariaLabel]="'Editar ' + s.nombre"
                    (onClick)="editarSet(s)"
                  />
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
      <form class="formulario" (submit)="$event.preventDefault(); guardarSet()">
        <div class="campos">
          <label>
            Código
            <input
              pInputText
              name="sCodigo"
              maxlength="20"
              [(ngModel)]="sCodigo"
              [disabled]="!!sEditando()"
              required
            />
          </label>
          <label>
            Nombre
            <input pInputText name="sNombre" maxlength="120" [(ngModel)]="sNombre" required />
          </label>
          <label for="s-vigencia">
            Vigencia en días
            <p-inputnumber
              inputId="s-vigencia"
              name="sVigencia"
              [(ngModel)]="sVigencia"
              [min]="1"
              [max]="365"
            />
          </label>
        </div>
        <label>
          Descripción
          <input pInputText name="sDescripcion" maxlength="300" [(ngModel)]="sDescripcion" />
        </label>
        <label class="check"
          ><input type="checkbox" name="sActivo" [(ngModel)]="sActivo" /> Activo</label
        >
        <div class="barra">
          <p-button
            type="submit"
            [label]="sEditando() ? 'Guardar cambios' : 'Crear set'"
            icon="pi pi-save"
            [disabled]="!sCodigo.trim() || !sNombre.trim() || !sVigencia"
          />
          @if (sEditando()) {
            <p-button
              label="Cancelar"
              severity="secondary"
              [outlined]="true"
              (onClick)="limpiarSet()"
            />
          }
        </div>
      </form>
    </section>
  `,
  styles: `
    .panel {
      margin-bottom: 1rem;
    }

    h2 {
      margin: 0 0 0.75rem;
      font-size: 1.05rem;
    }

    .tabla {
      overflow-x: auto;
      margin-bottom: 0.75rem;
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

    .formulario {
      display: grid;
      gap: 0.75rem;
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

    .check {
      display: flex;
      gap: 0.4rem;
      align-items: center;
      font-weight: 400;
    }

    .barra {
      display: flex;
      gap: 0.5rem;
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
export class CatalogoEsterilizacion implements OnInit {
  private readonly api = inject(EsterilizacionApiService);
  private readonly avisos = inject(NotificationService);

  protected readonly esterilizadores = signal<Esterilizador[]>([]);
  protected readonly sets = signal<SetInstrumental[]>([]);
  protected readonly eEditando = signal<string | null>(null);
  protected readonly sEditando = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly tipos = ETIQUETA_TIPO;
  protected readonly opcionesTipo = (Object.keys(ETIQUETA_TIPO) as TipoEsterilizacion[]).map(
    (valor) => ({ valor, etiqueta: ETIQUETA_TIPO[valor] }),
  );

  protected eCodigo = '';
  protected eNombre = '';
  protected eTipo: TipoEsterilizacion = 'VAPOR';
  protected eActivo = true;
  protected sCodigo = '';
  protected sNombre = '';
  protected sDescripcion = '';
  protected sVigencia: number | null = 30;
  protected sActivo = true;

  ngOnInit(): void {
    this.recargar();
  }

  protected editarEsterilizador(e: Esterilizador): void {
    this.eEditando.set(e.id);
    this.eCodigo = e.codigo;
    this.eNombre = e.nombre;
    this.eTipo = e.tipo;
    this.eActivo = e.activo;
  }

  protected limpiarEsterilizador(): void {
    this.eEditando.set(null);
    this.eCodigo = '';
    this.eNombre = '';
    this.eTipo = 'VAPOR';
    this.eActivo = true;
  }

  protected guardarEsterilizador(): void {
    const request = {
      codigo: this.eCodigo.trim(),
      nombre: this.eNombre.trim(),
      tipo: this.eTipo,
      activo: this.eActivo,
    };
    const id = this.eEditando();
    const llamada = id
      ? this.api.actualizarEsterilizador(id, request)
      : this.api.crearEsterilizador(request);
    llamada.subscribe({
      next: () => {
        this.avisos.success('El esterilizador quedó guardado');
        this.limpiarEsterilizador();
        this.recargar();
      },
      error: (e: { error?: { message?: string } }) =>
        this.error.set(e.error?.message ?? 'No pudimos guardar el esterilizador'),
    });
  }

  protected editarSet(s: SetInstrumental): void {
    this.sEditando.set(s.id);
    this.sCodigo = s.codigo;
    this.sNombre = s.nombre;
    this.sDescripcion = s.descripcion ?? '';
    this.sVigencia = s.vigenciaDias;
    this.sActivo = s.activo;
  }

  protected limpiarSet(): void {
    this.sEditando.set(null);
    this.sCodigo = '';
    this.sNombre = '';
    this.sDescripcion = '';
    this.sVigencia = 30;
    this.sActivo = true;
  }

  protected guardarSet(): void {
    const request = {
      codigo: this.sCodigo.trim(),
      nombre: this.sNombre.trim(),
      descripcion: this.sDescripcion.trim() || null,
      vigenciaDias: this.sVigencia ?? 30,
      activo: this.sActivo,
    };
    const id = this.sEditando();
    const llamada = id ? this.api.actualizarSet(id, request) : this.api.crearSet(request);
    llamada.subscribe({
      next: () => {
        this.avisos.success('El set quedó guardado');
        this.limpiarSet();
        this.recargar();
      },
      error: (e: { error?: { message?: string } }) =>
        this.error.set(e.error?.message ?? 'No pudimos guardar el set'),
    });
  }

  private recargar(): void {
    this.api.esterilizadores().subscribe({
      next: (e) => this.esterilizadores.set(e),
      error: () => this.error.set('No pudimos cargar los esterilizadores'),
    });
    this.api.sets().subscribe({ next: (s) => this.sets.set(s) });
  }
}
