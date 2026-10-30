import { Component, OnInit, OnDestroy, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Subscription, interval } from 'rxjs';
import { HelpdeskService } from '../services/helpdesk.service';
import { Ticket, Usuario, Prioridad, Comentario } from '../models/helpdesk.model';
import { AsignarDialog } from './asignar-dialog/asignar-dialog';
import { DetalleDialog } from './detalle-dialog/detalle-dialog';

@Component({
  selector: 'app-tickets',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDialogModule,
    MatSnackBarModule
  ],
  templateUrl: './tickets.html',
  styleUrl: './tickets.scss',
})
export class Tickets implements OnInit, OnDestroy {
  private helpdeskService = inject(HelpdeskService);
  private snackBar = inject(MatSnackBar);
  private dialog = inject(MatDialog);
  private cdr = inject(ChangeDetectorRef);
  private autoRefreshSub?: Subscription;

  tickets: Ticket[] = [];
  usuarios: Usuario[] = [];
  columnasTickets: string[] = ['id', 'titulo', 'prioridad', 'estado', 'tecnico', 'bitacora', 'acciones'];

  nuevoTitulo = '';
  nuevaDescripcion = '';
  nuevaPrioridad: Prioridad = 'MEDIA';
  solicitanteNombre = '';
  departamentoSeleccionado = 'Operaciones';

  departamentos: string[] = ['Contabilidad', 'Recursos Humanos', 'Ventas', 'Operaciones', 'Tecnologia / TI', 'Logistica'];

  ngOnInit(): void {
    this.cargarDatos();
    this.autoRefreshSub = interval(3000).subscribe(() => {
      this.cargarTickets();
    });
  }

  ngOnDestroy(): void {
    if (this.autoRefreshSub) {
      this.autoRefreshSub.unsubscribe();
    }
  }

  cargarDatos(): void {
    this.cargarTickets();
    this.cargarUsuarios();
  }

  cargarTickets(): void {
    this.helpdeskService.getTickets().subscribe({
      next: (res) => {
        this.tickets = [...res];
        this.cdr.markForCheck();
      },
      error: () => console.warn('Servicio de tickets no disponible')
    });
  }

  cargarUsuarios(): void {
    this.helpdeskService.getUsuarios().subscribe({
      next: (res) => (this.usuarios = res),
      error: () => console.warn('Servicio de identidad no disponible')
    });
  }

  get tecnicosDisponibles(): Usuario[] {
    return this.usuarios.filter(u => u.rol === 'TECNICO' || u.rol === 'ADMINISTRADOR');
  }

  getNombreTecnico(tecnicoId?: number | null): string {
    if (!tecnicoId) return 'Sin Asignar';
    const tec = this.usuarios.find(u => u.id === tecnicoId);
    return tec ? `${tec.nombre} (#${tec.id})` : `Tecnico #${tecnicoId}`;
  }

  crearTicket(): void {
    if (!this.nuevoTitulo.trim()) {
      this.snackBar.open('El titulo del incidente es obligatorio', 'Cerrar', { duration: 3000 });
      return;
    }
    const solicitanteInfo = this.solicitanteNombre.trim() ? `${this.solicitanteNombre.trim()} [${this.departamentoSeleccionado}]` : `Usuario [${this.departamentoSeleccionado}]`;
    const descripcionCompleta = this.nuevaDescripcion.trim()
      ? `Solicitante: ${solicitanteInfo}. Detalle: ${this.nuevaDescripcion.trim()}`
      : `Solicitante: ${solicitanteInfo}.`;

    const nuevo: Ticket = {
      titulo: this.nuevoTitulo,
      descripcion: descripcionCompleta,
      prioridad: this.nuevaPrioridad,
      solicitanteId: 1
    };
    this.helpdeskService.crearTicket(nuevo).subscribe({
      next: (creado) => {
        this.snackBar.open(`Incidente #${creado.id} registrado para ${solicitanteInfo}`, 'OK', { duration: 3000 });
        this.nuevoTitulo = '';
        this.nuevaDescripcion = '';
        this.solicitanteNombre = '';
        this.nuevaPrioridad = 'MEDIA';
        this.cargarTickets();
      },
      error: () => this.snackBar.open('Error al conectar con servicio-tickets', 'Cerrar', { duration: 3000 })
    });
  }

