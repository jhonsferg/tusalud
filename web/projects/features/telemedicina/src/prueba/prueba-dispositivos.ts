import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  computed,
  effect,
  inject,
  input,
  signal,
  untracked,
  viewChild,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Button } from 'primeng/button';
import { Checkbox } from 'primeng/checkbox';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';
import { firstValueFrom } from 'rxjs';

import {
  AuthService,
  NotificationService,
  TeleconsultaApiService,
  TeleconsultaStore,
  optionalIdAttribute,
} from '@tusalud/core';
import { PageHeader } from '@tusalud/ui/page-header';
import { SecurityNotice } from '@tusalud/ui/tablero';
import { DispositivosService, mensajeDeMedios } from '../dispositivos.service';

interface Opcion {
  label: string;
  value: string;
}

@Component({
  selector: 'app-prueba-dispositivos',
  imports: [DatePipe, FormsModule, Button, Checkbox, Message, Select, PageHeader, SecurityNotice],
  templateUrl: './prueba-dispositivos.html',
  styleUrl: './prueba-dispositivos.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PruebaDispositivos {
  readonly id = input(undefined, { transform: optionalIdAttribute });

  protected readonly store = inject(TeleconsultaStore);
  private readonly api = inject(TeleconsultaApiService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly notifications = inject(NotificationService);
  private readonly dispositivos = inject(DispositivosService);

  private readonly video = viewChild<ElementRef<HTMLVideoElement>>('vista');
  private flujo: MediaStream | null = null;
  private contexto: AudioContext | null = null;
  private animacion = 0;

  protected readonly acepta = signal(false);
  protected readonly guardando = signal(false);
  protected readonly probando = signal(false);
  protected readonly errorMedios = signal<string | null>(null);
  protected readonly camaras = signal<Opcion[]>([]);
  protected readonly microfonos = signal<Opcion[]>([]);
  protected readonly camaraElegida = signal<string | null>(null);
  protected readonly microfonoElegido = signal<string | null>(null);
  protected readonly nivelAudio = signal(0);
  protected readonly dispositivosListos = signal(false);
  protected readonly latencia = signal<number | null>(null);
  protected readonly probandoRed = signal(false);

  protected readonly teleconsulta = this.store.detalle.value;
  protected readonly esMedico = computed(() => this.auth.isMedico());
  protected readonly requiereConsentimiento = computed(
    () => this.teleconsulta()?.consentimientoPendiente === true,
  );
  protected readonly redBuena = computed(() => {
    const ms = this.latencia();
    return ms === null ? null : ms < 400;
  });
  protected readonly puedeEntrar = computed(
    () =>
      !!this.teleconsulta()?.puedeIngresar &&
      !this.requiereConsentimiento() &&
      this.dispositivosListos(),
  );

  constructor() {
    effect(() => {
      const id = this.id();
      if (id !== undefined) {
        untracked(() => {
          this.store.cargar(id);
          if (!this.auth.isMedico()) {
            this.store.cargarConsentimiento();
          }
        });
      }
    });
    inject(DestroyRef).onDestroy(() => this.detener());
  }

  protected aceptar(): void {
    const consentimiento = this.store.consentimiento.value();
    if (!consentimiento || !this.acepta()) {
      return;
    }
    this.guardando.set(true);
    this.store.aceptarConsentimiento(consentimiento.version).subscribe({
      next: () => {
        this.guardando.set(false);
        this.notifications.success('Registramos tu consentimiento.');
      },
      error: (error: { message: string }) => {
        this.guardando.set(false);
        this.notifications.error(error.message);
      },
    });
  }

  protected async probar(): Promise<void> {
    this.probando.set(true);
    this.errorMedios.set(null);
    this.detener();
    try {
      const restricciones = this.dispositivos.restricciones();
      this.flujo = await navigator.mediaDevices.getUserMedia(restricciones);
      const elementos = await navigator.mediaDevices.enumerateDevices();
      this.camaras.set(
        elementos
          .filter((d) => d.kind === 'videoinput')
          .map((d, i) => ({ value: d.deviceId, label: d.label || 'Cámara ' + (i + 1) })),
      );
      this.microfonos.set(
        elementos
          .filter((d) => d.kind === 'audioinput')
          .map((d, i) => ({ value: d.deviceId, label: d.label || 'Micrófono ' + (i + 1) })),
      );
      this.camaraElegida.set(this.flujo.getVideoTracks()[0]?.getSettings().deviceId ?? null);
      this.microfonoElegido.set(this.flujo.getAudioTracks()[0]?.getSettings().deviceId ?? null);
      queueMicrotask(() => {
        const elemento = this.video()?.nativeElement;
        if (elemento && this.flujo) {
          elemento.srcObject = this.flujo;
        }
      });
      this.medirVolumen();
      this.dispositivosListos.set(true);
    } catch (error) {
      this.dispositivosListos.set(false);
      this.errorMedios.set(mensajeDeMedios(error));
    } finally {
      this.probando.set(false);
    }
  }

  protected cambiarCamara(id: string): void {
    this.camaraElegida.set(id);
    this.dispositivos.camaraId.set(id);
    void this.probar();
  }

  protected cambiarMicrofono(id: string): void {
    this.microfonoElegido.set(id);
    this.dispositivos.microfonoId.set(id);
    void this.probar();
  }

  protected async probarRed(): Promise<void> {
    const id = this.id();
    if (!id) {
      return;
    }
    this.probandoRed.set(true);
    const tiempos: number[] = [];
    try {
      for (let i = 0; i < 3; i++) {
        const inicio = performance.now();
        await firstValueFrom(this.api.ice(id));
        tiempos.push(performance.now() - inicio);
      }
      this.latencia.set(Math.round(tiempos.reduce((a, b) => a + b, 0) / tiempos.length));
    } catch {
      this.latencia.set(9999);
    } finally {
      this.probandoRed.set(false);
    }
  }

  protected entrar(): void {
    const id = this.id();
    if (id) {
      this.detener();
      void this.router.navigate(['/teleconsultas', id, 'sala']);
    }
  }

  private medirVolumen(): void {
    if (!this.flujo || this.flujo.getAudioTracks().length === 0) {
      return;
    }
    this.contexto = new AudioContext();
    const analizador = this.contexto.createAnalyser();
    analizador.fftSize = 256;
    this.contexto.createMediaStreamSource(this.flujo).connect(analizador);
    const datos = new Uint8Array(analizador.frequencyBinCount);
    const ciclo = () => {
      analizador.getByteFrequencyData(datos);
      const promedio = datos.reduce((a, b) => a + b, 0) / datos.length;
      this.nivelAudio.set(Math.min(100, Math.round((promedio / 128) * 100)));
      this.animacion = requestAnimationFrame(ciclo);
    };
    ciclo();
  }

  private detener(): void {
    cancelAnimationFrame(this.animacion);
    this.flujo?.getTracks().forEach((pista) => pista.stop());
    this.flujo = null;
    void this.contexto?.close();
    this.contexto = null;
  }
}
