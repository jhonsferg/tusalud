import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Tag } from 'primeng/tag';
import { Textarea } from 'primeng/textarea';

import {
  AuthService,
  GuiaDetalle,
  GuiaResumen,
  NotificationService,
  SeguridadPacienteApiService,
} from '@tusalud/core';
import { PageHeader } from '@tusalud/ui/page-header';

@Component({
  selector: 'app-guias-clinicas',
  imports: [DatePipe, FormsModule, Button, InputText, Message, Tag, Textarea, PageHeader],
  template: `
    <app-page-header
      title="Guías de práctica clínica"
      subtitle="Biblioteca versionada de guías y protocolos, vinculada a los diagnósticos CIE-10"
      icon="pi-book"
    />
    <p-message severity="warn" [closable]="false">
      Las guías de demostración son resúmenes y deben validarse con el comité médico antes de usarse
      en la atención.
    </p-message>
    <section class="panel">
      <div class="busqueda">
        <input
          pInputText
          aria-label="Buscar guías por título, código o diagnóstico"
          placeholder="Buscar por título, código o diagnóstico"
          [ngModel]="q()"
          (ngModelChange)="q.set($event)"
          (keyup.enter)="buscar()"
        />
        <p-button label="Buscar" icon="pi pi-search" (onClick)="buscar()" />
      </div>
      @for (g of guias(); track g.id) {
        <article class="guia">
          <div>
            <strong>{{ g.titulo }}</strong>
            <p-tag [value]="'Versión ' + g.version" severity="info" />
            @if (g.porValidar) {
              <p-tag value="Por validar" severity="warn" />
            }
            <p class="text-muted">
              {{ g.codigo }} · CIE-10 {{ g.codigosCie10.join(', ') }} · vigente desde
              {{ g.vigenteDesde | date: 'd MMM y' }}
            </p>
            <p>{{ g.resumen }}</p>
          </div>
          <p-button
            label="Ver guía"
            size="small"
            [outlined]="true"
            [ariaLabel]="'Ver la guía ' + g.titulo"
            (onClick)="abrir(g)"
          />
        </article>
      } @empty {
        <p class="text-muted">No hay guías para esa búsqueda.</p>
      }
    </section>

    @if (detalle(); as d) {
      <section class="panel" aria-live="polite">
        <h3>{{ d.guia.titulo }}</h3>
        <pre class="contenido">{{ d.contenido }}</pre>
        <h4>Versiones</h4>
        @for (v of d.versiones; track v.id) {
          <p>
            Versión {{ v.version }} · {{ v.estado === 'VIGENTE' ? 'Vigente' : 'Obsoleta' }} desde
            {{ v.vigenteDesde | date: 'd MMM y' }}
          </p>
        }
      </section>
    }

    @if (puedeGestionar) {
      <section class="panel formulario">
        <h3>Publicar una guía nueva</h3>
        <input
          pInputText
          aria-label="Código de la guía"
          placeholder="Código"
          [ngModel]="codigo()"
          (ngModelChange)="codigo.set($event)"
        />
        <input
          pInputText
          aria-label="Título de la guía"
          placeholder="Título"
          [ngModel]="titulo()"
          (ngModelChange)="titulo.set($event)"
        />
        <input
          pInputText
          aria-label="Códigos CIE-10 separados por coma"
          placeholder="Códigos CIE-10 separados por coma"
          [ngModel]="cie()"
          (ngModelChange)="cie.set($event)"
        />
        <textarea
          pTextarea
          rows="2"
          aria-label="Resumen"
          placeholder="Resumen"
          [ngModel]="resumen()"
          (ngModelChange)="resumen.set($event)"
        ></textarea>
        <textarea
          pTextarea
          rows="5"
          aria-label="Contenido de la guía"
          placeholder="Contenido"
          [ngModel]="contenido()"
          (ngModelChange)="contenido.set($event)"
        ></textarea>
        <p-button
          label="Publicar guía"
          icon="pi pi-upload"
          [disabled]="!formularioValido()"
          (onClick)="publicar()"
        />
      </section>
    }
  `,
  styles: `
    section.panel {
      margin-top: 1rem;
    }

    .busqueda {
      display: flex;
      gap: 0.5rem;
      margin-bottom: 0.75rem;
    }

    .busqueda input {
      flex: 1;
    }

    .guia {
      display: flex;
      justify-content: space-between;
      gap: 1rem;
      padding: 0.75rem 0;
      border-bottom: 1px solid var(--p-content-border-color, #e5e7eb);
    }

    .guia p {
      margin: 0.25rem 0;
    }

    .contenido {
      white-space: pre-wrap;
      font-family: inherit;
    }

    .formulario {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
      max-width: 44rem;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GuiasClinicas implements OnInit {
  private readonly api = inject(SeguridadPacienteApiService);
  private readonly auth = inject(AuthService);
  private readonly avisos = inject(NotificationService);

  protected readonly puedeGestionar =
    this.auth.roles().includes('ROLE_CALIDAD') || this.auth.roles().includes('ROLE_ADMIN');
  protected readonly q = signal('');
  protected readonly guias = signal<GuiaResumen[]>([]);
  protected readonly detalle = signal<GuiaDetalle | null>(null);
  protected readonly codigo = signal('');
  protected readonly titulo = signal('');
  protected readonly cie = signal('');
  protected readonly resumen = signal('');
  protected readonly contenido = signal('');

  ngOnInit(): void {
    this.buscar();
  }

  protected buscar(): void {
    this.api.guias(this.q()).subscribe((lista) => this.guias.set(lista));
  }

  protected abrir(g: GuiaResumen): void {
    this.api.guia(g.id).subscribe((d) => this.detalle.set(d));
  }

  protected formularioValido(): boolean {
    return (
      this.codigo().trim().length > 0 &&
      this.titulo().trim().length > 0 &&
      this.cie().trim().length > 0 &&
      this.resumen().trim().length > 0 &&
      this.contenido().trim().length > 0
    );
  }

  protected publicar(): void {
    this.api
      .crearGuia({
        codigo: this.codigo().trim(),
        titulo: this.titulo().trim(),
        resumen: this.resumen().trim(),
        contenido: this.contenido().trim(),
        codigosCie10: this.cie()
          .split(',')
          .map((c) => c.trim())
          .filter((c) => c.length > 0),
        vigenteDesde: new Date().toISOString().slice(0, 10),
        porValidar: true,
      })
      .subscribe({
        next: () => {
          this.avisos.success('Guía publicada');
          this.codigo.set('');
          this.titulo.set('');
          this.cie.set('');
          this.resumen.set('');
          this.contenido.set('');
          this.buscar();
        },
        error: (e) => this.avisos.error(e.error?.message ?? 'No se pudo publicar la guía'),
      });
  }
}
