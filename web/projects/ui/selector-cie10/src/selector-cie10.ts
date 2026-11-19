import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { InputText } from 'primeng/inputtext';
import { Subject, catchError, debounceTime, distinctUntilChanged, of, switchMap, tap } from 'rxjs';

import { Cie10ApiService, Cie10Response } from '@tusalud/core';

let secuencia = 0;

@Component({
  selector: 'app-selector-cie10',
  imports: [FormsModule, InputText],
  templateUrl: './selector-cie10.html',
  styleUrl: './selector-cie10.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SelectorCie10 {
  readonly inputId = input('cie10-buscar');
  readonly etiqueta = input('Buscar diagnóstico por código o nombre');
  readonly excluir = input<readonly string[]>([]);
  readonly sugerirFrecuentes = input(true);
  readonly deshabilitado = input(false);

  readonly elegido = output<Cie10Response>();

  private readonly api = inject(Cie10ApiService);
  private readonly busquedas = new Subject<string>();
  private cerrarEn: ReturnType<typeof setTimeout> | null = null;
  private frecuentesCargados = false;

  protected readonly idLista = 'cie10-lista-' + ++secuencia;
  protected readonly texto = signal('');
  protected readonly abierto = signal(false);
  protected readonly buscando = signal(false);
  protected readonly fallo = signal(false);
  protected readonly activo = signal(-1);
  private readonly encontrados = signal<Cie10Response[]>([]);
  private readonly frecuentes = signal<Cie10Response[]>([]);

  protected readonly modoFrecuentes = computed(() => this.texto().trim().length < 2);

  protected readonly lista = computed(() => {
    const excluidos = new Set(this.excluir().map((codigo) => codigo.toUpperCase()));
    const base = this.modoFrecuentes() ? this.frecuentes() : this.encontrados();
    return base.filter((item) => !excluidos.has(item.codigo.toUpperCase()));
  });

  protected readonly anuncio = computed(() => {
    if (!this.abierto()) {
      return '';
    }
    if (this.buscando()) {
      return 'Buscando diagnósticos';
    }
    if (this.fallo()) {
      return 'No se pudo consultar el catálogo';
    }
    const total = this.lista().length;
    if (this.modoFrecuentes()) {
      return total === 0 ? '' : total + ' diagnósticos frecuentes disponibles';
    }
    return total === 0
      ? 'Sin resultados'
      : total + (total === 1 ? ' resultado disponible' : ' resultados disponibles');
  });

  protected readonly mostrarLista = computed(
    () =>
      this.abierto() &&
      (this.lista().length > 0 || (!this.modoFrecuentes() && !this.buscando()) || this.fallo()),
  );

  constructor() {
    this.busquedas
      .pipe(
        debounceTime(250),
        distinctUntilChanged(),
        tap(() => {
          this.buscando.set(true);
          this.fallo.set(false);
        }),
        switchMap((consulta) =>
          this.api.buscar(consulta, 15).pipe(
            catchError(() => {
              this.fallo.set(true);
              return of([] as Cie10Response[]);
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((resultado) => {
        this.encontrados.set(resultado);
        this.activo.set(resultado.length > 0 ? 0 : -1);
        this.buscando.set(false);
      });
    inject(DestroyRef).onDestroy(() => this.cancelarCierre());
  }

  protected escribir(valor: string): void {
    this.texto.set(valor);
    this.abierto.set(true);
    if (valor.trim().length >= 2) {
      this.busquedas.next(valor.trim());
    } else {
      this.encontrados.set([]);
      this.buscando.set(false);
      this.activo.set(this.lista().length > 0 ? 0 : -1);
      this.cargarFrecuentes();
    }
  }

  protected abrir(): void {
    this.cancelarCierre();
    this.abierto.set(true);
    if (this.modoFrecuentes()) {
      this.cargarFrecuentes();
    }
  }

  protected cerrarConRetraso(): void {
    this.cancelarCierre();
    this.cerrarEn = setTimeout(() => this.cerrar(), 150);
  }

  protected cerrar(): void {
    this.abierto.set(false);
    this.activo.set(-1);
  }

  protected teclado(evento: KeyboardEvent): void {
    const total = this.lista().length;
    switch (evento.key) {
      case 'ArrowDown':
        evento.preventDefault();
        this.abierto.set(true);
        this.activo.set(total === 0 ? -1 : Math.min(total - 1, this.activo() + 1));
        break;
      case 'ArrowUp':
        evento.preventDefault();
        this.activo.set(total === 0 ? -1 : Math.max(0, this.activo() - 1));
        break;
      case 'Home':
        if (this.abierto() && total > 0) {
          evento.preventDefault();
          this.activo.set(0);
        }
        break;
      case 'End':
        if (this.abierto() && total > 0) {
          evento.preventDefault();
          this.activo.set(total - 1);
        }
        break;
      case 'Enter': {
        const item = this.lista()[this.activo()];
        if (this.abierto() && item) {
          evento.preventDefault();
          this.elegir(item);
        }
        break;
      }
      case 'Escape':
        if (this.abierto()) {
          evento.preventDefault();
          evento.stopPropagation();
          this.cerrar();
        }
        break;
      default:
        break;
    }
  }

  protected elegir(item: Cie10Response): void {
    this.elegido.emit(item);
    this.texto.set('');
    this.encontrados.set([]);
    this.activo.set(-1);
    this.abierto.set(false);
  }

  protected idOpcion(indice: number): string {
    return this.idLista + '-' + indice;
  }

  protected restriccion(item: Cie10Response): string | null {
    const partes: string[] = [];
    if (item.sexo) {
      partes.push(item.sexo === 'F' ? 'Solo mujeres' : 'Solo hombres');
    }
    const minima = item.edadMinima;
    const maxima = item.edadMaxima;
    if (minima != null && maxima != null) {
      partes.push(minima + ' a ' + maxima + ' años');
    } else if (minima != null) {
      partes.push('Desde ' + minima + ' años');
    } else if (maxima != null) {
      partes.push('Hasta ' + maxima + ' años');
    }
    return partes.length > 0 ? partes.join(' · ') : null;
  }

  private cargarFrecuentes(): void {
    if (!this.sugerirFrecuentes() || this.frecuentesCargados) {
      return;
    }
    this.frecuentesCargados = true;
    this.api
      .frecuentes(8)
      .pipe(catchError(() => of([])))
      .subscribe((items) => {
        this.frecuentes.set(
          items.map((f) => ({
            codigo: f.codigo,
            descripcion: f.descripcion,
            muestra: false,
          })),
        );
        if (this.modoFrecuentes() && this.abierto() && this.activo() < 0 && items.length > 0) {
          this.activo.set(0);
        }
      });
  }

  private cancelarCierre(): void {
    if (this.cerrarEn !== null) {
      clearTimeout(this.cerrarEn);
      this.cerrarEn = null;
    }
  }
}
