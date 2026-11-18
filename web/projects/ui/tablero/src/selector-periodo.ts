import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

export const PERIODOS_DIAS: readonly number[] = [7, 30, 90];
const CLAVE_PERIODO = 'tusalud.periodo.dias';

export function leerPeriodo(predeterminado = 30): number {
  try {
    const valor = Number(globalThis.localStorage?.getItem(CLAVE_PERIODO));
    return PERIODOS_DIAS.includes(valor) ? valor : predeterminado;
  } catch {
    return predeterminado;
  }
}

export function guardarPeriodo(dias: number): void {
  try {
    globalThis.localStorage?.setItem(CLAVE_PERIODO, String(dias));
  } catch {
    return;
  }
}

@Component({
  selector: 'app-selector-periodo',
  template: `
    <div class="periodo" role="group" aria-label="Periodo del tablero">
      @for (dias of opciones; track dias) {
        <button
          type="button"
          [class.activo]="dias === valor()"
          [attr.aria-pressed]="dias === valor()"
          (click)="cambio.emit(dias)"
        >
          {{ dias }} días
        </button>
      }
    </div>
  `,
  styles: `
    .periodo {
      display: inline-flex;
      padding: 0.2rem;
      border: 1px solid var(--p-content-border-color);
      border-radius: var(--radio-md);
      background: var(--p-content-background);
    }

    button {
      min-height: 2.25rem;
      padding: 0 var(--sp-4);
      border: 0;
      border-radius: var(--radio-sm);
      background: transparent;
      color: var(--p-text-muted-color);
      font: inherit;
      font-weight: 600;
      cursor: pointer;
    }

    button.activo {
      background: var(--app-primary-soft);
      color: var(--app-primary-text);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SelectorPeriodo {
  readonly valor = input.required<number>();
  readonly cambio = output<number>();
  protected readonly opciones = PERIODOS_DIAS;
}
