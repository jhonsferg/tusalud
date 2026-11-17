import { CurrencyPipe } from '@angular/common';
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
import { Dialog } from 'primeng/dialog';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { ToggleSwitch } from 'primeng/toggleswitch';

import {
  AREAS_CAMA,
  AreaCama,
  CamaResponse,
  ETIQUETA_AREA,
  ETIQUETA_ESTADO_CAMA,
} from '@tusalud/app/core/models';
import { AuthService } from '@tusalud/app/core/services/auth.service';
import { NotificationService } from '@tusalud/app/core/services/notification.service';
import { HospitalizacionStore } from '@tusalud/app/core/state/hospitalizacion.store';
import { EmptyState } from '@tusalud/app/shared/components/empty-state/empty-state';
import { PageHeader } from '@tusalud/app/shared/components/page-header/page-header';

@Component({
  selector: 'app-camas',
  imports: [
    CurrencyPipe,
    FormsModule,
    ReactiveFormsModule,
    Button,
    Dialog,
    InputNumber,
    InputText,
    Message,
    Select,
    TableModule,
    Tag,
    ToggleSwitch,
    EmptyState,
    PageHeader,
  ],
  templateUrl: './camas.html',
  styleUrl: './camas.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Camas implements OnInit {
  protected readonly store = inject(HospitalizacionStore);
  private readonly auth = inject(AuthService);
  private readonly notifications = inject(NotificationService);
  private readonly fb = inject(FormBuilder).nonNullable;

  protected readonly gestiona = computed(() => this.auth.isAdmin());
  protected readonly areas = AREAS_CAMA.map((valor) => ({
    label: ETIQUETA_AREA[valor],
    value: valor,
  }));
  protected readonly etiquetaArea = ETIQUETA_AREA;
  protected readonly etiquetaEstado = ETIQUETA_ESTADO_CAMA;
  protected readonly estados = [
    { label: 'Libre', value: 'LIBRE' },
    { label: 'Limpieza', value: 'LIMPIEZA' },
    { label: 'Mantenimiento', value: 'MANTENIMIENTO' },
  ];

  protected readonly visible = signal(false);
  protected readonly editando = signal<CamaResponse | null>(null);
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = this.fb.group({
    codigo: ['', [Validators.required, Validators.maxLength(20)]],
    area: ['HOSPITALIZACION' as AreaCama, [Validators.required]],
    piso: [''],
    descripcion: [''],
    precioDia: [0, [Validators.required, Validators.min(0)]],
    activa: [true],
  });

  ngOnInit(): void {
    this.store.cargarCamas();
  }

  protected abrir(cama: CamaResponse | null): void {
    this.editando.set(cama);
    this.error.set(null);
    this.form.reset({
      codigo: cama?.codigo ?? '',
      area: cama?.area ?? 'HOSPITALIZACION',
      piso: cama?.piso ?? '',
      descripcion: cama?.descripcion ?? '',
      precioDia: cama?.precioDia ?? 0,
      activa: cama?.activa ?? true,
    });
    this.visible.set(true);
  }

  protected guardar(): void {
    this.error.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.error.set('Indica el código y el precio por día.');
      return;
    }
    const valor = this.form.getRawValue();
    const request = {
      codigo: valor.codigo.trim().toUpperCase(),
      area: valor.area,
      piso: valor.piso.trim() || null,
      descripcion: valor.descripcion.trim() || null,
      precioDia: valor.precioDia,
      activa: valor.activa,
    };
    const actual = this.editando();
    this.guardando.set(true);
    const operacion = actual
      ? this.store.actualizarCama(actual.id, request)
      : this.store.crearCama(request);
    operacion.subscribe({
      next: () => {
        this.guardando.set(false);
        this.visible.set(false);
        this.notifications.success('La cama fue guardada.');
      },
      error: (error: { message: string }) => {
        this.guardando.set(false);
        this.error.set(error.message);
      },
    });
  }

  protected cambiarEstado(cama: CamaResponse, estado: string): void {
    this.store.cambiarEstadoCama(cama.id, estado).subscribe({
      next: () =>
        this.notifications.success(
          `La cama ${cama.codigo} quedó en ${this.etiquetaEstado[estado].toLowerCase()}.`,
        ),
      error: () => undefined,
    });
  }
}
