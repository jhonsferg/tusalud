import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { REGLAS_ARCHIVO, ReglasArchivo } from '@tusalud/core';
import { CargaArchivos } from './carga-archivos';

@Component({
  imports: [CargaArchivos],
  template: `<app-carga-archivos
    [reglas]="reglas"
    [multiple]="multiple()"
    [subiendo]="subiendo()"
    [progreso]="progreso()"
    [(archivos)]="archivos"
    (rechazados)="rechazos.push($event)"
  />`,
})
class Anfitrion {
  reglas: ReglasArchivo = { ...REGLAS_ARCHIVO.IMAGEN_DIAGNOSTICA, maxArchivos: 2 };
  multiple = signal(true);
  subiendo = signal(false);
  progreso = signal<number | null>(null);
  archivos: File[] = [];
  rechazos: string[][] = [];
}

function archivo(nombre: string, bytes = 100): File {
  return new File([new Uint8Array(bytes)], nombre);
}

describe('CargaArchivos', () => {
  const crear = vi.fn(() => 'blob:vista');
  const revocar = vi.fn();

  beforeEach(() => {
    crear.mockClear();
    revocar.mockClear();
    vi.stubGlobal('URL', Object.assign(URL, { createObjectURL: crear, revokeObjectURL: revocar }));
  });

  afterEach(() => vi.unstubAllGlobals());

  async function montar() {
    const fixture = TestBed.createComponent(Anfitrion);
    fixture.detectChanges();
    await fixture.whenStable();
    const raiz = fixture.nativeElement as HTMLElement;
    const entrada = raiz.querySelector('input[type=file]') as HTMLInputElement;
    return { fixture, raiz, entrada, anfitrion: fixture.componentInstance };
  }

  function elegir(entrada: HTMLInputElement, archivos: File[]): void {
    Object.defineProperty(entrada, 'files', { value: archivos, configurable: true });
    entrada.dispatchEvent(new Event('change'));
  }

  it('muestra las reglas y restringe el selector a los tipos permitidos', async () => {
    const { raiz, entrada } = await montar();

    expect(entrada.accept).toBe('image/jpeg,image/png,application/pdf');
    expect(entrada.multiple).toBe(true);
    expect(raiz.querySelector('.ayuda')?.textContent).toContain('JPG, JPEG, PNG, PDF');
    expect(raiz.querySelector('.ayuda')?.textContent).toContain('5.0 MB');
  });

  it('acepta archivos válidos, los lista y los emite', async () => {
    const { fixture, raiz, entrada, anfitrion } = await montar();

    elegir(entrada, [archivo('toma.png'), archivo('informe.pdf')]);
    fixture.detectChanges();
    await fixture.whenStable();

    expect(anfitrion.archivos.map((a) => a.name)).toEqual(['toma.png', 'informe.pdf']);
    expect(raiz.querySelectorAll('.elegidos li')).toHaveLength(2);
    expect(raiz.querySelector('.elegidos img')).not.toBeNull();
  });

  it('rechaza formatos no permitidos y archivos demasiado grandes', async () => {
    const { fixture, raiz, entrada, anfitrion } = await montar();

    elegir(entrada, [
      archivo('virus.exe'),
      archivo('grande.png', 5 * 1024 * 1024 + 1),
      archivo('ok.jpg'),
    ]);
    fixture.detectChanges();
    await fixture.whenStable();

    expect(anfitrion.archivos.map((a) => a.name)).toEqual(['ok.jpg']);
    expect(anfitrion.rechazos[0]).toHaveLength(2);
    expect(raiz.querySelector('.errores')?.textContent).toContain('virus.exe');
    expect(raiz.querySelector('.errores')?.textContent).toContain('supera el máximo');
  });

  it('respeta el máximo de archivos y evita duplicados', async () => {
    const { fixture, entrada, anfitrion } = await montar();

    elegir(entrada, [archivo('a.png'), archivo('a.png'), archivo('b.png'), archivo('c.png')]);
    fixture.detectChanges();
    await fixture.whenStable();

    expect(anfitrion.archivos.map((a) => a.name)).toEqual(['a.png', 'b.png']);
  });

  it('en modo simple conserva solo el último archivo elegido', async () => {
    const { fixture, entrada, anfitrion } = await montar();
    anfitrion.multiple.set(false);
    fixture.detectChanges();

    elegir(entrada, [archivo('uno.png')]);
    fixture.detectChanges();
    await fixture.whenStable();
    elegir(entrada, [archivo('dos.png')]);
    fixture.detectChanges();
    await fixture.whenStable();

    expect(anfitrion.archivos.map((a) => a.name)).toEqual(['dos.png']);
  });

  it('quita archivos y libera las vistas previas', async () => {
    const { fixture, raiz, entrada, anfitrion } = await montar();
    elegir(entrada, [archivo('a.png'), archivo('b.png')]);
    fixture.detectChanges();
    await fixture.whenStable();

    (raiz.querySelector('.quitar') as HTMLButtonElement).click();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(anfitrion.archivos).toHaveLength(1);
    expect(revocar).toHaveBeenCalled();
  });

  it('bloquea la zona y muestra el progreso mientras sube', async () => {
    const { fixture, raiz, entrada, anfitrion } = await montar();

    anfitrion.subiendo.set(true);
    anfitrion.progreso.set(40);
    fixture.detectChanges();
    await fixture.whenStable();

    expect(entrada.disabled).toBe(true);
    expect(raiz.querySelector('.titulo')?.textContent).toContain('Cargando');
    expect(raiz.querySelector('p-progressbar')).not.toBeNull();
    expect(raiz.querySelector('.zona')?.classList).toContain('bloqueada');
  });

  it('acepta archivos soltados y marca la zona al arrastrar', async () => {
    const { fixture, raiz, anfitrion } = await montar();
    const zona = raiz.querySelector('.zona') as HTMLElement;

    const sobre = new Event('dragover', { cancelable: true });
    zona.dispatchEvent(sobre);
    fixture.detectChanges();
    expect(zona.classList).toContain('activa');
    expect(sobre.defaultPrevented).toBe(true);

    const soltar = new Event('drop', { cancelable: true }) as Event & { dataTransfer: unknown };
    soltar.dataTransfer = { files: [archivo('arrastrado.png')] };
    zona.dispatchEvent(soltar);
    fixture.detectChanges();
    await fixture.whenStable();

    expect(anfitrion.archivos.map((a) => a.name)).toEqual(['arrastrado.png']);
    expect(zona.classList).not.toContain('activa');
  });
});
