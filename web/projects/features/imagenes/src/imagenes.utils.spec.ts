import { OrdenImagenResponse } from '@tusalud/core';
import { resumenEstudios, tamanoLegible } from './imagenes.utils';

function orden(sobreescribir: Partial<OrdenImagenResponse> = {}): OrdenImagenResponse {
  return {
    id: '00000000-0000-4000-8000-000000000001',
    codigo: 'IMG-000001',
    fecha: '2026-11-17T10:00:00',
    estado: 'INFORMADA',
    prioridad: 'NORMAL',
    pacienteNombre: 'Juan <Pérez>',
    medicoNombre: 'Luis García',
    indicacionClinica: 'Tos "persistente"',
    estudios: [
      {
        id: '00000000-0000-4000-8000-000000000002',
        codigo: 'RX-TORAX',
        nombre: 'Radiografía de tórax',
        modalidad: 'RX',
        region: 'Tórax',
        duracionMinutos: 15,
      },
    ],
    archivos: [],
    informe: {
      id: '00000000-0000-4000-8000-000000000003',
      version: 2,
      hallazgos: 'Sin <script>alert(1)</script> lesiones',
      conclusion: 'Normal',
      firmaNombre: 'Renato Villanueva',
      firmaColegiatura: 'CMP-45210',
      fecha: '2026-11-17T12:00:00',
    },
    ...sobreescribir,
  };
}

describe('utilidades de imágenes diagnósticas', () => {
  it('muestra el tamaño de los archivos en una unidad legible', () => {
    expect(tamanoLegible(512)).toBe('512 B');
    expect(tamanoLegible(2048)).toBe('2.0 KB');
    expect(tamanoLegible(5 * 1024 * 1024)).toBe('5.0 MB');
  });

  it('resume los estudios de una orden', () => {
    expect(resumenEstudios(orden())).toBe('Radiografía de tórax');
  });
});
