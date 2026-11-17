import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { createSession } from '../../testing/auth-fixtures';
import { Rol } from '../models';
import { AuthService, SESSION_STORAGE_KEY } from './auth.service';
import { TourService } from './tour.service';

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

  it('navega a la pantalla de cada paso y vuelve al inicio al terminar', async () => {
    const tour = crearTour(['ROLE_PACIENTE'], ['nav:/citas', 'nav:/citas/nueva']);
    const router = TestBed.inject(Router);
    const navegar = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    tour.start();
    tour.next();
    expect(navegar).toHaveBeenLastCalledWith('/citas/nueva');
    tour.next();
    expect(navegar).toHaveBeenLastCalledWith('/citas');
    while (!tour.isLast()) {
      tour.next();
    }
    tour.next();
    expect(navegar).toHaveBeenLastCalledWith(TestBed.inject(AuthService).homeUrl());
  });

  it('solo se ofrece la primera vez en el navegador y no si el servidor ya la marcó', () => {
    const tour = crearTour(['ROLE_PACIENTE'], []);
    expect(tour.debeOfrecerse(false)).toBe(true);
    expect(tour.debeOfrecerse(true)).toBe(false);
    tour.marcarVisto();
    expect(tour.debeOfrecerse(false)).toBe(false);
  });
});
