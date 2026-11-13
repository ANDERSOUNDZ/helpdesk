import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Ticket, ResumenTickets, ContadorEstado, Usuario, Comentario, Notificacion,
  EncuestaSatisfaccion, ResumenEncuestas, Adjunto, PaginaComentarios,
  ContadorNotificaciones, FiltrosComentarios, FiltrosNotificaciones
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

  getNotificaciones(filtros: FiltrosNotificaciones = {}): Observable<Notificacion[]> {
    const params: Record<string, string | number | boolean> = {};
    if (filtros.usuarioId) params['usuarioId'] = filtros.usuarioId;
    if (filtros.leida !== null && filtros.leida !== undefined) params['leida'] = filtros.leida;
    if (filtros.tipo) params['tipo'] = filtros.tipo;
    if (filtros.ticketId) params['ticketId'] = filtros.ticketId;
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

  getEncuesta(ticketId: number): Observable<EncuestaSatisfaccion> {
    return this.http.get<EncuestaSatisfaccion>(`${environment.endpoints.encuestas}/${ticketId}`);
  }

  responderEncuesta(ticketId: number, puntuacion: number, comentario: string): Observable<EncuestaSatisfaccion> {
    return this.http.post<EncuestaSatisfaccion>(
      `${environment.endpoints.encuestas}/${ticketId}/respuesta`,
      { puntuacion, comentario: comentario.trim() || null });
  }

  getResumenEncuestas(): Observable<ResumenEncuestas> {
    return this.http.get<ResumenEncuestas>(`${environment.endpoints.encuestas}/resumen`);
  }

  subirAdjuntos(comentarioId: number, archivos: File[]): Observable<Adjunto[]> {
    const formData = new FormData();
    archivos.forEach(archivo => formData.append('archivos', archivo, archivo.name));
    return this.http.post<Adjunto[]>(
      `${environment.endpoints.comentarios}/${comentarioId}/adjuntos`, formData);
  }

  getAdjuntos(comentarioId: number): Observable<Adjunto[]> {
    return this.http.get<Adjunto[]>(
      `${environment.endpoints.comentarios}/${comentarioId}/adjuntos`);
  }

  eliminarAdjunto(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.endpoints.adjuntos}/${id}`);
  }

  getArchivo(url: string): Observable<Blob> {
    return this.http.get(url, { responseType: 'blob' });
  }
}
