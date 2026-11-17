import { CurrencyPipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { Textarea } from 'primeng/textarea';
import { ToggleSwitch } from 'primeng/toggleswitch';
import { Subject, debounceTime } from 'rxjs';

import {
  AuthService,
  EstudioImagenResponse,
  ETIQUETA_MODALIDAD,
  ImagenesStore,
  MODALIDADES_IMAGEN,
  ModalidadImagen,
  NotificationService,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';

@Component({
  selector: 'app-catalogo-imagenes',
  imports: [
    CurrencyPipe,
    FormsModule,
    ReactiveFormsModule,
    Button,
    Dialog,
    IconField,
    InputIcon,
    InputNumber,
    InputText,
    Message,
    Select,
    TableModule,
    Tag,
    Textarea,
    ToggleSwitch,
    EmptyState,
    PageHeader,
  ],
  templateUrl: './catalogo.html',
  styleUrl: './catalogo.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CatalogoImagenes implements OnInit {
  protected readonly store = inject(ImagenesStore);
  private readonly auth = inject(AuthService);
  private readonly notifications = inject(NotificationService);
  private readonly fb = inject(FormBuilder).nonNullable;

  protected readonly puedeEditar = computed(() => this.auth.isImagenes() || this.auth.isAdmin());
  protected readonly volver = computed(() =>
    this.auth.isImagenes() || this.auth.isMedico() ? '/imagenes' : undefined,
  );
  protected readonly etiquetaModalidad: Readonly<Record<string, string>> = ETIQUETA_MODALIDAD;
  protected readonly modalidades = MODALIDADES_IMAGEN.map((valor) => ({
    label: ETIQUETA_MODALIDAD[valor],
    value: valor,
  }));

  protected readonly busqueda = signal('');
  protected readonly modalidad = signal<ModalidadImagen | null>(null);
  private readonly cambio = new Subject<void>();

  protected readonly visible = signal(false);
  protected readonly editando = signal<EstudioImagenResponse | null>(null);
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = this.fb.group({
    codigo: ['', [Validators.required, Validators.maxLength(20)]],
    nombre: ['', [Validators.required, Validators.maxLength(150)]],
    modalidad: ['RX' as ModalidadImagen, [Validators.required]],
    region: ['', [Validators.required, Validators.maxLength(60)]],
    preparacion: ['', [Validators.maxLength(400)]],
    precio: [0, [Validators.required, Validators.min(0)]],
    duracionMinutos: [20, [Validators.required, Validators.min(5), Validators.max(240)]],
    activo: [true],
  });

  constructor() {
    this.cambio.pipe(debounceTime(300), takeUntilDestroyed()).subscribe(() => this.cargar());
  }

  ngOnInit(): void {
    this.cargar();
  }

  protected buscar(valor: string): void {
    this.busqueda.set(valor);
    this.cambio.next();
  }

  protected cargar(): void {
    this.store.cargarEstudios({
      q: this.busqueda().trim() || null,
      modalidad: this.modalidad(),
    });
  }

  protected abrir(estudio: EstudioImagenResponse | null): void {
    this.editando.set(estudio);
    this.error.set(null);
    this.form.reset({
      codigo: estudio?.codigo ?? '',
      nombre: estudio?.nombre ?? '',
      modalidad: estudio?.modalidad ?? 'RX',
      region: estudio?.region ?? '',
      preparacion: estudio?.preparacion ?? '',
      precio: estudio?.precio ?? 0,
      duracionMinutos: estudio?.duracionMinutos ?? 20,
      activo: estudio?.activo ?? true,
    });
    this.visible.set(true);
  }

  protected guardar(): void {
    this.error.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.error.set('Completa el código, el nombre, la región, el precio y la duración.');
      return;
    }
    const valor = this.form.getRawValue();
    const request = {
      codigo: valor.codigo.trim().toUpperCase(),
      nombre: valor.nombre.trim(),
      modalidad: valor.modalidad,
      region: valor.region.trim(),
      preparacion: valor.preparacion.trim() || null,
      precio: valor.precio,
      duracionMinutos: valor.duracionMinutos,
      activo: valor.activo,
    };
    const actual = this.editando();
    this.guardando.set(true);
    const operacion = actual
      ? this.store.actualizarEstudio(actual.id, request)
      : this.store.crearEstudio(request);
    operacion.subscribe({
      next: (estudio) => {
        this.guardando.set(false);
        this.visible.set(false);
        this.notifications.success(
          actual ? 'El estudio fue actualizado.' : `El estudio ${estudio.nombre} fue registrado.`,
        );
      },
      error: (error: { message: string }) => {
        this.guardando.set(false);
        this.error.set(error.message);
      },
    });
  }
}
