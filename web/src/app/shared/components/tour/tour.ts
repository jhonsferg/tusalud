import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  HostListener,
  afterRenderEffect,
  computed,
  inject,
  signal,
} from '@angular/core';
import { Button } from 'primeng/button';

import { TourService } from '@tusalud/app/core/services/tour.service';

interface Rect {
  readonly top: number;
  readonly left: number;
  readonly width: number;
  readonly height: number;
}

interface Posicion {
  readonly top: number;
  readonly left?: number;
}

const MARGEN = 8;
const ANCHO_TARJETA = 360;
const ALTO_TARJETA = 260;

@Component({
  selector: 'app-tour',
  imports: [Button],
  templateUrl: './tour.html',
  styleUrl: './tour.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Tour {
  protected readonly tour = inject(TourService);

  private readonly objetivo = signal<Rect | null>(null);
  private readonly viewport = signal({ ancho: window.innerWidth, alto: window.innerHeight });

  protected readonly spot = computed(() => {
    const rect = this.objetivo();
    return rect
      ? {
          top: rect.top - MARGEN,
          left: rect.left - MARGEN,
          width: rect.width + MARGEN * 2,
          height: rect.height + MARGEN * 2,
        }
      : null;
  });

  protected readonly tarjeta = computed<Posicion | null>(() => {
    const spot = this.spot();
    const { ancho, alto } = this.viewport();
    if (!spot) {
      return null;
    }
    if (ancho < 640) {
      const debajo = spot.top + spot.height + 12;
      return { top: debajo > alto - ALTO_TARJETA ? 16 : debajo };
    }
    const derecha = spot.left + spot.width + 16;
    if (derecha + ANCHO_TARJETA < ancho) {
      return { top: Math.max(16, Math.min(spot.top, alto - ALTO_TARJETA)), left: derecha };
    }
    return {
      top: Math.max(16, Math.min(spot.top + spot.height + 16, alto - ALTO_TARJETA)),
      left: Math.max(16, Math.min(spot.left, ancho - ANCHO_TARJETA - 16)),
    };
  });

  constructor() {
    const resize = () => {
      this.viewport.set({ ancho: window.innerWidth, alto: window.innerHeight });
      this.medir(false);
    };
    window.addEventListener('resize', resize);
    inject(DestroyRef).onDestroy(() => window.removeEventListener('resize', resize));
    afterRenderEffect(() => {
      this.tour.index();
      if (this.tour.active()) {
        this.medir(true);
      } else {
        this.objetivo.set(null);
      }
    });
  }

  @HostListener('document:keydown', ['$event'])
  protected teclado(evento: KeyboardEvent): void {
    if (!this.tour.active()) {
      return;
    }
    if (evento.key === 'Escape') {
      this.tour.close();
    } else if (evento.key === 'ArrowRight') {
      this.tour.next();
    } else if (evento.key === 'ArrowLeft') {
      this.tour.previous();
    }
  }

  private medir(desplazar: boolean): void {
    const paso = this.tour.step();
    const el = paso?.target ? this.tour.element(paso.target) : null;
    if (!el) {
      this.objetivo.set(null);
      return;
    }
    if (desplazar) {
      el.scrollIntoView({ block: 'nearest', inline: 'nearest' });
    }
    const rect = el.getBoundingClientRect();
    this.objetivo.set({ top: rect.top, left: rect.left, width: rect.width, height: rect.height });
  }
}
