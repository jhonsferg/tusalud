import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  afterRenderEffect,
  computed,
  effect,
  inject,
  input,
  signal,
  untracked,
  viewChild,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { firstValueFrom } from 'rxjs';

import {
  AuthService,
  IceServer,
  NotificationService,
  ResumenPacienteTeleconsultaResponse,
  TeleconsultaApiService,
  TeleconsultaResponse,
  optionalIdAttribute,
} from '@tusalud/core';
import { SecurityNotice } from '@tusalud/ui/tablero';
import { DispositivosService, mensajeDeMedios } from '../dispositivos.service';
import { NivelCalidad, medirCalidad } from '../llamada/calidad';
import {
  CanalSenalizacion,
  MensajeEntrante,
  urlDeSenalizacion,
} from '../llamada/canal-senalizacion';

export type EstadoConexion =
  | 'preparando'
  | 'conectando'
  | 'esperando'
  | 'negociando'
  | 'conectado'
  | 'reconectando'
  | 'finalizada'
  | 'error';

interface MensajeChat {
  propio: boolean;
  texto: string;
  hora: Date;
}

const MAX_REINTENTOS = 5;

@Component({
  selector: 'app-sala-teleconsulta',
  imports: [DatePipe, FormsModule, RouterLink, Button, InputText, Message, SecurityNotice],
  templateUrl: './sala-teleconsulta.html',
  styleUrl: './sala-teleconsulta.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SalaTeleconsulta {
  readonly id = input(undefined, { transform: optionalIdAttribute });

  private readonly api = inject(TeleconsultaApiService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly notifications = inject(NotificationService);
  private readonly dispositivos = inject(DispositivosService);

  private readonly videoRemoto = viewChild<ElementRef<HTMLVideoElement>>('remoto');
  private readonly videoLocal = viewChild<ElementRef<HTMLVideoElement>>('local');

  private canal: CanalSenalizacion | null = null;
  private pc: RTCPeerConnection | null = null;
  private flujoLocal: MediaStream | null = null;
  private flujoRemoto: MediaStream | null = null;
  private flujoPantalla: MediaStream | null = null;
  private servidoresIce: IceServer[] = [];
  private candidatosPendientes: RTCIceCandidateInit[] = [];
  private reintentos = 0;
  private cerrando = false;
  private temporizadorCalidad: ReturnType<typeof setInterval> | null = null;
  private reconexionesMedidas = 0;

  protected readonly teleconsulta = signal<TeleconsultaResponse | null>(null);
  protected readonly resumen = signal<ResumenPacienteTeleconsultaResponse | null>(null);
  protected readonly conexion = signal<EstadoConexion>('preparando');
  protected readonly error = signal<string | null>(null);
  protected readonly aviso = signal<string | null>(null);
  protected readonly medicoEnSala = signal(false);
  protected readonly pacienteEnSala = signal(false);
  protected readonly admitido = signal(false);
  protected readonly microfono = signal(true);
  protected readonly camara = signal(true);
  protected readonly pantalla = signal(false);
  protected readonly calidad = signal<NivelCalidad>('sin');
  protected readonly mensajes = signal<MensajeChat[]>([]);
  protected readonly texto = signal('');
  protected readonly finalizando = signal(false);
  protected readonly chatAbierto = signal(true);

  protected readonly esMedico = computed(() => this.auth.isMedico());
  protected readonly contraparte = computed(() => {
    const t = this.teleconsulta();
    return t ? (this.esMedico() ? t.pacienteNombre : t.medicoNombre) : '';
  });
  protected readonly contraparteEnSala = computed(() =>
    this.esMedico() ? this.pacienteEnSala() : this.medicoEnSala(),
  );
  protected readonly enLlamada = computed(() =>
    ['negociando', 'conectado', 'reconectando'].includes(this.conexion()),
  );
  protected readonly etiquetaCalidad = computed(() => {
    switch (this.calidad()) {
      case 'buena':
        return 'Conexión buena';
      case 'regular':
        return 'Conexión regular';
      case 'mala':
        return 'Conexión débil';
      default:
        return 'Midiendo la conexión';
    }
  });

  constructor() {
    effect(() => {
      const id = this.id();
      if (id !== undefined) {
        untracked(() => void this.iniciar(id));
      }
    });
    afterRenderEffect(() => {
      const remoto = this.videoRemoto()?.nativeElement;
      if (remoto && this.flujoRemoto && remoto.srcObject !== this.flujoRemoto) {
        remoto.srcObject = this.flujoRemoto;
      }
      const local = this.videoLocal()?.nativeElement;
      if (local && this.flujoLocal && local.srcObject !== this.flujoLocal) {
        local.srcObject = this.flujoLocal;
      }
    });
    inject(DestroyRef).onDestroy(() => this.limpiar());
  }

  protected admitir(): void {
    this.canal?.enviar({ t: 'admitir' });
  }

  protected finalizar(): void {
    this.finalizando.set(true);
    this.canal?.enviar({ t: 'finalizar' });
  }

  protected salir(): void {
    this.canal?.enviar({ t: 'salir' });
    this.limpiar();
    void this.router.navigate(['/teleconsultas']);
  }

  protected alternarMicrofono(): void {
    const activo = !this.microfono();
    this.microfono.set(activo);
    this.flujoLocal?.getAudioTracks().forEach((pista) => (pista.enabled = activo));
  }

  protected alternarCamara(): void {
    const activa = !this.camara();
    this.camara.set(activa);
    this.flujoLocal?.getVideoTracks().forEach((pista) => (pista.enabled = activa));
  }

  protected async alternarPantalla(): Promise<void> {
    if (this.pantalla()) {
      await this.detenerPantalla();
      return;
    }
    try {
      this.flujoPantalla = await navigator.mediaDevices.getDisplayMedia({ video: true });
    } catch {
      return;
    }
    const pista = this.flujoPantalla.getVideoTracks()[0];
    const emisor = this.pc?.getSenders().find((s) => s.track?.kind === 'video');
    if (!pista || !emisor) {
      return;
    }
    await emisor.replaceTrack(pista);
    pista.onended = () => void this.detenerPantalla();
    this.pantalla.set(true);
  }

  protected enviarChat(): void {
    const texto = this.texto().trim();
    if (!texto || !this.admitido()) {
      return;
    }
    this.canal?.enviar({ t: 'chat', texto });
    this.mensajes.update((lista) => [...lista, { propio: true, texto, hora: new Date() }]);
    this.texto.set('');
  }

  protected reintentar(): void {
    const id = this.id();
    if (id) {
      this.error.set(null);
      this.limpiar();
      this.cerrando = false;
      this.reintentos = 0;
      void this.iniciar(id);
    }
  }

  private async iniciar(id: string): Promise<void> {
    this.conexion.set('preparando');
    try {
      const t = await firstValueFrom(this.api.obtener(id));
      this.teleconsulta.set(t);
      if (t.consentimientoPendiente) {
        await this.router.navigate(['/teleconsultas', id, 'prueba']);
        return;
      }
      if (!t.puedeIngresar) {
        this.fallar(
          'La sala no está disponible en este momento. Revisa el horario de tu teleconsulta.',
        );
        return;
      }
      if (this.esMedico()) {
        this.api
          .resumenPaciente(id)
          .subscribe({ next: (r) => this.resumen.set(r), error: () => undefined });
      }
      this.flujoLocal = await navigator.mediaDevices.getUserMedia(
        this.dispositivos.restricciones(),
      );
      this.servidoresIce = (await firstValueFrom(this.api.ice(id))).iceServers;
      await this.abrirCanal(id);
    } catch (error) {
      const nombre = (error as { name?: string } | null)?.name;
      this.fallar(
        nombre
          ? mensajeDeMedios(error)
          : ((error as { message?: string })?.message ?? 'No pudimos iniciar la teleconsulta.'),
      );
    }
  }

  private async abrirCanal(id: string): Promise<void> {
    this.conexion.set('conectando');
    const ticket = await firstValueFrom(this.api.ticket(id));
    this.canal = new CanalSenalizacion(urlDeSenalizacion(ticket.ruta, ticket.ticket), {
      alAbrir: () => {
        this.reintentos = 0;
        if (this.conexion() !== 'conectado') {
          this.conexion.set('esperando');
        }
      },
      alMensaje: (mensaje) => void this.manejar(mensaje),
      alCerrar: () => this.alCerrarCanal(id),
    });
    this.canal.abrir();
  }

  private alCerrarCanal(id: string): void {
    if (this.cerrando || this.conexion() === 'finalizada' || this.conexion() === 'error') {
      return;
    }
    if (this.reintentos >= MAX_REINTENTOS) {
      this.fallar('Se perdió la conexión con la sala. Vuelve a intentarlo.');
      return;
    }
    this.reintentos++;
    this.reconexionesMedidas++;
    this.conexion.set('reconectando');
    setTimeout(
      () => {
        if (!this.cerrando) {
          void this.abrirCanal(id).catch(() => this.alCerrarCanal(id));
        }
      },
      1000 * 2 ** (this.reintentos - 1),
    );
  }

  private async manejar(mensaje: MensajeEntrante): Promise<void> {
    switch (mensaje.t) {
      case 'estado':
        await this.alEstado(mensaje);
        break;
      case 'offer':
        await this.alOferta(String(mensaje['sdp']));
        break;
      case 'answer':
        await this.pc?.setRemoteDescription({ type: 'answer', sdp: String(mensaje['sdp']) });
        await this.vaciarCandidatos();
        break;
      case 'ice':
        await this.alCandidato(mensaje['candidate'] as RTCIceCandidateInit);
        break;
      case 'chat':
        this.mensajes.update((lista) => [
          ...lista,
          { propio: false, texto: String(mensaje['texto']), hora: new Date() },
        ]);
        break;
      case 'finalizada':
        this.alFinalizar();
        break;
      case 'reemplazada':
        this.cerrando = true;
        this.fallar('Abriste esta sala en otra ventana. Cierra esa ventana o vuelve a ingresar.');
        break;
      case 'error':
        this.aviso.set(String(mensaje['mensaje'] ?? 'Ocurrió un problema'));
        this.finalizando.set(false);
        break;
      default:
        break;
    }
  }

  private async alEstado(mensaje: MensajeEntrante): Promise<void> {
    this.medicoEnSala.set(mensaje['medicoEnSala'] === true);
    this.pacienteEnSala.set(mensaje['pacienteEnSala'] === true);
    const admitido = mensaje['admitido'] === true;
    this.admitido.set(admitido);
    const ambos = this.medicoEnSala() && this.pacienteEnSala();
    if (!ambos && this.pc) {
      this.cerrarPar();
      this.conexion.set('esperando');
      return;
    }
    if (admitido && ambos && !this.pc) {
      this.crearPar();
      if (this.esMedico()) {
        await this.ofrecer(false);
      }
    }
  }

  private crearPar(): void {
    const pc = new RTCPeerConnection({ iceServers: this.servidoresIce });
    this.pc = pc;
    this.candidatosPendientes = [];
    this.conexion.set('negociando');
    this.flujoLocal?.getTracks().forEach((pista) => pc.addTrack(pista, this.flujoLocal!));
    pc.ontrack = (evento) => {
      this.flujoRemoto = evento.streams[0] ?? new MediaStream([evento.track]);
      const remoto = this.videoRemoto()?.nativeElement;
      if (remoto) {
        remoto.srcObject = this.flujoRemoto;
      }
    };
    pc.onicecandidate = (evento) => {
      if (evento.candidate) {
        this.canal?.enviar({ t: 'ice', candidate: evento.candidate.toJSON() });
      }
    };
    pc.onconnectionstatechange = () => {
      switch (pc.connectionState) {
        case 'connected':
          this.conexion.set('conectado');
          this.iniciarMedicion();
          break;
        case 'disconnected':
        case 'failed':
          if (this.conexion() === 'conectado') {
            this.reconexionesMedidas++;
          }
          this.conexion.set('reconectando');
          if (this.esMedico() && pc.connectionState === 'failed') {
            void this.ofrecer(true);
          }
          break;
        default:
          break;
      }
    };
  }

  private async ofrecer(reiniciarIce: boolean): Promise<void> {
    if (!this.pc) {
      return;
    }
    const oferta = await this.pc.createOffer({ iceRestart: reiniciarIce });
    await this.pc.setLocalDescription(oferta);
    this.canal?.enviar({ t: 'offer', sdp: oferta.sdp });
  }

  private async alOferta(sdp: string): Promise<void> {
    if (!this.pc) {
      this.crearPar();
    }
    const pc = this.pc!;
    await pc.setRemoteDescription({ type: 'offer', sdp });
    await this.vaciarCandidatos();
    const respuesta = await pc.createAnswer();
    await pc.setLocalDescription(respuesta);
    this.canal?.enviar({ t: 'answer', sdp: respuesta.sdp });
  }

  private async alCandidato(candidato: RTCIceCandidateInit): Promise<void> {
    if (this.pc?.remoteDescription) {
      await this.pc.addIceCandidate(candidato).catch(() => undefined);
    } else {
      this.candidatosPendientes.push(candidato);
    }
  }

  private async vaciarCandidatos(): Promise<void> {
    const pendientes = this.candidatosPendientes;
    this.candidatosPendientes = [];
    for (const candidato of pendientes) {
      await this.pc?.addIceCandidate(candidato).catch(() => undefined);
    }
  }

  private iniciarMedicion(): void {
    if (this.temporizadorCalidad) {
      return;
    }
    const medir = async () => {
      if (!this.pc) {
        return;
      }
      const muestra = await medirCalidad(this.pc).catch(() => null);
      if (muestra) {
        this.calidad.set(muestra.nivel);
        this.canal?.enviar({
          t: 'calidad',
          reconexiones: this.reconexionesMedidas,
          candidato: muestra.candidato,
        });
      }
    };
    setTimeout(() => void medir(), 2000);
    this.temporizadorCalidad = setInterval(() => void medir(), 30_000);
  }

  private alFinalizar(): void {
    this.cerrando = true;
    this.conexion.set('finalizada');
    this.limpiar();
  }

  private async detenerPantalla(): Promise<void> {
    this.flujoPantalla?.getTracks().forEach((pista) => pista.stop());
    this.flujoPantalla = null;
    const camara = this.flujoLocal?.getVideoTracks()[0];
    const emisor = this.pc?.getSenders().find((s) => s.track?.kind === 'video');
    if (camara && emisor) {
      await emisor.replaceTrack(camara);
    }
    this.pantalla.set(false);
  }

  private cerrarPar(): void {
    this.pc?.close();
    this.pc = null;
    this.flujoRemoto = null;
    const remoto = this.videoRemoto()?.nativeElement;
    if (remoto) {
      remoto.srcObject = null;
    }
    if (this.temporizadorCalidad) {
      clearInterval(this.temporizadorCalidad);
      this.temporizadorCalidad = null;
    }
  }

  private fallar(mensaje: string): void {
    this.error.set(mensaje);
    this.conexion.set('error');
    this.limpiar();
  }

  private limpiar(): void {
    this.cerrando = true;
    this.cerrarPar();
    this.canal?.cerrar();
    this.canal = null;
    this.flujoLocal?.getTracks().forEach((pista) => pista.stop());
    this.flujoLocal = null;
    this.flujoPantalla?.getTracks().forEach((pista) => pista.stop());
    this.flujoPantalla = null;
  }
}
