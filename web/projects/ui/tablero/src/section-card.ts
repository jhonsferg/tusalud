import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-section-card',
  template: `
    <section class="tarjeta" [attr.aria-busy]="cargando()">
      <header>
        <div class="titulos">
          @if (icono()) {
            <span class="icono" aria-hidden="true"><i [class]="'pi ' + icono()"></i></span>
          }
          <div>
            <h2>{{ titulo() }}</h2>
            @if (subtitulo()) {
              <p>{{ subtitulo() }}</p>
            }
          </div>
        </div>
        <div class="acciones"><ng-content select="[acciones]" /></div>
      </header>

      @if (cargando()) {
        <div class="esqueleto" role="status" aria-label="Cargando">
          <span></span><span></span><span></span>
        </div>
      } @else if (error()) {
        <p class="estado error" role="alert">
          <i class="pi pi-exclamation-triangle" aria-hidden="true"></i> {{ error() }}
        </p>
      } @else if (vacio()) {
        <p class="estado vacio">
          <i class="pi pi-inbox" aria-hidden="true"></i> {{ textoVacio() }}
        </p>
      } @else {
        <div class="cuerpo"><ng-content /></div>
      }

      <footer><ng-content select="[pie]" /></footer>
    </section>
  `,
  styles: `
    :host {
      display: block;
      height: 100%;
      min-width: 0;
    }

    footer ::ng-deep a {
      font-size: 0.875rem;
      font-weight: 600;
    }

    .tarjeta {
      display: flex;
      flex-direction: column;
      height: 100%;
      border: 1px solid var(--p-content-border-color);
      border-radius: var(--radio-md);
      background: var(--p-content-background);
      box-shadow: var(--sombra-1);
    }

    header {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: var(--sp-2) var(--sp-3);
      padding: var(--sp-4) var(--sp-5) var(--sp-3);
    }

    .titulos {
      display: flex;
      align-items: center;
      gap: var(--sp-3);
      min-width: 0;
    }

    .icono {
      display: grid;
      flex: none;
      place-items: center;
      width: 2.25rem;
      height: 2.25rem;
      border-radius: var(--radio-sm);
      background: var(--app-primary-soft);
      color: var(--app-primary-text);
    }

    h2 {
      margin: 0;
      font-size: 1rem;
      font-weight: 700;
    }

    p {
      margin: 0;
    }

    .titulos p {
      color: var(--p-text-muted-color);
      font-size: 0.8125rem;
    }

    .acciones {
      display: flex;
      flex-wrap: wrap;
      gap: var(--sp-2);
    }

    .cuerpo {
      flex: 1;
      padding: 0 var(--sp-5) var(--sp-4);
    }

    footer:empty {
      display: none;
    }

    footer {
      padding: var(--sp-3) var(--sp-5);
      border-top: 1px solid var(--p-content-border-color);
    }

    .estado {
      display: flex;
      align-items: center;
      gap: var(--sp-2);
      padding: var(--sp-4) var(--sp-5) var(--sp-5);
      color: var(--p-text-muted-color);
      font-size: var(--texto-dato);
    }

    .estado.error {
      color: var(--estado-peligro);
    }

    .esqueleto {
      display: grid;
      gap: var(--sp-3);
      padding: var(--sp-2) var(--sp-5) var(--sp-5);

      span {
        height: 0.9rem;
        border-radius: 6px;
        background: var(--p-content-hover-background);
        animation: pulso 1.4s ease-in-out infinite;
      }

      span:nth-child(2) {
        width: 80%;
      }

      span:nth-child(3) {
        width: 55%;
      }
    }

    @keyframes pulso {
      50% {
        opacity: 0.5;
      }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SectionCard {
  readonly titulo = input.required<string>();
  readonly subtitulo = input<string | null>(null);
  readonly icono = input<string | null>(null);
  readonly cargando = input(false);
  readonly error = input<string | null>(null);
  readonly vacio = input(false);
  readonly textoVacio = input('No hay información para mostrar.');
}
