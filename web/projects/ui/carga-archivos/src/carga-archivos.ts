import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  computed,
  effect,
  inject,
  input,
  model,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { ProgressBar } from 'primeng/progressbar';

import { ReglasArchivo, extensionDe, formatoTamano, validarArchivo } from '@tusalud/core';

interface Elegido {
  readonly archivo: File;
  readonly vista: string | null;
}

@Component({
  selector: 'app-carga-archivos',
  imports: [ProgressBar],
  templateUrl: './carga-archivos.html',
  styleUrl: './carga-archivos.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CargaArchivos {
  readonly reglas = input.required<ReglasArchivo>();
  readonly multiple = input(false);
  readonly subiendo = input(false);
  readonly progreso = input<number | null>(null);
  readonly inputId = input('carga-archivos');
  readonly titulo = input('Arrastra los archivos aquí o haz clic para elegirlos');
  readonly vistaPrevia = input(true);
  readonly deshabilitado = input(false);
  readonly archivos = model<File[]>([]);
  readonly rechazados = output<string[]>();

  protected readonly entrada = viewChild<ElementRef<HTMLInputElement>>('entrada');
  protected readonly arrastrando = signal(false);
  protected readonly errores = signal<string[]>([]);
  protected readonly elegidos = signal<Elegido[]>([]);
  protected readonly formato = formatoTamano;
  protected readonly accept = computed(() => this.reglas().tipos.join(','));
  protected readonly ayuda = computed(() => {
    const reglas = this.reglas();
    const tipos = reglas.extensiones.map((extension) => extension.toUpperCase()).join(', ');
    const cantidad = this.multiple() ? `, hasta ${reglas.maxArchivos}` : '';
    return `${tipos} de hasta ${formatoTamano(reglas.maxBytes)}${cantidad}`;
  });

  constructor() {
    inject(DestroyRef).onDestroy(() => this.liberarVistas(this.elegidos()));
    effect(() => {
      const actuales = this.archivos();
      const previos = this.elegidos();
      if (
        actuales.length === previos.length &&
        actuales.every((a, i) => a === previos[i].archivo)
      ) {
        return;
      }
      this.liberarVistas(previos);
      this.elegidos.set(actuales.map((archivo) => this.crear(archivo)));
    });
  }

  protected elegir(evento: Event): void {
    const entrada = evento.target as HTMLInputElement;
    this.agregar(Array.from(entrada.files ?? []));
    entrada.value = '';
  }

  protected soltar(evento: DragEvent): void {
    evento.preventDefault();
    this.arrastrando.set(false);
    if (this.deshabilitado() || this.subiendo()) {
      return;
    }
    this.agregar(Array.from(evento.dataTransfer?.files ?? []));
  }

  protected sobre(evento: DragEvent): void {
    evento.preventDefault();
    if (!this.deshabilitado() && !this.subiendo()) {
      this.arrastrando.set(true);
    }
  }

  protected quitar(indice: number): void {
    this.archivos.update((lista) => lista.filter((_, i) => i !== indice));
  }

  protected abrir(): void {
    this.entrada()?.nativeElement.click();
  }

  private agregar(nuevos: File[]): void {
    const reglas = this.reglas();
    const mensajes: string[] = [];
    const validos: File[] = [];
    const actuales = this.multiple() ? this.archivos() : [];
    for (const archivo of nuevos) {
      const problema = validarArchivo(archivo, reglas);
      if (problema) {
        mensajes.push(problema);
      } else if (actuales.length + validos.length >= (this.multiple() ? reglas.maxArchivos : 1)) {
        mensajes.push(`Solo se admiten ${this.multiple() ? reglas.maxArchivos : 1} archivo(s).`);
        break;
      } else if (
        ![...actuales, ...validos].some(
          (existente) => existente.name === archivo.name && existente.size === archivo.size,
        )
      ) {
        validos.push(archivo);
      }
    }
    this.errores.set(mensajes);
    if (mensajes.length > 0) {
      this.rechazados.emit(mensajes);
    }
    if (validos.length > 0) {
      this.archivos.set([...actuales, ...validos]);
    }
  }

  private crear(archivo: File): Elegido {
    const esImagen =
      this.vistaPrevia() && ['jpg', 'jpeg', 'png', 'webp'].includes(extensionDe(archivo.name));
    return { archivo, vista: esImagen ? URL.createObjectURL(archivo) : null };
  }

  private liberarVistas(lista: readonly Elegido[]): void {
    for (const item of lista) {
      if (item.vista) {
        URL.revokeObjectURL(item.vista);
      }
    }
  }
}
