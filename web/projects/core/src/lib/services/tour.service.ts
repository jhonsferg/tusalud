import {
  EnvironmentProviders,
  Injectable,
  InjectionToken,
  Provider,
  computed,
  inject,
  makeEnvironmentProviders,
  signal,
} from '@angular/core';

import { Router } from '@angular/router';

import { Rol } from '../models';
import { AuthService } from './auth.service';

export interface TourStep {
  readonly target: string | null;
  readonly title: string;
  readonly text: string;
  readonly icon: string;
  readonly roles?: readonly Rol[];
}

export const PASOS_TOUR_PREDETERMINADOS: readonly TourStep[] = [
  {
    target: null,
    icon: 'pi-heart-fill',
    title: 'Te damos la bienvenida a TuSalud',
    text: 'Te mostramos en un minuto dónde está cada herramienta. Puedes salir cuando quieras y volver a abrir esta guía desde el menú de tu cuenta.',
  },
  {
    target: 'nav:/inicio',
    icon: 'pi-home',
    title: 'Tu inicio',
    text: 'Tu tablero con lo más importante de tu día y accesos rápidos a lo que más usas.',
  },
  {
    target: 'nav:/admin/panel',
    icon: 'pi-th-large',
    title: 'Panel de control',
    text: 'Ingresos, citas por estado y médicos destacados de un vistazo.',
    roles: ['ROLE_ADMIN'],
  },
  {
    target: 'nav:/admin/usuarios',
    icon: 'pi-id-card',
    title: 'Usuarios y roles',
    text: 'Activa o desactiva cuentas y define qué rol tiene cada persona dentro de la clínica.',
    roles: ['ROLE_ADMIN'],
  },
  {
    target: 'nav:/especialidades',
    icon: 'pi-heart',
    title: 'Especialidades y tarifas',
    text: 'Cada especialidad tiene su precio de consulta. Cámbialo aquí y se aplica a las nuevas reservas.',
    roles: ['ROLE_ADMIN'],
  },
  {
    target: 'nav:/admin/pagos',
    icon: 'pi-wallet',
    title: 'Pagos y cupones',
    text: 'Revisa los pagos registrados y crea cupones de descuento para tus pacientes.',
    roles: ['ROLE_ADMIN'],
  },
  {
    target: 'nav:/agenda',
    icon: 'pi-calendar',
    title: 'Tu agenda',
    text: 'Consulta las citas del día, confírmalas y registra cada atención.',
    roles: ['ROLE_MEDICO'],
  },
  {
    target: 'nav:/pacientes',
    icon: 'pi-users',
    title: 'Tus pacientes',
    text: 'Accede a la historia clínica de quienes atiendes y comparte un paciente con otro colega cuando haga falta.',
    roles: ['ROLE_MEDICO'],
  },
  {
    target: 'nav:/citas/nueva',
    icon: 'pi-calendar-plus',
    title: 'Reserva tu cita',
    text: 'Elige especialidad, médico y un horario libre. Verás el precio antes de confirmar.',
    roles: ['ROLE_PACIENTE'],
  },
  {
    target: 'nav:/citas',
    icon: 'pi-calendar-clock',
    title: 'Mis citas',
    text: 'Aquí ves tus próximas citas y el historial. Puedes reprogramar, cancelar o valorar la atención.',
    roles: ['ROLE_PACIENTE'],
  },
  {
    target: 'nav:/historia-clinica',
    icon: 'pi-folder-open',
    title: 'Tu historia clínica',
    text: 'Diagnósticos, recetas y tus datos médicos, siempre a la mano.',
    roles: ['ROLE_PACIENTE'],
  },
  {
    target: 'nav:/pagos',
    icon: 'pi-wallet',
    title: 'Pagos',
    text: 'Paga con tarjeta, Yape o Plin y descarga tus comprobantes.',
    roles: ['ROLE_PACIENTE'],
  },
  {
    target: 'nav:/laboratorio',
    icon: 'pi-filter',
    title: 'Laboratorio',
    text: 'Solicita exámenes, sigue su avance y revisa los resultados con los valores fuera de rango resaltados.',
    roles: ['ROLE_MEDICO', 'ROLE_LABORATORIO', 'ROLE_ADMIN'],
  },
  {
    target: 'nav:/imagenes',
    icon: 'pi-image',
    title: 'Imágenes diagnósticas',
    text: 'Programa los estudios, registra la toma con sus imágenes y emite el informe firmado.',
    roles: ['ROLE_IMAGENES'],
  },
  {
    target: 'nav:/imagenes/solicitar',
    icon: 'pi-image',
    title: 'Estudios de imagen',
    text: 'Solicita radiografías, ecografías, tomografías o resonancias y revisa sus informes.',
    roles: ['ROLE_MEDICO'],
  },
  {
    target: 'nav:/mis-estudios',
    icon: 'pi-image',
    title: 'Mis estudios',
    text: 'Aquí ves la preparación de tus estudios de imagen y, cuando están listos, su informe y sus imágenes.',
    roles: ['ROLE_PACIENTE'],
  },
  {
    target: 'nav:/mis-resultados',
    icon: 'pi-filter',
    title: 'Mis resultados',
    text: 'Aquí aparecen tus exámenes de laboratorio apenas el laboratorio los publica.',
    roles: ['ROLE_PACIENTE'],
  },
  {
    target: 'nav:/mis-medicamentos',
    icon: 'pi-heart',
    title: 'Mis medicamentos',
    text: 'Revisa lo que la farmacia te entregó con tus recetas.',
    roles: ['ROLE_PACIENTE'],
  },
  {
    target: 'nav:/farmacia',
    icon: 'pi-plus-circle',
    title: 'Farmacia',
    text: 'Atiende las recetas pendientes, vende productos y consulta el historial de comprobantes.',
    roles: ['ROLE_FARMACIA'],
  },
  {
    target: 'nav:/almacen',
    icon: 'pi-box',
    title: 'Almacén',
    text: 'Controla el stock, los lotes con su vencimiento, los proveedores y las alertas de reposición.',
    roles: ['ROLE_ALMACEN'],
  },
  {
    target: 'nav:/cola',
    icon: 'pi-users',
    title: 'Cola de pacientes',
    text: 'Cuando recepción registra la llegada de un paciente, aparece aquí para que lo llames y lo atiendas.',
    roles: ['ROLE_MEDICO'],
  },
  {
    target: 'nav:/recepcion',
    icon: 'pi-users',
    title: 'Recepción',
    text: 'Registra llegadas, administra la cola de turnos, da de alta pacientes y reserva citas en mostrador.',
    roles: ['ROLE_RECEPCION'],
  },
  {
    target: 'nav:/caja',
    icon: 'pi-wallet',
    title: 'Caja',
    text: 'Abre tu caja, cobra consultas y servicios, emite boletas y facturas y cierra el turno con su diferencia.',
    roles: ['ROLE_CAJA'],
  },
  {
    target: 'nav:/hospitalizacion',
    icon: 'pi-building',
    title: 'Hospitalización',
    text: 'Consulta el mapa de camas, registra triajes de emergencia, evoluciones y altas.',
    roles: ['ROLE_ENFERMERIA', 'ROLE_MEDICO'],
  },
  {
    target: 'nav:/mis-ingresos',
    icon: 'pi-building',
    title: 'Mis hospitalizaciones',
    text: 'Si alguna vez te hospitalizan, aquí ves tu ingreso, tu estancia y las indicaciones de alta.',
    roles: ['ROLE_PACIENTE'],
  },
  {
    target: 'nav:/reportes/ejecutivo',
    icon: 'pi-chart-line',
    title: 'Reporte ejecutivo',
    text: 'Los indicadores clave de la clínica con su variación frente al periodo anterior.',
    roles: ['ROLE_GERENCIA', 'ROLE_ADMIN'],
  },
  {
    target: 'nav:/reportes/financiero',
    icon: 'pi-money-bill',
    title: 'Reporte financiero',
    text: 'Lo cobrado por día, método, origen, especialidad y médico, las anulaciones y las diferencias de caja.',
    roles: ['ROLE_FINANZAS', 'ROLE_GERENCIA', 'ROLE_ADMIN'],
  },
  {
    target: 'nav:/reportes/operativo',
    icon: 'pi-sitemap',
    title: 'Reporte operativo',
    text: 'Ocupación de camas, esperas, demanda por hora, uso de la agenda e inventario crítico.',
    roles: ['ROLE_OPERACIONES', 'ROLE_GERENCIA', 'ROLE_ADMIN'],
  },
  {
    target: 'nav:/reportes/calidad',
    icon: 'pi-verified',
    title: 'Reporte de calidad',
    text: 'Valoraciones por médico y especialidad, cancelaciones, ausencias y tiempos de entrega de laboratorio.',
    roles: ['ROLE_CALIDAD', 'ROLE_GERENCIA', 'ROLE_ADMIN'],
  },
  {
    target: 'nav:/rrhh',
    icon: 'pi-id-card',
    title: 'Recursos humanos',
    text: 'Resume la plantilla, administra las fichas laborales, asigna turnos y resuelve las solicitudes de ausencia.',
    roles: ['ROLE_RRHH', 'ROLE_GERENCIA', 'ROLE_ADMIN'],
  },
  {
    target: 'nav:/mi-espacio/turnos',
    icon: 'pi-clock',
    title: 'Mi espacio',
    text: 'Consulta tus turnos, solicita vacaciones o permisos y revisa tu ficha laboral.',
    roles: [
      'ROLE_ADMIN',
      'ROLE_MEDICO',
      'ROLE_ENFERMERIA',
      'ROLE_LABORATORIO',
      'ROLE_FARMACIA',
      'ROLE_ALMACEN',
      'ROLE_RECEPCION',
      'ROLE_CAJA',
      'ROLE_GERENCIA',
      'ROLE_FINANZAS',
      'ROLE_OPERACIONES',
      'ROLE_RRHH',
      'ROLE_CALIDAD',
      'ROLE_IMAGENES',
    ],
  },
  {
    target: 'nav:/mis-exportaciones',
    icon: 'pi-file-export',
    title: 'Mis exportaciones',
    text: 'Aquí encuentras los reportes y listados que exportaste a PDF o Excel. Se conservan unos días y solo tú puedes descargarlos.',
    roles: [
      'ROLE_ADMIN',
      'ROLE_GERENCIA',
      'ROLE_FINANZAS',
      'ROLE_OPERACIONES',
      'ROLE_CALIDAD',
      'ROLE_CAJA',
      'ROLE_ALMACEN',
      'ROLE_RRHH',
      'ROLE_MEDICO',
    ],
  },
  {
    target: 'nav:/plantillas',
    icon: 'pi-file-edit',
    title: 'Plantillas de documentos',
    text: 'Personaliza las plantillas con las que se generan los comprobantes, recetas, informes y reportes en PDF.',
    roles: ['ROLE_ADMIN'],
  },
  {
    target: 'nav:/calendario',
    icon: 'pi-calendar-plus',
    title: 'Calendario',
    text: 'Mira tus citas por mes, semana o día, arrastra para crear reuniones y tareas y recibe avisos antes de que empiecen.',
  },
  {
    target: 'bell',
    icon: 'pi-bell',
    title: 'Notificaciones en vivo',
    text: 'Te avisamos al instante cuando una cita cambia de estado, sin que tengas que recargar.',
  },
  {
    target: 'tema',
    icon: 'pi-moon',
    title: 'Modo claro y oscuro',
    text: 'Cambia la apariencia cuando quieras. También puedes elegir el color principal en tu perfil.',
  },
  {
    target: 'usuario',
    icon: 'pi-user',
    title: 'Tu cuenta',
    text: 'Desde aquí editas tu perfil, activas la verificación en dos pasos, revisas tus sesiones y vuelves a abrir esta guía.',
  },
  {
    target: null,
    icon: 'pi-check-circle',
    title: 'Todo listo',
    text: 'Ya conoces lo esencial. Si tienes dudas, abre la guía de nuevo desde el menú de tu cuenta.',
  },
];

