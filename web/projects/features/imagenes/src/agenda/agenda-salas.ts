import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';
import { Tag } from 'primeng/tag';

import {
  AgendaImagenResponse,
  AuthService,
  ETIQUETA_ESTADO_IMAGEN,
  ETIQUETA_MODALIDAD,
  ImagenesStore,
  SALAS_IMAGEN,
  toIsoDate,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { SEVERIDAD_IMAGEN } from '../imagenes.utils';

interface ColumnaSala {
  readonly sala: string;
  readonly citas: readonly AgendaImagenResponse[];
}

@Component({
  selector: 'app-agenda-salas',
  imports: [DatePipe, FormsModule, RouterLink, Button, DatePicker, Tag, EmptyState, PageHeader],
  templateUrl: './agenda-salas.html',
  styleUrl: './agenda-salas.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AgendaSalas implements OnInit {
  protected readonly store = inject(ImagenesStore);
  private readonly auth = inject(AuthService);

  protected readonly fecha = signal(new Date());
  protected readonly etiquetaEstado = ETIQUETA_ESTADO_IMAGEN;
  protected readonly etiquetaModalidad = ETIQUETA_MODALIDAD;
  protected readonly severidad = SEVERIDAD_IMAGEN;
  protected readonly puedeAbrir = computed(() => this.auth.isImagenes());

  protected readonly columnas = computed<readonly ColumnaSala[]>(() => {
    const citas = this.store.agenda.value() ?? [];
    const salas = [...SALAS_IMAGEN];
    for (const cita of citas) {
      if (!salas.includes(cita.sala)) {
        salas.push(cita.sala);
      }
    }
    return salas.map((sala) => ({
      sala,
      citas: citas
        .filter((cita) => cita.sala === sala)
        .sort((a, b) => a.programadaPara.localeCompare(b.programadaPara)),
    }));
  });

  protected readonly totalCitas = computed(() => (this.store.agenda.value() ?? []).length);

  ngOnInit(): void {
    this.cargar();
  }

  protected mover(dias: number): void {
    const nueva = new Date(this.fecha());
    nueva.setDate(nueva.getDate() + dias);
    this.elegir(nueva);
  }

  protected hoy(): void {
    this.elegir(new Date());
  }

  protected elegir(valor: Date | null): void {
    if (!valor) {
      return;
    }
    this.fecha.set(valor);
    this.cargar();
  }

  protected fin(cita: AgendaImagenResponse): Date {
    return new Date(new Date(cita.programadaPara).getTime() + cita.duracionMinutos * 60_000);
  }

  private cargar(): void {
    const dia = toIsoDate(this.fecha());
    this.store.cargarAgenda(dia, dia);
  }
}
