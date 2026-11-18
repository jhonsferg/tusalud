import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { BarChart } from './bar-chart';
import { BarList } from './bar-list';
import { QuickActions } from './quick-actions';
import { SectionCard } from './section-card';
import { SecurityNotice } from './security-notice';
import { dec, fechaCorta, horaCorta, saludoDelDia } from './saludo';
import { StatCard } from './stat-card';
import { StatusBadge } from './status-badge';
import { TaskList } from './task-list';
import { Timeline } from './timeline';

function crear<T>(tipo: new () => T, entradas: Record<string, unknown>) {
  TestBed.resetTestingModule();
  TestBed.configureTestingModule({ providers: [provideRouter([])] });
  const fixture = TestBed.createComponent(tipo);
  for (const [nombre, valor] of Object.entries(entradas)) {
    fixture.componentRef.setInput(nombre, valor);
  }
  fixture.detectChanges();
  return fixture.nativeElement as HTMLElement;
}

describe('ayudas de tablero', () => {
  it('saluda según la franja del día con el primer nombre', () => {
    expect(saludoDelDia('Juan Carlos Pérez', new Date(2026, 0, 1, 8))).toBe('Buenos días, Juan');
    expect(saludoDelDia('Ana', new Date(2026, 0, 1, 15))).toBe('Buenas tardes, Ana');
    expect(saludoDelDia('', new Date(2026, 0, 1, 21))).toBe('Buenas noches');
  });

  it('redondea a un decimal, da formato corto a fechas y horas', () => {
    expect(dec(13.7549)).toBe(13.8);
    expect(dec(null)).toBe(0);
    expect(fechaCorta('2026-11-05')).toContain('5');
    expect(horaCorta(null)).toBe('');
    expect(horaCorta('2026-11-05T09:05:00')).toBe('09:05');
  });
});

describe('StatCard', () => {
  it('muestra etiqueta, valor, tendencia accesible y serie', () => {
    const el = crear(StatCard, {
      etiqueta: 'Ingresos',
      valor: 'S/ 1,200',
      tendencia: 12.4,
      serie: [1, 3, 2, 5],
      detalle: 'vs. anterior',
    });
    expect(el.textContent).toContain('Ingresos');
    expect(el.textContent).toContain('S/ 1,200');
    expect(el.querySelector('.tendencia')?.getAttribute('aria-label')).toBe(
      'Aumento de 12 por ciento',
    );
    expect(el.querySelector('polyline')?.getAttribute('points')?.split(' ').length).toBe(4);
  });

  it('es un enlace cuando recibe destino y marca la tendencia negativa', () => {
    const el = crear(StatCard, {
      etiqueta: 'Espera',
      valor: 5,
      tendencia: -8,
      enlace: '/reportes',
    });
    expect(el.querySelector('a.stat')).not.toBeNull();
    expect(el.querySelector('.tendencia.baja')).not.toBeNull();
  });
});

describe('BarList y BarChart', () => {
  it('escala las barras con el mayor valor y respeta el máximo indicado', () => {
    const el = crear(BarList, {
      items: [
        { etiqueta: 'A', valor: 10 },
        { etiqueta: 'B', valor: 5, texto: '5 de 10' },
      ],
    });
    const anchos = Array.from(el.querySelectorAll<HTMLElement>('.relleno')).map(
      (barra) => barra.style.width,
    );
    expect(anchos).toEqual(['100%', '50%']);
    expect(el.textContent).toContain('5 de 10');
  });

  it('dibuja una barra por dato y ofrece una tabla para lectores de pantalla', () => {
    const el = crear(BarChart, {
      datos: [
        { etiqueta: 'lun', valor: 2 },
        { etiqueta: 'mar', valor: 4 },
        { etiqueta: 'mié', valor: 0 },
      ],
      titulo: 'Citas por día',
    });
    expect(el.querySelectorAll('rect').length).toBe(3);
    expect(el.querySelectorAll('table tbody tr').length).toBe(3);
    expect(el.querySelector('svg')?.getAttribute('aria-label')).toBe('Citas por día');
  });
});

describe('Timeline, TaskList y accesos', () => {
  it('lista los eventos con hora, estado y datos sensibles marcados', () => {
    const el = crear(Timeline, {
      items: [
        { id: '1', hora: '09:00', titulo: 'Jorge Medina', estado: 'Confirmada', sensible: true },
        { id: '2', hora: '10:00', titulo: 'Ana', detalle: 'Control' },
      ],
    });
    expect(el.querySelectorAll('li').length).toBe(2);
    expect(el.querySelector('.sensible')?.textContent).toContain('Jorge Medina');
    expect(el.textContent).toContain('Confirmada');
  });

  it('muestra la cantidad de cada tarea y su enlace', () => {
    const el = crear(TaskList, {
      items: [
        { id: 'a', titulo: 'Resultados', cantidad: 3, enlace: '/res', tono: 'info' },
        { id: 'b', titulo: 'Otro' },
      ],
    });
    expect(el.querySelector('.cantidad')?.textContent).toBe('3');
    expect(el.querySelectorAll('a').length).toBe(1);
  });

  it('presenta accesos rápidos como navegación', () => {
    const el = crear(QuickActions, {
      acciones: [{ label: 'Cobrar', icono: 'pi-dollar', enlace: '/caja/cobrar', detalle: 'Caja' }],
    });
    expect(el.querySelector('nav')?.getAttribute('aria-label')).toBe('Accesos rápidos');
    expect(el.querySelector('a')?.textContent).toContain('Cobrar');
  });
});

describe('SectionCard, StatusBadge y SecurityNotice', () => {
  it('prioriza cargando, error y vacío sobre el contenido', () => {
    expect(
      crear(SectionCard, { titulo: 'T', cargando: true }).querySelector('.esqueleto'),
    ).not.toBeNull();
    expect(crear(SectionCard, { titulo: 'T', error: 'Falló' }).textContent).toContain('Falló');
    expect(
      crear(SectionCard, { titulo: 'T', vacio: true, textoVacio: 'Nada aquí' }).textContent,
    ).toContain('Nada aquí');
  });

  it('muestra la insignia con su tono', () => {
    const el = crear(StatusBadge, { etiqueta: 'Pendiente', tono: 'aviso' });
    expect(el.querySelector('[data-tono="aviso"]')?.textContent).toContain('Pendiente');
  });

  it('avisa que el acceso queda auditado o personaliza el texto', () => {
    expect(crear(SecurityNotice, {}).textContent).toContain('queda registrado');
    expect(crear(SecurityNotice, { texto: 'Solo lectura' }).textContent).toContain('Solo lectura');
  });
});
