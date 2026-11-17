import { DefaultUrlSerializer, Route, UrlSerializer, UrlTree } from '@angular/router';

export function primerosSegmentos(rutas: readonly Route[]): ReadonlySet<string> {
  const segmentos = new Set<string>();
  const recorrer = (lista: readonly Route[]): void => {
    for (const ruta of lista) {
      const primero = (ruta.path ?? '').split('/').filter(Boolean)[0];
      if (primero && primero !== '**') {
        segmentos.add(primero);
      } else if (!primero && ruta.children) {
        recorrer(ruta.children);
      }
    }
  };
  recorrer(rutas);
  return segmentos;
}

export class RutaOcultaSerializer extends DefaultUrlSerializer {
  constructor(
    private readonly prefijo: string,
    private readonly otras: ReadonlySet<string>,
  ) {
    super();
  }

  override parse(url: string): UrlTree {
    if (url !== '' && !url.startsWith('/') && !url.startsWith('?') && !url.startsWith('#')) {
      return super.parse(url);
    }
    const corte = url.search(/[?#]/);
    const ruta = corte < 0 ? url : url.slice(0, corte);
    const resto = corte < 0 ? '' : url.slice(corte);
    const primero = ruta.split('/').filter(Boolean)[0];
    if (primero === undefined) {
      return super.parse(`/${this.prefijo}${resto}`);
    }
    if (primero === this.prefijo || this.otras.has(primero)) {
      return super.parse(url);
    }
    return super.parse(`/${this.prefijo}${ruta}${resto}`);
  }

  override serialize(arbol: UrlTree): string {
    const completa = super.serialize(arbol);
    const raiz = `/${this.prefijo}`;
    if (completa === raiz) {
      return '/';
    }
    if (completa.startsWith(`${raiz}/`)) {
      return completa.slice(raiz.length);
    }
    if (completa.startsWith(`${raiz}?`) || completa.startsWith(`${raiz}#`)) {
      return `/${completa.slice(raiz.length)}`;
    }
    return completa;
  }
}

export function serializadorRutaOculta(
  prefijo: string,
  rutas: readonly Route[],
): { provide: typeof UrlSerializer; useValue: RutaOcultaSerializer } {
  const otras = new Set(primerosSegmentos(rutas));
  otras.delete(prefijo);
  return { provide: UrlSerializer, useValue: new RutaOcultaSerializer(prefijo, otras) };
}
