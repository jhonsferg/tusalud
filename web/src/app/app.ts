import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ConfirmDialog } from 'primeng/confirmdialog';

import { ThemeService } from '@tusalud/core';
import { LoadingBar } from '@tusalud/ui/loading-bar';
import { NotificationOutlet } from '@tusalud/ui/notification-outlet';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, LoadingBar, NotificationOutlet, ConfirmDialog],
  template: `
    <app-loading-bar />
    <app-notification-outlet />
    <p-confirmdialog [style]="{ width: '28rem', maxWidth: '95vw' }" />
    <router-outlet />
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class App {
  constructor() {
    inject(ThemeService);
  }
}
