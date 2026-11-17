export interface SesionResponse {
  id: number;
  dispositivo: string;
  ip: string;
  creadaEn: string;
  ultimoAcceso: string;
  expiraEn: string;
  actual: boolean;
}

export interface PreferenciasResponse {
  recordatoriosActivos: boolean;
  horasAntelacionRecordatorio: number;
  alertasInicioSesion: boolean;
  tourCompletado: boolean;
}
