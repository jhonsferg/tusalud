import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { InputNumber } from 'primeng/inputnumber';
import { Select } from 'primeng/select';

import {
  ContabilidadApiService,
  ETIQUETA_LIBRO_ELECTRONICO,
  LibroElectronico,
  LibroElectronicoTipo,
} from '@tusalud/core';
import { PageHeader } from '@tusalud/ui/page-header';
import { ESTILOS_CONTABILIDAD } from '../contabilidad.estilos';
import { MESES, descargarTexto } from '../contabilidad.utils';

@Component({
  selector: 'app-libros-electronicos',
  imports: [FormsModule, Button, InputNumber, Select, PageHeader],
  template: `
    <app-page-header
      title="Libros electrónicos"
      subtitle="Archivos de texto plano para el Programa de Libros Electrónicos de SUNAT"
      icon="pi-file-export"
    />

    <section class="panel">
      <p class="aviso" role="note">
        El formato del archivo está por validar con el contador y con el PLE vigente de SUNAT antes
        de presentarlo. Úsalo como borrador de trabajo.
      </p>
      <div class="filtros">
        <label for="le-libro" class="font-semibold">Libro</label>
        <p-select
          inputId="le-libro"
          [options]="libros"
          optionLabel="label"
          optionValue="value"
          [ngModel]="libro()"
          (ngModelChange)="libro.set($event)"
          appendTo="body"
        />
        <label for="le-mes" class="font-semibold">Mes</label>
        <p-select
          inputId="le-mes"
          [options]="meses"
          optionLabel="label"
          optionValue="value"
          [ngModel]="mes()"
          (ngModelChange)="mes.set($event)"
          appendTo="body"
        />
        <label for="le-anio" class="font-semibold">Año</label>
        <p-inputnumber
          inputId="le-anio"
          [ngModel]="anio()"
          (ngModelChange)="anio.set($event)"
          [useGrouping]="false"
          [min]="2000"
          [max]="2100"
        />
        <p-button label="Generar" icon="pi pi-cog" [loading]="cargando()" (onClick)="generar()" />
      </div>
      @if (error(); as e) {
        <p class="error" role="alert">{{ e }}</p>
      }
      @if (resultado(); as r) {
        <dl class="ficha">
          <div>
            <dt>Archivo</dt>
            <dd>{{ r.nombreArchivo }}</dd>
          </div>
          <div>
            <dt>Registros</dt>
            <dd>{{ r.registros }}</dd>
          </div>
          <div>
            <dt>Formato</dt>
            <dd>Por validar</dd>
          </div>
        </dl>
        <div class="acciones">
          <p-button
            label="Descargar archivo"
            icon="pi pi-download"
            [disabled]="r.registros === 0"
            (onClick)="descargar(r)"
          />
        </div>
        <h3 class="subseccion">Vista previa</h3>
        @if (r.registros === 0) {
          <p class="text-muted">No hay registros en este mes.</p>
        } @else {
          <pre class="vista-previa" tabindex="0" aria-label="Vista previa del archivo">{{
            r.contenido
          }}</pre>
        }
      }
    </section>
  `,
  styles: ESTILOS_CONTABILIDAD,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LibrosElectronicos {
  private readonly api = inject(ContabilidadApiService);

  protected readonly meses = MESES;
  protected readonly libros = Object.entries(ETIQUETA_LIBRO_ELECTRONICO).map(([value, label]) => ({
    label,
    value,
  }));
  protected readonly libro = signal<LibroElectronicoTipo>('DIARIO');
  protected readonly mes = signal(new Date().getMonth() + 1);
  protected readonly anio = signal(new Date().getFullYear());
  protected readonly resultado = signal<LibroElectronico | null>(null);
  protected readonly cargando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected generar(): void {
    this.error.set(null);
    this.cargando.set(true);
    this.api.libroElectronico(this.libro(), this.anio(), this.mes()).subscribe({
      next: (r) => {
        this.resultado.set(r);
        this.cargando.set(false);
      },
      error: (e: { message?: string }) => {
        this.cargando.set(false);
        this.error.set(e?.message ?? 'No se pudo generar el libro.');
      },
    });
  }

  protected descargar(r: LibroElectronico): void {
    descargarTexto(r.nombreArchivo, r.contenido);
  }
}
