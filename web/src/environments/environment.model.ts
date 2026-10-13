export type EnvironmentName = 'local' | 'dev' | 'qa' | 'prod';

export interface Environment {
  production: boolean;
  name: EnvironmentName;
  apiUrl: string;
}
