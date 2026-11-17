import { Injectable, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';

import {
  PasswordChangeRequest,
  PerfilResponse,
  PerfilUpdateRequest,
} from '@tusalud/app/core/models';
import { PerfilApiService } from '@tusalud/app/core/services/api/perfil-api.service';
import { AuthService } from '@tusalud/app/core/services/auth.service';
import { AsyncState } from '@tusalud/app/core/state/async-state';
import { resetOnLogout } from '@tusalud/app/core/state/session-scoped';

@Injectable({ providedIn: 'root' })
export class PerfilStore {
  private readonly api = inject(PerfilApiService);
  private readonly auth = inject(AuthService);

  readonly perfil = new AsyncState<PerfilResponse>();

  constructor() {
    resetOnLogout(() => this.reset());
  }

  load(): void {
    this.perfil.load(this.api.obtener());
  }

  actualizar(request: PerfilUpdateRequest): Observable<PerfilResponse> {
    return this.api.actualizar(request).pipe(
      tap((perfil) => {
        this.perfil.set(perfil);
        this.auth.updateNombreCompleto(perfil.nombreCompleto);
      }),
    );
  }

  cambiarPassword(request: PasswordChangeRequest): Observable<void> {
    return this.api.cambiarPassword(request);
  }

  reset(): void {
    this.perfil.reset();
  }
}
