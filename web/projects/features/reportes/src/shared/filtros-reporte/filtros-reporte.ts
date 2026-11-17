import { ChangeDetectionStrategy, Component, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePicker } from 'primeng/datepicker';
import { SelectButton } from 'primeng/selectbutton';

import { AGRUPACIONES, Agrupacion, FiltroReporte, toIsoDate } from '@tusalud/core';

const DIA_MS = 86_400_000;
const MAXIMO_DIAS = 366;

@Component({
  selector: 'app-filtros-reporte',
  imports: [FormsModule, DatePicker, SelectButton],
  templateUrl: './filtros-reporte.html',
  styleUrl: './filtros-reporte.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FiltrosReporte implements OnInit {
  readonly cambio = output<FiltroReporte>();

  protected readonly agrupaciones = AGRUPACIONES.map((a) => ({ ...a }));
  protected readonly rango = signal<Date[] | null>([
    new Date(Date.now() - 29 * DIA_MS),
    new Date(),
  ]);
  protected readonly agrupacion = signal<Agrupacion>('DIA');
  protected readonly error = signal<string | null>(null);
  protected readonly maxima = new Date();

  ngOnInit(): void {
    this.emitir();
  }

  protected cambiarRango(valor: Date[] | null): void {
    this.rango.set(valor);
    const [inicio, fin] = valor ?? [];
    if (inicio && (fin || !valor?.[1])) {
      this.emitir();
    }
  }

  protected cambiarAgrupacion(valor: Agrupacion | null): void {
    if (valor) {
      this.agrupacion.set(valor);
      this.emitir();
    }
  }

  private emitir(): void {
    const [inicio, fin] = this.rango() ?? [];
    if (!inicio) {
      return;
    }
    const final = fin ?? inicio;
    const dias = Math.round((final.getTime() - inicio.getTime()) / DIA_MS) + 1;
    if (dias > MAXIMO_DIAS) {
      this.error.set('El rango no puede superar 366 días.');
      return;
    }
    this.error.set(null);
    this.cambio.emit({
      desde: toIsoDate(inicio),
      hasta: toIsoDate(final),
      agrupacion: this.agrupacion(),
    });
  }
}
