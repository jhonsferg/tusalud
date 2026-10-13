import { computed, signal } from '@angular/core';
import { Observable, Subscription, catchError, defer, finalize, tap, throwError } from 'rxjs';

import { ApiError } from '@tusalud/app/core/models';
import { asApiError } from '@tusalud/app/core/utils/api-error';

export class AsyncState<T> {
  private readonly valueState = signal<T | undefined>(undefined);
  private readonly loadingState = signal(false);
  private readonly errorState = signal<ApiError | null>(null);
  private subscription: Subscription | null = null;

  readonly value = this.valueState.asReadonly();
  readonly loading = this.loadingState.asReadonly();
  readonly error = this.errorState.asReadonly();
  readonly hasValue = computed(() => this.valueState() !== undefined);

  track(source: Observable<T>): Observable<T> {
    return defer(() => {
      this.loadingState.set(true);
      this.errorState.set(null);
      return source.pipe(
        tap((value) => this.valueState.set(value)),
        catchError((error: unknown) => {
          const apiError = asApiError(error);
          this.errorState.set(apiError);
          return throwError(() => apiError);
        }),
        finalize(() => this.loadingState.set(false)),
      );
    });
  }

  load(source: Observable<T>): void {
    this.cancel();
    this.subscription = this.track(source).subscribe({ error: () => undefined });
  }

  set(value: T | undefined): void {
    this.valueState.set(value);
  }

  update(updater: (current: T | undefined) => T | undefined): void {
    this.valueState.update(updater);
  }

  cancel(): void {
    this.subscription?.unsubscribe();
    this.subscription = null;
    this.loadingState.set(false);
  }

  reset(): void {
    this.cancel();
    this.valueState.set(undefined);
    this.errorState.set(null);
  }
}
