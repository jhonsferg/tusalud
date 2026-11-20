import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';
import { Tag } from 'primeng/tag';

import {
  EstadoQuirofano,
  NotificationService,
  Quirofano,
  QuirofanoApiService,
} from '@tusalud/core';
import { PageHeader } from '@tusalud/ui/page-header';
import { ETIQUETA_QUIROFANO } from '../quirofano.utils';

@Component({
  selector: 'app-quirofanos-admin',
  imports: [FormsModule, Button, InputText, Message, Select, Tag, PageHeader],
  template: `
    <app-page-header
      title="Quirófanos y equipos"
      subtitle="Catálogo de salas quirúrgicas y de su equipamiento"
      icon="pi-building"
    />
    @if (error()) {
      <p-message severity="error">{{ error() }}</p-message>
    }
    <div class="panel tabla">
      <table>
        <caption class="sr-only">
          Quirófanos registrados
        </caption>
        <thead>
          <tr>
            <th scope="col">Código</th>
            <th scope="col">Nombre</th>
            <th scope="col">Ubicación</th>
            <th scope="col">Estado</th>
            <th scope="col">Equipos</th>
            <th scope="col"><span class="sr-only">Acciones</span></th>
          </tr>
        </thead>
        <tbody>
          @for (q of quirofanos(); track q.id) {
            <tr>
              <th scope="row">{{ q.codigo }}</th>
              <td>{{ q.nombre }}</td>
              <td>{{ q.ubicacion ?? '—' }}</td>
              <td>
                <p-tag
                  [value]="estados[q.estado]"
                  [severity]="q.estado === 'DISPONIBLE' ? 'success' : 'warn'"
                />
                @if (!q.activo) {
                  <small class="block text-muted">Inactivo</small>
                }
              </td>
              <td>{{ q.equipos.length }}</td>
              <td>
                <p-button
                  label="Editar"
                  size="small"
                  [text]="true"
                  [ariaLabel]="'Editar ' + q.nombre"
                  (onClick)="editar(q)"
                />
              </td>
            </tr>
          }
        </tbody>
      </table>
    </div>
    <form class="panel formulario" (submit)="$event.preventDefault(); guardar()">
      <h2>{{ editandoId() ? 'Editar quirófano' : 'Nuevo quirófano' }}</h2>
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
          <input pInputText name="nombre" maxlength="100" [(ngModel)]="nombre" required />
        </label>
        <label>
          Ubicación
          <input pInputText name="ubicacion" maxlength="150" [(ngModel)]="ubicacion" />
        </label>
        <label for="qx-estado">
          Estado
          <p-select
            inputId="qx-estado"
            name="estado"
            [options]="opcionesEstado"
            optionLabel="etiqueta"
            optionValue="valor"
            [(ngModel)]="estado"
          />
        </label>
      </div>
      <label>
        Observaciones
        <input pInputText name="observaciones" maxlength="300" [(ngModel)]="observaciones" />
      </label>
      <fieldset>
        <legend>Equipos de la sala</legend>
        @for (e of equipos(); track $index) {
          <div class="fila">
            <input
              pInputText
              [name]="'eq-nombre-' + $index"
              maxlength="100"
              [ngModel]="e.nombre"
              (ngModelChange)="cambiarEquipo($index, 'nombre', $event)"
              [attr.aria-label]="'Nombre del equipo ' + ($index + 1)"
              placeholder="Nombre"
            />
            <input
              pInputText
              [name]="'eq-serie-' + $index"
              maxlength="60"
              [ngModel]="e.serie"
              (ngModelChange)="cambiarEquipo($index, 'serie', $event)"
              [attr.aria-label]="'Serie del equipo ' + ($index + 1)"
              placeholder="Serie"
            />
            <p-button
              icon="pi pi-trash"
              severity="danger"
              [text]="true"
              ariaLabel="Quitar equipo"
              (onClick)="quitarEquipo($index)"
            />
          </div>
        }
        <p-button
          label="Agregar equipo"
          icon="pi pi-plus"
          [text]="true"
          (onClick)="agregarEquipo()"
        />
      </fieldset>
      <label class="check"
        ><input type="checkbox" name="activo" [(ngModel)]="activo" /> Activo</label
      >
      <div class="barra">
        <p-button
          type="submit"
          [label]="editandoId() ? 'Guardar cambios' : 'Crear quirófano'"
          icon="pi pi-save"
          [loading]="guardando()"
          [disabled]="!codigo.trim() || !nombre.trim()"
        />
        @if (editandoId()) {
          <p-button label="Cancelar" severity="secondary" [outlined]="true" (onClick)="limpiar()" />
        }
      </div>
    </form>
  `,
  styles: `
    .panel {
      margin-bottom: 1rem;
    }

    h2 {
      margin: 0;
      font-size: 1.05rem;
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

    fieldset {
      border: 1px solid var(--p-surface-200, #e2e8f0);
      border-radius: 0.5rem;
      padding: 0.75rem;
      display: grid;
      gap: 0.5rem;
    }

    .fila,
    .barra {
      display: flex;
      gap: 0.5rem;
      flex-wrap: wrap;
      align-items: center;
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
export class QuirofanosAdmin implements OnInit {
  private readonly api = inject(QuirofanoApiService);
  private readonly avisos = inject(NotificationService);

  protected readonly quirofanos = signal<Quirofano[]>([]);
  protected readonly equipos = signal<{ nombre: string; serie: string }[]>([]);
  protected readonly editandoId = signal<string | null>(null);
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly estados = ETIQUETA_QUIROFANO;
  protected readonly opcionesEstado: { valor: EstadoQuirofano; etiqueta: string }[] = (
    Object.keys(ETIQUETA_QUIROFANO) as EstadoQuirofano[]
  ).map((valor) => ({ valor, etiqueta: ETIQUETA_QUIROFANO[valor] }));

  protected codigo = '';
  protected nombre = '';
  protected ubicacion = '';
  protected estado: EstadoQuirofano = 'DISPONIBLE';
  protected observaciones = '';
  protected activo = true;

  ngOnInit(): void {
    this.recargar();
  }

  protected editar(q: Quirofano): void {
    this.editandoId.set(q.id);
    this.codigo = q.codigo;
    this.nombre = q.nombre;
    this.ubicacion = q.ubicacion ?? '';
    this.estado = q.estado;
    this.observaciones = q.observaciones ?? '';
    this.activo = q.activo;
    this.equipos.set(q.equipos.map((e) => ({ nombre: e.nombre, serie: e.serie ?? '' })));
  }

  protected agregarEquipo(): void {
    this.equipos.update((lista) => [...lista, { nombre: '', serie: '' }]);
  }

  protected quitarEquipo(indice: number): void {
    this.equipos.update((lista) => lista.filter((_, i) => i !== indice));
  }

  protected cambiarEquipo(indice: number, campo: 'nombre' | 'serie', valor: string): void {
    this.equipos.update((lista) =>
      lista.map((e, i) => (i === indice ? { ...e, [campo]: valor } : e)),
    );
  }

  protected limpiar(): void {
    this.editandoId.set(null);
    this.codigo = '';
    this.nombre = '';
    this.ubicacion = '';
    this.estado = 'DISPONIBLE';
    this.observaciones = '';
    this.activo = true;
    this.equipos.set([]);
  }

  protected guardar(): void {
    this.guardando.set(true);
    this.error.set(null);
    const request = {
      codigo: this.codigo.trim(),
      nombre: this.nombre.trim(),
      ubicacion: this.ubicacion.trim() || null,
      estado: this.estado,
      activo: this.activo,
      observaciones: this.observaciones.trim() || null,
      equipos: this.equipos()
        .filter((e) => e.nombre.trim())
        .map((e) => ({ nombre: e.nombre.trim(), serie: e.serie.trim() || null })),
    };
    const id = this.editandoId();
    const llamada = id
      ? this.api.actualizarQuirofano(id, request)
      : this.api.crearQuirofano(request);
    llamada.subscribe({
      next: () => {
        this.guardando.set(false);
        this.avisos.success(id ? 'El quirófano se actualizó' : 'El quirófano se creó');
        this.limpiar();
        this.recargar();
      },
      error: (e: { error?: { message?: string } }) => {
        this.guardando.set(false);
        this.error.set(e.error?.message ?? 'No pudimos guardar el quirófano');
      },
    });
  }

  private recargar(): void {
    this.api.quirofanos(false).subscribe({
      next: (lista) => this.quirofanos.set(lista),
      error: () => this.error.set('No pudimos cargar los quirófanos'),
    });
  }
}
