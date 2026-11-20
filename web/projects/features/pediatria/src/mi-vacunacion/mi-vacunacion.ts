import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { Button } from 'primeng/button';
import { Message } from 'primeng/message';

import { CarneVacunacion, DescargaService, VacunacionApiService } from '@tusalud/core';
import { PageHeader } from '@tusalud/ui/page-header';
import { CarneVista } from '../carne-vista/carne-vista';

@Component({
  selector: 'app-mi-vacunacion',
  imports: [Button, Message, PageHeader, CarneVista],
  template: `
    <app-page-header
      title="Mi carné de vacunación"
      subtitle="Tus vacunas aplicadas, las que faltan y la próxima dosis"
      icon="pi-shield"
    />
    @if (cargando()) {
      <p class="text-muted" role="status">Cargando tu carné…</p>
    } @else if (error()) {
      <p-message severity="error">{{ error() }}</p-message>
    } @else if (carne(); as c) {
      <div class="panel">
        <p-button
          label="Descargar carné en PDF"
          icon="pi pi-download"
          [outlined]="true"
          [loading]="descargando()"
          (onClick)="descargar(c)"
        />
        <app-carne-vista [carne]="c" />
      </div>
    }
  `,
  styles: `
    .panel {
      display: grid;
      gap: 1rem;
    }

    p-button {
      justify-self: start;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MiVacunacion implements OnInit {
  private readonly api = inject(VacunacionApiService);
  private readonly descargas = inject(DescargaService);

  protected readonly carne = signal<CarneVacunacion | null>(null);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly descargando = signal(false);

  ngOnInit(): void {
    this.api.carneMio().subscribe({
      next: (c) => {
        this.carne.set(c);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar tu carné de vacunación');
        this.cargando.set(false);
      },
    });
  }

  protected descargar(c: CarneVacunacion): void {
    this.descargando.set(true);
    this.api.pdf(c.pacienteId).subscribe({
      next: (archivo) => {
        this.descargando.set(false);
        this.descargas.guardar(archivo);
      },
      error: () => this.descargando.set(false),
    });
  }
}
