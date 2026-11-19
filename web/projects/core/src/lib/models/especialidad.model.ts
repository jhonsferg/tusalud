export interface EspecialidadResponse {
  id: string;
  nombre: string;
  descripcion?: string | null;
  precioConsulta: number;
  precioVirtual?: number | null;
}

export interface EspecialidadRequest {
  nombre: string;
  descripcion?: string | null;
  precioConsulta: number;
  precioVirtual?: number | null;
}
