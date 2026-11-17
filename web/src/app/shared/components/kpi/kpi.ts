import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-kpi',
  template: `
    <div class="kpi" [attr.data-tono]="tono()">
      <span class="icono"><i [class]="'pi ' + icono()"></i></span>
      <div>
        <strong>{{ valor() }}</strong>
        <small>{{ etiqueta() }}</small>
      </div>
    </div>
  `,
  styles: `
    .kpi {
      display: flex;
      align-items: center;
      gap: 0.9rem;
      padding: 1rem 1.15rem;
      border: 1px solid var(--p-content-border-color);
      border-radius: 14px;
      background: var(--p-content-background);
      box-shadow: 0 1px 2px rgb(16 24 40 / 5%);
    }

    .icono {
      display: grid;
      flex: none;
      place-items: center;
      width: 2.8rem;
      height: 2.8rem;
      border-radius: 12px;
      background: var(--app-primary-soft);
      color: var(--app-primary-text);
      font-size: 1.2rem;
    }

    [data-tono='alerta'] .icono {
      background: color-mix(in srgb, var(--p-orange-500) 16%, transparent);
      color: var(--p-orange-600);
    }

    [data-tono='peligro'] .icono {
      background: color-mix(in srgb, var(--p-red-500) 14%, transparent);
      color: var(--p-red-600);
    }

    [data-tono='exito'] .icono {
      background: color-mix(in srgb, var(--p-green-500) 14%, transparent);
      color: var(--p-green-600);
    }

    strong {
      display: block;
      font-size: 1.5rem;
      line-height: 1.1;
    }

    small {
      color: var(--p-text-muted-color);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Kpi {
  readonly etiqueta = input.required<string>();
  readonly valor = input.required<string | number>();
  readonly icono = input('pi-chart-bar');
  readonly tono = input<'normal' | 'alerta' | 'peligro' | 'exito'>('normal');
}
