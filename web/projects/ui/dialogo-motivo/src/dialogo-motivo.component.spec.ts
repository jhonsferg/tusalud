import { TestBed } from '@angular/core/testing';

import { DialogoMotivo } from './dialogo-motivo';

interface Interno {
  texto: { set(valor: string): void };
  aceptar(): void;
  valido(): boolean;
  mostrarError(): boolean;
  mensajeError(): string;
  longitud(): number;
}

function montar() {
  TestBed.configureTestingModule({});
  const fixture = TestBed.createComponent(DialogoMotivo);
  fixture.componentRef.setInput('titulo', 'Cancelar cita');
  fixture.componentRef.setInput('visible', true);
  fixture.detectChanges();
  const emitidos: string[] = [];
  fixture.componentInstance.confirmar.subscribe((valor) => emitidos.push(valor));
  return { fixture, emitidos, interno: fixture.componentInstance as unknown as Interno };
}

describe('DialogoMotivo', () => {
  it('no confirma un motivo corto y muestra el mensaje de validación', () => {
    const { emitidos, interno } = montar();

    interno.texto.set('abc');
    interno.aceptar();

    expect(emitidos).toEqual([]);
    expect(interno.mostrarError()).toBe(true);
    expect(interno.mensajeError()).toBe('Escribe al menos 5 caracteres.');
  });

  it('confirma el motivo limpio de espacios repetidos y caracteres de control', () => {
    const { emitidos, interno } = montar();

    interno.texto.set('  Viaje\u0000   imprevisto \n del paciente ');
    interno.aceptar();

    expect(emitidos).toEqual(['Viaje imprevisto del paciente']);
    expect(interno.longitud()).toBe('Viaje imprevisto del paciente'.length);
  });

  it('rechaza un motivo que supera el máximo', () => {
    const { emitidos, interno } = montar();

    interno.texto.set('x'.repeat(301));
    interno.aceptar();

    expect(emitidos).toEqual([]);
    expect(interno.valido()).toBe(false);
    expect(interno.mensajeError()).toContain('300');
  });

  it('el campo de texto desactiva el autocompletado', () => {
    const { fixture } = montar();
    const campo = document.body.querySelector('textarea[name="motivo"]');

    expect(campo?.getAttribute('autocomplete')).toBe('off');
    expect(fixture.componentInstance.minimo()).toBe(5);
  });
});
