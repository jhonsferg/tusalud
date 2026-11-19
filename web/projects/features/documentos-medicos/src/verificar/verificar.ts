import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';

import { DocumentosMedicosApiService, VerificacionPublica } from '@tusalud/core';

@Component({
  selector: 'app-verificar',
  imports: [DatePipe, FormsModule, Button, InputText],
  template: `
    <main class="verificar">
      <h1>Verificar un documento médico</h1>
      <p class="intro">
        Ingresa el código que aparece al pie del documento o escanea su código QR. Solo mostramos si
        el documento es auténtico, nunca datos del paciente.
      </p>

      <form class="busqueda" (submit)="$event.preventDefault(); buscar()" novalidate>
        <label for="ver-codigo">Código de verificación</label>
        <input
          pInputText
          id="ver-codigo"
          autocomplete="off"
          placeholder="XXXX-XXXX-XXXX-XXXX"
          maxlength="40"
          [ngModel]="entrada()"
          (ngModelChange)="entrada.set($event)"
          [ngModelOptions]="{ standalone: true }"
        />
        <p-button type="submit" label="Verificar" icon="pi pi-search" [loading]="consultando()" />
      </form>

      @if (resultado(); as r) {
        <section
          class="resultado"
          [attr.data-resultado]="r.resultado"
          role="status"
          aria-live="polite"
        >
          @switch (r.resultado) {
            @case ('VALIDO') {
              <h2><i class="pi pi-verified" aria-hidden="true"></i> Documento válido</h2>
            }
            @case ('ANULADO') {
              <h2><i class="pi pi-ban" aria-hidden="true"></i> Documento anulado</h2>
              <p>
                El documento existe pero fue anulado por el médico emisor y ya no tiene validez.
              </p>
            }
            @case ('VENCIDO') {
              <h2><i class="pi pi-clock" aria-hidden="true"></i> Documento vencido</h2>
              <p>El documento es auténtico pero su vigencia terminó.</p>
            }
            @default {
              <h2>
                <i class="pi pi-times-circle" aria-hidden="true"></i> No se encontró el documento
              </h2>
              <p>Revisa que el código esté completo y vuelve a intentarlo.</p>
            }
          }
          @if (r.resultado !== 'NO_ENCONTRADO') {
            <dl>
              <div>
                <dt>Tipo</dt>
                <dd>{{ r.tipo }}</dd>
              </div>
              <div>
                <dt>Fecha de emisión</dt>
                <dd>{{ r.emitidoEn | date: 'd MMM y, HH:mm' }}</dd>
              </div>
              <div>
                <dt>Médico</dt>
                <dd>{{ r.medico }}</dd>
              </div>
              <div>
                <dt>CMP</dt>
                <dd>{{ r.colegiatura }}</dd>
              </div>
              <div>
                <dt>Establecimiento</dt>
                <dd>{{ r.establecimiento }}</dd>
              </div>
            </dl>
          }
        </section>
      }
      @if (limite()) {
        <p class="aviso" role="alert">
          Hiciste demasiadas consultas. Espera un minuto e inténtalo de nuevo.
        </p>
      }
    </main>
  `,
  styles: `
    .verificar {
      max-width: 40rem;
      margin: 2.5rem auto;
      padding: 0 1rem;
    }

    h1 {
      margin: 0 0 0.5rem;
      font-size: 1.6rem;
    }

    .intro {
      color: var(--p-text-muted-color);
    }

    .busqueda {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
      margin: 1.5rem 0;
    }

    .busqueda label {
      font-weight: 600;
    }

    .resultado {
      padding: 1.25rem;
      border-radius: 12px;
      border: 2px solid var(--p-content-border-color);
    }

    .resultado[data-resultado='VALIDO'] {
      border-color: #15803d;
      background: #f0fdf4;
    }

    .resultado[data-resultado='ANULADO'],
    .resultado[data-resultado='NO_ENCONTRADO'] {
      border-color: #b91c1c;
      background: #fef2f2;
    }

    .resultado[data-resultado='VENCIDO'] {
      border-color: #b45309;
      background: #fffbeb;
    }

    h2 {
      margin: 0 0 0.5rem;
      font-size: 1.25rem;
    }

    dl {
      display: grid;
      gap: 0.5rem;
      margin: 0.75rem 0 0;
    }

    dt {
      font-size: 0.78rem;
      color: var(--p-text-muted-color);
      font-weight: 700;
    }

    dd {
      margin: 0;
    }

    .aviso {
      color: var(--p-red-700);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Verificar implements OnInit {
  readonly codigo = input<string>();

  private readonly api = inject(DocumentosMedicosApiService);
  private readonly router = inject(Router);

  protected readonly entrada = signal('');
  protected readonly resultado = signal<VerificacionPublica | null>(null);
  protected readonly consultando = signal(false);
  protected readonly limite = signal(false);

  ngOnInit(): void {
    const codigo = this.codigo();
    if (codigo) {
      this.entrada.set(codigo);
      this.consultar(codigo);
    }
  }

  protected buscar(): void {
    const codigo = this.entrada().trim();
    if (!codigo) {
      return;
    }
    void this.router.navigate(['/verificar', codigo]);
    this.consultar(codigo);
  }

  private consultar(codigo: string): void {
    this.consultando.set(true);
    this.limite.set(false);
    this.api.verificar(codigo).subscribe({
      next: (r) => {
        this.consultando.set(false);
        this.resultado.set(r);
      },
      error: (e: { status?: number }) => {
        this.consultando.set(false);
        this.resultado.set(null);
        this.limite.set(e.status === 429);
      },
    });
  }
}
