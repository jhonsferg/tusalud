import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  input,
  model,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { InputText } from 'primeng/inputtext';
import { Subject, debounceTime, distinctUntilChanged, of, switchMap } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { PacienteMostradorResponse, RecepcionApiService } from '@tusalud/core';

@Component({
  selector: 'app-paciente-buscador',
  imports: [FormsModule, InputText],
  templateUrl: './paciente-buscador.html',
  styleUrl: './paciente-buscador.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PacienteBuscador {
  readonly paciente = model<PacienteMostradorResponse | null>(null);
  readonly inputId = input('paciente-buscar');
  readonly etiqueta = input('Buscar paciente por DNI o nombre');

  private readonly api = inject(RecepcionApiService);
  protected readonly termino = signal('');
  protected readonly resultados = signal<PacienteMostradorResponse[]>([]);
  protected readonly abierto = signal(false);
  protected readonly buscando = signal(false);
  private readonly consulta = new Subject<string>();

  constructor() {
    this.consulta
      .pipe(
        debounceTime(250),
        distinctUntilChanged(),
        switchMap((texto) => {
          if (texto.trim().length < 2) {
            this.buscando.set(false);
            return of([] as PacienteMostradorResponse[]);
          }
          this.buscando.set(true);
          return this.api.buscarPacientes(texto.trim());
        }),
        takeUntilDestroyed(inject(DestroyRef)),
      )
      .subscribe({
        next: (lista) => {
          this.buscando.set(false);
          this.resultados.set(lista);
          this.abierto.set(true);
        },
        error: () => this.buscando.set(false),
      });
  }

  protected escribir(texto: string): void {
    this.termino.set(texto);
    this.consulta.next(texto);
  }

  protected elegir(paciente: PacienteMostradorResponse): void {
    this.paciente.set(paciente);
    this.abierto.set(false);
    this.termino.set('');
    this.resultados.set([]);
  }

  protected limpiar(): void {
    this.paciente.set(null);
  }
}
