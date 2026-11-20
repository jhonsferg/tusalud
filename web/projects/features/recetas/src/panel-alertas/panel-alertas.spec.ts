import { TestBed } from '@angular/core/testing';

import { AlertaReceta } from '@tusalud/core';
import { PanelAlertas } from './panel-alertas';

function alerta(
  clave: string,
  severidad: AlertaReceta['severidad'],
  minimo: number,
  tipo: AlertaReceta['tipo'] = 'INTERACCION',
): AlertaReceta {
  return {
    clave,
    tipo,
    severidad,
    mensaje: `Mensaje ${clave}`,
    recomendacion: `Recomendación ${clave}`,
    medicamento: 'Warfarina + Ibuprofeno',
    requiereJustificacion: minimo > 0,
    minimoJustificacion: minimo,
  };
}

function montar(
  alertas: AlertaReceta[],
  justificaciones: Record<string, string> = {},
  cargando = false,
) {
  TestBed.configureTestingModule({});
  const fixture = TestBed.createComponent(PanelAlertas);
  fixture.componentRef.setInput('alertas', alertas);
  fixture.componentRef.setInput('justificaciones', justificaciones);
  fixture.componentRef.setInput('cargando', cargando);
  fixture.detectChanges();
  const cambios: { clave: string; texto: string }[] = [];
  fixture.componentInstance.cambio.subscribe((c) => cambios.push(c));
  return { fixture, cambios, host: fixture.nativeElement as HTMLElement };
}

describe('PanelAlertas', () => {
  it('indica que no hay alertas cuando la lista está vacía', () => {
    const { host } = montar([]);

    expect(host.textContent).toContain('Sin alertas para esta prescripción');
    expect(host.querySelectorAll('li.alerta')).toHaveLength(0);
  });

  it('avisa mientras revisa las alertas', () => {
    const { host } = montar([], {}, true);

    expect(host.querySelector('[role="status"]')?.textContent).toContain('Revisando');
  });

  it('muestra primero la más severa y pide justificación solo en graves y contraindicadas', () => {
    const { host } = montar([
      alerta('leve', 'INFORMATIVA', 0),
      alerta('grave', 'GRAVE', 10),
      alerta('contra', 'CONTRAINDICADA', 30),
    ]);

    const items = Array.from(host.querySelectorAll('li.alerta'));
    expect(items.map((i) => i.getAttribute('data-severidad'))).toEqual([
      'CONTRAINDICADA',
      'GRAVE',
      'INFORMATIVA',
    ]);
    expect(host.querySelectorAll('textarea')).toHaveLength(2);
    expect(host.textContent).toContain('mínimo 30 caracteres');
    expect(host.textContent).toContain('mínimo 10 caracteres');
  });

  it('muestra el contador y marca el error mientras falten caracteres', () => {
    const { host } = montar([alerta('grave', 'GRAVE', 10)], { grave: 'corto' });

    expect(host.querySelector('small')?.textContent?.replace(/\s+/g, ' ').trim()).toBe('5 de 10');
    expect(host.querySelector('small')?.classList.contains('error')).toBe(true);
    expect(host.querySelector('textarea')?.getAttribute('aria-invalid')).toBe('true');
  });

  it('deja de marcar el error cuando la justificación alcanza el mínimo', () => {
    const { host } = montar([alerta('grave', 'GRAVE', 10)], {
      grave: 'Control del INR semanal',
    });

    expect(host.querySelector('small')?.classList.contains('error')).toBe(false);
    expect(host.querySelector('textarea')?.getAttribute('aria-invalid')).toBe('false');
  });

  it('emite el cambio de la justificación con la clave de su alerta', () => {
    const { host, fixture, cambios } = montar([alerta('grave', 'GRAVE', 10)]);
    const caja = host.querySelector('textarea') as HTMLTextAreaElement;

    caja.value = 'Paciente con prótesis valvular';
    caja.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(cambios).toEqual([{ clave: 'grave', texto: 'Paciente con prótesis valvular' }]);
  });
});
