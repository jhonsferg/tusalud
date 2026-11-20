import { ChangeDetectionStrategy, Component, computed, effect, input, signal } from '@angular/core';

interface Diapositiva {
  readonly base: string;
  readonly foco: string;
}

const DIAPOSITIVAS: readonly Diapositiva[] = [
  { base: 'img/portal/slide-01', foco: '74% 50%' },
  { base: 'img/portal/slide-02', foco: '70% 50%' },
  { base: 'img/portal/slide-03', foco: '76% 50%' },
  { base: 'img/portal/slide-04', foco: '72% 50%' },
];

@Component({
  selector: 'app-carrusel-portada',
  templateUrl: './carrusel-portada.html',
  styleUrl: './carrusel-portada.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '(mouseenter)': 'pausaInteraccion.set(true)',
    '(mouseleave)': 'pausaInteraccion.set(false)',
    '(focusin)': 'pausaInteraccion.set(true)',
    '(focusout)': 'pausaInteraccion.set(false)',
  },
})
export class CarruselPortada {
  readonly intervaloMs = input(7500);

  protected readonly diapositivas = DIAPOSITIVAS;
  protected readonly indice = signal(0);
  protected readonly pausaManual = signal(false);
  protected readonly pausaInteraccion = signal(false);
  protected readonly reducirMovimiento = signal(false);
  protected readonly enMarcha = computed(
    () => !this.pausaManual() && !this.pausaInteraccion() && !this.reducirMovimiento(),
  );

  constructor() {
    if (typeof matchMedia === 'function') {
      const consulta = matchMedia('(prefers-reduced-motion: reduce)');
      this.reducirMovimiento.set(consulta.matches);
      consulta.addEventListener('change', (evento) => this.reducirMovimiento.set(evento.matches));
    }
    effect((alLimpiar) => {
      if (!this.enMarcha()) {
        return;
      }
      const temporizador = setInterval(
        () => this.indice.update((actual) => (actual + 1) % DIAPOSITIVAS.length),
        this.intervaloMs(),
      );
      alLimpiar(() => clearInterval(temporizador));
    });
  }

  protected ir(posicion: number): void {
    this.indice.set(posicion);
  }

  protected alternarPausa(): void {
    this.pausaManual.update((valor) => !valor);
  }
}
