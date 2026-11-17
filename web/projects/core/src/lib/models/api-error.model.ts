export interface ErrorResponse {
  timestamp?: string;
  status: number;
  error?: string;
  message: string;
  path?: string;
  validationErrors?: Record<string, string>;
}

export interface ApiError {
  readonly status: number;
  readonly message: string;
  readonly path: string | null;
  readonly timestamp: string | null;
  readonly validationErrors: Readonly<Record<string, string>>;
}
