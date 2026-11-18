import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';

import { DialogoMotivo } from './dialogo-motivo';
import { MotivoService } from './motivo.service';

@Component({
  selector: 'app-motivo-outlet',
  imports: [DialogoMotivo],
  template: `
    @if (solicitud(); as s) {
      <app-dialogo-motivo
        [visible]="true"
        [titulo]="s.titulo"
        [descripcion]="s.descripcion ?? null"
        [etiqueta]="s.etiqueta ?? 'Motivo'"
        [confirmarLabel]="s.confirmar ?? 'Confirmar'"
        [severidad]="s.severidad ?? 'primary'"
        [icono]="s.icono"
        [minimo]="s.minimo ?? 5"
        [maximo]="s.maximo ?? 300"
        (confirmar)="servicio.resolver($event)"
        (cancelar)="servicio.resolver(null)"
      />
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MotivoOutlet {
  protected readonly servicio = inject(MotivoService);
  protected readonly solicitud = computed(() => this.servicio.solicitud());
}
