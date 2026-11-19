import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  effect,
  inject,
  signal,
  untracked,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ConfirmationService } from 'primeng/api';
import { Button } from 'primeng/button';
import { Checkbox } from 'primeng/checkbox';
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Skeleton } from 'primeng/skeleton';
import { Tag } from 'primeng/tag';
import { ToggleSwitch } from 'primeng/toggleswitch';

import {
  ApiError,
  CANALES_EXTERNOS,
  CanalExterno,
  CanalesStore,
  ContactoCanal,
  ETIQUETA_CANAL,
  ETIQUETA_ESTADO_CONTACTO,
  ICONO_CANAL,
  NotificationService,
  TipoPreferencia,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';

type Paso = 'datos' | 'codigo';

interface GrupoPreferencias {
  categoria: string;
  tipos: TipoPreferencia[];
}

@Component({
  selector: 'app-preferencias-canales',
  imports: [
    DatePipe,
    FormsModule,
    Button,
    Checkbox,
    Dialog,
    InputText,
    Message,
    Skeleton,
    Tag,
    ToggleSwitch,
    EmptyState,
  ],
  template: `
    <section class="canales" aria-labelledby="canales-titulo">
      <header class="cabecera">
        <h2 id="canales-titulo">Avisos por correo, SMS y WhatsApp</h2>
        <p class="text-muted">
          Además de los avisos dentro de la aplicación, puedes recibir mensajes en tu correo o tu
          teléfono. Esos mensajes son siempre genéricos y llevan un enlace a la aplicación: nunca
          incluyen diagnósticos, resultados ni datos clínicos.
        </p>
      </header>

      @if (store.mis.error(); as problema) {
        <app-empty-state
          tone="error"
          icon="pi-exclamation-triangle"
          title="No pudimos cargar tus canales"
          [message]="problema.message"
        >
          <p-button label="Reintentar" icon="pi pi-refresh" (onClick)="store.cargarMis()" />
        </app-empty-state>
      } @else if (!store.mis.hasValue()) {
        <p-skeleton height="10rem" />
      } @else {
        <div class="tarjetas">
          @for (canal of store.mis.value()!.canales; track canal.canal) {
            <article class="tarjeta" [attr.data-canal]="canal.canal">
              <div class="titulo">
                <i class="pi {{ iconos[canal.canal] }}" aria-hidden="true"></i>
                <strong>{{ etiquetas[canal.canal] }}</strong>
                <p-tag
                  [value]="estados[canal.estado]"
                  [severity]="severidad(canal.estado)"
                  data-testid="estado-canal"
                />
              </div>
              <p class="destino">
                @if (canal.destinoMascara) {
                  {{ canal.destinoMascara }}
                } @else {
                  Sin número registrado
                }
              </p>
              @if (canal.verificadoEn) {
                <small class="text-muted"
                  >Verificado el {{ canal.verificadoEn | date: 'd MMM y, HH:mm' }}</small
                >
              }
              <div class="acciones">
                @switch (canal.estado) {
                  @case ('ACTIVO') {
                    <p-button
                      label="Dar de baja"
                      icon="pi pi-ban"
                      severity="danger"
                      [outlined]="true"
                      size="small"
                      [ariaLabel]="'Dar de baja ' + etiquetas[canal.canal]"
                      (onClick)="confirmarBaja(canal)"
                    />
                  }
                  @case ('PENDIENTE_VERIFICACION') {
                    <p-button
                      label="Ingresar código"
                      icon="pi pi-key"
                      size="small"
                      [ariaLabel]="'Ingresar código de ' + etiquetas[canal.canal]"
                      (onClick)="abrir(canal, 'codigo')"
                    />
                    <p-button
                      label="Reenviar"
                      severity="secondary"
                      [outlined]="true"
                      size="small"
                      (onClick)="abrir(canal, 'datos')"
                    />
                  }
                  @default {
                    <p-button
                      label="Activar"
                      icon="pi pi-plus"
                      size="small"
                      [ariaLabel]="'Activar ' + etiquetas[canal.canal]"
                      (onClick)="abrir(canal, 'datos')"
                    />
                  }
                }
              </div>
            </article>
          }
        </div>

        <h3 class="subtitulo">Qué avisos quieres recibir</h3>
        <div class="tabla-contenedor">
          <table class="tabla" aria-describedby="canales-titulo">
            <thead>
              <tr>
                <th scope="col">Aviso</th>
                @for (canal of canales; track canal) {
                  <th scope="col" class="centro">{{ etiquetas[canal] }}</th>
                }
              </tr>
            </thead>
            @for (grupo of grupos(); track grupo.categoria) {
              <tbody>
                <tr class="grupo">
                  <th scope="rowgroup" [attr.colspan]="canales.length + 1">
                    {{ grupo.categoria }}
                  </th>
                </tr>
                @for (tipo of grupo.tipos; track tipo.tipo) {
                  <tr>
                    <th scope="row" class="aviso">
                      {{ tipo.etiqueta }}
                      @if (tipo.obligatoria) {
                        <i
                          class="pi pi-lock candado"
                          aria-hidden="true"
                          title="Las alertas de seguridad siempre se envían a los canales activos"
                        ></i>
                        <span class="sr-only">Obligatoria</span>
                      }
                    </th>
                    @for (canal of canales; track canal) {
                      <td class="centro">
                        <p-toggleswitch
                          [ngModel]="valor(tipo, canal)"
                          (ngModelChange)="cambiar(tipo, canal, $event)"
                          [disabled]="tipo.obligatoria || !activo(canal)"
                          [ariaLabel]="tipo.etiqueta + ' por ' + etiquetas[canal]"
                        />
                      </td>
                    }
                  </tr>
                }
              </tbody>
            }
          </table>
        </div>
        <p class="nota text-muted">
          Los canales sin activar no reciben nada. Las alertas de seguridad no se pueden apagar y
          llegan a todo canal activo.
        </p>

        <h3 class="subtitulo">Horario de silencio</h3>
        <div class="silencio">
          <div class="check">
            <p-toggleswitch
              inputId="silencio-activo"
              [ngModel]="silencioActivo()"
              (ngModelChange)="silencioActivo.set($event); sucio.set(true)"
              ariaLabel="Activar el horario de silencio"
            />
            <label for="silencio-activo">No enviar avisos no urgentes entre estas horas</label>
          </div>
          <div class="horas">
            <label for="silencio-desde">Desde</label>
            <input
              pInputText
              id="silencio-desde"
              type="time"
              [ngModel]="silencioDesde()"
              (ngModelChange)="silencioDesde.set($event); sucio.set(true)"
              [disabled]="!silencioActivo()"
            />
            <label for="silencio-hasta">Hasta</label>
            <input
              pInputText
              id="silencio-hasta"
              type="time"
              [ngModel]="silencioHasta()"
              (ngModelChange)="silencioHasta.set($event); sucio.set(true)"
              [disabled]="!silencioActivo()"
            />
          </div>
        </div>
        @if (errorGuardado(); as texto) {
          <p-message severity="error" [text]="texto" />
        }
        <div class="pie">
          <p-button
            label="Guardar avisos por canal"
            icon="pi pi-check"
            [disabled]="!sucio()"
            [loading]="guardando()"
            (onClick)="guardar()"
          />
        </div>
      }
    </section>

    <p-dialog
      [visible]="dialogoAbierto()"
      (visibleChange)="dialogoAbierto.set($event)"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '30rem' }"
      [header]="'Activar ' + (canalActual() ? etiquetas[canalActual()!.canal] : '')"
    >
      @if (canalActual(); as canal) {
        <div class="dialogo">
          @if (paso() === 'datos') {
            @if (canal.destinoFijo) {
              <p>
                Enviaremos un código de verificación a tu correo
                <strong>{{ canal.destinoMascara }}</strong
                >.
              </p>
            } @else {
              <label for="canal-destino">Número de teléfono</label>
              <input
                pInputText
                id="canal-destino"
                class="w-full"
                inputmode="tel"
                autocomplete="tel"
                placeholder="+51 987 654 321"
                maxlength="20"
                [ngModel]="destino()"
                (ngModelChange)="destino.set($event)"
              />
            }
            <div class="check">
              <p-checkbox
                inputId="canal-consentimiento"
                [binary]="true"
                [ngModel]="consentimiento()"
                (ngModelChange)="consentimiento.set($event)"
              />
              <label for="canal-consentimiento">
                Acepto recibir avisos de TuSalud por {{ etiquetas[canal.canal] }}. Entiendo que son
                mensajes genéricos sin datos clínicos y que puedo darme de baja cuando quiera.
              </label>
            </div>
          } @else {
            <p>
              Te enviamos un código de 6 dígitos a <strong>{{ canal.destinoMascara }}</strong
              >. Vence en 10 minutos.
            </p>
            <label for="canal-codigo">Código de verificación</label>
            <input
              pInputText
              id="canal-codigo"
              class="w-full codigo"
              inputmode="numeric"
              autocomplete="one-time-code"
              maxlength="6"
              [ngModel]="codigo()"
              (ngModelChange)="codigo.set($event)"
            />
          }
          @if (errorDialogo(); as texto) {
            <p-message severity="error" [text]="texto" />
          }
        </div>
      }
      <ng-template #footer>
        <p-button
          label="Cancelar"
          severity="secondary"
          [outlined]="true"
          (onClick)="dialogoAbierto.set(false)"
        />
        @if (paso() === 'datos') {
          <p-button
            label="Enviar código"
            icon="pi pi-send"
            [loading]="enviando()"
            (onClick)="enviarCodigo()"
          />
        } @else {
          <p-button
            label="Verificar"
            icon="pi pi-check"
            [loading]="enviando()"
            (onClick)="verificar()"
          />
        }
      </ng-template>
    </p-dialog>
  `,
  styles: `
    .canales {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .cabecera h2 {
      margin: 0 0 0.35rem;
      font-size: 1.15rem;
    }

    .cabecera p {
      margin: 0;
    }

    .tarjetas {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(15rem, 1fr));
      gap: 0.75rem;
    }

    .tarjeta {
      display: flex;
      flex-direction: column;
      gap: 0.35rem;
      padding: 0.9rem 1rem;
      border: 1px solid var(--p-content-border-color);
      border-radius: 10px;
    }

    .titulo {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 0.5rem;
    }

    .destino {
      margin: 0;
      font-variant-numeric: tabular-nums;
    }

    .acciones {
      display: flex;
      flex-wrap: wrap;
      gap: 0.5rem;
      margin-top: 0.4rem;
    }

    .subtitulo {
      margin: 0.5rem 0 0;
      font-size: 1rem;
    }

    .tabla-contenedor {
      overflow-x: auto;
    }

    .tabla {
      width: 100%;
      border-collapse: collapse;
    }

    .tabla th,
    .tabla td {
      padding: 0.5rem 0.6rem;
      border-bottom: 1px solid var(--p-content-border-color);
      text-align: left;
      font-weight: normal;
    }

    .tabla thead th {
      font-weight: 600;
    }

    .tabla .centro {
      text-align: center;
    }

    .tabla .grupo th {
      padding-top: 0.9rem;
      background: var(--p-content-hover-background);
      font-size: 0.8rem;
      font-weight: 600;
      letter-spacing: 0.04em;
      text-transform: uppercase;
    }

    .candado {
      margin-left: 0.35rem;
      color: var(--p-text-muted-color);
      font-size: 0.8rem;
    }

    .nota {
      margin: 0;
      font-size: 0.85rem;
    }

    .silencio {
      display: flex;
      flex-direction: column;
      gap: 0.6rem;
    }

    .check {
      display: flex;
      align-items: flex-start;
      gap: 0.6rem;
    }

    .horas {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 0.6rem;
    }

    .pie {
      display: flex;
      justify-content: flex-end;
    }

    .dialogo {
      display: flex;
      flex-direction: column;
      gap: 0.6rem;
    }

    .dialogo label {
      font-weight: 600;
      font-size: 0.9rem;
    }

    .dialogo .check label {
      font-weight: normal;
    }

    .codigo {
      font-size: 1.4rem;
      letter-spacing: 0.4em;
      text-align: center;
    }

    .sr-only {
      position: absolute;
      width: 1px;
      height: 1px;
      overflow: hidden;
      clip: rect(0, 0, 0, 0);
      white-space: nowrap;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PreferenciasCanales implements OnInit {
  protected readonly store = inject(CanalesStore);
  private readonly notificaciones = inject(NotificationService);
  private readonly confirmacion = inject(ConfirmationService);

  protected readonly canales = CANALES_EXTERNOS;
  protected readonly etiquetas = ETIQUETA_CANAL;
  protected readonly iconos = ICONO_CANAL;
  protected readonly estados = ETIQUETA_ESTADO_CONTACTO;

  protected readonly borrador = signal<Record<string, boolean>>({});
  protected readonly silencioActivo = signal(true);
  protected readonly silencioDesde = signal('22:00');
  protected readonly silencioHasta = signal('07:00');
  protected readonly sucio = signal(false);
  protected readonly guardando = signal(false);
  protected readonly errorGuardado = signal<string | null>(null);

  protected readonly dialogoAbierto = signal(false);
  protected readonly canalActual = signal<ContactoCanal | null>(null);
  protected readonly paso = signal<Paso>('datos');
  protected readonly destino = signal('');
  protected readonly consentimiento = signal(false);
  protected readonly codigo = signal('');
  protected readonly enviando = signal(false);
  protected readonly errorDialogo = signal<string | null>(null);

  protected readonly grupos = computed<GrupoPreferencias[]>(() => {
    const preferencias = this.store.mis.value()?.preferencias ?? [];
    const mapa = new Map<string, TipoPreferencia[]>();
    for (const tipo of preferencias) {
      mapa.set(tipo.categoria, [...(mapa.get(tipo.categoria) ?? []), tipo]);
    }
    return [...mapa.entries()].map(([categoria, tipos]) => ({ categoria, tipos }));
  });

  constructor() {
    effect(() => {
      const mis = this.store.mis.value();
      if (!mis) {
        return;
      }
      untracked(() => {
        const valores: Record<string, boolean> = {};
        for (const tipo of mis.preferencias) {
          for (const canal of CANALES_EXTERNOS) {
            valores[this.clave(tipo.tipo, canal)] = tipo.activos[canal];
          }
        }
        this.borrador.set(valores);
        this.silencioActivo.set(mis.silencio.activo);
        this.silencioDesde.set(mis.silencio.desde.slice(0, 5));
        this.silencioHasta.set(mis.silencio.hasta.slice(0, 5));
        this.sucio.set(false);
      });
    });
  }

  ngOnInit(): void {
    this.store.cargarMis();
  }

  protected severidad(estado: string): 'success' | 'warn' | 'secondary' | 'danger' {
    if (estado === 'ACTIVO') {
      return 'success';
    }
    if (estado === 'PENDIENTE_VERIFICACION') {
      return 'warn';
    }
    return estado === 'BAJA' ? 'danger' : 'secondary';
  }

  protected activo(canal: CanalExterno): boolean {
    return this.store.mis.value()?.canales.find((c) => c.canal === canal)?.estado === 'ACTIVO';
  }

  protected valor(tipo: TipoPreferencia, canal: CanalExterno): boolean {
    return this.borrador()[this.clave(tipo.tipo, canal)] ?? false;
  }

  protected cambiar(tipo: TipoPreferencia, canal: CanalExterno, activo: boolean): void {
    this.borrador.update((actual) => ({ ...actual, [this.clave(tipo.tipo, canal)]: activo }));
    this.sucio.set(true);
  }

  protected guardar(): void {
    const mis = this.store.mis.value();
    if (!mis) {
      return;
    }
    const preferencias = mis.preferencias
      .filter((tipo) => !tipo.obligatoria)
      .flatMap((tipo) =>
        CANALES_EXTERNOS.map((canal) => ({
          tipo: tipo.tipo,
          canal,
          activo: this.valor(tipo, canal),
        })),
      );
    this.guardando.set(true);
    this.errorGuardado.set(null);
    this.store
      .guardarPreferencias({
        preferencias,
        silencio: {
          activo: this.silencioActivo(),
          desde: this.silencioDesde(),
          hasta: this.silencioHasta(),
        },
      })
      .subscribe({
        next: () => {
          this.guardando.set(false);
          this.notificaciones.success('Guardamos tus preferencias de avisos.');
        },
        error: (error: ApiError) => {
          this.guardando.set(false);
          this.errorGuardado.set(error.message);
        },
      });
  }

  protected abrir(canal: ContactoCanal, paso: Paso): void {
    this.canalActual.set(canal);
    this.paso.set(paso);
    this.destino.set('');
    this.codigo.set('');
    this.consentimiento.set(false);
    this.errorDialogo.set(null);
    this.dialogoAbierto.set(true);
  }

  protected enviarCodigo(): void {
    const canal = this.canalActual();
    if (!canal) {
      return;
    }
    if (!this.consentimiento()) {
      this.errorDialogo.set('Para continuar debes aceptar recibir avisos por este canal.');
      return;
    }
    if (!canal.destinoFijo && !this.destino().trim()) {
      this.errorDialogo.set('Indica el número de teléfono.');
      return;
    }
    this.enviando.set(true);
    this.errorDialogo.set(null);
    this.store
      .solicitar(canal.canal, {
        destino: canal.destinoFijo ? null : this.destino().trim(),
        consentimiento: true,
      })
      .subscribe({
        next: (actualizado) => {
          this.enviando.set(false);
          this.canalActual.set(actualizado);
          this.paso.set('codigo');
        },
        error: (error: ApiError) => {
          this.enviando.set(false);
          this.errorDialogo.set(error.message);
        },
      });
  }

  protected verificar(): void {
    const canal = this.canalActual();
    if (!canal) {
      return;
    }
    if (!/^\d{6}$/.test(this.codigo().trim())) {
      this.errorDialogo.set('El código tiene 6 dígitos.');
      return;
    }
    this.enviando.set(true);
    this.errorDialogo.set(null);
    this.store.verificar(canal.canal, this.codigo().trim()).subscribe({
      next: () => {
        this.enviando.set(false);
        this.dialogoAbierto.set(false);
        this.notificaciones.success(`${ETIQUETA_CANAL[canal.canal]} quedó activo.`);
      },
      error: (error: ApiError) => {
        this.enviando.set(false);
        this.errorDialogo.set(error.message);
      },
    });
  }

  protected confirmarBaja(canal: ContactoCanal): void {
    this.confirmacion.confirm({
      header: 'Dar de baja el canal',
      message: `Dejarás de recibir avisos por ${ETIQUETA_CANAL[canal.canal]}. Puedes activarlo de nuevo cuando quieras.`,
      icon: 'pi pi-ban',
      acceptLabel: 'Dar de baja',
      rejectLabel: 'Cancelar',
      acceptButtonProps: { severity: 'danger' },
      rejectButtonProps: { severity: 'secondary', outlined: true },
      accept: () =>
        this.store.revocar(canal.canal).subscribe({
          next: () => this.notificaciones.success(`Diste de baja ${ETIQUETA_CANAL[canal.canal]}.`),
          error: () => undefined,
        }),
    });
  }

  private clave(tipo: string, canal: CanalExterno): string {
    return `${tipo}:${canal}`;
  }
}
