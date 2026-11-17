import { Component, OnInit, OnDestroy, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDividerModule } from '@angular/material/divider';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Subject, Subscription, interval } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { HelpdeskService } from '../services/helpdesk.service';
import { AuthService } from '../auth/auth.service';
import {
  Ticket, Comentario, Notificacion, Usuario, EstadoTicket,
  ContadorNotificaciones, TipoNotificacion
} from '../models/helpdesk.model';

type SeccionBitacora = 'EN_CURSO' | 'RESUELTOS';

@Component({
  selector: 'app-comunicacion',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDividerModule,
    MatSnackBarModule
  ],
  templateUrl: './comunicacion.html',
  styleUrl: './comunicacion.scss',
})
export class Comunicacion implements OnInit, OnDestroy {
  private helpdeskService = inject(HelpdeskService);
  private snackBar = inject(MatSnackBar);
  private cdr = inject(ChangeDetectorRef);
  private autoRefreshSub?: Subscription;
  private busquedaSub?: Subscription;
  private busqueda$ = new Subject<string>();
  readonly auth = inject(AuthService);

  tickets: Ticket[] = [];
  usuarios: Usuario[] = [];
  comentarios: Comentario[] = [];
  notificaciones: Notificacion[] = [];

  contador: ContadorNotificaciones = { total: 0, noLeidas: 0 };

  usuarioActualId?: number;
  nuevoComentarioTicketId?: number;
  nuevoComentarioTexto = '';

  filtroTicketId: number | null = null;
  seccion: SeccionBitacora = 'EN_CURSO';
  busqueda = '';
  pagina = 0;
  tamanoPagina = 10;
  totalComentarios = 0;
  totalPaginas = 0;

  cargandoComentarios = false;
  enviandoComentario = false;

  ngOnInit(): void {
    this.usuarioActualId = this.leerUsuarioGuardado();
    this.cargarUsuarios();
    this.cargarTickets();
    this.cargarNotificaciones();
    this.cargarContador();

    this.busquedaSub = this.busqueda$.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(valor => {
      this.busqueda = valor;
      this.pagina = 0;
      this.cargarComentarios();
    });

    this.autoRefreshSub = interval(8000).subscribe(() => {
      this.cargarTickets();
      this.cargarNotificaciones();
      this.cargarContador();
    });
  }

  ngOnDestroy(): void {
    this.autoRefreshSub?.unsubscribe();
    this.busquedaSub?.unsubscribe();
    this.busqueda$.complete();
  }

  get usuarioActual(): Usuario | undefined {
    return this.usuarios.find(u => u.id === this.usuarioActualId);
  }

  get ticketSeleccionado(): Ticket | undefined {
    return this.tickets.find(t => t.id === this.filtroTicketId);
  }

  get ticketParaComentar(): Ticket | undefined {
    return this.tickets.find(t => t.id === this.nuevoComentarioTicketId);
  }

  get ticketsActivos(): Ticket[] {
    return this.tickets.filter(t => t.estado !== 'RESUELTO');
  }

  get ticketsResueltos(): Ticket[] {
    return this.tickets.filter(t => t.estado === 'RESUELTO');
  }

  get ticketsSeccion(): Ticket[] {
    return this.seccion === 'RESUELTOS' ? this.ticketsResueltos : this.ticketsActivos;
  }

  get idsTicketsSeccion(): number[] {
    return this.ticketsSeccion
      .map(t => t.id)
      .filter((id): id is number => id !== undefined);
  }

  leerUsuarioGuardado(): number | undefined {
    const valor = localStorage.getItem('helpdesk.usuarioActualId');
    const id = valor ? Number(valor) : NaN;
    return Number.isFinite(id) ? id : undefined;
  }

  cargarUsuarios(): void {
    this.helpdeskService.getUsuarios().subscribe({
      next: (res) => {
        this.usuarios = [...res];
        const existe = res.some(u => u.id === this.usuarioActualId);
        if (!existe) {
          const preferido = res.find(u => u.rol === 'TECNICO') ?? res[0];
          this.usuarioActualId = preferido?.id;
        }
        if (this.usuarioActualId) {
          const actual = res.find(u => u.id === this.usuarioActualId);
          localStorage.setItem('helpdesk.usuarioActualId', String(this.usuarioActualId));
          localStorage.setItem('helpdesk.usuarioActualNombre', actual?.nombre ?? '');
        }
        this.cargarNotificaciones();
        this.cargarContador();
        this.cdr.markForCheck();
      },
      error: () => console.warn('Usuarios no disponibles para la comunicacion')
    });
  }

