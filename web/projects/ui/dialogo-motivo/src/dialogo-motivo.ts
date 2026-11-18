import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  input,
  model,
  output,
  signal,
  untracked,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { Textarea } from 'primeng/textarea';

import { limpiarMotivo } from './motivo.service';

let secuencia = 0;

@Component({
  selector: 'app-dialogo-motivo',
  imports: [FormsModule, Button, Dialog, Textarea],
  template: `
    <p-dialog
      [header]="titulo()"
      [visible]="visible()"
      (visibleChange)="alCambiarVisible($event)"
      [modal]="true"
      [draggable]="false"
      [closable]="true"
      [style]="{ width: '30rem', maxWidth: '96vw' }"
    >
      <form class="motivo" (submit)="$event.preventDefault(); aceptar()" novalidate>
        @if (descripcion(); as texto) {
          <p class="descripcion">{{ texto }}</p>
        }
        <label [for]="idCampo">{{ etiqueta() }}</label>
        <textarea
          pTextarea
          [id]="idCampo"
          name="motivo"
          rows="4"
          class="w-full"
          autocomplete="off"
          autocorrect="off"
          spellcheck="true"
          data-lpignore="true"
          [attr.maxlength]="maximo() + 100"
          [attr.aria-invalid]="mostrarError()"
          [attr.aria-describedby]="idAyuda"
          [ngModel]="texto()"
          (ngModelChange)="texto.set($event)"
        ></textarea>
        <div class="pie-campo" [id]="idAyuda">
          @if (mostrarError()) {
            <span class="error" role="alert">{{ mensajeError() }}</span>
          } @else {
            <span class="ayuda">Entre {{ minimo() }} y {{ maximo() }} caracteres.</span>
          }
          <span class="contador" [class.excedido]="longitud() > maximo()"
            >{{ longitud() }}/{{ maximo() }}</span
          >
        </div>
      </form>
      <ng-template #footer>
        <p-button
          label="Volver"
          severity="secondary"
          [outlined]="true"
          (onClick)="cancelar.emit(); visible.set(false)"
        />
        <p-button
          [label]="confirmarLabel()"
          [severity]="severidad() === 'danger' ? 'danger' : undefined"
          [icon]="icono()"
          [loading]="cargando()"
          (onClick)="aceptar()"
        />
      </ng-template>
    </p-dialog>
  `,
  styles: `
    .motivo {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
    }

    .descripcion {
      margin: 0 0 0.25rem;
      color: var(--p-text-muted-color);
    }

    label {
      font-weight: 600;
    }

    .pie-campo {
      display: flex;
      justify-content: space-between;
      gap: 1rem;
      font-size: 0.82rem;
    }

    .ayuda,
    .contador {
      color: var(--p-text-muted-color);
    }

    .error {
      color: var(--p-red-600);
      font-weight: 600;
    }

    .contador.excedido {
      color: var(--p-red-600);
      font-weight: 700;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DialogoMotivo {
  readonly visible = model(false);
  readonly titulo = input.required<string>();
  readonly descripcion = input<string | null>(null);
  readonly etiqueta = input('Motivo');
  readonly confirmarLabel = input('Confirmar');
  readonly severidad = input<'danger' | 'primary'>('primary');
  readonly icono = input<string | undefined>(undefined);
  readonly minimo = input(5);
  readonly maximo = input(300);
  readonly cargando = input(false);

  readonly confirmar = output<string>();
  readonly cancelar = output<void>();

  protected readonly idCampo = `motivo-${++secuencia}`;
  protected readonly idAyuda = `${this.idCampo}-ayuda`;
  protected readonly texto = signal('');
  private readonly intentado = signal(false);

  protected readonly limpio = computed(() => limpiarMotivo(this.texto()));
  protected readonly longitud = computed(() => this.limpio().length);
  protected readonly valido = computed(
    () => this.longitud() >= this.minimo() && this.longitud() <= this.maximo(),
  );
  protected readonly mostrarError = computed(() => this.intentado() && !this.valido());
  protected readonly mensajeError = computed(() =>
    this.longitud() > this.maximo()
      ? `El motivo no puede superar los ${this.maximo()} caracteres.`
      : `Escribe al menos ${this.minimo()} caracteres.`,
  );

  constructor() {
    effect(() => {
      if (this.visible()) {
        untracked(() => {
          this.texto.set('');
          this.intentado.set(false);
        });
      }
    });
  }

  protected alCambiarVisible(abierto: boolean): void {
    this.visible.set(abierto);
    if (!abierto) {
      this.cancelar.emit();
    }
  }

  protected aceptar(): void {
    this.intentado.set(true);
    if (this.valido() && !this.cargando()) {
      this.confirmar.emit(this.limpio());
    }
  }
}
