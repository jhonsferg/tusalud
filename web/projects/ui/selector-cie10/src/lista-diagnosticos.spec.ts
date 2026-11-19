import { TestBed } from '@angular/core/testing';

import { DiagnosticoResponse } from '@tusalud/core';

import { ListaDiagnosticos } from './lista-diagnosticos';

function montar(diagnosticos: DiagnosticoResponse[], descripcion: string | null) {
  const fixture = TestBed.createComponent(ListaDiagnosticos);
  fixture.componentRef.setInput('diagnosticos', diagnosticos);
  fixture.componentRef.setInput('descripcion', descripcion);
  fixture.detectChanges();
  return fixture.nativeElement as HTMLElement;
}

const PRINCIPAL: DiagnosticoResponse = {
  codigo: 'I10',
  descripcion: 'Hipertensión esencial (primaria)',
  tipo: 'DEFINITIVO',
  principal: true,
};

describe('ListaDiagnosticos', () => {
  it('muestra el código, el principal y el tipo de cada diagnóstico', () => {
    const el = montar(
      [
        PRINCIPAL,
        { codigo: 'E11.9', descripcion: 'Diabetes', tipo: 'PRESUNTIVO', principal: false },
      ],
      null,
    );

    expect(el.querySelectorAll('li').length).toBe(2);
    expect(el.textContent).toContain('I10');
    expect(el.textContent).toContain('Principal');
    expect(el.textContent).toContain('Presuntivo');
  });

  it('muestra la descripción libre cuando difiere del diagnóstico codificado', () => {
    const el = montar([PRINCIPAL], 'Hipertensión en control');

    expect(el.querySelector('.nota')?.textContent).toContain('Hipertensión en control');
  });

  it('no repite la descripción libre idéntica al principal', () => {
    const el = montar([PRINCIPAL], 'Hipertensión esencial (primaria)');

    expect(el.querySelector('.nota')).toBeNull();
  });

  it('sin diagnósticos codificados muestra solo el texto libre de atenciones anteriores', () => {
    const el = montar([], 'Faringitis aguda');

    expect(el.querySelector('ul')).toBeNull();
    expect(el.textContent).toContain('Faringitis aguda');
  });
});
