import { CurrencyPipe, DatePipe } from '@angular/common';
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
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ConfirmationService } from 'primeng/api';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Tag } from 'primeng/tag';
import { Textarea } from 'primeng/textarea';

import {
  ETIQUETA_ESTADO_ORDEN,
  ExamenResponse,
  ItemOrdenResponse,
  ParametroExamen,
} from '@tusalud/app/core/models';
import { AuthService } from '@tusalud/app/core/services/auth.service';
import { NotificationService } from '@tusalud/app/core/services/notification.service';
import { LaboratorioStore } from '@tusalud/app/core/state/laboratorio.store';
import { optionalNumberAttribute } from '@tusalud/app/core/utils/route-input';
import { EmptyState } from '@tusalud/app/shared/components/empty-state/empty-state';
import { PageHeader } from '@tusalud/app/shared/components/page-header/page-header';
import {
  ICONO_ORDEN,
  SEVERIDAD_ORDEN,
  rangoReferencia,
} from '@tusalud/app/shared/utils/laboratorio';

@Component({
  selector: 'app-orden-detalle',
  imports: [
    CurrencyPipe,
    DatePipe,
    ReactiveFormsModule,
    Button,
    InputText,
    Message,
    Tag,
    Textarea,
    EmptyState,
    PageHeader,
  ],
  templateUrl: './orden-detalle.html',
  styleUrl: './orden-detalle.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OrdenDetalle {
  readonly id = input(undefined, { transform: optionalNumberAttribute });

  protected readonly store = inject(LaboratorioStore);
  private readonly auth = inject(AuthService);
  private readonly notifications = inject(NotificationService);
  private readonly confirmation = inject(ConfirmationService);
  private readonly fb = inject(FormBuilder);

  protected readonly etiqueta: Readonly<Record<string, string>> = ETIQUETA_ESTADO_ORDEN;
  protected readonly severidad: Readonly<
    Record<string, 'success' | 'info' | 'warn' | 'danger' | 'secondary'>
  > = SEVERIDAD_ORDEN;
  protected readonly icono: Readonly<Record<string, string>> = ICONO_ORDEN;
  protected readonly rango = rangoReferencia;

  protected readonly capturando = signal<number | null>(null);
  protected readonly trabajando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected formulario: FormGroup = this.fb.group({ observaciones: [''] });

  protected readonly esPersonal = computed(() => this.auth.isLaboratorio() || this.auth.isAdmin());
  protected readonly orden = this.store.orden.value;

  protected readonly examenPorId = computed(
    () => new Map((this.store.examenes.value() ?? []).map((examen) => [examen.id, examen])),
  );

  protected readonly puedeTomarMuestra = computed(
    () => this.esPersonal() && this.orden()?.estado === 'SOLICITADA',
  );
  protected readonly puedeCapturar = computed(() => {
    const estado = this.orden()?.estado;
    return this.esPersonal() && (estado === 'MUESTRA_TOMADA' || estado === 'EN_PROCESO');
  });
  protected readonly puedeValidar = computed(() => {
    const orden = this.orden();
    return (
      this.puedeCapturar() &&
      !!orden &&
      orden.items.length > 0 &&
      orden.items.every((item) => item.estado !== 'PENDIENTE')
    );
  });
  protected readonly puedeCancelar = computed(() => {
    const estado = this.orden()?.estado;
    if (estado === 'COMPLETADA' || estado === 'CANCELADA') {
      return false;
    }
    return this.esPersonal() || (this.auth.isMedico() && estado === 'SOLICITADA');
  });

  constructor() {
    effect(() => {
      const id = this.id();
      if (id !== undefined) {
        untracked(() => {
          this.store.orden.reset();
          this.store.cargarOrden(id);
          if (this.store.examenes.value() === undefined) {
            this.store.cargarExamenes();
          }
        });
      }
    });
  }

  protected parametros(item: ItemOrdenResponse): ParametroExamen[] {
    const examen: ExamenResponse | undefined = this.examenPorId().get(item.examenId);
    return examen?.parametros ?? [];
  }

  protected abrirCaptura(item: ItemOrdenResponse): void {
    const grupo: Record<string, unknown[]> = { observaciones: [item.observaciones ?? ''] };
    for (const parametro of this.parametros(item)) {
      const actual = item.resultados.find((resultado) => resultado.parametroId === parametro.id);
      grupo['p' + parametro.id] = [actual?.valor ?? ''];
    }
    this.formulario = this.fb.group(grupo);
    this.error.set(null);
    this.capturando.set(item.id);
  }

  protected guardarResultados(item: ItemOrdenResponse): void {
    const orden = this.orden();
    if (!orden) {
      return;
    }
    const valor = this.formulario.getRawValue() as Record<string, string>;
    const resultados = this.parametros(item)
      .map((parametro) => ({
        parametroId: parametro.id!,
        valor: (valor['p' + parametro.id] ?? '').trim(),
      }))
      .filter((resultado) => resultado.valor !== '');
    if (resultados.length === 0) {
      this.error.set('Ingresa al menos un resultado.');
      return;
    }
    this.trabajando.set(true);
    this.store
      .registrarResultados(orden.id, item.id, {
        resultados,
        observaciones: valor['observaciones']?.trim() || null,
      })
      .subscribe({
        next: () => {
          this.trabajando.set(false);
          this.capturando.set(null);
          this.notifications.success('Los resultados fueron guardados.');
        },
        error: (error: { message: string }) => {
          this.trabajando.set(false);
          this.error.set(error.message);
        },
      });
  }

  protected tomarMuestra(): void {
    const orden = this.orden();
    if (orden) {
      this.ejecutar(this.store.tomarMuestra(orden.id), 'La toma de muestra fue registrada.');
    }
  }

  protected validar(): void {
    const orden = this.orden();
    if (!orden) {
      return;
    }
    this.confirmation.confirm({
      header: 'Validar y publicar resultados',
      message:
        'El paciente y el médico recibirán los resultados. Esta acción no se puede deshacer.',
      icon: 'pi pi-verified',
      acceptLabel: 'Validar',
      rejectLabel: 'Cancelar',
      rejectButtonProps: { severity: 'secondary', outlined: true },
      accept: () =>
        this.ejecutar(
          this.store.validar(orden.id),
          'Los resultados fueron validados y publicados.',
        ),
    });
  }

  protected cancelar(): void {
    const orden = this.orden();
    if (!orden) {
      return;
    }
    this.confirmation.confirm({
      header: 'Cancelar orden',
      message: `Se cancelará la orden ${orden.codigo}.`,
      icon: 'pi pi-times-circle',
      acceptLabel: 'Cancelar orden',
      rejectLabel: 'Volver',
      acceptButtonProps: { severity: 'danger' },
      rejectButtonProps: { severity: 'secondary', outlined: true },
      accept: () => this.ejecutar(this.store.cancelar(orden.id), 'La orden fue cancelada.'),
    });
  }

  protected imprimir(): void {
    window.print();
  }

  private ejecutar(operacion: ReturnType<LaboratorioStore['tomarMuestra']>, mensaje: string): void {
    this.trabajando.set(true);
    operacion.subscribe({
      next: () => {
        this.trabajando.set(false);
        this.notifications.success(mensaje);
      },
      error: () => this.trabajando.set(false),
    });
  }
}
