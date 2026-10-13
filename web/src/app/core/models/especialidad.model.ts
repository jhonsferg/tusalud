export interface EspecialidadResponse {
  id: number;
  nombre: string;
  descripcion?: string | null;
}

export interface EspecialidadRequest {
  nombre: string;
  descripcion?: string | null;
}