export const TOUR_STEPS = new InjectionToken<readonly TourStep[]>('TOUR_STEPS', {
  providedIn: 'root',
  factory: () => PASOS_TOUR_PREDETERMINADOS,
});

export function provideTourSteps(pasos: readonly TourStep[]): EnvironmentProviders {
  const proveedor: Provider = { provide: TOUR_STEPS, useValue: pasos };
  return makeEnvironmentProviders([proveedor]);
}

@Injectable({ providedIn: 'root' })
export class TourService {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly pasos = inject(TOUR_STEPS);

  private readonly activeState = signal(false);
  private readonly indexState = signal(0);
  private readonly stepsState = signal<readonly TourStep[]>([]);
  private onClose: (() => void) | null = null;

  readonly active = this.activeState.asReadonly();
  readonly index = this.indexState.asReadonly();
  readonly steps = this.stepsState.asReadonly();
  readonly step = computed(() => this.stepsState()[this.indexState()] ?? null);
  readonly isFirst = computed(() => this.indexState() === 0);
  readonly isLast = computed(() => this.indexState() >= this.stepsState().length - 1);

  start(onClose?: () => void): void {
    const visibles = this.pasos
      .filter((paso) => !paso.roles || this.auth.hasAnyRole(paso.roles))
      .filter((paso) => paso.target === null || this.element(paso.target) !== null);
    if (visibles.length === 0) {
      return;
    }
    this.onClose = onClose ?? null;
    this.stepsState.set(visibles);
    this.indexState.set(0);
    this.activeState.set(true);
    this.navegarAlPaso();
  }

