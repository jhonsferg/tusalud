import { DatePipe } from '@angular/common';
import { toSignal } from '@angular/core/rxjs-interop';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  effect,
  inject,
  input,
  signal,
  untracked,
} from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  FormsModule,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { ConfirmationService } from 'primeng/api';
import { Button } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Password } from 'primeng/password';
import { Select } from 'primeng/select';
import { SelectButton } from 'primeng/selectbutton';
import { Skeleton } from 'primeng/skeleton';
import { Tab, TabList, TabPanel, TabPanels, Tabs } from 'primeng/tabs';
import { Tag } from 'primeng/tag';
import { ToggleSwitch } from 'primeng/toggleswitch';

import {
  ANTICIPACIONES_RECORDATORIO,
  ApiError,
  ETIQUETA_ROL,
  SesionResponse,
} from '@tusalud/app/core/models';
import { NotificationService } from '@tusalud/app/core/services/notification.service';
import { PALETAS, ThemeMode, ThemeService } from '@tusalud/app/core/services/theme.service';
import { PerfilStore } from '@tusalud/app/core/state/perfil.store';
import { parseIsoDate, toIsoDate } from '@tusalud/app/core/utils/date';
import { AppValidators, PASSWORD_MAX } from '@tusalud/app/core/validators/app.validators';
import { applyServerErrors } from '@tusalud/app/core/validators/server-errors';
import { EmptyState } from '@tusalud/app/shared/components/empty-state/empty-state';
import { FormField } from '@tusalud/app/shared/components/form-field/form-field';
import { MfaPanel } from '@tusalud/app/features/perfil/mfa-panel/mfa-panel';
import { PageHeader } from '@tusalud/app/shared/components/page-header/page-header';
import { UserAvatar } from '@tusalud/app/shared/components/user-avatar/user-avatar';

const AVATAR_LADO = 256;
const AVATAR_MAX_ENTRADA = 8 * 1024 * 1024;

function passwordsIguales(group: AbstractControl): ValidationErrors | null {
  const nueva = group.get('passwordNueva');
  const confirmar = group.get('confirmar');
  if (!nueva || !confirmar || !confirmar.value) {
    return null;
  }
  const errores = { ...(confirmar.errors ?? {}) };
  if (nueva.value === confirmar.value) {
    delete errores['passwordMismatch'];
  } else {
    errores['passwordMismatch'] = true;
  }
  confirmar.setErrors(Object.keys(errores).length > 0 ? errores : null, { emitEvent: false });
  return null;
}

function reducirImagen(archivo: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const lector = new FileReader();
    lector.onerror = () => reject(new Error('No se pudo leer la imagen.'));
    lector.onload = () => {
      const imagen = new Image();
      imagen.onerror = () => reject(new Error('El archivo no es una imagen válida.'));
      imagen.onload = () => {
        const lienzo = document.createElement('canvas');
        lienzo.width = AVATAR_LADO;
        lienzo.height = AVATAR_LADO;
        const contexto = lienzo.getContext('2d');
        if (!contexto) {
          reject(new Error('No se pudo procesar la imagen.'));
          return;
        }
        const lado = Math.min(imagen.width, imagen.height);
        contexto.drawImage(
          imagen,
          (imagen.width - lado) / 2,
          (imagen.height - lado) / 2,
          lado,
          lado,
          0,
          0,
          AVATAR_LADO,
          AVATAR_LADO,
        );
        resolve(lienzo.toDataURL('image/jpeg', 0.85));
      };
      imagen.src = String(lector.result);
    };
    lector.readAsDataURL(archivo);
  });
}

