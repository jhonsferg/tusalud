import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject } from '@angular/core';
import { Router } from '@angular/router';
import { Button } from 'primeng/button';
import { Tag } from 'primeng/tag';

import {
  AuthService,
  ETIQUETA_ESTADO_TELECONSULTA,
  EstadoTeleconsulta,
  NotificationService,
  TeleconsultaResponse,
  TeleconsultaStore,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';

const SEVERIDAD: Readonly<
  Record<EstadoTeleconsulta, 'info' | 'success' | 'warn' | 'danger' | 'secondary'>
> = {
  PROGRAMADA: 'info',
  SALA_ABIERTA: 'success',
  EN_CURSO: 'success',
  FINALIZADA: 'secondary',
  CANCELADA: 'danger',
  NO_ASISTIO: 'warn',
};

const ACTIVAS: readonly EstadoTeleconsulta[] = ['PROGRAMADA', 'SALA_ABIERTA', 'EN_CURSO'];

@Component({
  selector: 'app-lista-teleconsultas',
  imports: [DatePipe, Button, Tag, EmptyState, PageHeader],
  templateUrl: './lista-teleconsultas.html',
  styleUrl: './lista-teleconsultas.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ListaTeleconsultas implements OnInit {
  protected readonly store = inject(TeleconsultaStore);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly notifications = inject(NotificationService);

  protected readonly esMedico = computed(() => this.auth.isMedico());
  protected readonly etiqueta = ETIQUETA_ESTADO_TELECONSULTA;
  protected readonly severidad = SEVERIDAD;

  protected readonly proximas = computed(() =>
    (this.store.lista.value() ?? []).filter((t) => ACTIVAS.includes(t.estado)),
  );
  protected readonly historial = computed(() =>
    (this.store.lista.value() ?? []).filter((t) => !ACTIVAS.includes(t.estado)),
  );

  ngOnInit(): void {
    if (this.auth.isMedico()) {
      this.store.cargarDelMedico();
    } else {
      this.store.cargarMias();
    }
  }

  protected ingresar(teleconsulta: TeleconsultaResponse): void {
    if (this.esMedico()) {
      this.store.abrir(teleconsulta.id).subscribe({
        next: () => void this.router.navigate(['/teleconsultas', teleconsulta.id, 'sala']),
        error: (error: { message: string }) => this.notifications.error(error.message),
      });
      return;
    }
    void this.router.navigate(['/teleconsultas', teleconsulta.id, 'prueba']);
  }
}
