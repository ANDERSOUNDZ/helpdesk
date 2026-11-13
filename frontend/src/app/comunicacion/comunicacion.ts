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
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Subject, Subscription, forkJoin, interval, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { HelpdeskService } from '../services/helpdesk.service';
import { AuthService } from '../auth/auth.service';
import {
  Ticket, Comentario, Notificacion, Usuario, EncuestaSatisfaccion,
  ResumenEncuestas, Adjunto, ContadorNotificaciones, TipoNotificacion
} from '../models/helpdesk.model';

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
    MatChipsModule,
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
  adjuntosPorComentario: Record<number, Adjunto[]> = {};

  contador: ContadorNotificaciones = { total: 0, noLeidas: 0 };
  resumenEncuestas: ResumenEncuestas = {
    totalEncuestas: 0,
    respondidas: 0,
    pendientes: 0,
    promedio: 0,
    distribucion: { 1: 0, 2: 0, 3: 0, 4: 0, 5: 0 }
  };
  encuestaSeleccionada?: EncuestaSatisfaccion;
  encuestaPuntuacion = 0;
  encuestaComentario = '';

  usuarioActualId?: number;
  nuevoComentarioTicketId?: number;
  nuevoComentarioTexto = '';
  archivosSeleccionados: File[] = [];

  filtroTicketId: number | null = null;
  busqueda = '';
  pagina = 0;
  tamanoPagina = 10;
  totalComentarios = 0;
  totalPaginas = 0;

  filtroNotifTipo: TipoNotificacion | '' = '';
  filtroNotifTicketId: number | null = null;
  filtroNotifLeida: '' | 'true' | 'false' = '';

  cargandoComentarios = false;
  enviandoComentario = false;
  enviandoEncuesta = false;
  estrellas = [1, 2, 3, 4, 5];

  ngOnInit(): void {
    this.usuarioActualId = this.leerUsuarioGuardado();
    this.cargarUsuarios();
    this.cargarTickets();
    this.cargarComentarios();
    this.cargarNotificaciones();
    this.cargarContador();
    this.cargarResumenEncuestas();

    this.busquedaSub = this.busqueda$.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(valor => {
      this.busqueda = valor;
      this.pagina = 0;
      this.cargarComentarios();
    });

    this.autoRefreshSub = interval(8000).subscribe(() => {
      this.cargarComentarios();
      this.cargarNotificaciones();
      this.cargarContador();
      this.cargarResumenEncuestas();
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

  get ticketParaEncuesta(): Ticket | undefined {
    return this.tickets.find(t => t.id === this.filtroTicketId);
  }

  get puedeResponderEncuesta(): boolean {
    return !!this.encuestaSeleccionada && !this.encuestaSeleccionada.respondida;
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
        if (res.length > 0 && !this.nuevoComentarioTicketId) {
          this.nuevoComentarioTicketId = res[0].id;
        }
        if (this.filtroTicketId && !res.some(t => t.id === this.filtroTicketId)) {
          this.filtroTicketId = null;
        }
        this.cargarEncuesta(this.filtroTicketId);
        this.cdr.markForCheck();
      },
      error: () => console.warn('Tickets no disponibles para comentarios')
    });
  }

  cargarComentarios(): void {
    this.cargandoComentarios = true;
    this.helpdeskService.getComentariosPaginados({
      ticketId: this.filtroTicketId,
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
        this.cargarAdjuntosVisibles();
        this.cdr.markForCheck();
      },
      error: () => {
        this.cargandoComentarios = false;
        this.snackBar.open('Servicio de comunicacion no disponible', 'Cerrar', { duration: 3000 });
      }
    });
  }

  cargarAdjuntosVisibles(): void {
    const conId = this.comentarios.filter(c => !!c.id);
    if (conId.length === 0) {
      this.adjuntosPorComentario = {};
      return;
    }
    const peticiones = conId.map(c => this.helpdeskService.getAdjuntos(c.id!)
      .pipe(catchError(() => of([] as Adjunto[]))));
    forkJoin(peticiones).subscribe({
      next: (respuestas) => {
        const mapa: Record<number, Adjunto[]> = {};
        conId.forEach((c, indice) => mapa[c.id!] = respuestas[indice] ?? []);
        this.adjuntosPorComentario = mapa;
        this.cdr.markForCheck();
      }
    });
  }

  cargarNotificaciones(): void {
    const filtros = {
      usuarioId: this.usuarioActualId ?? null,
      tipo: this.filtroNotifTipo || null,
      ticketId: this.filtroNotifTicketId,
      leida: this.filtroNotifLeida === '' ? null : this.filtroNotifLeida === 'true'
    };
    this.helpdeskService.getNotificaciones(filtros).subscribe({
      next: (res) => {
        this.notificaciones = [...res];
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

  cargarResumenEncuestas(): void {
    this.helpdeskService.getResumenEncuestas().subscribe({
      next: (res) => {
        this.resumenEncuestas = res;
        this.cdr.markForCheck();
      },
      error: () => console.warn('Resumen de encuestas no disponible')
    });
  }

  cargarEncuesta(ticketId: number | null | undefined): void {
    if (!ticketId) {
      this.encuestaSeleccionada = undefined;
      return;
    }
    this.helpdeskService.getEncuesta(ticketId).subscribe({
      next: (res) => {
        this.encuestaSeleccionada = res;
        this.encuestaPuntuacion = res.puntuacion ?? 0;
        this.encuestaComentario = res.comentario ?? '';
        this.cdr.markForCheck();
      },
      error: () => {
        this.encuestaSeleccionada = undefined;
        this.encuestaPuntuacion = 0;
        this.encuestaComentario = '';
        this.cdr.markForCheck();
      }
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

  onFiltroTicketChange(ticketId: number | null): void {
    this.filtroTicketId = ticketId;
    this.pagina = 0;
    this.cargarComentarios();
    this.cargarEncuesta(ticketId);
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
    this.cargarComentarios();
    this.cargarNotificaciones();
    this.cargarContador();
    this.cargarResumenEncuestas();
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
      next: (creado) => {
        const finalizar = (mensaje: string) => {
          this.enviandoComentario = false;
          this.nuevoComentarioTexto = '';
          this.archivosSeleccionados = [];
          this.snackBar.open(mensaje, 'OK', { duration: 3000 });
          this.pagina = 0;
          this.cargarComentarios();
          this.cargarNotificaciones();
          this.cargarContador();
        };

        if (creado.id && this.archivosSeleccionados.length > 0) {
          this.helpdeskService.subirAdjuntos(creado.id, this.archivosSeleccionados).subscribe({
            next: () => finalizar('Comentario y adjuntos registrados'),
            error: () => finalizar('Comentario guardado, pero no se pudieron subir los adjuntos')
          });
        } else {
          finalizar('Comentario registrado con exito');
        }
      },
      error: (err) => {
        this.enviandoComentario = false;
        const detalle = err?.error?.detail ?? 'Error al registrar comentario';
        this.snackBar.open(detalle, 'Cerrar', { duration: 4000 });
      }
    });
  }

  seleccionarArchivos(event: Event): void {
    const input = event.target as HTMLInputElement;
    const archivos = Array.from(input.files ?? []);
    if (archivos.length === 0) return;

    const permitidos = ['image/png', 'image/jpeg', 'application/pdf'];
    const maximo = 5 * 1024 * 1024;
    if (archivos.length > 3) {
      this.snackBar.open('Maximo 3 archivos por comentario', 'Cerrar', { duration: 3000 });
      input.value = '';
      return;
    }
    const invalido = archivos.find(a => !permitidos.includes(a.type) || a.size > maximo);
    if (invalido) {
      this.snackBar.open('Solo PNG/JPG/PDF de hasta 5 MB', 'Cerrar', { duration: 3000 });
      input.value = '';
      return;
    }
    this.archivosSeleccionados = archivos;
    input.value = '';
  }

  quitarArchivo(indice: number): void {
    this.archivosSeleccionados = this.archivosSeleccionados.filter((_, i) => i !== indice);
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
      this.helpdeskService.marcarNotificacionLeida(n.id).subscribe({
        next: () => {
          this.cargarContador();
          this.cargarNotificaciones();
        }
      });
    }
    if (n.ticketId) {
      this.filtroTicketId = n.ticketId;
      this.pagina = 0;
      this.cargarComentarios();
      this.cargarEncuesta(n.ticketId);
      setTimeout(() => document.getElementById('bitacora')?.scrollIntoView({ behavior: 'smooth' }), 120);
    }
  }

  responderEncuesta(): void {
    if (!this.encuestaSeleccionada || !this.filtroTicketId) return;
    if (this.encuestaPuntuacion < 1 || this.encuestaPuntuacion > 5) {
      this.snackBar.open('Selecciona una puntuacion de 1 a 5', 'Cerrar', { duration: 3000 });
      return;
    }
    this.enviandoEncuesta = true;
    this.helpdeskService.responderEncuesta(
      this.filtroTicketId, this.encuestaPuntuacion, this.encuestaComentario
    ).subscribe({
      next: (res) => {
        this.enviandoEncuesta = false;
        this.encuestaSeleccionada = res;
        this.encuestaPuntuacion = res.puntuacion ?? 0;
        this.encuestaComentario = res.comentario ?? '';
        this.snackBar.open('Gracias por calificar la atencion', 'OK', { duration: 3000 });
        this.cargarResumenEncuestas();
        this.cargarNotificaciones();
        this.cargarContador();
      },
      error: (err) => {
        this.enviandoEncuesta = false;
        const detalle = err?.error?.detail ?? 'No se pudo guardar la encuesta';
        this.snackBar.open(detalle, 'Cerrar', { duration: 4000 });
      }
    });
  }

  abrirAdjunto(adjunto: Adjunto, descargar = false): void {
    // Se pide con HttpClient (lleva el token) y se abre como blob: un enlace directo no enviaria el token.
    const url = descargar ? adjunto.urlDescarga : adjunto.urlVista;
    this.helpdeskService.getArchivo(url).subscribe({
      next: (blob) => {
        const temporal = URL.createObjectURL(blob);
        if (descargar) {
          const enlace = document.createElement('a');
          enlace.href = temporal;
          enlace.download = adjunto.nombreOriginal;
          enlace.click();
        } else {
          window.open(temporal, '_blank');
        }
        setTimeout(() => URL.revokeObjectURL(temporal), 60000);
      },
      error: () => this.snackBar.open('No se pudo abrir el adjunto', 'Cerrar', { duration: 3000 })
    });
  }

  adjuntosDe(comentarioId?: number): Adjunto[] {
    return comentarioId ? (this.adjuntosPorComentario[comentarioId] ?? []) : [];
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
      case 'ENCUESTA_SATISFACCION': return 'Encuesta';
      default: return 'Sistema';
    }
  }

  iconoTipo(tipo?: TipoNotificacion | null): string {
    switch (tipo) {
      case 'TICKET_ESTADO': return 'sync';
      case 'COMENTARIO_NUEVO': return 'chat';
      case 'ENCUESTA_SATISFACCION': return 'star';
      default: return 'notifications';
    }
  }

  claseTipo(tipo?: TipoNotificacion | null): string {
    return `tipo-${(tipo ?? 'SISTEMA').toLowerCase()}`;
  }

  formatearTamano(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
  }

  esImagen(adjunto: Adjunto): boolean {
    return adjunto.contentType.startsWith('image/');
  }

  porcentajeDistribucion(puntuacion: number): number {
    const total = this.resumenEncuestas.respondidas;
    if (!total) return 0;
    return Math.round(((this.resumenEncuestas.distribucion[puntuacion] ?? 0) / total) * 100);
  }

  trackByComentario(_: number, c: Comentario): number | undefined {
    return c.id;
  }

  trackByNotificacion(_: number, n: Notificacion): number | undefined {
    return n.id;
  }
}
