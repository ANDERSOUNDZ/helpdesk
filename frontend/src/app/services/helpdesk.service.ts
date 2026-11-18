import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Ticket, Categoria, ResumenTickets, ContadorEstado, Usuario, Comentario, Notificacion,
  PaginaComentarios, ContadorNotificaciones, FiltrosComentarios
} from '../models/helpdesk.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class HelpdeskService {

  constructor(private http: HttpClient) {}

  // ==========================================
  // 1. SERVICIO DE TICKETS (Ariel)
  // ==========================================
  getTickets(): Observable<Ticket[]> {
    return this.http.get<Ticket[]>(environment.endpoints.tickets);
  }

  crearTicket(ticket: Ticket): Observable<Ticket> {
    return this.http.post<Ticket>(environment.endpoints.tickets, ticket);
  }

  actualizarTicket(id: number, ticket: Partial<Ticket>): Observable<Ticket> {
    return this.http.put<Ticket>(`${environment.endpoints.tickets}/${id}`, ticket);
  }

  asignarTicket(id: number, tecnicoId: number): Observable<Ticket> {
    return this.http.put<Ticket>(`${environment.endpoints.tickets}/${id}/asignar/${tecnicoId}`, {});
  }

  resolverTicket(id: number): Observable<Ticket> {
    return this.http.put<Ticket>(`${environment.endpoints.tickets}/${id}/resolver`, {});
  }

  reabrirTicket(id: number): Observable<Ticket> {
    return this.http.put<Ticket>(`${environment.endpoints.tickets}/${id}/reabrir`, {});
  }

  eliminarTicket(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.endpoints.tickets}/${id}`);
  }

  // Categorias de los tickets (las gestiona el administrador; todos pueden listarlas)
  getCategorias(): Observable<Categoria[]> {
    return this.http.get<Categoria[]>('/categorias');
  }

  crearCategoria(categoria: Categoria): Observable<Categoria> {
    return this.http.post<Categoria>('/categorias', categoria);
  }

  actualizarCategoria(id: number, categoria: Partial<Categoria>): Observable<Categoria> {
    return this.http.put<Categoria>(`/categorias/${id}`, categoria);
  }

  eliminarCategoria(id: number): Observable<void> {
    return this.http.delete<void>(`/categorias/${id}`);
  }

  // ==========================================
  // 2. SERVICIO DE ESTADÍSTICAS (Miguel Chanchay)
  // ==========================================
  getResumen(): Observable<ResumenTickets> {
    return this.http.get<ResumenTickets>(environment.endpoints.estadisticas);
  }

  getContadoresEstados(): Observable<ContadorEstado[]> {
    return this.http.get<ContadorEstado[]>('/estadisticas/estados');
  }

  ajustarContador(estado: string, cantidad: number): Observable<ContadorEstado> {
    return this.http.put<ContadorEstado>(`/estadisticas/estados/${estado}?cantidad=${cantidad}`, {});
  }

  resetearEstadisticas(): Observable<void> {
    return this.http.delete<void>('/estadisticas/reset');
  }

  // ==========================================
  // 3. SERVICIO DE IDENTIDAD (Catálogo)
  // ==========================================
  getUsuarios(): Observable<Usuario[]> {
    return this.http.get<Usuario[]>(environment.endpoints.usuarios);
  }

  crearUsuario(usuario: Usuario): Observable<Usuario> {
    return this.http.post<Usuario>(environment.endpoints.usuarios, usuario);
  }

  actualizarUsuario(id: number, usuario: Usuario): Observable<Usuario> {
    return this.http.put<Usuario>(`${environment.endpoints.usuarios}/${id}`, usuario);
  }

  eliminarUsuario(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.endpoints.usuarios}/${id}`);
  }

  // ==========================================
  // 4. SERVICIO DE COMUNICACIÓN (Gabriel)
  // ==========================================
  getComentarios(ticketId?: number): Observable<Comentario[]> {
    const url = ticketId ? `${environment.endpoints.comentarios}?ticketId=${ticketId}` : environment.endpoints.comentarios;
    return this.http.get<Comentario[]>(url);
  }

  getComentariosPaginados(filtros: FiltrosComentarios): Observable<PaginaComentarios<Comentario>> {
    const params: Record<string, string | number> = {
      page: filtros.page ?? 0,
      size: filtros.size ?? 10,
      sort: filtros.sort ?? 'creadoEn,desc'
    };
    if (filtros.ticketId) params['ticketId'] = filtros.ticketId;
    if (filtros.ticketIds?.length) params['ticketIds'] = filtros.ticketIds.join(',');
    if (filtros.q?.trim()) params['q'] = filtros.q.trim();
    return this.http.get<PaginaComentarios<Comentario>>(
      `${environment.endpoints.comentarios}/paginados`, { params });
  }

  crearComentario(comentario: Comentario): Observable<Comentario> {
    return this.http.post<Comentario>(environment.endpoints.comentarios, comentario);
  }

  eliminarComentario(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.endpoints.comentarios}/${id}`);
  }

  getNotificaciones(usuarioId?: number): Observable<Notificacion[]> {
    const params: Record<string, string | number> = {};
    if (usuarioId) params['usuarioId'] = usuarioId;
    return this.http.get<Notificacion[]>(environment.endpoints.notificaciones, { params });
  }

  getContadorNotificaciones(usuarioId: number): Observable<ContadorNotificaciones> {
    return this.http.get<ContadorNotificaciones>(
      `${environment.endpoints.notificaciones}/contador`, { params: { usuarioId } });
  }

  marcarTodasNotificacionesLeidas(usuarioId: number): Observable<void> {
    return this.http.put<void>(
      `${environment.endpoints.notificaciones}/leidas`, {}, { params: { usuarioId } });
  }

  marcarNotificacionLeida(id: number): Observable<Notificacion> {
    return this.http.put<Notificacion>(`${environment.endpoints.notificaciones}/${id}/leida`, {});
  }
}