  cargarTickets(): void {
    this.helpdeskService.getTickets().subscribe({
      next: (res) => {
        this.tickets = [...res];

        const activos = res.filter(t => t.estado !== 'RESUELTO');
        if (this.nuevoComentarioTicketId && !activos.some(t => t.id === this.nuevoComentarioTicketId)) {
          this.nuevoComentarioTicketId = undefined;
        }
        if (!this.nuevoComentarioTicketId && activos.length > 0) {
          this.nuevoComentarioTicketId = activos[0].id;
        }

        if (this.filtroTicketId && !res.some(t => t.id === this.filtroTicketId)) {
          this.filtroTicketId = null;
        }

        this.cargarComentarios();
        this.cdr.markForCheck();
      },
      error: () => console.warn('Tickets no disponibles para comentarios')
    });
  }

  cargarComentarios(): void {
    this.cargandoComentarios = true;
    const ids = this.idsTicketsSeccion;
    this.helpdeskService.getComentariosPaginados({
      ticketId: this.filtroTicketId,
      ticketIds: this.filtroTicketId ? undefined : (ids.length ? ids : [-1]),
      q: this.busqueda,
      page: this.pagina,
      size: this.tamanoPagina
    }).subscribe({
      next: (res) => {
        this.comentarios = [...res.content];
        this.totalComentarios = res.totalElements;
        this.totalPaginas = res.totalPages;
        this.pagina = res.page;
        this.cargandoComentarios = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.cargandoComentarios = false;
        this.snackBar.open('Servicio de comunicacion no disponible', 'Cerrar', { duration: 3000 });
      }
    });
  }

  cargarNotificaciones(): void {
    if (!this.usuarioActualId) {
      this.notificaciones = [];
      return;
    }
    this.helpdeskService.getNotificaciones(this.usuarioActualId).subscribe({
      next: (res) => {
        this.notificaciones = [...res].sort((a, b) => {
          const pendientes = Number(a.leida) - Number(b.leida);
          return pendientes !== 0 ? pendientes : (b.id ?? 0) - (a.id ?? 0);
        });
        this.cdr.markForCheck();
      },
      error: () => console.warn('Notificaciones no disponibles')
    });
  }

  cargarContador(): void {
    if (!this.usuarioActualId) return;
    this.helpdeskService.getContadorNotificaciones(this.usuarioActualId).subscribe({
      next: (res) => {
        this.contador = res;
        this.cdr.markForCheck();
      },
      error: () => console.warn('Contador de notificaciones no disponible')
    });
  }

  cambiarUsuario(usuarioId: number): void {
    this.usuarioActualId = usuarioId;
    const usuario = this.usuarios.find(u => u.id === usuarioId);
    localStorage.setItem('helpdesk.usuarioActualId', String(usuarioId));
    localStorage.setItem('helpdesk.usuarioActualNombre', usuario?.nombre ?? '');
    this.cargarNotificaciones();
    this.cargarContador();
  }

  cambiarSeccion(seccion: SeccionBitacora): void {
    if (this.seccion === seccion && !this.filtroTicketId) return;
    this.seccion = seccion;
    this.filtroTicketId = null;
    this.pagina = 0;
    this.cargarComentarios();
  }

  limpiarFiltroTicket(): void {
    this.filtroTicketId = null;
    this.pagina = 0;
    this.cargarComentarios();
  }

  onBusquedaChange(valor: string): void {
    this.busqueda$.next(valor);
  }

  onTamanoPaginaChange(): void {
    this.pagina = 0;
    this.cargarComentarios();
  }

  cambiarPagina(delta: number): void {
    const siguiente = this.pagina + delta;
    if (siguiente < 0 || siguiente >= this.totalPaginas) return;
    this.pagina = siguiente;
    this.cargarComentarios();
  }

  cargarDatos(): void {
    this.cargarTickets();
    this.cargarNotificaciones();
    this.cargarContador();
  }

  determinarDestinatario(ticket: Ticket | undefined, autorId: number): number | null {
    if (!ticket) return null;
    if (ticket.solicitanteId === autorId) return ticket.tecnicoId ?? null;
    if (ticket.tecnicoId === autorId) return ticket.solicitanteId;
    return this.usuarioActual?.rol === 'SOLICITANTE'
      ? (ticket.tecnicoId ?? null)
      : ticket.solicitanteId;
  }

