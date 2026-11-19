import { SelectorCie10 } from '@tusalud/ui/selector-cie10';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { MotivoService } from '@tusalud/ui/dialogo-motivo';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  signal,
  untracked,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';
import { Tag } from 'primeng/tag';
import { Textarea } from 'primeng/textarea';

import {
  AuthService,
  HospitalizacionStore,
  NotificationService,
  optionalIdAttribute,
  Cie10Response,
} from '@tusalud/core';
import { COLOR_TRIAJE } from '@tusalud/ui/utils';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { SecurityNotice } from '@tusalud/ui/tablero';

@Component({
  selector: 'app-ingreso-detalle',
  imports: [
    SecurityNotice,
    CurrencyPipe,
    DatePipe,
    FormsModule,
    Button,
    Dialog,
    InputNumber,
    InputText,
    Message,
    Select,
    Tag,
    Textarea,
    SelectorCie10,
    EmptyState,
    PageHeader,
  ],
  templateUrl: './ingreso-detalle.html',
  styleUrl: './ingreso-detalle.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class IngresoDetalle {
  readonly id = input(undefined, { transform: optionalIdAttribute });

  protected readonly store = inject(HospitalizacionStore);
  private readonly auth = inject(AuthService);
  private readonly notifications = inject(NotificationService);
  private readonly motivos = inject(MotivoService);
  private readonly router = inject(Router);

  protected readonly colores = COLOR_TRIAJE;
  protected readonly ingreso = this.store.ingreso.value;
  protected readonly activo = computed(() => this.ingreso()?.estado === 'ACTIVO');
  protected readonly tipoEvolucion = computed<'MEDICA' | 'ENFERMERIA' | null>(() =>
    this.auth.isMedico() ? 'MEDICA' : this.auth.isEnfermeria() ? 'ENFERMERIA' : null,
  );
  protected readonly puedeAlta = computed(() => this.activo() && this.auth.isMedico());
  protected readonly puedeCancelar = computed(
    () =>
      this.activo() && this.auth.isMedico() && (this.store.evoluciones.value() ?? []).length === 0,
  );
  protected readonly veEvoluciones = computed(
    () => this.auth.isMedico() || this.auth.isEnfermeria(),
  );
  protected readonly puedeTrasladar = computed(
    () => this.activo() && (this.auth.isMedico() || this.auth.isEnfermeria()),
  );

  protected readonly nota = signal('');
  protected readonly pa = signal('');
  protected readonly fc = signal<number | null>(null);
  protected readonly temp = signal<number | null>(null);
  protected readonly sat = signal<number | null>(null);
  protected readonly enviando = signal(false);
  protected readonly errorEvolucion = signal<string | null>(null);

  protected readonly altaVisible = signal(false);
  protected readonly diagnosticoAlta = signal('');
  protected readonly cie10Alta = signal<Cie10Response | null>(null);
  protected readonly indicaciones = signal('');
  protected readonly errorAlta = signal<string | null>(null);

  protected readonly trasladoVisible = signal(false);
  protected readonly camaDestino = signal<string | null>(null);
  protected readonly camasLibres = computed(() =>
    (this.store.camas.value() ?? [])
      .filter((cama) => cama.estado === 'LIBRE' && cama.activa)
      .map((cama) => ({ label: `${cama.codigo} · ${cama.area}`, value: cama.id })),
  );

  constructor() {
    effect(() => {
      const id = this.id();
      if (id !== undefined) {
        untracked(() => {
          this.store.ingreso.reset();
          this.store.cargarIngreso(id, this.veEvoluciones());
        });
      }
    });
  }

  protected registrarEvolucion(): void {
    const ingreso = this.ingreso();
    const tipo = this.tipoEvolucion();
    this.errorEvolucion.set(null);
    if (!ingreso || !tipo || !this.nota().trim()) {
      this.errorEvolucion.set('Escribe la nota de evolución.');
      return;
    }
    this.enviando.set(true);
    this.store
      .registrarEvolucion(ingreso.id, {
        tipo,
        nota: this.nota().trim(),
        presionArterial: this.pa().trim() || null,
        frecuenciaCardiaca: this.fc(),
        temperatura: this.temp(),
        saturacionO2: this.sat(),
      })
      .subscribe({
        next: () => {
          this.enviando.set(false);
          this.nota.set('');
          this.pa.set('');
          this.fc.set(null);
          this.temp.set(null);
          this.sat.set(null);
          this.notifications.success('La evolución fue registrada.');
        },
        error: (error: { message: string }) => {
          this.enviando.set(false);
          this.errorEvolucion.set(error.message);
        },
      });
  }

  protected cancelarIngreso(): void {
    const ingreso = this.ingreso();
    if (!ingreso) {
      return;
    }
    this.motivos
      .solicitar({
        titulo: 'Cancelar ingreso',
        descripcion: `Se cancelará el ingreso ${ingreso.codigo} y la cama quedará libre. Solo es posible mientras no tenga evoluciones.`,
        etiqueta: 'Motivo de la cancelación',
        confirmar: 'Cancelar ingreso',
        severidad: 'danger',
        icono: 'pi pi-times-circle',
      })
      .subscribe((motivo) =>
        this.store.cancelarIngreso(ingreso.id, motivo).subscribe({
          next: () => {
            this.notifications.success('El ingreso fue cancelado y la cama quedó libre.');
            void this.router.navigate(['/hospitalizacion/ingresos']);
          },
          error: () => undefined,
        }),
      );
  }

  protected abrirAlta(): void {
    this.diagnosticoAlta.set('');
    this.cie10Alta.set(null);
    this.indicaciones.set('');
    this.errorAlta.set(null);
    this.altaVisible.set(true);
  }

  protected darAlta(): void {
    const ingreso = this.ingreso();
    if (!ingreso || !this.diagnosticoAlta().trim() || !this.indicaciones().trim()) {
      this.errorAlta.set('Indica el diagnóstico de alta y las indicaciones para el paciente.');
      return;
    }
    this.store
      .alta(ingreso.id, {
        diagnosticoAlta: this.diagnosticoAlta().trim(),
        cie10Alta: this.cie10Alta()?.codigo ?? null,
        indicacionesAlta: this.indicaciones().trim(),
      })
      .subscribe({
        next: () => {
          this.altaVisible.set(false);
          this.notifications.success('El paciente recibió el alta.');
        },
        error: (error: { message: string }) => this.errorAlta.set(error.message),
      });
  }

  protected abrirTraslado(): void {
    this.store.cargarCamas();
    this.camaDestino.set(null);
    this.trasladoVisible.set(true);
  }

  protected trasladar(): void {
    const ingreso = this.ingreso();
    const cama = this.camaDestino();
    if (!ingreso || cama === null) {
      return;
    }
    this.store.trasladar(ingreso.id, cama).subscribe({
      next: () => {
        this.trasladoVisible.set(false);
        this.notifications.success('El paciente fue trasladado.');
      },
      error: () => undefined,
    });
  }
}
