import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { createSession } from '@tusalud/testing/auth-fixtures';
import { Rol } from '@tusalud/app/core/models';
import { SESSION_STORAGE_KEY } from '@tusalud/app/core/services/auth.service';
import { TourService } from '@tusalud/app/core/services/tour.service';

function crearTour(roles: Rol[], destinos: string[]): TourService {
  TestBed.resetTestingModule();
  localStorage.clear();
  localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(createSession(roles)));
  TestBed.configureTestingModule({
    providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
  });
  document.body.innerHTML = '';
  for (const destino of [...destinos, 'bell', 'tema', 'usuario']) {
    const el = document.createElement('div');
    el.dataset['tour'] = destino;
    el.getBoundingClientRect = () => ({ width: 10, height: 10 }) as DOMRect;
    document.body.appendChild(el);
  }
  return TestBed.inject(TourService);
}

describe('TourService', () => {
  it('solo incluye los pasos del rol y cuyo elemento está visible', () => {
    const tour = crearTour(['ROLE_MEDICO'], ['nav:/agenda', 'nav:/pacientes', 'nav:/admin/panel']);
    tour.start();

    const titulos = tour.steps().map((paso) => paso.title);
    expect(titulos).toContain('Tu agenda');
    expect(titulos).toContain('Tus pacientes');
    expect(titulos).not.toContain('Panel de control');
    expect(tour.steps()[0].target).toBeNull();
    expect(tour.steps().at(-1)?.title).toBe('Todo listo');
  });

  it('avanza, retrocede y avisa al cerrar', () => {
    const tour = crearTour(['ROLE_PACIENTE'], ['nav:/citas', 'nav:/citas/nueva']);
    const alCerrar = vi.fn();
    tour.start(alCerrar);

    expect(tour.active()).toBe(true);
    expect(tour.isFirst()).toBe(true);
    tour.next();
    expect(tour.index()).toBe(1);
    tour.previous();
    tour.previous();
    expect(tour.index()).toBe(0);

    while (!tour.isLast()) {
      tour.next();
    }
    tour.next();
    expect(tour.active()).toBe(false);
    expect(alCerrar).toHaveBeenCalledTimes(1);
  });

  it('omite los pasos de pantallas que no se muestran', () => {
    const tour = crearTour(['ROLE_PACIENTE'], []);
    tour.start();
    expect(tour.steps().map((paso) => paso.target)).toEqual([
      null,
      'bell',
      'tema',
      'usuario',
      null,
    ]);
  });
});
