import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  afterNextRender,
  computed,
  inject,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { Button } from 'primeng/button';

interface Punto {
  readonly x: number;
  readonly y: number;
}

const GROSOR = 2.4;
const COLOR = '#0f172a';

@Component({
  selector: 'app-firma',
  imports: [Button],
  template: `
    <div class="firma" [class.deshabilitada]="deshabilitada()">
      <canvas
        #lienzo
        class="lienzo"
        role="img"
        [attr.aria-label]="etiqueta()"
        tabindex="0"
        (pointerdown)="empezar($event)"
        (pointermove)="trazar($event)"
        (pointerup)="terminar($event)"
        (pointercancel)="terminar($event)"
        (pointerleave)="terminar($event)"
        (keydown.delete)="limpiar()"
        (keydown.control.z)="deshacer()"
      ></canvas>
      @if (!hayTrazos()) {
        <span class="ayuda" aria-hidden="true">{{ ayuda() }}</span>
      }
    </div>
    <div class="acciones">
      <p-button
        label="Deshacer"
        icon="pi pi-undo"
        severity="secondary"
        [text]="true"
        size="small"
        [disabled]="!hayTrazos() || deshabilitada()"
        (onClick)="deshacer()"
      />
      <p-button
        label="Limpiar"
        icon="pi pi-eraser"
        severity="secondary"
        [text]="true"
        size="small"
        [disabled]="!hayTrazos() || deshabilitada()"
        (onClick)="limpiar()"
      />
    </div>
  `,
  styles: `
    :host {
      display: block;
    }

    .firma {
      position: relative;
      border: 2px dashed var(--p-content-border-color);
      border-radius: 12px;
      background: #ffffff;
    }

    .firma:focus-within {
      border-color: var(--p-primary-color);
    }

    .firma.deshabilitada {
      opacity: 0.6;
      pointer-events: none;
    }

    .lienzo {
      display: block;
      width: 100%;
      height: 11rem;
      touch-action: none;
      cursor: crosshair;
      outline: none;
    }

    .ayuda {
      position: absolute;
      inset: 0;
      display: grid;
      place-items: center;
      color: var(--p-text-muted-color);
      font-size: 0.9rem;
      pointer-events: none;
    }

    .acciones {
      display: flex;
      justify-content: flex-end;
      gap: 0.25rem;
      margin-top: 0.35rem;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Firma {
  readonly etiqueta = input('Recuadro para dibujar la firma con el dedo o el mouse');
  readonly ayuda = input('Firme aquí');
  readonly deshabilitada = input(false);

  readonly cambio = output<boolean>();

  private readonly lienzo = viewChild.required<ElementRef<HTMLCanvasElement>>('lienzo');
  private readonly elemento = inject<ElementRef<HTMLElement>>(ElementRef);

  private trazos: Punto[][] = [];
  private actual: Punto[] | null = null;
  private readonly cantidad = signal(0);

  protected readonly hayTrazos = computed(() => this.cantidad() > 0);

  constructor() {
    afterNextRender(() => {
      this.ajustar();
      const observador = new ResizeObserver(() => this.ajustar());
      observador.observe(this.elemento.nativeElement);
    });
  }

  vacia(): boolean {
    return this.trazos.length === 0;
  }

  exportarPng(): string | null {
    if (this.vacia()) {
      return null;
    }
    return this.lienzo().nativeElement.toDataURL('image/png');
  }

  limpiar(): void {
    this.trazos = [];
    this.actual = null;
    this.redibujar();
    this.notificar();
  }

  deshacer(): void {
    this.trazos.pop();
    this.redibujar();
    this.notificar();
  }

  protected empezar(evento: PointerEvent): void {
    if (this.deshabilitada() || (evento.pointerType === 'mouse' && evento.button !== 0)) {
      return;
    }
    evento.preventDefault();
    this.lienzo().nativeElement.setPointerCapture(evento.pointerId);
    this.actual = [this.punto(evento)];
    this.trazos.push(this.actual);
    this.redibujar();
  }

  protected trazar(evento: PointerEvent): void {
    if (!this.actual) {
      return;
    }
    evento.preventDefault();
    this.actual.push(this.punto(evento));
    this.redibujar();
  }

  protected terminar(evento: PointerEvent): void {
    if (!this.actual) {
      return;
    }
    this.lienzo().nativeElement.releasePointerCapture?.(evento.pointerId);
    if (this.actual.length < 2) {
      this.trazos.pop();
    }
    this.actual = null;
    this.redibujar();
    this.notificar();
  }

  private notificar(): void {
    this.cantidad.set(this.trazos.length);
    this.cambio.emit(this.trazos.length > 0);
  }

  private punto(evento: PointerEvent): Punto {
    const rect = this.lienzo().nativeElement.getBoundingClientRect();
    return { x: evento.clientX - rect.left, y: evento.clientY - rect.top };
  }

  private ajustar(): void {
    const lienzo = this.lienzo().nativeElement;
    const rect = lienzo.getBoundingClientRect();
    const escala = window.devicePixelRatio || 1;
    const ancho = Math.max(1, Math.round(rect.width * escala));
    const alto = Math.max(1, Math.round(rect.height * escala));
    if (lienzo.width !== ancho || lienzo.height !== alto) {
      lienzo.width = ancho;
      lienzo.height = alto;
      this.redibujar();
    }
  }

  private redibujar(): void {
    const lienzo = this.lienzo().nativeElement;
    const contexto = lienzo.getContext('2d');
    if (!contexto) {
      return;
    }
    const escala = window.devicePixelRatio || 1;
    contexto.setTransform(1, 0, 0, 1, 0, 0);
    contexto.clearRect(0, 0, lienzo.width, lienzo.height);
    contexto.scale(escala, escala);
    contexto.lineWidth = GROSOR;
    contexto.lineCap = 'round';
    contexto.lineJoin = 'round';
    contexto.strokeStyle = COLOR;
    for (const trazo of this.trazos) {
      contexto.beginPath();
      trazo.forEach((p, i) => (i === 0 ? contexto.moveTo(p.x, p.y) : contexto.lineTo(p.x, p.y)));
      if (trazo.length === 1) {
        contexto.lineTo(trazo[0].x + 0.01, trazo[0].y + 0.01);
      }
      contexto.stroke();
    }
  }
}
