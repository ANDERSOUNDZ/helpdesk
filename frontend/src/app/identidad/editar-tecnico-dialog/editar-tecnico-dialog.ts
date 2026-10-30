import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Usuario } from '../../models/helpdesk.model';

@Component({
  selector: 'app-editar-tecnico-dialog',
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
  templateUrl: './editar-tecnico-dialog.html',
  styleUrl: './editar-tecnico-dialog.scss',
})
export class EditarTecnicoDialog {
  readonly dialogRef = inject(MatDialogRef<EditarTecnicoDialog>);
  readonly data: Usuario = inject(MAT_DIALOG_DATA);

  nombre: string = this.data.nombre;
  rol: 'USUARIO' | 'TECNICO' | 'ADMINISTRADOR' = this.data.rol;

  cancelar(): void {
    this.dialogRef.close();
  }

  guardar(): void {
    if (this.nombre.trim()) {
      this.dialogRef.close({
        id: this.data.id,
        nombre: this.nombre.trim(),
        rol: this.rol
      });
    }
  }
}
