import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { Avatar } from 'primeng/avatar';

function hue(texto: string): number {
  let hash = 0;
  for (const caracter of texto) {
    hash = (hash * 31 + caracter.charCodeAt(0)) % 360;
  }
  return hash;
}

@Component({
  selector: 'app-user-avatar',
  imports: [Avatar],
  template: `
    @if (imagen()) {
      <p-avatar [image]="imagen()!" [size]="size()" shape="circle" [attr.aria-label]="nombre()" />
    } @else {
      <p-avatar
        [label]="iniciales()"
        [size]="size()"
        shape="circle"
        [style]="{ background: fondo(), color: '#ffffff', fontWeight: 700 }"
        [attr.aria-label]="nombre()"
      />
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserAvatar {
  readonly nombre = input.required<string>();
  readonly imagen = input<string | null | undefined>(null);
  readonly size = input<'normal' | 'large' | 'xlarge'>('normal');

  protected readonly iniciales = computed(() =>
    this.nombre()
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map((parte) => parte[0].toUpperCase())
      .join(''),
  );
  protected readonly fondo = computed(() => `hsl(${hue(this.nombre())} 55% 42%)`);
}
