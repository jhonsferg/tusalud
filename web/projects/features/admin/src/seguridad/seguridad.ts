import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';

import {
  SEVERIDADES_SEGURIDAD,
  SeguridadOperativaStore,
  SeveridadSeguridad,
  TIPOS_EVENTO_SEGURIDAD,
  TipoEventoSeguridad,
  toIsoDate,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { SecurityNotice, StatusBadge, TonoTablero } from '@tusalud/ui/tablero';

const TONO: Readonly<Record<SeveridadSeguridad, TonoTablero>> = {
  ALTA: 'peligro',
  AVISO: 'aviso',
  INFO: 'info',
};

const TAMANO = 20;

@Component({
  selector: 'app-seguridad',
  imports: [
    DatePipe,
    FormsModule,
    Button,
    DatePicker,
    Select,
    TableModule,
    EmptyState,
    PageHeader,
    SecurityNotice,
    StatusBadge,
  ],
  templateUrl: './seguridad.html',
  styleUrl: './seguridad.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Seguridad implements OnInit {
  protected readonly store = inject(SeguridadOperativaStore);

  protected readonly tipos = TIPOS_EVENTO_SEGURIDAD.map((t) => ({
    label: t.label,
    value: t.value,
  }));
  protected readonly severidades = SEVERIDADES_SEGURIDAD.map((s) => ({
    label: s.label,
    value: s.value,
  }));
  protected readonly tipo = signal<TipoEventoSeguridad | null>(null);
  protected readonly severidad = signal<SeveridadSeguridad | null>(null);
  protected readonly rango = signal<Date[] | null>(null);
  protected readonly pagina = signal(0);
  protected readonly tamano = TAMANO;

  protected readonly eventos = computed(() => this.store.eventos.value()?.contenido ?? []);
  protected readonly total = computed(() => this.store.eventos.value()?.total ?? 0);
  protected readonly primero = computed(() => this.pagina() * TAMANO);

  ngOnInit(): void {
    this.store.cargarResumen();
    this.cargar();
  }

  protected tono(severidad: SeveridadSeguridad): TonoTablero {
    return TONO[severidad];
  }

  protected etiquetaSeveridad(severidad: SeveridadSeguridad): string {
    return this.severidades.find((s) => s.value === severidad)?.label ?? severidad;
  }

  protected cambiarFiltro(): void {
    this.pagina.set(0);
    this.cargar();
  }

  protected cambiarRango(valor: Date[] | null): void {
    this.rango.set(valor);
    const [inicio, fin] = valor ?? [];
    if (!valor || inicio) {
      void fin;
      this.cambiarFiltro();
    }
  }

  protected limpiar(): void {
    this.tipo.set(null);
    this.severidad.set(null);
    this.rango.set(null);
    this.cambiarFiltro();
  }

  protected cambiarPagina(evento: { first?: number | null }): void {
    this.pagina.set(Math.floor((evento.first ?? 0) / TAMANO));
    this.cargar();
  }

  protected cargar(): void {
    const [inicio, fin] = this.rango() ?? [];
    this.store.cargarEventos({
      tipo: this.tipo(),
      severidad: this.severidad(),
      desde: inicio ? toIsoDate(inicio) : null,
      hasta: fin ? toIsoDate(fin) : inicio ? toIsoDate(inicio) : null,
      pagina: this.pagina(),
      tamano: TAMANO,
    });
  }
}
