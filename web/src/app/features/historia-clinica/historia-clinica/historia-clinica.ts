import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  signal,
  untracked,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { Select } from 'primeng/select';
import { Skeleton } from 'primeng/skeleton';
import { Tag } from 'primeng/tag';
import { Textarea } from 'primeng/textarea';

import { AuthService } from '@tusalud/app/core/services/auth.service';
import { NotificationService } from '@tusalud/app/core/services/notification.service';
import { ClinicaStore } from '@tusalud/app/core/state/clinica.store';
import { optionalNumberAttribute } from '@tusalud/app/core/utils/route-input';
import { EmptyState } from '@tusalud/app/shared/components/empty-state/empty-state';
import { FormField } from '@tusalud/app/shared/components/form-field/form-field';
import { PageHeader } from '@tusalud/app/shared/components/page-header/page-header';

const GRUPOS = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];

@Component({
  selector: 'app-historia-clinica',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    RouterLink,
    Button,
    Select,
    Skeleton,
    Tag,
    Textarea,
    EmptyState,
    FormField,
    PageHeader,
  ],
  templateUrl: './historia-clinica.html',
  styleUrl: './historia-clinica.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HistoriaClinica {
  readonly id = input(undefined, { transform: optionalNumberAttribute });

  protected readonly store = inject(ClinicaStore);
  protected readonly auth = inject(AuthService);
  private readonly notifications = inject(NotificationService);

  protected readonly editando = signal(false);
  protected readonly guardando = signal(false);
  protected readonly grupos = GRUPOS.map((grupo) => ({ label: grupo, value: grupo }));
  protected readonly esPropia = computed(() => this.id() === undefined);
  protected readonly volver = computed(() =>
    this.auth.isMedico() ? '/pacientes' : '/admin/usuarios',
  );

  protected readonly form = inject(FormBuilder).group({
    grupoSanguineo: [null as string | null],
    alergias: ['', [Validators.maxLength(1000)]],
    antecedentes: ['', [Validators.maxLength(2000)]],
  });

  constructor() {
    effect(() => {
      const id = this.id();
      untracked(() => {
        this.store.historia.reset();
        if (id === undefined) {
          this.store.cargarMiHistoria();
        } else {
          this.store.cargarHistoriaDePaciente(id);
        }
      });
    });
  }

  protected editar(): void {
    const paciente = this.store.historia.value()?.paciente;
    this.form.reset({
      grupoSanguineo: paciente?.grupoSanguineo ?? null,
      alergias: paciente?.alergias ?? '',
      antecedentes: paciente?.antecedentes ?? '',
    });
    this.editando.set(true);
  }

  protected guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const valor = this.form.getRawValue();
    this.guardando.set(true);
    this.store
      .guardarDatosMedicos({
        grupoSanguineo: valor.grupoSanguineo,
        alergias: valor.alergias?.trim() || null,
        antecedentes: valor.antecedentes?.trim() || null,
      })
      .subscribe({
        next: () => {
          this.guardando.set(false);
          this.editando.set(false);
          this.notifications.success('Tus datos médicos fueron actualizados.');
        },
        error: () => this.guardando.set(false),
      });
  }

  protected recargar(): void {
    const id = this.id();
    if (id === undefined) {
      this.store.cargarMiHistoria();
    } else {
      this.store.cargarHistoriaDePaciente(id);
    }
  }
}
