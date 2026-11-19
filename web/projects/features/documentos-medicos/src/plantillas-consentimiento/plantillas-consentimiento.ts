import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { Checkbox } from 'primeng/checkbox';
import { Dialog } from 'primeng/dialog';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { Textarea } from 'primeng/textarea';

import {
  ConsentimientoPlantilla,
  DocumentosMedicosStore,
  NotificationService,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';

@Component({
  selector: 'app-plantillas-consentimiento',
  imports: [
    FormsModule,
    Button,
    Checkbox,
    Dialog,
    InputNumber,
    InputText,
    Message,
    TableModule,
    Tag,
    Textarea,
    EmptyState,
    PageHeader,
  ],
  template: `
    <app-page-header
      title="Plantillas de consentimiento"
      subtitle="Textos de consentimiento informado por procedimiento"
      icon="pi-pencil"
    >
      <p-button label="Nueva plantilla" icon="pi pi-plus" (onClick)="nueva()" />
    </app-page-header>

    <section class="panel">
      <p-table
        [value]="store.plantillas.value() ?? []"
        [loading]="store.plantillas.loading()"
        responsiveLayout="scroll"
      >
        <ng-template #header>
          <tr>
            <th>Plantilla</th>
            <th>Vigencia</th>
            <th>Testigo</th>
            <th>Estado</th>
            <th></th>
          </tr>
        </ng-template>
        <ng-template #body let-p>
          <tr>
            <td>
              <strong>{{ p.nombre }}</strong>
              <small class="block text-muted">{{ p.codigo }} · versión {{ p.version }}</small>
            </td>
            <td>{{ p.vigenciaDias }} días</td>
            <td>{{ p.requiereTestigo ? 'Sí' : 'No' }}</td>
            <td>
              <p-tag
                [value]="p.activa ? 'Activa' : 'Inactiva'"
                [severity]="p.activa ? 'success' : 'secondary'"
              />
            </td>
            <td class="derecha">
              <p-button
                label="Editar"
                size="small"
                [outlined]="true"
                [ariaLabel]="'Editar ' + p.nombre"
                (onClick)="editar(p)"
              />
            </td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr>
            <td colspan="5"><app-empty-state icon="pi-pencil" title="Sin plantillas" /></td>
          </tr>
        </ng-template>
      </p-table>
    </section>

    <p-dialog
      [header]="editandoId() ? 'Editar plantilla' : 'Nueva plantilla'"
      [(visible)]="abierto"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '44rem', maxWidth: '96vw' }"
    >
      <form class="form" (submit)="$event.preventDefault(); guardar()" novalidate>
        @if (error(); as mensaje) {
          <p-message severity="error" styleClass="w-full">{{ mensaje }}</p-message>
        }
        <label for="pl-codigo">Código</label>
        <input
          pInputText
          id="pl-codigo"
          class="w-full"
          maxlength="40"
          autocomplete="off"
          [ngModel]="codigo()"
          (ngModelChange)="codigo.set($event)"
          [ngModelOptions]="{ standalone: true }"
        />
        <label for="pl-nombre">Nombre</label>
        <input
          pInputText
          id="pl-nombre"
          class="w-full"
          maxlength="200"
          autocomplete="off"
          [ngModel]="nombre()"
          (ngModelChange)="nombre.set($event)"
          [ngModelOptions]="{ standalone: true }"
        />
        <label for="pl-texto">Texto del consentimiento</label>
        <textarea
          pTextarea
          id="pl-texto"
          rows="5"
          class="w-full"
          [ngModel]="texto()"
          (ngModelChange)="texto.set($event)"
          [ngModelOptions]="{ standalone: true }"
        ></textarea>
        <label for="pl-riesgos">Riesgos</label>
        <textarea
          pTextarea
          id="pl-riesgos"
          rows="3"
          class="w-full"
          [ngModel]="riesgos()"
          (ngModelChange)="riesgos.set($event)"
          [ngModelOptions]="{ standalone: true }"
        ></textarea>
        <label for="pl-beneficios">Beneficios</label>
        <textarea
          pTextarea
          id="pl-beneficios"
          rows="2"
          class="w-full"
          [ngModel]="beneficios()"
          (ngModelChange)="beneficios.set($event)"
          [ngModelOptions]="{ standalone: true }"
        ></textarea>
        <label for="pl-alternativas">Alternativas</label>
        <textarea
          pTextarea
          id="pl-alternativas"
          rows="2"
          class="w-full"
          [ngModel]="alternativas()"
          (ngModelChange)="alternativas.set($event)"
          [ngModelOptions]="{ standalone: true }"
        ></textarea>
        <label for="pl-vigencia">Vigencia en días</label>
        <p-inputnumber
          inputId="pl-vigencia"
          [min]="1"
          [max]="3650"
          [ngModel]="vigencia()"
          (ngModelChange)="vigencia.set($event ?? 30)"
          [ngModelOptions]="{ standalone: true }"
        />
        <div class="check">
          <p-checkbox
            inputId="pl-testigo"
            [binary]="true"
            [ngModel]="testigo()"
            (ngModelChange)="testigo.set($event)"
            [ngModelOptions]="{ standalone: true }"
          />
          <label for="pl-testigo">Requiere testigo</label>
        </div>
        <div class="check">
          <p-checkbox
            inputId="pl-activa"
            [binary]="true"
            [ngModel]="activa()"
            (ngModelChange)="activa.set($event)"
            [ngModelOptions]="{ standalone: true }"
          />
          <label for="pl-activa">Plantilla activa</label>
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
      max-height: 65vh;
      overflow: auto;
      padding-right: 0.25rem;
    }

    .form label {
      font-weight: 600;
      font-size: 0.9rem;
    }

    .check {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PlantillasConsentimiento implements OnInit {
  protected readonly store = inject(DocumentosMedicosStore);
  private readonly notificaciones = inject(NotificationService);

  protected readonly abierto = signal(false);
  protected readonly editandoId = signal<string | null>(null);
  protected readonly codigo = signal('');
  protected readonly nombre = signal('');
  protected readonly texto = signal('');
  protected readonly riesgos = signal('');
  protected readonly beneficios = signal('');
  protected readonly alternativas = signal('');
  protected readonly vigencia = signal(30);
  protected readonly testigo = signal(false);
  protected readonly activa = signal(true);
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.store.cargarPlantillas();
  }

  protected nueva(): void {
    this.rellenar(null);
  }

  protected editar(p: ConsentimientoPlantilla): void {
    this.rellenar(p);
  }

  protected guardar(): void {
    if (!this.codigo().trim() || !this.nombre().trim() || !this.texto().trim()) {
      this.error.set('Completa el código, el nombre y el texto.');
      return;
    }
    this.guardando.set(true);
    this.store
      .guardarPlantilla(this.editandoId(), {
        codigo: this.codigo().trim(),
        nombre: this.nombre().trim(),
        texto: this.texto().trim(),
        riesgos: this.riesgos().trim(),
        beneficios: this.beneficios().trim(),
        alternativas: this.alternativas().trim(),
        requiereTestigo: this.testigo(),
        vigenciaDias: this.vigencia(),
        activa: this.activa(),
      })
      .subscribe({
        next: () => {
          this.guardando.set(false);
          this.abierto.set(false);
          this.notificaciones.success('La plantilla se guardó.');
        },
        error: (e: { message: string }) => {
          this.guardando.set(false);
          this.error.set(e.message);
        },
      });
  }

  private rellenar(p: ConsentimientoPlantilla | null): void {
    this.error.set(null);
    this.editandoId.set(p?.id ?? null);
    this.codigo.set(p?.codigo ?? '');
    this.nombre.set(p?.nombre ?? '');
    this.texto.set(p?.texto ?? '');
    this.riesgos.set(p?.riesgos ?? '');
    this.beneficios.set(p?.beneficios ?? '');
    this.alternativas.set(p?.alternativas ?? '');
    this.vigencia.set(p?.vigenciaDias ?? 30);
    this.testigo.set(p?.requiereTestigo ?? false);
    this.activa.set(p?.activa ?? true);
    this.abierto.set(true);
  }
}
