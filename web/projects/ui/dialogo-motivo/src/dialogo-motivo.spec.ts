import { TestBed } from '@angular/core/testing';

import { MotivoService, limpiarMotivo } from './motivo.service';

describe('MotivoService', () => {
  let servicio: MotivoService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    servicio = TestBed.inject(MotivoService);
  });

  it('limpia espacios y caracteres de control del motivo', () => {
    expect(limpiarMotivo('  Viaje\u0000   imprevisto \n urgente  ')).toBe(
      'Viaje imprevisto urgente',
    );
    expect(limpiarMotivo('   ')).toBe('');
  });

  it('emite el motivo confirmado y completa', () => {
    const valores: string[] = [];
    let completo = false;
    servicio
      .solicitar({ titulo: 'Cancelar cita' })
      .subscribe({ next: (v) => valores.push(v), complete: () => (completo = true) });

    expect(servicio.solicitud()?.titulo).toBe('Cancelar cita');
    servicio.resolver('Motivo válido');

    expect(valores).toEqual(['Motivo válido']);
    expect(completo).toBe(true);
    expect(servicio.solicitud()).toBeNull();
  });

  it('completa sin emitir cuando el usuario vuelve', () => {
    const valores: string[] = [];
    let completo = false;
    servicio
      .solicitar({ titulo: 'Anular' })
      .subscribe({ next: (v) => valores.push(v), complete: () => (completo = true) });

    servicio.resolver(null);

    expect(valores).toEqual([]);
    expect(completo).toBe(true);
  });

  it('una solicitud nueva reemplaza a la pendiente', () => {
    let primeraCompleta = false;
    servicio
      .solicitar({ titulo: 'Primera' })
      .subscribe({ complete: () => (primeraCompleta = true) });
    servicio.solicitar({ titulo: 'Segunda' }).subscribe();

    expect(primeraCompleta).toBe(true);
    expect(servicio.solicitud()?.titulo).toBe('Segunda');
  });
});
