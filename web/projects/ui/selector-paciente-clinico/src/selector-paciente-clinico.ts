import { ChangeDetectionStrategy, Component, OnInit, inject, model, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';

import { PacienteClinicoResumen, VacunacionApiService } from '@tusalud/core';

export function edadLegible(meses: number | null): string {
  if (meses === null) {
    return 'Edad no registrada';
  }
  if (meses < 24) {
    return `${meses} ${meses === 1 ? 'mes' : 'meses'}`;
  }
  return `${Math.floor(meses / 12)} años`;
}

@Component({
  selector: 'app-selector-paciente-clinico',
  imports: [FormsModule, Button, InputText],
  template: `
    <section class="selector panel" aria-label="Selección de paciente">
      <form class="busqueda" (submit)="$event.preventDefault(); buscar()">
        <label for="selector-paciente-q" class="etiqueta">Buscar por nombre o DNI</label>
        <div class="fila">
          <input
            pInputText
            id="selector-paciente-q"
            type="search"
            [ngModel]="texto()"
            (ngModelChange)="texto.set($event)"
            name="q"
            autocomplete="off"
          />
          <p-button type="submit" label="Buscar" icon="pi pi-search" [loading]="cargando()" />
        </div>
      </form>
      @if (error()) {
        <p class="error" role="alert">{{ error() }}</p>
      } @else if (pacientes().length === 0 && !cargando()) {
        <p class="text-muted vacio">No hay pacientes con atención vigente para mostrar.</p>
      } @else {
        <ul class="lista" role="listbox" aria-label="Pacientes disponibles">
          @for (p of pacientes(); track p.id) {
            <li>
              <button
                type="button"
                role="option"
                class="paciente"
                [class.activo]="seleccionado()?.id === p.id"
                [attr.aria-selected]="seleccionado()?.id === p.id"
                (click)="seleccionado.set(p)"
              >
                <strong>{{ p.nombre }}</strong>
                <small>DNI {{ p.dni }} · {{ edad(p.edadMeses) }}</small>
              </button>
            </li>
          }
        </ul>
      }
    </section>
  `,
  styles: `
    .selector {
      display: grid;
      gap: 0.75rem;
    }

    .etiqueta {
      font-weight: 600;
      font-size: 0.85rem;
    }

    .fila {
      display: flex;
      gap: 0.5rem;
    }

    .fila input {
      flex: 1;
    }

    .lista {
      list-style: none;
      margin: 0;
      padding: 0;
      display: grid;
      gap: 0.35rem;
      max-height: 16rem;
      overflow: auto;
    }

    .paciente {
      width: 100%;
      display: grid;
      text-align: left;
      gap: 0.1rem;
      padding: 0.55rem 0.75rem;
      border: 1px solid var(--p-surface-300, #cbd5e1);
      border-radius: 0.5rem;
      background: var(--p-surface-0, #fff);
      cursor: pointer;
    }

    .paciente:hover,
    .paciente:focus-visible {
      border-color: var(--p-primary-500, #2563eb);
    }

    .paciente.activo {
      border-color: var(--p-primary-600, #2563eb);
      background: var(--p-primary-50, #eff6ff);
    }

    .vacio,
    .error {
      margin: 0;
    }

    .error {
      color: var(--p-red-700, #b91c1c);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SelectorPacienteClinico implements OnInit {
  readonly seleccionado = model<PacienteClinicoResumen | null>(null);

  private readonly api = inject(VacunacionApiService);
  protected readonly texto = signal('');
  protected readonly pacientes = signal<PacienteClinicoResumen[]>([]);
  protected readonly cargando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly edad = edadLegible;

  ngOnInit(): void {
    this.buscar();
  }

  protected buscar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.api.pacientes(this.texto()).subscribe({
      next: (lista) => {
        this.pacientes.set(lista);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar la lista de pacientes');
        this.cargando.set(false);
      },
    });
  }
}
