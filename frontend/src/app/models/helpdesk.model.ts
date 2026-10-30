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
  rol: 'USUARIO' | 'TECNICO' | 'ADMINISTRADOR';
}

export interface Comentario {
  id?: number;
  ticketId: number;
  autorId: number;
  texto: string;
}

export interface Notificacion {
  id?: number;
  usuarioId: number;
  mensaje: string;
  leida: boolean;
}
