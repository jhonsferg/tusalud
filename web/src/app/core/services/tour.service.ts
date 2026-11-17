import { Injectable, computed, inject, signal } from '@angular/core';

import { Rol } from '@tusalud/app/core/models';
import { AuthService } from '@tusalud/app/core/services/auth.service';

export interface TourStep {
  readonly target: string | null;
  readonly title: string;
  readonly text: string;
  readonly icon: string;
  readonly roles?: readonly Rol[];
}

const PASOS: readonly TourStep[] = [
  {
    target: null,
    icon: 'pi-heart-fill',
    title: 'Te damos la bienvenida a TuSalud',
    text: 'Te mostramos en un minuto dónde está cada herramienta. Puedes salir cuando quieras y volver a abrir esta guía desde el menú de tu cuenta.',
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

@Injectable({ providedIn: 'root' })
export class TourService {
  private readonly auth = inject(AuthService);

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
    const visibles = PASOS.filter((paso) => !paso.roles || this.auth.hasAnyRole(paso.roles)).filter(
      (paso) => paso.target === null || this.element(paso.target) !== null,
    );
    if (visibles.length === 0) {
      return;
    }
    this.onClose = onClose ?? null;
    this.stepsState.set(visibles);
    this.indexState.set(0);
    this.activeState.set(true);
  }

  next(): void {
    if (this.isLast()) {
      this.close();
      return;
    }
    this.indexState.update((value) => value + 1);
  }

  previous(): void {
    this.indexState.update((value) => Math.max(0, value - 1));
  }

  close(): void {
    this.activeState.set(false);
    const callback = this.onClose;
    this.onClose = null;
    callback?.();
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
