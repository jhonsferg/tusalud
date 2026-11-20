import { ChangeDetectionStrategy, Component, ElementRef, inject } from '@angular/core';

interface ObjetoFlotante {
  readonly nombre: string;
  readonly clase: string;
}

@Component({
  selector: 'app-objetos-flotantes',
  templateUrl: './objetos-flotantes.html',
  styleUrl: './objetos-flotantes.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    'aria-hidden': 'true',
    '(window:pointermove)': 'mover($event)',
  },
})
export class ObjetosFlotantes {
  private readonly elemento = inject<ElementRef<HTMLElement>>(ElementRef);
  private cuadro = 0;

  protected readonly objetos: readonly ObjetoFlotante[] = [
    { nombre: 'escudo-cruz', clase: 'escudo' },
    { nombre: 'corazon-pulso', clase: 'corazon' },
    { nombre: 'calendario', clase: 'calendario' },
    { nombre: 'estetoscopio', clase: 'estetoscopio' },
  ];

  protected mover(evento: PointerEvent): void {
    if (typeof window === 'undefined' || this.cuadro) {
      return;
    }
    this.cuadro = requestAnimationFrame(() => {
      this.cuadro = 0;
      const x = (evento.clientX / window.innerWidth - 0.5) * 2;
      const y = (evento.clientY / window.innerHeight - 0.5) * 2;
      const estilo = this.elemento.nativeElement.style;
      estilo.setProperty('--px', x.toFixed(3));
      estilo.setProperty('--py', y.toFixed(3));
    });
  }
}
