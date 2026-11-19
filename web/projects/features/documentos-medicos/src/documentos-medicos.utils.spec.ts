import {
  CAMPOS_DOCUMENTO,
  contenidoDesdeValores,
  faltantes,
  fechaIso,
  valoresIniciales,
} from './documentos-medicos.utils';

describe('utilidades de documentos médicos', () => {
  it('define campos para los seis tipos', () => {
    expect(Object.keys(CAMPOS_DOCUMENTO).sort()).toEqual(
      ['CERTIFICADO', 'CONTRARREFERENCIA', 'DESCANSO', 'EPICRISIS', 'INFORME', 'REFERENCIA'].sort(),
    );
  });

  it('detecta los campos requeridos faltantes', () => {
    expect(faltantes('DESCANSO', valoresIniciales('DESCANSO'))).toEqual([
      'Tipo de descanso',
      'Desde',
      'Hasta',
      'Contingencia',
      'Indicación de reposo',
    ]);
  });

  it('no marca faltantes cuando el contenido está completo', () => {
    const valores = valoresIniciales('CONTRARREFERENCIA', { resumen: 'Atendido' });
    expect(faltantes('CONTRARREFERENCIA', valores)).toEqual([]);
  });

  it('construye el contenido sin campos vacíos y con números', () => {
    const valores = {
      ...valoresIniciales('CERTIFICADO'),
      motivo: '  Reposo  ',
      vigenciaDias: '7',
    };
    expect(contenidoDesdeValores('CERTIFICADO', valores)).toEqual({
      motivo: 'Reposo',
      vigenciaDias: 7,
    });
  });

  it('rellena los valores iniciales desde un contenido existente', () => {
    expect(valoresIniciales('CERTIFICADO', { motivo: 'Control', vigenciaDias: 5 })).toEqual({
      motivo: 'Control',
      hallazgos: '',
      recomendaciones: '',
      vigenciaDias: '5',
    });
  });

  it('formatea la fecha local como ISO', () => {
    expect(fechaIso(new Date(2026, 0, 5))).toBe('2026-01-05');
  });
});
