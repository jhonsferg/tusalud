import { REGLAS_ARCHIVO, extensionDe, formatoTamano, validarArchivo } from './archivo.model';

function archivo(nombre: string, bytes: number): File {
  return new File([new Uint8Array(bytes)], nombre);
}

describe('modelo de archivos', () => {
  it('extrae la extensión en minúsculas', () => {
    expect(extensionDe('Foto.PNG')).toBe('png');
    expect(extensionDe('sin-extension')).toBe('');
    expect(extensionDe('termina.')).toBe('');
  });

  it('muestra el tamaño en una unidad legible', () => {
    expect(formatoTamano(512)).toBe('512 B');
    expect(formatoTamano(2048)).toBe('2.0 KB');
    expect(formatoTamano(5 * 1024 * 1024)).toBe('5.0 MB');
  });

  it('acepta un archivo válido para el propósito', () => {
    expect(validarArchivo(archivo('foto.jpg', 1000), REGLAS_ARCHIVO.AVATAR)).toBeNull();
    expect(validarArchivo(archivo('toma.PDF', 1000), REGLAS_ARCHIVO.IMAGEN_DIAGNOSTICA)).toBeNull();
  });

  it('rechaza vacíos, grandes y formatos no permitidos', () => {
    expect(validarArchivo(archivo('foto.png', 0), REGLAS_ARCHIVO.AVATAR)).toContain('vacío');
    expect(
      validarArchivo(archivo('foto.png', 2 * 1024 * 1024 + 1), REGLAS_ARCHIVO.AVATAR),
    ).toContain('supera el máximo de 2.0 MB');
    expect(validarArchivo(archivo('foto.svg', 100), REGLAS_ARCHIVO.AVATAR)).toContain(
      'no tiene un formato permitido',
    );
    expect(validarArchivo(archivo('virus.exe', 100), REGLAS_ARCHIVO.DOCUMENTO_PACIENTE)).toContain(
      'formato permitido',
    );
    expect(validarArchivo(archivo('sin', 100), REGLAS_ARCHIVO.AVATAR)).toContain(
      'formato permitido',
    );
  });

  it('refleja los límites del backend por propósito', () => {
    expect(REGLAS_ARCHIVO.AVATAR.maxBytes).toBe(2 * 1024 * 1024);
    expect(REGLAS_ARCHIVO.IMAGEN_DIAGNOSTICA.maxArchivos).toBe(12);
    expect(REGLAS_ARCHIVO.PLANTILLA.extensiones).toContain('jrxml');
    expect(REGLAS_ARCHIVO.REPORTE.maxBytes).toBe(25 * 1024 * 1024);
  });
});
