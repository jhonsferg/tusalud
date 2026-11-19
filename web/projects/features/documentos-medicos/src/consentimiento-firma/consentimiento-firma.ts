import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  output,
  signal,
  untracked,
  viewChild,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { SelectButton } from 'primeng/selectbutton';

import {
  ConsentimientoDetalle,
  DocumentosMedicosApiService,
  DocumentosMedicosStore,
  FirmanteTipo,
  NotificationService,
  ViaFirma,
} from '@tusalud/core';
import { Firma } from '@tusalud/ui/firma';

@Component({
  selector: 'app-consentimiento-firma',
  imports: [FormsModule, Button, Dialog, InputText, Message, SelectButton, Firma],
  template: `
    <p-dialog
      [header]="'Firmar consentimiento'"
      [visible]="id() !== null"
      (visibleChange)="$event ? null : cerrar.emit()"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '44rem', maxWidth: '96vw' }"
    >
      @if (detalle(); as d) {
        <p class="aviso" role="note">
          Lea con atención el texto completo. La firma manuscrita queda guardada junto con la fecha,
          la versión del texto y quien la registró.
        </p>
        <section class="texto" tabindex="0" aria-label="Texto del consentimiento informado">
          <h3>{{ d.resumen.plantillaNombre }}</h3>
          <pre>{{ d.textoAceptado }}</pre>
        </section>

        <form class="form" (submit)="$event.preventDefault(); firmar()" novalidate>
          @if (error(); as mensaje) {
            <p-message severity="error" styleClass="w-full">{{ mensaje }}</p-message>
          }
          <p-selectbutton
            [options]="opcionesFirmante"
            optionLabel="label"
            optionValue="value"
            [ngModel]="firmanteTipo()"
            (ngModelChange)="firmanteTipo.set($event)"
            [ngModelOptions]="{ standalone: true }"
            [allowEmpty]="false"
            ariaLabel="Quién firma"
          />
          <div class="dos">
            <label>
              Nombre completo
              <input
                pInputText
                id="firma-nombre"
                autocomplete="off"
                class="w-full"
                [ngModel]="nombre()"
                (ngModelChange)="nombre.set($event)"
                [ngModelOptions]="{ standalone: true }"
              />
            </label>
            <label>
              DNI
              <input
                pInputText
                id="firma-dni"
                autocomplete="off"
                maxlength="12"
                class="w-full"
                [ngModel]="dni()"
                (ngModelChange)="dni.set($event)"
                [ngModelOptions]="{ standalone: true }"
              />
            </label>
          </div>
          @if (firmanteTipo() === 'REPRESENTANTE') {
            <label>
              Parentesco con el paciente
              <input
                pInputText
                id="firma-parentesco"
                autocomplete="off"
                class="w-full"
                [ngModel]="parentesco()"
                (ngModelChange)="parentesco.set($event)"
                [ngModelOptions]="{ standalone: true }"
              />
            </label>
          }
          @if (d.resumen.requiereTestigo) {
            <div class="dos">
              <label>
                Nombre del testigo
                <input
                  pInputText
                  id="firma-testigo"
                  autocomplete="off"
                  class="w-full"
                  [ngModel]="testigoNombre()"
                  (ngModelChange)="testigoNombre.set($event)"
                  [ngModelOptions]="{ standalone: true }"
                />
              </label>
              <label>
                DNI del testigo
                <input
                  pInputText
                  id="firma-testigo-dni"
                  autocomplete="off"
                  maxlength="12"
                  class="w-full"
                  [ngModel]="testigoDni()"
                  (ngModelChange)="testigoDni.set($event)"
                  [ngModelOptions]="{ standalone: true }"
                />
              </label>
            </div>
          }
          <app-firma #lienzo />
        </form>
      }
      <ng-template #footer>
        <p-button
          label="Cancelar"
          severity="secondary"
          [outlined]="true"
          (onClick)="cerrar.emit()"
        />
        <p-button
          label="Firmar consentimiento"
          icon="pi pi-pencil"
          [loading]="firmando()"
          [disabled]="!detalle()"
          (onClick)="firmar()"
        />
      </ng-template>
    </p-dialog>
  `,
  styles: `
    .aviso {
      margin: 0 0 0.75rem;
      color: var(--p-text-muted-color);
      font-size: 0.9rem;
    }

    .texto {
      max-height: 14rem;
      overflow: auto;
      margin-bottom: 1rem;
      padding: 0.75rem 1rem;
      border: 1px solid var(--p-content-border-color);
      border-radius: 10px;
      background: var(--p-content-hover-background);
    }

    .texto h3 {
      margin: 0 0 0.4rem;
      font-size: 1rem;
    }

    .texto pre {
      margin: 0;
      font: inherit;
      white-space: pre-wrap;
    }

    .form {
      display: flex;
      flex-direction: column;
      gap: 0.8rem;
    }

    .form label {
      display: flex;
      flex-direction: column;
      gap: 0.3rem;
      font-size: 0.9rem;
      font-weight: 600;
    }

    .dos {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 0.75rem;
    }

    @media (max-width: 640px) {
      .dos {
        grid-template-columns: 1fr;
      }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConsentimientoFirma {
  readonly id = input<string | null>(null);
  readonly via = input<ViaFirma>('APP_PACIENTE');
  readonly nombreInicial = input('');
  readonly dniInicial = input('');

  readonly firmado = output<ConsentimientoDetalle>();
  readonly cerrar = output<void>();

  private readonly api = inject(DocumentosMedicosApiService);
  private readonly store = inject(DocumentosMedicosStore);
  private readonly notificaciones = inject(NotificationService);
  private readonly lienzo = viewChild<Firma>('lienzo');

  protected readonly opcionesFirmante: { label: string; value: FirmanteTipo }[] = [
    { label: 'Firma el paciente', value: 'PACIENTE' },
    { label: 'Firma su representante legal', value: 'REPRESENTANTE' },
  ];
  protected readonly detalle = signal<ConsentimientoDetalle | null>(null);
  protected readonly firmanteTipo = signal<FirmanteTipo>('PACIENTE');
  protected readonly nombre = signal('');
  protected readonly dni = signal('');
  protected readonly parentesco = signal('');
  protected readonly testigoNombre = signal('');
  protected readonly testigoDni = signal('');
  protected readonly firmando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly requiereRepresentante = computed(
    () => this.firmanteTipo() === 'REPRESENTANTE',
  );

  constructor() {
    effect(() => {
      const id = this.id();
      untracked(() => {
        this.detalle.set(null);
        this.error.set(null);
        this.firmanteTipo.set('PACIENTE');
        this.nombre.set(this.nombreInicial());
        this.dni.set(this.dniInicial());
        this.parentesco.set('');
        this.testigoNombre.set('');
        this.testigoDni.set('');
        if (id) {
          this.api.consentimiento(id).subscribe({
            next: (d) => this.detalle.set(d),
            error: (e: { message: string }) => this.error.set(e.message),
          });
        }
      });
    });
  }

  protected firmar(): void {
    const d = this.detalle();
    const lienzo = this.lienzo();
    if (!d || !this.id()) {
      return;
    }
    this.error.set(null);
    const png = lienzo?.exportarPng();
    if (!png) {
      this.error.set('Dibuje la firma en el recuadro antes de continuar.');
      return;
    }
    if (!this.nombre().trim() || !this.dni().trim()) {
      this.error.set('Indique el nombre completo y el DNI del firmante.');
      return;
    }
    this.firmando.set(true);
    this.store
      .firmarConsentimiento(this.id() as string, {
        firmaPng: png,
        firmanteTipo: this.firmanteTipo(),
        nombre: this.nombre().trim(),
        dni: this.dni().trim(),
        parentesco: this.requiereRepresentante() ? this.parentesco().trim() : null,
        testigoNombre: d.resumen.requiereTestigo ? this.testigoNombre().trim() : null,
        testigoDni: d.resumen.requiereTestigo ? this.testigoDni().trim() : null,
        via: this.via(),
      })
      .subscribe({
        next: (resultado) => {
          this.firmando.set(false);
          this.notificaciones.success('El consentimiento fue firmado.');
          this.firmado.emit(resultado);
        },
        error: (e: { message: string }) => {
          this.firmando.set(false);
          this.error.set(e.message);
        },
      });
  }
}
