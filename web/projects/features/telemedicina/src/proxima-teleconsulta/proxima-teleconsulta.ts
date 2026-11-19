import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AuthService, TeleconsultaStore } from '@tusalud/core';

@Component({
  selector: 'app-proxima-teleconsulta',
  imports: [DatePipe, RouterLink],
  template: `
    @if (proxima(); as t) {
      <a class="tarjeta" routerLink="/teleconsultas" aria-label="Próxima teleconsulta">
        <i class="pi pi-video" aria-hidden="true"></i>
        <span class="texto">
          <strong>Próxima teleconsulta</strong>
          <small>
            {{ t.fechaHora | date: 'EEEE d MMM, HH:mm' }} con
            {{ esMedico() ? t.pacienteNombre : t.medicoNombre }}
          </small>
        </span>
        @if (t.puedeIngresar) {
          <span class="ahora">Disponible ahora</span>
        }
      </a>
    }
  `,
  styles: `
    .tarjeta {
      display: flex;
      align-items: center;
      gap: 0.9rem;
      margin-bottom: 1rem;
      padding: 0.9rem 1.1rem;
      border: 1px solid color-mix(in srgb, var(--p-primary-color) 35%, transparent);
      border-radius: 12px;
      background: color-mix(in srgb, var(--p-primary-color) 7%, var(--p-content-background));
      color: var(--p-text-color);
      text-decoration: none;
    }

    .pi {
      font-size: 1.4rem;
      color: var(--p-primary-color);
    }

    .texto {
      display: flex;
      flex: 1;
      flex-direction: column;
      text-transform: capitalize;
    }

    .ahora {
      padding: 0.2rem 0.6rem;
      border-radius: 999px;
      background: var(--p-primary-color);
      color: #ffffff;
      font-size: 0.78rem;
      font-weight: 700;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProximaTeleconsulta implements OnInit {
  private readonly store = inject(TeleconsultaStore);
  private readonly auth = inject(AuthService);

  protected readonly esMedico = computed(() => this.auth.isMedico());
  protected readonly proxima = computed(
    () =>
      (this.store.lista.value() ?? []).find((t) =>
        ['PROGRAMADA', 'SALA_ABIERTA', 'EN_CURSO'].includes(t.estado),
      ) ?? null,
  );

  ngOnInit(): void {
    if (this.auth.isMedico()) {
      this.store.cargarDelMedico();
    } else {
      this.store.cargarMias();
    }
  }
}
