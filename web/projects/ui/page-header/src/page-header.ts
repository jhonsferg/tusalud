import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

export interface MigaDePan {
  readonly label: string;
  readonly link?: string | readonly string[] | null;
}

@Component({
  selector: 'app-page-header',
  imports: [RouterLink],
  templateUrl: './page-header.html',
  styleUrl: './page-header.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PageHeader {
  readonly title = input.required<string>();
  readonly subtitle = input<string>();
  readonly icon = input<string>();
  readonly backLink = input<string>();
  readonly backLabel = input('Volver');
  readonly migas = input<readonly MigaDePan[] | null>(null);
}
