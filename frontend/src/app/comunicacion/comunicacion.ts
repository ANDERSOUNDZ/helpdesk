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
import { Subscription, interval } from 'rxjs';
import { HelpdeskService } from '../services/helpdesk.service';
import { Ticket, Comentario, Notificacion } from '../models/helpdesk.model';

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

  tickets: Ticket[] = [];
  comentarios: Comentario[] = [];
  notificaciones: Notificacion[] = [];

  nuevoComentarioTexto = '';
  nuevoComentarioTicketId: number = 1;

  ngOnInit(): void {
    this.cargarDatos();
    this.autoRefreshSub = interval(3000).subscribe(() => {
      this.cargarDatos();
    });
  }

  ngOnDestroy(): void {
    if (this.autoRefreshSub) {
      this.autoRefreshSub.unsubscribe();
    }
  }

  cargarDatos(): void {
    this.cargarTickets();
    this.cargarComentarios();
    this.cargarNotificaciones();
  }

  cargarTickets(): void {
    this.helpdeskService.getTickets().subscribe({
      next: (res) => {
        this.tickets = [...res];
        if (res.length > 0 && !this.nuevoComentarioTicketId) {
          this.nuevoComentarioTicketId = res[0].id!;
        }
        this.cdr.markForCheck();
      },
      error: () => console.warn('Tickets no disponibles para comentarios')
    });
  }

  cargarComentarios(): void {
    this.helpdeskService.getComentarios().subscribe({
      next: (res) => {
        this.comentarios = [...res];
        this.cdr.markForCheck();
      },
      error: () => console.warn('Servicio de comunicacion no disponible')
    });
  }

  cargarNotificaciones(): void {
    this.helpdeskService.getNotificaciones().subscribe({
      next: (res) => {
        this.notificaciones = [...res];
        this.cdr.markForCheck();
      },
      error: () => console.warn('Notificaciones no disponibles')
    });
  }

  agregarComentario(): void {
    if (!this.nuevoComentarioTexto.trim()) {
      this.snackBar.open('El comentario no puede estar vacio', 'Cerrar', { duration: 3000 });
      return;
    }
    const nuevo: Comentario = {
      ticketId: this.nuevoComentarioTicketId,
      texto: this.nuevoComentarioTexto,
      autorId: 1
    };
    this.helpdeskService.crearComentario(nuevo).subscribe({
      next: () => {
        this.snackBar.open('Comentario registrado con exito', 'OK', { duration: 3000 });
        this.nuevoComentarioTexto = '';
        this.cargarComentarios();
      },
      error: () => this.snackBar.open('Error al registrar comentario', 'Cerrar', { duration: 3000 })
    });
  }

  eliminarComentario(id?: number): void {
    if (!id) return;
    this.helpdeskService.eliminarComentario(id).subscribe({
      next: () => {
        this.snackBar.open('Comentario eliminado', 'OK', { duration: 3000 });
        this.cargarComentarios();
      },
      error: () => this.snackBar.open('Error al eliminar comentario', 'Cerrar', { duration: 3000 })
    });
  }

  marcarNotificacionLeida(id?: number): void {
    if (!id) return;
    this.helpdeskService.marcarNotificacionLeida(id).subscribe({
      next: () => this.cargarNotificaciones(),
      error: () => this.snackBar.open('Error al actualizar notificacion', 'Cerrar', { duration: 3000 })
    });
  }
}
