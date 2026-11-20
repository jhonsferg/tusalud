import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-vacio-plan',
  template: `
    <section class="panel">
      <p>
        Tu médico todavía no te asignó un plan de tratamiento. Cuando lo haga verás aquí tus metas,
        tus controles y podrás registrar tus mediciones.
      </p>
    </section>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EmptyStateAviso {}
