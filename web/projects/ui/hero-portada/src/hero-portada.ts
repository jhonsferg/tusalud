import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';

const FOCO_MOVIL: Readonly<Record<string, string>> = {
  paciente: '70% 35%',
  medico: '80% 30%',
  admin: '72% 35%',
  gerencia: '62% 40%',
  finanzas: '62% 30%',
  operaciones: '86% 35%',
  calidad: '62% 35%',
  rrhh: '66% 35%',
  recepcion: '72% 35%',
  caja: '76% 35%',
  enfermeria: '70% 35%',
  laboratorio: '80% 35%',
  imagenes: '80% 35%',
  farmacia: '82% 35%',
  almacen: '62% 35%',
};

const FOCO_ESCRITORIO: Readonly<Record<string, string>> = {
  paciente: '50% 42%',
  medico: '50% 40%',
  admin: '50% 38%',
  gerencia: '50% 45%',
  finanzas: '50% 36%',
  operaciones: '50% 40%',
  calidad: '50% 38%',
  rrhh: '50% 38%',
  recepcion: '50% 42%',
  caja: '50% 38%',
  enfermeria: '50% 40%',
  laboratorio: '50% 40%',
  imagenes: '50% 40%',
  farmacia: '50% 40%',
  almacen: '50% 38%',
};

@Component({
  selector: 'app-hero-portada',
  templateUrl: './hero-portada.html',
  styleUrl: './hero-portada.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HeroPortada {
  readonly app = input.required<string>();
  readonly title = input.required<string>();
  readonly subtitle = input<string>();

  protected readonly imagenDisponible = signal(true);
  protected readonly focoMovil = computed(() => FOCO_MOVIL[this.app()] ?? '70% 35%');
  protected readonly focoEscritorio = computed(() => FOCO_ESCRITORIO[this.app()] ?? '50% 40%');

  protected sinImagen(): void {
    this.imagenDisponible.set(false);
  }
}
