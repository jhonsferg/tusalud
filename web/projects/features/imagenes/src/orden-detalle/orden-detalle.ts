import { CurrencyPipe, DatePipe } from '@angular/common';
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
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Button } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';
import { Tag } from 'primeng/tag';
import { Textarea } from 'primeng/textarea';
import { ToggleSwitch } from 'primeng/toggleswitch';

import {
  ArchivoImagenResponse,
  AuthService,
  ETIQUETA_ESTADO_IMAGEN,
  ETIQUETA_MODALIDAD,
  ICONO_MODALIDAD,
  ImagenesStore,
  NotificationService,
  OrdenImagenResponse,
  REGLAS_ARCHIVO,
  ReglasArchivo,
  SALAS_IMAGEN,
  optionalIdAttribute,
  toIsoDateTime,
} from '@tusalud/core';
import { CargaArchivos } from '@tusalud/ui/carga-archivos';
import { BotonDocumento } from '@tusalud/ui/boton-documento';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { SecurityNotice } from '@tusalud/ui/tablero';
import { ICONO_ESTADO_IMAGEN, SEVERIDAD_IMAGEN, tamanoLegible } from '../imagenes.utils';
import { VisorImagen } from '../visor/visor-imagen';

@Component({
  selector: 'app-orden-imagen-detalle',
  imports: [
    BotonDocumento,
    SecurityNotice,
    CargaArchivos,
    CurrencyPipe,
    DatePipe,
    FormsModule,
    ReactiveFormsModule,
    Button,
    DatePicker,
    Dialog,
    InputText,
    Message,
    Select,
    Tag,
    Textarea,
    ToggleSwitch,
    EmptyState,
    PageHeader,
    VisorImagen,
  ],
  templateUrl: './orden-detalle.html',
  styleUrl: './orden-detalle.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OrdenImagenDetalle {
  readonly id = input(undefined, { transform: optionalIdAttribute });

  protected readonly store = inject(ImagenesStore);
  private readonly auth = inject(AuthService);
  private readonly notifications = inject(NotificationService);
  private readonly motivos = inject(MotivoService);
  private readonly fb = inject(FormBuilder).nonNullable;

  protected readonly orden = this.store.orden.value;
  protected readonly esPersonal = computed(() => this.auth.isImagenes());
  protected readonly etiquetaEstado = ETIQUETA_ESTADO_IMAGEN;
  protected readonly etiquetaModalidad = ETIQUETA_MODALIDAD;
  protected readonly iconoModalidad = ICONO_MODALIDAD;
  protected readonly severidad = SEVERIDAD_IMAGEN;
  protected readonly iconoEstado = ICONO_ESTADO_IMAGEN;
  protected readonly tamano = tamanoLegible;
  protected readonly salas = [...SALAS_IMAGEN];
  protected readonly ahora = new Date();

  protected readonly puedeProgramar = computed(
    () =>
      this.esPersonal() &&
      (this.orden()?.estado === 'SOLICITADA' || this.orden()?.estado === 'PROGRAMADA'),
  );
  protected readonly puedeCargar = computed(
    () =>
      this.esPersonal() &&
      (this.orden()?.estado === 'PROGRAMADA' || this.orden()?.estado === 'REALIZADA'),
  );
  protected readonly puedeRealizar = computed(
    () => this.esPersonal() && this.orden()?.estado === 'PROGRAMADA',
  );
  protected readonly puedeInformar = computed(
    () => this.esPersonal() && this.orden()?.estado === 'REALIZADA',
  );
  protected readonly puedeCorregir = computed(
    () => this.esPersonal() && this.orden()?.estado === 'INFORMADA',
  );
  protected readonly puedeCancelar = computed(() => {
    const estado = this.orden()?.estado;
    return this.esPersonal()
      ? estado === 'SOLICITADA' || estado === 'PROGRAMADA'
      : estado === 'SOLICITADA';
  });

  protected readonly programarVisible = signal(false);
  protected readonly programarFecha = signal<Date | null>(null);
  protected readonly programarSala = signal<string | null>(null);
  protected readonly errorProgramar = signal<string | null>(null);

  protected readonly realizarVisible = signal(false);
  protected readonly observaciones = signal('');
  protected readonly errorRealizar = signal<string | null>(null);

  protected readonly subiendo = signal(false);
  protected readonly errorCarga = signal<string | null>(null);
  protected readonly pendientes = signal<File[]>([]);
  protected readonly progreso = signal<number | null>(null);
  protected readonly reglasImagen: ReglasArchivo = {
    ...REGLAS_ARCHIVO.IMAGEN_DIAGNOSTICA,
    maxArchivos: 12,
  };

  protected readonly visorVisible = signal(false);
  protected readonly visorIndice = signal(0);

  protected readonly informeVisible = signal(false);
  protected readonly guardandoInforme = signal(false);
  protected readonly errorInforme = signal<string | null>(null);
  protected readonly versionesVisibles = signal(false);

  protected readonly formInforme = this.fb.group({
    hallazgos: ['', [Validators.required, Validators.maxLength(4000)]],
    conclusion: ['', [Validators.required, Validators.maxLength(2000)]],
    hallazgoCritico: [false],
    firmaColegiatura: [
      '',
      [Validators.required, Validators.minLength(3), Validators.maxLength(30)],
    ],
    motivoCorreccion: ['', [Validators.maxLength(300)]],
  });

  constructor() {
    effect(() => {
      const id = this.id();
      if (id !== undefined) {
        untracked(() => {
          this.store.orden.reset();
          this.store.informes.reset();
          this.store.cargarOrden(id);
          if (this.esPersonal()) {
            this.store.cargarFirma();
          }
        });
      }
    });
    effect(() => {
      const orden = this.orden();
      if (orden && orden.estado === 'INFORMADA' && !this.store.informes.hasValue()) {
        untracked(() => this.store.cargarInformes(orden.id));
      }
    });
    effect(() => {
      const colegiatura = this.store.firma.value()?.colegiatura;
      if (colegiatura && !this.formInforme.controls.firmaColegiatura.value) {
        untracked(() => this.formInforme.controls.firmaColegiatura.setValue(colegiatura));
      }
    });
  }

  protected abrirProgramar(orden: OrdenImagenResponse): void {
    this.programarFecha.set(orden.programadaPara ? new Date(orden.programadaPara) : null);
    this.programarSala.set(orden.sala ?? null);
    this.errorProgramar.set(null);
    this.programarVisible.set(true);
  }

  protected programar(orden: OrdenImagenResponse): void {
    const fecha = this.programarFecha();
    const sala = this.programarSala();
    if (!fecha || !sala?.trim()) {
      this.errorProgramar.set('Indica la fecha, la hora y la sala.');
      return;
    }
    this.store
      .programar(orden.id, { programadaPara: toIsoDateTime(fecha), sala: sala.trim() })
      .subscribe({
        next: () => {
          this.programarVisible.set(false);
          this.notifications.success('El estudio fue programado y el paciente recibió el aviso.');
        },
        error: (error: { message: string }) => this.errorProgramar.set(error.message),
      });
  }

  protected elegidos(archivos: File[], orden: OrdenImagenResponse): void {
    this.pendientes.set(archivos);
    this.subir(orden, archivos);
  }

  private subir(orden: OrdenImagenResponse, archivos: File[]): void {
    if (archivos.length === 0) {
      return;
    }
    this.errorCarga.set(null);
    this.subiendo.set(true);
    this.progreso.set(null);
    this.store.subirArchivos(orden.id, archivos).subscribe({
      next: () => {
        this.subiendo.set(false);
        this.pendientes.set([]);
        this.notifications.success(
          archivos.length === 1 ? 'La imagen fue cargada.' : 'Las imágenes fueron cargadas.',
        );
      },
      error: (error: { message: string }) => {
        this.subiendo.set(false);
        this.pendientes.set([]);
        this.errorCarga.set(error.message);
      },
    });
  }

  protected quitar(orden: OrdenImagenResponse, archivo: ArchivoImagenResponse): void {
    this.store.eliminarArchivo(orden.id, archivo.id).subscribe({
      next: () => this.notifications.success('La imagen fue quitada.'),
      error: (error: { message: string }) => this.errorCarga.set(error.message),
    });
  }

  protected abrirVisor(indice: number): void {
    this.visorIndice.set(indice);
    this.visorVisible.set(true);
  }

  protected abrirRealizar(): void {
    this.observaciones.set('');
    this.errorRealizar.set(null);
    this.realizarVisible.set(true);
  }

  protected realizar(orden: OrdenImagenResponse): void {
    this.store
      .realizar(orden.id, { observaciones: this.observaciones().trim() || null })
      .subscribe({
        next: () => {
          this.realizarVisible.set(false);
          this.notifications.success('La toma quedó registrada y el estudio espera su informe.');
        },
        error: (error: { message: string }) => this.errorRealizar.set(error.message),
      });
  }

  protected abrirInforme(corregir: boolean, orden: OrdenImagenResponse): void {
    this.errorInforme.set(null);
    const informe = orden.informe;
    const controles = this.formInforme.controls;
    controles.hallazgos.setValue(corregir && informe ? informe.hallazgos : '');
    controles.conclusion.setValue(corregir && informe ? informe.conclusion : '');
    controles.hallazgoCritico.setValue(corregir ? (orden.hallazgoCritico ?? false) : false);
    controles.motivoCorreccion.setValue('');
    controles.motivoCorreccion.setValidators(
      corregir ? [Validators.required, Validators.maxLength(300)] : [Validators.maxLength(300)],
    );
    controles.motivoCorreccion.updateValueAndValidity();
    this.informeVisible.set(true);
  }

  protected guardarInforme(orden: OrdenImagenResponse): void {
    this.errorInforme.set(null);
    if (this.formInforme.invalid) {
      this.formInforme.markAllAsTouched();
      this.errorInforme.set('Completa los hallazgos, la conclusión y la colegiatura.');
      return;
    }
    const valor = this.formInforme.getRawValue();
    this.guardandoInforme.set(true);
    this.store
      .informar(orden.id, {
        hallazgos: valor.hallazgos.trim(),
        conclusion: valor.conclusion.trim(),
        hallazgoCritico: valor.hallazgoCritico,
        firmaColegiatura: valor.firmaColegiatura.trim(),
        motivoCorreccion: valor.motivoCorreccion.trim() || null,
      })
      .subscribe({
        next: () => {
          this.guardandoInforme.set(false);
          this.informeVisible.set(false);
          this.notifications.success('El informe fue guardado y se avisó al médico y al paciente.');
        },
        error: (error: { message: string }) => {
          this.guardandoInforme.set(false);
          this.errorInforme.set(error.message);
        },
      });
  }

  protected cancelar(orden: OrdenImagenResponse): void {
    this.motivos
      .solicitar({
        titulo: 'Cancelar la orden',
        descripcion: `Se cancelará la orden ${orden.codigo}.`,
        etiqueta: 'Motivo de la cancelación',
        confirmar: 'Cancelar orden',
        severidad: 'danger',
        icono: 'pi pi-times',
      })
      .subscribe((motivo) =>
        this.store.cancelar(orden.id, motivo).subscribe({
          next: () => this.notifications.success('La orden fue cancelada.'),
          error: () => undefined,
        }),
      );
  }

  protected recargar(): void {
    const id = this.id();
    if (id !== undefined) {
      this.store.cargarOrden(id);
    }
  }
}
