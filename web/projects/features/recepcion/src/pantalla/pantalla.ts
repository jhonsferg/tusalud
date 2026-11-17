import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject } from '@angular/core';

import { RecepcionStore } from '@tusalud/core';
import { PageHeader } from '@tusalud/ui/page-header';

const REFRESCO_MS = 5000;

@Component({
  selector: 'app-pantalla',
  imports: [PageHeader],
  templateUrl: './pantalla.html',
  styleUrl: './pantalla.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Pantalla implements OnInit {
  protected readonly store = inject(RecepcionStore);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.store.cargarPantalla();
    const temporizador = setInterval(() => this.store.cargarPantalla(), REFRESCO_MS);
    this.destroyRef.onDestroy(() => clearInterval(temporizador));
  }
}
