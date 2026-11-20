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
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Tag } from 'primeng/tag';

import { DescargaService, RecetaResumen, RecetasApiService } from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { ETIQUETA_ESTADO_RECETA, SEVERIDAD_ESTADO_RECETA, textoVigencia } from '../recetas.utils';

@Component({
  selector: 'app-recetas-medico',
  imports: [
    DatePipe,
    FormsModule,
    RouterLink,
    Button,
    InputText,
    Message,
    Tag,
    EmptyState,
    PageHeader,
  ],
  template: `
    <app-page-header
      title="Recetas electrónicas"
      subtitle="Las recetas que emitiste, con su estado de dispensación y su firma"
      icon="pi-file-edit"
    />
    @if (error()) {
      <p-message severity="error">{{ error() }}</p-message>
    }
    <div class="panel">
      <label for="filtro-recetas" class="filtro">
        Buscar por número, paciente o DNI
        <input
          pInputText
          id="filtro-recetas"
          name="filtro"
          [ngModel]="filtro()"
          (ngModelChange)="filtro.set($event)"
        />
      </label>
      @if (cargando()) {
        <p class="text-muted" role="status">Cargando recetas…</p>
      } @else if (visibles().length === 0) {
        <app-empty-state
          icon="pi-file-edit"
          title="Sin recetas"
          message="Emite una receta electrónica desde la atención de una cita, con el botón Receta electrónica."
        />
      } @else {
        <div class="tabla">
          <table>
            <caption class="sr-only">
              Recetas electrónicas emitidas
            </caption>
            <thead>
              <tr>
                <th scope="col">Número</th>
                <th scope="col">Paciente</th>
                <th scope="col">Emisión</th>
                <th scope="col">Vigencia</th>
                <th scope="col">Medicamentos</th>
                <th scope="col">Estado</th>
                <th scope="col"><span class="sr-only">Acciones</span></th>
              </tr>
            </thead>
            <tbody>
              @for (r of visibles(); track r.id) {
                <tr>
                  <th scope="row">
                    {{ r.numero }}
                    @if (r.controlEspecial) {
                      <small class="control">Control especial</small>
                    }
                  </th>
                  <td>
                    {{ r.pacienteNombre
                    }}<small class="block text-muted">DNI {{ r.pacienteDni }}</small>
                  </td>
                  <td>{{ r.emitidaEn | date: 'd MMM y, HH:mm' }}</td>
                  <td>{{ vigencia(r) }}</td>
                  <td>{{ r.medicamentos }}</td>
                  <td>
                    <p-tag [value]="etiqueta[r.estado]" [severity]="severidad[r.estado]" />
                  </td>
                  <td class="acciones">
                    <p-button
                      label="Ver"
                      size="small"
                      [outlined]="true"
                      [routerLink]="['/recetas', r.id]"
                      [ariaLabel]="'Ver la receta ' + r.numero"
                    />
                    <p-button
                      icon="pi pi-download"
                      size="small"
                      severity="secondary"
                      [text]="true"
                      [loading]="descargando() === r.id"
                      [ariaLabel]="'Descargar el PDF de la receta ' + r.numero"
                      (onClick)="descargar(r)"
                    />
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </div>
  `,
  styles: `
    .sr-only {
      position: absolute;
      width: 1px;
      height: 1px;
      padding: 0;
      margin: -1px;
      overflow: hidden;
      clip: rect(0, 0, 0, 0);
      white-space: nowrap;
      border: 0;
    }
    .filtro {
      display: grid;
      gap: 0.25rem;
      max-width: 24rem;
      font-weight: 600;
      margin-bottom: 1rem;
    }

    .tabla {
      overflow-x: auto;
    }

    table {
      width: 100%;
      border-collapse: collapse;
      font-size: 0.92rem;
    }

    th,
    td {
      text-align: left;
      padding: 0.45rem 0.6rem;
      border-bottom: 1px solid var(--p-surface-200, #e2e8f0);
    }

    .block {
      display: block;
    }

    .control {
      display: block;
      color: #b45309;
      font-weight: 600;
    }

    .acciones {
      display: flex;
      gap: 0.25rem;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RecetasMedico implements OnInit {
  private readonly api = inject(RecetasApiService);
  private readonly descargas = inject(DescargaService);

  protected readonly etiqueta = ETIQUETA_ESTADO_RECETA;
  protected readonly severidad = SEVERIDAD_ESTADO_RECETA;
  protected readonly recetas = signal<RecetaResumen[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly filtro = signal('');
  protected readonly descargando = signal<string | null>(null);

  protected readonly visibles = computed(() => {
    const q = this.filtro().trim().toLowerCase();
    if (!q) {
      return this.recetas();
    }
    return this.recetas().filter(
      (r) =>
        r.numero.toLowerCase().includes(q) ||
        r.pacienteNombre.toLowerCase().includes(q) ||
        r.pacienteDni.includes(q),
    );
  });

  ngOnInit(): void {
    this.api.emitidas().subscribe({
      next: (lista) => {
        this.recetas.set(lista);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar las recetas');
        this.cargando.set(false);
      },
    });
  }

  protected vigencia(r: RecetaResumen): string {
    return r.estado === 'ANULADA' ? 'Anulada' : textoVigencia(r.vigente, r.vigenteHasta);
  }

  protected descargar(r: RecetaResumen): void {
    this.descargando.set(r.id);
    this.api.pdf(r.id).subscribe({
      next: (archivo) => {
        this.descargando.set(null);
        this.descargas.guardar(archivo);
      },
      error: () => this.descargando.set(null),
    });
  }
}
