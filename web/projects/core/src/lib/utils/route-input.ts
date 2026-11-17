export function optionalIdAttribute(value: unknown): string | undefined {
  if (typeof value !== 'string') {
    return undefined;
  }
  const id = value.trim();
  return id === '' ? undefined : id;
}
