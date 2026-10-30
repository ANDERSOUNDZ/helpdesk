import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Ticket, Usuario } from '../../models/helpdesk.model';

export interface AsignarDialogData {
  ticket: Ticket;
  tecnicos: Usuario[];
}

@Component({
  selector: 'app-asignar-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule
  ],
  templateUrl: './asignar-dialog.html',
  styleUrl: './asignar-dialog.scss',
})
export class AsignarDialog {
  readonly dialogRef = inject(MatDialogRef<AsignarDialog>);
  readonly data: AsignarDialogData = inject(MAT_DIALOG_DATA);

  tecnicoIdSeleccionado: number | null = this.data.ticket.tecnicoId || null;

  cancelar(): void {
    this.dialogRef.close();
  }

  confirmar(): void {
    if (this.tecnicoIdSeleccionado) {
      this.dialogRef.close(this.tecnicoIdSeleccionado);
    }
  }
}
