import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { Checkbox } from 'primeng/checkbox';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { Tag } from 'primeng/tag';
import { Textarea } from 'primeng/textarea';

import {
  EventoResumen,
  NotificationService,
  SeguridadPacienteApiService,
  SeveridadEvento,
  TipoReporteSeguridad,
} from '@tusalud/core';
import { PageHeader } from '@tusalud/ui/page-header';
import {
  ETIQUETA_ESTADO_EVENTO,
  ETIQUETA_SEVERIDAD,
  ETIQUETA_TIPO_EVENTO,
} from '../seguridad.utils';

@Component({
  selector: 'app-notificar-evento',
  imports: [DatePipe, FormsModule, Button, Checkbox, InputText, Select, Tag, Textarea, PageHeader],
  template: `
    <app-page-header
      title="Notificar un evento de seguridad"
      subtitle="Reporta eventos adversos y casi incidentes. Calidad los analiza y tú puedes seguir su estado"
      icon="pi-exclamation-triangle"
    />
    <section class="panel formulario">
      <label for="ev-tipo">Tipo</label>
      <p-select
        inputId="ev-tipo"
        [options]="tipos"
        optionLabel="label"
        optionValue="value"
        [ngModel]="tipo()"
        (ngModelChange)="tipo.set($event)"
      />
      <label for="ev-severidad">Severidad</label>
      <p-select
        inputId="ev-severidad"
        [options]="severidades"
        optionLabel="label"
        optionValue="value"
        [ngModel]="severidad()"
        (ngModelChange)="severidad.set($event)"
      />
      <label for="ev-area">Área donde ocurrió</label>
      <input pInputText id="ev-area" [ngModel]="area()" (ngModelChange)="area.set($event)" />
      <label for="ev-ocurrido">Fecha y hora</label>
      <input
        pInputText
        id="ev-ocurrido"
        type="datetime-local"
        [ngModel]="ocurrido()"
        (ngModelChange)="ocurrido.set($event)"
      />
      <label for="ev-descripcion">Qué ocurrió</label>
      <textarea
        pTextarea
        id="ev-descripcion"
        rows="4"
        [ngModel]="descripcion()"
        (ngModelChange)="descripcion.set($event)"
      ></textarea>
      <div class="anonimo">
        <p-checkbox
          inputId="ev-anonimo"
          [binary]="true"
          [ngModel]="anonimo()"
          (ngModelChange)="anonimo.set($event)"
        />
        <label for="ev-anonimo"
          >Notificar de forma anónima: calidad no verá mi nombre y no se vincula ningún
          paciente</label
        >
      </div>
      <p-button
        label="Enviar reporte"
        icon="pi pi-send"
        [disabled]="!valido()"
        [loading]="enviando()"
        (onClick)="enviar()"
      />
    </section>

    <section class="panel">
      <h3>Mis reportes</h3>
      @if (mios().length === 0) {
        <p class="text-muted">Todavía no has notificado ningún evento.</p>
      }
      @for (e of mios(); track e.id) {
        <p class="fila">
          <strong>{{ e.codigo }}</strong>
          <p-tag [value]="tipoEtiqueta[e.tipo]" severity="info" />
          <p-tag [value]="severidadEtiqueta[e.severidad]" severity="warn" />
          <p-tag [value]="estadoEtiqueta[e.estado]" severity="secondary" />
          <span class="text-muted"
            >{{ e.area }} · {{ e.notificadoEn | date: 'd MMM y, HH:mm' }}</span
          >
        </p>
      }
    </section>
  `,
  styles: `
    .formulario {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
      max-width: 40rem;
      margin-bottom: 1rem;
    }

    .anonimo {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .fila {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 0.5rem;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NotificarEvento implements OnInit {
  private readonly api = inject(SeguridadPacienteApiService);
  private readonly avisos = inject(NotificationService);

  protected readonly tipoEtiqueta = ETIQUETA_TIPO_EVENTO;
  protected readonly severidadEtiqueta = ETIQUETA_SEVERIDAD;
  protected readonly estadoEtiqueta = ETIQUETA_ESTADO_EVENTO;
  protected readonly tipos = Object.entries(ETIQUETA_TIPO_EVENTO).map(([value, label]) => ({
    value,
    label,
  }));
  protected readonly severidades = Object.entries(ETIQUETA_SEVERIDAD).map(([value, label]) => ({
    value,
    label,
  }));

  protected readonly tipo = signal<TipoReporteSeguridad>('CASI_INCIDENTE');
  protected readonly severidad = signal<SeveridadEvento>('LEVE');
  protected readonly area = signal('');
  protected readonly ocurrido = signal('');
  protected readonly descripcion = signal('');
  protected readonly anonimo = signal(false);
  protected readonly enviando = signal(false);
  protected readonly mios = signal<EventoResumen[]>([]);

  ngOnInit(): void {
    this.cargar();
  }

  protected valido(): boolean {
    return (
      this.area().trim().length > 0 &&
      this.ocurrido().length > 0 &&
      this.descripcion().trim().length >= 10
    );
  }

  protected enviar(): void {
    this.enviando.set(true);
    this.api
      .notificar({
        tipo: this.tipo(),
        severidad: this.severidad(),
        area: this.area().trim(),
        descripcion: this.descripcion().trim(),
        anonimo: this.anonimo(),
        ocurridoEn: this.ocurrido(),
      })
      .subscribe({
        next: (creado) => {
          this.enviando.set(false);
          this.avisos.success(`Reporte ${creado.codigo} enviado a calidad`);
          this.area.set('');
          this.descripcion.set('');
          this.anonimo.set(false);
          this.cargar();
        },
        error: (e) => {
          this.enviando.set(false);
          this.avisos.error(e.error?.message ?? 'No se pudo enviar el reporte');
        },
      });
  }

  private cargar(): void {
    this.api.mios().subscribe((lista) => this.mios.set(lista));
  }
}
