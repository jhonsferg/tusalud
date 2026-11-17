export interface SesionResponse {
  id: string;
  dispositivo: string;
  ip: string;
  creadaEn: string;
  ultimoAcceso: string;
  expiraEn: string;
  actual: boolean;
}

export interface PreferenciasResponse {
  recordatoriosActivos: boolean;
  minutosAntelacionRecordatorio: number;
  alertasInicioSesion: boolean;
  tourCompletado: boolean;
}

export const ANTICIPACIONES_RECORDATORIO: { label: string; value: number }[] = [
  { label: '15 minutos antes', value: 15 },
  { label: '30 minutos antes', value: 30 },
  { label: '1 hora antes', value: 60 },
  { label: '2 horas antes', value: 120 },
  { label: '3 horas antes', value: 180 },
  { label: '6 horas antes', value: 360 },
  { label: '12 horas antes', value: 720 },
  { label: '1 día antes', value: 1440 },
  { label: '2 días antes', value: 2880 },
  { label: '3 días antes', value: 4320 },
];
