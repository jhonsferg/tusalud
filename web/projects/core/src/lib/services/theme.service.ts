import { DOCUMENT, Injectable, computed, effect, inject, signal } from '@angular/core';
import { updatePrimaryPalette } from '@primeuix/themes';

import { StorageService } from './storage.service';

export type ThemeMode = 'light' | 'dark' | 'auto';
export type PaletteName = 'azul' | 'turquesa' | 'esmeralda' | 'violeta' | 'rosa';

export interface PaletteOption {
  readonly name: PaletteName;
  readonly label: string;
  readonly primitive: string;
  readonly preview: string;
}

export const PALETAS: readonly PaletteOption[] = [
  { name: 'azul', label: 'Azul clínico', primitive: 'blue', preview: '#3b82f6' },
  { name: 'turquesa', label: 'Turquesa', primitive: 'teal', preview: '#14b8a6' },
  { name: 'esmeralda', label: 'Esmeralda', primitive: 'emerald', preview: '#10b981' },
  { name: 'violeta', label: 'Violeta', primitive: 'violet', preview: '#8b5cf6' },
  { name: 'rosa', label: 'Rosa', primitive: 'rose', preview: '#f43f5e' },
];

const STORAGE_KEY = 'tusalud.theme';
const SHADES = [50, 100, 200, 300, 400, 500, 600, 700, 800, 900, 950] as const;
export const DARK_CLASS = 'app-dark';

interface StoredTheme {
  mode: ThemeMode;
  palette: PaletteName;
}

function isStoredTheme(value: unknown): value is StoredTheme {
  const theme = value as StoredTheme;
  return (
    typeof theme === 'object' &&
    theme !== null &&
    ['light', 'dark', 'auto'].includes(theme.mode) &&
    PALETAS.some((palette) => palette.name === theme.palette)
  );
}

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly document = inject(DOCUMENT);
  private readonly storage = inject(StorageService);
  private readonly saved = this.storage.read(STORAGE_KEY, isStoredTheme);

  private readonly modeState = signal<ThemeMode>(this.saved?.mode ?? 'light');
  private readonly paletteState = signal<PaletteName>(this.saved?.palette ?? 'azul');
  private readonly prefersDark = signal(this.mediaQuery()?.matches ?? false);

  readonly mode = this.modeState.asReadonly();
  readonly palette = this.paletteState.asReadonly();
  readonly isDark = computed(
    () => this.modeState() === 'dark' || (this.modeState() === 'auto' && this.prefersDark()),
  );

  constructor() {
    this.mediaQuery()?.addEventListener('change', (event) => this.prefersDark.set(event.matches));
    effect(() => {
      this.document.documentElement.classList.toggle(DARK_CLASS, this.isDark());
    });
    effect(() => {
      const option = PALETAS.find((item) => item.name === this.paletteState()) ?? PALETAS[0];
      updatePrimaryPalette(
        Object.fromEntries(SHADES.map((shade) => [shade, `{${option.primitive}.${shade}}`])),
      );
    });
    effect(() => {
      this.storage.write(STORAGE_KEY, { mode: this.modeState(), palette: this.paletteState() });
    });
  }

  setMode(mode: ThemeMode): void {
    this.modeState.set(mode);
  }

  setPalette(palette: PaletteName): void {
    this.paletteState.set(palette);
  }

  toggle(): void {
    this.modeState.set(this.isDark() ? 'light' : 'dark');
  }

  private mediaQuery(): MediaQueryList | null {
    return this.document.defaultView?.matchMedia?.('(prefers-color-scheme: dark)') ?? null;
  }
}