@Component({
  selector: 'app-perfil',
  imports: [
    MfaPanel,
    DatePipe,
    FormsModule,
    ReactiveFormsModule,
    Button,
    DatePicker,
    InputText,
    Message,
    Password,
    Select,
    SelectButton,
    Skeleton,
    Tab,
    TabList,
    TabPanel,
    TabPanels,
    Tabs,
    Tag,
    ToggleSwitch,
    EmptyState,
    FormField,
    PageHeader,
    UserAvatar,
  ],
  templateUrl: './perfil.html',
  styleUrl: './perfil.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Perfil implements OnInit {
  readonly tab = input<string>();

  protected readonly store = inject(PerfilStore);
  protected readonly theme = inject(ThemeService);
  private readonly notifications = inject(NotificationService);
  private readonly confirmation = inject(ConfirmationService);
  private readonly fb = inject(FormBuilder);

  protected readonly guardando = signal(false);
  protected readonly cambiando = signal(false);
  protected readonly subiendoAvatar = signal(false);
  protected readonly guardandoPreferencias = signal(false);
  protected readonly errorPassword = signal<string | null>(null);
  protected readonly paletas = PALETAS;
  protected readonly modos: { label: string; value: ThemeMode; icon: string }[] = [
    { label: 'Claro', value: 'light', icon: 'pi pi-sun' },
    { label: 'Oscuro', value: 'dark', icon: 'pi pi-moon' },
    { label: 'Automático', value: 'auto', icon: 'pi pi-desktop' },
  ];
  protected readonly ayer = (() => {
    const fecha = new Date();
    fecha.setDate(fecha.getDate() - 1);
    return fecha;
  })();

  protected readonly tabActiva = computed(() => {
    const valida = ['datos', 'seguridad', 'preferencias', 'apariencia'];
    const solicitada = this.tab();
    return solicitada && valida.includes(solicitada) ? solicitada : 'datos';
  });

  protected readonly esPaciente = computed(() => !!this.store.perfil.value()?.dni);
  protected readonly esMedico = computed(() => !!this.store.perfil.value()?.medicoId);
  protected readonly roles = computed(() =>
    (this.store.perfil.value()?.roles ?? []).map((rol) => ETIQUETA_ROL[rol] ?? rol),
  );

  protected readonly datos = this.fb.group({
    nombre: ['', [Validators.required, AppValidators.notBlank, Validators.maxLength(100)]],
    apellido: ['', [Validators.required, AppValidators.notBlank, Validators.maxLength(100)]],
    telefono: ['', [AppValidators.telefono]],
    fechaNacimiento: [null as Date | null],
    consultorio: ['', [Validators.maxLength(50)]],
  });

  protected readonly seguridad = this.fb.nonNullable.group(
    {
      passwordActual: ['', [Validators.required]],
      passwordNueva: [
        '',
        [Validators.required, AppValidators.password, Validators.maxLength(PASSWORD_MAX)],
      ],
      confirmar: ['', [Validators.required]],
    },
    { validators: [passwordsIguales] },
  );

  protected readonly anticipaciones = ANTICIPACIONES_RECORDATORIO;
  protected readonly preferencias = this.fb.nonNullable.group({
    recordatoriosActivos: [true],
    minutosAntelacionRecordatorio: [1440, [Validators.required]],
    alertasInicioSesion: [true],
  });

  private readonly eventosDatos = toSignal(this.datos.events, { initialValue: null });
  private readonly eventosPreferencias = toSignal(this.preferencias.events, { initialValue: null });
  protected readonly datosModificados = computed(() => {
    this.eventosDatos();
    return this.datos.dirty;
  });
  protected readonly preferenciasModificadas = computed(() => {
    this.eventosPreferencias();
    return this.preferencias.dirty;
  });

  constructor() {
    effect(() => {
      const perfil = this.store.perfil.value();
      if (perfil && !untracked(() => this.datos.dirty)) {
        this.restablecer();
      }
    });
    effect(() => {
      const prefs = this.store.preferencias.value();
      if (prefs && !untracked(() => this.preferencias.dirty)) {
        this.preferencias.reset(prefs);
      }
    });
  }

  ngOnInit(): void {
    this.store.load();
    this.store.cargarPreferencias();
    this.store.cargarSesiones();
  }

  protected recargar(): void {
    this.store.load();
  }

  protected restablecer(): void {
    const perfil = this.store.perfil.value();
    if (!perfil) {
      return;
    }
    this.datos.reset({
      nombre: perfil.nombre,
      apellido: perfil.apellido,
      telefono: perfil.telefono ?? '',
      fechaNacimiento: perfil.fechaNacimiento ? parseIsoDate(perfil.fechaNacimiento) : null,
      consultorio: perfil.consultorio ?? '',
    });
  }

  protected guardar(): void {
    if (this.datos.invalid) {
      this.datos.markAllAsTouched();
      return;
    }
    const valor = this.datos.getRawValue();
    this.guardando.set(true);
    this.store
      .actualizar({
        nombre: valor.nombre!.trim(),
        apellido: valor.apellido!.trim(),
        telefono: valor.telefono || null,
        fechaNacimiento:
          this.esPaciente() && valor.fechaNacimiento ? toIsoDate(valor.fechaNacimiento) : null,
        consultorio: this.esMedico() ? valor.consultorio?.trim() || null : null,
      })
      .subscribe({
        next: () => {
          this.guardando.set(false);
          this.datos.markAsPristine();
          this.notifications.success('Tus datos fueron actualizados.');
        },
        error: (error: ApiError) => {
          this.guardando.set(false);
          applyServerErrors(this.datos, error);
        },
      });
  }

  protected async elegirAvatar(evento: Event): Promise<void> {
    const entrada = evento.target as HTMLInputElement;
    const archivo = entrada.files?.[0];
    entrada.value = '';
    if (!archivo) {
      return;
    }
    if (!archivo.type.startsWith('image/') || archivo.size > AVATAR_MAX_ENTRADA) {
      this.notifications.warning('Elige una imagen de hasta 8 MB.');
      return;
    }
    this.subiendoAvatar.set(true);
    try {
      const imagen = await reducirImagen(archivo);
      this.store.actualizarAvatar(imagen).subscribe({
        next: () => {
          this.subiendoAvatar.set(false);
          this.notifications.success('Tu foto de perfil fue actualizada.');
        },
        error: () => this.subiendoAvatar.set(false),
      });
    } catch (error) {
      this.subiendoAvatar.set(false);
      this.notifications.error(
        error instanceof Error ? error.message : 'No se pudo cargar la imagen.',
      );
    }
  }

  protected quitarAvatar(): void {
    this.store.eliminarAvatar().subscribe({
      next: () => this.notifications.success('Tu foto de perfil fue eliminada.'),
      error: () => undefined,
    });
  }

  protected cambiarPassword(): void {
    this.errorPassword.set(null);
    if (this.seguridad.invalid) {
      this.seguridad.markAllAsTouched();
      return;
    }
    const { passwordActual, passwordNueva } = this.seguridad.getRawValue();
    this.cambiando.set(true);
    this.store.cambiarPassword({ passwordActual, passwordNueva }).subscribe({
      next: () => {
        this.cambiando.set(false);
        this.seguridad.reset();
        this.notifications.success('Tu contraseña fue actualizada.');
      },
      error: (error: ApiError) => {
        this.cambiando.set(false);
        const sinMapear = applyServerErrors(this.seguridad, error);
        this.errorPassword.set(sinMapear[0] ?? error.message);
      },
    });
  }

  protected cerrarSesion(sesion: SesionResponse): void {
    this.confirmation.confirm({
      header: 'Cerrar sesión remota',
      message: `¿Quieres cerrar la sesión iniciada desde ${sesion.dispositivo}?`,
      icon: 'pi pi-sign-out',
      acceptLabel: 'Cerrar sesión',
      rejectLabel: 'Cancelar',
      acceptButtonProps: { severity: 'danger' },
      rejectButtonProps: { severity: 'secondary', outlined: true },
      accept: () =>
        this.store.cerrarSesion(sesion.id).subscribe({
          next: () => this.notifications.success('La sesión fue cerrada.'),
          error: () => undefined,
        }),
    });
  }

  protected cerrarOtras(): void {
    this.confirmation.confirm({
      header: 'Cerrar las demás sesiones',
      message: 'Se cerrarán todas las sesiones excepto la de este dispositivo.',
      icon: 'pi pi-sign-out',
      acceptLabel: 'Cerrar sesiones',
      rejectLabel: 'Cancelar',
      acceptButtonProps: { severity: 'danger' },
      rejectButtonProps: { severity: 'secondary', outlined: true },
      accept: () =>
        this.store.cerrarOtrasSesiones().subscribe({
          next: () => this.notifications.success('Se cerraron las demás sesiones.'),
          error: () => undefined,
        }),
    });
  }

  protected guardarPreferencias(): void {
    if (this.preferencias.invalid) {
      this.preferencias.markAllAsTouched();
      return;
    }
    this.guardandoPreferencias.set(true);
    this.store.guardarPreferencias(this.preferencias.getRawValue()).subscribe({
      next: () => {
        this.guardandoPreferencias.set(false);
        this.preferencias.markAsPristine();
        this.notifications.success('Tus preferencias fueron guardadas.');
      },
      error: () => this.guardandoPreferencias.set(false),
    });
  }
}
