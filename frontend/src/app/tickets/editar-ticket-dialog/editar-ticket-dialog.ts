import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Ticket, Prioridad } from '../../models/helpdesk.model';

@Component({
  selector: 'app-editar-ticket-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule
  ],
  templateUrl: './editar-ticket-dialog.html',
  styleUrl: './editar-ticket-dialog.scss',
})
export class EditarTicketDialog {
  readonly dialogRef = inject(MatDialogRef<EditarTicketDialog>);
  readonly data: Ticket = inject(MAT_DIALOG_DATA);

  titulo: string = this.data.titulo;
  descripcion: string = this.data.descripcion || '';
  prioridad: Prioridad = this.data.prioridad;

  cancelar(): void {
    this.dialogRef.close();
  }

  guardar(): void {
    if (this.titulo.trim()) {
      this.dialogRef.close({
        id: this.data.id,
        titulo: this.titulo.trim(),
        descripcion: this.descripcion.trim(),
        prioridad: this.prioridad
      });
    }
  }
}
