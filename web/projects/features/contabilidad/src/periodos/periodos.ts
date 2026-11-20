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
import { ConfirmationService } from 'primeng/api';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { InputNumber } from 'primeng/inputnumber';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';

import {
  AuthService,
  ContabilidadApiService,
  NotificationService,
  PeriodoResponse,
} from '@tusalud/core';
import { MotivoService } from '@tusalud/ui/dialogo-motivo';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { ESTILOS_CONTABILIDAD } from '../contabilidad.estilos';
import { MESES } from '../contabilidad.utils';

@Component({
  selector: 'app-periodos-contables',
  imports: [
    DatePipe,
    FormsModule,
    Button,
    Dialog,
    InputNumber,
    Select,
    TableModule,
    Tag,
    EmptyState,
    PageHeader,
  ],
  template: `
    <app-page-header
      title="Periodos contables"
      subtitle="Apertura y cierre mensual. Quien registró asientos del periodo no puede cerrarlo."
      icon="pi-calendar"
    >
      @if (esFinanzas()) {
        <p-button label="Abrir periodo" icon="pi pi-plus" (onClick)="dialogo.set(true)" />
      }
    </app-page-header>

    <section class="panel">
      <p-table [value]="lista()" [loading]="cargando()" [rowHover]="true" responsiveLayout="scroll">
        <ng-template #header>
          <tr>
            <th>Periodo</th>
            <th>Estado</th>
            <th class="text-right">Asientos</th>
            <th class="text-right">Borradores</th>
            <th>Cierre</th>
            <th style="width: 14rem"></th>
          </tr>
        </ng-template>
        <ng-template #body let-p>
          <tr>
            <td class="font-semibold">{{ p.etiqueta }}</td>
            <td>
              <p-tag
                [value]="p.estado === 'ABIERTO' ? 'Abierto' : 'Cerrado'"
                [severity]="p.estado === 'ABIERTO' ? 'success' : 'secondary'"
              />
            </td>
            <td class="text-right">{{ p.asientos }}</td>
            <td class="text-right">{{ p.borradores }}</td>
            <td>
              @if (p.cerradoEn) {
                {{ p.cerradoEn | date: 'd MMM y HH:mm' }}
                <small class="block text-muted">{{ p.cerradoPor }}</small>
              }
              @if (p.motivoReapertura) {
                <small class="block text-muted">Reabierto: {{ p.motivoReapertura }}</small>
              }
            </td>
            <td class="derecha">
              @if (p.estado === 'ABIERTO') {
                <p-button
                  label="Cerrar"
                  icon="pi pi-lock"
                  size="small"
                  [outlined]="true"
                  [ariaLabel]="'Cerrar el periodo ' + p.etiqueta"
                  (onClick)="cerrar(p)"
                />
              } @else if (esGerencia()) {
                <p-button
                  label="Reabrir"
                  icon="pi pi-lock-open"
                  size="small"
                  severity="danger"
                  [outlined]="true"
                  [ariaLabel]="'Reabrir el periodo ' + p.etiqueta"
                  (onClick)="reabrir(p)"
                />
              }
            </td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr>
            <td colspan="6">
              <app-empty-state
                icon="pi-calendar"
                title="Sin periodos"
                message="Abre el primer periodo contable para registrar asientos."
              />
            </td>
          </tr>
        </ng-template>
      </p-table>
    </section>

    <p-dialog
      header="Abrir periodo contable"
      [visible]="dialogo()"
      (visibleChange)="dialogo.set($event)"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '26rem', maxWidth: '96vw' }"
    >
      <div class="formulario">
        <label for="pe-mes"
          >Mes
          <p-select
            inputId="pe-mes"
            [options]="meses"
            optionLabel="label"
            optionValue="value"
            [ngModel]="mes()"
            (ngModelChange)="mes.set($event)"
            appendTo="body"
          />
        </label>
        <label for="pe-anio"
          >Año
          <p-inputnumber
            inputId="pe-anio"
            [ngModel]="anio()"
            (ngModelChange)="anio.set($event)"
            [useGrouping]="false"
            [min]="2000"
            [max]="2100"
          />
        </label>
      </div>
      <ng-template #footer>
        <p-button
          label="Cancelar"
          severity="secondary"
          [outlined]="true"
          (onClick)="dialogo.set(false)"
        />
        <p-button label="Abrir" icon="pi pi-check" [loading]="ocupado()" (onClick)="abrir()" />
      </ng-template>
    </p-dialog>
  `,
  styles: ESTILOS_CONTABILIDAD,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PeriodosContables implements OnInit {
  private readonly api = inject(ContabilidadApiService);
  private readonly auth = inject(AuthService);
  private readonly notificaciones = inject(NotificationService);
  private readonly motivos = inject(MotivoService);
  private readonly confirmacion = inject(ConfirmationService);

  protected readonly meses = MESES;
  protected readonly lista = signal<PeriodoResponse[]>([]);
  protected readonly cargando = signal(false);
  protected readonly ocupado = signal(false);
  protected readonly dialogo = signal(false);
  protected readonly mes = signal(new Date().getMonth() + 1);
  protected readonly anio = signal(new Date().getFullYear());
  protected readonly esFinanzas = computed(() => this.auth.isFinanzas());
  protected readonly esGerencia = computed(() => this.auth.isGerencia());

  ngOnInit(): void {
    this.cargar();
  }

  private cargar(): void {
    this.cargando.set(true);
    this.api.periodos().subscribe({
      next: (l) => {
        this.lista.set(l);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
  }

  protected abrir(): void {
    this.ocupado.set(true);
    this.api.abrirPeriodo(this.anio(), this.mes()).subscribe({
      next: (p) => {
        this.ocupado.set(false);
        this.dialogo.set(false);
        this.notificaciones.success(`El periodo ${p.etiqueta} quedó abierto.`);
        this.cargar();
      },
      error: () => this.ocupado.set(false),
    });
  }

  protected cerrar(p: PeriodoResponse): void {
    this.confirmacion.confirm({
      header: 'Cerrar periodo',
      message: `Al cerrar ${p.etiqueta} no se podrán registrar más asientos en él. Solo la gerencia puede reabrirlo.`,
      icon: 'pi pi-lock',
      acceptLabel: 'Cerrar periodo',
      rejectLabel: 'Cancelar',
      rejectButtonProps: { severity: 'secondary', outlined: true },
      accept: () =>
        this.api.cerrarPeriodo(p.id).subscribe(() => {
          this.notificaciones.success(`El periodo ${p.etiqueta} fue cerrado.`);
          this.cargar();
        }),
    });
  }

  protected reabrir(p: PeriodoResponse): void {
    this.motivos
      .solicitar({
        titulo: 'Reabrir periodo',
        descripcion: `Se reabrirá ${p.etiqueta} y quedará registrado quién lo hizo y por qué.`,
        etiqueta: 'Motivo de la reapertura',
        confirmar: 'Reabrir periodo',
        severidad: 'danger',
        minimo: 5,
      })
      .subscribe((motivo) =>
        this.api.reabrirPeriodo(p.id, motivo).subscribe(() => {
          this.notificaciones.success(`El periodo ${p.etiqueta} fue reabierto.`);
          this.cargar();
        }),
      );
  }
}
