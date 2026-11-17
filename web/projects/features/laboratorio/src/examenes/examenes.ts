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
import {
  FormArray,
  FormBuilder,
  FormsModule,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
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
  CategoriaExamen,
  CATEGORIAS_EXAMEN,
  ETIQUETA_CATEGORIA,
  ETIQUETA_MUESTRA,
  ExamenResponse,
  LaboratorioStore,
  NotificationService,
  TipoMuestra,
  TipoParametro,
  TIPOS_MUESTRA,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { rangoReferencia } from '@tusalud/ui/utils';

@Component({
  selector: 'app-examenes',
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
  templateUrl: './examenes.html',
  styleUrl: './examenes.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Examenes implements OnInit {
  protected readonly store = inject(LaboratorioStore);
  private readonly auth = inject(AuthService);
  private readonly notifications = inject(NotificationService);
  private readonly fb = inject(FormBuilder);

  protected readonly gestiona = computed(() => this.auth.isLaboratorio() || this.auth.isAdmin());
  protected readonly busqueda = signal('');
  protected readonly categoria = signal<CategoriaExamen | null>(null);
  private readonly cambio = new Subject<void>();

  protected readonly categorias = CATEGORIAS_EXAMEN.map((valor) => ({
    label: ETIQUETA_CATEGORIA[valor],
    value: valor,
  }));
  protected readonly muestras = TIPOS_MUESTRA.map((valor) => ({
    label: ETIQUETA_MUESTRA[valor],
    value: valor,
  }));
  protected readonly tiposParametro = [
    { label: 'Numérico', value: 'NUMERICO' },
    { label: 'Texto', value: 'TEXTO' },
  ];
  protected readonly etiquetaCategoria: Readonly<Record<string, string>> = ETIQUETA_CATEGORIA;
  protected readonly rango = rangoReferencia;

  protected readonly visible = signal(false);
  protected readonly editando = signal<ExamenResponse | null>(null);
  protected readonly detalle = signal<ExamenResponse | null>(null);
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    codigo: ['', [Validators.required, Validators.maxLength(30)]],
    nombre: ['', [Validators.required, Validators.maxLength(150)]],
    categoria: ['HEMATOLOGIA' as CategoriaExamen, [Validators.required]],
    muestra: ['SANGRE' as TipoMuestra, [Validators.required]],
    precio: [0, [Validators.required, Validators.min(0)]],
    tiempoEntregaHoras: [24, [Validators.required, Validators.min(1)]],
    indicaciones: ['', [Validators.maxLength(300)]],
    activo: [true],
    parametros: this.fb.array([this.nuevoParametro()]),
  });

  protected get parametros(): FormArray {
    return this.form.controls.parametros;
  }

  constructor() {
    this.cambio.pipe(debounceTime(300), takeUntilDestroyed()).subscribe(() => this.cargar());
  }

  ngOnInit(): void {
    this.cargar();
  }

  protected nuevoParametro(valor?: {
    id?: string;
    nombre: string;
    unidad?: string | null;
    tipo: TipoParametro;
    valorMin?: number | null;
    valorMax?: number | null;
  }) {
    return this.fb.nonNullable.group({
      id: [valor?.id ?? (null as string | null)],
      nombre: [valor?.nombre ?? '', [Validators.required, Validators.maxLength(100)]],
      unidad: [valor?.unidad ?? ''],
      tipo: [valor?.tipo ?? ('NUMERICO' as TipoParametro)],
      valorMin: [valor?.valorMin ?? (null as number | null)],
      valorMax: [valor?.valorMax ?? (null as number | null)],
    });
  }

  protected buscar(valor: string): void {
    this.busqueda.set(valor);
    this.cambio.next();
  }

  protected cargar(): void {
    this.store.cargarExamenes({ q: this.busqueda().trim() || null, categoria: this.categoria() });
  }

  protected abrir(examen: ExamenResponse | null): void {
    this.editando.set(examen);
    this.error.set(null);
    this.form.reset({
      codigo: examen?.codigo ?? '',
      nombre: examen?.nombre ?? '',
      categoria: examen?.categoria ?? 'HEMATOLOGIA',
      muestra: examen?.muestra ?? 'SANGRE',
      precio: examen?.precio ?? 0,
      tiempoEntregaHoras: examen?.tiempoEntregaHoras ?? 24,
      indicaciones: examen?.indicaciones ?? '',
      activo: examen?.activo ?? true,
    });
    this.parametros.clear();
    for (const parametro of examen?.parametros ?? [undefined]) {
      this.parametros.push(this.nuevoParametro(parametro));
    }
    this.visible.set(true);
  }

  protected agregarParametro(): void {
    this.parametros.push(this.nuevoParametro());
  }

  protected quitarParametro(indice: number): void {
    if (this.parametros.length > 1) {
      this.parametros.removeAt(indice);
    }
  }

  protected guardar(): void {
    this.error.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const valor = this.form.getRawValue();
    const request = {
      codigo: valor.codigo.trim().toUpperCase(),
      nombre: valor.nombre.trim(),
      categoria: valor.categoria,
      muestra: valor.muestra,
      precio: valor.precio,
      tiempoEntregaHoras: valor.tiempoEntregaHoras,
      indicaciones: valor.indicaciones.trim() || null,
      activo: valor.activo,
      parametros: (valor.parametros as Record<string, unknown>[]).map((p, indice) => ({
        nombre: String(p['nombre']).trim(),
        unidad: String(p['unidad'] ?? '').trim() || null,
        tipo: p['tipo'] as TipoParametro,
        valorMin: p['tipo'] === 'NUMERICO' ? (p['valorMin'] as number | null) : null,
        valorMax: p['tipo'] === 'NUMERICO' ? (p['valorMax'] as number | null) : null,
        orden: indice + 1,
      })),
    };
    const actual = this.editando();
    this.guardando.set(true);
    const operacion = actual
      ? this.store.actualizarExamen(actual.id, request)
      : this.store.crearExamen(request);
    operacion.subscribe({
      next: (examen) => {
        this.guardando.set(false);
        this.visible.set(false);
        this.notifications.success(
          actual ? 'El examen fue actualizado.' : `${examen.nombre} fue registrado.`,
        );
        this.cargar();
      },
      error: (error: { message: string }) => {
        this.guardando.set(false);
        this.error.set(error.message);
      },
    });
  }
}
