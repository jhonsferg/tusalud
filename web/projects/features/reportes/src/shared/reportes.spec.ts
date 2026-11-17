import { TestBed } from '@angular/core/testing';

import { Indicador } from '@tusalud/core';
import { FiltrosReporte } from './filtros-reporte/filtros-reporte';
import { etiquetaFecha, horaDelDia, pct } from './formato';
import { KpiVariacion } from './kpi-variacion/kpi-variacion';

function montar(indicador: Indicador | null, menorEsMejor: boolean) {
  const fixture = TestBed.createComponent(KpiVariacion);
  fixture.componentRef.setInput('etiqueta', 'Citas');
  fixture.componentRef.setInput('valor', '28');
  fixture.componentRef.setInput('indicador', indicador);
  fixture.componentRef.setInput('menorEsMejor', menorEsMejor);
  fixture.detectChanges();
  return fixture.nativeElement as HTMLElement;
}

describe('KpiVariacion', () => {
  it('muestra una subida como favorable', () => {
    const el = montar({ actual: 28, anterior: 20, variacionPct: 40 }, false);
    expect(el.querySelector('.variacion')?.getAttribute('data-tono')).toBe('bien');
    expect(el.textContent).toContain('40.0 % vs periodo anterior');
  });

  it('muestra una subida de un indicador que conviene bajar como desfavorable', () => {
    const el = montar({ actual: 12, anterior: 6, variacionPct: 100 }, true);
    expect(el.querySelector('.variacion')?.getAttribute('data-tono')).toBe('mal');
  });

  it('indica que no hay comparación cuando el periodo anterior fue cero', () => {
    const el = montar({ actual: 5, anterior: 0 }, false);
    expect(el.querySelector('.variacion')?.getAttribute('data-tono')).toBe('neutro');
    expect(el.textContent).toContain('Sin comparación');
  });
});

describe('FiltrosReporte', () => {
  it('emite el rango inicial de 30 días con agrupación por día', () => {
    const fixture = TestBed.createComponent(FiltrosReporte);
    const emitidos: unknown[] = [];
    fixture.componentInstance.cambio.subscribe((filtro) => emitidos.push(filtro));
    fixture.detectChanges();
    expect(emitidos.length).toBe(1);
    const filtro = emitidos[0] as { desde: string; hasta: string; agrupacion: string };
    expect(filtro.agrupacion).toBe('DIA');
    expect(filtro.desde <= filtro.hasta).toBe(true);
  });
});

describe('formato de reportes', () => {
  it('da formato a fechas, horas y porcentajes', () => {
    expect(etiquetaFecha('2026-11-17')).toBe('17/11');
    expect(horaDelDia(7)).toBe('07:00');
    expect(pct(41.666)).toBe('41.7 %');
    expect(pct(undefined)).toBe('0.0 %');
  });
});
