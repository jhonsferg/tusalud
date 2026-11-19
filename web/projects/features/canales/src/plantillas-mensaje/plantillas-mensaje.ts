import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ConfirmationService } from 'primeng/api';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { Textarea } from 'primeng/textarea';

import {
  ApiError,
  CANALES_EXTERNOS,
  CanalExterno,
  CanalesStore,
  ETIQUETA_CANAL,
  NotificationService,
  PlantillaMensaje,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';

const MUESTRA: Readonly<Record<string, string>> = {
  nombre: 'María',
  enlace: 'https://tusalud.example',
  centro: 'TuSalud',
};

export function vistaPrevia(texto: string | null): string {
  return (texto ?? '').replace(/\{\{\s*([a-zA-Z_]+)\s*\}\}/g, (_, nombre: string) => {
    return MUESTRA[nombre] ?? '';
  });
}

@Component({
  selector: 'app-plantillas-mensaje',
  imports: [
    DatePipe,
    FormsModule,
    Button,
    Dialog,
    InputText,
    Message,
    Select,
    TableModule,
    Tag,
    Textarea,
    EmptyState,
    PageHeader,
  ],
  template: `
    <app-page-header
      title="Plantillas de mensajes"
      subtitle="Textos de los avisos por correo, SMS y WhatsApp. Solo admiten las variables nombre, enlace y centro."
      icon="pi-envelope"
    />

    <section class="panel">
      <div class="filtros">
        <label for="pl-canal">Canal</label>
        <p-select
          inputId="pl-canal"
          [options]="opcionesCanal"
          optionLabel="label"
          optionValue="value"
          [ngModel]="canal()"
          (ngModelChange)="canal.set($event)"
          [showClear]="true"
          placeholder="Todos"
          ariaLabel="Filtrar por canal"
        />
      </div>

      @if (store.plantillas.error(); as problema) {
        <app-empty-state
          tone="error"
          icon="pi-exclamation-triangle"
          title="No pudimos cargar las plantillas"
          [message]="problema.message"
        >
          <p-button label="Reintentar" icon="pi pi-refresh" (onClick)="store.cargarPlantillas()" />
        </app-empty-state>
      } @else {
        <p-table
          [value]="visibles()"
          [loading]="store.plantillas.loading() && !store.plantillas.hasValue()"
          [rowHover]="true"
          responsiveLayout="scroll"
        >
          <ng-template #header>
            <tr>
              <th>Aviso</th>
              <th>Canal</th>
              <th>Versión</th>
              <th>Origen</th>
              <th>Última actualización</th>
              <th style="width: 15rem"></th>
            </tr>
          </ng-template>
          <ng-template #body let-p>
            <tr>
              <td>
                <div class="font-semibold">{{ p.etiqueta }}</div>
                <small class="text-muted">{{ p.categoria }}</small>
              </td>
              <td>{{ etiquetas[p.canal] }}</td>
              <td>{{ p.version === 0 ? 'Predeterminada' : 'v' + p.version }}</td>
              <td>
                <p-tag
                  [value]="p.personalizada ? 'Personalizada' : 'Predeterminada'"
                  [severity]="p.personalizada ? 'warn' : 'secondary'"
                />
              </td>
              <td>
                @if (p.actualizadaEn) {
                  {{ p.actualizadaEn | date: 'd MMM y, HH:mm' }}
                  <small class="text-muted block">{{ p.actualizadaPor }}</small>
                }
              </td>
              <td class="derecha acciones">
                <p-button
                  label="Editar"
                  size="small"
                  [outlined]="true"
                  [ariaLabel]="'Editar la plantilla ' + p.etiqueta + ' de ' + etiquetas[p.canal]"
                  (onClick)="editar(p)"
                />
                <p-button
                  icon="pi pi-history"
                  size="small"
                  [text]="true"
                  [rounded]="true"
                  [ariaLabel]="'Ver el historial de ' + p.etiqueta + ' de ' + etiquetas[p.canal]"
                  (onClick)="verHistorial(p)"
                />
                @if (p.personalizada) {
                  <p-button
                    icon="pi pi-undo"
                    size="small"
                    [text]="true"
                    [rounded]="true"
                    severity="secondary"
                    [ariaLabel]="'Restaurar ' + p.etiqueta + ' de ' + etiquetas[p.canal]"
                    (onClick)="restaurar(p)"
                  />
                }
              </td>
            </tr>
          </ng-template>
        </p-table>
      }
    </section>

    <p-dialog
      [visible]="editando() !== null"
      (visibleChange)="$event ? null : editando.set(null)"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '40rem' }"
      [header]="
        editando() ? 'Editar: ' + editando()!.etiqueta + ' por ' + etiquetas[editando()!.canal] : ''
      "
    >
      @if (editando(); as p) {
        <form class="form" (submit)="$event.preventDefault(); guardar()" novalidate>
          @if (p.canal === 'CORREO') {
            <label for="pl-asunto">Asunto</label>
            <input
              pInputText
              id="pl-asunto"
              class="w-full"
              maxlength="150"
              [ngModel]="asunto()"
              (ngModelChange)="asunto.set($event)"
              [ngModelOptions]="{ standalone: true }"
            />
          }
          <label for="pl-cuerpo">Mensaje</label>
          <textarea
            pTextarea
            id="pl-cuerpo"
            rows="7"
            class="w-full"
            [maxlength]="p.canal === 'CORREO' ? 1000 : 320"
            [ngModel]="cuerpo()"
            (ngModelChange)="cuerpo.set($event)"
            [ngModelOptions]="{ standalone: true }"
          ></textarea>
          <small class="text-muted">
            Variables:
            @for (variable of variables(); track variable) {
              <code>{{ marcador(variable) }}</code
              >{{ ' ' }}
            }
            · {{ cuerpo().length }} caracteres
          </small>
          <label for="pl-motivo">Motivo del cambio</label>
          <input
            pInputText
            id="pl-motivo"
            class="w-full"
            maxlength="200"
            [ngModel]="motivo()"
            (ngModelChange)="motivo.set($event)"
            [ngModelOptions]="{ standalone: true }"
          />
          <div class="previa" aria-live="polite">
            <strong>Vista previa</strong>
            @if (p.canal === 'CORREO') {
              <p class="asunto">{{ previaAsunto() }}</p>
            }
            <p class="texto">{{ previaCuerpo() }}</p>
          </div>
          @if (error(); as texto) {
            <p-message severity="error" [text]="texto" />
          }
        </form>
      }
      <ng-template #footer>
        <p-button
          label="Cancelar"
          severity="secondary"
          [outlined]="true"
          (onClick)="editando.set(null)"
        />
        <p-button
          label="Publicar versión"
          icon="pi pi-check"
          [loading]="guardando()"
          (onClick)="guardar()"
        />
      </ng-template>
    </p-dialog>

    <p-dialog
      [visible]="historial() !== null"
      (visibleChange)="$event ? null : historial.set(null)"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '38rem' }"
      header="Historial de versiones"
    >
      @if (historial(); as versiones) {
        @if (versiones.length === 0) {
          <p class="text-muted">Esta plantilla nunca se personalizó.</p>
        }
        <ul class="versiones">
          @for (v of versiones; track v.version) {
            <li>
              <strong>v{{ v.version }}</strong>
              <span class="text-muted">
                {{ v.actualizadaEn | date: 'd MMM y, HH:mm' }} · {{ v.actualizadaPor }}
              </span>
              @if (v.motivo) {
                <em>{{ v.motivo }}</em>
              }
              <pre>{{ v.cuerpo }}</pre>
            </li>
          }
        </ul>
      }
    </p-dialog>
  `,
  styles: `
    .filtros {
      display: flex;
      align-items: center;
      gap: 0.6rem;
      margin-bottom: 1rem;
    }

    .filtros label {
      font-weight: 600;
      font-size: 0.9rem;
    }

    .derecha {
      text-align: right;
    }

    .acciones {
      white-space: nowrap;
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

    .previa {
      display: flex;
      flex-direction: column;
      gap: 0.3rem;
      margin-top: 0.5rem;
      padding: 0.8rem 1rem;
      border: 1px dashed var(--p-content-border-color);
      border-radius: 8px;
      background: var(--p-content-hover-background);
    }

    .previa p {
      margin: 0;
      white-space: pre-wrap;
    }

    .previa .asunto {
      font-weight: 600;
    }

    .versiones {
      display: grid;
      gap: 0.8rem;
      margin: 0;
      padding: 0;
      list-style: none;
    }

    .versiones li {
      display: flex;
      flex-direction: column;
      gap: 0.2rem;
      padding-bottom: 0.6rem;
      border-bottom: 1px solid var(--p-content-border-color);
    }

    .versiones pre {
      margin: 0;
      white-space: pre-wrap;
      font: inherit;
      font-size: 0.9rem;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PlantillasMensajeAdmin implements OnInit {
  protected readonly store = inject(CanalesStore);
  private readonly notificaciones = inject(NotificationService);
  private readonly confirmacion = inject(ConfirmationService);

  protected readonly etiquetas: Record<string, string> = ETIQUETA_CANAL;
  protected readonly opcionesCanal = CANALES_EXTERNOS.map((value) => ({
    value,
    label: ETIQUETA_CANAL[value],
  }));
  protected readonly canal = signal<CanalExterno | null>(null);
  protected readonly editando = signal<PlantillaMensaje | null>(null);
  protected readonly historial = signal<PlantillaMensaje[] | null>(null);
  protected readonly asunto = signal('');
  protected readonly cuerpo = signal('');
  protected readonly motivo = signal('');
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly variables = computed(() => this.store.plantillas.value()?.variables ?? []);
  protected readonly visibles = computed(() => {
    const todas = this.store.plantillas.value()?.plantillas ?? [];
    const canal = this.canal();
    return canal ? todas.filter((p) => p.canal === canal) : todas;
  });
  protected readonly previaAsunto = computed(() => vistaPrevia(this.asunto()));
  protected readonly previaCuerpo = computed(() => vistaPrevia(this.cuerpo()));

  ngOnInit(): void {
    this.store.cargarPlantillas();
  }

  protected marcador(variable: string): string {
    return '{' + '{' + variable + '}' + '}';
  }

  protected editar(plantilla: PlantillaMensaje): void {
    this.asunto.set(plantilla.asunto ?? '');
    this.cuerpo.set(plantilla.cuerpo);
    this.motivo.set('');
    this.error.set(null);
    this.editando.set(plantilla);
  }

  protected guardar(): void {
    const plantilla = this.editando();
    if (!plantilla) {
      return;
    }
    if (plantilla.canal === 'CORREO' && !this.asunto().trim()) {
      this.error.set('El correo necesita un asunto.');
      return;
    }
    if (!this.cuerpo().trim()) {
      this.error.set('Escribe el mensaje.');
      return;
    }
    if (this.motivo().trim().length < 5) {
      this.error.set('Indica el motivo del cambio, mínimo 5 caracteres.');
      return;
    }
    this.guardando.set(true);
    this.error.set(null);
    this.store
      .actualizarPlantilla(plantilla, {
        asunto: plantilla.canal === 'CORREO' ? this.asunto().trim() : null,
        cuerpo: this.cuerpo().trim(),
        motivo: this.motivo().trim(),
      })
      .subscribe({
        next: (nueva) => {
          this.guardando.set(false);
          this.editando.set(null);
          this.notificaciones.success(`Publicamos la versión ${nueva.version} de la plantilla.`);
        },
        error: (error: ApiError) => {
          this.guardando.set(false);
          this.error.set(error.message);
        },
      });
  }

  protected verHistorial(plantilla: PlantillaMensaje): void {
    this.store.historialPlantilla(plantilla).subscribe({
      next: (versiones) => this.historial.set(versiones),
      error: () => undefined,
    });
  }

  protected restaurar(plantilla: PlantillaMensaje): void {
    this.confirmacion.confirm({
      header: 'Restaurar plantilla',
      message: `${plantilla.etiqueta} por ${ETIQUETA_CANAL[plantilla.canal]} volverá al texto predeterminado. La versión actual queda en el historial.`,
      icon: 'pi pi-undo',
      acceptLabel: 'Restaurar',
      rejectLabel: 'Cancelar',
      rejectButtonProps: { severity: 'secondary', outlined: true },
      accept: () =>
        this.store.restaurarPlantilla(plantilla).subscribe({
          next: () => this.notificaciones.success('Se restauró el texto predeterminado.'),
          error: () => undefined,
        }),
    });
  }
}
