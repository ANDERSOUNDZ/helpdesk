import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { Usuario } from '../../models/helpdesk.model';

export interface ConfirmarEliminarData {
  usuario: Usuario;
  /** Tickets (como solicitante o como tecnico) ligados al usuario; null si no se pudieron consultar. */
  ticketsAsociados: number | null;
}

@Component({
  selector: 'app-confirmar-eliminar-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule],
  templateUrl: './confirmar-eliminar-dialog.html',
  styleUrl: './confirmar-eliminar-dialog.scss',
})
export class ConfirmarEliminarDialog {
  readonly dialogRef = inject(MatDialogRef<ConfirmarEliminarDialog>);
  readonly data: ConfirmarEliminarData = inject(MAT_DIALOG_DATA);

  cancelar(): void {
    this.dialogRef.close(false);
  }

  confirmar(): void {
    this.dialogRef.close(true);
  }
}
