import { DatePipe } from '@angular/common';
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
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { Subject, debounceTime } from 'rxjs';

import {
  AuthService,
  ESTADOS_IMAGEN,
  EstadoOrdenImagen,
  ETIQUETA_ESTADO_IMAGEN,
  ImagenesStore,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { Kpi } from '@tusalud/ui/kpi';
import { PageHeader } from '@tusalud/ui/page-header';
import { ICONO_ESTADO_IMAGEN, SEVERIDAD_IMAGEN, resumenEstudios } from '../imagenes.utils';

@Component({
  selector: 'app-cola-imagenes',
  imports: [
    DatePipe,
    FormsModule,
    RouterLink,
    Button,
    IconField,
    InputIcon,
    InputText,
    Select,
    TableModule,
    Tag,
    EmptyState,
    Kpi,
    PageHeader,
  ],
  templateUrl: './cola.html',
  styleUrl: './cola.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ColaImagenes implements OnInit {
  protected readonly store = inject(ImagenesStore);
  private readonly auth = inject(AuthService);

  protected readonly esPersonal = computed(() => this.auth.isImagenes());
  protected readonly busqueda = signal('');
  protected readonly estado = signal<EstadoOrdenImagen | null>(null);
  private readonly cambio = new Subject<void>();

  protected readonly estados = ESTADOS_IMAGEN.map((valor) => ({
    label: ETIQUETA_ESTADO_IMAGEN[valor],
    value: valor,
  }));
  protected readonly etiqueta: Readonly<Record<string, string>> = ETIQUETA_ESTADO_IMAGEN;
  protected readonly severidad = SEVERIDAD_IMAGEN;
  protected readonly icono = ICONO_ESTADO_IMAGEN;
  protected readonly resumirEstudios = resumenEstudios;

  constructor() {
    this.cambio.pipe(debounceTime(300), takeUntilDestroyed()).subscribe(() => this.cargar());
  }

  ngOnInit(): void {
    if (this.esPersonal()) {
      this.store.cargarResumen();
    }
    this.cargar();
  }

  protected buscar(valor: string): void {
    this.busqueda.set(valor);
    this.cambio.next();
  }

  protected cargar(): void {
    this.store.cargarOrdenes({ estado: this.estado(), q: this.busqueda().trim() || null });
  }
}
