import { Route, Routes } from '@angular/router';

import { ADMIN_ROUTES } from '@tusalud/features/admin';
import { AGENDA_ROUTES } from '@tusalud/features/agenda';
import { ALMACEN_ROUTES } from '@tusalud/features/almacen';
import { CAJA_ROUTES } from '@tusalud/features/caja';
import { CALENDARIO_ROUTES } from '@tusalud/features/calendario';
import { CITAS_ROUTES } from '@tusalud/features/citas';
import { ESPECIALIDADES_ROUTES } from '@tusalud/features/especialidades';
import { FARMACIA_ROUTES, MEDICAMENTOS_ROUTES } from '@tusalud/features/farmacia';
import { HISTORIA_CLINICA_ROUTES, PACIENTES_ROUTES } from '@tusalud/features/historia-clinica';
import { HOSPITALIZACION_ROUTES, MIS_INGRESOS_ROUTES } from '@tusalud/features/hospitalizacion';
import { ESTUDIOS_ROUTES, IMAGENES_ROUTES } from '@tusalud/features/imagenes';
import { LABORATORIO_ROUTES, RESULTADOS_ROUTES } from '@tusalud/features/laboratorio';
import { MEDICOS_ROUTES } from '@tusalud/features/medicos';
import { NOTIFICACIONES_ROUTES } from '@tusalud/features/notificaciones';
import { PAGOS_ROUTES } from '@tusalud/features/pagos';
import { PERFIL_ROUTES } from '@tusalud/features/perfil';
import { COLA_ROUTES, RECEPCION_ROUTES } from '@tusalud/features/recepcion';
import { REPORTES_ROUTES } from '@tusalud/features/reportes';
import { MI_ESPACIO_ROUTES, RRHH_ROUTES } from '@tusalud/features/rrhh';
import {
  SEGUROS_FINANZAS_ROUTES,
  SEGUROS_MEDICO_ROUTES,
  SEGUROS_PACIENTE_ROUTES,
  SEGUROS_RECEPCION_ROUTES,
} from '@tusalud/features/seguros';

const DOMINIOS: Record<string, Routes> = {
  ADMIN_ROUTES,
  AGENDA_ROUTES,
  ALMACEN_ROUTES,
  CAJA_ROUTES,
  CALENDARIO_ROUTES,
  CITAS_ROUTES,
  ESPECIALIDADES_ROUTES,
  FARMACIA_ROUTES,
  MEDICAMENTOS_ROUTES,
  HISTORIA_CLINICA_ROUTES,
  PACIENTES_ROUTES,
  HOSPITALIZACION_ROUTES,
  MIS_INGRESOS_ROUTES,
  IMAGENES_ROUTES,
  ESTUDIOS_ROUTES,
  LABORATORIO_ROUTES,
  RESULTADOS_ROUTES,
  MEDICOS_ROUTES,
  NOTIFICACIONES_ROUTES,
  PAGOS_ROUTES,
  PERFIL_ROUTES,
  COLA_ROUTES,
  RECEPCION_ROUTES,
  REPORTES_ROUTES,
  RRHH_ROUTES,
  MI_ESPACIO_ROUTES,
  SEGUROS_FINANZAS_ROUTES,
  SEGUROS_RECEPCION_ROUTES,
  SEGUROS_MEDICO_ROUTES,
  SEGUROS_PACIENTE_ROUTES,
};

function aplanar(rutas: Routes): Route[] {
  return rutas.flatMap((ruta) => [ruta, ...aplanar(ruta.children ?? [])]);
}

describe('rutas de las funcionalidades', () => {
  for (const [nombre, rutas] of Object.entries(DOMINIOS)) {
    it(`${nombre} define rutas y todas cargan su componente`, async () => {
      const todas = aplanar(rutas);
      expect(todas.length).toBeGreaterThan(0);
      for (const ruta of todas) {
        if (ruta.loadComponent) {
          const componente = await ruta.loadComponent();
          expect(componente).toBeTruthy();
        }
      }
    });
  }
});
