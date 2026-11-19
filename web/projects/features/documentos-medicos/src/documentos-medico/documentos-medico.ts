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
import { Router, RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';

import {
  DocumentosMedicosStore,
  ETIQUETA_ESTADO_DOCUMENTO_MEDICO,
  ETIQUETA_TIPO_DOCUMENTO_MEDICO,
  TIPOS_DOCUMENTO_MEDICO,
  TipoDocumentoMedico,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { SEVERIDAD_ESTADO_DOCUMENTO, SeveridadEtiqueta } from '../documentos-medicos.utils';

@Component({
  selector: 'app-documentos-medico',
  imports: [
    DatePipe,
    FormsModule,
    RouterLink,
    Button,
    Select,
    TableModule,
    Tag,
    EmptyState,
    PageHeader,
  ],
  template: `
    <app-page-header
      title="Documentos médicos"
      subtitle="Certificados, descansos, informes, referencias y epicrisis que emitiste"
      icon="pi-file-check"
    >
      <p-button label="Nuevo documento" icon="pi pi-plus" routerLink="/documentos/nuevo" />
    </app-page-header>

    <section class="panel">
      <div class="filtros">
        <p-select
          [options]="opcionesTipo"
          optionLabel="label"
          optionValue="value"
          [ngModel]="tipo()"
          (ngModelChange)="tipo.set($event)"
          placeholder="Todos los tipos"
          [showClear]="true"
          ariaLabel="Filtrar por tipo de documento"
          appendTo="body"
          styleClass="w-full md:w-16rem"
        />
        <p-select
          [options]="opcionesEstado"
          optionLabel="label"
          optionValue="value"
          [ngModel]="estado()"
          (ngModelChange)="estado.set($event)"
          placeholder="Todos los estados"
          [showClear]="true"
          ariaLabel="Filtrar por estado"
          appendTo="body"
          styleClass="w-full md:w-14rem"
        />
      </div>

      <p-table
        [value]="filtrados()"
        [loading]="store.documentos.loading()"
        [paginator]="filtrados().length > 10"
        [rows]="10"
        [rowHover]="true"
        responsiveLayout="scroll"
      >
        <ng-template #header>
          <tr>
            <th>Documento</th>
            <th>Paciente</th>
            <th>Emisión</th>
            <th>Estado</th>
            <th class="acciones"></th>
          </tr>
        </ng-template>
        <ng-template #body let-d>
          <tr>
            <td>
              <strong>{{ d.tipoEtiqueta }}</strong>
              <small class="block text-muted">{{ d.numero || 'Borrador sin numerar' }}</small>
            </td>
            <td class="sensible">{{ d.pacienteNombre }}</td>
            <td>{{ d.emitidoEn ?? d.creadoEn | date: 'd MMM y, HH:mm' }}</td>
            <td>
              <p-tag [value]="etiquetaEstado[d.estado]" [severity]="severidad[d.estado]" />
            </td>
            <td class="acciones">
              <p-button
                label="Abrir"
                size="small"
                [outlined]="true"
                [ariaLabel]="'Abrir ' + d.tipoEtiqueta + ' ' + (d.numero ?? 'borrador')"
                (onClick)="abrir(d.id)"
              />
            </td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr>
            <td colspan="5">
              <app-empty-state
                icon="pi-file-check"
                title="Sin documentos"
                message="Aún no emitiste documentos médicos con esos filtros."
              />
            </td>
          </tr>
        </ng-template>
      </p-table>
    </section>
  `,
  styles: `
    .filtros {
      display: flex;
      flex-wrap: wrap;
      gap: 0.75rem;
      margin-bottom: 1rem;
    }

    .acciones {
      width: 7rem;
      text-align: right;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DocumentosMedico implements OnInit {
  protected readonly store = inject(DocumentosMedicosStore);
  private readonly router = inject(Router);

  protected readonly tipo = signal<TipoDocumentoMedico | null>(null);
  protected readonly estado = signal<string | null>(null);
  protected readonly etiquetaEstado: Record<string, string> = ETIQUETA_ESTADO_DOCUMENTO_MEDICO;
  protected readonly severidad: Record<string, SeveridadEtiqueta> = SEVERIDAD_ESTADO_DOCUMENTO;
  protected readonly opcionesTipo = TIPOS_DOCUMENTO_MEDICO.map((valor) => ({
    label: ETIQUETA_TIPO_DOCUMENTO_MEDICO[valor],
    value: valor,
  }));
  protected readonly opcionesEstado = Object.entries(ETIQUETA_ESTADO_DOCUMENTO_MEDICO).map(
    ([value, label]) => ({ label, value }),
  );

  protected readonly filtrados = computed(() =>
    (this.store.documentos.value() ?? []).filter(
      (d) =>
        (!this.tipo() || d.tipo === this.tipo()) && (!this.estado() || d.estado === this.estado()),
    ),
  );

  ngOnInit(): void {
    this.store.cargarEmitidos();
  }

  protected abrir(id: string): void {
    void this.router.navigate(['/documentos', id]);
  }
}
