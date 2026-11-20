import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Button } from 'primeng/button';
import { Message } from 'primeng/message';

import { DescargaService, HceApiService } from '@tusalud/core';
import { PageHeader } from '@tusalud/ui/page-header';

@Component({
  selector: 'app-mi-historia',
  imports: [Button, Message, PageHeader],
  template: `
    <app-page-header
      title="Mi historia clínica"
      subtitle="Descarga tu historia completa en PDF con un código para verificarla"
      icon="pi-file-pdf"
    />
    <section class="panel">
      <p>
        El documento reúne tus atenciones, diagnósticos, indicaciones y las rectificaciones con su
        motivo. Lleva un código y un QR que cualquier persona puede usar en la página pública de
        verificación para comprobar que es auténtico.
      </p>
      <p-button
        label="Descargar mi historia en PDF"
        icon="pi pi-download"
        [loading]="descargando()"
        (onClick)="descargar()"
      />
      @if (error()) {
        <p-message severity="error">{{ error() }}</p-message>
      }
    </section>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MiHistoria {
  private readonly api = inject(HceApiService);
  private readonly descargas = inject(DescargaService);

  protected readonly descargando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected descargar(): void {
    this.descargando.set(true);
    this.error.set(null);
    this.api.miHistoria().subscribe({
      next: (archivo) => {
        this.descargando.set(false);
        this.descargas.guardar(archivo);
      },
      error: () => {
        this.descargando.set(false);
        this.error.set('No se pudo generar tu historia clínica');
      },
    });
  }
}
