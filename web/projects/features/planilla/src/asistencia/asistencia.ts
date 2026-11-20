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
import { Button } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';
import { Dialog } from 'primeng/dialog';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';

import {
  AsistenciaDiaResponse,
  DestinoExtra,
  EmpleadoResponse,
  ETIQUETA_ESTADO_ASISTENCIA,
  EstadoAsistencia,
  NotificationService,
  PlanillaApiService,
  RrhhApiService,
  TipoMarcacion,
  toIsoDate,
} from '@tusalud/core';
import { BotonExportar } from '@tusalud/ui/boton-exportar';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { ESTILOS_PLANILLA } from '../planilla.estilos';
import { inicioDeMes } from '../planilla.utils';

function severidad(estado: EstadoAsistencia): 'success' | 'warn' | 'danger' | 'info' | 'secondary' {
  switch (estado) {
    case 'PRESENTE':
      return 'success';
    case 'TARDANZA':
    case 'SIN_SALIDA':
      return 'warn';
    case 'FALTA':
      return 'danger';
    case 'AUSENCIA_APROBADA':
      return 'info';
    default:
      return 'secondary';
  }
}

@Component({
  selector: 'app-asistencia-personal',
  imports: [
    DatePipe,
    FormsModule,
    Button,
    DatePicker,
    Dialog,
    InputNumber,
    InputText,
    Select,
    TableModule,
    Tag,
    BotonExportar,
    EmptyState,
    PageHeader,
  ],
  template: `
    <app-page-header
      title="Asistencia del personal"
      subtitle="Se consolida desde los turnos, las ausencias aprobadas y las marcaciones."
      icon="pi-clock"
    >
      <app-boton-exportar
        tipo="asistencia-personal"
        formato="XLSX"
        [parametros]="parametros()"
        etiqueta="Exportar a Excel"
      />
      <p-button
        label="Marcación manual"
        icon="pi pi-plus"
        severity="secondary"
        [outlined]="true"
        (onClick)="abrirMarcacion()"
      />
      <p-button
        label="Consolidar"
        icon="pi pi-sync"
        [loading]="consolidando()"
        (onClick)="consolidar()"
      />
    </app-page-header>

    <section class="panel">
      <div class="filtros">
        <label for="as-desde"
          >Desde
          <p-datepicker
            inputId="as-desde"
            [ngModel]="desde()"
            (ngModelChange)="cambiarDesde($event)"
            dateFormat="dd/mm/yy"
          />
        </label>
        <label for="as-hasta"
          >Hasta
          <p-datepicker
            inputId="as-hasta"
            [ngModel]="hasta()"
            (ngModelChange)="cambiarHasta($event)"
            dateFormat="dd/mm/yy"
          />
        </label>
        <label for="as-emp"
          >Empleado
          <p-select
            inputId="as-emp"
            [options]="empleados()"
            optionLabel="nombreCompleto"
            optionValue="id"
            [filter]="true"
            [showClear]="true"
            placeholder="Todos"
            [ngModel]="empleadoId()"
            (ngModelChange)="cambiarEmpleado($event)"
          />
        </label>
      </div>

      <p-table
        [value]="lista()"
        [loading]="cargando()"
        [rowHover]="true"
        [paginator]="lista().length > 20"
        [rows]="20"
        responsiveLayout="scroll"
      >
        <ng-template #header>
          <tr>
            <th>Fecha</th>
            <th>Empleado</th>
            <th>Estado</th>
            <th>Turno</th>
            <th>Entrada</th>
            <th>Salida</th>
            <th class="text-right">Tardanza</th>
            <th class="text-right">Extra</th>
            <th style="width: 5rem"></th>
          </tr>
        </ng-template>
        <ng-template #body let-d>
          <tr>
            <td>{{ d.fecha | date: 'EEE d MMM' }}</td>
            <td class="font-semibold">{{ d.empleadoNombre }}</td>
            <td>
              <p-tag [value]="etiqueta(d.estado)" [severity]="severidad(d.estado)" />
              @if (d.faltaJustificada) {
                <small class="block text-muted">Justificada</small>
              }
              @if (d.ajustada) {
                <small class="block text-muted">Ajustada</small>
              }
            </td>
            <td>
              @if (d.programadaInicio) {
                {{ d.programadaInicio.substring(0, 5) }} a {{ d.programadaFin?.substring(0, 5) }}
              } @else {
                Sin turno
              }
            </td>
            <td>{{ d.entrada ? (d.entrada | date: 'HH:mm') : '' }}</td>
            <td>{{ d.salida ? (d.salida | date: 'HH:mm') : '' }}</td>
            <td class="text-right">{{ d.minutosTardanza }} min</td>
            <td class="text-right">
              {{ d.minutosExtra }} min
              @if (d.minutosExtra > 0) {
                <small class="block text-muted">{{
                  d.extraDestino === 'PAGO' ? 'Se paga' : 'Se compensa'
                }}</small>
              }
            </td>
            <td class="derecha">
              <p-button
                icon="pi pi-pencil"
                [rounded]="true"
                [text]="true"
                [ariaLabel]="'Ajustar el día de ' + d.empleadoNombre"
                (onClick)="abrirAjuste(d)"
              />
            </td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr>
            <td colspan="9">
              <app-empty-state
                icon="pi-clock"
                title="Sin registros"
                message="Consolida el rango para generar la asistencia desde los turnos y marcaciones."
              />
            </td>
          </tr>
        </ng-template>
      </p-table>
    </section>

    <p-dialog
      header="Ajustar asistencia"
      [visible]="!!ajustando()"
      (visibleChange)="$event ? null : ajustando.set(null)"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '30rem', maxWidth: '96vw' }"
    >
      @if (ajustando(); as a) {
        <p class="aviso">
          {{ a.empleadoNombre }}, {{ a.fecha | date: 'd MMM y' }}. Todo ajuste queda registrado con
          su justificación.
        </p>
        <div class="formulario">
          @if (a.estado === 'FALTA') {
            <label for="aj-just" class="casilla">
              <input
                id="aj-just"
                type="checkbox"
                [checked]="ajFalta()"
                (change)="ajFalta.set($any($event.target).checked)"
              />
              Justificar la falta
            </label>
          }
          <div class="dos">
            <label for="aj-tard"
              >Minutos de tardanza
              <p-inputnumber
                inputId="aj-tard"
                [ngModel]="ajTardanza()"
                (ngModelChange)="ajTardanza.set($event)"
                [min]="0"
                [max]="1440"
              />
            </label>
            <label for="aj-ext"
              >Minutos extra
              <p-inputnumber
                inputId="aj-ext"
                [ngModel]="ajExtra()"
                (ngModelChange)="ajExtra.set($event)"
                [min]="0"
                [max]="1440"
              />
            </label>
          </div>
          <label for="aj-dest"
            >Horas extra
            <p-select
              inputId="aj-dest"
              [options]="destinos"
              optionLabel="label"
              optionValue="value"
              [ngModel]="ajDestino()"
              (ngModelChange)="ajDestino.set($event)"
              appendTo="body"
            />
          </label>
          <label for="aj-motivo"
            >Justificación
            <input
              pInputText
              id="aj-motivo"
              maxlength="300"
              [ngModel]="ajMotivo()"
              (ngModelChange)="ajMotivo.set($event)"
            />
          </label>
        </div>
      }
      <ng-template #footer>
        <p-button
          label="Cancelar"
          severity="secondary"
          [outlined]="true"
          (onClick)="ajustando.set(null)"
        />
        <p-button
          label="Guardar ajuste"
          icon="pi pi-check"
          [loading]="ocupado()"
          [disabled]="ajMotivo().trim().length < 5"
          (onClick)="guardarAjuste()"
        />
      </ng-template>
    </p-dialog>

    <p-dialog
      header="Marcación manual"
      [visible]="dialogoMarcacion()"
      (visibleChange)="dialogoMarcacion.set($event)"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '30rem', maxWidth: '96vw' }"
    >
      <div class="formulario">
        <label for="mm-emp"
          >Empleado
          <p-select
            inputId="mm-emp"
            [options]="empleados()"
            optionLabel="nombreCompleto"
            optionValue="id"
            [filter]="true"
            [ngModel]="mmEmpleado()"
            (ngModelChange)="mmEmpleado.set($event)"
            appendTo="body"
          />
        </label>
        <div class="dos">
          <label for="mm-tipo"
            >Tipo
            <p-select
              inputId="mm-tipo"
              [options]="tiposMarcacion"
              optionLabel="label"
              optionValue="value"
              [ngModel]="mmTipo()"
              (ngModelChange)="mmTipo.set($event)"
              appendTo="body"
            />
          </label>
          <label for="mm-fecha"
            >Fecha y hora
            <p-datepicker
              inputId="mm-fecha"
              [ngModel]="mmFecha()"
              (ngModelChange)="mmFecha.set($event)"
              [showTime]="true"
              [maxDate]="hoy"
              dateFormat="dd/mm/yy"
              appendTo="body"
            />
          </label>
        </div>
        <label for="mm-motivo"
          >Motivo de la marcación manual
          <input
            pInputText
            id="mm-motivo"
            maxlength="200"
            [ngModel]="mmMotivo()"
            (ngModelChange)="mmMotivo.set($event)"
          />
        </label>
      </div>
      <ng-template #footer>
        <p-button
          label="Cancelar"
          severity="secondary"
          [outlined]="true"
          (onClick)="dialogoMarcacion.set(false)"
        />
        <p-button
          label="Registrar"
          icon="pi pi-check"
          [loading]="ocupado()"
          [disabled]="!mmEmpleado() || !mmFecha() || mmMotivo().trim().length < 3"
          (onClick)="registrarMarcacion()"
        />
      </ng-template>
    </p-dialog>
  `,
  styles: [
    ESTILOS_PLANILLA,
    `
      .casilla {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        font-weight: 500;
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AsistenciaPersonal implements OnInit {
  private readonly api = inject(PlanillaApiService);
  private readonly rrhh = inject(RrhhApiService);
  private readonly notificaciones = inject(NotificationService);

  protected readonly hoy = new Date();
  protected readonly destinos: { label: string; value: DestinoExtra }[] = [
    { label: 'Se paga con sobretasa', value: 'PAGO' },
    { label: 'Se compensa con descanso', value: 'COMPENSACION' },
  ];
  protected readonly tiposMarcacion: { label: string; value: TipoMarcacion }[] = [
    { label: 'Entrada', value: 'ENTRADA' },
    { label: 'Salida', value: 'SALIDA' },
  ];
  protected readonly desde = signal<Date>(inicioDeMes());
  protected readonly hasta = signal<Date>(new Date());
  protected readonly empleadoId = signal<string | null>(null);
  protected readonly empleados = signal<EmpleadoResponse[]>([]);
  protected readonly lista = signal<AsistenciaDiaResponse[]>([]);
  protected readonly cargando = signal(false);
  protected readonly consolidando = signal(false);
  protected readonly ocupado = signal(false);
  protected readonly ajustando = signal<AsistenciaDiaResponse | null>(null);
  protected readonly ajFalta = signal(false);
  protected readonly ajTardanza = signal(0);
  protected readonly ajExtra = signal(0);
  protected readonly ajDestino = signal<DestinoExtra>('PAGO');
  protected readonly ajMotivo = signal('');
  protected readonly dialogoMarcacion = signal(false);
  protected readonly mmEmpleado = signal<string | null>(null);
  protected readonly mmTipo = signal<TipoMarcacion>('ENTRADA');
  protected readonly mmFecha = signal<Date | null>(null);
  protected readonly mmMotivo = signal('');
  protected readonly parametros = computed(() => ({
    desde: toIsoDate(this.desde()),
    hasta: toIsoDate(this.hasta()),
    empleadoId: this.empleadoId(),
  }));

  ngOnInit(): void {
    this.rrhh.empleados({}).subscribe((e) => this.empleados.set(e));
    this.cargar();
  }

  protected etiqueta(estado: EstadoAsistencia): string {
    return ETIQUETA_ESTADO_ASISTENCIA[estado];
  }

  protected severidad(estado: EstadoAsistencia) {
    return severidad(estado);
  }

  protected cambiarDesde(valor: Date): void {
    this.desde.set(valor);
    this.cargar();
  }

  protected cambiarHasta(valor: Date): void {
    this.hasta.set(valor);
    this.cargar();
  }

  protected cambiarEmpleado(valor: string | null): void {
    this.empleadoId.set(valor);
    this.cargar();
  }

  private cargar(): void {
    this.cargando.set(true);
    this.api
      .diasAsistencia(
        toIsoDate(this.desde()),
        toIsoDate(this.hasta()),
        this.empleadoId() ?? undefined,
      )
      .subscribe({
        next: (l) => {
          this.lista.set(l);
          this.cargando.set(false);
        },
        error: () => this.cargando.set(false),
      });
  }

  protected consolidar(): void {
    this.consolidando.set(true);
    this.api
      .consolidar(toIsoDate(this.desde()), toIsoDate(this.hasta()), this.empleadoId())
      .subscribe({
        next: (r) => {
          this.consolidando.set(false);
          this.notificaciones.success(
            `Se consolidaron ${r.dias} días de ${r.empleados} empleados.` +
              (r.ajustadasOmitidas ? ` Se conservaron ${r.ajustadasOmitidas} días ajustados.` : ''),
          );
          this.cargar();
        },
        error: () => this.consolidando.set(false),
      });
  }

  protected abrirAjuste(d: AsistenciaDiaResponse): void {
    this.ajFalta.set(d.faltaJustificada);
    this.ajTardanza.set(d.minutosTardanza);
    this.ajExtra.set(d.minutosExtra);
    this.ajDestino.set(d.extraDestino);
    this.ajMotivo.set('');
    this.ajustando.set(d);
  }

  protected guardarAjuste(): void {
    const d = this.ajustando();
    if (!d) {
      return;
    }
    this.ocupado.set(true);
    this.api
      .ajustarDia(d.id, {
        faltaJustificada: d.estado === 'FALTA' ? this.ajFalta() : null,
        minutosTardanza: this.ajTardanza(),
        minutosExtra: this.ajExtra(),
        extraDestino: this.ajDestino(),
        justificacion: this.ajMotivo().trim(),
      })
      .subscribe({
        next: () => {
          this.ocupado.set(false);
          this.ajustando.set(null);
          this.notificaciones.success('El ajuste quedó registrado.');
          this.cargar();
        },
        error: () => this.ocupado.set(false),
      });
  }

  protected abrirMarcacion(): void {
    this.mmEmpleado.set(null);
    this.mmTipo.set('ENTRADA');
    this.mmFecha.set(new Date());
    this.mmMotivo.set('');
    this.dialogoMarcacion.set(true);
  }

  protected registrarMarcacion(): void {
    const fecha = this.mmFecha();
    const empleado = this.mmEmpleado();
    if (!fecha || !empleado) {
      return;
    }
    const pad = (n: number) => n.toString().padStart(2, '0');
    const iso = `${toIsoDate(fecha)}T${pad(fecha.getHours())}:${pad(fecha.getMinutes())}:00`;
    this.ocupado.set(true);
    this.api
      .registrarMarcacion({
        empleadoId: empleado,
        tipo: this.mmTipo(),
        fechaHora: iso,
        observacion: this.mmMotivo().trim(),
      })
      .subscribe({
        next: () => {
          this.ocupado.set(false);
          this.dialogoMarcacion.set(false);
          this.notificaciones.success('La marcación quedó registrada. Consolida para reflejarla.');
        },
        error: () => this.ocupado.set(false),
      });
  }
}
