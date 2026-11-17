import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ConfirmDialog } from 'primeng/confirmdialog';

import { LoadingBar } from '@tusalud/app/shared/components/loading-bar/loading-bar';
import { NotificationOutlet } from '@tusalud/app/shared/components/notification-outlet/notification-outlet';

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
export class App {}
