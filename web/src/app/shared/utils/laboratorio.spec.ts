import { rangoReferencia } from './laboratorio';

describe('rangoReferencia', () => {
  it('formatea rangos completos y abiertos', () => {
    expect(rangoReferencia(70, 100, 'mg/dL')).toBe('70 - 100 mg/dL');
    expect(rangoReferencia(40, null, 'mg/dL')).toBe('≥ 40 mg/dL');
    expect(rangoReferencia(null, 200)).toBe('≤ 200');
    expect(rangoReferencia(null, null)).toBe('—');
  });
});
