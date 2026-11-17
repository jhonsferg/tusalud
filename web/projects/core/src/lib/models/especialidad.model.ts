export interface EspecialidadResponse {
  id: string;
  nombre: string;
  descripcion?: string | null;
  precioConsulta: number;
}

export interface EspecialidadRequest {
  nombre: string;
  descripcion?: string | null;
  precioConsulta: number;
}
