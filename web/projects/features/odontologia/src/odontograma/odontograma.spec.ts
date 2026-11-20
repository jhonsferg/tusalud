import { TestBed } from '@angular/core/testing';

import { Odontograma as OdontogramaDatos } from '@tusalud/core';
import { Odontograma } from './odontograma';

const DATOS: OdontogramaDatos = {
  pacienteId: 'p1',
  pacienteNombre: 'Paciente de prueba',
  edadAnios: 30,
  piezas: [
    {
      pieza: 16,
      dentadura: 'PERMANENTE',
      estado: null,
      observacion: null,
      superficies: { O: 'CARIES' },
      ultimoCambio: '2026-01-01T10:00:00',
    },
    {
      pieza: 36,
      dentadura: 'PERMANENTE',
      estado: 'AUSENTE',
      observacion: null,
      superficies: {},
      ultimoCambio: '2026-01-01T10:00:00',
    },
  ],
};

async function crear(datos: OdontogramaDatos = DATOS) {
  TestBed.resetTestingModule();
  TestBed.configureTestingModule({ imports: [Odontograma] });
  const fixture = TestBed.createComponent(Odontograma);
  fixture.componentRef.setInput('datos', datos);
  await fixture.whenStable();
  fixture.detectChanges();
  return fixture;
}

describe('Odontograma', () => {
  it('muestra las 32 piezas permanentes como botones con nombre accesible', async () => {
    const fixture = await crear();
    const botones = Array.from<HTMLButtonElement>(
      fixture.nativeElement.querySelectorAll('button.diente'),
    ).filter((b) => Number(b.dataset['pieza']) < 50);

    expect(botones).toHaveLength(32);
    expect(botones.every((b) => (b.getAttribute('aria-label') ?? '').startsWith('Pieza'))).toBe(
      true,
    );
    const p16 = fixture.nativeElement.querySelector('button[data-pieza="16"]') as HTMLButtonElement;
    expect(p16.getAttribute('aria-label')).toContain('caries');
    const p36 = fixture.nativeElement.querySelector('button[data-pieza="36"]') as HTMLButtonElement;
    expect(p36.getAttribute('aria-label')).toContain('Ausente');
  });

  it('solo una pieza participa del orden de tabulación', async () => {
    const fixture = await crear();
    const botones = Array.from<HTMLButtonElement>(
      fixture.nativeElement.querySelectorAll('button.diente'),
    );

    expect(botones.filter((b) => b.getAttribute('tabindex') === '0')).toHaveLength(1);
  });

  it('selecciona una pieza con el clic y la marca como presionada', async () => {
    const fixture = await crear();
    const boton = fixture.nativeElement.querySelector(
      'button[data-pieza="24"]',
    ) as HTMLButtonElement;

    boton.click();
    fixture.detectChanges();

    expect(fixture.componentInstance.seleccionada()).toBe(24);
    expect(boton.getAttribute('aria-pressed')).toBe('true');
    expect(boton.getAttribute('tabindex')).toBe('0');
  });

  it('las flechas mueven el foco a la pieza vecina', async () => {
    const fixture = await crear();
    const documento = fixture.nativeElement.ownerDocument as Document;
    const origen = fixture.nativeElement.querySelector(
      'button[data-pieza="16"]',
    ) as HTMLButtonElement;
    document.body.appendChild(fixture.nativeElement);
    origen.focus();

    origen.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight', bubbles: true }));
    await Promise.resolve();

    expect((documento.activeElement as HTMLElement | null)?.dataset['pieza']).toBe('15');
  });

  it('muestra la dentición temporal cuando el paciente es niño', async () => {
    const fixture = await crear({ ...DATOS, edadAnios: 6, piezas: [] });

    expect(fixture.nativeElement.querySelector('button[data-pieza="55"]')).not.toBeNull();
    const adulto = await crear();
    expect(adulto.nativeElement.querySelector('button[data-pieza="55"]')).toBeNull();
  });
});
