import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';
import { Tab, TabList, TabPanel, TabPanels, Tabs } from 'primeng/tabs';
import { Tag } from 'primeng/tag';
import { Textarea } from 'primeng/textarea';

import {
  ApiError,
  CategoriaControl,
  CategoriaEmbarazo,
  CompatibilidadLactancia,
  DescargaService,
  ImportacionCatalogo,
  InteraccionMedicamentosa,
  Medicamento,
  MedicamentosApiService,
  NotificationService,
  PrincipioActivo,
  SeveridadInteraccion,
} from '@tusalud/core';
import { PageHeader } from '@tusalud/ui/page-header';

const CONTROLES: CategoriaControl[] = ['NINGUNA', 'ANTIBIOTICO', 'PSICOTROPICO', 'ESTUPEFACIENTE'];
const EMBARAZOS: CategoriaEmbarazo[] = ['A', 'B', 'C', 'D', 'X', 'SIN_CLASIFICAR'];
const LACTANCIAS: CompatibilidadLactancia[] = [
  'COMPATIBLE',
  'PRECAUCION',
  'CONTRAINDICADO',
  'SIN_CLASIFICAR',
];
const SEVERIDADES: SeveridadInteraccion[] = ['LEVE', 'MODERADA', 'GRAVE', 'CONTRAINDICADA'];

