import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  effect,
  inject,
  input,
  model,
  signal,
  untracked,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DomSanitizer } from '@angular/platform-browser';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { Slider } from 'primeng/slider';
import { ToggleSwitch } from 'primeng/toggleswitch';

import { ArchivoImagenResponse, ImagenesStore } from '@tusalud/core';

const ZOOM_MIN = 0.25;
const ZOOM_MAX = 6;

@Component({
  selector: 'app-visor-imagen',
  imports: [FormsModule, Button, Dialog, Slider, ToggleSwitch],
  templateUrl: './visor-imagen.html',
  styleUrl: './visor-imagen.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class VisorImagen {
  readonly ordenId = input.required<string>();
  readonly archivos = input.required<readonly ArchivoImagenResponse[]>();
  readonly visible = model(false);
  readonly indice = model(0);

  private readonly store = inject(ImagenesStore);
  private readonly sanitizer = inject(DomSanitizer);

  protected readonly zoom = signal(1);
  protected readonly brillo = signal(100);
  protected readonly contraste = signal(100);
  protected readonly invertido = signal(false);
  protected readonly url = signal<string | null>(null);
  protected readonly cargando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly actual = computed<ArchivoImagenResponse | null>(
    () => this.archivos()[this.indice()] ?? null,
  );
  protected readonly esPdf = computed(() => this.actual()?.tipoContenido === 'application/pdf');
  protected readonly urlSegura = computed(() => {
    const url = this.url();
    return url ? this.sanitizer.bypassSecurityTrustResourceUrl(url) : null;
  });
  protected readonly filtro = computed(
    () =>
      `brightness(${this.brillo()}%) contrast(${this.contraste()}%) invert(${this.invertido() ? 1 : 0})`,
  );
  protected readonly zoomPorcentaje = computed(() => Math.round(this.zoom() * 100));
  protected readonly titulo = computed(() => {
    const archivo = this.actual();
    const total = this.archivos().length;
    return archivo ? `${archivo.nombre} (${this.indice() + 1} de ${total})` : 'Visor de imágenes';
  });

  private solicitud = 0;

  constructor() {
    effect(() => {
      const archivo = this.actual();
      const visible = this.visible();
      untracked(() => {
        if (visible && archivo) {
          this.cargar(archivo);
        } else {
          this.liberar();
        }
      });
    });
    inject(DestroyRef).onDestroy(() => this.liberar());
  }

  protected acercar(): void {
    this.zoom.update((valor) => Math.min(ZOOM_MAX, +(valor + 0.25).toFixed(2)));
  }

  protected alejar(): void {
    this.zoom.update((valor) => Math.max(ZOOM_MIN, +(valor - 0.25).toFixed(2)));
  }

  protected ajustar(): void {
    this.zoom.set(1);
    this.brillo.set(100);
    this.contraste.set(100);
    this.invertido.set(false);
  }

  protected rueda(evento: WheelEvent): void {
    evento.preventDefault();
    if (evento.deltaY < 0) {
      this.acercar();
    } else {
      this.alejar();
    }
  }

  protected anterior(): void {
    this.cambiar(-1);
  }

  protected siguiente(): void {
    this.cambiar(1);
  }

  protected cambiarVisible(valor: boolean): void {
    this.visible.set(valor);
  }

  private cambiar(paso: number): void {
    const total = this.archivos().length;
    if (total === 0) {
      return;
    }
    this.indice.set((this.indice() + paso + total) % total);
    this.zoom.set(1);
  }

  private cargar(archivo: ArchivoImagenResponse): void {
    this.liberar();
    const solicitud = ++this.solicitud;
    this.cargando.set(true);
    this.error.set(null);
    this.store.descargarArchivo(this.ordenId(), archivo.id).subscribe({
      next: (blob) => {
        if (solicitud !== this.solicitud) {
          return;
        }
        this.url.set(URL.createObjectURL(blob));
        this.cargando.set(false);
      },
      error: () => {
        if (solicitud !== this.solicitud) {
          return;
        }
        this.cargando.set(false);
        this.error.set('No pudimos cargar la imagen.');
      },
    });
  }

  private liberar(): void {
    this.solicitud++;
    const url = this.url();
    if (url) {
      URL.revokeObjectURL(url);
    }
    this.url.set(null);
  }
}
