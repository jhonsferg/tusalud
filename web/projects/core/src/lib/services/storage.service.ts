import { DOCUMENT, Injectable, inject } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class StorageService {
  private readonly storage: Storage | null = this.resolveStorage();

  read<T>(key: string, isValid: (value: unknown) => value is T): T | null {
    const raw = this.safely(() => this.storage?.getItem(key) ?? null);
    if (!raw) {
      return null;
    }
    try {
      const parsed: unknown = JSON.parse(raw);
      if (isValid(parsed)) {
        return parsed;
      }
    } catch {
      this.remove(key);
      return null;
    }
    this.remove(key);
    return null;
  }

  write(key: string, value: unknown): void {
    this.safely(() => this.storage?.setItem(key, JSON.stringify(value)));
  }

  remove(key: string): void {
    this.safely(() => this.storage?.removeItem(key));
  }

  private resolveStorage(): Storage | null {
    return this.safely(() => inject(DOCUMENT).defaultView?.localStorage ?? null) ?? null;
  }

  private safely<R>(operation: () => R): R | undefined {
    try {
      return operation();
    } catch {
      return undefined;
    }
  }
}
