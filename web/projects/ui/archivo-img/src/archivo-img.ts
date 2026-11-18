import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  effect,
  inject,
  input,
  signal,
  untracked,
} from '@angular/core';

import { ArchivoBlob, ArchivoBlobService } from '@tusalud/core';

@Component({
  selector: 'app-archivo-img',
  template: `
    @if (url(); as direccion) {
      <img [src]="direccion" [alt]="alt()" [style.object-fit]="ajuste()" />
    } @else if (fallo()) {
      <span class="estado" role="img" [attr.aria-label]="alt()"><i class="pi pi-image"></i></span>
    } @else {
      <span class="estado cargando" aria-hidden="true"></span>
    }
  `,
  styles: `
    :host {
      display: inline-block;
      overflow: hidden;
      line-height: 0;
    }

    img {
      width: 100%;
      height: 100%;
    }

    .estado {
      display: grid;
      place-items: center;
      width: 100%;
      height: 100%;
      min-height: 2rem;
      background: var(--p-content-hover-background);
      color: var(--p-text-muted-color);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ArchivoImg {
  readonly archivoId = input<string | null | undefined>(null);
  readonly alt = input('');
  readonly ajuste = input<'cover' | 'contain'>('cover');

  protected readonly url = signal<string | null>(null);
  protected readonly fallo = signal(false);

  private readonly blobs = inject(ArchivoBlobService);
  private actual: ArchivoBlob | null = null;
  private pedido: string | null = null;

  constructor() {
    inject(DestroyRef).onDestroy(() => this.liberar());
    effect(() => {
      const id = this.archivoId() ?? null;
      untracked(() => this.cargar(id));
    });
  }

  private cargar(id: string | null): void {
    this.liberar();
    this.fallo.set(false);
    this.pedido = id;
    if (!id) {
      return;
    }
    this.blobs.cargar(id).subscribe({
      next: (blob) => {
        if (this.pedido !== id) {
          blob.liberar();
          return;
        }
        this.actual = blob;
        this.url.set(blob.url);
      },
      error: () => {
        if (this.pedido === id) {
          this.fallo.set(true);
        }
      },
    });
  }

  private liberar(): void {
    this.actual?.liberar();
    this.actual = null;
    this.url.set(null);
    this.pedido = null;
  }
}
