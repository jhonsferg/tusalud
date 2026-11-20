export const ESTILOS_COMPRAS = `
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
    font-size: 1.4rem;
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
    grid-template-columns: repeat(auto-fit, minmax(14rem, 1fr));
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

  .aviso {
    margin-bottom: 1rem;
    padding: 0.75rem 1rem;
    border-left: 4px solid var(--p-primary-color);
  }

  .totales {
    margin-left: auto;
    width: min(20rem, 100%);
    display: grid;
    gap: 0.25rem;
  }

  .totales div {
    display: flex;
    justify-content: space-between;
  }

  .totales .total {
    font-weight: 700;
    font-size: 1.1rem;
  }

  .tabla-comparativa th,
  .tabla-comparativa td {
    text-align: right;
  }

  .tabla-comparativa th:first-child,
  .tabla-comparativa td:first-child {
    text-align: left;
  }

  .mejor {
    color: var(--p-green-700);
    font-weight: 700;
  }

  .linea {
    display: grid;
    grid-template-columns: minmax(12rem, 3fr) 7rem 8rem 2.5rem;
    gap: 0.5rem;
    align-items: end;
    margin-bottom: 0.5rem;
  }

  .linea.simple {
    grid-template-columns: minmax(12rem, 3fr) 7rem 2.5rem;
  }

  .historial {
    list-style: none;
    margin: 0;
    padding: 0;
    display: grid;
    gap: 0.5rem;
  }

  .historial li {
    display: flex;
    gap: 0.75rem;
    padding: 0.4rem 0;
    border-bottom: 1px solid var(--p-content-border-color);
    font-size: 0.9rem;
  }

  .subseccion {
    margin: 1.25rem 0 0.5rem;
    font-size: 1.05rem;
  }

  @media (max-width: 720px) {
    .dos,
    .linea,
    .linea.simple {
      grid-template-columns: 1fr;
    }
  }
`;
