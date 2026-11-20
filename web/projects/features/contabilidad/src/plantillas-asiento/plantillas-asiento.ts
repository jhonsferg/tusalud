import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { Checkbox } from 'primeng/checkbox';
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';

import {
  AuthService,
  ContabilidadApiService,
  CuentaContableResponse,
  ETIQUETA_CONCEPTO_MONTO,
  ETIQUETA_EVENTO_CONTABLE,
  NotificationService,
  PlantillaAsientoResponse,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { ESTILOS_CONTABILIDAD } from '../contabilidad.estilos';

@Component({
  selector: 'app-plantillas-asiento',
  imports: [
    FormsModule,
    Button,
    Checkbox,
    Dialog,
    InputText,
    Select,
    TableModule,
    Tag,
    EmptyState,
    PageHeader,
  ],
  template: `
    <app-page-header
      title="Plantillas de asiento"
      subtitle="Cuentas que usa el sistema al contabilizar cobros, compras y pagos automáticamente"
      icon="pi-sliders-h"
    />

    <section class="panel">
      <p-table [value]="plantillas()" [rowHover]="true" responsiveLayout="scroll">
        <ng-template #header>
          <tr>
            <th>Hecho</th>
            <th>Asiento que genera</th>
            <th>Estado</th>
            <th style="width: 6rem"></th>
          </tr>
        </ng-template>
        <ng-template #body let-p>
          <tr>
            <td>
              <span class="font-semibold">{{ eventos[p.evento] }}</span>
              <small class="block text-muted">{{ p.nombre }}</small>
            </td>
            <td>
              @for (l of p.lineas; track l.id) {
                <div>
                  <span class="negrita">{{ l.lado === 'DEBE' ? 'Debe' : 'Haber' }}</span>
                  {{ l.cuentaCodigo }} {{ l.cuentaNombre }}
                  <small class="text-muted">({{ conceptos[l.concepto] }})</small>
                </div>
              }
            </td>
            <td>
              <p-tag
                [value]="p.activa ? 'Activa' : 'Inactiva'"
                [severity]="p.activa ? 'success' : 'secondary'"
              />
            </td>
            <td class="derecha">
              @if (esFinanzas()) {
                <p-button
                  icon="pi pi-pencil"
                  size="small"
                  [text]="true"
                  [rounded]="true"
                  [ariaLabel]="'Editar la plantilla ' + eventos[p.evento]"
                  (onClick)="abrir(p)"
                />
              }
            </td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr>
            <td colspan="4">
              <app-empty-state
                icon="pi-sliders-h"
                title="Sin plantillas"
                message="No hay plantillas de asiento."
              />
            </td>
          </tr>
        </ng-template>
      </p-table>
    </section>

    <p-dialog
      [header]="'Plantilla: ' + (editada() ? eventos[editada()!.evento] : '')"
      [visible]="dialogo()"
      (visibleChange)="dialogo.set($event)"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '44rem', maxWidth: '96vw' }"
    >
      @if (editada(); as p) {
        <div class="formulario">
          <label for="pl-nombre"
            >Nombre
            <input
              pInputText
              id="pl-nombre"
              [ngModel]="nombre()"
              (ngModelChange)="nombre.set($event)"
              maxlength="120"
              class="w-full"
            />
          </label>
          @for (l of p.lineas; track l.id) {
            <label [for]="'pl-' + l.id"
              >{{ l.lado === 'DEBE' ? 'Debe' : 'Haber' }}, {{ conceptos[l.concepto] }}
              <p-select
                [inputId]="'pl-' + l.id"
                [options]="cuentasMovimiento()"
                optionLabel="etiqueta"
                optionValue="id"
                [ngModel]="cuentasLinea()[l.id]"
                (ngModelChange)="elegir(l.id, $event)"
                [filter]="true"
                filterBy="etiqueta"
                appendTo="body"
              />
            </label>
          }
          <label class="flex align-items-center gap-2" for="pl-activa">
            <p-checkbox
              inputId="pl-activa"
              [ngModel]="activa()"
              (ngModelChange)="activa.set($event)"
              [binary]="true"
            />
            Plantilla activa
          </label>
          <p class="aviso" role="note">
            Si desactivas una plantilla, los hechos de ese tipo quedan pendientes de contabilizar
            hasta que la actives de nuevo.
          </p>
        </div>
      }
      <ng-template #footer>
        <p-button
          label="Cancelar"
          severity="secondary"
          [outlined]="true"
          (onClick)="dialogo.set(false)"
        />
        <p-button label="Guardar" icon="pi pi-check" [loading]="ocupado()" (onClick)="guardar()" />
      </ng-template>
    </p-dialog>
  `,
  styles: ESTILOS_CONTABILIDAD,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PlantillasAsiento implements OnInit {
  private readonly api = inject(ContabilidadApiService);
  private readonly auth = inject(AuthService);
  private readonly notificaciones = inject(NotificationService);

  protected readonly eventos: Record<string, string> = ETIQUETA_EVENTO_CONTABLE;
  protected readonly conceptos: Record<string, string> = ETIQUETA_CONCEPTO_MONTO;
  protected readonly plantillas = signal<PlantillaAsientoResponse[]>([]);
  protected readonly cuentas = signal<CuentaContableResponse[]>([]);
  protected readonly dialogo = signal(false);
  protected readonly editada = signal<PlantillaAsientoResponse | null>(null);
  protected readonly nombre = signal('');
  protected readonly activa = signal(true);
  protected readonly cuentasLinea = signal<Record<string, string>>({});
  protected readonly ocupado = signal(false);
  protected readonly esFinanzas = computed(() => this.auth.isFinanzas());
  protected readonly cuentasMovimiento = computed(() =>
    this.cuentas()
      .filter((c) => c.activa && c.aceptaMovimientos)
      .map((c) => ({ id: c.id, etiqueta: `${c.codigo} ${c.nombre}` })),
  );

  ngOnInit(): void {
    this.cargar();
    this.api.cuentas(true).subscribe((c) => this.cuentas.set(c));
  }

  private cargar(): void {
    this.api.plantillas().subscribe((p) => this.plantillas.set(p));
  }

  protected abrir(p: PlantillaAsientoResponse): void {
    this.editada.set(p);
    this.nombre.set(p.nombre);
    this.activa.set(p.activa);
    this.cuentasLinea.set(Object.fromEntries(p.lineas.map((l) => [l.id, l.cuentaId])));
    this.dialogo.set(true);
  }

  protected elegir(lineaId: string, cuentaId: string): void {
    this.cuentasLinea.update((actual) => ({ ...actual, [lineaId]: cuentaId }));
  }

  protected guardar(): void {
    const p = this.editada();
    if (!p || !this.nombre().trim()) {
      return;
    }
    this.ocupado.set(true);
    this.api
      .actualizarPlantilla(p.id, {
        nombre: this.nombre().trim(),
        activa: this.activa(),
        cuentas: p.lineas.map((l) => ({ lineaId: l.id, cuentaId: this.cuentasLinea()[l.id] })),
      })
      .subscribe({
        next: () => {
          this.ocupado.set(false);
          this.dialogo.set(false);
          this.notificaciones.success('La plantilla fue guardada.');
          this.cargar();
        },
        error: () => this.ocupado.set(false),
      });
  }
}
