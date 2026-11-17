import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  effect,
  inject,
  input,
  untracked,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';

import { ETIQUETA_ESTADO_IMAGEN, ImagenesStore, optionalIdAttribute } from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { ICONO_ESTADO_IMAGEN, SEVERIDAD_IMAGEN, resumenEstudios } from '../imagenes.utils';

@Component({
  selector: 'app-estudios-paciente',
  imports: [DatePipe, RouterLink, Button, TableModule, Tag, EmptyState, PageHeader],
  templateUrl: './estudios-paciente.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EstudiosPaciente {
  readonly pacienteId = input(undefined, { transform: optionalIdAttribute });

  protected readonly store = inject(ImagenesStore);
  protected readonly etiqueta: Readonly<Record<string, string>> = ETIQUETA_ESTADO_IMAGEN;
  protected readonly severidad = SEVERIDAD_IMAGEN;
  protected readonly icono = ICONO_ESTADO_IMAGEN;
  protected readonly resumirEstudios = resumenEstudios;

  constructor() {
    effect(() => {
      const id = this.pacienteId();
      if (id !== undefined) {
        untracked(() => {
          this.store.ordenesDePaciente.reset();
          this.store.cargarOrdenesDePaciente(id);
        });
      }
    });
  }
}
