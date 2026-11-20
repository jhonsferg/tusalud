import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { Button } from 'primeng/button';

import { EmptyStateAviso } from '../vacio';
import { NotificationService, PlanDetalle, PlanResumen, PlanesApiService } from '@tusalud/core';
import { PageHeader } from '@tusalud/ui/page-header';
import { PlanDetalleVista } from '../plan-detalle/plan-detalle';

@Component({
  selector: 'app-mi-plan',
  imports: [Button, PageHeader, PlanDetalleVista, EmptyStateAviso],
  template: `
    <app-page-header
      title="Mi plan de tratamiento"
      subtitle="Tus metas, tus controles y el registro de tus mediciones"
      icon="pi-heart"
    />
    @if (planes().length === 0) {
      <app-vacio-plan />
    }
    <div class="selector">
      @for (p of planes(); track p.id) {
        <p-button
          [label]="p.programa"
          size="small"
          [outlined]="true"
          [ariaLabel]="'Ver mi plan de ' + p.programa"
          (onClick)="abrir(p)"
        />
      }
    </div>
    <app-plan-detalle [plan]="detalle()" [puedeCumplir]="false" (medir)="medir($event)" />
  `,
  styles: `
    .selector {
      display: flex;
      gap: 0.5rem;
      margin-bottom: 1rem;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MiPlan implements OnInit {
  private readonly api = inject(PlanesApiService);
  private readonly avisos = inject(NotificationService);

  protected readonly planes = signal<PlanResumen[]>([]);
  protected readonly detalle = signal<PlanDetalle | null>(null);

  ngOnInit(): void {
    this.api.mios().subscribe((lista) => {
      this.planes.set(lista);
      if (lista.length > 0) {
        this.abrir(lista[0]);
      }
    });
  }

  protected abrir(p: PlanResumen): void {
    this.api.obtener(p.id).subscribe((d) => this.detalle.set(d));
  }

  protected medir(evento: {
    parametro: PlanDetalle['metas'][number]['parametro'];
    valor: number;
  }): void {
    const plan = this.detalle();
    if (!plan) {
      return;
    }
    this.api.medir(plan.id, evento).subscribe({
      next: (d) => {
        this.detalle.set(d);
        this.avisos.success('Medición registrada');
      },
      error: (e) => this.avisos.error(e.error?.message ?? 'No se pudo registrar la medición'),
    });
  }
}
