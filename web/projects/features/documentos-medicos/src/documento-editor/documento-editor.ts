import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  effect,
  inject,
  input,
  signal,
  untracked,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Button } from 'primeng/button';
import { Checkbox } from 'primeng/checkbox';
import { DatePicker } from 'primeng/datepicker';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';
import { Textarea } from 'primeng/textarea';

import {
  AtencionResponse,
  ClinicaApiService,
  DocumentoMedicoDetalle,
  DocumentosMedicosStore,
  ETIQUETA_TIPO_DOCUMENTO_MEDICO,
  NotificationService,
  PacienteMostradorResponse,
  TIPOS_DOCUMENTO_MEDICO,
  TipoDocumentoMedico,
} from '@tusalud/core';
import { PacienteBuscador } from '@tusalud/ui/paciente-buscador';
import { PageHeader } from '@tusalud/ui/page-header';
import { SecurityNotice } from '@tusalud/ui/tablero';
import {
  CAMPOS_DOCUMENTO,
  contenidoDesdeValores,
  faltantes,
  fechaIso,
  valoresIniciales,
} from '../documentos-medicos.utils';

@Component({
  selector: 'app-documento-editor',
  imports: [
    FormsModule,
    Button,
    Checkbox,
    DatePicker,
    InputNumber,
    InputText,
    Message,
    Select,
    Textarea,
    PacienteBuscador,
    PageHeader,
    SecurityNotice,
  ],
  template: `
    <app-page-header
      [title]="esEdicion() ? 'Editar borrador' : 'Nuevo documento médico'"
      subtitle="El documento queda como borrador hasta que lo emitas. Una vez emitido ya no se modifica."
      icon="pi-file-edit"
      backLink="/documentos"
      backLabel="Volver a documentos"
    />

    <form class="panel formulario" (submit)="$event.preventDefault(); guardar()" novalidate>
      @if (error(); as mensaje) {
        <p-message severity="error" styleClass="w-full">{{ mensaje }}</p-message>
      }

      @if (!esEdicion()) {
        <div class="fila">
          <label for="doc-tipo">Tipo de documento</label>
          <p-select
            inputId="doc-tipo"
            [options]="tipos"
            optionLabel="label"
            optionValue="value"
            [ngModel]="tipo()"
            (ngModelChange)="cambiarTipo($event)"
            [ngModelOptions]="{ standalone: true }"
            appendTo="body"
            styleClass="w-full"
          />
        </div>

        @if (pacienteIdFijo()) {
          <div class="fila">
            <span class="etiqueta">Paciente</span>
            <strong class="sensible">{{ nombrePaciente() || 'Paciente seleccionado' }}</strong>
          </div>
        } @else {
          <div class="fila">
            <label for="doc-paciente">Paciente</label>
            <app-paciente-buscador inputId="doc-paciente" [(paciente)]="paciente" />
          </div>
        }
      } @else {
        <div class="fila">
          <span class="etiqueta">Paciente</span>
          <strong class="sensible">{{ nombrePaciente() }}</strong>
        </div>
      }

      @if (
        tipo() !== 'EPICRISIS' &&
        tipo() !== 'CONTRARREFERENCIA' &&
        atenciones().length > 0 &&
        !esEdicion()
      ) {
        <div class="fila">
          <label for="doc-atencion">Atención de origen</label>
          <p-select
            inputId="doc-atencion"
            [options]="opcionesAtencion()"
            optionLabel="label"
            optionValue="value"
            [ngModel]="atencionId()"
            (ngModelChange)="atencionId.set($event)"
            [ngModelOptions]="{ standalone: true }"
            placeholder="Sin atención asociada"
            [showClear]="true"
            appendTo="body"
            styleClass="w-full"
          />
        </div>
      }

      @for (campo of campos(); track campo.clave) {
        <div class="fila">
          <label [for]="'doc-' + campo.clave">
            {{ campo.etiqueta }}
            @if (campo.requerido) {
              <span class="req" aria-hidden="true"> *</span>
            }
          </label>
          @switch (campo.clase) {
            @case ('area') {
              <textarea
                pTextarea
                rows="3"
                class="w-full"
                autocomplete="off"
                [id]="'doc-' + campo.clave"
                [attr.maxlength]="campo.maximo"
                [ngModel]="valores()[campo.clave]"
                (ngModelChange)="poner(campo.clave, $event)"
                [ngModelOptions]="{ standalone: true }"
              ></textarea>
            }
            @case ('texto') {
              <input
                pInputText
                class="w-full"
                autocomplete="off"
                [id]="'doc-' + campo.clave"
                [attr.maxlength]="campo.maximo"
                [ngModel]="valores()[campo.clave]"
                (ngModelChange)="poner(campo.clave, $event)"
                [ngModelOptions]="{ standalone: true }"
              />
            }
            @case ('numero') {
              <p-inputnumber
                [inputId]="'doc-' + campo.clave"
                [min]="1"
                [max]="365"
                [showButtons]="true"
                [ngModel]="numero(campo.clave)"
                (ngModelChange)="poner(campo.clave, $event === null ? '' : '' + $event)"
                [ngModelOptions]="{ standalone: true }"
              />
            }
            @case ('fecha') {
              <p-datepicker
                [inputId]="'doc-' + campo.clave"
                dateFormat="dd/mm/yy"
                [showIcon]="true"
                appendTo="body"
                [ngModel]="fecha(campo.clave)"
                (ngModelChange)="ponerFecha(campo.clave, $event)"
                [ngModelOptions]="{ standalone: true }"
              />
            }
            @case ('opcion') {
              <p-select
                [inputId]="'doc-' + campo.clave"
                [options]="campo.opciones ?? []"
                optionLabel="label"
                optionValue="value"
                placeholder="Selecciona"
                appendTo="body"
                styleClass="w-full"
                [ngModel]="valores()[campo.clave]"
                (ngModelChange)="poner(campo.clave, $event)"
                [ngModelOptions]="{ standalone: true }"
              />
            }
            @case ('establecimiento') {
              <p-select
                [inputId]="'doc-' + campo.clave"
                [options]="opcionesEstablecimiento()"
                optionLabel="label"
                optionValue="value"
                placeholder="Selecciona el establecimiento"
                [filter]="true"
                appendTo="body"
                styleClass="w-full"
                [ngModel]="valores()[campo.clave]"
                (ngModelChange)="poner(campo.clave, $event)"
                [ngModelOptions]="{ standalone: true }"
              />
            }
          }
          @if (campo.ayuda) {
            <small class="text-muted">{{ campo.ayuda }}</small>
          }
        </div>
      }

      @if (admiteDiagnostico()) {
        <div class="autorizacion">
          <p-checkbox
            inputId="doc-autoriza"
            [binary]="true"
            [ngModel]="autoriza()"
            (ngModelChange)="autoriza.set($event)"
            [ngModelOptions]="{ standalone: true }"
          />
          <label for="doc-autoriza">
            El paciente autoriza incluir sus diagnósticos CIE-10 en este documento
            @if (tipo() === 'INFORME') {
              <strong> (obligatorio para emitir un informe)</strong>
            }
          </label>
        </div>
        @if (autoriza() && !atencionId() && !esEdicion()) {
          <small class="text-muted">
            Elige la atención de origen para tomar de ella los diagnósticos codificados.
          </small>
        }
      }

      <app-security-notice
        variante="auditado"
        texto="El documento se emite a nombre del médico con su colegiatura, queda numerado, auditado y verificable con un código."
      />

      <div class="acciones">
        <p-button label="Cancelar" severity="secondary" [outlined]="true" (onClick)="cancelar()" />
        <p-button
          type="submit"
          [label]="esEdicion() ? 'Guardar cambios' : 'Guardar borrador'"
          icon="pi pi-check"
          [loading]="guardando()"
        />
      </div>
    </form>
  `,
  styles: `
    .formulario {
      display: flex;
      flex-direction: column;
      gap: 1rem;
      max-width: 52rem;
    }

    .fila {
      display: flex;
      flex-direction: column;
      gap: 0.35rem;
    }

    .fila label,
    .etiqueta {
      font-size: 0.9rem;
      font-weight: 600;
    }

    .req {
      color: var(--p-red-600);
    }

    .autorizacion {
      display: flex;
      align-items: flex-start;
      gap: 0.6rem;
    }

    .acciones {
      display: flex;
      justify-content: flex-end;
      gap: 0.6rem;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DocumentoEditor implements OnInit {
  readonly id = input<string>();
  readonly tipoInicial = input<string>(undefined, { alias: 'tipo' });
  readonly pacienteIdInicial = input<string>(undefined, { alias: 'pacienteId' });
  readonly atencionIdInicial = input<string>(undefined, { alias: 'atencionId' });
  readonly ingresoIdInicial = input<string>(undefined, { alias: 'ingresoId' });
  readonly referenciaIdInicial = input<string>(undefined, { alias: 'referenciaId' });
  readonly origenIdInicial = input<string>(undefined, { alias: 'origenId' });

  protected readonly store = inject(DocumentosMedicosStore);
  private readonly clinica = inject(ClinicaApiService);
  private readonly router = inject(Router);
  private readonly notificaciones = inject(NotificationService);

  protected readonly tipos = TIPOS_DOCUMENTO_MEDICO.filter(
    (t) => t !== 'EPICRISIS' && t !== 'CONTRARREFERENCIA',
  ).map((valor) => ({ label: ETIQUETA_TIPO_DOCUMENTO_MEDICO[valor], value: valor }));

  protected readonly tipo = signal<TipoDocumentoMedico>('CERTIFICADO');
  protected readonly paciente = signal<PacienteMostradorResponse | null>(null);
  protected readonly nombrePaciente = signal('');
  protected readonly atenciones = signal<AtencionResponse[]>([]);
  protected readonly atencionId = signal<string | null>(null);
  protected readonly valores = signal<Record<string, string>>({});
  protected readonly autoriza = signal(false);
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly borrador = signal<DocumentoMedicoDetalle | null>(null);

  protected readonly esEdicion = computed(() => !!this.id());
  protected readonly pacienteIdFijo = computed(() => this.pacienteIdInicial() ?? null);
  protected readonly campos = computed(() => CAMPOS_DOCUMENTO[this.tipo()]);
  protected readonly admiteDiagnostico = computed(
    () => this.tipo() !== 'EPICRISIS' && this.tipo() !== 'CONTRARREFERENCIA',
  );
  protected readonly opcionesAtencion = computed(() =>
    this.atenciones().map((a) => ({
      label: `${a.fecha.slice(0, 10)} · ${a.especialidad}`,
      value: a.id,
    })),
  );
  protected readonly opcionesEstablecimiento = computed(() =>
    (this.store.establecimientos.value() ?? []).map((e) => ({
      label: e.nombre + (e.distrito ? ' · ' + e.distrito : ''),
      value: e.id,
    })),
  );

  constructor() {
    effect(() => {
      const paciente = this.paciente();
      if (paciente) {
        untracked(() => {
          this.nombrePaciente.set(paciente.nombreCompleto);
          this.cargarAtenciones(paciente.pacienteId);
        });
      }
    });
    effect(() => {
      const id = this.id();
      if (id) {
        untracked(() => this.cargarBorrador(id));
      }
    });
  }

  ngOnInit(): void {
    this.store.cargarEstablecimientos();
    const inicial = this.tipoInicial() as TipoDocumentoMedico | undefined;
    if (inicial && TIPOS_DOCUMENTO_MEDICO.includes(inicial)) {
      this.tipo.set(inicial);
    }
    this.valores.set(valoresIniciales(this.tipo()));
    if (this.atencionIdInicial()) {
      this.atencionId.set(this.atencionIdInicial() ?? null);
    }
    const pacienteId = this.pacienteIdInicial();
    if (pacienteId && !this.id()) {
      this.cargarAtenciones(pacienteId);
    }
  }

  protected cambiarTipo(tipo: TipoDocumentoMedico): void {
    this.tipo.set(tipo);
    this.valores.set(valoresIniciales(tipo));
    this.autoriza.set(false);
  }

  protected poner(clave: string, valor: string | null): void {
    this.valores.update((actual) => ({ ...actual, [clave]: valor ?? '' }));
  }

  protected numero(clave: string): number | null {
    const valor = this.valores()[clave];
    return valor ? Number(valor) : null;
  }

  protected fecha(clave: string): Date | null {
    const valor = this.valores()[clave];
    return valor ? new Date(valor + 'T00:00:00') : null;
  }

  protected ponerFecha(clave: string, valor: Date | null): void {
    this.poner(clave, valor ? fechaIso(valor) : '');
  }

  protected cancelar(): void {
    void this.router.navigate(['/documentos']);
  }

  protected guardar(): void {
    this.error.set(null);
    const tipo = this.tipo();
    const pendientes = faltantes(tipo, this.valores());
    if (pendientes.length > 0) {
      this.error.set('Completa: ' + pendientes.join(', ') + '.');
      return;
    }
    const contenido = contenidoDesdeValores(tipo, this.valores());
    this.guardando.set(true);
    const borrador = this.borrador();
    if (borrador) {
      this.store
        .actualizar(borrador.resumen.id, { autorizaDiagnostico: this.autoriza(), contenido })
        .subscribe({
          next: (detalle) => this.terminar(detalle, 'El borrador se guardó.'),
          error: (e: { message: string }) => this.fallar(e),
        });
      return;
    }
    const pacienteId = this.pacienteIdFijo() ?? this.paciente()?.pacienteId;
    if (!pacienteId) {
      this.guardando.set(false);
      this.error.set('Elige el paciente del documento.');
      return;
    }
    this.store
      .crear({
        pacienteId,
        tipo,
        atencionId: this.atencionId(),
        ingresoId: this.ingresoIdInicial() ?? null,
        documentoOrigenId: this.origenIdInicial() ?? null,
        referenciaId: this.referenciaIdInicial() ?? null,
        autorizaDiagnostico: this.autoriza(),
        contenido,
      })
      .subscribe({
        next: (detalle) => this.terminar(detalle, 'El borrador se creó. Revísalo y emítelo.'),
        error: (e: { message: string }) => this.fallar(e),
      });
  }

  private terminar(detalle: DocumentoMedicoDetalle, mensaje: string): void {
    this.guardando.set(false);
    this.notificaciones.success(mensaje);
    void this.router.navigate(['/documentos', detalle.resumen.id]);
  }

  private fallar(error: { message: string }): void {
    this.guardando.set(false);
    this.error.set(error.message);
  }

  private cargarBorrador(id: string): void {
    this.store.cargarDetalle(id);
    const intento = setInterval(() => {
      const detalle = this.store.detalle.value();
      if (detalle && detalle.resumen.id === id) {
        clearInterval(intento);
        this.borrador.set(detalle);
        this.tipo.set(detalle.resumen.tipo);
        this.nombrePaciente.set(detalle.resumen.pacienteNombre);
        this.valores.set(valoresIniciales(detalle.resumen.tipo, detalle.contenido));
        this.autoriza.set(detalle.autorizaDiagnostico);
      }
    }, 100);
    setTimeout(() => clearInterval(intento), 8000);
  }

  private cargarAtenciones(pacienteId: string): void {
    this.clinica.historiaDePaciente(pacienteId).subscribe({
      next: (historia) => {
        this.atenciones.set(historia.atenciones);
        this.nombrePaciente.set(historia.paciente.nombreCompleto);
      },
      error: () => this.atenciones.set([]),
    });
  }
}
