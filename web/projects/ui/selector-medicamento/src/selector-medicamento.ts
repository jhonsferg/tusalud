import { ChangeDetectionStrategy, Component, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AutoComplete, AutoCompleteCompleteEvent } from 'primeng/autocomplete';
import { catchError, of } from 'rxjs';

import { Medicamento, MedicamentosApiService } from '@tusalud/core';

@Component({
  selector: 'app-selector-medicamento',
  imports: [FormsModule, AutoComplete],
  template: `
    <p-autocomplete
      [ngModel]="texto()"
      (ngModelChange)="texto.set($event)"
      [suggestions]="sugerencias()"
      (completeMethod)="buscar($event)"
      (onSelect)="elegir($event.value)"
      optionLabel="nombre"
      [minLength]="2"
      [delay]="250"
      [inputId]="inputId()"
      [placeholder]="placeholder()"
      [disabled]="deshabilitado()"
      [ariaLabel]="etiqueta()"
      [showClear]="false"
      emptyMessage="Sin resultados en el catálogo"
      fluid
    >
      <ng-template #item let-m>
        <div class="medicamento">
          <strong>{{ m.dci }}</strong>
          <span>{{ m.concentracion }} · {{ m.formaFarmaceutica }} · {{ m.viaAdministracion }}</span>
          @if (m.categoriaControl !== 'NINGUNA') {
            <span class="control">{{ m.categoriaControl }}</span>
          }
        </div>
      </ng-template>
    </p-autocomplete>
    @if (fallo()) {
      <p class="fallo" role="alert">No se pudo consultar el catálogo de medicamentos</p>
    }
  `,
  styles: `
    .medicamento {
      display: grid;
      gap: 0.1rem;
    }

    .medicamento span {
      color: var(--p-text-muted-color);
      font-size: 0.85rem;
    }

    .medicamento .control {
      color: #b45309;
      font-weight: 600;
    }

    .fallo {
      color: #b91c1c;
      font-size: 0.85rem;
      margin: 0.25rem 0 0;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SelectorMedicamento {
  readonly inputId = input('medicamento-buscar');
  readonly etiqueta = input('Buscar medicamento por nombre o principio activo');
  readonly placeholder = input('Buscar por nombre o principio activo');
  readonly deshabilitado = input(false);

  readonly elegido = output<Medicamento>();

  private readonly api = inject(MedicamentosApiService);

  protected readonly texto = signal<string | Medicamento | null>('');
  protected readonly sugerencias = signal<Medicamento[]>([]);
  protected readonly fallo = signal(false);

  protected buscar(evento: AutoCompleteCompleteEvent): void {
    this.fallo.set(false);
    this.api
      .buscar(evento.query)
      .pipe(
        catchError(() => {
          this.fallo.set(true);
          return of([] as Medicamento[]);
        }),
      )
      .subscribe((lista) => this.sugerencias.set(lista));
  }

  protected elegir(medicamento: Medicamento): void {
    this.elegido.emit(medicamento);
    this.texto.set('');
    this.sugerencias.set([]);
  }
}
