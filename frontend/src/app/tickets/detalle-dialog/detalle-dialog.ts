import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatSnackBar } from '@angular/material/snack-bar';
import { HelpdeskService } from '../../services/helpdesk.service';
import { Ticket, Comentario } from '../../models/helpdesk.model';

export interface DetalleDialogData {
  ticket: Ticket;
  nombreTecnico: string;
}

@Component({
  selector: 'app-detalle-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatDividerModule
  ],
  templateUrl: './detalle-dialog.html',
  styleUrl: './detalle-dialog.scss',
})
export class DetalleDialog implements OnInit {
  readonly dialogRef = inject(MatDialogRef<DetalleDialog>);
  readonly data: DetalleDialogData = inject(MAT_DIALOG_DATA);
  private helpdeskService = inject(HelpdeskService);
  private snackBar = inject(MatSnackBar);

  comentarios: Comentario[] = [];
  nuevaNota = '';

  ngOnInit(): void {
    this.cargarComentarios();
  }

  cargarComentarios(): void {
    if (!this.data.ticket.id) return;
    this.helpdeskService.getComentarios(this.data.ticket.id).subscribe({
      next: (res) => (this.comentarios = res),
      error: () => console.warn('Error al cargar comentarios')
    });
  }

  agregarNota(): void {
    if (!this.nuevaNota.trim() || !this.data.ticket.id) return;
    const comentario: Comentario = {
      ticketId: this.data.ticket.id,
      autorId: 1,
      texto: this.nuevaNota
    };
    this.helpdeskService.crearComentario(comentario).subscribe({
      next: () => {
        this.snackBar.open('Nota tecnica agregada a la bitacora', 'OK', { duration: 2500 });
        this.nuevaNota = '';
        this.cargarComentarios();
      },
      error: () => this.snackBar.open('Error al guardar nota', 'Cerrar', { duration: 3000 })
    });
  }

  cerrar(): void {
    this.dialogRef.close();
  }
}
