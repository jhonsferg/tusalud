import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';

import {
  NotificationService,
  PlanillaApiService,
  TerminalAsistenciaResponse,
  TerminalCreadaResponse,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { ESTILOS_PLANILLA } from '../planilla.estilos';

@Component({
  selector: 'app-terminales-asistencia',
  imports: [
    DatePipe,
    FormsModule,
    Button,
    Dialog,
    InputText,
    TableModule,
    Tag,
    EmptyState,
    PageHeader,
  ],
  template: `
    <app-page-header
      title="Terminales de asistencia"
      subtitle="Relojes o tabletas que registran entradas y salidas con su propio token."
      icon="pi-tablet"
    >
      <p-button label="Nueva terminal" icon="pi pi-plus" (onClick)="abrir()" />
    </app-page-header>

    <section class="panel">
      <p-table [value]="lista()" [loading]="cargando()" [rowHover]="true" responsiveLayout="scroll">
        <ng-template #header>
          <tr>
            <th>Terminal</th>
            <th>Estado</th>
            <th>Último uso</th>
            <th style="width: 11rem"></th>
          </tr>
        </ng-template>
        <ng-template #body let-t>
          <tr>
            <td class="font-semibold">{{ t.nombre }}</td>
            <td>
              <p-tag
                [value]="t.activo ? 'Activa' : 'Desactivada'"
                [severity]="t.activo ? 'success' : 'secondary'"
              />
            </td>
            <td>{{ t.ultimoUso ? (t.ultimoUso | date: 'd MMM y HH:mm') : 'Nunca' }}</td>
            <td class="derecha">
              <p-button
                [label]="t.activo ? 'Desactivar' : 'Activar'"
                size="small"
                [outlined]="true"
                [severity]="t.activo ? 'danger' : 'secondary'"
                [ariaLabel]="(t.activo ? 'Desactivar ' : 'Activar ') + t.nombre"
                (onClick)="cambiar(t)"
              />
            </td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr>
            <td colspan="4">
              <app-empty-state
                icon="pi-tablet"
                title="Sin terminales"
                message="Registra una terminal para que el personal marque su entrada y salida."
              />
            </td>
          </tr>
        </ng-template>
      </p-table>
    </section>

    <p-dialog
      header="Nueva terminal"
      [visible]="dialogo()"
      (visibleChange)="dialogo.set($event)"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '30rem', maxWidth: '96vw' }"
    >
      @if (creada(); as c) {
        <p class="aviso">
          Copia el token ahora. Solo se muestra una vez y no se puede recuperar después.
        </p>
        <pre class="vista-previa">{{ c.token }}</pre>
      } @else {
        <label class="formulario" for="tm-nombre"
          >Nombre de la terminal
          <input
            pInputText
            id="tm-nombre"
            maxlength="80"
            [ngModel]="nombre()"
            (ngModelChange)="nombre.set($event)"
          />
        </label>
      }
      <ng-template #footer>
        <p-button
          label="Cerrar"
          severity="secondary"
          [outlined]="true"
          (onClick)="dialogo.set(false)"
        />
        @if (!creada()) {
          <p-button
            label="Crear"
            icon="pi pi-check"
            [loading]="ocupado()"
            [disabled]="!nombre().trim()"
            (onClick)="crear()"
          />
        }
      </ng-template>
    </p-dialog>
  `,
  styles: ESTILOS_PLANILLA,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TerminalesAsistencia implements OnInit {
  private readonly api = inject(PlanillaApiService);
  private readonly notificaciones = inject(NotificationService);

  protected readonly lista = signal<TerminalAsistenciaResponse[]>([]);
  protected readonly cargando = signal(false);
  protected readonly ocupado = signal(false);
  protected readonly dialogo = signal(false);
  protected readonly nombre = signal('');
  protected readonly creada = signal<TerminalCreadaResponse | null>(null);

  ngOnInit(): void {
    this.cargar();
  }

  private cargar(): void {
    this.cargando.set(true);
    this.api.terminales().subscribe({
      next: (l) => {
        this.lista.set(l);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
  }

  protected abrir(): void {
    this.nombre.set('');
    this.creada.set(null);
    this.dialogo.set(true);
  }

  protected crear(): void {
    this.ocupado.set(true);
    this.api.crearTerminal(this.nombre().trim()).subscribe({
      next: (c) => {
        this.ocupado.set(false);
        this.creada.set(c);
        this.cargar();
      },
      error: () => this.ocupado.set(false),
    });
  }

  protected cambiar(t: TerminalAsistenciaResponse): void {
    this.api.estadoTerminal(t.id, !t.activo).subscribe(() => {
      this.notificaciones.success(
        t.activo ? 'La terminal quedó desactivada.' : 'La terminal quedó activa.',
      );
      this.cargar();
    });
  }
}
