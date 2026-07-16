// Tipos espelhando os DTOs da API (backend/src/main/java/.../dto).

export type Role =
  | 'ADMINISTRADOR'
  | 'OPERADOR'
  | 'FINANCEIRO'
  | 'VISUALIZADOR'
  | 'CLIENTE';

export interface UsuarioResumo {
  id: number;
  nome: string;
  email: string;
  role: Role;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  usuario: UsuarioResumo;
}

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  errors?: { campo: string; mensagem: string }[];
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface DashboardResumo {
  pedidosPorStatus: Record<string, number>;
  orcamentosPorStatus: Record<string, number>;
  impressorasPorStatus: Record<string, number>;
  filamentosEstoqueBaixo: number;
  itensEstoqueBaixo: number;
  clientesAtivos: number;
  impressoesEmAndamento: number;
  pedidosMesAtual: number;
  faturamentoMesAtual: number;
}
