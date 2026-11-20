import { clasificarCalidad } from './calidad';
import { urlDeSenalizacion } from './canal-senalizacion';

describe('calidad de la llamada', () => {
  it('clasifica según la latencia y la pérdida de paquetes', () => {
    expect(clasificarCalidad(null, 0)).toBe('sin');
    expect(clasificarCalidad(0.05, 0.001)).toBe('buena');
    expect(clasificarCalidad(0.4, 0.05)).toBe('regular');
    expect(clasificarCalidad(0.9, 0)).toBe('mala');
    expect(clasificarCalidad(0.1, 0.2)).toBe('mala');
  });

  it('arma la URL de señalización con el protocolo del sitio y el ticket codificado', () => {
    const http = { protocol: 'http:', host: 'localhost:4200' } as Location;
    const https = { protocol: 'https:', host: 'clinica.pe' } as Location;
    expect(urlDeSenalizacion('/ws/teleconsulta', 'abc_-1', http)).toBe(
      'ws://localhost:4200/ws/teleconsulta?ticket=abc_-1',
    );
    expect(urlDeSenalizacion('/ws/teleconsulta', 'a b', https)).toBe(
      'wss://clinica.pe/ws/teleconsulta?ticket=a%20b',
    );
    expect(
      urlDeSenalizacion(
        '/ws/teleconsulta',
        't',
        https,
        'https://api.tusalud.codesoftlabs.com/api/v1',
      ),
    ).toBe('wss://api.tusalud.codesoftlabs.com/ws/teleconsulta?ticket=t');
  });
});
