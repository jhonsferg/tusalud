import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { Subject, debounceTime } from 'rxjs';

import { ETIQUETA_ESTADO_ORDEN, EstadoOrden } from '@tusalud/app/core/models';
import { AuthService } from '@tusalud/app/core/services/auth.service';
import { LaboratorioStore } from '@tusalud/app/core/state/laboratorio.store';
import { EmptyState } from '@tusalud/app/shared/components/empty-state/empty-state';
import { Kpi } from '@tusalud/app/shared/components/kpi/kpi';
import { PageHeader } from '@tusalud/app/shared/components/page-header/page-header';
import { ICONO_ORDEN, SEVERIDAD_ORDEN } from '@tusalud/app/shared/utils/laboratorio';

@Component({
  selector: 'app-ordenes',
  imports: [
    DatePipe,
    FormsModule,
    RouterLink,
    Button,
    IconField,
    InputIcon,
    InputText,
    Select,
    TableModule,
    Tag,
    EmptyState,
    Kpi,
    PageHeader,
  ],
  templateUrl: './ordenes.html',
  styleUrl: './ordenes.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Ordenes implements OnInit {
  protected readonly store = inject(LaboratorioStore);
  private readonly auth = inject(AuthService);

  protected readonly esPersonal = computed(() => this.auth.isLaboratorio() || this.auth.isAdmin());
  protected readonly esMedico = computed(() => this.auth.isMedico());
  protected readonly puedeAbrir = computed(() => this.auth.isLaboratorio() || this.auth.isMedico());
  protected readonly busqueda = signal('');
  protected readonly estado = signal<EstadoOrden | null>(null);
  private readonly cambio = new Subject<void>();

  protected readonly estados = (Object.keys(ETIQUETA_ESTADO_ORDEN) as EstadoOrden[]).map(
    (valor) => ({
      label: ETIQUETA_ESTADO_ORDEN[valor],
      value: valor,
    }),
  );
  protected readonly etiqueta: Readonly<Record<string, string>> = ETIQUETA_ESTADO_ORDEN;
  protected readonly severidad: Readonly<
    Record<string, 'success' | 'info' | 'warn' | 'danger' | 'secondary'>
  > = SEVERIDAD_ORDEN;
  protected readonly icono: Readonly<Record<string, string>> = ICONO_ORDEN;

  constructor() {
    this.cambio.pipe(debounceTime(300), takeUntilDestroyed()).subscribe(() => this.cargar());
  }

  ngOnInit(): void {
    if (this.esPersonal()) {
      this.store.cargarResumen();
    }
    this.cargar();
  }

  protected buscar(valor: string): void {
    this.busqueda.set(valor);
    this.cambio.next();
  }

  protected cargar(): void {
    this.store.cargarOrdenes({ estado: this.estado(), q: this.busqueda().trim() || null });
  }
}
