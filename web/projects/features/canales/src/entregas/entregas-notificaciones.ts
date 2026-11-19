import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';

import {
  AuthService,
  CANALES_EXTERNOS,
  CanalExterno,
  CanalesApiService,
  CanalesStore,
  ESTADOS_ENTREGA,
  ETIQUETA_CANAL,
  ETIQUETA_ESTADO_ENTREGA,
  EntregaNotificacion,
  EstadoEntrega,
  ICONO_CANAL,
  MensajeSimulado,
  NotificationService,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';

@Component({
  selector: 'app-entregas-notificaciones',
  imports: [
    DatePipe,
    FormsModule,
    Button,
    InputText,
    Select,
    TableModule,
    Tag,
    EmptyState,
    PageHeader,
  ],
  template: `
    <app-page-header
      title="Entregas de notificaciones"
      subtitle="Estado de los avisos enviados por correo, SMS y WhatsApp. No se guarda el texto de los mensajes."
      icon="pi-send"
    >
      <p-button label="Actualizar" icon="pi pi-refresh" [outlined]="true" (onClick)="recargar()" />
    </app-page-header>

    <section class="resumen" aria-label="Resumen de los últimos 7 días">
      @for (canal of store.resumen.value()?.canales ?? []; track canal.canal) {
        <article class="kpi" [attr.data-canal]="canal.canal">
          <header>
            <i class="pi {{ iconos[canal.canal] }}" aria-hidden="true"></i>
            <strong>{{ etiquetas[canal.canal] }}</strong>
          </header>
          <p class="total">{{ canal.total }}</p>
          <small class="text-muted">
            {{ canal.porEstado.ENTREGADO ?? 0 }} entregados ·
            {{ canal.porEstado.ENVIADO ?? 0 }} enviados ·
            {{ canal.porEstado.PENDIENTE ?? 0 }} pendientes ·
            {{ (canal.porEstado.FALLIDO ?? 0) + (canal.porEstado.REBOTADO ?? 0) }} con problemas
          </small>
        </article>
      }
    </section>

    @if (admin() && store.proveedores.value(); as p) {
      <p class="proveedores text-muted">
        Proveedores:
        @for (canal of canales; track canal) {
          <p-tag [value]="etiquetas[canal] + ': ' + p.proveedores[canal]" severity="secondary" />
        }
        · Máximo {{ p.limitePorHora }} avisos por hora y destinatario ·
        {{ p.workerHabilitado ? 'Cola activa' : 'Cola detenida' }}
      </p>
    }

    <section class="panel">
      <div class="filtros">
        <div class="campo">
          <label for="en-canal">Canal</label>
          <p-select
            inputId="en-canal"
            [options]="opcionesCanal"
            optionLabel="label"
            optionValue="value"
            [ngModel]="canal()"
            (ngModelChange)="canal.set($event); filtrar()"
            [showClear]="true"
            placeholder="Todos"
          />
        </div>
        <div class="campo">
          <label for="en-estado">Estado</label>
          <p-select
            inputId="en-estado"
            [options]="opcionesEstado"
            optionLabel="label"
            optionValue="value"
            [ngModel]="estado()"
            (ngModelChange)="estado.set($event); filtrar()"
            [showClear]="true"
            placeholder="Todos"
          />
        </div>
        <div class="campo">
          <label for="en-desde">Desde</label>
          <input
            pInputText
            id="en-desde"
            type="date"
            [ngModel]="desde()"
            (ngModelChange)="desde.set($event); filtrar()"
          />
        </div>
        <div class="campo">
          <label for="en-hasta">Hasta</label>
          <input
            pInputText
            id="en-hasta"
            type="date"
            [ngModel]="hasta()"
            (ngModelChange)="hasta.set($event); filtrar()"
          />
        </div>
      </div>

      @if (store.entregas.error(); as problema) {
        <app-empty-state
          tone="error"
          icon="pi-exclamation-triangle"
          title="No pudimos cargar las entregas"
          [message]="problema.message"
        >
          <p-button label="Reintentar" icon="pi pi-refresh" (onClick)="cargar()" />
        </app-empty-state>
      } @else {
        <p-table
          [value]="store.entregas.value()?.contenido ?? []"
          [loading]="store.entregas.loading() && !store.entregas.hasValue()"
          [rowHover]="true"
          responsiveLayout="scroll"
        >
          <ng-template #header>
            <tr>
              <th>Fecha</th>
              <th>Aviso</th>
              <th>Canal</th>
              <th>Destino</th>
              <th>Estado</th>
              <th>Intentos</th>
              <th>Motivo</th>
              @if (admin()) {
                <th></th>
              }
            </tr>
          </ng-template>
          <ng-template #body let-e>
            <tr>
              <td>{{ e.creadoEn | date: 'd MMM y, HH:mm' }}</td>
              <td>
                {{ e.tipo }}
                @if (e.urgente) {
                  <p-tag value="Urgente" severity="warn" />
                }
              </td>
              <td>{{ etiquetas[e.canal] }}</td>
              <td>{{ e.destino }}</td>
              <td>
                <p-tag [value]="estados[e.estado]" [severity]="severidad(e.estado)" />
                @if (e.estado === 'PENDIENTE' && e.proximoIntento) {
                  <small class="text-muted block"
                    >Próximo intento {{ e.proximoIntento | date: 'd MMM, HH:mm' }}</small
                  >
                }
              </td>
              <td>{{ e.intentos }}</td>
              <td>{{ e.motivo || '—' }}</td>
              @if (admin()) {
                <td class="derecha">
                  @if (e.estado === 'FALLIDO') {
                    <p-button
                      label="Reintentar"
                      size="small"
                      [outlined]="true"
                      [ariaLabel]="'Reintentar el aviso ' + e.tipo + ' a ' + e.destino"
                      (onClick)="reintentar(e)"
                    />
                  }
                </td>
              }
            </tr>
          </ng-template>
          <ng-template #emptymessage>
            <tr>
              <td [attr.colspan]="admin() ? 8 : 7">
                <app-empty-state
                  icon="pi-send"
                  title="Sin entregas"
                  message="Cuando se envíen avisos por canales externos los verás aquí."
                />
              </td>
            </tr>
          </ng-template>
        </p-table>

        @if (store.entregas.value(); as pagina) {
          <nav class="paginacion" aria-label="Paginación de entregas">
            <span class="text-muted"
              >{{ pagina.total }} registros · página {{ pagina.pagina + 1 }} de
              {{ pagina.totalPaginas || 1 }}</span
            >
            <p-button
              icon="pi pi-chevron-left"
              [outlined]="true"
              size="small"
              ariaLabel="Página anterior"
              [disabled]="pagina.pagina === 0"
              (onClick)="irA(pagina.pagina - 1)"
            />
            <p-button
              icon="pi pi-chevron-right"
              [outlined]="true"
              size="small"
              ariaLabel="Página siguiente"
              [disabled]="pagina.pagina + 1 >= pagina.totalPaginas"
              (onClick)="irA(pagina.pagina + 1)"
            />
          </nav>
        }
      }
    </section>

    @if (admin() && simulados().length > 0) {
      <section class="panel simulacion">
        <h2>Bandeja del proveedor simulado</h2>
        <p class="text-muted">
          Mensajes que el sistema habría enviado por canales sin proveedor real. Solo para
          desarrollo y pruebas.
        </p>
        <ul>
          @for (m of simulados(); track m.id) {
            <li>
              <strong>{{ etiquetas[m.canal] }}</strong> a {{ m.destino }}
              <span class="text-muted">{{ m.fecha | date: 'HH:mm:ss' }}</span>
              <p>{{ m.texto }}</p>
            </li>
          }
        </ul>
      </section>
    }
  `,
  styles: `
    .resumen {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(14rem, 1fr));
      gap: 0.75rem;
      margin-bottom: 1rem;
    }

    .kpi {
      padding: 0.9rem 1rem;
      border: 1px solid var(--p-content-border-color);
      border-radius: 10px;
      background: var(--p-content-background);
    }

    .kpi header {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .kpi .total {
      margin: 0.2rem 0;
      font-size: 1.8rem;
      font-weight: 700;
    }

    .proveedores {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 0.4rem;
      margin: 0 0 1rem;
      font-size: 0.9rem;
    }

    .filtros {
      display: flex;
      flex-wrap: wrap;
      gap: 0.75rem;
      margin-bottom: 1rem;
    }

    .campo {
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
    }

    .campo label {
      font-weight: 600;
      font-size: 0.85rem;
    }

    .derecha {
      text-align: right;
    }

    .paginacion {
      display: flex;
      align-items: center;
      justify-content: flex-end;
      gap: 0.5rem;
      margin-top: 0.75rem;
    }

    .simulacion h2 {
      margin: 0 0 0.3rem;
      font-size: 1.05rem;
    }

    .simulacion ul {
      display: grid;
      gap: 0.6rem;
      margin: 0;
      padding: 0;
      list-style: none;
    }

    .simulacion li p {
      margin: 0.2rem 0 0;
      font-size: 0.9rem;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EntregasNotificaciones implements OnInit {
  protected readonly store = inject(CanalesStore);
  private readonly api = inject(CanalesApiService);
  private readonly notificaciones = inject(NotificationService);
  protected readonly admin = inject(AuthService).isAdmin;

  protected readonly canales = CANALES_EXTERNOS;
  protected readonly etiquetas: Record<string, string> = ETIQUETA_CANAL;
  protected readonly iconos: Record<string, string> = ICONO_CANAL;
  protected readonly estados: Record<string, string> = ETIQUETA_ESTADO_ENTREGA;
  protected readonly opcionesCanal = CANALES_EXTERNOS.map((value) => ({
    value,
    label: ETIQUETA_CANAL[value],
  }));
  protected readonly opcionesEstado = ESTADOS_ENTREGA.map((value) => ({
    value,
    label: ETIQUETA_ESTADO_ENTREGA[value],
  }));

  protected readonly canal = signal<CanalExterno | null>(null);
  protected readonly estado = signal<EstadoEntrega | null>(null);
  protected readonly desde = signal('');
  protected readonly hasta = signal('');
  protected readonly pagina = signal(0);
  protected readonly simulados = signal<MensajeSimulado[]>([]);

  ngOnInit(): void {
    this.recargar();
  }

  protected recargar(): void {
    this.cargar();
    this.store.cargarResumen(7);
    if (this.admin()) {
      this.store.cargarProveedores();
      this.api.mensajesSimulados(10).subscribe({
        next: (mensajes) => this.simulados.set(mensajes),
        error: () => undefined,
      });
    }
  }

  protected cargar(): void {
    this.store.cargarEntregas({
      canal: this.canal(),
      estado: this.estado(),
      desde: this.desde() || null,
      hasta: this.hasta() || null,
      pagina: this.pagina(),
      tamano: 15,
    });
  }

  protected filtrar(): void {
    this.pagina.set(0);
    this.cargar();
  }

  protected irA(pagina: number): void {
    this.pagina.set(Math.max(pagina, 0));
    this.cargar();
  }

  protected severidad(estado: EstadoEntrega): 'success' | 'info' | 'warn' | 'danger' | 'secondary' {
    switch (estado) {
      case 'ENTREGADO':
        return 'success';
      case 'ENVIADO':
        return 'info';
      case 'PENDIENTE':
        return 'warn';
      case 'FALLIDO':
      case 'REBOTADO':
        return 'danger';
      default:
        return 'secondary';
    }
  }

  protected reintentar(entrega: EntregaNotificacion): void {
    this.store.reintentar(entrega.id).subscribe({
      next: () => {
        this.notificaciones.success('El aviso volvió a la cola de envío.');
        this.cargar();
      },
      error: () => undefined,
    });
  }
}
