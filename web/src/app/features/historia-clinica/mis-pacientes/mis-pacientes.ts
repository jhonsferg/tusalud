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
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputText } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';

import { ClinicaStore } from '@tusalud/app/core/state/clinica.store';
import { EmptyState } from '@tusalud/app/shared/components/empty-state/empty-state';
import { PageHeader } from '@tusalud/app/shared/components/page-header/page-header';
import { UserAvatar } from '@tusalud/app/shared/components/user-avatar/user-avatar';

@Component({
  selector: 'app-mis-pacientes',
  imports: [
    DatePipe,
    FormsModule,
    RouterLink,
    Button,
    IconField,
    InputIcon,
    InputText,
    TableModule,
    EmptyState,
    PageHeader,
    UserAvatar,
  ],
  templateUrl: './mis-pacientes.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MisPacientes implements OnInit {
  protected readonly store = inject(ClinicaStore);
  protected readonly busqueda = signal('');

  protected readonly filtrados = computed(() => {
    const termino = this.busqueda().trim().toLowerCase();
    return (this.store.pacientes.value() ?? []).filter(
      (paciente) =>
        !termino ||
        paciente.nombreCompleto.toLowerCase().includes(termino) ||
        paciente.dni.includes(termino) ||
        paciente.historialClinicoNumero.toLowerCase().includes(termino),
    );
  });

  ngOnInit(): void {
    this.store.cargarMisPacientes();
  }
}
