import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-notification-outlet',
  imports: [],
  templateUrl: './notification-outlet.html',
  styleUrl: './notification-outlet.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NotificationOutlet {}
