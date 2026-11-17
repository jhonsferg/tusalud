export interface EspecialidadResponse {
  id: number;
  nombre: string;
  descripcion?: string | null;
  precioConsulta: number;
}

export interface EspecialidadRequest {
  nombre: string;
  descripcion?: string | null;
  precioConsulta: number;
}
