import { TestBed } from '@angular/core/testing';

import { DescargaService } from './descarga.service';

describe('DescargaService', () => {
  const crear = vi.fn(() => 'blob:prueba');
  const revocar = vi.fn();

  beforeEach(() => {
    vi.useFakeTimers();
    crear.mockClear();
    revocar.mockClear();
    vi.stubGlobal('URL', Object.assign(URL, { createObjectURL: crear, revokeObjectURL: revocar }));
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.unstubAllGlobals();
  });

  it('guarda el archivo con un enlace temporal que no lleva token', () => {
    const servicio = TestBed.inject(DescargaService);
    let enlace: HTMLAnchorElement | undefined;
    const original = HTMLAnchorElement.prototype.click;
    HTMLAnchorElement.prototype.click = function (this: HTMLAnchorElement) {
      enlace = this;
    };

    servicio.guardar({ blob: new Blob(['x']), nombre: 'receta.pdf' });
    HTMLAnchorElement.prototype.click = original;

    expect(enlace?.download).toBe('receta.pdf');
    expect(enlace?.href).toBe('blob:prueba');
    expect(document.body.contains(enlace ?? null)).toBe(false);
    vi.advanceTimersByTime(60_000);
    expect(revocar).toHaveBeenCalledWith('blob:prueba');
  });

  it('abre el archivo en otra pestaña', () => {
    const servicio = TestBed.inject(DescargaService);
    const abrir = vi.spyOn(window, 'open').mockReturnValue({} as Window);

    servicio.abrir({ blob: new Blob(['x']), nombre: 'receta.pdf' });

    expect(abrir).toHaveBeenCalledWith('blob:prueba', '_blank', 'noopener');
    abrir.mockRestore();
  });

  it('descarga el archivo cuando el navegador bloquea la ventana', () => {
    const servicio = TestBed.inject(DescargaService);
    const abrir = vi.spyOn(window, 'open').mockReturnValue(null);
    const guardar = vi.spyOn(servicio, 'guardar').mockImplementation(() => undefined);

    servicio.abrir({ blob: new Blob(['x']), nombre: 'receta.pdf' });

    expect(revocar).toHaveBeenCalledWith('blob:prueba');
    expect(guardar).toHaveBeenCalledTimes(1);
    abrir.mockRestore();
  });
});