  agregarComentario(): void {
    if (!this.nuevoComentarioTexto.trim()) {
      this.snackBar.open('El comentario no puede estar vacio', 'Cerrar', { duration: 3000 });
      return;
    }
    if (!this.nuevoComentarioTicketId || !this.usuarioActualId) {
      this.snackBar.open('Selecciona un ticket y un usuario', 'Cerrar', { duration: 3000 });
      return;
    }

    const ticket = this.ticketParaComentar;
    const nuevo: Comentario = {
      ticketId: this.nuevoComentarioTicketId,
      autorId: this.usuarioActualId,
      autorNombre: this.usuarioActual?.nombre,
      ticketTitulo: ticket?.titulo,
      texto: this.nuevoComentarioTexto.trim(),
      destinatarioId: this.determinarDestinatario(ticket, this.usuarioActualId)
    };

    this.enviandoComentario = true;
    this.helpdeskService.crearComentario(nuevo).subscribe({
      next: () => {
        this.enviandoComentario = false;
        this.nuevoComentarioTexto = '';
        this.seccion = ticket?.estado === 'RESUELTO' ? 'RESUELTOS' : 'EN_CURSO';
        this.filtroTicketId = null;
        this.pagina = 0;
        this.snackBar.open('Comentario registrado con exito', 'OK', { duration: 3000 });
        this.cargarComentarios();
        this.cargarNotificaciones();
        this.cargarContador();
      },
      error: (err) => {
        this.enviandoComentario = false;
        const detalle = err?.error?.detail ?? 'Error al registrar comentario';
        this.snackBar.open(detalle, 'Cerrar', { duration: 4000 });
      }
    });
  }

  eliminarComentario(id?: number): void {
    if (!id) return;
    this.helpdeskService.eliminarComentario(id).subscribe({
      next: () => {
        this.snackBar.open('Comentario eliminado', 'OK', { duration: 2500 });
        this.cargarComentarios();
      },
      error: () => this.snackBar.open('Error al eliminar comentario', 'Cerrar', { duration: 3000 })
    });
  }

  marcarNotificacionLeida(id?: number): void {
    if (!id) return;
    this.helpdeskService.marcarNotificacionLeida(id).subscribe({
      next: () => {
        this.cargarNotificaciones();
        this.cargarContador();
      },
      error: () => this.snackBar.open('Error al actualizar notificacion', 'Cerrar', { duration: 3000 })
    });
  }

  marcarTodasLeidas(): void {
    if (!this.usuarioActualId) return;
    this.helpdeskService.marcarTodasNotificacionesLeidas(this.usuarioActualId).subscribe({
      next: () => {
        this.snackBar.open('Notificaciones marcadas como leidas', 'OK', { duration: 2500 });
        this.cargarNotificaciones();
        this.cargarContador();
      },
      error: () => this.snackBar.open('Error al marcar notificaciones', 'Cerrar', { duration: 3000 })
    });
  }

  abrirNotificacion(n: Notificacion): void {
    if (!n.leida && n.id) {
      this.marcarNotificacionLeida(n.id);
    }
    if (!n.ticketId) return;

    const ticket = this.tickets.find(t => t.id === n.ticketId);
    this.seccion = ticket?.estado === 'RESUELTO' ? 'RESUELTOS' : 'EN_CURSO';
    this.filtroTicketId = n.ticketId;
    this.pagina = 0;
    this.cargarComentarios();
    setTimeout(() => document.getElementById('bitacora')?.scrollIntoView({
      behavior: 'smooth',
      block: 'start'
    }), 120);
  }

  getEstadoTicket(comentario: Comentario): EstadoTicket | undefined {
    return this.tickets.find(t => t.id === comentario.ticketId)?.estado;
  }

  getNombreAutor(comentario: Comentario): string {
    if (comentario.autorNombre) return comentario.autorNombre;
    const usuario = this.usuarios.find(u => u.id === comentario.autorId);
    return usuario?.nombre ?? `Usuario #${comentario.autorId}`;
  }

  getTituloTicket(comentario: Comentario): string {
    if (comentario.ticketTitulo) return comentario.ticketTitulo;
    const ticket = this.tickets.find(t => t.id === comentario.ticketId);
    return ticket?.titulo ?? `Ticket #${comentario.ticketId}`;
  }

  getNombreUsuario(usuarioId?: number | null): string {
    if (!usuarioId) return 'Sin usuario';
    return this.usuarios.find(u => u.id === usuarioId)?.nombre ?? `Usuario #${usuarioId}`;
  }

  getTituloPorTicket(ticketId?: number | null): string {
    if (!ticketId) return 'Sin ticket';
    return this.tickets.find(t => t.id === ticketId)?.titulo ?? `Ticket #${ticketId}`;
  }

  etiquetaTipo(tipo?: TipoNotificacion | null): string {
    switch (tipo) {
      case 'TICKET_ESTADO': return 'Estado';
      case 'COMENTARIO_NUEVO': return 'Comentario';
      default: return 'Sistema';
    }
  }

  iconoTipo(tipo?: TipoNotificacion | null): string {
    switch (tipo) {
      case 'TICKET_ESTADO': return 'sync';
      case 'COMENTARIO_NUEVO': return 'chat';
      default: return 'notifications';
    }
  }

  claseTipo(tipo?: TipoNotificacion | null): string {
    return `tipo-${(tipo ?? 'SISTEMA').toLowerCase()}`;
  }

  trackByComentario(_: number, c: Comentario): number | undefined {
    return c.id;
  }

  trackByNotificacion(_: number, n: Notificacion): number | undefined {
    return n.id;
  }
}
