import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { PageHeader } from './page-header';

function crear(entradas: Record<string, unknown>): HTMLElement {
  TestBed.resetTestingModule();
  TestBed.configureTestingModule({ providers: [provideRouter([])] });
  const fixture = TestBed.createComponent(PageHeader);
  for (const [nombre, valor] of Object.entries(entradas)) {
    fixture.componentRef.setInput(nombre, valor);
  }
  fixture.detectChanges();
  return fixture.nativeElement as HTMLElement;
}

describe('PageHeader', () => {
  it('muestra el título, el subtítulo y el enlace de retorno', () => {
    const el = crear({ title: 'Mis citas', subtitle: 'Reservas', backLink: '/inicio' });
    expect(el.querySelector('h1')?.textContent).toBe('Mis citas');
    expect(el.textContent).toContain('Reservas');
    expect(el.querySelector('a.back')).not.toBeNull();
  });

  it('dibuja migas de pan y marca la última como la página actual', () => {
    const el = crear({
      title: 'Orden',
      migas: [{ label: 'Laboratorio', link: '/laboratorio' }, { label: 'LAB-000001' }],
    });
    const items = el.querySelectorAll('nav.migas li');
    expect(items.length).toBe(2);
    expect(items[0].querySelector('a')).not.toBeNull();
    expect(items[1].querySelector('[aria-current="page"]')?.textContent).toBe('LAB-000001');
  });

  it('no dibuja migas si no se indican', () => {
    expect(crear({ title: 'Inicio' }).querySelector('nav.migas')).toBeNull();
  });
});
