import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Button } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';
import { Dialog } from 'primeng/dialog';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { Textarea } from 'primeng/textarea';

import {
  AusenciaResponse,
  ETIQUETA_ESTADO_AUSENCIA,
  ETIQUETA_TIPO_AUSENCIA,
  NotificationService,
  RrhhStore,
  TIPOS_AUSENCIA,
  TipoAusencia,
  toIsoDate,
} from '@tusalud/core';
import { Kpi } from '@tusalud/ui/kpi';
import { EmptyState } from '@tusalud/ui/empty-state';
import { severidadAusencia } from '../shared/ausencia';

@Component({
  selector: 'app-mis-ausencias',
  imports: [
    DatePipe,
    FormsModule,
    ReactiveFormsModule,
    Button,
    DatePicker,
    Dialog,
    Message,
    Select,
    TableModule,
    Tag,
    Textarea,
    EmptyState,
    Kpi,
  ],
  templateUrl: './mis-ausencias.html',
  styleUrl: './mis-ausencias.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MisAusencias implements OnInit {
  protected readonly store = inject(RrhhStore);
  private readonly notifications = inject(NotificationService);
  private readonly fb = inject(FormBuilder).nonNullable;

  protected readonly etiquetaTipo = ETIQUETA_TIPO_AUSENCIA;
  protected readonly etiquetaEstado = ETIQUETA_ESTADO_AUSENCIA;
  protected readonly severidad = severidadAusencia;
  protected readonly tipos = TIPOS_AUSENCIA.map((t) => ({
    label: ETIQUETA_TIPO_AUSENCIA[t],
    value: t,
  }));
  protected readonly manana = new Date(Date.now() + 86_400_000);
  protected readonly sinFicha = computed(() => this.store.misAusencias.error()?.status === 404);

  protected readonly visible = signal(false);
  protected readonly enviando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = this.fb.group({
    tipo: ['VACACIONES' as TipoAusencia, [Validators.required]],
    rango: this.fb.control<Date[] | null>(null, [Validators.required]),
    motivo: ['', [Validators.maxLength(300)]],
  });

  ngOnInit(): void {
    this.store.cargarMisAusencias();
  }

  protected abrir(): void {
    this.error.set(null);
    this.form.reset({ tipo: 'VACACIONES', rango: null, motivo: '' });
    this.visible.set(true);
  }

  protected solicitar(): void {
    this.error.set(null);
    const valor = this.form.getRawValue();
    const [inicio, fin] = valor.rango ?? [];
    if (this.form.invalid || !inicio) {
      this.form.markAllAsTouched();
      this.error.set('Elige el tipo y las fechas de la ausencia.');
      return;
    }
    this.enviando.set(true);
    this.store
      .solicitarAusencia({
        tipo: valor.tipo,
        fechaInicio: toIsoDate(inicio),
        fechaFin: toIsoDate(fin ?? inicio),
        motivo: valor.motivo.trim() || null,
      })
      .subscribe({
        next: () => {
          this.enviando.set(false);
          this.visible.set(false);
          this.notifications.success('Tu solicitud fue enviada a recursos humanos.');
        },
        error: (error: { message: string }) => {
          this.enviando.set(false);
          this.error.set(error.message);
        },
      });
  }

  protected puedeCancelar(ausencia: AusenciaResponse): boolean {
    return ausencia.estado === 'SOLICITADA' || ausencia.estado === 'APROBADA';
  }

  protected cancelar(ausencia: AusenciaResponse): void {
    this.store.cancelarMiAusencia(ausencia.id).subscribe({
      next: () => this.notifications.success('La ausencia fue cancelada.'),
      error: () => undefined,
    });
  }
}
