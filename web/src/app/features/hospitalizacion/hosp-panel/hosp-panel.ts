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
import { Router, RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Textarea } from 'primeng/textarea';

import {
  CamaResponse,
  ETIQUETA_AREA,
  ETIQUETA_ESTADO_CAMA,
  PacienteMostradorResponse,
} from '@tusalud/app/core/models';
import { AuthService } from '@tusalud/app/core/services/auth.service';
import { NotificationService } from '@tusalud/app/core/services/notification.service';
import { HospitalizacionStore } from '@tusalud/app/core/state/hospitalizacion.store';
import { EmptyState } from '@tusalud/app/shared/components/empty-state/empty-state';
import { Kpi } from '@tusalud/app/shared/components/kpi/kpi';
import { PacienteBuscador } from '@tusalud/app/shared/components/paciente-buscador/paciente-buscador';
import { PageHeader } from '@tusalud/app/shared/components/page-header/page-header';
import { colorCama } from '@tusalud/app/shared/utils/hospitalizacion';

const REFRESCO_MS = 20_000;

@Component({
  selector: 'app-hosp-panel',
  imports: [
    FormsModule,
    RouterLink,
    Button,
    Dialog,
    InputText,
    Message,
    Textarea,
    EmptyState,
    Kpi,
    PacienteBuscador,
    PageHeader,
  ],
  templateUrl: './hosp-panel.html',
  styleUrl: './hosp-panel.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HospPanel implements OnInit {
  protected readonly store = inject(HospitalizacionStore);
  private readonly auth = inject(AuthService);
  private readonly notifications = inject(NotificationService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly etiquetaArea = ETIQUETA_AREA;
  protected readonly etiquetaEstado = ETIQUETA_ESTADO_CAMA;
  protected readonly color = colorCama;
  protected readonly puedeIngresar = computed(() => this.auth.isMedico());

  protected readonly cama = signal<CamaResponse | null>(null);
  protected readonly paciente = signal<PacienteMostradorResponse | null>(null);
  protected readonly motivo = signal('');
  protected readonly diagnostico = signal('');
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.cargar();
    const temporizador = setInterval(() => this.cargar(), REFRESCO_MS);
    this.destroyRef.onDestroy(() => clearInterval(temporizador));
  }

  protected cargar(): void {
    this.store.cargarResumen();
    this.store.cargarMapa();
  }

  protected abrirCama(cama: CamaResponse): void {
    if (cama.estado === 'OCUPADA' && cama.ingresoActivoId) {
      void this.router.navigate(['/hospitalizacion/ingresos', cama.ingresoActivoId]);
      return;
    }
    if (cama.estado === 'LIBRE' && this.puedeIngresar()) {
      this.cama.set(cama);
      this.paciente.set(null);
      this.motivo.set('');
      this.diagnostico.set('');
      this.error.set(null);
    }
  }

  protected ingresar(): void {
    const cama = this.cama();
    const paciente = this.paciente();
    if (!cama || !paciente || !this.motivo().trim()) {
      this.error.set('Elige el paciente y escribe el motivo del ingreso.');
      return;
    }
    this.guardando.set(true);
    this.store
      .crearIngreso({
        pacienteId: paciente.pacienteId,
        camaId: cama.id,
        motivo: this.motivo().trim(),
        diagnosticoIngreso: this.diagnostico().trim() || null,
      })
      .subscribe({
        next: (ingreso) => {
          this.guardando.set(false);
          this.cama.set(null);
          this.notifications.success(
            `${paciente.nombreCompleto} ingresó a la cama ${cama.codigo}.`,
          );
          void this.router.navigate(['/hospitalizacion/ingresos', ingreso.id]);
        },
        error: (error: { message: string }) => {
          this.guardando.set(false);
          this.error.set(error.message);
        },
      });
  }
}
