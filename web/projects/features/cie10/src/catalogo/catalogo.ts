import { DatePipe, DecimalPipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { Subject, catchError, debounceTime, of, switchMap } from 'rxjs';

import {
  ApiError,
  Cie10ApiService,
  Cie10ImportacionResponse,
  Cie10Response,
  Cie10ResumenResponse,
  DescargaService,
  NotificationService,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { Kpi } from '@tusalud/ui/kpi';
import { PageHeader } from '@tusalud/ui/page-header';

const TAMANO_MAXIMO = 5 * 1024 * 1024;

@Component({
  selector: 'app-catalogo-cie10',
  imports: [
    DatePipe,
    DecimalPipe,
    FormsModule,
    Button,
    InputText,
    Message,
    TableModule,
    Tag,
    EmptyState,
    Kpi,
    PageHeader,
  ],
  templateUrl: './catalogo.html',
  styleUrl: './catalogo.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CatalogoCie10 implements OnInit {
  private readonly api = inject(Cie10ApiService);
  private readonly descarga = inject(DescargaService);
  private readonly notifications = inject(NotificationService);
  private readonly consultas = new Subject<string>();

  protected readonly resumen = signal<Cie10ResumenResponse | null>(null);
  protected readonly errorResumen = signal<string | null>(null);

  protected readonly consulta = signal('');
  protected readonly resultados = signal<Cie10Response[]>([]);
  protected readonly buscando = signal(false);
  protected readonly buscado = signal(false);

  protected readonly archivo = signal<File | null>(null);
  protected readonly errorArchivo = signal<string | null>(null);
  protected readonly desactivarAusentes = signal(false);
  protected readonly omitirErrores = signal(false);
  protected readonly etiqueta = signal('');
  protected readonly vista = signal<Cie10ImportacionResponse | null>(null);
  protected readonly analizando = signal(false);
  protected readonly importando = signal(false);
  protected readonly errorImportacion = signal<string | null>(null);
  protected readonly importada = signal<Cie10ImportacionResponse | null>(null);

  protected readonly puedeImportar = computed(() => {
    const vista = this.vista();
    return (
      !!this.archivo() &&
      !!vista &&
      vista.validas > 0 &&
      (vista.totalErrores === 0 || this.omitirErrores()) &&
      this.etiqueta().trim().length > 0 &&
      !this.importando()
    );
  });

  constructor() {
    this.consultas
      .pipe(
        debounceTime(250),
        switchMap((texto) => {
          if (texto.trim().length < 2) {
            this.buscando.set(false);
            return of<Cie10Response[] | null>(null);
          }
          this.buscando.set(true);
          return this.api.buscar(texto.trim(), 30).pipe(catchError(() => of<Cie10Response[]>([])));
        }),
        takeUntilDestroyed(),
      )
      .subscribe((resultado) => {
        this.buscando.set(false);
        this.buscado.set(resultado !== null);
        this.resultados.set(resultado ?? []);
      });
  }

  ngOnInit(): void {
    this.cargarResumen();
  }

  protected cargarResumen(): void {
    this.api.resumen().subscribe({
      next: (resumen) => {
        this.resumen.set(resumen);
        this.errorResumen.set(null);
      },
      error: (error: ApiError) => this.errorResumen.set(error.message),
    });
  }

  protected buscar(texto: string): void {
    this.consulta.set(texto);
    this.consultas.next(texto);
  }

  protected descargarPlantilla(): void {
    this.api.plantilla().subscribe((blob) => {
      this.descarga.guardar({ blob, nombre: 'plantilla-cie10.csv' });
    });
  }

  protected elegirArchivo(evento: Event): void {
    const campo = evento.target as HTMLInputElement;
    const elegido = campo.files?.[0] ?? null;
    this.vista.set(null);
    this.importada.set(null);
    this.errorImportacion.set(null);
    this.errorArchivo.set(null);
    if (!elegido) {
      this.archivo.set(null);
      return;
    }
    if (!/\.(csv|txt)$/i.test(elegido.name)) {
      this.archivo.set(null);
      this.errorArchivo.set('El archivo debe ser un CSV.');
      campo.value = '';
      return;
    }
    if (elegido.size > TAMANO_MAXIMO) {
      this.archivo.set(null);
      this.errorArchivo.set('El archivo supera el máximo de 5 MB.');
      campo.value = '';
      return;
    }
    this.archivo.set(elegido);
  }

  protected previsualizar(): void {
    const archivo = this.archivo();
    if (!archivo) {
      return;
    }
    this.errorImportacion.set(null);
    this.importada.set(null);
    this.analizando.set(true);
    this.api.vistaPrevia(archivo, this.desactivarAusentes()).subscribe({
      next: (vista) => {
        this.analizando.set(false);
        this.vista.set(vista);
      },
      error: (error: ApiError) => {
        this.analizando.set(false);
        this.errorImportacion.set(error.message);
      },
    });
  }

  protected importar(): void {
    const archivo = this.archivo();
    if (!archivo || !this.puedeImportar()) {
      return;
    }
    this.errorImportacion.set(null);
    this.importando.set(true);
    this.api
      .importar(archivo, this.etiqueta().trim(), this.desactivarAusentes(), this.omitirErrores())
      .subscribe({
        next: (resultado) => {
          this.importando.set(false);
          this.importada.set(resultado);
          this.vista.set(null);
          this.archivo.set(null);
          this.etiqueta.set('');
          this.notifications.success(
            'El catálogo se importó: ' +
              resultado.altas +
              ' altas y ' +
              resultado.cambios +
              ' cambios.',
          );
          this.cargarResumen();
        },
        error: (error: ApiError) => {
          this.importando.set(false);
          this.errorImportacion.set(error.message);
        },
      });
  }

  protected restriccion(item: Cie10Response): string {
    const partes: string[] = [];
    if (item.sexo) {
      partes.push(item.sexo === 'F' ? 'Solo mujeres' : 'Solo hombres');
    }
    if (item.edadMinima != null && item.edadMaxima != null) {
      partes.push(item.edadMinima + ' a ' + item.edadMaxima + ' años');
    } else if (item.edadMinima != null) {
      partes.push('Desde ' + item.edadMinima + ' años');
    } else if (item.edadMaxima != null) {
      partes.push('Hasta ' + item.edadMaxima + ' años');
    }
    return partes.join(' · ');
  }

  protected severidadAccion(accion: string): 'success' | 'info' | 'secondary' {
    return accion === 'ALTA' ? 'success' : accion === 'CAMBIO' ? 'info' : 'secondary';
  }

  protected etiquetaAccion(accion: string): string {
    return accion === 'ALTA' ? 'Alta' : accion === 'CAMBIO' ? 'Cambio' : 'Sin cambios';
  }
}
