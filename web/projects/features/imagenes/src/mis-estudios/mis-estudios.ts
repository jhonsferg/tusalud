import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { Button } from 'primeng/button';
import { Tag } from 'primeng/tag';

import {
  ETIQUETA_ESTADO_IMAGEN,
  ETIQUETA_MODALIDAD,
  ICONO_MODALIDAD,
  ImagenesStore,
  OrdenImagenResponse,
} from '@tusalud/core';
import { BotonDocumento } from '@tusalud/ui/boton-documento';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { ICONO_ESTADO_IMAGEN, SEVERIDAD_IMAGEN } from '../imagenes.utils';
import { VisorImagen } from '../visor/visor-imagen';

@Component({
  selector: 'app-mis-estudios',
  imports: [BotonDocumento, DatePipe, Button, Tag, EmptyState, PageHeader, VisorImagen],
  templateUrl: './mis-estudios.html',
  styleUrl: './mis-estudios.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MisEstudios implements OnInit {
  protected readonly store = inject(ImagenesStore);

  protected readonly etiquetaEstado = ETIQUETA_ESTADO_IMAGEN;
  protected readonly etiquetaModalidad = ETIQUETA_MODALIDAD;
  protected readonly iconoModalidad = ICONO_MODALIDAD;
  protected readonly severidad = SEVERIDAD_IMAGEN;
  protected readonly icono = ICONO_ESTADO_IMAGEN;

  protected readonly visorOrden = signal<OrdenImagenResponse | null>(null);
  protected readonly visorVisible = signal(false);
  protected readonly visorIndice = signal(0);
  protected readonly informeAbierto = signal<string | null>(null);

  ngOnInit(): void {
    this.store.cargarMisEstudios();
  }

  protected verImagen(orden: OrdenImagenResponse, indice: number): void {
    this.visorOrden.set(orden);
    this.visorIndice.set(indice);
    this.visorVisible.set(true);
  }

  protected alternarInforme(orden: OrdenImagenResponse): void {
    this.informeAbierto.set(this.informeAbierto() === orden.id ? null : orden.id);
  }
}
