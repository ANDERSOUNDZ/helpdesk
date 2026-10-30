import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Ticket, ResumenTickets, ContadorEstado, Usuario, Comentario, Notificacion } from '../models/helpdesk.model';
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

  crearComentario(comentario: Comentario): Observable<Comentario> {
    return this.http.post<Comentario>(environment.endpoints.comentarios, comentario);
  }

  eliminarComentario(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.endpoints.comentarios}/${id}`);
  }

  getNotificaciones(): Observable<Notificacion[]> {
    return this.http.get<Notificacion[]>(environment.endpoints.notificaciones);
  }

  marcarNotificacionLeida(id: number): Observable<Notificacion> {
    return this.http.put<Notificacion>(`${environment.endpoints.notificaciones}/${id}/leida`, {});
  }
}
