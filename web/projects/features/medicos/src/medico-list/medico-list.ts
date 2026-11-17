import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Avatar } from 'primeng/avatar';
import { Button } from 'primeng/button';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { Skeleton } from 'primeng/skeleton';
import { Tag } from 'primeng/tag';

import { AuthService, EspecialidadesStore, MedicoResponse, MedicosStore } from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';

@Component({
  selector: 'app-medico-list',
  imports: [
    CurrencyPipe,
    FormsModule,
    RouterLink,
    Avatar,
    Button,
    IconField,
    InputIcon,
    InputText,
    Select,
    Skeleton,
    Tag,
    EmptyState,
    PageHeader,
  ],
  templateUrl: './medico-list.html',
  styleUrl: './medico-list.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MedicoList implements OnInit {
  protected readonly store = inject(MedicosStore);
  protected readonly especialidades = inject(EspecialidadesStore);
  protected readonly auth = inject(AuthService);

  protected readonly busqueda = signal('');
  protected readonly esqueleto = [1, 2, 3, 4, 5, 6];

  protected readonly filtrados = computed(() => {
    const termino = this.busqueda().trim().toLowerCase();
    const base = this.auth.isAdmin() ? this.store.medicos() : this.store.medicosActivos();
    return base.filter(
      (medico) =>
        !termino ||
        medico.nombreCompleto.toLowerCase().includes(termino) ||
        medico.especialidades.some((e) => e.nombre.toLowerCase().includes(termino)) ||
        medico.colegiatura.toLowerCase().includes(termino),
    );
  });

  ngOnInit(): void {
    this.especialidades.load();
    this.store.load(this.store.filtroEspecialidad());
  }

  protected desde(medico: MedicoResponse): number {
    return Math.min(...medico.especialidades.map((especialidad) => especialidad.precioConsulta));
  }

  protected filtrarPorEspecialidad(id: string | null): void {
    this.store.load(id);
  }

  protected iniciales(nombre: string, apellido: string): string {
    return `${nombre[0] ?? ''}${apellido[0] ?? ''}`.toUpperCase();
  }

  protected recargar(): void {
    this.store.load(this.store.filtroEspecialidad());
  }
}
