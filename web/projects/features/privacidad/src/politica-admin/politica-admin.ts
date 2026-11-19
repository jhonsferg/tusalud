import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { Textarea } from 'primeng/textarea';

import {
  NotificationService,
  PoliticaPrivacidad,
  ProteccionDatosApiService,
  toIsoDate,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';

@Component({
  selector: 'app-politica-admin',
  imports: [
    DatePipe,
    FormsModule,
    Button,
    DatePicker,
    Dialog,
    InputText,
    Message,
    TableModule,
    Tag,
    Textarea,
    EmptyState,
    PageHeader,
  ],
  template: `
    <app-page-header
      title="Política de privacidad"
      subtitle="Versiones del texto y aceptaciones registradas por los usuarios"
      icon="pi-book"
    >
      <p-button label="Nueva versión" icon="pi pi-plus" (onClick)="abrirNueva()" />
    </app-page-header>

    <section class="panel">
      <p-table [value]="politicas()" responsiveLayout="scroll">
        <ng-template #header>
          <tr>
            <th>Versión</th>
            <th>Vigente desde</th>
            <th>Estado</th>
            <th class="text-right">Aceptaciones</th>
            <th></th>
          </tr>
        </ng-template>
        <ng-template #body let-p>
          <tr>
            <td>
              <strong>v{{ p.version }}</strong>
              <small class="block text-muted">{{ p.titulo }}</small>
            </td>
            <td>{{ p.vigenteDesde | date: 'd MMM y' }}</td>
            <td>
              <p-tag
                [value]="p.publicada ? 'Publicada' : 'Borrador'"
                [severity]="p.publicada ? 'success' : 'warn'"
              />
            </td>
            <td class="text-right">{{ p.aceptaciones }}</td>
            <td class="derecha">
              <p-button
                label="Ver"
                size="small"
                [outlined]="true"
                [ariaLabel]="'Ver la versión ' + p.version"
                (onClick)="ver(p)"
              />
              @if (!p.publicada) {
                <p-button
                  label="Editar"
                  size="small"
                  [outlined]="true"
                  [ariaLabel]="'Editar la versión ' + p.version"
                  (onClick)="abrirEdicion(p)"
                />
                <p-button
                  label="Publicar"
                  size="small"
                  [ariaLabel]="'Publicar la versión ' + p.version"
                  (onClick)="publicar(p)"
                />
              }
            </td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr>
            <td colspan="5">
              <app-empty-state icon="pi-book" title="Sin versiones de la política" />
            </td>
          </tr>
        </ng-template>
      </p-table>
    </section>

    <p-dialog
      [header]="editandoId() ? 'Editar el borrador' : 'Nueva versión'"
      [(visible)]="editor"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '46rem', maxWidth: '96vw' }"
    >
      <form class="form" (submit)="$event.preventDefault(); guardar()" novalidate>
        @if (error(); as mensaje) {
          <p-message severity="error" styleClass="w-full">{{ mensaje }}</p-message>
        }
        <label for="pol-titulo">Título</label>
        <input
          pInputText
          id="pol-titulo"
          class="w-full"
          maxlength="150"
          [ngModel]="titulo()"
          (ngModelChange)="titulo.set($event)"
          [ngModelOptions]="{ standalone: true }"
        />
        <label for="pol-desde">Vigente desde</label>
        <p-datepicker
          inputId="pol-desde"
          [ngModel]="desde()"
          (ngModelChange)="desde.set($event)"
          [ngModelOptions]="{ standalone: true }"
          dateFormat="dd/mm/yy"
          [showIcon]="true"
          appendTo="body"
        />
        <label for="pol-texto">Texto</label>
        <textarea
          pTextarea
          id="pol-texto"
          rows="14"
          class="w-full"
          maxlength="40000"
          [ngModel]="contenido()"
          (ngModelChange)="contenido.set($event)"
          [ngModelOptions]="{ standalone: true }"
        ></textarea>
        <small class="text-muted"
          >Una versión publicada no se puede modificar. Los usuarios deberán aceptar la nueva
          versión.</small
        >
      </form>
      <ng-template #footer>
        <p-button
          label="Cancelar"
          severity="secondary"
          [outlined]="true"
          (onClick)="editor.set(false)"
        />
        <p-button
          label="Guardar borrador"
          icon="pi pi-check"
          [loading]="guardando()"
          (onClick)="guardar()"
        />
      </ng-template>
    </p-dialog>

    <p-dialog
      [header]="lectura() ? lectura()!.titulo + ' · v' + lectura()!.version : ''"
      [visible]="lectura() !== null"
      (visibleChange)="$event ? null : lectura.set(null)"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '44rem', maxWidth: '96vw' }"
    >
      @if (lectura(); as p) {
        <div class="texto">{{ p.contenido }}</div>
        <small class="text-muted block">Huella: {{ p.hash }}</small>
      }
    </p-dialog>
  `,
  styles: `
    .derecha {
      text-align: right;
      white-space: nowrap;
    }

    .form {
      display: flex;
      flex-direction: column;
      gap: 0.4rem;
    }

    .form label {
      font-weight: 600;
      font-size: 0.9rem;
      margin-top: 0.3rem;
    }

    .texto {
      white-space: pre-wrap;
      line-height: 1.55;
      max-height: 60vh;
      overflow: auto;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PoliticaAdmin implements OnInit {
  private readonly api = inject(ProteccionDatosApiService);
  private readonly notificaciones = inject(NotificationService);

  protected readonly politicas = signal<PoliticaPrivacidad[]>([]);
  protected readonly lectura = signal<PoliticaPrivacidad | null>(null);
  protected readonly editor = signal(false);
  protected readonly editandoId = signal<string | null>(null);
  protected readonly titulo = signal('');
  protected readonly contenido = signal('');
  protected readonly desde = signal<Date>(new Date());
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.cargar();
  }

  protected ver(p: PoliticaPrivacidad): void {
    this.lectura.set(p);
  }

  protected abrirNueva(): void {
    this.error.set(null);
    this.editandoId.set(null);
    const ultima = this.politicas()[0];
    this.titulo.set(ultima?.titulo ?? 'Política de privacidad y tratamiento de datos personales');
    this.contenido.set(ultima?.contenido ?? '');
    this.desde.set(new Date());
    this.editor.set(true);
  }

  protected abrirEdicion(p: PoliticaPrivacidad): void {
    this.error.set(null);
    this.editandoId.set(p.id);
    this.titulo.set(p.titulo);
    this.contenido.set(p.contenido);
    this.desde.set(new Date(p.vigenteDesde + 'T00:00:00'));
    this.editor.set(true);
  }

  protected guardar(): void {
    if (!this.titulo().trim() || this.contenido().trim().length < 50) {
      this.error.set('Indique el título y un texto de al menos 50 caracteres.');
      return;
    }
    this.error.set(null);
    this.guardando.set(true);
    const request = {
      titulo: this.titulo().trim(),
      contenido: this.contenido().trim(),
      vigenteDesde: toIsoDate(this.desde()),
    };
    const id = this.editandoId();
    (id ? this.api.actualizarPolitica(id, request) : this.api.crearPolitica(request)).subscribe({
      next: () => {
        this.guardando.set(false);
        this.editor.set(false);
        this.notificaciones.success('El borrador se guardó.');
        this.cargar();
      },
      error: (e: { message: string }) => {
        this.guardando.set(false);
        this.error.set(e.message);
      },
    });
  }

  protected publicar(p: PoliticaPrivacidad): void {
    this.api.publicarPolitica(p.id).subscribe(() => {
      this.notificaciones.success(`La versión ${p.version} quedó publicada.`);
      this.cargar();
    });
  }

  private cargar(): void {
    this.api.politicas().subscribe((lista) => this.politicas.set(lista));
  }
}
