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
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';
import { Tag } from 'primeng/tag';

import {
  AREAS_LABORALES,
  AuthService,
  NOMBRES_DIAS_SEMANA,
  ETIQUETA_AREA_LABORAL,
  ETIQUETA_TIPO_AUSENCIA,
  FilaSemana,
  NotificationService,
  RrhhStore,
  TurnoPersonalResponse,
  toIsoDate,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { horaCorta, lunesDe, sumarDias } from '../shared/ausencia';

const MAXIMO_HORAS_SEMANA = 48;

@Component({
  selector: 'app-turnos',
  imports: [
    DatePipe,
    FormsModule,
    Button,
    Dialog,
    InputText,
    Message,
    Select,
    Tag,
    EmptyState,
    PageHeader,
  ],
  templateUrl: './turnos.html',
  styleUrl: './turnos.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Turnos implements OnInit {
  protected readonly store = inject(RrhhStore);
  private readonly auth = inject(AuthService);
  private readonly notifications = inject(NotificationService);

  protected readonly gestiona = computed(() => this.auth.isRrhh());
  protected readonly etiquetaTipo = ETIQUETA_TIPO_AUSENCIA;
  protected readonly dias = NOMBRES_DIAS_SEMANA;
  protected readonly hora = horaCorta;
  protected readonly maximo = MAXIMO_HORAS_SEMANA;
  protected readonly areas = AREAS_LABORALES.map((a) => ({
    label: ETIQUETA_AREA_LABORAL[a],
    value: a,
  }));

  protected readonly inicio = signal(lunesDe(new Date()));
  protected readonly area = signal<string | null>(null);
  protected readonly fechas = computed(() =>
    Array.from({ length: 7 }, (_, i) => sumarDias(this.inicio(), i)),
  );
  protected readonly fin = computed(() => sumarDias(this.inicio(), 6));

  protected readonly plantillas = computed(() =>
    (this.store.plantillas.value() ?? []).map((p) => ({
      label: p.nombre + ' · ' + this.hora(p.horaInicio) + ' a ' + this.hora(p.horaFin),
      value: p.id,
    })),
  );

  protected readonly celda = signal<{ fila: FilaSemana; fecha: string } | null>(null);
  protected readonly plantillaId = signal<string | null>(null);
  protected readonly notas = signal('');
  protected readonly asignando = signal(false);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.store.cargarPlantillas();
    this.cargar();
  }

  protected cargar(): void {
    this.store.cargarSemana(toIsoDate(this.inicio()), this.area());
  }

  protected mover(dias: number): void {
    this.inicio.set(sumarDias(this.inicio(), dias));
    this.cargar();
  }

  protected hoy(): void {
    this.inicio.set(lunesDe(new Date()));
    this.cargar();
  }

  protected cambiarArea(valor: string | null): void {
    this.area.set(valor);
    this.cargar();
  }

  protected abrir(fila: FilaSemana, fecha: string): void {
    this.celda.set({ fila, fecha });
    this.plantillaId.set(null);
    this.notas.set('');
    this.error.set(null);
  }

  protected asignar(): void {
    const celda = this.celda();
    const plantilla = this.plantillaId();
    if (!celda || !plantilla) {
      this.error.set('Elige la plantilla de turno.');
      return;
    }
    this.asignando.set(true);
    this.store
      .asignarTurno({
        empleadoId: celda.fila.empleadoId,
        plantillaId: plantilla,
        fecha: celda.fecha,
        notas: this.notas().trim() || null,
      })
      .subscribe({
        next: () => {
          this.asignando.set(false);
          this.celda.set(null);
          this.notifications.success('El turno fue asignado.');
          this.cargar();
        },
        error: (error: { message: string }) => {
          this.asignando.set(false);
          this.error.set(error.message);
        },
      });
  }

  protected quitar(turno: TurnoPersonalResponse): void {
    this.store.eliminarTurno(turno.id).subscribe({
      next: () => {
        this.notifications.success('El turno fue eliminado.');
        this.cargar();
      },
      error: () => undefined,
    });
  }
}
