import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { Button, ButtonSeverity } from 'primeng/button';

import {
  ExportacionesStore,
  FormatoExportacion,
  NotificationService,
  ParametrosExportacion,
  TipoExportacion,
} from '@tusalud/core';

export function parametrosLimpios(parametros: ParametrosExportacion): Record<string, string> {
  const limpio: Record<string, string> = {};
  for (const [clave, valor] of Object.entries(parametros)) {
    if (valor !== null && valor !== undefined && valor !== '') {
      limpio[clave] = String(valor);
    }
  }
  return limpio;
}

@Component({
  selector: 'app-boton-exportar',
  imports: [Button],
  template: `
    <p-button
      [label]="etiqueta() ?? (formato() === 'PDF' ? 'Exportar PDF' : 'Exportar Excel')"
      [icon]="'pi ' + (icono() ?? (formato() === 'PDF' ? 'pi-file-pdf' : 'pi-file-excel'))"
      [outlined]="true"
      size="small"
      [severity]="severidad()"
      [loading]="exportando()"
      [disabled]="deshabilitado()"
      [ariaLabel]="etiqueta() ?? (formato() === 'PDF' ? 'Exportar a PDF' : 'Exportar a Excel')"
      (onClick)="exportar()"
    />
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BotonExportar {
  readonly tipo = input.required<TipoExportacion>();
  readonly formato = input<FormatoExportacion>('PDF');
  readonly parametros = input<ParametrosExportacion>({});
  readonly etiqueta = input<string | null>(null);
  readonly icono = input<string | null>(null);
  readonly severidad = input<ButtonSeverity>('secondary');
  readonly deshabilitado = input(false);

  private readonly store = inject(ExportacionesStore);
  private readonly notificaciones = inject(NotificationService);

  protected readonly exportando = signal(false);

  protected exportar(): void {
    if (this.exportando()) {
      return;
    }
    this.exportando.set(true);
    this.store
      .solicitarYEsperar({
        tipo: this.tipo(),
        formato: this.formato(),
        parametros: parametrosLimpios(this.parametros()),
      })
      .subscribe({
        next: (resultado) => {
          this.exportando.set(false);
          if (resultado.estado === 'descargada') {
            this.notificaciones.success('La exportación se descargó.');
          } else if (resultado.estado === 'en-proceso') {
            this.notificaciones.info(
              'La exportación sigue en proceso. Cuando termine la encontrarás en Mis exportaciones.',
              8000,
            );
          } else {
            this.notificaciones.error(
              resultado.exportacion.error ?? 'No se pudo generar la exportación.',
            );
          }
        },
        error: () => this.exportando.set(false),
      });
  }
}
