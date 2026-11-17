import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ProgressBar } from 'primeng/progressbar';

import { LoadingService } from '@tusalud/app/core/services/loading.service';

@Component({
  selector: 'app-loading-bar',
  imports: [ProgressBar],
  templateUrl: './loading-bar.html',
  styleUrl: './loading-bar.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoadingBar {
  protected readonly loading = inject(LoadingService).isLoading;
}
