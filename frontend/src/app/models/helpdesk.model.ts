export type Prioridad = 'BAJA' | 'MEDIA' | 'ALTA';
export type EstadoTicket = 'ABIERTO' | 'EN_PROGRESO' | 'RESUELTO';

export interface Ticket {
  id?: number;
  titulo: string;
  descripcion?: string;
  prioridad: Prioridad;
  estado?: EstadoTicket;
  solicitanteId: number;
  tecnicoId?: number | null;
}

export interface Categoria {
  id?: number;
  nombre: string;
  descripcion?: string;
}

export interface ResumenTickets {
  abiertos: number;
  enProgreso: number;
  resueltos: number;
  total: number;
}

export interface ContadorEstado {
  estado: string;
  cantidad: number;
}

export interface Usuario {
  id?: number;
  nombre: string;
  rol: 'SOLICITANTE' | 'TECNICO' | 'ADMINISTRADOR';
  username?: string;
  clave?: string;
  activo?: boolean;
  fechaIngreso?: string | null;
}

export interface Comentario {
  id?: number;
  ticketId: number;
  autorId: number;
  autorNombre?: string | null;
  ticketTitulo?: string | null;
  texto: string;
  creadoEn?: string | null;
  destinatarioId?: number | null;
}

export type TipoNotificacion =
  | 'TICKET_ESTADO'
  | 'COMENTARIO_NUEVO'
  | 'SISTEMA';

export interface Notificacion {
  id?: number;
  usuarioId: number;
  mensaje: string;
  leida: boolean;
  tipo?: TipoNotificacion | null;
  ticketId?: number | null;
  referenciaId?: number | null;
  creadaEn?: string | null;
  leidaEn?: string | null;
}

export interface PaginaComentarios<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface ContadorNotificaciones {
  total: number;
  noLeidas: number;
}

export interface FiltrosComentarios {
  ticketId?: number | null;
  ticketIds?: number[];
  q?: string;
  page?: number;
  size?: number;
  sort?: string;
}
