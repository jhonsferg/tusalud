import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';

import {
  CamaResponse,
  ETIQUETA_TRIAJE,
  IngresoHospResponse,
  PacienteMostradorResponse,
} from '@tusalud/app/core/models';
import { AuthService } from '@tusalud/app/core/services/auth.service';
import { NotificationService } from '@tusalud/app/core/services/notification.service';
import { HospitalizacionStore } from '@tusalud/app/core/state/hospitalizacion.store';
import { EmptyState } from '@tusalud/app/shared/components/empty-state/empty-state';
import { PacienteBuscador } from '@tusalud/app/shared/components/paciente-buscador/paciente-buscador';
import { PageHeader } from '@tusalud/app/shared/components/page-header/page-header';
import { COLOR_TRIAJE, NIVELES_TRIAJE } from '@tusalud/app/shared/utils/hospitalizacion';

const REFRESCO_MS = 15_000;

@Component({
  selector: 'app-emergencias',
  imports: [
    FormsModule,
    RouterLink,
    Button,
    Dialog,
    InputNumber,
    InputText,
    Message,
    Select,
    EmptyState,
    PacienteBuscador,
    PageHeader,
  ],
  templateUrl: './emergencias.html',
  styleUrl: './emergencias.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Emergencias implements OnInit {
  protected readonly store = inject(HospitalizacionStore);
  private readonly auth = inject(AuthService);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly niveles = NIVELES_TRIAJE;
  protected readonly colores = COLOR_TRIAJE;
  protected readonly etiquetaTriaje = ETIQUETA_TRIAJE;
  protected readonly puedeAsignar = computed(
    () => this.auth.isMedico() || this.auth.isEnfermeria() || this.auth.isAdmin(),
  );

  protected readonly visible = signal(false);
  protected readonly paciente = signal<PacienteMostradorResponse | null>(null);
  protected readonly nivel = signal(3);
  protected readonly presion = signal('');
  protected readonly fc = signal<number | null>(null);
  protected readonly fr = signal<number | null>(null);
  protected readonly temperatura = signal<number | null>(null);
  protected readonly saturacion = signal<number | null>(null);
  protected readonly glasgow = signal<number | null>(null);
  protected readonly motivo = signal('');
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly asignando = signal<IngresoHospResponse | null>(null);
  protected readonly camaElegida = signal<number | null>(null);
  protected readonly camasLibres = computed(() =>
    (this.store.camas.value() ?? [])
      .filter(
        (cama: CamaResponse) =>
          cama.estado === 'LIBRE' && cama.activa && cama.area !== 'HOSPITALIZACION',
      )
      .map((cama) => ({ label: `${cama.codigo} · ${cama.area}`, value: cama.id })),
  );

  ngOnInit(): void {
    this.store.cargarEmergencias();
    const temporizador = setInterval(() => this.store.cargarEmergencias(), REFRESCO_MS);
    this.destroyRef.onDestroy(() => clearInterval(temporizador));
  }

  protected abrir(): void {
    this.paciente.set(null);
    this.nivel.set(3);
    this.presion.set('');
    this.fc.set(null);
    this.fr.set(null);
    this.temperatura.set(null);
    this.saturacion.set(null);
    this.glasgow.set(null);
    this.motivo.set('');
    this.error.set(null);
    this.visible.set(true);
  }

  protected registrar(): void {
    const paciente = this.paciente();
    if (!paciente || !this.motivo().trim()) {
      this.error.set('Elige el paciente y escribe el motivo de consulta.');
      return;
    }
    this.guardando.set(true);
    this.store
      .registrarEmergencia({
        pacienteId: paciente.pacienteId,
        triaje: {
          nivel: this.nivel(),
          presionArterial: this.presion().trim() || null,
          frecuenciaCardiaca: this.fc(),
          frecuenciaRespiratoria: this.fr(),
          temperatura: this.temperatura(),
          saturacionO2: this.saturacion(),
          glasgow: this.glasgow(),
          motivoConsulta: this.motivo().trim(),
        },
      })
      .subscribe({
        next: () => {
          this.guardando.set(false);
          this.visible.set(false);
          this.notifications.success(
            'El triaje fue registrado y el paciente entró a la cola de emergencia.',
          );
        },
        error: (error: { message: string }) => {
          this.guardando.set(false);
          this.error.set(error.message);
        },
      });
  }

  protected abrirAsignacion(ingreso: IngresoHospResponse): void {
    this.store.cargarCamas();
    this.camaElegida.set(null);
    this.asignando.set(ingreso);
  }

  protected asignar(): void {
    const ingreso = this.asignando();
    const cama = this.camaElegida();
    if (!ingreso || cama === null) {
      return;
    }
    this.store.asignarCama(ingreso.id, cama).subscribe({
      next: () => {
        this.asignando.set(null);
        this.notifications.success('La cama fue asignada.');
      },
      error: () => undefined,
    });
  }
}