  // --- MODAL DE ASIGNACIÓN DE TÉCNICO ---
  abrirModalAsignar(ticket: Ticket): void {
    const dialogRef = this.dialog.open(AsignarDialog, {
      width: '420px',
      data: {
        ticket,
        tecnicos: this.tecnicosDisponibles
      }
    });

    dialogRef.afterClosed().subscribe((tecnicoId: number | undefined) => {
      if (tecnicoId && ticket.id) {
        this.helpdeskService.asignarTicket(ticket.id, tecnicoId).subscribe({
          next: () => {
            this.snackBar.open(`Ticket #${ticket.id} asignado a tecnico #${tecnicoId}`, 'OK', { duration: 3000 });
            this.cargarTickets();
          },
          error: () => this.snackBar.open('Error al asignar tecnico', 'Cerrar', { duration: 3000 })
        });
      }
    });
  }

  // --- MODAL DE BITÁCORA Y NOTAS TÉCNICAS (servicio-comunicacion) ---
  abrirModalDetalle(ticket: Ticket): void {
    this.dialog.open(DetalleDialog, {
      width: '550px',
      data: {
        ticket,
        nombreTecnico: this.getNombreTecnico(ticket.tecnicoId)
      }
    });
  }

  // --- TÉCNICO RESUELVE EL INCIDENTE ---
  resolverTicket(ticket: Ticket): void {
    if (!ticket.id) return;
    this.helpdeskService.resolverTicket(ticket.id).subscribe({
      next: () => {
        // Registra nota automática de resolución en servicio-comunicacion
        const nota: Comentario = {
          ticketId: ticket.id!,
          autorId: ticket.tecnicoId || 1,
          texto: 'Trabajo tecnico finalizado satisfactoriamente. Incidente resuelto, en espera de validacion del solicitante.'
        };
        this.helpdeskService.crearComentario(nota).subscribe();

        this.snackBar.open(`Ticket #${ticket.id} resuelto por el tecnico con nota de finalizacion`, 'OK', { duration: 3500 });
        this.cargarTickets();
      },
      error: () => this.snackBar.open('Error al resolver ticket', 'Cerrar', { duration: 3000 })
    });
  }

  // --- SOLICITANTE REABRE EL TICKET ---
  reabrirTicket(ticket: Ticket): void {
    if (!ticket.id) return;
    this.helpdeskService.reabrirTicket(ticket.id).subscribe({
      next: () => {
        // Registra nota del solicitante en servicio-comunicacion
        const nota: Comentario = {
          ticketId: ticket.id!,
          autorId: ticket.solicitanteId,
          texto: 'Ticket reabierto por el solicitante: el problema o falla persiste en el mismo caso.'
        };
        this.helpdeskService.crearComentario(nota).subscribe();

        this.snackBar.open(`Ticket #${ticket.id} reabierto. El tecnico fue notificado de que la falla persiste.`, 'OK', { duration: 3500 });
        this.cargarTickets();
      },
      error: () => this.snackBar.open('Error al reabrir ticket', 'Cerrar', { duration: 3000 })
    });
  }

  // --- SOLICITANTE PIDE OTRO TICKET RELACIONADO ---
  pedirOtroTicketRelacionado(ticket: Ticket): void {
    this.nuevoTitulo = `Derivado de #${ticket.id}: `;
    this.nuevaDescripcion = `Caso nuevo relacionado al incidente previo #${ticket.id} (${ticket.titulo}).`;
    this.nuevaPrioridad = ticket.prioridad;
    this.snackBar.open(`Formulario preparado para registrar seguimiento de #${ticket.id}`, 'Cerrar', { duration: 3000 });
  }

  eliminarTicket(id?: number): void {
    if (!id) return;
    this.helpdeskService.eliminarTicket(id).subscribe({
      next: () => {
        this.snackBar.open(`Ticket #${id} eliminado`, 'OK', { duration: 3000 });
        this.cargarTickets();
      },
      error: () => this.snackBar.open('Error al eliminar ticket', 'Cerrar', { duration: 3000 })
    });
  }
}
