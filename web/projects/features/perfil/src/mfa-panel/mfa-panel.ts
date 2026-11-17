import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Password } from 'primeng/password';
import { Skeleton } from 'primeng/skeleton';
import { Tag } from 'primeng/tag';
import QRCode from 'qrcode';

import {
  AuthApiService,
  MfaConfiguracionResponse,
  MfaEstadoResponse,
  NotificationService,
} from '@tusalud/core';

type Modo = 'activar' | 'desactivar' | 'regenerar' | null;

@Component({
  selector: 'app-mfa-panel',
  imports: [ReactiveFormsModule, Button, Dialog, InputText, Message, Password, Skeleton, Tag],
  templateUrl: './mfa-panel.html',
  styleUrl: './mfa-panel.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MfaPanel implements OnInit {
  private readonly api = inject(AuthApiService);
  private readonly notifications = inject(NotificationService);
  private readonly fb = inject(FormBuilder);

  protected readonly estado = signal<MfaEstadoResponse | null>(null);
  protected readonly cargando = signal(true);
  protected readonly modo = signal<Modo>(null);
  protected readonly paso = signal<1 | 2 | 3>(1);
  protected readonly trabajando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly configuracion = signal<MfaConfiguracionResponse | null>(null);
  protected readonly qr = signal<string | null>(null);
  protected readonly codigosRecuperacion = signal<string[]>([]);

  protected readonly codigo = this.fb.nonNullable.control('', [
    Validators.required,
    Validators.pattern(/^\d{6}$/),
  ]);
  protected readonly password = this.fb.nonNullable.control('', [Validators.required]);

  ngOnInit(): void {
    this.cargar();
  }

  protected abrirActivacion(): void {
    this.reiniciar('activar');
    this.trabajando.set(true);
    this.api.configurarMfa().subscribe({
      next: async (configuracion) => {
        this.configuracion.set(configuracion);
        this.qr.set(await QRCode.toDataURL(configuracion.otpauthUri, { width: 220, margin: 1 }));
        this.trabajando.set(false);
      },
      error: () => {
        this.trabajando.set(false);
        this.modo.set(null);
      },
    });
  }

  protected abrirDesactivacion(): void {
    this.reiniciar('desactivar');
  }

  protected abrirRegeneracion(): void {
    this.reiniciar('regenerar');
  }

  protected cerrar(): void {
    this.modo.set(null);
    this.configuracion.set(null);
    this.qr.set(null);
    this.codigosRecuperacion.set([]);
    if (this.paso() === 3) {
      this.cargar();
    }
  }

  protected continuar(): void {
    this.paso.set(2);
  }

  protected confirmar(): void {
    this.error.set(null);
    if (this.codigo.invalid || (this.modo() === 'desactivar' && this.password.invalid)) {
      this.codigo.markAsTouched();
      this.password.markAsTouched();
      return;
    }
    const codigo = this.codigo.value;
    this.trabajando.set(true);
    const modo = this.modo();
    if (modo === 'activar') {
      this.api.activarMfa(codigo).subscribe({
        next: (respuesta) => this.mostrarCodigos(respuesta.codigosRecuperacion),
        error: () => this.fallo(),
      });
    } else if (modo === 'regenerar') {
      this.api.regenerarCodigosMfa(codigo).subscribe({
        next: (respuesta) => this.mostrarCodigos(respuesta.codigosRecuperacion),
        error: () => this.fallo(),
      });
    } else if (modo === 'desactivar') {
      this.api.desactivarMfa({ password: this.password.value, codigo }).subscribe({
        next: () => {
          this.trabajando.set(false);
          this.cerrar();
          this.cargar();
          this.notifications.success('La verificación en dos pasos fue desactivada.');
        },
        error: () => this.fallo(),
      });
    }
  }

  protected async copiarCodigos(): Promise<void> {
    await navigator.clipboard?.writeText(this.codigosRecuperacion().join('\n'));
    this.notifications.success('Códigos copiados al portapapeles.');
  }

  protected descargarCodigos(): void {
    const blob = new Blob(
      [`Códigos de recuperación de TuSalud\n\n${this.codigosRecuperacion().join('\n')}\n`],
      { type: 'text/plain' },
    );
    const url = URL.createObjectURL(blob);
    const enlace = document.createElement('a');
    enlace.href = url;
    enlace.download = 'codigos-recuperacion-tusalud.txt';
    enlace.click();
    URL.revokeObjectURL(url);
  }

  private cargar(): void {
    this.cargando.set(true);
    this.api.estadoMfa().subscribe({
      next: (estado) => {
        this.estado.set(estado);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
  }

  private reiniciar(modo: Modo): void {
    this.modo.set(modo);
    this.paso.set(modo === 'activar' ? 1 : 2);
    this.error.set(null);
    this.codigo.reset('');
    this.password.reset('');
    this.codigosRecuperacion.set([]);
  }

  private mostrarCodigos(codigos: string[]): void {
    this.trabajando.set(false);
    this.codigosRecuperacion.set(codigos);
    this.paso.set(3);
    this.notifications.success('La verificación en dos pasos está activa.');
  }

  private fallo(): void {
    this.trabajando.set(false);
    this.error.set(
      'El código no es válido. Revisa la hora de tu dispositivo e inténtalo de nuevo.',
    );
  }
}