  debeOfrecerse(completadoEnServidor: boolean): boolean {
    if (completadoEnServidor) {
      return false;
    }
    try {
      return localStorage.getItem(this.claveVisto()) === null;
    } catch {
      return true;
    }
  }

  marcarVisto(): void {
    try {
      localStorage.setItem(this.claveVisto(), new Date().toISOString());
    } catch {
      return;
    }
  }

  next(): void {
    if (this.isLast()) {
      this.close();
      return;
    }
    this.indexState.update((value) => value + 1);
    this.navegarAlPaso();
  }

  previous(): void {
    this.indexState.update((value) => Math.max(0, value - 1));
    this.navegarAlPaso();
  }

  close(): void {
    const estabaActivo = this.activeState();
    this.activeState.set(false);
    const callback = this.onClose;
    this.onClose = null;
    callback?.();
    if (estabaActivo) {
      this.navegar(this.auth.homeUrl());
    }
  }

  private claveVisto(): string {
    return `tusalud.tour.visto.${this.auth.user()?.id ?? 'anonimo'}`;
  }

  private navegarAlPaso(): void {
    const target = this.step()?.target;
    if (target?.startsWith('nav:')) {
      this.navegar(target.slice(4));
    }
  }

  private navegar(url: string): void {
    this.router.navigateByUrl(url).catch(() => false);
  }

  element(target: string): HTMLElement | null {
    const candidates = document.querySelectorAll<HTMLElement>(`[data-tour="${target}"]`);
    return (
      Array.from(candidates).find((el) => {
        const rect = el.getBoundingClientRect();
        return rect.width > 0 && rect.height > 0;
      }) ?? null
    );
  }
}
