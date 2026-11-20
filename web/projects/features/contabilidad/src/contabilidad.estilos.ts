export const ESTILOS_CONTABILIDAD = `
  :host {
    display: block;
  }

  .filtros {
    display: flex;
    flex-wrap: wrap;
    gap: 0.75rem;
    margin-bottom: 1rem;
    align-items: center;
  }

  .derecha {
    text-align: right;
    white-space: nowrap;
  }

  .acciones {
    display: flex;
    flex-wrap: wrap;
    gap: 0.5rem;
    margin-top: 1rem;
  }

  .resumen {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(11rem, 1fr));
    gap: 0.75rem;
    margin-bottom: 1rem;
  }

  .resumen > div {
    display: flex;
    flex-direction: column;
    padding: 0.8rem 1rem;
    border: 1px solid var(--p-content-border-color);
    border-radius: 8px;
    background: var(--p-content-background);
  }

  .resumen strong {
    font-size: 1.3rem;
  }

  .resumen span {
    color: var(--p-text-muted-color);
    font-size: 0.85rem;
  }

  .resumen .alerta strong {
    color: var(--p-red-600);
  }

  .ficha {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(12rem, 1fr));
    gap: 0.75rem 1.5rem;
    margin: 0 0 1rem;
  }

  .ficha dt {
    color: var(--p-text-muted-color);
    font-size: 0.8rem;
  }

  .ficha dd {
    margin: 0;
    font-weight: 600;
  }

  .formulario {
    display: grid;
    gap: 0.75rem;
  }

  .formulario label {
    display: grid;
    gap: 0.25rem;
    font-weight: 600;
    font-size: 0.9rem;
  }

  .dos {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 0.75rem;
  }

  .linea {
    display: grid;
    grid-template-columns: minmax(14rem, 3fr) 8rem 8rem minmax(8rem, 2fr) 2.5rem;
    gap: 0.5rem;
    align-items: end;
    margin-bottom: 0.5rem;
  }

  .cuadre {
    display: flex;
    justify-content: flex-end;
    gap: 1.5rem;
    margin: 0.75rem 0;
    font-weight: 600;
  }

  .cuadre .ok {
    color: var(--p-green-700);
  }

  .cuadre .mal {
    color: var(--p-red-600);
  }

  .subseccion {
    margin: 1.25rem 0 0.5rem;
    font-size: 1.05rem;
  }

  .aviso {
    margin-bottom: 1rem;
    padding: 0.75rem 1rem;
    border-left: 4px solid var(--p-primary-color);
  }

  .error {
    color: var(--p-red-600);
  }

  .negrita {
    font-weight: 700;
  }

  .agregada {
    font-weight: 700;
    background: var(--p-content-hover-background);
  }

  .columnas {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(20rem, 1fr));
    gap: 1rem;
  }

  .vista-previa {
    max-height: 22rem;
    overflow: auto;
    padding: 0.75rem;
    border: 1px solid var(--p-content-border-color);
    border-radius: 8px;
    font-family: monospace;
    font-size: 0.8rem;
    white-space: pre;
    background: var(--p-content-hover-background);
  }

  @media (max-width: 720px) {
    .dos,
    .linea {
      grid-template-columns: 1fr;
    }
  }
`;
