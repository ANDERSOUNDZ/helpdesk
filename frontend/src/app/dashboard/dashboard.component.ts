import { Component, inject, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subscription, interval } from 'rxjs';
import { HelpdeskService } from '../services/helpdesk.service';
import { ResumenTickets, ContadorEstado, Ticket, Usuario } from '../models/helpdesk.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatTableModule,
    MatChipsModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatSelectModule
  ],
})
export class DashboardComponent implements OnInit, OnDestroy {
  private helpdeskService = inject(HelpdeskService);
  private snackBar = inject(MatSnackBar);
  private cdr = inject(ChangeDetectorRef);
  private autoRefreshSub?: Subscription;

  resumen: ResumenTickets = { abiertos: 0, enProgreso: 0, resueltos: 0, total: 0 };
  contadoresDetalle: ContadorEstado[] = [];
  todosLosTickets: Ticket[] = [];
  usuarios: Usuario[] = [];

  // Filtros del Reporte
  filtroEstado: string = 'TODOS';
  filtroPrioridad: string = 'TODAS';

  columnasReporte: string[] = ['id', 'titulo', 'solicitante', 'prioridad', 'estado', 'tecnico'];

  ngOnInit(): void {
    this.cargarDatos();
    this.cargarUsuarios();
    this.autoRefreshSub = interval(3000).subscribe(() => {
      this.cargarDatos();
    });
  }

  ngOnDestroy(): void {
    if (this.autoRefreshSub) {
      this.autoRefreshSub.unsubscribe();
    }
  }

  cargarUsuarios(): void {
    this.helpdeskService.getUsuarios().subscribe({
      next: (res) => (this.usuarios = res),
      error: () => console.warn('Usuarios no disponibles para reporte')
    });
  }

  getNombreTecnico(tecnicoId?: number | null): string {
    if (!tecnicoId) return 'Sin Asignar';
    const tec = this.usuarios.find(u => u.id === tecnicoId);
    return tec ? `${tec.nombre}` : `Tecnico #${tecnicoId}`;
  }

  extraerSolicitante(ticket: Ticket): string {
    if (!ticket.descripcion) return 'Usuario General';
    if (ticket.descripcion.startsWith('Solicitante:')) {
      const fin = ticket.descripcion.indexOf('. Detalle:');
      if (fin !== -1) {
        return ticket.descripcion.substring(13, fin).trim();
      }
      return ticket.descripcion.replace('Solicitante:', '').replace('.', '').trim();
    }
    return 'Usuario General';
  }

  cargarDatos(): void {
    this.helpdeskService.getTickets().subscribe({
      next: (tickets) => {
        this.todosLosTickets = [...tickets];
        const abiertos = tickets.filter(t => t.estado === 'ABIERTO').length;
        const enProgreso = tickets.filter(t => t.estado === 'EN_PROGRESO').length;
        const resueltos = tickets.filter(t => t.estado === 'RESUELTO').length;
        const total = tickets.length;

        this.resumen = { abiertos, enProgreso, resueltos, total };
        this.contadoresDetalle = [
          { estado: 'ABIERTO', cantidad: abiertos },
          { estado: 'EN_PROGRESO', cantidad: enProgreso },
          { estado: 'RESUELTO', cantidad: resueltos }
        ];
        this.cdr.markForCheck();
      },
      error: () => {
        this.helpdeskService.getResumen().subscribe({
          next: (res) => {
            this.resumen = { ...res };
            this.cdr.markForCheck();
          }
        });
      }
    });
  }

  get ticketsFiltrados(): Ticket[] {
    return this.todosLosTickets.filter(t => {
      const coincideEstado = this.filtroEstado === 'TODOS' || t.estado === this.filtroEstado;
      const coincidePrioridad = this.filtroPrioridad === 'TODAS' || t.prioridad === this.filtroPrioridad;
      return coincideEstado && coincidePrioridad;
    });
  }

  resetearEstadisticas(): void {
    this.helpdeskService.resetearEstadisticas().subscribe({
      next: () => {
        this.snackBar.open('Contadores reiniciados en servicio-estadisticas', 'Cerrar', { duration: 3000 });
        this.cargarDatos();
      },
      error: () => this.snackBar.open('Error al resetear metricas', 'Cerrar', { duration: 3000 })
    });
  }
}
