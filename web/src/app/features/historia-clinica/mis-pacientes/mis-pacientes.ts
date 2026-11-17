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
import { Dialog } from 'primeng/dialog';
import { Select } from 'primeng/select';
import { Tag } from 'primeng/tag';
import { Textarea } from 'primeng/textarea';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputText } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';

import { PacienteResumenResponse } from '@tusalud/app/core/models';
import { NotificationService } from '@tusalud/app/core/services/notification.service';
import { AuthService } from '@tusalud/app/core/services/auth.service';
import { MedicosStore } from '@tusalud/app/core/state/medicos.store';
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
    Dialog,
    Select,
    Tag,
    Textarea,
    IconField,
    InputIcon,
    InputText,
    TableModule,
    EmptyState,
    PageHeader,
    UserAvatar,
  ],
  templateUrl: './mis-pacientes.html',
  styleUrl: './mis-pacientes.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MisPacientes implements OnInit {
  protected readonly store = inject(ClinicaStore);
  private readonly medicos = inject(MedicosStore);
  private readonly auth = inject(AuthService);
  private readonly notifications = inject(NotificationService);
  protected readonly busqueda = signal('');

  protected readonly compartiendo = signal<PacienteResumenResponse | null>(null);
  protected readonly colegaId = signal<string | null>(null);
  protected readonly nota = signal('');
  protected readonly enviando = signal(false);

  protected readonly colegas = computed(() => {
    const compartidos = new Set((this.store.compartidos.value() ?? []).map((c) => c.medicoId));
    return this.medicos
      .medicosActivos()
      .filter((medico) => medico.usuarioId !== this.auth.user()?.id && !compartidos.has(medico.id))
      .map((medico) => ({
        value: medico.id,
        label: `${medico.nombreCompleto} · ${medico.especialidades.map((e) => e.nombre).join(', ')}`,
      }));
  });

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

  protected abrirCompartir(paciente: PacienteResumenResponse): void {
    this.medicos.load(null);
    this.store.cargarCompartidos(paciente.pacienteId);
    this.colegaId.set(null);
    this.nota.set('');
    this.compartiendo.set(paciente);
  }

  protected compartir(): void {
    const paciente = this.compartiendo();
    const medicoId = this.colegaId();
    if (!paciente || medicoId === null) {
      return;
    }
    this.enviando.set(true);
    this.store
      .compartirPaciente(paciente.pacienteId, { medicoId, nota: this.nota().trim() || null })
      .subscribe({
        next: (compartido) => {
          this.enviando.set(false);
          this.colegaId.set(null);
          this.nota.set('');
          this.notifications.success(
            `${paciente.nombreCompleto} fue compartido con ${compartido.medicoNombre}.`,
          );
        },
        error: () => this.enviando.set(false),
      });
  }

  protected dejarDeCompartir(medicoId: string): void {
    const paciente = this.compartiendo();
    if (paciente) {
      this.store.dejarDeCompartir(paciente.pacienteId, medicoId).subscribe({
        next: () => this.notifications.success('Se dejó de compartir el paciente.'),
        error: () => undefined,
      });
    }
  }
}
