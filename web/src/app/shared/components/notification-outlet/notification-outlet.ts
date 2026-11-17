import { ChangeDetectionStrategy, Component, effect, inject, untracked } from '@angular/core';
import { MessageService } from 'primeng/api';
import { Toast } from 'primeng/toast';

import { NotificationService } from '@tusalud/app/core/services/notification.service';

const SEVERITY = { success: 'success', info: 'info', warning: 'warn', error: 'error' } as const;
const TITLE = {
  success: 'Listo',
  info: 'Información',
  warning: 'Atención',
  error: 'No se pudo completar',
} as const;

@Component({
  selector: 'app-notification-outlet',
  imports: [Toast],
  templateUrl: './notification-outlet.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NotificationOutlet {
  private readonly notifications = inject(NotificationService);
  private readonly messages = inject(MessageService);

  constructor() {
    effect(() => {
      const pending = this.notifications.notifications();
      untracked(() => {
        pending.forEach((item) => {
          this.messages.add({
            severity: SEVERITY[item.type],
            summary: TITLE[item.type],
            detail: item.message,
            life: 5000,
          });
          this.notifications.dismiss(item.id);
        });
      });
    });
  }
}
