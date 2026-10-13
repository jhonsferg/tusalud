import { effect, inject, untracked } from '@angular/core';

import { AuthService } from '@tusalud/app/core/services/auth.service';

export function resetOnLogout(reset: () => void): void {
  const auth = inject(AuthService);
  effect(() => {
    if (!auth.isAuthenticated()) {
      untracked(reset);
    }
  });
}
