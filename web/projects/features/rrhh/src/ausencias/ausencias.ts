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
import { Dialog } from 'primeng/dialog';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';
import { SelectButton } from 'primeng/selectbutton';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { Textarea } from 'primeng/textarea';

import {
  AREAS_LABORALES,
  AuthService,
  AusenciaResponse,
  ESTADOS_AUSENCIA,
  ETIQUETA_AREA_LABORAL,
  ETIQUETA_ESTADO_AUSENCIA,
  ETIQUETA_TIPO_AUSENCIA,
  NotificationService,
  RrhhStore,
  toIsoDate,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { severidadAusencia } from '../shared/ausencia';

interface FilaCalendario {
  empleado: string;
  area: string;
  dias: (AusenciaResponse | null)[];
}

@Component({
  selector: 'app-ausencias',
  imports: [
    DatePipe,
    FormsModule,
    Button,
    Dialog,
    Message,
    Select,
    SelectButton,
    TableModule,
    Tag,
    Textarea,
    EmptyState,
    PageHeader,
  ],
  templateUrl: './ausencias.html',
  styleUrl: './ausencias.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Ausencias implements OnInit {
  protected readonly store = inject(RrhhStore);
  private readonly auth = inject(AuthService);
  private readonly notifications = inject(NotificationService);

  protected readonly gestiona = computed(() => this.auth.isRrhh());
  protected readonly etiquetaArea = ETIQUETA_AREA_LABORAL;
  protected readonly etiquetaTipo = ETIQUETA_TIPO_AUSENCIA;
  protected readonly etiquetaEstado = ETIQUETA_ESTADO_AUSENCIA;
  protected readonly severidad = severidadAusencia;
  protected readonly vistas = [
    { label: 'Solicitudes', value: 'lista' },
    { label: 'Calendario', value: 'calendario' },
  ];
  protected readonly estados = ESTADOS_AUSENCIA.map((e) => ({
    label: ETIQUETA_ESTADO_AUSENCIA[e],
    value: e,
  }));
  protected readonly areas = AREAS_LABORALES.map((a) => ({
    label: ETIQUETA_AREA_LABORAL[a],
    value: a,
  }));

  protected readonly vista = signal<'lista' | 'calendario'>('lista');
  protected readonly estado = signal<string | null>('SOLICITADA');
  protected readonly area = signal<string | null>(null);
  protected readonly mes = signal(new Date(new Date().getFullYear(), new Date().getMonth(), 1));

  protected readonly resolviendo = signal<{
    ausencia: AusenciaResponse;
    accion: 'aprobar' | 'rechazar';
  } | null>(null);
  protected readonly motivo = signal('');
  protected readonly procesando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly diasMes = computed(() => {
    const inicio = this.mes();
    const total = new Date(inicio.getFullYear(), inicio.getMonth() + 1, 0).getDate();
    return Array.from(
      { length: total },
      (_, i) => new Date(inicio.getFullYear(), inicio.getMonth(), i + 1),
    );
  });

  protected readonly filasCalendario = computed<FilaCalendario[]>(() => {
    const dias = this.diasMes();
    const porEmpleado = new Map<string, FilaCalendario>();
    for (const ausencia of this.store.ausencias.value() ?? []) {
      if (ausencia.estado === 'RECHAZADA' || ausencia.estado === 'CANCELADA') {
        continue;
      }
      let fila = porEmpleado.get(ausencia.empleadoId);
      if (!fila) {
        fila = {
          empleado: ausencia.empleadoNombre,
          area: this.etiquetaArea[ausencia.area] ?? ausencia.area,
          dias: dias.map(() => null),
        };
        porEmpleado.set(ausencia.empleadoId, fila);
      }
      dias.forEach((dia, i) => {
        const iso = toIsoDate(dia);
        if (iso >= ausencia.fechaInicio && iso <= ausencia.fechaFin) {
          fila.dias[i] = ausencia;
        }
      });
    }
    return [...porEmpleado.values()].sort((a, b) => a.empleado.localeCompare(b.empleado));
  });

  ngOnInit(): void {
    this.cargar();
  }

  protected cambiarVista(vista: 'lista' | 'calendario' | null): void {
    if (vista) {
      this.vista.set(vista);
      this.cargar();
    }
  }

  protected moverMes(delta: number): void {
    const actual = this.mes();
    this.mes.set(new Date(actual.getFullYear(), actual.getMonth() + delta, 1));
    this.cargar();
  }

  protected cargar(): void {
    if (this.vista() === 'lista') {
      this.store.cargarAusencias({ estado: this.estado(), area: this.area() });
      return;
    }
    const dias = this.diasMes();
    this.store.cargarAusencias({
      area: this.area(),
      desde: toIsoDate(dias[0]),
      hasta: toIsoDate(dias[dias.length - 1]),
    });
  }

  protected abrir(ausencia: AusenciaResponse, accion: 'aprobar' | 'rechazar'): void {
    this.resolviendo.set({ ausencia, accion });
    this.motivo.set('');
    this.error.set(null);
  }

  protected confirmar(): void {
    const actual = this.resolviendo();
    if (!actual) {
      return;
    }
    if (actual.accion === 'rechazar' && !this.motivo().trim()) {
      this.error.set('Indica el motivo del rechazo.');
      return;
    }
    this.procesando.set(true);
    this.store
      .resolverAusencia(actual.ausencia.id, actual.accion, this.motivo().trim() || null)
      .subscribe({
        next: () => {
          this.procesando.set(false);
          this.resolviendo.set(null);
          this.notifications.success(
            actual.accion === 'aprobar'
              ? 'La solicitud fue aprobada.'
              : 'La solicitud fue rechazada.',
          );
          this.cargar();
        },
        error: (error: { message: string }) => {
          this.procesando.set(false);
          this.error.set(error.message);
        },
      });
  }
}