@Component({
  selector: 'app-catalogo-medicamentos',
  imports: [
    FormsModule,
    Button,
    InputNumber,
    InputText,
    Message,
    Select,
    Tab,
    TabList,
    TabPanel,
    TabPanels,
    Tabs,
    Tag,
    Textarea,
    PageHeader,
  ],
  template: `
    <app-page-header
      title="Catálogo de medicamentos"
      subtitle="Medicamentos por denominación común, principios activos, interacciones e importación"
      icon="pi-list"
    />
    <p-message severity="warn" class="aviso"
      >Los datos de demostración del catálogo, las interacciones y las dosis máximas están por
      validar con un químico farmacéutico y con el petitorio vigente del MINSA.</p-message
    >
    @if (error()) {
      <p-message severity="error">{{ error() }}</p-message>
    }
    <p-tabs value="medicamentos">
      <p-tablist>
        <p-tab value="medicamentos">Medicamentos</p-tab>
        <p-tab value="principios">Principios activos</p-tab>
        <p-tab value="interacciones">Interacciones</p-tab>
        <p-tab value="importar">Importación CSV</p-tab>
      </p-tablist>
      <p-tabpanels>
        <p-tabpanel value="medicamentos">
          <div class="panel">
            <form class="busqueda" (submit)="$event.preventDefault(); buscar()">
              <label for="buscar-med">
                Buscar por nombre o principio activo
                <input pInputText id="buscar-med" name="q" [(ngModel)]="consulta" />
              </label>
              <p-button type="submit" label="Buscar" icon="pi pi-search" />
            </form>
            <div class="tabla">
              <table>
                <caption class="sr-only">
                  Medicamentos del catálogo
                </caption>
                <thead>
                  <tr>
                    <th scope="col">Principio activo</th>
                    <th scope="col">Presentación</th>
                    <th scope="col">Vía</th>
                    <th scope="col">Control</th>
                    <th scope="col">Producto de farmacia</th>
                    <th scope="col"><span class="sr-only">Acciones</span></th>
                  </tr>
                </thead>
                <tbody>
                  @for (m of medicamentos(); track m.id) {
                    <tr>
                      <th scope="row">{{ m.dci }}</th>
                      <td>{{ m.concentracion }} · {{ m.formaFarmaceutica }}</td>
                      <td>{{ m.viaAdministracion }}</td>
                      <td>
                        @if (m.categoriaControl !== 'NINGUNA') {
                          <p-tag [value]="m.categoriaControl" severity="warn" />
                        } @else {
                          —
                        }
                      </td>
                      <td>{{ m.productoId ? 'Vinculado' : 'Sin vincular' }}</td>
                      <td>
                        <p-button
                          label="Editar"
                          size="small"
                          [text]="true"
                          [ariaLabel]="'Editar ' + m.nombre"
                          (onClick)="editarMedicamento(m)"
                        />
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          </div>
          <form class="panel formulario" (submit)="$event.preventDefault(); guardarMedicamento()">
            <h3>{{ medicamentoId() ? 'Editar medicamento' : 'Nuevo medicamento' }}</h3>
            <div class="campos">
              <label for="med-principio">
                Principio activo
                <p-select
                  inputId="med-principio"
                  name="principio"
                  [options]="principios()"
                  optionLabel="dci"
                  optionValue="id"
                  [filter]="true"
                  [(ngModel)]="medPrincipioId"
                />
              </label>
              <label for="med-forma">
                Forma farmacéutica
                <input
                  pInputText
                  id="med-forma"
                  name="forma"
                  maxlength="60"
                  [(ngModel)]="medForma"
                />
              </label>
              <label for="med-conc">
                Concentración
                <input
                  pInputText
                  id="med-conc"
                  name="conc"
                  maxlength="60"
                  [(ngModel)]="medConcentracion"
                />
              </label>
              <label for="med-via">
                Vía de administración
                <input pInputText id="med-via" name="via" maxlength="30" [(ngModel)]="medVia" />
              </label>
              <label for="med-pres">
                Presentación
                <input
                  pInputText
                  id="med-pres"
                  name="pres"
                  maxlength="120"
                  [(ngModel)]="medPresentacion"
                />
              </label>
              <label for="med-control">
                Categoría de control
                <p-select
                  inputId="med-control"
                  name="control"
                  [options]="controles"
                  [(ngModel)]="medControl"
                />
              </label>
            </div>
            <div class="barra">
              <p-button
                type="submit"
                label="Guardar medicamento"
                icon="pi pi-check"
                [loading]="guardando()"
              />
              @if (medicamentoId()) {
                <p-button
                  label="Cancelar edición"
                  severity="secondary"
                  [text]="true"
                  (onClick)="limpiarMedicamento()"
                />
              }
            </div>
          </form>
        </p-tabpanel>

        <p-tabpanel value="principios">
          <div class="panel tabla">
            <table>
              <caption class="sr-only">
                Principios activos
              </caption>
              <thead>
                <tr>
                  <th scope="col">Denominación común</th>
                  <th scope="col">Clase</th>
                  <th scope="col">Grupo de alergia</th>
                  <th scope="col">Dosis máxima adulto</th>
                  <th scope="col">Embarazo</th>
                  <th scope="col">Lactancia</th>
                  <th scope="col"><span class="sr-only">Acciones</span></th>
                </tr>
              </thead>
              <tbody>
                @for (p of principios(); track p.id) {
                  <tr>
                    <th scope="row">{{ p.dci }}</th>
                    <td>{{ p.claseTerapeutica ?? '—' }}</td>
                    <td>{{ p.grupoAlergia ?? '—' }}</td>
                    <td>{{ p.dosisMaxAdultoMgDia ? p.dosisMaxAdultoMgDia + ' mg' : '—' }}</td>
                    <td>{{ p.categoriaEmbarazo }}</td>
                    <td>{{ p.lactancia }}</td>
                    <td>
                      <p-button
                        label="Editar"
                        size="small"
                        [text]="true"
                        [ariaLabel]="'Editar ' + p.dci"
                        (onClick)="editarPrincipio(p)"
                      />
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
          <form class="panel formulario" (submit)="$event.preventDefault(); guardarPrincipio()">
            <h3>{{ principioId() ? 'Editar principio activo' : 'Nuevo principio activo' }}</h3>
            <div class="campos">
              <label for="pa-dci">
                Denominación común
                <input pInputText id="pa-dci" name="dci" maxlength="120" [(ngModel)]="paDci" />
              </label>
              <label for="pa-clase">
                Clase terapéutica
                <input pInputText id="pa-clase" name="clase" maxlength="80" [(ngModel)]="paClase" />
              </label>
              <label for="pa-grupo">
                Grupo de alergia
                <input pInputText id="pa-grupo" name="grupo" maxlength="40" [(ngModel)]="paGrupo" />
              </label>
              <label for="pa-alias">
                Alias de alergia separados por coma
                <input
                  pInputText
                  id="pa-alias"
                  name="alias"
                  maxlength="250"
                  [(ngModel)]="paAlias"
                />
              </label>
              <label for="pa-max">
                Dosis máxima adulto en mg por día
                <p-inputnumber
                  inputId="pa-max"
                  name="max"
                  [(ngModel)]="paMaxAdulto"
                  [min]="0"
                  [useGrouping]="false"
                  [maxFractionDigits]="2"
                />
              </label>
              <label for="pa-ped">
                Dosis máxima pediátrica en mg por kg por día
                <p-inputnumber
                  inputId="pa-ped"
                  name="ped"
                  [(ngModel)]="paMaxPediatrica"
                  [min]="0"
                  [useGrouping]="false"
                  [maxFractionDigits]="3"
                />
              </label>
              <label for="pa-emb">
                Categoría en el embarazo
                <p-select
                  inputId="pa-emb"
                  name="emb"
                  [options]="embarazos"
                  [(ngModel)]="paEmbarazo"
                />
              </label>
              <label for="pa-lac">
                Lactancia
                <p-select
                  inputId="pa-lac"
                  name="lac"
                  [options]="lactancias"
                  [(ngModel)]="paLactancia"
                />
              </label>
            </div>
            <label for="pa-renal">
              Aviso por función renal
              <input pInputText id="pa-renal" name="renal" maxlength="300" [(ngModel)]="paRenal" />
            </label>
            <div class="barra">
              <p-button
                type="submit"
                label="Guardar principio activo"
                icon="pi pi-check"
                [loading]="guardando()"
              />
              @if (principioId()) {
                <p-button
                  label="Cancelar edición"
                  severity="secondary"
                  [text]="true"
                  (onClick)="limpiarPrincipio()"
                />
              }
            </div>
          </form>
        </p-tabpanel>

        <p-tabpanel value="interacciones">
          <div class="panel tabla">
            <table>
              <caption class="sr-only">
                Interacciones medicamentosas
              </caption>
              <thead>
                <tr>
                  <th scope="col">Principios activos</th>
                  <th scope="col">Severidad</th>
                  <th scope="col">Descripción</th>
                  <th scope="col"><span class="sr-only">Acciones</span></th>
                </tr>
              </thead>
              <tbody>
                @for (i of interacciones(); track i.id) {
                  <tr>
                    <th scope="row">{{ i.principioA }} + {{ i.principioB }}</th>
                    <td>
                      <p-tag
                        [value]="i.severidad"
                        [severity]="
                          i.severidad === 'CONTRAINDICADA' || i.severidad === 'GRAVE'
                            ? 'danger'
                            : i.severidad === 'MODERADA'
                              ? 'warn'
                              : 'secondary'
                        "
                      />
                    </td>
                    <td>{{ i.descripcion }}</td>
                    <td>
                      <p-button
                        label="Quitar"
                        size="small"
                        severity="danger"
                        [text]="true"
                        [ariaLabel]="
                          'Quitar la interacción entre ' + i.principioA + ' y ' + i.principioB
                        "
                        (onClick)="quitarInteraccion(i)"
                      />
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
          <form class="panel formulario" (submit)="$event.preventDefault(); guardarInteraccion()">
            <h3>Nueva interacción</h3>
            <div class="campos">
              <label for="int-a">
                Primer principio activo
                <p-select
                  inputId="int-a"
                  name="a"
                  [options]="principios()"
                  optionLabel="dci"
                  optionValue="id"
                  [filter]="true"
                  [(ngModel)]="intA"
                />
              </label>
              <label for="int-b">
                Segundo principio activo
                <p-select
                  inputId="int-b"
                  name="b"
                  [options]="principios()"
                  optionLabel="dci"
                  optionValue="id"
                  [filter]="true"
                  [(ngModel)]="intB"
                />
              </label>
              <label for="int-sev">
                Severidad
                <p-select
                  inputId="int-sev"
                  name="sev"
                  [options]="severidades"
                  [(ngModel)]="intSeveridad"
                />
              </label>
            </div>
            <label for="int-desc">
              Descripción
              <textarea
                pTextarea
                id="int-desc"
                name="desc"
                rows="2"
                maxlength="400"
                [(ngModel)]="intDescripcion"
              ></textarea>
            </label>
            <label for="int-rec">
              Recomendación
              <input
                pInputText
                id="int-rec"
                name="rec"
                maxlength="400"
                [(ngModel)]="intRecomendacion"
              />
            </label>
            <div class="barra">
              <p-button
                type="submit"
                label="Guardar interacción"
                icon="pi pi-check"
                [loading]="guardando()"
              />
            </div>
          </form>
        </p-tabpanel>

        <p-tabpanel value="importar">
          <div class="panel formulario">
            <h3>Carga masiva por CSV</h3>
            <p class="text-muted">
              El archivo se valida primero sin modificar el catálogo. Una carga repetida actualiza
              los medicamentos existentes por denominación, forma y concentración.
            </p>
            <div class="barra">
              <p-button
                label="Descargar plantilla"
                icon="pi pi-download"
                severity="secondary"
                [outlined]="true"
                (onClick)="descargarPlantilla()"
              />
            </div>
            <label for="archivo-csv">
              Archivo CSV
              <input
                id="archivo-csv"
                type="file"
                accept=".csv,text/csv"
                (change)="elegirArchivo($event)"
              />
            </label>
            <div class="barra">
              <p-button
                label="Validar"
                icon="pi pi-search"
                [outlined]="true"
                [disabled]="!archivo"
                [loading]="importando()"
                (onClick)="importar(true)"
              />
              <p-button
                label="Importar"
                icon="pi pi-upload"
                [disabled]="!archivo"
                [loading]="importando()"
                (onClick)="importar(false)"
              />
            </div>
            @if (importacion(); as r) {
              <p-message [severity]="r.errores.length ? 'warn' : 'success'" role="status">
                {{ r.creados }} nuevos y {{ r.actualizados }} actualizados, {{ r.errores.length }}
                errores.
              </p-message>
              @if (r.errores.length > 0) {
                <ul class="errores">
                  @for (e of r.errores; track $index) {
                    <li>{{ e }}</li>
                  }
                </ul>
              }
            }
          </div>
        </p-tabpanel>
      </p-tabpanels>
    </p-tabs>
  `,
  styles: `
    .sr-only {
      position: absolute;
      width: 1px;
      height: 1px;
      padding: 0;
      margin: -1px;
      overflow: hidden;
      clip: rect(0, 0, 0, 0);
      white-space: nowrap;
      border: 0;
    }
    .aviso {
      display: block;
      margin-bottom: 1rem;
    }

    .panel {
      margin-bottom: 1rem;
    }

    .tabla {
      overflow-x: auto;
    }

    table {
      width: 100%;
      border-collapse: collapse;
      font-size: 0.92rem;
    }

    th,
    td {
      text-align: left;
      padding: 0.45rem 0.6rem;
      border-bottom: 1px solid var(--p-surface-200, #e2e8f0);
    }

    .busqueda {
      display: flex;
      align-items: end;
      flex-wrap: wrap;
      gap: 0.75rem;
      margin-bottom: 1rem;
    }

    .formulario {
      display: grid;
      gap: 0.75rem;
    }

    h3 {
      margin: 0;
    }

    .campos {
      display: grid;
      gap: 0.75rem;
      grid-template-columns: repeat(auto-fit, minmax(14rem, 1fr));
    }

    label {
      display: grid;
      gap: 0.25rem;
      font-weight: 600;
    }

    textarea {
      width: 100%;
    }

    .barra {
      display: flex;
      flex-wrap: wrap;
      gap: 0.5rem;
    }

    .errores {
      margin: 0;
      padding-left: 1.2rem;
      color: #b91c1c;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CatalogoMedicamentos implements OnInit {
  private readonly api = inject(MedicamentosApiService);
  private readonly descargas = inject(DescargaService);
  private readonly notifications = inject(NotificationService);

  protected readonly controles = CONTROLES;
  protected readonly embarazos = EMBARAZOS;
  protected readonly lactancias = LACTANCIAS;
  protected readonly severidades = SEVERIDADES;

  protected readonly medicamentos = signal<Medicamento[]>([]);
  protected readonly principios = signal<PrincipioActivo[]>([]);
  protected readonly interacciones = signal<InteraccionMedicamentosa[]>([]);
  protected readonly error = signal<string | null>(null);
  protected readonly guardando = signal(false);
  protected readonly importando = signal(false);
  protected readonly importacion = signal<ImportacionCatalogo | null>(null);
  protected readonly medicamentoId = signal<string | null>(null);
  protected readonly principioId = signal<string | null>(null);

  protected consulta = '';
  protected archivo: File | null = null;

  protected medPrincipioId: string | null = null;
  protected medForma = '';
  protected medConcentracion = '';
  protected medVia = '';
  protected medPresentacion = '';
  protected medControl: CategoriaControl = 'NINGUNA';

  protected paDci = '';
  protected paClase = '';
  protected paGrupo = '';
  protected paAlias = '';
  protected paMaxAdulto: number | null = null;
  protected paMaxPediatrica: number | null = null;
  protected paEmbarazo: CategoriaEmbarazo = 'SIN_CLASIFICAR';
  protected paLactancia: CompatibilidadLactancia = 'SIN_CLASIFICAR';
  protected paRenal = '';

  protected intA: string | null = null;
  protected intB: string | null = null;
  protected intSeveridad: SeveridadInteraccion = 'MODERADA';
  protected intDescripcion = '';
  protected intRecomendacion = '';

  ngOnInit(): void {
    this.cargarTodo();
  }

  private cargarTodo(): void {
    this.buscar();
    this.api.principios().subscribe({
      next: (lista) => this.principios.set(lista),
      error: () => this.error.set('No se pudo cargar el catálogo'),
    });
    this.api.interacciones().subscribe({
      next: (lista) => this.interacciones.set(lista),
      error: () => this.error.set('No se pudieron cargar las interacciones'),
    });
  }

  protected buscar(): void {
    this.api.buscar(this.consulta).subscribe({
      next: (lista) => this.medicamentos.set(lista),
      error: () => this.error.set('No se pudo buscar en el catálogo'),
    });
  }

  protected editarMedicamento(m: Medicamento): void {
    this.medicamentoId.set(m.id);
    this.medPrincipioId = m.principioActivoId;
    this.medForma = m.formaFarmaceutica;
    this.medConcentracion = m.concentracion;
    this.medVia = m.viaAdministracion;
    this.medPresentacion = m.presentacion ?? '';
    this.medControl = m.categoriaControl;
  }

  protected limpiarMedicamento(): void {
    this.medicamentoId.set(null);
    this.medPrincipioId = null;
    this.medForma = '';
    this.medConcentracion = '';
    this.medVia = '';
    this.medPresentacion = '';
    this.medControl = 'NINGUNA';
  }

  protected guardarMedicamento(): void {
    if (
      !this.medPrincipioId ||
      !this.medForma.trim() ||
      !this.medConcentracion.trim() ||
      !this.medVia.trim()
    ) {
      this.notifications.error('Completa el principio activo, la forma, la concentración y la vía');
      return;
    }
    const request = {
      principioActivoId: this.medPrincipioId,
      formaFarmaceutica: this.medForma.trim(),
      concentracion: this.medConcentracion.trim(),
      viaAdministracion: this.medVia.trim(),
      presentacion: this.medPresentacion.trim() || null,
      categoriaControl: this.medControl,
    };
    const id = this.medicamentoId();
    this.guardando.set(true);
    (id ? this.api.actualizar(id, request) : this.api.crear(request)).subscribe({
      next: () => {
        this.guardando.set(false);
        this.notifications.success('Medicamento guardado.');
        this.limpiarMedicamento();
        this.buscar();
      },
      error: (e: ApiError) => this.fallar(e),
    });
  }

  protected editarPrincipio(p: PrincipioActivo): void {
    this.principioId.set(p.id);
    this.paDci = p.dci;
    this.paClase = p.claseTerapeutica ?? '';
    this.paGrupo = p.grupoAlergia ?? '';
    this.paAlias = p.aliasAlergia ?? '';
    this.paMaxAdulto = p.dosisMaxAdultoMgDia;
    this.paMaxPediatrica = p.dosisMaxPediatricaMgKgDia;
    this.paEmbarazo = p.categoriaEmbarazo;
    this.paLactancia = p.lactancia;
    this.paRenal = p.avisoRenal ?? '';
  }

  protected limpiarPrincipio(): void {
    this.principioId.set(null);
    this.paDci = '';
    this.paClase = '';
    this.paGrupo = '';
    this.paAlias = '';
    this.paMaxAdulto = null;
    this.paMaxPediatrica = null;
    this.paEmbarazo = 'SIN_CLASIFICAR';
    this.paLactancia = 'SIN_CLASIFICAR';
    this.paRenal = '';
  }

  protected guardarPrincipio(): void {
    if (!this.paDci.trim()) {
      this.notifications.error('La denominación común es obligatoria');
      return;
    }
    const request = {
      dci: this.paDci.trim(),
      claseTerapeutica: this.paClase.trim() || null,
      grupoAlergia: this.paGrupo.trim() || null,
      aliasAlergia: this.paAlias.trim() || null,
      dosisMaxAdultoMgDia: this.paMaxAdulto,
      dosisMaxPediatricaMgKgDia: this.paMaxPediatrica,
      categoriaEmbarazo: this.paEmbarazo,
      lactancia: this.paLactancia,
      avisoRenal: this.paRenal.trim() || null,
    };
    const id = this.principioId();
    this.guardando.set(true);
    (id ? this.api.actualizarPrincipio(id, request) : this.api.crearPrincipio(request)).subscribe({
      next: () => {
        this.guardando.set(false);
        this.notifications.success('Principio activo guardado.');
        this.limpiarPrincipio();
        this.cargarTodo();
      },
      error: (e: ApiError) => this.fallar(e),
    });
  }

  protected guardarInteraccion(): void {
    if (!this.intA || !this.intB || !this.intDescripcion.trim()) {
      this.notifications.error('Elige los dos principios activos e indica la descripción');
      return;
    }
    this.guardando.set(true);
    this.api
      .crearInteraccion({
        principioAId: this.intA,
        principioBId: this.intB,
        severidad: this.intSeveridad,
        descripcion: this.intDescripcion.trim(),
        recomendacion: this.intRecomendacion.trim() || null,
      })
      .subscribe({
        next: () => {
          this.guardando.set(false);
          this.notifications.success('Interacción guardada.');
          this.intA = null;
          this.intB = null;
          this.intDescripcion = '';
          this.intRecomendacion = '';
          this.cargarTodo();
        },
        error: (e: ApiError) => this.fallar(e),
      });
  }

  protected quitarInteraccion(i: InteraccionMedicamentosa): void {
    this.api.eliminarInteraccion(i.id).subscribe({
      next: () => {
        this.notifications.success('Interacción eliminada.');
        this.cargarTodo();
      },
      error: (e: ApiError) => this.fallar(e),
    });
  }

  protected descargarPlantilla(): void {
    this.api.plantillaCsv().subscribe({ next: (archivo) => this.descargas.guardar(archivo) });
  }

  protected elegirArchivo(evento: Event): void {
    const entrada = evento.target as HTMLInputElement;
    this.archivo = entrada.files?.item(0) ?? null;
    this.importacion.set(null);
  }

  protected importar(soloValidar: boolean): void {
    if (!this.archivo) {
      return;
    }
    this.importando.set(true);
    this.api.importar(this.archivo, soloValidar).subscribe({
      next: (resultado) => {
        this.importando.set(false);
        this.importacion.set(resultado);
        if (!soloValidar) {
          this.cargarTodo();
        }
      },
      error: (e: ApiError) => {
        this.importando.set(false);
        this.notifications.error(e.message ?? 'No se pudo procesar el archivo');
      },
    });
  }

  private fallar(e: ApiError): void {
    this.guardando.set(false);
    this.notifications.error(e.message ?? 'No se pudo guardar');
  }
}
