import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ConfirmationService } from 'primeng/api';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';
import { Tag } from 'primeng/tag';
import { TableModule } from 'primeng/table';
import { InputText } from 'primeng/inputtext';

import {
  ETIQUETA_ESTADO_TURNO,
  ETIQUETA_PRIORIDAD,
  PacienteMostradorResponse,
  PrioridadTurno,
  TurnoResponse,
} from '@tusalud/app/core/models';
import { NotificationService } from '@tusalud/app/core/services/notification.service';
import { MedicosStore } from '@tusalud/app/core/state/medicos.store';
import { RecepcionStore } from '@tusalud/app/core/state/recepcion.store';
import { EmptyState } from '@tusalud/app/shared/components/empty-state/empty-state';
import { Kpi } from '@tusalud/app/shared/components/kpi/kpi';
import { PacienteBuscador } from '@tusalud/app/shared/components/paciente-buscador/paciente-buscador';
import { PageHeader } from '@tusalud/app/shared/components/page-header/page-header';

const REFRESCO_MS = 15_000;

@Component({
  selector: 'app-recepcion-panel',
  imports: [
    DatePipe,
    FormsModule,
    RouterLink,
    Button,
    Dialog,
    InputText,
    Message,
    Select,
    TableModule,
    Tag,
    EmptyState,
    Kpi,
    PacienteBuscador,
    PageHeader,
  ],
  templateUrl: './recepcion-panel.html',
  styleUrl: './recepcion-panel.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RecepcionPanel implements OnInit {
  protected readonly store = inject(RecepcionStore);
  protected readonly medicos = inject(MedicosStore);
  private readonly notifications = inject(NotificationService);
  private readonly confirmation = inject(ConfirmationService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly etiquetaEstado = ETIQUETA_ESTADO_TURNO;
  protected readonly etiquetaPrioridad = ETIQUETA_PRIORIDAD;
  protected readonly prioridades = (Object.keys(ETIQUETA_PRIORIDAD) as PrioridadTurno[]).map(
    (valor) => ({
      label: ETIQUETA_PRIORIDAD[valor],
      value: valor,
    }),
  );
  protected readonly busquedaCitas = signal('');

  protected readonly turnoVisible = signal(false);
  protected readonly pacienteTurno = signal<PacienteMostradorResponse | null>(null);
  protected readonly prioridadTurno = signal<PrioridadTurno>('NORMAL');
  protected readonly medicoTurno = signal<number | null>(null);
  protected readonly motivoTurno = signal('');
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.cargar();
    this.medicos.load(null);
    const temporizador = setInterval(() => this.cargar(), REFRESCO_MS);
    this.destroyRef.onDestroy(() => clearInterval(temporizador));
  }

  protected cargar(): void {
    this.store.cargarResumen();
    this.store.cargarCola();
    this.store.cargarCitasHoy(this.busquedaCitas().trim() || null);
  }

  protected severidad(turno: TurnoResponse): 'success' | 'info' | 'warn' | 'danger' | 'secondary' {
    switch (turno.estado) {
      case 'EN_ESPERA':
        return turno.prioridad === 'URGENTE' ? 'danger' : 'warn';
      case 'LLAMADO':
      case 'EN_ATENCION':
        return 'info';
      case 'ATENDIDO':
        return 'success';
      default:
        return 'secondary';
    }
  }

  protected accion(turno: TurnoResponse, accion: 'llamar' | 'ausente' | 'cancelar'): void {
    const ejecutar = () =>
      this.store.cambiar(turno.id, accion).subscribe({
        next: () =>
          this.notifications.success(
            accion === 'llamar'
              ? `Se llamó al turno ${turno.numero}.`
              : accion === 'ausente'
                ? `El turno ${turno.numero} quedó como ausente.`
                : `El turno ${turno.numero} fue cancelado.`,
          ),
        error: () => undefined,
      });
    if (accion === 'cancelar') {
      this.confirmation.confirm({
        header: 'Cancelar turno',
        message: `Se cancelará el turno ${turno.numero} de ${turno.pacienteNombre}.`,
        icon: 'pi pi-times-circle',
        acceptLabel: 'Cancelar turno',
        rejectLabel: 'Volver',
        acceptButtonProps: { severity: 'danger' },
        rejectButtonProps: { severity: 'secondary', outlined: true },
        accept: ejecutar,
      });
      return;
    }
    ejecutar();
  }

  protected llegada(citaId: number, paciente: string): void {
    this.store.registrarLlegada(citaId).subscribe({
      next: (turno) =>
        this.notifications.success(`${paciente} quedó en espera con el turno ${turno.numero}.`),
      error: () => undefined,
    });
  }

  protected abrirTurno(): void {
    this.pacienteTurno.set(null);
    this.prioridadTurno.set('NORMAL');
    this.medicoTurno.set(null);
    this.motivoTurno.set('');
    this.error.set(null);
    this.turnoVisible.set(true);
  }

  protected crearTurno(): void {
    const paciente = this.pacienteTurno();
    if (!paciente) {
      this.error.set('Elige un paciente.');
      return;
    }
    this.guardando.set(true);
    this.store
      .crearTurno({
        pacienteId: paciente.pacienteId,
        medicoId: this.medicoTurno(),
        prioridad: this.prioridadTurno(),
        motivo: this.motivoTurno().trim() || null,
      })
      .subscribe({
        next: (turno) => {
          this.guardando.set(false);
          this.turnoVisible.set(false);
          this.notifications.success(
            `Turno ${turno.numero} generado para ${paciente.nombreCompleto}.`,
          );
        },
        error: (error: { message: string }) => {
          this.guardando.set(false);
          this.error.set(error.message);
        },
      });
  }

  protected opcionesMedico(): { label: string; value: number }[] {
    return this.medicos.medicosActivos().map((medico) => ({
      label: `${medico.nombreCompleto} · ${medico.especialidades.map((e) => e.nombre).join(', ')}`,
      value: medico.id,
    }));
  }
}
