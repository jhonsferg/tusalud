import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { Checkbox } from 'primeng/checkbox';
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';

import {
  DocumentosMedicosStore,
  ETIQUETA_NIVEL_ESTABLECIMIENTO,
  EstablecimientoSalud,
  NivelEstablecimiento,
  NotificationService,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';

@Component({
  selector: 'app-establecimientos',
  imports: [
    FormsModule,
    Button,
    Checkbox,
    Dialog,
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
      title="Establecimientos de salud"
      subtitle="Catálogo de destinos para referencias y contrarreferencias"
      icon="pi-building"
    >
      <p-button label="Nuevo establecimiento" icon="pi pi-plus" (onClick)="nuevo()" />
    </app-page-header>

    <section class="panel">
      <p-table
        [value]="store.establecimientos.value() ?? []"
        [loading]="store.establecimientos.loading()"
        responsiveLayout="scroll"
      >
        <ng-template #header>
          <tr>
            <th>Establecimiento</th>
            <th>Nivel</th>
            <th>Distrito</th>
            <th>Estado</th>
            <th></th>
          </tr>
        </ng-template>
        <ng-template #body let-e>
          <tr>
            <td>
              <strong>{{ e.nombre }}</strong>
              <small class="block text-muted">{{ e.codigoIpress || 'Sin código IPRESS' }}</small>
            </td>
            <td>{{ niveles[e.nivel] }}</td>
            <td>{{ e.distrito || '—' }}</td>
            <td>
              <p-tag
                [value]="e.activo ? 'Activo' : 'Inactivo'"
                [severity]="e.activo ? 'success' : 'secondary'"
              />
            </td>
            <td class="derecha">
              <p-button
                label="Editar"
                size="small"
                [outlined]="true"
                [ariaLabel]="'Editar ' + e.nombre"
                (onClick)="editar(e)"
              />
            </td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr>
            <td colspan="5"><app-empty-state icon="pi-building" title="Sin establecimientos" /></td>
          </tr>
        </ng-template>
      </p-table>
    </section>

    <p-dialog
      [header]="editandoId() ? 'Editar establecimiento' : 'Nuevo establecimiento'"
      [(visible)]="abierto"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '32rem', maxWidth: '96vw' }"
    >
      <form class="form" (submit)="$event.preventDefault(); guardar()" novalidate>
        @if (error(); as mensaje) {
          <p-message severity="error" styleClass="w-full">{{ mensaje }}</p-message>
        }
        <label for="est-nombre">Nombre</label>
        <input
          pInputText
          id="est-nombre"
          class="w-full"
          maxlength="200"
          autocomplete="off"
          [ngModel]="nombre()"
          (ngModelChange)="nombre.set($event)"
          [ngModelOptions]="{ standalone: true }"
        />
        <label for="est-codigo">Código IPRESS</label>
        <input
          pInputText
          id="est-codigo"
          class="w-full"
          maxlength="20"
          autocomplete="off"
          [ngModel]="codigo()"
          (ngModelChange)="codigo.set($event)"
          [ngModelOptions]="{ standalone: true }"
        />
        <label for="est-nivel">Nivel</label>
        <p-select
          inputId="est-nivel"
          [options]="opcionesNivel"
          optionLabel="label"
          optionValue="value"
          [ngModel]="nivel()"
          (ngModelChange)="nivel.set($event)"
          [ngModelOptions]="{ standalone: true }"
          appendTo="body"
          styleClass="w-full"
        />
        <label for="est-distrito">Distrito</label>
        <input
          pInputText
          id="est-distrito"
          class="w-full"
          maxlength="100"
          autocomplete="off"
          [ngModel]="distrito()"
          (ngModelChange)="distrito.set($event)"
          [ngModelOptions]="{ standalone: true }"
        />
        <label for="est-contacto">Contacto</label>
        <input
          pInputText
          id="est-contacto"
          class="w-full"
          maxlength="150"
          autocomplete="off"
          [ngModel]="contacto()"
          (ngModelChange)="contacto.set($event)"
          [ngModelOptions]="{ standalone: true }"
        />
        <div class="check">
          <p-checkbox
            inputId="est-activo"
            [binary]="true"
            [ngModel]="activo()"
            (ngModelChange)="activo.set($event)"
            [ngModelOptions]="{ standalone: true }"
          />
          <label for="est-activo">Disponible como destino</label>
        </div>
      </form>
      <ng-template #footer>
        <p-button
          label="Cancelar"
          severity="secondary"
          [outlined]="true"
          (onClick)="abierto.set(false)"
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
    }

    .check {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      margin-top: 0.5rem;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Establecimientos implements OnInit {
  protected readonly store = inject(DocumentosMedicosStore);
  private readonly notificaciones = inject(NotificationService);

  protected readonly niveles: Record<string, string> = ETIQUETA_NIVEL_ESTABLECIMIENTO;
  protected readonly opcionesNivel = Object.entries(ETIQUETA_NIVEL_ESTABLECIMIENTO).map(
    ([value, label]) => ({ label, value }),
  );
  protected readonly abierto = signal(false);
  protected readonly editandoId = signal<string | null>(null);
  protected readonly nombre = signal('');
  protected readonly codigo = signal('');
  protected readonly nivel = signal<NivelEstablecimiento>('PRIMER_NIVEL');
  protected readonly distrito = signal('');
  protected readonly contacto = signal('');
  protected readonly activo = signal(true);
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.store.cargarEstablecimientos();
  }

  protected nuevo(): void {
    this.rellenar(null);
  }

  protected editar(e: EstablecimientoSalud): void {
    this.rellenar(e);
  }

  protected guardar(): void {
    if (!this.nombre().trim()) {
      this.error.set('Indica el nombre del establecimiento.');
      return;
    }
    this.guardando.set(true);
    this.store
      .guardarEstablecimiento(this.editandoId(), {
        nombre: this.nombre().trim(),
        codigoIpress: this.codigo().trim() || null,
        nivel: this.nivel(),
        distrito: this.distrito().trim() || null,
        contacto: this.contacto().trim() || null,
        activo: this.activo(),
      })
      .subscribe({
        next: () => {
          this.guardando.set(false);
          this.abierto.set(false);
          this.notificaciones.success('El establecimiento se guardó.');
        },
        error: (e: { message: string }) => {
          this.guardando.set(false);
          this.error.set(e.message);
        },
      });
  }

  private rellenar(e: EstablecimientoSalud | null): void {
    this.error.set(null);
    this.editandoId.set(e?.id ?? null);
    this.nombre.set(e?.nombre ?? '');
    this.codigo.set(e?.codigoIpress ?? '');
    this.nivel.set(e?.nivel ?? 'PRIMER_NIVEL');
    this.distrito.set(e?.distrito ?? '');
    this.contacto.set(e?.contacto ?? '');
    this.activo.set(e?.activo ?? true);
    this.abierto.set(true);
  }
}
