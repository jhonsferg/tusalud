import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ConfirmationService } from 'primeng/api';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';

import { ApiError, NotificationService, PlantillaResponse, PlantillasStore } from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';

const TAMANO_MAXIMO = 512 * 1024;

@Component({
  selector: 'app-plantillas',
  imports: [
    DatePipe,
    FormsModule,
    Button,
    Dialog,
    InputText,
    Message,
    TableModule,
    Tag,
    EmptyState,
    PageHeader,
  ],
  templateUrl: './plantillas.html',
  styleUrl: './plantillas.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Plantillas implements OnInit {
  protected readonly store = inject(PlantillasStore);
  private readonly notificaciones = inject(NotificationService);
  private readonly confirmacion = inject(ConfirmationService);

  protected readonly reemplazando = signal<PlantillaResponse | null>(null);
  protected readonly archivo = signal<File | null>(null);
  protected readonly motivo = signal('');
  protected readonly enviando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly viendoHistorial = signal<PlantillaResponse | null>(null);

  ngOnInit(): void {
    this.store.cargar();
  }

  protected descargar(plantilla: PlantillaResponse): void {
    this.store.descargar(plantilla.codigo).subscribe({ error: () => undefined });
  }

  protected abrirReemplazo(plantilla: PlantillaResponse): void {
    this.archivo.set(null);
    this.motivo.set('');
    this.error.set(null);
    this.reemplazando.set(plantilla);
  }

  protected elegir(evento: Event): void {
    const entrada = evento.target as HTMLInputElement;
    const elegido = entrada.files?.item(0) ?? null;
    this.error.set(null);
    if (elegido && elegido.size > TAMANO_MAXIMO) {
      this.archivo.set(null);
      this.error.set('La plantilla supera los 512 KB permitidos.');
      return;
    }
    if (elegido && !/\.(jrxml|xml)$/i.test(elegido.name)) {
      this.archivo.set(null);
      this.error.set('Elige un archivo JRXML.');
      return;
    }
    this.archivo.set(elegido);
  }

  protected reemplazar(): void {
    const plantilla = this.reemplazando();
    const archivo = this.archivo();
    if (!plantilla || !archivo) {
      this.error.set('Elige el archivo JRXML de la nueva plantilla.');
      return;
    }
    this.enviando.set(true);
    this.error.set(null);
    this.store.reemplazar(plantilla.codigo, archivo, this.motivo().trim() || null).subscribe({
      next: (nueva) => {
        this.enviando.set(false);
        this.reemplazando.set(null);
        this.notificaciones.success(
          `La plantilla ${nueva.nombre} pasó a la versión ${nueva.version}.`,
        );
      },
      error: (error: ApiError) => {
        this.enviando.set(false);
        this.error.set(error.message);
      },
    });
  }

  protected restaurar(plantilla: PlantillaResponse): void {
    this.confirmacion.confirm({
      header: 'Restaurar plantilla',
      message: `${plantilla.nombre} volverá a la plantilla predeterminada. La versión actual queda en el historial.`,
      icon: 'pi pi-undo',
      acceptLabel: 'Restaurar',
      rejectLabel: 'Cancelar',
      rejectButtonProps: { severity: 'secondary', outlined: true },
      accept: () =>
        this.store.restaurar(plantilla.codigo, null).subscribe({
          next: () => this.notificaciones.success('Se restauró la plantilla predeterminada.'),
          error: () => undefined,
        }),
    });
  }

  protected abrirHistorial(plantilla: PlantillaResponse): void {
    this.viendoHistorial.set(plantilla);
    this.store.cargarHistorial(plantilla.codigo);
  }
}
