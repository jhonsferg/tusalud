import { CurrencyPipe, DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
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
import { Message } from 'primeng/message';
import { Textarea } from 'primeng/textarea';

import { ETIQUETA_METODO_CAJA, SesionCajaResponse } from '@tusalud/app/core/models';
import { NotificationService } from '@tusalud/app/core/services/notification.service';
import { CajaStore } from '@tusalud/app/core/state/caja.store';
import { Kpi } from '@tusalud/app/shared/components/kpi/kpi';
import { PageHeader } from '@tusalud/app/shared/components/page-header/page-header';

@Component({
  selector: 'app-caja-panel',
  imports: [
    CurrencyPipe,
    DatePipe,
    FormsModule,
    RouterLink,
    Button,
    Dialog,
    InputNumber,
    Message,
    Textarea,
    Kpi,
    PageHeader,
  ],
  templateUrl: './caja-panel.html',
  styleUrl: './caja-panel.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CajaPanel implements OnInit {
  protected readonly store = inject(CajaStore);
  private readonly notifications = inject(NotificationService);

  protected readonly etiquetaMetodo = ETIQUETA_METODO_CAJA;
  protected readonly montoApertura = signal(0);
  protected readonly abriendo = signal(false);
  protected readonly errorApertura = signal<string | null>(null);

  protected readonly cierreVisible = signal(false);
  protected readonly montoContado = signal(0);
  protected readonly observaciones = signal('');
  protected readonly cerrando = signal(false);
  protected readonly errorCierre = signal<string | null>(null);
  protected readonly cerrada = signal<SesionCajaResponse | null>(null);

  protected readonly abierta = computed(() => this.store.sesion.value()?.estado === 'ABIERTA');
  protected readonly cargando = computed(
    () => this.store.sesion.loading() && !this.store.sesion.hasValue(),
  );

  ngOnInit(): void {
    this.store.cargarSesion();
    this.store.cargarResumen();
  }

  protected abrir(): void {
    this.errorApertura.set(null);
    this.abriendo.set(true);
    this.store.abrir(this.montoApertura()).subscribe({
      next: () => {
        this.abriendo.set(false);
        this.store.cargarResumen();
        this.notifications.success('La caja fue abierta.');
      },
      error: (error: { message: string }) => {
        this.abriendo.set(false);
        this.errorApertura.set(error.message);
      },
    });
  }

  protected abrirCierre(): void {
    this.montoContado.set(0);
    this.observaciones.set('');
    this.errorCierre.set(null);
    this.cierreVisible.set(true);
  }

  protected cerrar(): void {
    this.errorCierre.set(null);
    this.cerrando.set(true);
    this.store
      .cerrar({
        montoContado: this.montoContado(),
        observaciones: this.observaciones().trim() || null,
      })
      .subscribe({
        next: (sesion) => {
          this.cerrando.set(false);
          this.cierreVisible.set(false);
          this.cerrada.set(sesion);
        },
        error: (error: { message: string }) => {
          this.cerrando.set(false);
          this.errorCierre.set(error.message);
        },
      });
  }
}
