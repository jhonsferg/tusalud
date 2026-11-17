import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { ToggleSwitch } from 'primeng/toggleswitch';

import { AlmacenStore, NotificationService, ProveedorResponse } from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';

@Component({
  selector: 'app-proveedores',
  imports: [
    ReactiveFormsModule,
    Button,
    Dialog,
    InputText,
    Message,
    TableModule,
    Tag,
    ToggleSwitch,
    EmptyState,
    PageHeader,
  ],
  templateUrl: './proveedores.html',
  styleUrl: './proveedores.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Proveedores implements OnInit {
  protected readonly store = inject(AlmacenStore);
  private readonly notifications = inject(NotificationService);
  private readonly fb = inject(FormBuilder).nonNullable;

  protected readonly visible = signal(false);
  protected readonly editando = signal<ProveedorResponse | null>(null);
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = this.fb.group({
    ruc: ['', [Validators.required, Validators.pattern(/^\d{11}$/)]],
    razonSocial: ['', [Validators.required, Validators.maxLength(150)]],
    contacto: ['', [Validators.maxLength(100)]],
    telefono: ['', [Validators.maxLength(20)]],
    email: ['', [Validators.email]],
    direccion: ['', [Validators.maxLength(200)]],
    activo: [true],
  });

  ngOnInit(): void {
    this.store.cargarProveedores();
  }

  protected abrir(proveedor: ProveedorResponse | null): void {
    this.editando.set(proveedor);
    this.error.set(null);
    this.form.reset({
      ruc: proveedor?.ruc ?? '',
      razonSocial: proveedor?.razonSocial ?? '',
      contacto: proveedor?.contacto ?? '',
      telefono: proveedor?.telefono ?? '',
      email: proveedor?.email ?? '',
      direccion: proveedor?.direccion ?? '',
      activo: proveedor?.activo ?? true,
    });
    this.visible.set(true);
  }

  protected guardar(): void {
    this.error.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const valor = this.form.getRawValue();
    const request = {
      ruc: valor.ruc,
      razonSocial: valor.razonSocial.trim(),
      contacto: valor.contacto.trim() || null,
      telefono: valor.telefono.trim() || null,
      email: valor.email.trim() || null,
      direccion: valor.direccion.trim() || null,
      activo: valor.activo,
    };
    const actual = this.editando();
    this.guardando.set(true);
    const operacion = actual
      ? this.store.actualizarProveedor(actual.id, request)
      : this.store.crearProveedor(request);
    operacion.subscribe({
      next: (proveedor) => {
        this.guardando.set(false);
        this.visible.set(false);
        this.notifications.success(
          actual ? 'El proveedor fue actualizado.' : `${proveedor.razonSocial} fue registrado.`,
        );
      },
      error: (error: { message: string }) => {
        this.guardando.set(false);
        this.error.set(error.message);
      },
    });
  }
}
