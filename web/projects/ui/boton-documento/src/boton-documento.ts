import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { ButtonSeverity } from 'primeng/button';
import { Button } from 'primeng/button';

import { DescargaService, DocumentosApiService, TipoDocumentoPdf } from '@tusalud/core';

@Component({
  selector: 'app-boton-documento',
  imports: [Button],
  template: `
    <p-button
      [label]="soloIcono() ? undefined : etiqueta()"
      [icon]="'pi ' + icono()"
      [outlined]="true"
      [rounded]="soloIcono()"
      [text]="soloIcono()"
      size="small"
      [severity]="severidad()"
      [loading]="cargando()"
      [disabled]="deshabilitado()"
      [ariaLabel]="ariaEtiqueta() ?? etiqueta()"
      (onClick)="generar()"
    />
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BotonDocumento {
  readonly tipo = input.required<TipoDocumentoPdf>();
  readonly registro = input.required<string>();
  readonly etiqueta = input('Descargar PDF');
  readonly ariaEtiqueta = input<string | null>(null);
  readonly icono = input('pi-file-pdf');
  readonly accion = input<'descargar' | 'imprimir'>('descargar');
  readonly severidad = input<ButtonSeverity>('secondary');
  readonly soloIcono = input(false);
  readonly deshabilitado = input(false);

  private readonly api = inject(DocumentosApiService);
  private readonly descargas = inject(DescargaService);

  protected readonly cargando = signal(false);

  protected generar(): void {
    if (this.cargando()) {
      return;
    }
    this.cargando.set(true);
    this.api.generar(this.tipo(), this.registro()).subscribe({
      next: (archivo) => {
        this.cargando.set(false);
        if (this.accion() === 'imprimir') {
          this.descargas.abrir(archivo);
        } else {
          this.descargas.guardar(archivo);
        }
      },
      error: () => this.cargando.set(false),
    });
  }
}
