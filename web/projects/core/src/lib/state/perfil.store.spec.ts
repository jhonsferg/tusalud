import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { createSession } from '../../testing/auth-fixtures';
import { PerfilResponse } from '../models';
import { SESSION_STORAGE_KEY } from '../services/auth.service';
import { PerfilStore } from './perfil.store';

const AVATAR = '11111111-1111-4111-8111-111111111111';
const OTRO = '22222222-2222-4222-8222-222222222222';

function perfil(avatarId: string | null): PerfilResponse {
  return {
    usuarioId: '00000000-0000-4000-8000-000000000001',
    nombre: 'Juan',
    apellido: 'Pérez',
    nombreCompleto: 'Juan Pérez',
    email: 'juan@correo.com',
    roles: ['ROLE_PACIENTE'],
    createdAt: '2026-01-01T00:00:00',
    avatarId,
  };
}

describe('PerfilStore avatar', () => {
  const crear = vi.fn((blob: Blob) => `blob:${blob.size}`);
  const revocar = vi.fn();
  let store: PerfilStore;
  let controller: HttpTestingController;

  beforeEach(() => {
    crear.mockClear();
    revocar.mockClear();
    vi.stubGlobal('URL', Object.assign(URL, { createObjectURL: crear, revokeObjectURL: revocar }));
    localStorage.clear();
    localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(createSession(['ROLE_PACIENTE'])));
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    store = TestBed.inject(PerfilStore);
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    controller.verify();
    vi.unstubAllGlobals();
  });

  it('descarga la foto autenticada cuando el perfil trae un avatar', () => {
    store.perfil.set(perfil(AVATAR));
    TestBed.tick();

    controller.expectOne(`/api/v1/archivos/${AVATAR}`).flush(new Blob(['abc']));

    expect(store.avatarUrl()).toBe('blob:3');
  });

  it('reemplaza y libera la foto anterior al cambiar el avatar', () => {
    store.perfil.set(perfil(AVATAR));
    TestBed.tick();
    controller.expectOne(`/api/v1/archivos/${AVATAR}`).flush(new Blob(['abc']));

    store.perfil.set(perfil(OTRO));
    TestBed.tick();

    expect(revocar).toHaveBeenCalledWith('blob:3');
    controller.expectOne(`/api/v1/archivos/${OTRO}`).flush(new Blob(['abcd']));
    expect(store.avatarUrl()).toBe('blob:4');
  });

  it('quita la foto cuando ya no hay avatar y al cerrar la sesión', () => {
    store.perfil.set(perfil(AVATAR));
    TestBed.tick();
    controller.expectOne(`/api/v1/archivos/${AVATAR}`).flush(new Blob(['abc']));

    store.perfil.set(perfil(null));
    TestBed.tick();
    expect(store.avatarUrl()).toBeNull();
    expect(revocar).toHaveBeenCalledWith('blob:3');

    store.perfil.set(perfil(AVATAR));
    TestBed.tick();
    controller.expectOne(`/api/v1/archivos/${AVATAR}`).flush(new Blob(['abc']));
    store.reset();
    expect(store.avatarUrl()).toBeNull();
  });

  it('no deja una foto vieja si la respuesta llega después de cambiar de avatar', () => {
    store.perfil.set(perfil(AVATAR));
    TestBed.tick();
    const primera = controller.expectOne(`/api/v1/archivos/${AVATAR}`);

    store.perfil.set(perfil(OTRO));
    TestBed.tick();
    primera.flush(new Blob(['abc']));
    controller.expectOne(`/api/v1/archivos/${OTRO}`).flush(new Blob(['abcd']));

    expect(store.avatarUrl()).toBe('blob:4');
    expect(revocar).toHaveBeenCalledWith('blob:3');
  });
});
