import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-security-notice',
  template: `
    <p class="aviso" role="note" [attr.data-variante]="variante()">
      <i
        [class]="'pi ' + (variante() === 'auditado' ? 'pi-shield' : 'pi-lock')"
        aria-hidden="true"
      ></i>
      <span>
        @if (texto()) {
          {{ texto() }}
        } @else if (variante() === 'auditado') {
          Este acceso queda registrado en la auditoría clínica con tu usuario, la fecha y la hora.
        } @else {
          Información confidencial. Úsala solo para la atención del paciente.
        }
      </span>
    </p>
  `,
  styles: `
    .aviso {
      display: flex;
      align-items: flex-start;
      gap: var(--sp-2);
      margin: 0;
      padding: var(--sp-2) var(--sp-3);
      border: 1px solid color-mix(in srgb, var(--estado-info) 35%, transparent);
      border-radius: var(--radio-sm);
      background: var(--estado-info-fondo);
      color: var(--estado-info);
      font-size: 0.8125rem;

      i {
        margin-top: 0.15rem;
      }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SecurityNotice {
  readonly variante = input<'auditado' | 'confidencial'>('auditado');
  readonly texto = input<string | null>(null);
}
